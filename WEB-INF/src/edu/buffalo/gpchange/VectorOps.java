package edu.buffalo.gpchange;
/**
 * Miscellaneous Vector Operations.
 * 
 * @author Varun Chandola
 */

public class VectorOps implements java.io.Serializable{
	
	private static final long serialVersionUID = 1L;

	/* ------------------------
	   Public Methods
	 * ------------------------ */
	
	/** Computes the squared Euclidean distance between vectors A and B
	 * @param A first vector
	 * @param B second vector
	 * @param len length of A and B
	 * @return squared Euclidean distance
	 */	
	public static double EuclideanDistance(double [] A, double [] B, int len){
		double dist = 0;
		for(int i = 0; i < len; i++){
			dist += Math.pow(A[i] - B[i],2);
		}
		return dist;
	}

	/** Computes the product of vector A with a scalar s
	 * @param A input vector
	 * @param s scalar to be multiplied
	 * @param len length of vector A
	 * @return Resulting vector = s*A
	 */	
	public static double[] scalarProd(double[] A, double s, int len) {
		double [] B = new double[len];
		for(int i = 0; i < len; i++) B[i] = s*A[i];
		return B;
	}
	
	/** Find the subvector of vector A with specified bounds
	 * 
	 * @param A input vector
	 * @param start start index for the subvector
	 * @param end end index for the subvector 
	 * @param len length of vector A
	 * @return Resulting sub-vector or null if index exceeds bound
	 */
	public static double[] subvector(double[] A, int start, int end, int len){
		if((start >= len) || (end >= len) || (start < 0) || (end < 0) || (start > end))
			return null;
		double [] B = new double[end-start+1];
        System.arraycopy(A, start, B, 0, B.length);
		return B;
	}

	/** Reverse a vector
	 * 
	 * @param A input vector
	 * @param len length of vector A
	 * @return Resulting reversed vector
	 */
	public static double[] reverse(double[] A, int len){
		double [] B = new double[len];
		for(int i = 0; i < len; i++) B[i] = A[len-i-1];
		return B;
	}

	/** Compute dot product of two vectors
	 * 
	 * @param A first input vector
	 * @param B second input vector
	 * @return scalar dot product
	 * @throws IllegalArgumentException If two vectors are not of same length
	 */
	public static double dotproduct(double[] A, double [] B){
		if(A.length != B.length)
			throw new IllegalArgumentException("Vectors A and B should be of equal length.");
		double dp = 0;
		for(int i = 0; i < A.length; i++) dp += A[i]*B[i];
		return dp;
	}

	/** Compute dot product of a vector with reverse of another vector
	 * 
	 * @param A first input vector
	 * @param B second input vector
	 * @return scalar dot product of A and reverse(B)
	 * @throws IllegalArgumentException If two vectors are not of same length
	 */
	public static double dotproductr(double[] A, double [] B){
		if(A.length != B.length)
			throw new IllegalArgumentException("Vectors A and B should be of equal length.");
		double dp = 0;
		for(int i = 0; i < A.length; i++) dp += A[i]*B[B.length-i-1];
		return dp;
	}
}
