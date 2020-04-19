import com.gigaspaces.strassen.StrassenMatrixMultiplier;
import org.apache.spark.mllib.linalg.DenseMatrix;
import org.apache.spark.mllib.linalg.Matrices;
import org.apache.spark.mllib.linalg.Matrix;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class StrassenMatrixMulitplicationTest {
    @Test
    public void testBasicMuliplication(){
        int n = 5;
        Matrix A = DenseMatrix.rand(n,n, new Random());
        Matrix B = DenseMatrix.rand(n,n, new Random());
        Matrix C = A.multiply((DenseMatrix) Matrices.dense(B.numRows(),B.numCols(), B.toArray()));
        breeze.linalg.Matrix<Object> strassenC = new StrassenMatrixMultiplier().strassenMultiply(A.asBreeze(), B.asBreeze());
        Assert.assertTrue(isEqual(C,strassenC));
    }

    @Test
    public void testMuliplicationWithRecursion(){
        int n = 5;
        Matrix A = DenseMatrix.rand(n,n, new Random());
        Matrix B = DenseMatrix.rand(n,n, new Random());
        Matrix C = A.multiply((DenseMatrix) Matrices.dense(B.numRows(),B.numCols(), B.toArray()));
        breeze.linalg.Matrix<Object> strassenC = new StrassenMatrixMultiplier().strassenMultiply(A.asBreeze(), B.asBreeze(), 0);
        Assert.assertTrue(isEqual(C,strassenC));
    }


    private boolean isEqual(Matrix a, breeze.linalg.Matrix<Object> b){
        for (int i = 0; i < a.numRows(); i++) {
            for (int j = 0; j < a.numCols(); j++) {
                Double diff=a.apply(i,j)-(Double) b.apply(i,j);
                if(Math.abs(diff) >= 1e-13){
                    return false;
                }

            }
        }
        return true;
    }
}
