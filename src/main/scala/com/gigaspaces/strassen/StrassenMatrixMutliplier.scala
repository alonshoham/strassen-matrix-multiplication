package com.gigaspaces.strassen

import org.apache.spark.mllib.linalg.distributed.DistributedMatrix

class StrassenMatrixMutliplier {

  def multiply (A: DistributedMatrix,B: DistributedMatrix): DistributedMatrix = {
    null
  }
  def multiply (A: Array[Double],B: Array[Double]): Array[Double] = {
    null
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
