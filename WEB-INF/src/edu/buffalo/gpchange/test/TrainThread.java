/**
 * 
 */
package edu.buffalo.gpchange.test;


import edu.buffalo.gpchange.GPChange;

/** TrainThread class represents a single thread that can train
 * hyperparameters of a GP using a set of time series.
 * 
 * @author Varun Chandola
 *
 */
public class TrainThread implements Runnable{

	private double [][] X;
	private int dX;
	private double [][] Y;
	private int length;
	private int red;
	private GPChange gpc;
	Thread t;
	
	
	/** Constructor for TrainThread. Initializes the fields of the objects with values passed as arguments and starts the thread.
	 * @param X Input multivariate data
	 * @param dX Number of attributes in X
	 * @param Y Set of univariate observation data vectors, each data vector arranged as a column
	 * @param length Length of the run for gradient descent
	 * @param red Reduction in function value after each line search
	 * @param gpc GPChange object required for minimization
	 */
	public TrainThread(double [][]X, int dX, double [][] Y, int length, int red, GPChange gpc){		
		this.X = X;
		this.dX = dX;
		this.Y = Y;
		this.length = length;
		this.red = red;
		this.gpc = gpc;
		t = new Thread(this,"TrainThread");
	}

	public synchronized void run() {
		this.getGpc().minimize(this.X, this.dX, this.Y, this.length, this.red, 3, -1);
	}

	/** Get gpc
	 * @return GPChange object
	 */
	public GPChange getGpc() {
		return gpc;
	}
	

}
