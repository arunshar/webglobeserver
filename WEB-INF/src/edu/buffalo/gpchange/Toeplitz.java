
package edu.buffalo.gpchange;

import java.util.Vector;

import javax.management.RuntimeErrorException;

/**
 * Class for manipulating a Toeplitz Matrix
 * 
 * @author Varun Chandola
 *
 */
public class Toeplitz {
	
	/**
	 * First row of the symmetric toeplitz matrix to be inverted.
	 */
	private double [] r;

	
	/** Constructor for Toeplitz class
	 * @param r
	 */
	public Toeplitz(double[] r) {
		this.r = r;
	}

	/** Set r
	 * @param r
	 */
	public void setR(double [] r) {
		this.r = r;
	}

	/** Get r
	 * @return r
	 */
	public double [] getR() {
		return r;
	}
	
	/** Compute the inverse of a symmetric Toeplitz matrix
	 * <p/>
	 * Uses the algorithm proposed in "An algorithm for the inversion of finite Toelitz Matrices" Trench, W. F., J. SIAM, 1964.
	 * @return inverse of L matrix
	 * @throws RuntimeErrorException
	 */
	public double [][] inverse(){
		double fac = 1;
		double [] r1 = this.getR();
		if(r1[0] != 1){
			fac = r1[0];
			if(fac == 0) throw new RuntimeErrorException(null, "Main diagonal entries cannot be 0.");
			r1 = VectorOps.scalarProd(r1, 1 / fac, r1.length);
		}
		
		int n = r1.length - 1;
		double [] lambdas = new double[n];
		double [] gammas = new double[n-1];
		Vector<double []> gs = new Vector<double []>(0);
		
		//initialization
		lambdas[0] = 1 - Math.pow(r1[1],2);
		double [] g1 = new double[1];
		g1[0] = -1*r1[1];
		gs.add(g1);
		
		for(int i = 0; i < n-1; i++){
			double [] ri = VectorOps.subvector(r1, 1, 1+i, n+1);
			double [] gihat = VectorOps.reverse(gs.elementAt(i),gs.elementAt(i).length);
			gammas[i] = -1*(r1[i+2] + VectorOps.dotproductr(ri,gs.elementAt(i)));
			lambdas[i+1] = lambdas[i] - Math.pow(gammas[i],2)/lambdas[i];
			double gbyl = gammas[i]/lambdas[i];
			double [] giplus1hat = new double[1+gihat.length];
			giplus1hat[0] = gbyl;
			for(int j = 1; j < giplus1hat.length; j++) giplus1hat[j] = gihat[j-1] + gbyl*(gs.elementAt(i))[j-1];
			gs.add(VectorOps.reverse(giplus1hat, giplus1hat.length));			
		}
		//Compute the final inverse
		lambdas[n-1] = lambdas[n-1]*fac; 
		double [][] B = new double[n+1][n+1];
		B[0][0] = 1/lambdas[n-1];
		double [] gsn = gs.elementAt(n-1);		
		for(int i = 1;i < n+1;i++){
			B[i][0] = gsn[i-1]/lambdas[n-1];
			for(int j = 1; j <= i;j++)
				B[i][j] = B[i-1][j-1] + (gsn[i-1]*gsn[j-1] - gsn[n-i]*gsn[n-j])/lambdas[n-1];
		}
		for(int i = 0;i < n+1; i++)
			for(int j = i+1;j < n+1;j++)
				B[i][j] = B[j][i];

		return B;
	}
	
