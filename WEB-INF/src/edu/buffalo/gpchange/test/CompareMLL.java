package edu.buffalo.gpchange.test;

import java.util.Random;

import edu.buffalo.gpchange.CovSEEPNoiseiso;
import edu.buffalo.gpchange.GPChange;

public class CompareMLL {

	public static void main(String[] args) {
		Random rand = new Random();
		int datalength = 730;
		int numSeries = 1;
		int omega = 365;
		
		double [][] data = new double[datalength][numSeries];
		
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				data[i][j] = rand.nextDouble()*10;
			}
		} 

		int T = data.length;
		double[][] X = new double[T][1];
		for (int i = 0; i < T; i++)
			X[i][0] = (double) (i + 1) / omega;
		int dX = 1;

		double[] loghypers = { (double) Math.log(rand.nextDouble()), (double) Math.log(rand.nextDouble()), (double) Math.log(rand.nextDouble()),
				(double) Math.log(rand.nextDouble()) };
        CovSEEPNoiseiso cse = new CovSEEPNoiseiso(loghypers, loghypers.length);

		GPChange gpc = new GPChange(cse);
		
		double[] mll = gpc.computeMLL(X, dX, data);
		double[] mllfast = gpc.computeMLLFast(X, dX, data);
		
		double error = 0;
		for (int i = 0; i < mll.length; i++) {
			System.out.println(mll[i] + ", " + mllfast[i]);
			error += Math.abs(mllfast[i]/mll[i]-1);
		}
		error /= mll.length;
		System.out.println(error);		

	}

}
