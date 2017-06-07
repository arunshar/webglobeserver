/**
 *
 */
package edu.buffalo.gpchange.test;

import edu.buffalo.gpchange.CovSEEPNoiseiso;
import edu.buffalo.gpchange.GPChange;
import edu.buffalo.gpchange.Stats;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;
import java.util.Vector;

/** Class for multithreaded training of hyperparameters from a set of time series.
 * @author Varun Chandola
 *
 */
public class TrainAllP{

	/** Multithreaded version of TrainAll. Reads data from a text file containing one time series per row.
	 * param trainingdatafilename Training data in csv format
	 * param omega Length of cycle
	 * param outputfilename Output file to store log hyper-parameters
	 * param ncols Number of columns in each time series
	 * param normalize Boolean flag indicating if normalization is required (1) or not (0)
	 * param numthreads Number of threads to create
	 * param startrownum Starting row from which processing starts (Starts from 0)
	 * param numrows Number of data instances to process
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	public static void main(String[] args) throws NumberFormatException, IOException{
		if(args.length != 8){
			throw new IllegalArgumentException("Incorrect number of parameters.\n Usage: java TrainAllP trainingdatafile omega outputfile ncols normalize numthreads startrownum numrows");
		}
		double[] loghypers = {(double) Math.log(1),(double) Math.log(1),(double) Math.log(1),(double) Math.log(1)};
		String trainFileName = args[0];
		int omega = Integer.parseInt(args[1]);
		String outputFileName = args[2];
		int ncols = Integer.parseInt(args[3]);
		int normalize = Integer.parseInt(args[4]);
		int numthreads = Integer.parseInt(args[5]);
		int startrownum = Integer.parseInt(args[6]);
		int numrows = Integer.parseInt(args[7]);
		if(startrownum < 0)
			throw new IllegalArgumentException("Processing has to start from at least row #0");

		boolean endflag = false;
		double[][] X = new double[ncols][1];
		for(int i = 0;i < ncols;i++) X[i][0] = (double) (i+1)/omega;
		long start = System.currentTimeMillis();
		try {
			BufferedReader bTrain = new BufferedReader(new FileReader(trainFileName));
			PrintWriter pWriter = new PrintWriter(outputFileName);
			//skip till startrownum
			for(int i = 0; i < startrownum-1;i++){bTrain.readLine();}
			int r = 0;
			while(!endflag){
				Vector<String> lines = new Vector<String>(0);
				for(int i = 0; i < numthreads;i++){
					String line = bTrain.readLine();
					if(line == null){
						endflag = true;
						break;
					}else{
						lines.add(line);
					}
				}
				if(!lines.isEmpty()){
					double [][] lhs = new double[lines.size()][4];
					TrainThread [] ts = new TrainThread[lines.size()];
					for(int i = 0; i < lines.size(); i++){
						int c = 0;
						double [] Y = new double[ncols];
						double [][] Ynorm = new double[ncols][1];
						StringTokenizer st = new StringTokenizer(lines.get(i),",");
						while(st.hasMoreTokens()){
							Y[c] = Integer.parseInt(st.nextToken().trim());
							c++;
						}
						if(normalize == 1) {
							double[] norm = Stats.normalize(Y);
							for (int j = 0; j < norm.length; j++) {
								Ynorm[j][0] = norm[j];
							}
						} else {
							for (int j = 0; j < Y.length; j++) {
								Ynorm[j][0] = Y[j];
							}
						}
						//create a new thread to process YnormM
						CovSEEPNoiseiso cse = new CovSEEPNoiseiso(loghypers,4);
						GPChange gpc = new GPChange(cse);
						TrainThread t = new TrainThread(X,1,Ynorm,10,-1,gpc);
						ts[i] = t;t.t.start();
					}
					for(int i = 0; i < lines.size(); i++){
						try {
							ts[i].t.join();
							double[] lh = ts[i].getGpc().getCovFunc().getLogHypers();
							for(int j = 0; j < lh.length; j++) lhs[i][j] = lh[j];
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						for(int j = 0; j < lhs[i].length-1;j++)
							pWriter.printf("%f ", lhs[i][j]);
						pWriter.printf("%f\n", lhs[i][lhs[i].length-1]);
					}
					r += lines.size();
					if(r >= numrows) endflag = true;
				}
			}
			pWriter.close();
			bTrain.close();
			long end = System.currentTimeMillis();
			System.err.println("Computed hyper params for lines# "+startrownum+" to "+(startrownum + r - 1)+" in "+(end-start)/1000+" seconds.");
		} catch (FileNotFoundException e) {
			System.err.println("Training file not found - "+trainFileName);
			e.printStackTrace();
		}
	}

}
