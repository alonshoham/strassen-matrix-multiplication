package com.gigaspaces.strassen
import breeze.linalg._
import com.gigaspaces.strassen.MatrixTag._
import com.gigaspaces.strassen.StrassenMatrixTag.StrassenMatrixTag

import scala.collection.mutable

@SerialVersionUID(114L)
case class Block(matrixTag: MatrixTag, tag: List[StrassenMatrixTag], matrix: Matrix[Double]) extends Serializable{

  def splitBlockToStrassenComponents(): List[Block] = {
    val n = matrix.rows
    val matrix11 = matrix(0 until n / 2, 0 until n / 2).copy
    val matrix12 = matrix(0 until n / 2, n / 2 until n).copy
    val matrix21 = matrix(n / 2 until n, 0 until n / 2).copy
    val matrix22 = matrix(n / 2 until n, n / 2 until n).copy
    matrixTag match {
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
  }

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
