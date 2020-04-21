package com.gigaspaces.strassen

import breeze.linalg.{DenseMatrix, Matrix}
import com.gigaspaces.strassen.StrassenMatrixTag.StrassenMatrixTag
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd._
import org.apache.spark.sql.SparkSession

import scala.language.implicitConversions
import scala.math._


class StrassenMatrixMultiplier extends Serializable {
  def strassenMultiply(A: Matrix[Double], B: Matrix[Double], recursionLevel: Int): Matrix[Double] = {
    require(A.cols == B.rows,"A number of columns must match B number of rows")
    //    require(newSizeExponent <= recursionLevel, "TBD")
    val (newA, newB) = prepareMatrices(A,B)
    val blockA = Block(MatrixTag.A, List(StrassenMatrixTag.M), newA)
    val blockB = Block(MatrixTag.B, List(StrassenMatrixTag.M), newB)
    val master: String = new SparkConf().get("spark.master", "local[*]")
    val sparkContext: SparkContext = SparkSession.builder().master(master).getOrCreate().sparkContext
    val rddA = sparkContext parallelize Seq(blockA)
    val rddB = sparkContext parallelize Seq(blockB)
    val splitA = recursiveStrassenSplit(rddA, recursionLevel)
    val splitB = recursiveStrassenSplit(rddB, recursionLevel)
    val serial = multiplyRDDs(mapMatricesToStrassenPairs(splitA, splitB))
    val result = recursiveStrassenCombine(serial, recursionLevel).first().matrix
    stripMatrixOfZeros(result, A.rows, B.cols)
  }

  def strassenMultiply(A: Matrix[Double], B: Matrix[Double]): Matrix[Double] = {
    strassenMultiply(A, B, maxRecursionLevel(A,B))
  }

  def strassenMultiply(matA: Array[Array[Double]], matB:Array[Array[Double]]): Matrix[Double] = {
    val A = array2DToMatrix(matA)
    val B = array2DToMatrix(matB)
    strassenMultiply(A,B)
  }

  def strassenMultiply(matA: Array[Array[Double]], matB:Array[Array[Double]], recursionLevel:Int): Matrix[Double] = {
    val A = array2DToMatrix(matA)
    val B = array2DToMatrix(matB)
    strassenMultiply(A,B, recursionLevel)
  }

  private def array2DToMatrix(array: Array[Array[Double]]): Matrix[Double] = {
    Matrix.create[Double](array.length,array.apply(0).length,array.flatten)
  }

  private def prepareMatrices(A: Matrix[Double], B: Matrix[Double]): (DenseMatrix[Double], DenseMatrix[Double]) = {
    if(isSquarePowerOfTwoMatrices(A,B))
      (A.toDenseMatrix, B.toDenseMatrix)
    val newSizeExponent: Int = calcExponent(A,B)
    (padMatrixWithZeros(A, pow(2,newSizeExponent).toInt), padMatrixWithZeros(B, pow(2,newSizeExponent).toInt))
  }

  private def isSquarePowerOfTwoMatrices(A: Matrix[Double], B: Matrix[Double]): Boolean = {
    A.cols == A.rows && A.cols == B.rows && B.rows == B.cols && ((A.cols & (A.cols - 1)) == 0)
  }

  private def maxRecursionLevel(A: Matrix[Double], B: Matrix[Double]): Int = {
    calcExponent(A,B) - 1
  }

  private def calcExponent(A: Matrix[Double], B: Matrix[Double]): Int = {
    val n = A.cols
    val m = A.rows
    val p = B.cols
    val max = Math.max(Math.max(n,m),p)
    32 - Integer.numberOfLeadingZeros(max - 1)
  }
  private def mapMatricesToStrassenPairs(rddA: RDD[Block], rddB: RDD[Block]) : RDD[(String, Iterable[Block])] = {
    rddA.union(rddB).flatMap(x => splitBlockToStrassenComponents(x)).keyBy(x=>x.tag.mkString(",")).groupByKey()
  }

  private def multiplyRDDs(rdd: RDD[(String, Iterable[Block])]): RDD[Block] = {
    rdd.mapValues(pair => {
      val A = pair.toSeq.head
      val B = pair.toSeq(1)
      Block(MatrixTag.C, A.tag, A.matrix * B.matrix)
    }).values
  }

  private def padMatrixWithZeros(originalMatrix:  Matrix[Double], newMatrixSize: Int) : DenseMatrix[Double] = {
    val result = Matrix.zeros[Double](newMatrixSize, newMatrixSize)
    result(0 until originalMatrix.rows, 0 until originalMatrix.cols) := originalMatrix
    result.toDenseMatrix
  }

  private def stripMatrixOfZeros(m: Matrix[Double], newMatrixRows: Int, newMatrixColumns: Int) : Matrix[Double] = {
    m(0 until newMatrixRows, 0 until newMatrixColumns)
  }

  private def splitBlockToStrassenComponents(block: Block): Iterable[Block] = {
    val matrix = block.matrix
    val matrixTag = block.matrixTag
    val tag = block.tag
    val n = matrix.rows
    val matrix11 = matrix(0 until n/2, 0 until n/2).toDenseMatrix
    val matrix12 = matrix(0 until n/2, n/2 until n).toDenseMatrix
    val matrix21 = matrix(n/2 until n, 0 until n/2).toDenseMatrix
    val matrix22 = matrix(n/2 until n, n/2 until n).toDenseMatrix
    val result = matrixTag match {
      case MatrixTag.A => List(
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M1), matrix11 + matrix22),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M2), matrix21 + matrix22),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M3), matrix11),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M4), matrix22),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M5), matrix11 + matrix12),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M6), matrix21 - matrix11),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M7), matrix12 - matrix22))
      case MatrixTag.B => List( //Matrix is B
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M1), matrix11 + matrix22),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M2), matrix11),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M3), matrix12 - matrix22),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M4), matrix21 - matrix11),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M5), matrix22),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M6), matrix11 + matrix12),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M7), matrix21 + matrix22))
    }
    result
  }

  private def combineStrassenComponents(blocks: Iterable[Block]): Block = {
    val n = blocks.head.matrix.rows
    var M1,M2,M3,M4,M5,M6,M7: DenseMatrix[Double] = Matrix.zeros[Double](n,n).toDenseMatrix
    var tags : List[StrassenMatrixTag] = List.empty
    blocks.foreach(block => {
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
    val res = DenseMatrix.zeros[Double](2*n, 2*n)

    res(0 until n, 0 until n) := M1 + M4 - M5 + M7 //C11
    res(0 until n, n until 2*n) := M3 + M5 //C12
    res(n until 2*n, 0 until n) := M2 + M4 //C21
    res(n until 2*n, n until 2*n) := M1 - M2 + M3 + M6 //C22

    val block = Block(MatrixTag.C, tags.take(tags.size - 1), res)
    block
  }

  private def recursiveStrassenSplit(rdd: RDD[Block], recursionLevel: Int): RDD[Block] = {
    if(recursionLevel==0)
      return rdd
    recursiveStrassenSplit(rdd.flatMap(block => splitBlockToStrassenComponents(block)), recursionLevel - 1)
  }

  private def recursiveStrassenCombine(rdd: RDD[Block], recursionLevel: Int): RDD[Block] = {
    val result = rdd.keyBy(block => block.parentTags()).groupByKey().values.map(blocks => combineStrassenComponents(blocks))
    if(recursionLevel == 0)
      return result
    recursiveStrassenCombine(result, recursionLevel - 1)
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
