package edu.buffalo.gpchange;

import java.io.Serializable;

/**
 * Gaussian process monitor class.
 * <p>
 * Container class for various entities related to Gaussian process change detection.
 * 
 * @author Varun Chandola
 */


public class GPMonitor implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3600516300493970783L;
	
	/** Predicted means
	 */
	private double [][] Yhat;
	/** Predicted variances
	 */
	private double [] Vhat;
	/** Corrections for outliers
	 */
	private double [][] Ycorr;
	/** Z-scores
	 */
	private double [][] Z;

	/** Spacial Smoothed Z-scores
	 */
	private double [][][] SSZ;	
	/** P-values
	 */
	private double [][] P;
	/** Length of training portion
	 */
	private int n;
	
	
	/** Default constructor
	 */
	public GPMonitor(){
		setYhat(null); 
		setVhat(null);
		setYcorr(null); 
		setZ(null); 
		setP(null); 
		setN(-1);
	}
	
	/** Construct a GPMonitor with set of time series and length of training portion
	 * @param Y
	 * @param n
	 */
	public GPMonitor(double [][] Y, int n){
		setYhat(null); 
		setVhat(null);
		setYcorr(null); 
		setZ(null); 
		setP(null); 
		setN(n);
	}

	/** Set Yhat
	 * @param yhat Predictive means
	 */
	public void setYhat(double [][] yhat) {
		Yhat = yhat;
	}

	/** Get Yhat
	 * @return Yhat Predictive means
	 */
	public double [][] getYhat() {
		return Yhat;
	}

	/** Set Vhat 
	 * @param vhat Predicted variances
	 */
	public void setVhat(double [] vhat) {
		Vhat = vhat;
	}

	/** Get Vhat
	 * @return Vhat Predictive variances
	 */
	public double [] getVhat() {
		return Vhat;
	}

	/** Set Ycorr
	 * @param ycorr Corrected observations
	 */
	public void setYcorr(double [][] ycorr) {
		Ycorr = ycorr;
	}

	/** Get Ycorr
	 * @return Ycorr Corrected observations 
	 */
	public double [][] getYcorr() {
		return Ycorr;
	}

	/** Set Z
	 * @param z Z-scores
	 */
	public void setZ(double [][] z) {
		Z = z;
	}

	/** Get Z
	 * @return Z Z-scores 
	 */
	public double [][] getZ() {
		return Z;
	}

	/** Set P
	 * @param p P-values using wo-sided z-test
	 */
	public void setP(double [][] p) {
		P = p;
	}

	/** Get P
	 * @return P P-values using wo-sided z-test
	 */
	public double [][] getP() {
		return P;
	}

	/** Set n
	 * @param n Length of training portion
	 */
	public void setN(int n) {
		this.n = n;
	}

	/** Get n
	 * @return n Length of training portion 
	 */
	public int getN() {
		return n;
	}

	public void setSpacialSmoothedZ(double[][][] ssz) {
		this.SSZ = ssz;
		
	}
	
	public double[][][] getSpacialSmoothedZ() {
		return this.SSZ;
		
	}
	
}
