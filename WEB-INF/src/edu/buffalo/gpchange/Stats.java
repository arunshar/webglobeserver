package edu.buffalo.gpchange;

import javax.management.RuntimeErrorException;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.OutOfRangeException;
/**
 * Miscellaneous statistical methods.
 * 
 * @author Varun Chandola
 */

public class Stats implements java.io.Serializable{
	
	private static final long serialVersionUID = 1L;

	/* ------------------------
	   Public Methods
	 * ------------------------ */
	/** One-sample Z-test (two-sided) 
	 * @param x observation
	 * @param m mean
	 * @param sigma standard deviation
	 * @return p-value for the two-sided z-test
	 */
	public static double ztest(double x, double m, double sigma){
		double z = Math.abs((x - m)/sigma);
		NormalDistribution nd1 = new NormalDistribution();				
		double p = -1;
		try {
			p = nd1.cumulativeProbability(-1*z);
		} catch (OutOfRangeException e) {
			System.err.print("Could not compute norm inverse.");
			e.printStackTrace();
		}
		return 2*p;
	}


	/** Normalize a vector of observations using the sample mean and standard deviation
	 * @param x observation vector
	 * @return normalized vector
	 * @throws RuntimeException
	 */
	public static double[] normalize(double[] x){
		if(x.length == 0) throw new RuntimeErrorException(null,"Divide by zero.");
		//compute mean and standard deviation
		double mean = 0;
		double sd = 0;
		for(int i = 0; i < x.length; i++)
			mean += x[i];
		mean = mean/x.length;
		for(int i = 0; i < x.length; i++)
			sd += Math.pow(x[i] - mean,2);
		sd = Math.sqrt(sd/(x.length-1));		
		return Stats.normalize(x,mean,sd);		
	}

	/** Normalize a vector of observations using the specified mean and standard deviation
	 * @param x observation vector
	 * @param mean population mean
	 * @param sd population standard deviation
	 * @return normalized vector
	 * @throws RuntimeException
	 */
	public static double[] normalize(double[] x,double mean, double sd){
		//compute mean and standard deviation
//		if(sd == 0) throw new RuntimeErrorException(null,"Divide by zero.");
		double[] xnorm = new double[x.length];
		for(int i = 0; i < x.length; i++)
			xnorm[i] = (x[i] - mean)/sd;
		return xnorm;
	}
}
