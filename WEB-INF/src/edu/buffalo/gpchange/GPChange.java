package edu.buffalo.gpchange;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import javax.management.RuntimeErrorException;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.OutOfRangeException;

import Jama.CholeskyDecomposition;
import Jama.Matrix;

/**
 * Time Series monitoring and change detection using Gaussian Process.
 * 
 * <p>
 * Implements various routines for parameter estimation using conjugate gradient
 * descent, likelihood estimation, and time series monitoring. The output of the
 * monitoring routine can be used for change detection.
 * 
 * @author Varun Chandola
 */
public class GPChange {
	/**
	 * Associated covariance function
	 */
	private CovFunction covfunc;

	/**
	 * Default constructor
	 */
	public GPChange() {
		this.covfunc = null;
	}

	/**
	 * Construct GPChange with a particular covariance function
	 * 
	 * @param covfunc
	 *            Covariance function for GP
	 */
	public GPChange(CovFunction covfunc) {
		setCovFunction(covfunc);
	}

	/**
	 * Construct GPChange with a particular covariance function and
	 * hyper-parameters specified in a file
	 * 
	 * @param covfunc
	 *            Covariance function for GP
	 * @param loghyperfile
	 *            Location of file with log hyper-parameters in one line (space
	 *            separated)
	 */
	public GPChange(CovFunction covfunc, String loghyperfile) {
		setCovFunction(covfunc);
		BufferedReader bf = null;
		try {
			bf = new BufferedReader(new FileReader(loghyperfile));
			String s = null;
			try {
				s = bf.readLine();
				String[] st = s.split("\\s");
				double[] lh = new double[st.length];
				for (int j = 0; j < st.length; j++)
					lh[j] = Double.parseDouble(st[j]);
				this.covfunc.setLogHypers(lh);
			} catch (IOException e) {
				System.err.println(" Cannot read from the loghypers file.");
				e.printStackTrace();
			}
			bf.close();
		} catch (FileNotFoundException e1) {
			System.err.println(" Error while reading loghyper file.");
			e1.printStackTrace();
		} catch (IOException e) {
			System.err.println("Cannot close the opened loghyper file.");
			e.printStackTrace();
		}
	}

	/**
	 * Set covariance function for the GP
	 * 
	 * @param covfunc
	 *            Covariance function for GP
	 */
	public void setCovFunction(CovFunction covfunc) {
		this.covfunc = covfunc;
	}

	/**
	 * Get covariance function for the GP
	 * 
	 * @return covfunc Covariance function for GP
	 */
	public CovFunction getCovFunc() {
		return covfunc;
	}

	/**
	 * Compute Marginal Log Likelihood of a set of observation data Y and the
	 * input data X for a given set of hyper-parameters.
	 * <p>
	 * Number of observations in Y and inputs in X must match. Number of columns
	 * in Y is equal to the number of observation vectors. Also computes the
	 * partial derivatives of the marginal log likelihood with respect to each
	 * hyper-parameter.
	 * 
	 * @param X
	 *            Input multivariate data
	 * @param dX
	 *            Number of attributes in X
	 * @param Y
	 *            Set of univariate observation data vectors, each data vector
	 *            arranged as a column
	 * @return A vector of length 1+n where n is the number of hyper-parameters.
	 *         First value is the negative marginal log likelihood and next n
	 *         values are the derivatives
	 */
	public double[] computeMLL(double[][] X, int dX, double[][] Y) {
		double[] retvals = new double[1 + covfunc.getNumParams()];
		int T = X.length;
		if (Y.length != T)
			throw new IllegalArgumentException("Length of input and observed data must be same.");
		Matrix K = new Matrix(covfunc.covMat(X, T, dX));
		CholeskyDecomposition cd = K.chol();
		if (!cd.isSPD())
			throw new RuntimeErrorException(null, "Matrix is either not symmetric or positive definite.");
		Matrix mY = new Matrix(Y);
		int M = mY.getColumnDimension();
		Matrix mtY = mY.transpose();
		Matrix mYY = mY.times(mtY);
		Matrix Lalpha = cd.solve(mYY);
		double s = 0;
		Matrix Lchol = cd.getL();
		for (int i = 0; i < cd.getL().getRowDimension(); i++) {
			s = s + Math.log(Lchol.get(i, i));
		}
		retvals[0] = 0.5 * Lalpha.trace() + M * s + 0.5 * T * M * Math.log(2 * Math.PI);
		Matrix Kinv = cd.solve(Matrix.identity(T, T));
		Matrix W = Matrix.identity(T, T).times(M).minus(Lalpha);
		for (int i = 1; i <= covfunc.getNumParams(); i++) {
			Matrix dK = new Matrix(covfunc.covMatDer(X, T, dX, i));
			retvals[i] = 0.5 * ((Kinv.times(dK)).times(W)).trace();
		}
		return retvals;
	}

	public double[] computeMLLSingle(double[][] X, int dX, double[] Y) {
		double[] retvals = new double[1 + covfunc.getNumParams()];
		int T = X.length;
		if (Y.length != T)
			throw new IllegalArgumentException("Length of input and observed data must be same.");
		Matrix K = new Matrix(covfunc.covMat(X, T, dX));
		CholeskyDecomposition cd = K.chol();
		if (!cd.isSPD())
			throw new RuntimeErrorException(null, "Matrix is either not symmetric or positive definite.");
		Matrix mY = new Matrix(Y, Y.length);
		Matrix Lalpha = cd.solve(mY);
		double s = 0;
		Matrix Lchol = cd.getL();
		for (int i = 0; i < cd.getL().getRowDimension(); i++) {
			s = s + Math.log(Lchol.get(i, i));
		}
		retvals[0] = 0.5 * (mY.transpose().times(Lalpha)).get(0, 0) + s + 0.5 * T * Math.log(2 * Math.PI);
		Matrix Kinv = cd.solve(Matrix.identity(T, T));
		Matrix W = Kinv.minus(Lalpha.times(Lalpha.transpose()));
		for (int i = 1; i <= covfunc.getNumParams(); i++) {
			Matrix dK = new Matrix(covfunc.covMatDer(X, T, dX, i));
			retvals[i] = 0.5 * (W.times(dK)).trace();
		}
		return retvals;
	}

