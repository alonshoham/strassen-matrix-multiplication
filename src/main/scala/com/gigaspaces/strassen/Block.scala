package com.gigaspaces.strassen
import breeze.linalg._
import com.gigaspaces.strassen
import com.gigaspaces.strassen.MatrixTag._
import com.gigaspaces.strassen.StrassenMatrixTag.StrassenMatrixTag

import scala.collection.mutable

@SerialVersionUID(114L)
case class Block(matrixTag: MatrixTag, tag: List[StrassenMatrixTag], matrix: Matrix[Double]) extends Serializable{

  //substract
  //add
  //multiply
  /*
  split to Strassen matrix component:
  split matrix to quadrants
  calculate component by strassen matrix index and matrix tag
  return 7 blocks with component and add to the matrix tag the string: ",M1|2..|7"
   */
  /*
  multiply:
  last division contain blocks from A and B with the same tag
  multiply A and B and return block with matrix name C and the same tag
   */
  /*
  recombine:
  inspect all C matrices with identical tag up to the last identifier
  last identifier is this level's strassen M matrix
  create new C from identified M matrices
   */
  //get block size
  // get recursion level

  def splitMatrixToQuadrants(m: Matrix[Double]) : List[Matrix[Double]] = ???

  def splitBlockToStrassenComponents(): List[Block] = {
    val n = matrix.rows
    val matrix11 = matrix(0 until n / 2, 0 until n / 2).copy
    val matrix12 = matrix(0 until n / 2, n / 2 until n).copy
    val matrix21 = matrix(n / 2 until n, 0 until n / 2).copy
    val matrix22 = matrix(n / 2 until n, n / 2 until n).copy
    val res: List[Block] = matrixTag match {
      case MatrixTag.A => List(
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M1), matrix11 + matrix22),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M2), matrix21 + matrix22),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M3), matrix11),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M4), matrix22),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M5), matrix11 + matrix12),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M6), matrix21 - matrix11),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M7), matrix12 - matrix22))
      case MatrixTag.B => List(//Matrix is B
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M1), matrix11 + matrix22),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M2), matrix11),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M3), matrix12 - matrix22),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M4), matrix21 - matrix11),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M5), matrix22),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M6), matrix11 + matrix12),
        Block(matrixTag, tag ++ List(StrassenMatrixTag.M7), matrix21 + matrix22))
    }
    res
  }

  def recombineBlockFromStrassenMatrices[Block](matrice: Seq[Block]): Block = ???

  def combineQuadrantsToMatrix[Double](upperLeftQuadrant: Matrix[Double], upperRightQuadrant: Matrix[Double], lowerLeftQuadrant: Matrix[Double], lowerRightQuadrant: Matrix[Double]): Matrix[Double] = ???

  def calcParentTag(): String = {
    if(tag.size == 0)
      return ""
    val s = tag.mkString(",")
    val index = s.lastIndexOf(",")
    if(index == -1)
      return ""
    s.substring(0, index)
  }

}
