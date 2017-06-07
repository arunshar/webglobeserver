package edu.buffalo.gpchange;


/**
 * Squared Exponential + Exponential Periodic + Noise covariance function.
 * <p>
 *  Covariance function for a squared exponential + exponential periodic + 
 *  noise function, with unit period. The covariance function is:
 *  k(x^p, x^q)
 *    = sigma2f*exp(-(x_p-x_q)^2/(2*l^2))exp(-(1 - cos(2*pi*(x_p-x_q))/a) + sigma2n 
 *    
 *  where the hyperparameters are:
 *  logtheta = [ log(l) log(sqrt(sigma2f)) log(a) log(sqrt(sigma2n))]
 *  
 * @author Varun Chandola
 */

public class CovSEEPNoiseiso extends CovFunction implements CovFunctionInterface{
	
    public CovSEEPNoiseiso() {
	super();
    }

	public CovSEEPNoiseiso(double[] loghypers, int numParams) {
		super(loghypers, numParams);
	}

	public int retNumParams(){
	        return 4;
	}

	public double[][] covMat(double[][] X, int nX, int dX){
		double[] lh = getLogHypers(); 
		double l = Math.exp(lh[0]);
		double sf2 = Math.exp(2*lh[1]);
		double a = Math.exp(lh[2]);
		double sn2 = Math.exp(2*lh[3]);
		
		double [][] cM = new double[nX][nX];
		for(int i=0; i < nX; i++){
			for(int j=i; j < nX; j++){
				double _int = VectorOps.EuclideanDistance(X[i],X[j],dX);
				cM[i][j] = (sf2*Math.exp(-_int/(2*Math.pow(l, 2)))*Math.exp(-(1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a));				
				if(j != i) cM[j][i] = cM[i][j];
				else cM[i][j] += sn2;
			}
		}
		return cM;
	}

	public double[][] covMatDer(double[][] X, int nX, int dX, int param){
		double[] lh = getLogHypers(); 
		double l = Math.exp(lh[0]);
		double sf2 = Math.exp(2*lh[1]);
		double a = Math.exp(lh[2]);
		double sn2 = Math.exp(2*lh[3]);
		double [][] cMD = new double[nX][nX];
		for(int i=0; i < nX; i++){
			for(int j=i; j < nX; j++){
				double _int = (VectorOps.EuclideanDistance(X[i],X[j],dX));
				switch(param){
				case 1://log(l)
					cMD[i][j] = sf2*Math.exp(-_int/(2*Math.pow(l, 2)))*Math.exp(-(1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a)*_int/Math.pow(l,2);
					break;
				case 2://log(sf)
					cMD[i][j] = (2*sf2*Math.exp(-_int/(2*Math.pow(l, 2)))*Math.exp(-(1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a));
					break;
				case 3://log(a)
					cMD[i][j] = ((sf2*Math.exp(-_int/(2*Math.pow(l, 2))))*Math.exp(-(1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a)*((1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a));
					break;
				case 4://log(sn)
					if(i == j) cMD[i][j] = 2*sn2;
					else cMD[i][j] = 0;
					break;
				default:
					throw new IllegalArgumentException("Incorrect Parameter Value");						
				}
				if(j != i) cMD[j][i] = cMD[i][j];
			}
		}		
		return cMD;
	}

	public double[] covVec(double[][] X, int nX, int dX){
		double[] lh = getLogHypers(); 
		double l = Math.exp(lh[0]);
		double sf2 = Math.exp(2*lh[1]);
		double a = Math.exp(lh[2]);
		double sn2 = Math.exp(2*lh[3]);
		
		double [] cM = new double[nX];
		for(int i=0; i < nX; i++){
			double _int = VectorOps.EuclideanDistance(X[i],X[0],dX);
			cM[i] = (sf2*Math.exp(-_int/(2*Math.pow(l, 2)))*Math.exp(-(1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a));				
			if(i == 0) cM[i] += sn2;
		}
		return cM;
	}

	public double[] covVecDer(double[][] X, int nX, int dX, int param){
		double[] lh = getLogHypers(); 
		double l = Math.exp(lh[0]);
		double sf2 = Math.exp(2*lh[1]);
		double a = Math.exp(lh[2]);
		double sn2 = Math.exp(2*lh[3]);
		double [] cMD = new double[nX];
		for(int i=0; i < nX; i++){
			double _int = (VectorOps.EuclideanDistance(X[i],X[0],dX));
			switch(param){
			case 1://log(l)
				cMD[i] = sf2*Math.exp(-_int/(2*Math.pow(l, 2)))*Math.exp(-(1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a)*_int/Math.pow(l,2);
				break;
			case 2://log(sf)
				cMD[i] = (2*sf2*Math.exp(-_int/(2*Math.pow(l, 2)))*Math.exp(-(1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a));
				break;
			case 3://log(a)
				cMD[i] = ((sf2*Math.exp(-_int/(2*Math.pow(l, 2))))*Math.exp(-(1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a)*((1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a));
				break;
			case 4://log(sn)
				if(i == 0) cMD[i] = 2*sn2;
				else cMD[i] = 0;
				break;
			default:
				throw new IllegalArgumentException("Incorrect Parameter Value");						
			}			
		}		
		return cMD;
	}

	public double[][] covMatCross(double[][] X, int nX, int dX, double [][]Y, int nY, int dY){
		if(dX != dY) throw new IllegalArgumentException("X and Y should have equal number of attributes.");
		double[] lh = getLogHypers(); 
		double l = Math.exp(lh[0]);
		double sf2 = Math.exp(2*lh[1]);
		double a = Math.exp(lh[2]);
		double [][] cMX = new double[nX][nY];
		for(int i=0; i < nX; i++){
			for(int j=0; j < nY; j++){
				double _int = VectorOps.EuclideanDistance(X[i],Y[j],dX);
				cMX[i][j] = (sf2*Math.exp(-_int/(2*Math.pow(l, 2)))*Math.exp(-(1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a));				
			}
		}		
		return cMX;
	}
	
	public double[][] covMatCrossDer(double[][] X, int nX, int dX, double [][]Y, int nY, int dY, int param){
		if(dX != dY) throw new IllegalArgumentException("X and Y should have equal number of attributes.");
		double[] lh = getLogHypers(); 
		double l = Math.exp(lh[0]);
		double sf2 = Math.exp(2*lh[1]);
		double a = Math.exp(lh[2]);
		double [][] cMXD = new double[nX][nY];
		for(int i=0; i < nX; i++){
			for(int j=0; j < nY; j++){
				double _int = (VectorOps.EuclideanDistance(X[i],Y[j],dX));
				switch(param){
				case 1://log(l)
					cMXD[i][j] = (sf2*Math.exp(-_int/(2*Math.pow(l, 2)))*Math.exp(-(1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a)*_int/Math.pow(l, 2));
					break;
				case 2://log(sf)
					cMXD[i][j] = (2*sf2*Math.exp(-_int/(2*Math.pow(l, 2)))*Math.exp(-(1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a));
					break;
				case 3://log(a)
					cMXD[i][j] = (sf2*Math.exp(-_int/(2*Math.pow(l, 2)))*Math.exp(-(1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a)*((1 - Math.cos(2*Math.PI*Math.sqrt(_int)))/a));
					break;
				case 4://log(sn)
					cMXD[i][j] = 0;
					break;
				default:
					throw new IllegalArgumentException("Incorrect Parameter Value");						
				}
			}
		}		
		return cMXD;
	}
	
	public double[] covMatSelfTest(double[][] X, int nX, int dX){
		double[] lh = getLogHypers(); 
		double sf2 = Math.exp(2*lh[1]);
		double sn2 = Math.exp(2*lh[3]);
		double [] cMS = new double[nX];
		for(int i=0; i < nX; i++){
				cMS[i] = sf2+sn2;
		}
		return cMS;
	}
	
	public double[] covMatSelfTestDer(double[][] X, int nX, int dX, int param){
		double[] lh = getLogHypers(); 
		double sf2 = Math.exp(2*lh[1]);
		double sn2 = Math.exp(2*lh[3]);
		double [] cMSD = new double[nX];
		for(int i=0; i < nX; i++){
			switch(param){
			case 1://log(l)
				cMSD[i] = 0;
				break;
			case 2://log(sf)
				cMSD[i] = 2*sf2;
				break;
			case 3://log(a)
				cMSD[i] = 0;
				break;
			case 4://log(sn)
				cMSD[i] = 2*sn2;
				break;
			default:
				throw new IllegalArgumentException("Incorrect Parameter Value");						
			}
		}
		return cMSD;
	}
}