	public double[] computeMLLFast(double[][] X, int dX, Vector<double[]> vY) {
		double[] retvals = new double[1 + covfunc.getNumParams()];
		int T = X.length;

		double[] k = covfunc.covVec(X, T, dX);
		Toeplitz tz = new Toeplitz(k);
		retvals[0] = 0.5 * vY.size() * (tz.logdet() + T * Math.log(2 * Math.PI));
		Vector<double[]> dks = new Vector<double[]>(covfunc.getNumParams());
		Vector<Toeplitz> tps = new Vector<Toeplitz>(covfunc.getNumParams());
		for (int i = 1; i <= covfunc.getNumParams(); i++) {
			dks.add(covfunc.covVecDer(X, T, dX, i));
			retvals[i] = 0.5 * vY.size() * tz.traceprod(dks.get(i - 1));
			tps.add(new Toeplitz(dks.get(i - 1)));
		}

		for (int i = 0; i < vY.size(); i++) {
			double[] vyi = tz.solve(vY.get(i)).get(0);
			retvals[0] += 0.5 * VectorOps.dotproduct(vY.get(i), vyi);
			for (int j = 1; j <= covfunc.getNumParams(); j++) {
				retvals[j] -= 0.5 * VectorOps.dotproduct(tps.get(j - 1).vectorprod(vyi), vyi);
			}
		}
		return retvals;
	}

	public double[] computeMLLFast(double[][] X, int dX, double[][] vY) {
		double[] retvals = new double[1 + covfunc.getNumParams()];
		int T = X.length;

		double[] k = covfunc.covVec(X, T, dX);
		Toeplitz tz = new Toeplitz(k);
		retvals[0] = 0.5 * vY[0].length * (tz.logdet() + T * Math.log(2 * Math.PI));
		Vector<double[]> dks = new Vector<double[]>(covfunc.getNumParams());
		Vector<Toeplitz> tps = new Vector<Toeplitz>(covfunc.getNumParams());
		for (int i = 1; i <= covfunc.getNumParams(); i++) {
			dks.add(covfunc.covVecDer(X, T, dX, i));
			retvals[i] = 0.5 * vY[0].length * tz.traceprod(dks.get(i - 1));
			tps.add(new Toeplitz(dks.get(i - 1)));
		}

		double[] y = new double[T];
		for (int i = 0; i < vY[0].length; i++) {
			for (int j = 0; j < y.length; j++) {
				y[j] = vY[j][i];
			}
			double[] vyi = tz.solve(y).get(0);
			retvals[0] += 0.5 * VectorOps.dotproduct(y, vyi);
			for (int j = 1; j <= covfunc.getNumParams(); j++) {
				retvals[j] -= 0.5 * VectorOps.dotproduct(tps.get(j - 1).vectorprod(vyi), vyi);
			}
		}
		return retvals;
	}

	public double[] computeMLLFast1(double[][] X, int dX, double[][] Y) {

		Matrix mY = new Matrix(Y);
		int M = mY.getColumnDimension();
		double[] retvals = new double[1 + covfunc.getNumParams()];
		for (int j = 0; j < retvals.length; j++)
			retvals[j] = 0;
		// System.out.println("Number of rows = "+mY.getRowDimension()+" Number
		// of columns = "+mY.getColumnDimension());
		for (int i = 0; i < M; i++) {
			double[] retvalssingle = computeMLLFastSingle(X, dX,
					(mY.getMatrix(0, X.length - 1, i, i)).getColumnPackedCopy());
			for (int j = 0; j < retvals.length; j++)
				retvals[j] += retvalssingle[j];
		}
		return retvals;
	}

	public double[] computeMLLFastSingle(double[][] X, int dX, double[] Y) {
		double[] retvals = new double[1 + covfunc.getNumParams()];
		int T = X.length;

		double[] k = covfunc.covVec(X, T, dX);
		Toeplitz tz = new Toeplitz(k);
		retvals[0] = 0.5 * (tz.logdet() + T * Math.log(2 * Math.PI));
		double[] vyi = tz.solve(Y).get(0);
		retvals[0] += 0.5 * VectorOps.dotproduct(Y, vyi);
		for (int i = 1; i <= covfunc.getNumParams(); i++) {
			double[] dk = covfunc.covVecDer(X, T, dX, i);
			retvals[i] = 0.5 * tz.traceprod(dk);
			Toeplitz txk = new Toeplitz(dk);
			retvals[i] -= 0.5 * VectorOps.dotproduct(txk.vectorprod(vyi), vyi);
		}

		return retvals;
	}

	/**
	 * Compute Conditional Log Likelihood of a set of observation data Y and the
	 * input data X for a given set of hyper-parameters.
	 * <p>
	 * Number of observations in Y and inputs in X must match. Number of columns
	 * in Y is equal to the number of observation vectors. Also computes the
	 * partial derivatives of the conditional log likelihood with respect to
	 * each hyper-parameter. Uses initial portion of the observation data for
	 * training, specified by n.
	 * 
	 * @param X
	 *            Input multivariate data
	 * @param dX
	 *            Number of attributes in X
	 * @param Y
	 *            Set of univariate observation data vectors, each data vector
	 *            arranged as a column
	 * @param n
	 *            Training portion, should be between 1 and Y.length
	 * @return A vector of length 1+n where n is the number of hyper-parameters.
	 *         First value is the negative conditional log likelihood and next n
	 *         values are the derivatives
	 */
	public double[] computeCLL(double[][] X, int dX, double[][] Y, int n) {
		double[] retvals = new double[1 + covfunc.getNumParams()];
		for (int i = 0; i < retvals.length; i++)
			retvals[i] = 0;

		int T = X.length;
		if (Y.length != T)
			throw new IllegalArgumentException("Length of input and observed data must be same.");
		if ((n < 1) || (n >= T))
			throw new IllegalArgumentException(
					"Training portion should be longer than 1 and less than length of input time series.");

		Matrix K = new Matrix(covfunc.covMat(X, n, dX));
		CholeskyDecomposition cd = K.chol();
		if (!cd.isSPD())
			throw new RuntimeErrorException(null, "Matrix is either not symmetric or positive definite.");
		CholeskyDecompositionU cdu = new CholeskyDecompositionU(cd);

		for (int t = n + 1; t <= T; t++) {
			double[][] x = new double[1][dX];
			for (int j = 0; j < dX; j++)
				x[0][j] = X[t - 1][j];
			Matrix K1 = new Matrix(covfunc.covMatCross(X, t - 1, dX, x, 1, dX));
			Matrix K11 = new Matrix(covfunc.covMatSelfTest(x, 1, dX), 1);
			Matrix mY = (new Matrix(Y)).getMatrix(0, t - 2, new int[] { 0 });
			int M = mY.getColumnDimension();
			Matrix Lalpha = cdu.solve(mY);
			// obtain predictions
			Matrix Yhatt = K1.transpose().times(Lalpha);
			Matrix Lcholv = cdu.getL().solve(K1);
			double Vhatt = (K11.minus(Lcholv.transpose().times(Lcholv))).get(0, 0);
			// compute derivatives
			Matrix dYhatt = new Matrix(covfunc.getNumParams(), M);
			Matrix dVhatt = new Matrix(covfunc.getNumParams(), 1);
			Matrix Lbeta = cdu.solve(K1);
			int[] inds = new int[M];
			for (int i = 0; i < M; i++)
				inds[i] = i;
			for (int i = 0; i < covfunc.getNumParams(); i++) {
				Matrix dK1 = new Matrix(covfunc.covMatCrossDer(X, t - 1, dX, x, 1, dX, i + 1));
				Matrix dK11 = new Matrix(covfunc.covMatSelfTestDer(x, 1, dX, i + 1), 1);
				Matrix dK = new Matrix(covfunc.covMatDer(X, t - 1, dX, i + 1));
				dYhatt.setMatrix(i, i, inds, dK1.transpose().minus(Lbeta.transpose().times(dK)).times(Lalpha));
				dVhatt.set(i, 0, (dK11
						.minus((dK1.transpose().times(Lbeta).times(2)).minus(Lbeta.transpose().times(dK).times(Lbeta))))
								.get(0, 0));
			}
			Matrix currY = (new Matrix(Y)).getMatrix(t - 1, t - 1, inds);
			Matrix _int = currY.minus(Yhatt);
			retvals[0] += 0.5 * M * (Math.log(2 * Math.PI) + Math.log(Vhatt))
					+ (0.5 / Vhatt) * Math.pow(_int.norm2(), 2);
			for (int i = 0; i < covfunc.getNumParams(); i++) {
				double _v1 = 0.5 * M * dVhatt.get(i, 0) / Vhatt;
				double _v2 = 0.5 * Math.pow(_int.norm2(), 2) * dVhatt.get(i, 0) / Math.pow(Vhatt, 2);
				Matrix _m3 = _int.arrayTimes(dYhatt.getMatrix(i, i, inds));
				double _v4 = 0;
				for (int j = 0; j < M; j++)
					_v4 += _m3.get(0, j);
				_v4 = _v4 / Vhatt;
				retvals[i + 1] += _v1 - _v2 - _v4;
			}

			// prepare augmented cholesky decomposition for next iteration
			Matrix mnewcol = new Matrix(t, 1);
			mnewcol.setMatrix(0, t - 2, new int[] { 0 }, K1);
			mnewcol.setMatrix(t - 1, t - 1, new int[] { 0 }, K11);
			double[] newcol = mnewcol.getColumnPackedCopy();
			cdu.update(newcol);
		}
		return retvals;
	}