	/** Compute the log of determinant of a symmetric Toeplitz matrix
	 * <p/>
	 * @return log(det(A))
	 * @throws RuntimeErrorException
	 */
	public double logdet(){
		double logdet = 0;
		double fac = 1;
		double [] r1 = this.getR();
		if(r1[0] != 1){
			fac = r1[0];
			if(fac == 0) throw new RuntimeErrorException(null, "Main diagonal entries cannot be 0.");
			r1 = VectorOps.scalarProd(r1,1/fac,r1.length);
		}
		
		int n = r1.length - 1;
		double [] lambdas = new double[n];
		double [] gammas = new double[n-1];
		Vector<double []> gs = new Vector<double []>(0);
		
		//initialization
		lambdas[0] = 1 - Math.pow(r1[1],2);
		double [] g1 = new double[1];
		g1[0] = -1*r1[1];
		gs.add(g1);
		
		for(int i = 0; i < n-1; i++){
			double [] ri = VectorOps.subvector(r1, 1, 1+i, n+1);
			double [] gihat = VectorOps.reverse(gs.elementAt(i),gs.elementAt(i).length);
			gammas[i] = -1*(r1[i+2] + VectorOps.dotproductr(ri,gs.elementAt(i)));
			lambdas[i+1] = lambdas[i] - Math.pow(gammas[i],2)/lambdas[i];
			double gbyl = gammas[i]/lambdas[i];
			double [] giplus1hat = new double[1+gihat.length];
			giplus1hat[0] = gbyl;
			for(int j = 1; j < giplus1hat.length; j++) giplus1hat[j] = gihat[j-1] + gbyl*(gs.elementAt(i))[j-1];
			gs.add(VectorOps.reverse(giplus1hat, giplus1hat.length));			
		}
//		lambdas[n-1] = lambdas[n-1]; 
//		double tmp = 1;
//		for(int i = 0; i < lambdas.length;i++)
//			tmp *= lambdas[i];
//		tmp *= Math.pow(fac, n+1);
//		logdet = Math.log(Math.abs(tmp));

		logdet = 0;
		for(int i = 0; i < lambdas.length;i++)
			logdet += Math.log(lambdas[i]);
		logdet += Math.log(fac)*(n+1);
		return logdet;
	}
	
	/** Compute the diagonal sums inverse of a symmetric Toeplitz matrix
	 * <p/>
	 * Uses the algorithm adapted from the Yule-Walker equations solver as proposed in "Matrix Computations", Golub and Van Loan, John Hopkins Press, 1996.
	 * @return vector containing sum of diagonals
	 * @throws RuntimeErrorException
	 */
	public double [] diagsums(){
		double [] r1 = this.getR();
		int n = r1.length - 1;
		double r0 = r1[0];
		double [] rn = VectorOps.subvector(r1,1,n,n+1);
		double [] r2 = VectorOps.subvector(r1,0,n-1,n+1);
		Toeplitz tz = new Toeplitz(r2);
		double []alpha = tz.solve(VectorOps.scalarProd(rn,-1,n)).get(0);
		double gamma = 1/(r0+VectorOps.dotproduct(rn,alpha));
		
		double [] nu = new double[n+1];
		for(int i = 0; i < n;i++){
			nu[i] = gamma*alpha[n-i-1];
		}
		nu[n] = gamma;
		int N = n+1;
		double [] sums = new double[N];
		for(int t = 0;t < N; t++){
			double st = 0;			
			for(int i = 1; i < N-t+1;i++){
				st += (2*i+t-N)*nu[i-1]*nu[i+t-1];
			}
			sums[t] = st/gamma;
		}
		return sums;
	}
	
	/** Solve a system of equations for a symmetric Toeplitz matrix L. Does not explicitly compute the inverse of the L matrix.
	 * <p/>
	 * Uses the algorithm proposed in "The Solution of a Toeplitz Set of Linear Equations", Zohar, S., JACM, 1974.
	 * 
	 * @param y Target vector for equation Lx = y
	 * @return Solution x = inv(L)y
	 * @throws RuntimeErrorException
	 */
	public Vector<double []> solve(double [] y){
		double fac = 1;
		double [] r1 = this.getR();
		if(y.length != r1.length) throw new RuntimeErrorException(null, "Target vector length not compatible with matrix size.");
		if(r1[0] != 1){
			fac = r1[0];
			if(fac == 0) throw new RuntimeErrorException(null, "Main diagonal entries cannot be 0.");
			r1 = VectorOps.scalarProd(r1,1/fac,r1.length);
			y = VectorOps.scalarProd(y, 1/fac, y.length);
		}
		
		int n = r1.length - 1;
		double lambdai;
		double gammai;
		double thetai;
		double[] gi = new double[n];
		double[] giplus1 = new double[n];
		double[] si = new double[n+1];
		double[] siplus1 = new double[n+1];		
		
		//initialization
		lambdai = 1 - Math.pow(r1[1],2);
		gi[0] = -1*r1[1];
		si[0] = y[0];
		
		for(int i = 0; i < n-1; i++){
			thetai = y[i+1];
			for(int j = 0; j < i+1; j++) thetai -= r1[j+1]*si[i-j];
			gammai = -r1[i+2];
			for(int j = 0; j < i+1; j++) gammai -= r1[j+1]*gi[i-j];
			double tbyl = thetai/lambdai;
			for(int j = 0; j < i+1; j++) siplus1[j] = si[j] + tbyl*gi[i-j];
			siplus1[i+1] = tbyl;

			double gbyl = gammai/lambdai;
			for(int j = 0; j < i+1; j++) giplus1[j] = gi[j] + gbyl*gi[i-j];
			giplus1[i+1] = gbyl;

			lambdai = lambdai - (gammai*gammai)/lambdai;
			
			//swap arrays
			double[] temp;
			temp = gi;
			gi = giplus1;
			giplus1 = temp;

			temp = si;
			si = siplus1;
			siplus1 = temp;			
		}

		//Compute the final solution vector
		double thetan = y[y.length - 1];
		for(int i = 0; i < n; i++) thetan -= r1[i+1]*si[n-i-1];
		double tnbyl = thetan/lambdai;
		for(int i = 0; i < siplus1.length-1; i++) siplus1[i] = si[i] + tnbyl*gi[n-i-1];
		siplus1[siplus1.length-1] = tnbyl;
		
		double [] vlambdat = {lambdai};
		Vector<double []> result = new Vector<double []>();
		result.add(siplus1);
		result.add(gi);
		result.add(vlambdat);
		
		return result;
	}
		
