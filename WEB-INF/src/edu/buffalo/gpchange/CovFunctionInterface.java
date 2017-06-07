package edu.buffalo.gpchange;
/**
 * Covariance Function Interface.
 *	<P>
 *	Defines the methods implemented by a covariance function to be used
 *  within the Gaussian process framework.
 *
 * @author Varun Chandola
 */

public interface CovFunctionInterface {
	/* method declarations */
	
	/** Return number of hyper-parameters for the covariance function.
	 * @return number of hyper-parameters.
	 */
	public int retNumParams();
	
	/** Compute element wise covariance matrix for a vector of observations, X.
	 * @param X vector of observations, each observation can be multivariate
	 * @param nX number of observations in X
	 * @param dX number of variables for each observation
	 * @return nX x nX covariance matrix
	 */
	public double[][] covMat(double[][] X, int nX, int dX);

	/** Compute partial derivative of the element wise covariance matrix for a vector of observations, X, with respect to log of a hyper-parameter.
	 * @param X vector of observations, each observation can be multivariate
	 * @param nX number of observations in X
	 * @param dX number of variables for each observation
	 * @param param index of the hyper-parameter for computing the partial derivative 
	 * @return nX x nX partial derivative of covariance matrix 
	 */
	public double[][] covMatDer(double[][] X, int nX, int dX,int param);

	/** Compute top row of the element wise covariance matrix for a vector of observations, X.
	 * @param X vector of observations, each observation can be multivariate
	 * @param nX number of observations in X
	 * @param dX number of variables for each observation
	 * @return top row of covariance matrix
	 */
	public double[] covVec(double[][] X, int nX, int dX);
	
	/** Compute top row of the partial derivative of the element wise covariance matrix for a vector of observations, X, with respect to log of a hyper-parameter.
	 * @param X vector of observations, each observation can be multivariate
	 * @param nX number of observations in X
	 * @param dX number of variables for each observation
	 * @param param index of the hyper-parameter for computing the partial derivative 
	 * @return top row of the partial derivative of covariance matrix 
	 */
	public double[] covVecDer(double[][] X, int nX, int dX, int param);
	
	/** Compute element wise cross-covariance matrix between a vector of training observations, X and a vector test observations, Y.
	 * <p>
	 * Note: Number of variables in training and test observations must be same.
	 * @param X vector of training observations, each observation can be multivariate
	 * @param nX number of observations in X
	 * @param dX number of variables for each observation
	 * @param Y vector of test observations, each observation can be multivariate
	 * @param nY number of observations in Y
	 * @param dY number of variables for each observation
	 * @return nX x nY cross-covariance matrix
	 */
    public double[][] covMatCross(double[][] X, int nX, int dX, double [][]Y, int nY, int dY);

	/** Compute partial derivative of element wise cross-covariance matrix between a vector of training observations, X and a vector test observations, Y, with respect to log of a hyper-parameter.
	 * <p>
	 * Note: Number of variables in training and test observations must be same.
	 * @param X vector of training observations, each observation can be multivariate
	 * @param nX number of observations in X
	 * @param dX number of variables for each observation
	 * @param Y vector of test observations, each observation can be multivariate
	 * @param nY number of observations in Y
	 * @param dY number of variables for each observation
	 * @return nX x nY cross-covariance matrix
	 * @param param index of the hyper-parameter for computing the partial derivative 
	 */
    public double[][] covMatCrossDer(double[][] X, int nX, int dX, double [][]Y, int nY, int dY, int param);

    /** Compute self covariance for a vector of observations, X.
	 * @param X vector of observations, each observation can be multivariate
	 * @param nX number of observations in X
	 * @param dX number of variables for each observation
	 * @return nX vector of self covariances
	 */
	public double[]   covMatSelfTest(double[][] X, int nX, int dX);

    /** Compute partial derivative of self covariance for a vector of observations, X, with respect to log of a hyper-parameter.
	 * @param X vector of observations, each observation can be multivariate
	 * @param nX number of observations in X
	 * @param dX number of variables for each observation
	 * @param param index of the hyper-parameter for computing the partial derivative 
	 * @return nX vector of self covariances
	 */
	public double[]   covMatSelfTestDer(double[][] X, int nX, int dX, int param);
}