	/**
	 * Minimize the marginal or conditional log likelihood of given data as a
	 * function of the hyper-parameters using conjugate gradient descent.
	 * <p>
	 * The current values of the hyper-parameters are chosen as the starting
	 * point. The "length" gives the length of the run: if it is positive, it
	 * gives the maximum number of line searches, if negative its absolute gives
	 * the maximum allowed number of function evaluations. The "red" parameter
	 * indicates the reduction in function value to be expected in the first
	 * line-search.
	 * <p>
	 * The function returns when either its length is up, or if no further
	 * progress can be made (ie, we are at a (local) minimum, or so close that
	 * due to numerical problems, we cannot get any closer). The function sets
	 * the final solution as the updated log hyper-parameters for the covariance
	 * function.
	 * <p>
	 * The Polack-Ribiere flavour of conjugate gradients is used to compute
	 * search directions, and a line search using quadratic and cubic polynomial
	 * approximations and the Wolfe-Powell stopping criteria is used together
	 * with the slope ratio method for guessing initial step sizes. Additionally
	 * a bunch of checks are made to make sure that exploration is taking place
	 * and that extrapolation will not be unboundedly large.
	 * 
	 * @param X
	 *            Input multivariate data
	 * @param dX
	 *            Number of attributes in X
	 * @param Y
	 *            Set of univariate observation data vectors, each data vector
	 *            arranged as a column
	 * @param length
	 *            Length of the run for gradient descent
	 * @param red
	 *            Reduction in function value after each line search
	 * @param method
	 *            Function to compute log likelihood, 1 - Marginal Log
	 *            Likelihood, 2 - Conditional Log Likelihood
	 * @param n
	 *            Length of training portion for computing conditional log
	 *            likelihood
	 */
	public void minimize(double[][] X, int dX, double[][] Y, int length, int red, int method, int n) {
		int T = X.length;
		if (Y.length != T)
			throw new IllegalArgumentException("Length of input and observed data must be same.");
		if (((method == 2) && (n < 1)) || (n >= T))
			throw new IllegalArgumentException(
					"Training portion should be longer than 1 (for CLL) and less than length of input time series.");

		double[] l01 = covfunc.getLogHypers();
		Matrix l = new Matrix(l01, l01.length);
		// declarations here
		double d3;
		double x2, f2, d2, f3, x4 = 0, f4 = 0, d4 = 0, A, B;
		Matrix df3, L0 = null;
		double int1 = 0.1, ext = 3.0, ratio = 10, sig = 0.1;
		double rho = sig / 2;
		int max = 20;
		if (red == -1)
			red = 1;
		int i = 0, ls_failed = 0;
		double[] retvals;
		if (method == 1)
			retvals = computeMLL(X, dX, Y);
		else if (method == 2)
			retvals = computeCLL(X, dX, Y, n);
		else
			retvals = computeMLLFast(X, dX, Y);
		double f0 = retvals[0];
		Matrix df0 = (new Matrix(retvals, retvals.length)).getMatrix(1, retvals.length - 1, 0, 0);
		if (length < 0)
			i = i + 1;
		Matrix s = df0.times(-1);
		double d0 = ((s.transpose().times(s)).times(-1)).get(0, 0);
		double x3 = red / (1 - d0);
		while (i < Math.abs(length)) {
			if (length > 0)
				i = i + 1;
			L0 = l;
			double F0 = f0;
			Matrix dF0 = df0;
			int m;
			if (length > 0)
				m = max;
			else
				m = Math.min(max, -1 * (length + i));
			while (true) {
				x2 = 0;
				f2 = f0;
				d2 = d0;
				f3 = f0;
				df3 = df0;
				boolean success = false;
				while ((!success) && (m > 0)) {
					m--;
					if (length < 0)
						i = i + 1;
					covfunc.setLogHypers(l.plus(s.times(x3)).getColumnPackedCopy());
					if (method == 1)
						retvals = computeMLL(X, dX, Y);
					else if (method == 2)
						retvals = computeCLL(X, dX, Y, n);
					else
						retvals = computeMLLFast(X, dX, Y);
					f3 = retvals[0];
					df3 = (new Matrix(retvals, retvals.length)).getMatrix(1, retvals.length - 1, 0, 0);
					boolean s1 = false;
					for (int j = 0; j < retvals.length; j++) {
						if (Double.isNaN(retvals[j]) || Double.isInfinite(retvals[j]))
							s1 = true;
					}
					if (!s1)
						success = true;
					else
						x3 = (x2 + x3) / 2;
				}
				if (f3 < F0) {
					L0 = l.plus(s.times(x3));
					F0 = f3;
					dF0 = df3;
				}
				d3 = df3.transpose().times(s).get(0, 0);
				if ((d3 > sig * d0) || (f3 > f0 + x3 * rho * d0) || (m == 0))
					break;
				double x1 = x2, f1 = f2, d1 = d2;
				x2 = x3;
				f2 = f3;
				d2 = d3;
				A = 6 * (f1 - f2) + 3 * (d2 + d1) * (x2 - x1);
				B = 3 * (f2 - f1) - (2 * d1 + d2) * (x2 - x1);
				x3 = x1 - d1 * Math.pow(x2 - x1, 2) / (B + Math.sqrt(B * B - A * d1 * (x2 - x1)));
				if (Double.isNaN(x3) || Double.isInfinite(x3) || x3 < 0)
					x3 = x2 * ext;
				else if (x3 > x2 * ext)
					x3 = x2 * ext;
				else if (x3 < x2 + int1 * (x2 - x1))
					x3 = x2 + int1 * (x2 - x1);
			}
			while ((Math.abs(d3) > -sig * d0 || f3 > f0 + x3 * rho * d0) && m > 0) {
				if (d3 > 0 || f3 > f0 + x3 * rho * d0) {
					x4 = x3;
					f4 = f3;
					d4 = d3;
				} else {
					x2 = x3;
					f2 = f3;
					d2 = d3;
				}
				if (f4 > f0) {
					x3 = x2 - (0.5 * d2 * Math.pow(x4 - x2, 2) / (f4 - f2 - d2 * (x4 - x2)));
				} else {
					A = 6 * (f2 - f4) / (x4 - x2) + 3 * (d4 + d2);
					B = 3 * (f4 - f2) - (2 * d2 + d4) * (x4 - x2);
					x3 = x2 + (Math.sqrt(B * B - A * d2 * Math.pow(x4 - x2, 2)) - B) / A;
				}
				if (Double.isNaN(x3) || Double.isInfinite(x3)) {
					x3 = (x2 + x4) / 2;
				}
				x3 = Math.max(Math.min(x3, x4 - int1 * (x4 - x2)), x2 + int1 * (x4 - x2));
				covfunc.setLogHypers(l.plus(s.times(x3)).getColumnPackedCopy());
				if (method == 1)
					retvals = computeMLL(X, dX, Y);
				else if (method == 2)
					retvals = computeCLL(X, dX, Y, n);
				else
					retvals = computeMLLFast(X, dX, Y);
				f3 = retvals[0];
				df3 = (new Matrix(retvals, retvals.length)).getMatrix(1, retvals.length - 1, 0, 0);
				if (f3 < F0) {
					L0 = l.plus(s.times(x3));
					F0 = f3;
					dF0 = df3;
				}
				m--;
				if (length < 0)
					i = i + 1;
				d3 = df3.transpose().times(s).get(0, 0);
			}
			if (Math.abs(d3) < -sig * d0 && f3 < f0 + x3 * rho * d0) {
				l = l.plus(s.times(x3));
				f0 = f3;
				s = s.times((df3.transpose().times(df3).get(0, 0) - df0.transpose().times(df3).get(0, 0))
						/ (df0.transpose().times(df0).get(0, 0))).minus(df3);
				df0 = df3;
				d3 = d0;
				d0 = df0.transpose().times(s).get(0, 0);
				if (d0 > 0) {
					s = df0.times(-1);
					d0 = ((s.transpose().times(s)).times(-1)).get(0, 0);
				}
				x3 = x3 * Math.min(ratio, d3 / (d0 - Double.MIN_VALUE));
				ls_failed = 0;
			} else {
				l = L0;
				f0 = F0;
				df0 = dF0;
				if (ls_failed == 1 || i > Math.abs(length))
					break;
				s = df0.times(-1);
				d0 = ((s.transpose().times(s)).times(-1)).get(0, 0);
				x3 = 1 / (1 - d0);
				ls_failed = 1;
			}
		}
		covfunc.setLogHypers(l.getColumnPackedCopy());
	}

