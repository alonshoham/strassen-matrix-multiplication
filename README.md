# Strassen matrix multiplication algorithm on top of Spark

Basic ALGORITHM
---
To multiply two matrices of size n*n (where n=2^p) A and B:

1. Divide A and B to four sub-matrices of size n/2
2. Mark each sub-matrix by its quadrant position (A11, A12, A21, A22, B11, B12, B21, B22)
3. Combine the sub-matrices into seven Strassen matrices:
    1. M1 = (A11 + A22)*(B11 + B22)
    2. M2 = (A21 + A22)*B11
    3. M3 = A11*(B12 − B22)
    4. M4 = A22*(B21 − B11)
    5. M5 = (A11 + A12)*(B22)
    6. M6 = (A21 − A11)*(B11 + B12)
    7. M7 = (A12 − A22)*(B21 + B22)
4. The result matrix C=A*B quadrants will be:
   1. C11 = (M1 + M4 − M5 + M7)
   2. C12 = (M3 + M5)
   3. C21 = (M2 + M4)
   4. C22 = (M1 − M2 + M3 - M6)
5. This can be performed recursively, each stage runs on n/2 matrix


The implementation stages:

Matrices preparation  
---
The Strassen alogrithm is limited to square matrices of size n where n=2^p. In order to deal with any size,
we can pad the matrices with zero rows and columns up until it reaches the nearest power of 2. 
If the recursion level is smaller log2(n), the matrices will be padded to smaller dimensions, following this:
1. Given recursion level L and matrix of size n
2. Pad matrix to size n' such that n'=2^L * ceiling(n/2^L)
3. Example: n=221, L=3 => n'=8 * ceiling(221/8)=8 * ceiling(27.625)=> 8 * 28=224
This padding can also be applied to a stop condition given in sub-matrix size, from which the matrices will be multiplied regularly
1. Given sub-matrix size k and matrix of size n
2. Pad matrix to size n', such that n'=k * 2^(ceiling(log(n) - log(k))) 
3. Example n=221, k=17 => n' = 17 * ceiling(log(221) - log(17)) = 17 * 16 = 272

ALGORITHM 1: Distributed Strassen Matrix Multiplication  
---
Procedure DistStrass(RDD < Block > A,RDD < Block > B, int n)  
Result: RDD of blocks of the product matrix C  
**.size = Size of matrix A or B  
blockSize = Size of a single matrix block  
n = size/blockSize   
if n=1 then**  
*Boundary Condition: RDD A and B
contain blocks with a pair of blocks
(candidates for multiplication)
having same matname property*  
**MulBlockMat(A,B)  
else n = n/2**  
*Divide Matrix A and B into 4
sub-matrices each (A11 to B22).
Replicate and add or subtract the
sub-matrices so that they can form 7
sub-matrices (M1 to M7)*  
**D = DivNRep(A,B)**  
*Recursively call DistStrass() to
multiply two sub-matrices of block
size n/2*  
**R = DistStrass(A,B,n)**  
*Combine seven submatrices (single RDD of blocks (M1 to M7)) of size n/2
into
single matrix (RDD of blocks (C))*  
**C = Combine(R)  
end  
return C**  

ALGORITHM 2: Divide and Replication  
---
Procedure DivNRep(RDD < Block > A, RDD < Block > B)  
Result: RDD < Block > C  
*Make union of two input RDDs. Each block
of the resulting RDD having a tag with
string similar to A|B, M1|..|7, M-index.
In the first recursive call the tag is
A|B, M, 0*  
**RDD < Block > AunionB = A.union(B)**  
*Map each block to multiple (key, Block)
pairs according to the block index. For
example, A11 is replicated four times.
Each key contains string M1|2...|7,
M-index. Each block contains a tag with
string A11|12|21|22 or B11|12|21|22.*  
**PairRDD < string, Block > firstMap = AunionB.flatMapToPair()**  
*Group the blocks according to the key.
For each key this will group blocks with
tags that eventually form M1 to M7.*  
**PairRDD < string, iterable < Block >> group = firstMap.groupByKey()**  
*Add or subtract blocks with the tag
start with similar character (A|B) to get
the two blocks of RDDs for the next
divide phase.*  
**RDD < Block > C = group.mapToPair()  
return C**  

ALGORITHM 3: Block Matrix Multiplication  
---
Procedure MulBlockMat(RDD < Block > A, RDD < Block > B)  
*Result contains RDD of blocks. Each
block is the product of two matrix
blocks residing in the same computer.*  
Result: RDD < Block > C  
*Make union of two input RDDs. Each block
of the resulting RDD having a tag with
string similar to A|B, M1|2...|7, index.*  
**RDD < Block > AunionB = A.union(B)**  
*Map each block to a (key,Block) pair.
The key contains string M1|2...|7, index.
Each Block contains a tag with string
A|B.*   
**PairRDD < string, Block > firstMap = AunionB.mapToPair()**  
*Group the blocks according to the key.
For each key, this will group two
blocks, one with block tag A and another
with B.*  
**PairRDD < string, iterable < Block >> group =firstMap.groupByKey()**  
*Multiply two block matrix inside a
single computer serially and return each
Block to the resulting RDD.*  
**RDD < Block > C = group.map()  
return C**  

ALGORITHM 4: Combine Phase  
---
Procedure Combine(RDD < Block > BlockRDD)  
Result: RDD < Block > C  
*Map each block to (key,Block) pair. Both
the key and block mat-name contains
string M1|2...|7, index. indexes are divided
by 7 to blocks can be grouped of the
same M sub-matrix.*  
**PairRDD < String, Block > firstMap = BlockRDD.map()**  
*Group the blocks that comes from the
same M sub-matrix*  
**PairRDD < string, iterable < Block >> group = firstMap.groupByKey()**  
*combine the 7 sub-matrices of size n/2
to a single sub-matrix of size n having
the same key*  
**RDD < Block > C = group.flatMap()  
return C**  # strassen-matrix-multiplication
