package com.gigaspaces.strassen

import java.util.concurrent.TimeUnit

import breeze.linalg.Matrix
import breeze.stats.distributions.Rand

object Utils {
    def main(args: Array[String]): Unit = {
      val n = Integer.valueOf(args(0))
      val recursionLevel = Integer.valueOf(args(1))
      var start = System.nanoTime()
      val A: Matrix[Double] = Matrix.rand[Double](n,n, Rand.uniform)
      val B: Matrix[Double] = Matrix.rand[Double](n,n, Rand.uniform)
      val C = A * B
      println(s"multiplication took ${TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start)} seconds")
      start = System.nanoTime()
      var strassenC = new StrassenMatrixMultiplier().strassenMultiply(A,B,recursionLevel)
      println(s"Strassen multiplication took ${TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start)} seconds")
    }
  }
