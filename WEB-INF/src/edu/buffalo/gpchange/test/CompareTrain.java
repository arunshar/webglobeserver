package edu.buffalo.gpchange.test;

import edu.buffalo.gpchange.CovSEEPNoiseiso;
import edu.buffalo.gpchange.GPChange;

import java.util.Random;

public class CompareTrain {

	public static void main(String[] args) {
		Random rand = new Random();
		int omega = 365;
		double [][] data = new double[10*omega][1];
		
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				data[i][j] = rand.nextDouble()*10;
			}
		}
		
		double[] loghypersfast = { (double) Math.log(1), (double) Math.log(1), (double) Math.log(1),
				(double) Math.log(1) };
		CovSEEPNoiseiso cse = new CovSEEPNoiseiso(loghypersfast, loghypersfast.length);

		GPChange gpc = new GPChange(cse);


		long startTime = System.currentTimeMillis();
		System.out.println("Training Fast..." + startTime);
		gpc.train(data, 20, 1, 3, -1, omega);
		System.out.println((System.currentTimeMillis()-startTime)*1.0/60000);
		loghypersfast = gpc.getCovFunc().getLogHypers();
 
		double[] loghypersnormal =  { (double) Math.log(1), (double) Math.log(1), (double) Math.log(1),
				(double) Math.log(1) };  
		gpc.getCovFunc().setLogHypers(loghypersnormal);
		startTime = System.currentTimeMillis();
		System.out.println("Training..." + startTime);
		gpc.train(data, 20, 1, 1, -1, omega);
		System.out.println((System.currentTimeMillis()-startTime)*1.0/60000);
		loghypersnormal = gpc.getCovFunc().getLogHypers();
				
		System.out.println("Done...");
		
		double error = 0;
		for (int i = 0; i < loghypersnormal.length; i++) {
			System.out.println(loghypersnormal[i] + ", " + loghypersfast[i]);
			error += Math.abs(loghypersnormal[i] - loghypersfast[i]);
		}
		error /= loghypersnormal.length;
		System.out.println(error);		

	}

}