	/**
	 * Train the covariance function using the minimize function for a given set
	 * of univariate time series.
	 * 
	 * @param Y
	 *            Set of univariate time series, one in each column
	 * @param length
	 *            Length of the run for gradient descent
	 * @param red
	 *            Reduction in function value after each line search
	 * @param method
	 *            Function to compute log likelihood, 1 - Marginal Log
	 *            Likelihood, 2 - Conditional Log Likelihood, 3 - Marginal Log
	 *            Likelihood with Toeplitz operations
	 * @param n
	 *            Length of training portion for computing conditional log
	 *            likelihood
	 * @param omega
	 *            Length of cycle (or periodicity), used to compute the input X
	 * @see #minimize
	 */
	public void train(double[][] Y, int length, int red, int method, int n, int omega) {
		int T = Y.length;
		double[][] X = new double[T][1];
		for (int i = 0; i < T; i++)
			X[i][0] = (double) (i + 1) / omega;
		int dX = 1;
		minimize(X, dX, Y, length, red, method, n);
	}

	/**
	 * Monitor a set of time series using a Gaussian process.
	 * <p>
	 * Compute the predictive means and variances at each time step and also
	 * compute the P-values and Z-scores associated with predictions at each
	 * time step.
	 * 
	 * @param Y
	 *            Set of univariate time series, one in each column
	 * @param dY
	 *            Number of time series in Y
	 * @param omega
	 *            Length of cycle (or periodicity), used to compute the input X
	 * @param alpha
	 *            Threshold to identify outliers (between 0 and 1).
	 * @param missing
	 *            A matrix of same dimension as Y indicating if any observation
	 *            is missing, i.e., missing[i][j] == 1 => Y[i][j] is missing
	 * @param n
	 *            Length of training portion
	 * @return GPMonitor object containing the predicted means and variances,
	 *         corrections, Z-scores, and P-values.
	 */
	public GPMonitor monitor(double[][] Y, int dY, int omega, double alpha, double[][] missing, int n) {
		GPMonitor gpm = new GPMonitor(Y, n);

		int T = Y.length;
		double[][] X = new double[T][1];
		for (int i = 0; i < T; i++)
			X[i][0] = (double) (i + 1) / omega;
		int dX = 1;
		if ((n < 1) || (n >= T))
			throw new IllegalArgumentException(
					"Training portion should be longer than 1 and less than length of input time series.");
		int[] inds = new int[dY];
		for (int j = 0; j < dY; j++)
			inds[j] = j;
		// initialize
		Matrix mZ = new Matrix(T, dY);
		Matrix mYhat = new Matrix(T, dY);
		Matrix mP = new Matrix(T, dY);
		Matrix mVhat = new Matrix(T, 1);
		// end of initialization

		Matrix K = new Matrix(covfunc.covMat(X, n, dX));

		CholeskyDecomposition cd = K.chol();
		if (!cd.isSPD())
			throw new RuntimeErrorException(null, "Matrix is either not symmetric or positive definite.");
		CholeskyDecompositionU cdu = new CholeskyDecompositionU(cd);
		double eps = 0.001;
		for (int t = 0; t < n; t++) {
			mYhat.setMatrix(t, t, inds, (new Matrix(Y)).getMatrix(t, t, inds));
		}
		for (int t = n + 1; t <= T; t++) {
			double[][] x = new double[1][dX];
			for (int j = 0; j < dX; j++)
				x[0][j] = X[t - 1][j];
			Matrix K1 = new Matrix(covfunc.covMatCross(X, t - 1, dX, x, 1, dX));
			Matrix K11 = new Matrix(covfunc.covMatSelfTest(x, 1, dX), 1);
			Matrix mY = (new Matrix(Y)).getMatrix(0, t - 2, inds);
			Matrix Lalpha = cdu.solve(mY);
			// obtain predictions
			Matrix Yhatt = K1.transpose().times(Lalpha);
			Matrix Lcholv = cdu.getL().solve(K1);
			double Vhatt = (K11.minus(Lcholv.transpose().times(Lcholv))).get(0, 0);
			// set values
			mVhat.set(t - 1, 0, Vhatt);
			mYhat.setMatrix(t - 1, t - 1, inds, Yhatt);
			// check for missing values
			if (missing != null)
				for (int j = 0; j < dY; j++)
					if (missing[t - 1][j] == 1)
						Y[t - 1][j] = mYhat.get(t - 1, j) + eps;

			// compute P-values Z scores and fix Outliers
			for (int j = 0; j < dY; j++) {
				double pval = 0.5 * Stats.ztest(Y[t - 1][j], mYhat.get(t - 1, j), Math.sqrt(mVhat.get(t - 1, 0)));
				mP.set(t - 1, j, pval);
				NormalDistribution nd = new NormalDistribution(mYhat.get(t - 1, j), Math.sqrt(mVhat.get(t - 1, 0)));
				if (mP.get(t - 1, j) < alpha && Y[t - 1][j] > mYhat.get(t - 1, j)) {
					try {
						Y[t - 1][j] = nd.inverseCumulativeProbability(1 - alpha);
					} catch (OutOfRangeException e) {
						System.err.print("Could not compute norm inverse.");
						e.printStackTrace();
					}
				}
				if (mP.get(t - 1, j) < alpha && Y[t - 1][j] <= mYhat.get(t - 1, j)) {
					try {
						Y[t - 1][j] = nd.inverseCumulativeProbability(alpha);
					} catch (OutOfRangeException e) {
						System.err.print("Could not compute norm inverse.");
						e.printStackTrace();
					}
				}
				// recompute P-values
				pval = 0.5 * Stats.ztest(Y[t - 1][j], mYhat.get(t - 1, j), Math.sqrt(mVhat.get(t - 1, 0)));
				mP.set(t - 1, j, pval);
				NormalDistribution nd1 = new NormalDistribution();
				try {
					mZ.set(t - 1, j, nd1.inverseCumulativeProbability(pval));
				} catch (OutOfRangeException e) {
					System.err.print("Could not compute norm inverse.");
					e.printStackTrace();
				}
			}

			// prepare augmented cholesky decomposition for next iteration
			Matrix mnewcol = new Matrix(t, 1);
			mnewcol.setMatrix(0, t - 2, new int[] { 0 }, K1);
			mnewcol.setMatrix(t - 1, t - 1, new int[] { 0 }, K11);
			double[] newcol = mnewcol.getColumnPackedCopy();
			cdu.update(newcol);
		}
		gpm.setYcorr(Y);
		gpm.setYhat(mYhat.getArray());
		gpm.setVhat(mVhat.getColumnPackedCopy());
		gpm.setP(mP.getArray());
		gpm.setZ(mZ.getArray());

		return gpm;
	}

