package edu.buffalo.gpchange.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Vector;

import Jama.Matrix;
import edu.buffalo.gpchange.CovSEEPNoiseiso;
import edu.buffalo.gpchange.Toeplitz;

public class CheckCovFunction {

	public static void main(String[] args) {
		double[] loghypers = { 3.5252414329, 4.8970101024, 0.9953762458, 1.0653494296 };
		CovSEEPNoiseiso cse = new CovSEEPNoiseiso(loghypers, loghypers.length);
		
		int T = 36500;
		int omega = 365;
		int n = 36499;
		double[][] X = new double[T][1];
		for (int i = 0; i < T; i++)
			X[i][0] = (double) (i + 1) / omega;
		int dX = 1;

		// init
		Vector<double[]> preVal_k1 = new Vector<double[]>();
		double[] k = cse.covVec(X, n, dX);
		Toeplitz tz = new Toeplitz(k);
		double[][] x = new double[1][dX];

		for (int j = 0; j < dX; j++)
			x[0][j] = X[n][j];
		double[] k1 = (new Matrix(cse.covMatCross(X, n, dX, x, 1, dX))).getColumnPackedCopy();

		preVal_k1 = tz.solve(k1);

		File file = new File("/home/dtran/CovFunction.csv");
		try{
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file));
			writer.write("delta_t,k,zt_k\n");
			for (int i = 0; i < k1.length; i++) {
				writer.write((X[n][0] - X[i][0]) + "," + k1[i] + "," + preVal_k1.get(0)[i] + "\n");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
