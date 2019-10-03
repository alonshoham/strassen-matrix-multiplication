package com.gigaspaces.strassen

object MatrixTag extends Enumeration {
  type MatrixTag = Value
  val A,B,C,A11,B11,C11,A12,B12,C12,A21,B21,C21,A22,B22,C22 = Value
}

object StrassenMatrixTag extends Enumeration {
  type StrassenMatrixTag = Value
  val M,M1,M2,M3,M4,M5,M6,M7 = Value
}
