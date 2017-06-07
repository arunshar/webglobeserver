package edu.buffalo.gpchange;

/** 
 * Base class for covariance functions.
 *
 * @author Varun Chandola
 */

public abstract class CovFunction implements CovFunctionInterface{
	//private members
	/**
	 * Number of hyper-parameters
	 */
	private int numParams;
	/**
	 * Vector of log of hyper-parameters
	 */
	private double[] loghypers;
	
	//public members
	/** Construct an empty covariance function
	 */
	public CovFunction(){
	}
	
	/** Construct a covariance function using a set of log of hyperparameters 
	 * @param loghypers log of hyperparameters associated with the function
	 * @param numParams number of hyperparameters
	 */
	public CovFunction(double[] loghypers,int numParams){
		setLogHypers(loghypers);
		setNumParams(numParams);
	}
	
	/** Get numParams value
	 * @return numParams number of hyper-parameters
	 */
	public int getNumParams(){
		return numParams;
	}

	/** Set numParams value
	 * @param numParams number of hyper-parameters
	 */
	public void setNumParams(int numParams){
		this.numParams = numParams;
	}
	
	/** Get loghypers value
	 * @return loghypers Vector of log of hyper-parameters
	 */
	public double[] getLogHypers(){
		return loghypers;
	}
	
	/** Set loghypers value
	 * @param loghypers Vector of log of hyper-parameters
	 */
	public void setLogHypers(double[] loghypers){
		this.loghypers = loghypers;
		this.numParams = loghypers.length;
	}
	
	public int retNumParams(){
		return -1;
	}	
	public double[][] covMat(double[][] X, int nX, int dX){
		return null;
	}
	public double[][] covMatDer(double[][] X, int nX, int dX,int param){
		return null;
	}
	public double[] covVec(double[][] X, int nX, int dX){
		return null;
	}
	public double[] covVecDer(double[][] X, int nX, int dX, int param){
		return null;
	}
	public double[][] covMatCross(double[][] X, int nX, int dX, double [][]Y, int nY, int dY){
		return null;
	}
	public double[][] covMatCrossDer(double[][] X, int nX, int dX, double [][]Y, int nY, int dY, int param){
		return null;
	}
	public double[]   covMatSelfTest(double[][] X, int nX, int dX){
		return null;
	}
	public double[]   covMatSelfTestDer(double[][] X, int nX, int dX, int param){
		return null;
	}
	
}
