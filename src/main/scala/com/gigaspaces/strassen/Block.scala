package com.gigaspaces.strassen
import breeze.linalg._
import com.gigaspaces.strassen.MatrixTag._
import com.gigaspaces.strassen.StrassenMatrixTag.StrassenMatrixTag

@SerialVersionUID(114L)
case class Block(matrixTag: MatrixTag, tag: List[StrassenMatrixTag], matrix: DenseMatrix[Double]) extends Serializable{

}