	/**
	 * Monitor a set of time series using a Gaussian process.
	 * <p>
	 * Compute the predictive means and variances at each time step and also
	 * compute the P-values and Z-scores associated with predictions at each
	 * time step. Use Toeplitz solve algorithm to compute K^(-1)y
	 * 
	 * @param Y
	 *            Set of univariate time series, one in each column
	 * @param dY
	 *            Number of time series in Y
	 * @param omega
	 *            Length of cycle (or periodicity), used to compute the input X
	 * @param alpha
	 *            Threshold to identify outliers (between 0 and 1).
	 * @param missing
	 *            A matrix of same dimension as Y indicating if any observation
	 *            is missing, i.e., missing[i][j] == 1 => Y[i][j] is missing
	 * @param n
	 *            Length of training portion
	 * @return GPMonitor object containing the predicted means and variances,
	 *         corrections, Z-scores, and P-values.
	 */
	public GPMonitor monitorFast(double[][] Y, int dY, int omega, double alpha, double[][] missing, int n) {
		GPMonitor gpm = new GPMonitor(Y, n);

		int T = Y.length;
		double[][] X = new double[T][1];
		for (int i = 0; i < T; i++)
			X[i][0] = (double) (i + 1) / omega;
		int dX = 1;
		if ((n < 1) || (n >= T))
			throw new IllegalArgumentException(
					"Training portion should be longer than 1 and less than length of input time series.");
		int[] inds = new int[dY];
		for (int j = 0; j < dY; j++)
			inds[j] = j;
		// initialize
		Matrix mZ = new Matrix(T, dY);
		Matrix mYhat = new Matrix(T, dY);
		Matrix mP = new Matrix(T, dY);
		Matrix mVhat = new Matrix(T, 1);
		// end of initialization

		double eps = 0.001;
		for (int t = 0; t < n; t++) {
			mYhat.setMatrix(t, t, inds, (new Matrix(Y)).getMatrix(t, t, inds));
		}
		double[] k = covfunc.covVec(X, n, dX);
		Toeplitz tz = new Toeplitz(k);

		for (int t = n + 1; t <= T; t++) {
			double[][] x = new double[1][dX];
			for (int j = 0; j < dX; j++)
				x[0][j] = X[t - 1][j];
			double[] k1 = (new Matrix(covfunc.covMatCross(X, t - 1, dX, x, 1, dX))).getColumnPackedCopy();
			double[] y = new double[t - 1];

			// obtain predictions
			Matrix Yhatt = new Matrix(1, dY);
			for (int i = 0; i < dY; i++) {
				for (int j = 0; j < t - 1; j++)
					y[j] = Y[j][i];
				Yhatt.set(0, i, VectorOps.dotproduct(k1, tz.solve(y).get(0)));
			}
			Matrix K11 = new Matrix(covfunc.covMatSelfTest(x, 1, dX), 1);
			double Vhatt = K11.get(0, 0) - VectorOps.dotproduct(k1, tz.solve(k1).get(0));

			// set values
			mVhat.set(t - 1, 0, Vhatt);
			mYhat.setMatrix(t - 1, t - 1, inds, Yhatt);
			// check for missing values
			if (missing != null)
				for (int j = 0; j < dY; j++)
					if (missing[t - 1][j] == 1)
						Y[t - 1][j] = mYhat.get(t - 1, j) + eps;

			// compute P-values Z scores and fix Outliers
			for (int j = 0; j < dY; j++) {
				double pval = 0.5 * Stats.ztest(Y[t - 1][j], mYhat.get(t - 1, j), Math.sqrt(mVhat.get(t - 1, 0)));
				mP.set(t - 1, j, pval);
				NormalDistribution nd = new NormalDistribution(mYhat.get(t - 1, j), Math.sqrt(mVhat.get(t - 1, 0)));
				if (mP.get(t - 1, j) < alpha && Y[t - 1][j] > mYhat.get(t - 1, j)) {
					try {
						Y[t - 1][j] = nd.inverseCumulativeProbability(1 - alpha);
					} catch (OutOfRangeException e) {
						System.err.print("Could not compute norm inverse.");
						e.printStackTrace();
					}
				}
				if (mP.get(t - 1, j) < alpha && Y[t - 1][j] <= mYhat.get(t - 1, j)) {
					try {
						Y[t - 1][j] = nd.inverseCumulativeProbability(alpha);
					} catch (OutOfRangeException e) {
						System.err.print("Could not compute norm inverse.");
						e.printStackTrace();
					}
				}
				// recompute P-values
				pval = 0.5 * Stats.ztest(Y[t - 1][j], mYhat.get(t - 1, j), Math.sqrt(mVhat.get(t - 1, 0)));
				mP.set(t - 1, j, pval);
				NormalDistribution nd1 = new NormalDistribution();
				try {
					mZ.set(t - 1, j, nd1.inverseCumulativeProbability(pval));
				} catch (OutOfRangeException e) {
					System.err.print("Could not compute norm inverse.");
					e.printStackTrace();
				}
			}

			// prepare new Toeplitz matrix
			k = covfunc.covVec(X, t, dX);
			tz = new Toeplitz(k);
		}
		gpm.setYcorr(Y);
		gpm.setYhat(mYhat.getArray());
		gpm.setVhat(mVhat.getColumnPackedCopy());
		gpm.setP(mP.getArray());
		gpm.setZ(mZ.getArray());

		return gpm;
	}