	/** Solve a system of equations for a symmetric Toeplitz matrix L. Does not explicitly compute the inverse of the L matrix.
	 * <p/>
	 * Uses the incremental algorithm proposed in "A Gaussian Process Based Online Change Detection Algorithm for Monitoring 
	 * Periodic Time Series", Varun Chandola, SIAM, ...
	 *
	 * @return Solution x = inv(L)y
	 * @throws RuntimeErrorException
	 */
	public static Vector<double []> solveInc(double [] ktplus1, double ytplus1, double [] zt, double [] gtminus1, double lambdastminus1 ){		
		double fac = 1;
		if(ktplus1[0] != 1){
			fac = ktplus1[0];
			if(fac == 0) throw new RuntimeErrorException(null, "Main diagonal entries cannot be 0.");
			ktplus1 = VectorOps.scalarProd(ktplus1,1/fac,ktplus1.length);
			ytplus1 = ytplus1/fac;
		}
		
		int n = ktplus1.length - 1;

		double [] k2tot = VectorOps.subvector(ktplus1, 1, n-1, n+1);
		double gammastminus1 = -ktplus1[n] - VectorOps.dotproductr(gtminus1, k2tot);
		
		double [] gtminus1hat = VectorOps.reverse(gtminus1,gtminus1.length);
		double [] gt = new double [gtminus1.length + 1];
		double gbyl = gammastminus1/lambdastminus1; 
		for(int j = 0; j < gt.length-1; j++) gt[j] = gtminus1[j] + gbyl*gtminus1hat[j];
		gt[gt.length-1] = gbyl;
		
		double lambdat = lambdastminus1 - gammastminus1*gammastminus1/lambdastminus1;
		
		double [] k2totplus1 = VectorOps.subvector(ktplus1, 1, n, n+1);		
		double thetat = ytplus1 - VectorOps.dotproductr(zt, k2totplus1);
		double [] gthat = VectorOps.reverse(gt,gt.length);
		double [] ztplus1 = new double [zt.length + 1];
		double tbyl = thetat/lambdat; 
		for(int j = 0; j < ztplus1.length-1; j++) ztplus1[j] = zt[j] + tbyl*gthat[j];
		ztplus1[ztplus1.length-1] = tbyl;
		
		double [] vlambdat = {lambdat};
		Vector<double []> result = new Vector<double []>();
		result.add(ztplus1);
		result.add(gt);
		result.add(vlambdat);
		
		return result;
	}

	
	/** Solve a system of equations for a symmetric Toeplitz matrix L. Does not explicitly compute the inverse of the L matrix.
	 * <p/>
	 * Uses the incremental algorithm proposed in "A Gaussian Process Based Online Change Detection Algorithm for Monitoring 
	 * Periodic Time Series", Varun Chandola, SIAM, ...
	 * 
	 * @return Solution x = inv(L)y
	 * @throws RuntimeErrorException
	 */
	public static Vector<double []> solveInc(int t, double [] ktplus1, double ytplus1, double [] zt, double [] gtminus1, double lambdastminus1, double [] ztplus1, double [] gt){		
		double fac = 1;
		if(ktplus1[0] != 1){
			fac = ktplus1[0];
			if(fac == 0) throw new RuntimeErrorException(null, "Main diagonal entries cannot be 0.");
			ktplus1 = VectorOps.scalarProd(ktplus1,1/fac,ktplus1.length);
			ytplus1 = ytplus1/fac;
		}
		
		double gammastminus1 = -ktplus1[t];
		for(int j = 0; j < t-1; j++) gammastminus1 -= ktplus1[j+1]*gtminus1[t-j-2];		
		
		double gbyl = gammastminus1/lambdastminus1; 
		for(int j = 0; j < t-1; j++) gt[j] = gtminus1[j] + gbyl*gtminus1[t-j-2];
		gt[t-1] = gbyl;
		
		double lambdat = lambdastminus1 - gammastminus1*gammastminus1/lambdastminus1;
				
		double thetat = ytplus1;
		for(int j = 0; j < t; j++) thetat -= ktplus1[j+1]*zt[t-j-1];		

		double tbyl = thetat/lambdat; 
		for(int j = 0; j < t; j++) ztplus1[j] = zt[j] + tbyl*gt[t-j-1];
		ztplus1[t] = tbyl;
		
		double [] vlambdat = {lambdat};
		Vector<double []> result = new Vector<double []>();
		result.add(ztplus1);
		result.add(gt);
		result.add(vlambdat);
		
		return result;
	}	

