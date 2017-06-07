package edu.buffalo.gpchange.spark;

import java.io.IOException;
import java.net.URISyntaxException;

import edu.buffalo.gpchange.CovSEEPNoiseiso;
import edu.buffalo.gpchange.GPChange;
import edu.buffalo.webglobe.server.utils.Printer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

import scala.Tuple2;
import ucar.ma2.InvalidRangeException;

public class TrainHypersNew {

	public static void main(String[] args) throws IOException, InvalidRangeException, URISyntaxException {

		final String chunksFile = args[0];
		final String logHypersFile = args[1];
		final int numSlices = Integer.parseInt(args[2]);
		final int train_start_ind = Integer.parseInt(args[3]);
		final int train_end_ind = Integer.parseInt(args[4]);
		final int train_method = Integer.parseInt(args[5]);
		final int train_maxit = Integer.parseInt(args[6]);
		
		SparkConf conf = new SparkConf().setAppName("GPChange");

		JavaSparkContext sc = new JavaSparkContext(conf);
		
		JavaRDD<Tuple2<Tuple2<int[],int[]>, double[][]>> chunksRdd = sc.objectFile(chunksFile,numSlices);
		
		JavaRDD<Tuple2<Tuple2<int[],int[]>, double[]>> logHypersRdd = chunksRdd
				.map(new Function<Tuple2<Tuple2<int[],int[]>, double[][]>, Tuple2<Tuple2<int[],int[]>, double[]>>() {

					private static final long serialVersionUID = -1646630789792520423L;

					@Override
					public Tuple2<Tuple2<int[],int[]>, double[]> call(Tuple2<Tuple2<int[],int[]>, double[][]> vl) throws Exception {
						Printer.printArrayInt(vl._1._1);
					
						int yearLen = 365;
						double[][] data = vl._2;
						double[] loghypers = { (double) Math.log(1), (double) Math.log(1), (double) Math.log(1),
								(double) Math.log(1) };
						CovSEEPNoiseiso cse = new CovSEEPNoiseiso(loghypers, loghypers.length);

						GPChange gpc = new GPChange(cse);
						int omega = yearLen;

						System.out.println("Training...");
//						double [][] trainData = new double [train_end_ind-train_start_ind+1][data[0].length];
//						for (int i = 0; i < trainData.length; i++) {
//							for (int j = 0; j < trainData[i].length; j++) {
//								trainData[i][j] = data[train_start_ind+i][j];
//							}
//						}
						double [][] trainData = new double [train_end_ind-train_start_ind+1][1];
						for (int i = 0; i < trainData.length; i++) {
							trainData[i][0] = data[train_start_ind+i][data[0].length/2];
						}
						
						gpc.train(trainData, train_maxit, 1, train_method, -1, omega); 
						
						Tuple2<int[],int[]> location = vl._1;
						
						return new Tuple2<Tuple2<int[],int[]>, double[]>(location, gpc.getCovFunc().getLogHypers());
					}
				});		
		
		logHypersRdd.saveAsObjectFile(logHypersFile);
				
		sc.close();
	}

}
