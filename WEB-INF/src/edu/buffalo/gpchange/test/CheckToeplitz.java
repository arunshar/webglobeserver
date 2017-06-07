package edu.buffalo.gpchange.test;

import java.util.Random;
import java.util.Vector;

import edu.buffalo.gpchange.Toeplitz;
import edu.buffalo.gpchange.VectorOps;

public class CheckToeplitz {

	public static void main(String[] args) {
		int length = 360;
		Random rand = new Random();
		double [] x = new double[length];
		double [] y = new double[length];
		for (int i = 0; i < y.length; i++) {
			x[i] = i + 1;
			y[i] = rand.nextDouble()*10;
		}

		Toeplitz tp = new Toeplitz(x);
		double [] z = tp.solve(y).get(0);

		double [] x1 = VectorOps.subvector(x, 0, 4, 11);
		double [] y1 = VectorOps.subvector(y, 0, 4, 11);
		Toeplitz tp1 = new Toeplitz(x1);
		Vector<double []> result = tp1.solve(y1);

		for (int i = 5; i < length; i++) {		
			x1 = VectorOps.subvector(x, 0, i, length);
			result = Toeplitz.solveInc(x1, y[i], result.get(0), result.get(1), result.get(2)[0]);
		}
		double [] z1 = result.get(0);
		double error = 0;
		for (int i = 0; i < z1.length; i++) {
			System.out.println(z[i] + " - " + z1[i]);
			error += Math.abs(z[i]-z1[i]);
		}
		error /= z1.length;
		
		System.out.println(error);
		
//		double [] x = {4, 3, 2, 1};
//		Toeplitz tp = new Toeplitz(x);
//		double [] y = {0, 0, 0, 1};
//		double [] cl = tp.solve(y).get(0);
//
//		for (int i = 0; i < cl.length; i++) {
//			System.out.println(cl[i]);
//		}
//		
//		double [] diags = tp.diagsums();
//		for (int i = 0; i < diags.length; i++) {
//			System.out.println(diags[i]);
//		}
		
//		double [] k2 = {4, 6, 2, 1};
//		System.out.println(tp.traceprod(k2));
		
	}

}