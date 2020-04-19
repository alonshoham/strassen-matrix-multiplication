package com.gigaspaces.strassen

import breeze.linalg.Matrix
import breeze.stats.distributions.Rand

object Utils {
    def main(args: Array[String]): Unit = {
      val A: Matrix[Double] = Matrix.rand[Double](2,3, Rand.uniform)
      val B: Matrix[Double] = Matrix.rand[Double](3,4, Rand.uniform)
      val C = A * B
      println(C)
      val strassenC = new StrassenMatrixMultiplier().strassenMultiply(A,B)
      println(strassenC - C)
    }
  }
