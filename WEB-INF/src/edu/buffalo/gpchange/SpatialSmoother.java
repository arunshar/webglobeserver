package edu.buffalo.gpchange;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;

/** Class to perform spatial smoothing on a 2-D matrix.
 * 
 * @author Varun Chandola
 */
public class SpatialSmoother {
	static double [] means = {0,0};
	static double [][] covariances = {{1,0},{0,1}};
	static MultivariateNormalDistribution mNormal = new MultivariateNormalDistribution(means, covariances);
	
	/** Spatial smoothing routine for floating point (double precision) data.
	 * @param data original 2D matrix
	 * @param k neighborhood extent
	 * @return smootheddata smoothed version of data  
	 */
	public static double[] smooth(double[] data, int row, int col, int k){
		double[] smootheddata = data.clone();
		for(int i = 0; i < row; i++){
			for(int j = 0; j < col; j++){
				int nbdx1 = Math.max(i - k, 0);
				int nbdx2 = Math.min(i + k , row-1);
				int nbdy1 = Math.max(j - k, 0);
				int nbdy2 = Math.min(j + k , col-1);
				//replace with mean of neighborhood
				double mean = 0;double size = 0;
				double[] vals = new double[2]; 
				for(int i1 = nbdx1; i1 <= nbdx2; i1++){
					for(int j1 = nbdy1; j1 <= nbdy2; j1++){
						vals[0] = (3.0*(i1 - i))/k;
						vals[1] = (3.0*(j1 - j))/k;
						mean += mNormal.density(vals)*data[i1*col + j1];
						size += mNormal.density(vals);
					}
				}
				smootheddata[i*col + j] = mean/size;
			}			
		}
		return smootheddata;
	}

	
	/** Spatial smoothing routine for floating point (double precision) data.
	 * @param data original 2D matrix
	 * @param k neighborhood extent
	 * @return smootheddata smoothed version of data  
	 */
	public static double[][] smooth(double[][] data, int k){
		double[][] smootheddata = data.clone();
		for(int i = 0; i < data.length; i++){
			for(int j = 0; j < data[i].length; j++){
				int nbdx1 = Math.max(i - k + 1, 0);
				int nbdx2 = Math.min(i + k , data.length);
				int nbdy1 = Math.max(j - k + 1, 0);
				int nbdy2 = Math.min(j + k , data.length);
				//replace with mean of neighborhood
				double mean = 0;int size = 0;
				for(int i1 = nbdx1; i1 <= nbdx2; i1++){
					for(int j1 = nbdy1; j1 <= nbdy2; j1++){
						mean += data[i1][j1];
						size++;
					}
				}
				smootheddata[i][j] = mean/size;
			}			
		}
		return smootheddata;
	}

	/** Spatial smoothing routine for integer data.
	 * <p/>
	 * Method specifies the smoothing heurisitic. If method == 1
	 * the data[i][j] is replaced by the majority of the neighborhood,
	 * if the majority is significant (> frac).
	 * If method == 2 and data[i][j] == 0 and majority == 2 then data[i][j]
	 * is replaced with 1.
	 * If method == 2 and data[i][j] == 2 and majority == 0 then data[i][j]
	 * is replaced with 1.
	 * 
	 * @param data original 2D matrix
	 * @param k neighborhood extent
	 * @param meth method for smoothing (1 for neighborhood majority, 2 for specialized)
	 * @param frac fraction of neighborhood required for a significant majority
	 * @return smootheddata smoothed version of data
	 * @throws IllegalArgumentException incorrect method specified 
	 */
	public static int[][] smooth(int[][] data, int k, int meth, double frac){
		int[][] smootheddata = data.clone();
		for(int i = 0; i < data.length; i++){
			for(int j = 0; j < data[i].length; j++){
				int nbdx1 = Math.max(i - k, 0);
				int nbdx2 = Math.min(i + k , data.length-1);
				int nbdy1 = Math.max(j - k, 0);
				int nbdy2 = Math.min(j + k , data[i].length-1);
				//compute neighborhood majority
				HashMap<Integer,Integer> mp = new HashMap<Integer,Integer>();
				int nbdsize = 0;
				for(int i1 = nbdx1; i1 <= nbdx2; i1++){
					for(int j1 = nbdy1; j1 <= nbdy2; j1++){
						nbdsize++;
						if(mp.containsKey(data[i1][j1])){
							Integer c = mp.get(data[i1][j1]);
							mp.put(data[i1][j1], c+1);
						}else{
							mp.put(data[i1][j1], 1);
						}
					}
				}
				Iterator<Map.Entry<Integer,Integer>> it = mp.entrySet().iterator();
				int maxid = -1, maxval = -1;
				while(it.hasNext()){
					Map.Entry<Integer, Integer> en = it.next();
					if(en.getValue() > maxval){
						maxval = en.getValue();
						maxid = en.getKey();
					}
				}
				//check if majority is significant
				if(maxval >= frac*nbdsize){
					switch(meth){
					case 1:
						smootheddata[i][j] = maxid;
						break;
					case 2:
						if((data[i][j] == 0) && (maxid == 2))
							smootheddata[i][j] = 1;
						if((data[i][j] == 2) && (maxid == 0))
							smootheddata[i][j] = 1;
						break;
					default:
						throw new IllegalArgumentException("Illegal method invoked for smoothing.");
					}
				}
			}			
		}
		return smootheddata;
	}
}
