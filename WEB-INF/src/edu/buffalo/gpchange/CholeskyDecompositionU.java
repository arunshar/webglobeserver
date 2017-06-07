package edu.buffalo.gpchange;
import Jama.CholeskyDecomposition;
import Jama.Matrix;
/**
 * Cholesky Decomposition Update.
 *	<P>
 *	For a symmetric and positive-definite matrix A, update its cholesky
 *	decomposition matrix L (upper triangle) when a vector x is appended
 *	to A (row/column). When A is (n by n), x should be (n+1) dim.
 *
 * @author Varun Chandola
 */

public class CholeskyDecompositionU implements java.io.Serializable {

	/* ------------------------
	   Class variables
	 * ------------------------ */

	private static final long serialVersionUID = 1L;

	/** Size of square matrix A
	 * @serial is size of A.
	 */
	private int n;
	
	/** Cholesky decomposition of A = L*L'
	 * @serial is current Cholesky decomposition (lower triangular matrix)
	 */
	private double[][] L;

	/* ------------------------
	   Constructor
	 * ------------------------ */

	/** Constructs an object using an existing Cholesky Decomposition
	 * @param cd	Existing Cholesky decomposition
	 */
	public CholeskyDecompositionU(CholeskyDecomposition cd){
		L = cd.getL().getArray();
		n = L.length;
	}
	
	/** Returns the current Cholesky decomposition 
	 * @return	Current Cholesky decomposition.
	 */
	public Matrix getL(){
		return new Matrix(L,n,n);
	}
	
	/* ------------------------
	   Public Methods
	 * ------------------------ */
	
	/** Description:
		For a symmetric and positive-definite matrix A, update its Cholesky
		decomposition matrix L (upper triangle) when a vector x is appended
		to A (row/column). When A is (n by n), x should be (n+1) dim.
		*/
	
	/** Update Cholesky decomposition
	 * @param 		x - row and column to be added to original A
	 * @exception	IllegalArgumentException	Vector x is not of length n+1
	 */
	public void update(double[] x){
		if(x.length != n+1)
			throw new IllegalArgumentException("Length of input vector must be n+1.");
		Matrix y = ((new Matrix(x,1)).transpose()).getMatrix(0,n-1,new int[]{0});
		Matrix Lmat = new Matrix(L);
		Matrix LmatS = Lmat.solve(y);
		double c = Math.sqrt(x[n] - LmatS.transpose().times(LmatS).get(0, 0));
		//update L
		L = Matrix.identity(n+1, n+1).getArray();
		for(int i = 0; i < n; i++)
			for(int j = 0; j < n; j++)
				L[i][j] = Lmat.get(i,j);
		for(int i = 0; i < n; i++)
			L[n][i] = LmatS.get(i,0);
		L[n][n] = c;
		//update n
		n = n + 1;
	}
	
    /** Solve for Ax = B
     * @param B	A matrix with as many rows as A and any number of columns
     * @return Matrix B such that Ax = L'Lx = B
     * @exception	IllegalArgumentException	Dimensions of Matrix A and B do not match 
     */
    public Matrix solve (Matrix B) {
	  if (B.getRowDimension() != n) {
		  throw new IllegalArgumentException("Matrix row dimensions must agree.");
	  }
      // Copy right hand side.
      double[][] X = B.getArrayCopy();
      int nx = B.getColumnDimension();
      // Solve L*Y = B;
      for (int k = 0; k < n; k++) {
        for (int j = 0; j < nx; j++) {
           for (int i = 0; i < k ; i++) {
               X[k][j] -= X[i][j]*L[k][i];
           }
           X[k][j] /= L[k][k];
        }
      }
	
      // Solve L'*X = Y;
      for (int k = n-1; k >= 0; k--) {
        for (int j = 0; j < nx; j++) {
           for (int i = k+1; i < n ; i++) {
               X[k][j] -= X[i][j]*L[i][k];
           }
           X[k][j] /= L[k][k];
        }
      }
      return new Matrix(X,n,nx);
   }
}
