package com.gigaspaces.strassen

import breeze.linalg.{DenseMatrix, Matrix}
import com.gigaspaces.strassen.StrassenMatrixTag.StrassenMatrixTag
import org.apache.spark.SparkContext
import org.apache.spark.rdd._
import org.apache.spark.sql.SparkSession

import scala.Predef._
import scala.language.implicitConversions
import scala.math._


class StrassenMatrixMultiplier extends Serializable {
//  @transient
//  private val sparkContext: SparkContext = SparkSession.builder().master("local[*]").getOrCreate().sparkContext

  def strassenMultiply (A: Matrix[Double],B: Matrix[Double]): Matrix[Double] = {
    val n = A.rows
    require(n == B.rows,"Input matrices must be of equal size")
    if(n > 0){
      require(n == A.rows && n == B.rows,"Input matrices must be square matrices")
    }
    val recursionLevel: Int = ceil(log(n)/log(2)).toInt
    val paddedSize = pow(2,recursionLevel).toInt // matrix size is 2^(closest power of 2 larger than n)
    val paddedA = padMatrixWithZeros(A, n, paddedSize)
    val paddedB = padMatrixWithZeros(B, n, paddedSize)
    val blockA = Block(MatrixTag.A, List(StrassenMatrixTag.M), paddedA.toDenseMatrix)
    val blockB = Block(MatrixTag.B, List(StrassenMatrixTag.M), paddedB.toDenseMatrix)
    val C = recursiveStrassenMultiplication(blockA,blockB, paddedSize)
//    stripMatrixOfZeros(recombine(C).matrix, n)
    recombine(C).matrix
  }

  def strassenMultiply (matA: Array[Array[Double]],matB:Array[Array[Double]]): Matrix[Double] = {
    val n = matA.length
    require(n == matB.length,"Input matrices must be of equal size")
    if(n > 0){
      require(n == matA(0).length && n == matB(0).length,"Input matrices must be square matrices")
    }
    val originalAMatrix = Matrix.create[Double](n,n,matA.flatten)
    val originalBMatrix = Matrix.create[Double](n,n,matB.flatten)
    strassenMultiply(originalAMatrix,originalBMatrix)
  }

  private def recursiveStrassenMultiplication(A: Block, B: Block, n: Int): RDD[Block]= {
    println(s"recursive call: n=$n, rowsA=${A.matrix.rows}, rowsB=${B.matrix.rows}")
    val sparkContext: SparkContext = SparkSession.builder().master("local[*]").getOrCreate().sparkContext
    val rddA = sparkContext.parallelize(Seq(A))
    val rddB = sparkContext.parallelize(Seq(B))

    if (n==2){//|| n==pow(2,recursionLevel).toInt) {
      multiplyRDDs(mapMatricesToStrassenPairs(rddA, rddB))//returns an RDD of computed strassen matrices
    }
    else {
      mapMatricesToStrassenPairs(rddA, rddB).mapValues(pair => recombine(recursiveStrassenMultiplication(pair.toSeq(0), pair.toSeq(1), n/2))).values
    }
  }

  private def mapMatricesToStrassenPairs(rddA: RDD[Block], rddB: RDD[Block]) : RDD[(String, Iterable[Block])] = {
    rddA.union(rddB).flatMap(x => x.splitBlockToStrassenComponents).keyBy(x=>x.tag.mkString(",")).groupByKey()
  }

  def multiplyRDDs(rdd: RDD[(String, Iterable[Block])]): RDD[Block] = {
    rdd.mapValues(pair => {
      val A = pair.toSeq(0)
      val B = pair.toSeq(1)
      Block(MatrixTag.C, A.tag, A.matrix * B.matrix) // serial multiplication
    }).values
  }

  private def recombine(rdd: RDD[Block]) : Block = {
    val size = rdd.first().matrix.rows
    var M1,M2,M3,M4,M5,M6,M7: DenseMatrix[Double] = Matrix.zeros[Double](size,size).toDenseMatrix
    var tags : List[StrassenMatrixTag] = List.empty
    rdd.toLocalIterator.foreach(block => {
      tags = block.tag
      val m = block.matrix
      val lastTag = block.tag.last
      lastTag match {
        case StrassenMatrixTag.M1 => M1 := m
        case StrassenMatrixTag.M2 => M2 := m
        case StrassenMatrixTag.M3 => M3 := m
        case StrassenMatrixTag.M4 => M4 := m
        case StrassenMatrixTag.M5 => M5 := m
        case StrassenMatrixTag.M6 => M6 := m
        case StrassenMatrixTag.M7 => M7 := m
      }
    })
    val C11 = M1 + M4 - M5 + M7
    val C12 = M3 + M5
    val C21 = M2 + M4
    val C22 = M1 - M2 + M3 + M6
    val n = size * 2
    val res = DenseMatrix.zeros[Double](n, n)

    res(0 until n/2, 0 until n/2) := C11
    res(0 until n/2, n/2 until n) := C12
    res(n/2 until n, 0 until n/2) := C21
    res(n/2 until n, n/2 until n) := C22

    Block(MatrixTag.C, tags.take(tags.size - 1), res)
  }

  private def padMatrixWithZeros(originalMatrix:  Matrix[Double], oldMatrixSize: Int, newMatrixSize: Int) : Matrix[Double] = {
    val result = Matrix.zeros[Double](newMatrixSize, newMatrixSize)
    result(0 until oldMatrixSize, 0 until oldMatrixSize) := originalMatrix
    result
  }

  private def stripMatrixOfZeros(m: Matrix[Double], newMatrixSize: Int) : Matrix[Double] = {
    m(0 until newMatrixSize, 0 until newMatrixSize)
  }



  /*/
  Paramaters:
  Two matrices: either two 2d arrays or two spark matrices. requirement is that they are square matrices
  recursion level: determines the number of recursions to perform before moving to regular matrix multiplication. Default is log(n)
  Spark context: if working with spark cluster, the spark context can be injected. default is local[*]

  Parent algorithm:
  1. Prepare matrices: depending on recursion level pad matrices with empty rows&columns
  2. Convert matrices to Blocks

  Distributed Strassen:
  Takes two blocks A and B
  if A.recursionLevel == configured recursion level
  return A*B
  else
  divide A and B to 4 quadrants of size n/2
  partially calculate strassen matrices without multiplying -> using mapping of the quadrants to strassen pairs
  1. M1 = (A11 + A22)*(B11 + B22) = M1A*M1B
  2. M2 = (A21 + A22)*B11 = M2A*M2B
  3. M3 = A11*(B12 − B22) = M3A*M3B
  4. M4 = A22*(B21 − B11) = M4A*M4B
  5. M5 = (A11 + A12)*(B22) = M5A*M5B
  6. M6 = (A21 − A11)*(B11 + B12) = M6A*M6B
  7. M7 = (A12 − A22)*(B21 + B22) = M7A*M7B
  RECURSIVELY call each multiplication 1-7 with strassen algorithm
  Combine phase:
  calculate C - result matrix blocks:
  1. C11 = (M1 + M4 − M5 + M7)
  2. C12 = (M3 + M5)
  3. C21 = (M2 + M4)
  4. C22 = (M1 − M2 + M3 - M6)
  return C after Unpadding -> return output of 2d array
   */

}
