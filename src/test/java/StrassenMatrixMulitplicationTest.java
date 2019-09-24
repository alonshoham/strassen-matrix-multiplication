import com.gigaspaces.strassen.Utils;
import org.apache.spark.mllib.linalg.DenseMatrix;
import org.apache.spark.mllib.linalg.Matrices;
import org.apache.spark.mllib.linalg.Matrix;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class StrassenMatrixMulitplicationTest {
    @Test
    public void testBasicMuliplication(){
        Matrix A = Matrices.rand(100,100, new Random());
        Matrix B = Matrices.rand(100,100, new Random());

        Matrix C = A.multiply((DenseMatrix) Matrices.dense(B.numRows(),B.numCols(), B.toArray()));
        Matrix strassenC = Utils.strassenMultiply(A,B);

        Assert.assertTrue(isEqual(C,strassenC));

    }


    private boolean isEqual(Matrix a, Matrix b){
        for (int i = 0; i < a.numRows(); i++) {
            for (int j = 0; j < a.numCols(); j++) {
                Double diff=a.apply(i,j)-b.apply(i,j);
                if(Math.abs(diff) >= 1e-13){
                    return false;
                }

            }
        }
        return true;
    }
}
