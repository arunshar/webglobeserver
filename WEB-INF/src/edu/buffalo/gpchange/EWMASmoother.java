package edu.buffalo.gpchange;

import java.io.Serializable;

import Jama.Matrix;
/**
 * Class for performing Exponentially Weighted Moving Average (EWMA) based smoothing.
 * <p>
 * Uses two sets of thresholds to classify an observation in a time series into one of three categories
 * <ul>
 * <li> No change. </li>
 * <li> Possible change. </li>
 * <li> Definite change. </li>
 * </ul>
 * 
 * @author Varun Chandola
 */

public class EWMASmoother implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7604900869550579510L;
	
	/** 
	 * Indication of no change (Alarms[i][j] = 0), possible change (Alarms[i][j] = 1), and 
	 * definite change (Alarms[i][j] = 2) for jth location of ith time series.
	 */
	private int[][] Alarms;
	/**
	 * Scores assigned using the lower settings for identifying possible changes.
	 */
	private double[][] ScoresLow;
	/**
	 * Scores assigned using the higher settings for identifying definite changes.
	 */
	private double[][] ScoresHigh;
	
	/**
	 * Default constructor.
	 */
	public EWMASmoother(){
		this.setAlarms(null);
		this.setScoresHigh(null);
		this.setScoresLow(null);
	}

	/** Construct an EWMAsmoother using given alarms and scores.
	 * @param alarms
	 * @param scoresLow
	 * @param scoresHigh
	 */
	public EWMASmoother(int[][] alarms,double[][] scoresLow,double[][] scoresHigh){
		this.setAlarms(alarms);
		this.setScoresHigh(scoresHigh);
		this.setScoresLow(scoresLow);		
	}
	
	
	/**
	 * @param alarms Alarms to set
	 */
	public void setAlarms(int[][] alarms) {
		Alarms = alarms;
	}
	/**
	 * @return Alarms
	 */
	public int[][] getAlarms() {
		return Alarms;
	}
	/**
	 * @param scoresLow scoresLow to set
	 */
	public void setScoresLow(double[][] scoresLow) {
		ScoresLow = scoresLow;
	}
	/**
	 * @return scoresLow
	 */
	public double[][] getScoresLow() {
		return ScoresLow;
	}
	/**
	 * @param scoresHigh scoresHigh to set
	 */
	public void setScoresHigh(double[][] scoresHigh) {
		ScoresHigh = scoresHigh;
	}
	/**
	 * @return scoresHigh
	 */
	public double[][] getScoresHigh() {
		return ScoresHigh;
	}
	
	/** Perform EWMA smoothing on the given set of Z-scores.
	 * <p>
	 * Thresholds are calculated as:
	 * threshlow  = Mlow * sqrt(lambdalow/(2-lambdalow));
	 * threshhigh = Mhigh * sqrt(lambdahigh/(2-lambdahigh));
	 * 
	 * @param Z Set of Z-scores for multiple time series, one time series in each column of Z.
	 * @param lambdahigh High threshold parameter (should be between 0 and 1).
	 * @param lambdalow Low threshold parameter (should be between 0 and 1).
	 * @param Mhigh Scalar high threshold parameter.
	 * @param Mlow Scalar low threshold parameter.
	 * @param n Length of training portion.
	 */
	public void smooth(double[][] Z, double lambdahigh, double lambdalow, double Mhigh, double Mlow,int n){
		Matrix slow = new Matrix(Z.length,Z[0].length);
		Matrix shigh = new Matrix(Z.length,Z[0].length);
		int[][] als = new int[Z.length][Z[0].length];
		for(int t = 0; t < Z.length;t++)
			for(int j = 0; j < Z[0].length;j++)
				als[t][j] = 0;
		
		double threshlow = Mlow*Math.sqrt(lambdalow/(2- lambdalow));
		double threshhigh = Mhigh*Math.sqrt(lambdahigh/(2- lambdahigh));
		for(int t = n+1; t <= Z.length;t++){
			for(int j = 0; j < Z[0].length; j++){
				double plow = 0, phigh = 0;
				if(t > 1){
					plow = slow.get(t-2,j);
					phigh = shigh.get(t-2,j);
				}
				slow.set(t-1, j, lambdalow*Z[t-1][j] + (1-lambdalow)*plow);
				shigh.set(t-1, j, lambdahigh*Z[t-1][j] + (1-lambdahigh)*phigh);
				if(Math.abs(slow.get(t-1, j)) > threshlow) als[t-1][j] = 1;
				if(Math.abs(shigh.get(t-1, j)) > threshhigh) als[t-1][j] = 2;
			}
		}
		this.setScoresHigh(shigh.getArray());
		this.setScoresLow(slow.getArray());
		this.setAlarms(als);
	}
}