	/**
	 * Monitor a set of time series using a Gaussian process.
	 * <p>
	 * Compute the predictive means and variances at each time step and also
	 * compute the P-values and Z-scores associated with predictions at each
	 * time step. Use Toeplitz incremental algorithm to compute K^(-1)y
	 * 
	 * @param Y
	 *            Set of univariate time series, one in each column
	 * @param dY
	 *            Number of time series in Y
	 * @param omega
	 *            Length of cycle (or periodicity), used to compute the input X
	 * @param alpha
	 *            Threshold to identify outliers (between 0 and 1).
	 * @param missing
	 *            A matrix of same dimension as Y indicating if any observation
	 *            is missing, i.e., missing[i][j] == 1 => Y[i][j] is missing
	 * @param n
	 *            Length of training portion
	 * @return GPMonitor object containing the predicted means and variances,
	 *         corrections, Z-scores, and P-values.
	 */
	public GPMonitor monitorFastInc(double[][] Y, int dY, int omega, double alpha, double[][] missing, int n) {
		GPMonitor gpm = new GPMonitor(Y, n);

		int T = Y.length;
		double[][] X = new double[T][1];
		for (int i = 0; i < T; i++)
			X[i][0] = (double) (i + 1) / omega;
		int dX = 1;
		if ((n < 1) || (n >= T))
			throw new IllegalArgumentException(
					"Training portion should be longer than 1 and less than length of input time series.");
		int[] inds = new int[dY];
		for (int j = 0; j < dY; j++)
			inds[j] = j;
		// initialize
		Matrix mZ = new Matrix(T, dY);
		Matrix mYhat = new Matrix(T, dY);
		Matrix mP = new Matrix(T, dY);
		Matrix mVhat = new Matrix(T, 1);
		// end of initialization

		double eps = 0.001;
		for (int t = 0; t < n; t++) {
			mYhat.setMatrix(t, t, inds, (new Matrix(Y)).getMatrix(t, t, inds));
		}

		// init
		Vector<Vector<double[]>> preVal_y = new Vector<Vector<double[]>>();
		Vector<double[]> preVal_k1 = new Vector<double[]>();
		double[] k = covfunc.covVec(X, n, dX);
		Toeplitz tz = new Toeplitz(k);
		double[][] x = new double[1][dX];
		for (int j = 0; j < dX; j++)
			x[0][j] = X[n][j];
		double[] k1 = (new Matrix(covfunc.covMatCross(X, n, dX, x, 1, dX))).getColumnPackedCopy();
		double[] y = new double[n];

		for (int i = 0; i < dY; i++) {
			for (int j = 0; j < n; j++)
				y[j] = Y[j][i];
			preVal_y.add(tz.solve(y));
		}

		preVal_k1 = tz.solve(k1);

		for (int t = n + 1; t <= T; t++) {
			// obtain predictions
			Matrix Yhatt = new Matrix(1, dY);
			for (int i = 0; i < dY; i++) {
				double[] zt_y = preVal_y.get(i).get(0);
				Yhatt.set(0, i, VectorOps.dotproduct(k1, zt_y));
			}
			Matrix K11 = new Matrix(covfunc.covMatSelfTest(x, 1, dX), 1);
			double[] zt_k1 = preVal_k1.get(0);
			double Vhatt = K11.get(0, 0) - VectorOps.dotproduct(k1, zt_k1);

			// set values
			mVhat.set(t - 1, 0, Vhatt);
			mYhat.setMatrix(t - 1, t - 1, inds, Yhatt);
			// check for missing values
			if (missing != null)
				for (int j = 0; j < dY; j++)
					if (missing[t - 1][j] == 1)
						Y[t - 1][j] = mYhat.get(t - 1, j) + eps;

			// compute P-values, Z scores and fix Outliers
			for (int j = 0; j < dY; j++) {
				double pval = 0.5 * Stats.ztest(Y[t - 1][j], mYhat.get(t - 1, j), Math.sqrt(mVhat.get(t - 1, 0)));
				mP.set(t - 1, j, pval);
				NormalDistribution nd = new NormalDistribution(mYhat.get(t - 1, j), Math.sqrt(mVhat.get(t - 1, 0)));
				if (mP.get(t - 1, j) < alpha && Y[t - 1][j] > mYhat.get(t - 1, j)) {
					try {
						Y[t - 1][j] = nd.inverseCumulativeProbability(1 - alpha);
					} catch (OutOfRangeException e) {
						System.err.print("Could not compute norm inverse.");
						e.printStackTrace();
					}
				}
				if (mP.get(t - 1, j) < alpha && Y[t - 1][j] <= mYhat.get(t - 1, j)) {
					try {
						Y[t - 1][j] = nd.inverseCumulativeProbability(alpha);
					} catch (OutOfRangeException e) {
						System.err.print("Could not compute norm inverse.");
						e.printStackTrace();
					}
				}
				// recompute P-values
				pval = 0.5 * Stats.ztest(Y[t - 1][j], mYhat.get(t - 1, j), Math.sqrt(mVhat.get(t - 1, 0)));
				mP.set(t - 1, j, pval);
				NormalDistribution nd1 = new NormalDistribution();
				try {
					mZ.set(t - 1, j, nd1.inverseCumulativeProbability(pval));
				} catch (OutOfRangeException e) {
					System.err.print("Could not compute norm inverse.");
					e.printStackTrace();
				}
			}

			if (t < T) {
				// prepare new Toeplitz matrix
				k = covfunc.covVec(X, t, dX);
				x = new double[1][dX];
				for (int j = 0; j < dX; j++)
					x[0][j] = X[t][j];
				k1 = (new Matrix(covfunc.covMatCross(X, t, dX, x, 1, dX))).getColumnPackedCopy();

//				tz = new Toeplitz(k);
//				preVal_k1 = tz.solve(k1);
				preVal_k1 = Toeplitz.solveReverseInc(k, k1[0], preVal_k1.get(0), preVal_k1.get(1), preVal_k1.get(2)[0]);

				for (int i = 0; i < dY; i++) {
					preVal_y.set(i, Toeplitz.solveInc(k, Y[t - 1][i], preVal_y.get(i).get(0), preVal_y.get(i).get(1),
							preVal_y.get(i).get(2)[0]));
				}
			}
		}
		gpm.setYcorr(Y);
		gpm.setYhat(mYhat.getArray());
		gpm.setVhat(mVhat.getColumnPackedCopy());
		gpm.setP(mP.getArray());
		gpm.setZ(mZ.getArray());

		return gpm;
	}	

