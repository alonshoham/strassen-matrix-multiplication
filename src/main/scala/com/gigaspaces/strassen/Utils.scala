package com.gigaspaces.strassen

import java.util.Random

import breeze.linalg.Matrix
import breeze.stats.distributions.Rand
import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.distributed.BlockMatrix
import org.apache.spark.mllib.linalg.{DenseMatrix, Matrices}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

import scala.collection.mutable.ArrayBuffer

object Utils {
//  private val sparkContext: SparkContext = SparkSession.builder().master("local[*]").getOrCreate().sparkContext

//  def splitMatrixToQuadrants(matrix: Matrix): Array[BlockMatrix] = {
//    val nRows = matrix.numRows
//    val mCols = matrix.numCols
//    val upperLeftArr,upperRightArr,lowerLeftArr,lowerRightArr = ArrayBuffer[Double]()
//
//    for (i <- 0 until nRows) {
//      for (j <- 0 until mCols) {
//        val entry: Double = matrix.apply(i, j)
//        if (i < nRows / 2) { //upper half
//          if (j < mCols / 2) { // left half
//            upperLeftArr += entry
//          }
//          else { //right half
//            upperRightArr += entry
//          }
//        }
//        else { //lower half
//          if (j < mCols / 2) { // left half
//            lowerLeftArr += entry
//          }
//          else { //right half
//            lowerRightArr += entry
//          }
//        }
//      }
//
//    }
//    val upperLeftQuadrant = Matrices.dense(nRows / 2, mCols / 2, upperLeftArr.toArray)
//    val upperRightQuadrant = Matrices.dense(nRows / 2, mCols / 2, upperRightArr.toArray)
//    val lowerLeftQuadrant = Matrices.dense(nRows / 2, mCols / 2, lowerLeftArr.toArray)
//    val lowerRightQuadrant = Matrices.dense(nRows / 2, mCols / 2, lowerRightArr.toArray)
//    Array[Matrix](upperLeftQuadrant, upperRightQuadrant, lowerLeftQuadrant, lowerRightQuadrant).map(matrixToBlockMatrix)
//  }
//
//    def matrixToBlockMatrix(matrix: Matrix): BlockMatrix = {
//      val rdd: RDD[((Int,Int), Matrix)] = sparkContext.parallelize(Seq(((0,0), matrix)))
//      new BlockMatrix(rdd, matrix.numRows, matrix.numCols).transpose
//    }
//
//    def combineQuadrantsToMatrix(upperLeftQuadrant: Matrix, upperRightQuadrant: Matrix, lowerLeftQuadrant: Matrix, lowerRightQuadrant: Matrix): Matrix = {
//      val upperHalf = Matrices.horzcat(Array(upperLeftQuadrant,upperRightQuadrant))
//      val lowerHalf = Matrices.horzcat(Array(lowerLeftQuadrant,lowerRightQuadrant))
//      Matrices.vertcat(Array(upperHalf, lowerHalf))
//    }
//
//    def strassenMultiply(matA: Matrix, matB: Matrix): Matrix = {
//      val quadrantsA = splitMatrixToQuadrants(matA)
//      val quadrantsB = splitMatrixToQuadrants(matB)
//      val A11 = quadrantsA.apply(0)
//      val A12 = quadrantsA.apply(1)
//      val A21 = quadrantsA.apply(2)
//      val A22 = quadrantsA.apply(3)
//      val B11 = quadrantsB.apply(0)
//      val B12 = quadrantsB.apply(1)
//      val B21 = quadrantsB.apply(2)
//      val B22 = quadrantsB.apply(3)
//
//      // M1 = (A11 + A22)*(B11 + B22)
//      val M1 = A11.add(A22).multiply(B11.add(B22))
//      // M2 = (A21 + A22)*B11
//      val M2 = A21.add(A22).multiply(B11)
//      //M3 = A11*(B12 − B22)
//      val M3 = A11.multiply(B12.subtract(B22))
//      //M4 = A22*(B21 − B11)
//      val M4 = A22.multiply(B21.subtract(B11))
//      // M5 = (A11 + A12)*(B22)
//      val M5 = A11.add(A12).multiply(B22)
//      // M6 = (A21 − A11)*(B11 + B12)
//      val M6 = A21.subtract(A11).multiply(B11.add(B12))
//      // M7 = (A12 − A22)*(B21 + B22)
//      val M7 = A12.subtract(A22).multiply(B21.add(B22))
//      /*
//      1. C11 = (M1 + M4 − M5 + M7)
//     2. C12 = (M3 + M5)
//     3. C21 = (M2 + M4)
//     4. C22 = (M1 − M2 + M3 + M6)
//       */
//      val C11 = M1.add(M4).subtract(M5).add(M7).toLocalMatrix()
//      val C12 = M3.add(M5).toLocalMatrix()
//      val C21 = M2.add(M4).toLocalMatrix()
//      val C22 = M1.subtract(M2).add(M3).add(M6).toLocalMatrix()
//
//      combineQuadrantsToMatrix(C11, C12, C21, C22)
//    }

    def main(args: Array[String]): Unit = {
      val A: Matrix[Double] = Matrix.rand[Double](4,4, Rand.uniform)
      val B: Matrix[Double] = Matrix.rand[Double](4,4, Rand.uniform)
//      val B = Matrices.rand(4,4, new Random())
      println(s"~~~~~~~A: $A")
      println(s"~~~~~~~B: $B")
      val C = A * B
      println(s"~~~~~~~C:$C")
      //      val C = A.multiply(Matrices.dense(B.numRows,B.numCols, B.toArray).asInstanceOf[DenseMatrix])
      val strassenC = new StrassenMatrixMultiplier().strassenMultiply(A,B)

      println(strassenC)
    }
  }