	/** Solve a system of equations for a symmetric Toeplitz matrix L. Does not explicitly compute the inverse of the L matrix.
	 * <p/>
	 * Uses the incremental algorithm proposed in "A Gaussian Process Based Online Change Detection Algorithm for Monitoring 
	 * Periodic Time Series", Varun Chandola, SIAM, ...
	 * 
	 * @return Solution x = inv(L)y
	 * @throws RuntimeErrorException
	 */
	public static Vector<double []> solveReverseInc(double [] ktplus1, double ytplus1, double [] zt, double [] gtminus1, double lambdastminus1 ){		
		double fac = 1;
		if(ktplus1[0] != 1){
			fac = ktplus1[0];
			if(fac == 0) throw new RuntimeErrorException(null, "Main diagonal entries cannot be 0.");
			ktplus1 = VectorOps.scalarProd(ktplus1,1/fac,ktplus1.length);
			ytplus1 = ytplus1/fac;
		}
		
		int n = ktplus1.length - 1;

		double [] k2tot = VectorOps.subvector(ktplus1, 1, n-1, n+1);
		double gammastminus1 = -ktplus1[n] - VectorOps.dotproductr(gtminus1, k2tot);
		
		double [] gtminus1hat = VectorOps.reverse(gtminus1,gtminus1.length);
		double [] gt = new double [gtminus1.length + 1];
		double gbyl = gammastminus1/lambdastminus1; 
		for(int j = 0; j < gt.length-1; j++) gt[j] = gtminus1[j] + gbyl*gtminus1hat[j];
		gt[gt.length-1] = gbyl;
		
		double lambdat = lambdastminus1 - gammastminus1*gammastminus1/lambdastminus1;
		
		double [] k2totplus1 = VectorOps.subvector(ktplus1, 1, n, n+1);		
		double thetat = ytplus1 - VectorOps.dotproduct(zt, k2totplus1);
		double [] ztplus1 = new double [zt.length + 1];
		double tbyl = thetat/lambdat; 
		for(int j = 1; j < ztplus1.length; j++) ztplus1[j] = zt[j-1] + tbyl*gt[j-1];
		ztplus1[0] = tbyl;
		
		double [] vlambdat = {lambdat};
		Vector<double []> result = new Vector<double []>();
		result.add(ztplus1);
		result.add(gt);
		result.add(vlambdat);
		
		return result;
	}
	
	
	/** Compute the product of a vector and a symmetric Toeplitz matrix L.
	 * 
	 * @param x Vector to be multiplied
	 * @return Solution vp = x'*L
	 */
	public double[] vectorprod(double [] x){					
		double [] vp = new double[x.length];
		for(int i = 0; i < x.length; i++){
			vp[i] = 0;
			for(int j = 0; j < x.length; j++)
				vp[i] += x[j]*this.r[Math.abs(i-j)];			
		}
		return vp;
	}
	
      /** Compute the trace of product of inverse of a Toeplitz matrix L and another Toeplitz matrix K
       *
       * @param k Vector for second Toeplitz matrix K
       * @return tp = trace(inv(L)*K)
       */
	public double traceprod(double [] k){
		double [] cs = this.diagsums();
		double tp = k[0]*cs[0];
		for(int i = 1; i < k.length; i++)
			tp += 2*k[i]*cs[i];
		return tp;
	}
}