	/**
	 * Monitor a set of time series using a Gaussian process.
	 * <p>
	 * Compute the predictive means and variances at each time step and also
	 * compute the P-values and Z-scores associated with predictions at each
	 * time step. Use Toeplitz incremental algorithm to compute K^(-1)y
	 * 
	 * @param Y
	 *            Set of univariate time series, one in each column
	 * @param dY
	 *            Number of time series in Y
	 * @param omega
	 *            Length of cycle (or periodicity), used to compute the input X
	 * @param alpha
	 *            Threshold to identify outliers (between 0 and 1).
	 * @param missing
	 *            A matrix of same dimension as Y indicating if any observation
	 *            is missing, i.e., missing[i][j] == 1 => Y[i][j] is missing
	 * @param n
	 *            Length of training portion
	 * @return GPMonitor object containing the predicted means and variances,
	 *         corrections, Z-scores, and P-values.
	 */
	public GPMonitor monitorFastInc1(double[][] Y, int dY, int omega, double alpha, double[][] missing, int n) {
		GPMonitor gpm = new GPMonitor(Y, n);

		int T = Y.length;
		double[][] X = new double[T][1];
		for (int i = 0; i < T; i++)
			X[i][0] = (double) (i + 1) / omega;
		int dX = 1;
		if ((n < 1) || (n >= T))
			throw new IllegalArgumentException(
					"Training portion should be longer than 1 and less than length of input time series.");
		int[] inds = new int[dY];
		for (int j = 0; j < dY; j++)
			inds[j] = j;
		// initialize
		Matrix mZ = new Matrix(T, dY);
		Matrix mYhat = new Matrix(T, dY);
		Matrix mP = new Matrix(T, dY);
		Matrix mVhat = new Matrix(T, 1);
		// end of initialization

		double eps = 0.001;
		for (int t = 0; t < n; t++) {
			mYhat.setMatrix(t, t, inds, (new Matrix(Y)).getMatrix(t, t, inds));
		}

		// init
		Vector<double[]> preVal_k1 = new Vector<double[]>();
		double[] k = covfunc.covVec(X, n, dX);
		Toeplitz tz = new Toeplitz(k);
		double[][] x = new double[1][dX];

		for (int j = 0; j < dX; j++)
			x[0][j] = X[n][j];
		double[] k1 = (new Matrix(covfunc.covMatCross(X, n, dX, x, 1, dX))).getColumnPackedCopy();

		preVal_k1 = tz.solve(k1);

		for (int t = n + 1; t <= T; t++) {
			// obtain predictions
			Matrix K11 = new Matrix(covfunc.covMatSelfTest(x, 1, dX), 1);
			double[] zt_k1 = preVal_k1.get(0);
			double Vhatt = K11.get(0, 0) - VectorOps.dotproduct(k1, zt_k1);

			
			double [][] smoothedyhatt = new double[1][dY];
			for (int i = 0; i < dY; i++) {
				double yhatt = 0;
				for (int j = 0; j < zt_k1.length; j++) {
					yhatt += zt_k1[j]*Y[j][i];
				}
				smoothedyhatt[0][i] = yhatt;
			}
			
			smoothedyhatt[0] = SpatialSmoother.smooth(smoothedyhatt[0], 20, 20, 4);
			
			Matrix Yhatt = new Matrix(smoothedyhatt);
			// set values
			mVhat.set(t - 1, 0, Vhatt);
			mYhat.setMatrix(t - 1, t - 1, inds, Yhatt);
			// check for missing values
			if (missing != null)
				for (int j = 0; j < dY; j++)
					if (missing[t - 1][j] == 1)
						Y[t - 1][j] = mYhat.get(t - 1, j) + eps;

			// check for missing values 
			// for the using dataset (https://cds.nccs.nasa.gov/nex-gddp/), missing value has been set to 10E20
			for (int j = 0; j < dY; j++)
				if (Y[t - 1][j] > 10000)
					Y[t - 1][j] = mYhat.get(t - 1, j) + eps;
			
			// compute P-values, Z scores and fix Outliers
			for (int j = 0; j < dY; j++) {
				double pval = 0.5 * Stats.ztest(Y[t - 1][j], mYhat.get(t - 1, j), Math.sqrt(mVhat.get(t - 1, 0)));
				mP.set(t - 1, j, pval);
				NormalDistribution nd = new NormalDistribution(mYhat.get(t - 1, j), Math.sqrt(mVhat.get(t - 1, 0)));
				if (mP.get(t - 1, j) < alpha && Y[t - 1][j] > mYhat.get(t - 1, j)) {
					try {
						Y[t - 1][j] = nd.inverseCumulativeProbability(1 - alpha);
					} catch (OutOfRangeException e) {
						System.err.print("Could not compute norm inverse.");
						e.printStackTrace();
					}
				}
				if (mP.get(t - 1, j) < alpha && Y[t - 1][j] <= mYhat.get(t - 1, j)) {
					try {
						Y[t - 1][j] = nd.inverseCumulativeProbability(alpha);
					} catch (OutOfRangeException e) {
						System.err.print("Could not compute norm inverse.");
						e.printStackTrace();
					}
				}
				// recompute P-values
				pval = 0.5 * Stats.ztest(Y[t - 1][j], mYhat.get(t - 1, j), Math.sqrt(mVhat.get(t - 1, 0)));
				mP.set(t - 1, j, pval);
				NormalDistribution nd1 = new NormalDistribution();
				try {
					mZ.set(t - 1, j, nd1.inverseCumulativeProbability(pval));
				} catch (OutOfRangeException e) {
					System.err.print("Could not compute norm inverse.");
					e.printStackTrace();
				}
			}

			if (t < T) {
				// prepare new Toeplitz matrix
				k = covfunc.covVec(X, t, dX);
				x = new double[1][dX];
				for (int j = 0; j < dX; j++)
					x[0][j] = X[t][j];
				k1 = (new Matrix(covfunc.covMatCross(X, t, dX, x, 1, dX))).getColumnPackedCopy();

				preVal_k1 = Toeplitz.solveReverseInc(k, k1[0], preVal_k1.get(0), preVal_k1.get(1), preVal_k1.get(2)[0]);
			}
		}
		gpm.setYcorr(Y);
		gpm.setYhat(mYhat.getArray());
		gpm.setVhat(mVhat.getColumnPackedCopy());
		gpm.setP(mP.getArray());
		gpm.setZ(mZ.getArray());

		return gpm;
	}	
	
