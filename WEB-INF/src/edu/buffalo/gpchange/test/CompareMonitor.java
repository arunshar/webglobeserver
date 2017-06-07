package edu.buffalo.gpchange.test;

import edu.buffalo.gpchange.CovSEEPNoiseiso;
import edu.buffalo.gpchange.GPChange;
import edu.buffalo.gpchange.GPMonitor;

import java.util.Random;


public class CompareMonitor {

	public static void main(String[] args) {
		Random rand = new Random(1);
		int omega = 30;
		int numSeries = 32*32;
		int trainLen = omega*2;
		int monitorLen = (int) (omega*1);

		double [][] data = new double[trainLen + monitorLen][numSeries];
		double [][] dataFast = new double[trainLen + monitorLen][numSeries];
		double [][] dataFastInc = new double[trainLen + monitorLen][numSeries];
		
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				data[i][j] = rand.nextDouble()*10;
				dataFast[i][j] = data[i][j];
				dataFastInc[i][j] = data[i][j];
			}
		}
		
		double[] loghypers = { (double) Math.log(1), (double) Math.log(1), (double) Math.log(1),
				(double) Math.log(1) };
		CovSEEPNoiseiso cse = new CovSEEPNoiseiso(loghypers, loghypers.length);

		GPChange gpc = new GPChange(cse);
		
		GPMonitor gpm;
		
		long startTime = System.currentTimeMillis();
		System.out.println("Monitoring Fast..." + startTime);
		gpm = gpc.monitorFast(dataFast, numSeries, omega, 0.0001, null, trainLen);
		double [][] monitorFast = gpm.getZ();
//		double [][] monitorFast = new double[data.length][1]; 
//		for (int i = 0; i < monitorFast.length; i++) {
//			monitorFast[i][0] = gpm.getVhat()[i];
//		}		
		System.out.println((System.currentTimeMillis()-startTime)*1.0/60000);
		startTime = System.currentTimeMillis();
		System.out.println("Monitoring Fast Inc..." + startTime);
		gpm = gpc.monitorFastInc1(dataFastInc, numSeries,omega, 0.0001, null, trainLen);
		double [][] monitorFastInc = gpm.getZ();
//		double [][] monitorFastInc = new double[data.length][1]; 
//		for (int i = 0; i < monitorFastInc.length; i++) {
//			monitorFastInc[i][0] = gpm.getVhat()[i];
//		}		
		System.out.println((System.currentTimeMillis()-startTime)*1.0/60000);
		startTime = System.currentTimeMillis();
//		System.out.println("Monitoring..." + startTime);
//		gpm = gpc.monitor(data, numSeries, omega, 0.0001, null, trainLen);
//		double [][] monitor = new double[data.length][1]; 
//		for (int i = 0; i < monitor.length; i++) {
//			monitor[i][0] = gpm.getVhat()[i];
//		}		
//		System.out.println(System.currentTimeMillis()-startTime);
		System.out.println("Done...");
		
		double error = 0;
//		for (int i = trainLen; i < monitor.length; i++) {
//			System.out.println(monitor[i][0] + " - " + monitorFast[i][0] + " - " + monitorFastInc[i][0]);
//			for (int j = 0; j < monitor[i].length; j++) {
//				error += Math.abs(monitorFast[i][j]/monitor[i][j] - 1);
//			}
//		}
//		error /= ((data.length-trainLen)*monitor[0].length);
//		System.out.println(error);
		
		error = 0;
		for (int i = trainLen; i < monitorFast.length; i++) {
			for (int j = 0; j < monitorFast[i].length; j++) {
				error += Math.abs(monitorFastInc[i][j]/monitorFast[i][j]-1);
			}
		}
		error /= ((data.length-trainLen)*monitorFast[0].length);
		System.out.println(error);

//		error = 0;
//		for (int i = trainLen; i < monitor.length; i++) {
//			for (int j = 0; j < monitor[i].length; j++) {
//				error += Math.abs(monitorFastInc[i][j]/monitor[i][j]-1);
//			}
//		}
//		error /= ((data.length-trainLen)*monitor[0].length);
//		System.out.println(error);
		
	}

}