	/**
	 * Save the log of hyper-parameters associated with the GP to a file.
	 * 
	 * @param loghyperfile
	 *            File to which the log of hyper-parameters are to be written.
	 */
	public void saveloghypers(String loghyperfile) {
		PrintWriter pw;
		try {
			pw = new PrintWriter(loghyperfile);
			double[] lh = this.getCovFunc().getLogHypers();
			for (int i = 0; i < lh.length - 1; i++)
				pw.printf("%f ", lh[i]);
			pw.printf("%f\n", lh[lh.length - 1]);
			pw.close();
		} catch (FileNotFoundException e) {
			System.err.println("Could not open loghyperfile for writing.");
			e.printStackTrace();
		}
	}

	public void monitorslow(double[][] Y, int dY, int omega, int n) {

		int T = Y.length;
		double[][] X = new double[T][1];
		for (int i = 0; i < T; i++)
			X[i][0] = (double) (i + 1) / omega;
		int dX = 1;
		if ((n < 1) || (n >= T))
			throw new IllegalArgumentException(
					"Training portion should be longer than 1 and less than length of input time series.");
		int[] inds = new int[dY];
		for (int j = 0; j < dY; j++)
			inds[j] = j;
		// end of initialization

		Matrix K = new Matrix(covfunc.covMat(X, n, dX));
		CholeskyDecomposition cd = K.chol();
		if (!cd.isSPD())
			throw new RuntimeErrorException(null, "Matrix is either not symmetric or positive definite.");
		for (int t = n + 1; t <= T; t++) {
			double[][] x = new double[1][dX];
			for (int j = 0; j < dX; j++)
				x[0][j] = X[t - 1][j];
			Matrix K1 = new Matrix(covfunc.covMatCross(X, t - 1, dX, x, 1, dX));
			Matrix K11 = new Matrix(covfunc.covMatSelfTest(x, 1, dX), 1);
			Matrix mY = (new Matrix(Y)).getMatrix(0, t - 2, inds);
			for (int j = 0; j < mY.getColumnDimension(); j++) {
				cd.solve(mY.getMatrix(0, mY.getRowDimension() - 1, j, j));
			}
			// obtain predictions
			cd.getL().solve(K1);
			// prepare new cholesky decomposition for next iteration
			Matrix Knew = new Matrix(K.getRowDimension() + 1, K.getColumnDimension() + 1);
			Knew.setMatrix(0, t - 2, 0, t - 2, K);
			Knew.setMatrix(0, t - 2, t - 1, t - 1, K1);
			Knew.setMatrix(t - 1, t - 1, 0, t - 2, K1.transpose());
			Knew.set(t - 1, t - 1, K11.get(0, 0));
			K = Knew;
			cd = Knew.chol();
			if (!cd.isSPD())
				throw new RuntimeErrorException(null, "Matrix is either not symmetric or positive definite.");
		}
	}

	public void monitorfast(Vector<double[]> vY, int omega, int n) {
		int T = vY.get(1).length;
		double[][] X = new double[T][1];
		for (int i = 0; i < T; i++)
			X[i][0] = (double) (i + 1) / omega;
		int dX = 1;
		int dY = vY.size();
		if ((n < 1) || (n >= T))
			throw new IllegalArgumentException(
					"Training portion should be longer than 1 and less than length of input time series.");
		int[] inds = new int[dY];
		for (int j = 0; j < dY; j++)
			inds[j] = j;
		// end of initialization
		double[] k = covfunc.covVec(X, n, dX);
		Toeplitz tz = new Toeplitz(k);
		for (int t = n + 1; t <= T; t++) {
			double[][] x = new double[1][dX];
			for (int j = 0; j < dX; j++)
				x[0][j] = X[t - 1][j];
			double[][] _k1 = covfunc.covMatCross(X, t - 1, dX, x, 1, dX);
			double[] k1 = (new Matrix(_k1)).getColumnPackedCopy();
			double[] y = new double[t - 1];
			for (int j = 0; j < t - 1; j++)
				y[j] = vY.get(0)[j];
			for (int j = 0; j < dY; j++) {
				tz.solve(y);
			}
			// obtain predictions
			tz.solve(k1);

			// prepare new Toeplitz matrix
			k = covfunc.covVec(X, t, dX);
			tz = new Toeplitz(k);
		}
	}
}
