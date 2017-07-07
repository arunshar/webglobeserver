package edu.buffalo.gpchange.spark;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import edu.buffalo.gpchange.CovSEEPNoiseiso;
import edu.buffalo.gpchange.GPChange;
import edu.buffalo.webglobe.server.netcdf.NetcdfUtils;
import edu.buffalo.webglobe.server.utils.Printer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.broadcast.Broadcast;

import scala.Tuple2;
import ucar.ma2.InvalidRangeException;

public class TrainHypers {
	public static void main(String[] args) throws IOException, InvalidRangeException, URISyntaxException {
		final String hdfsuri = args[0];
		final String inputDir = args[1];
		final String logHypersFile = args[2];
		final int numSlices = Integer.parseInt(args[3]);
		final int d = Integer.parseInt(args[4]);
		final int train_start_ind = Integer.parseInt(args[5]);
		final int train_end_ind = Integer.parseInt(args[6]);
		final int train_method = Integer.parseInt(args[7]);
		final int train_maxit = Integer.parseInt(args[8]);

		// get list of netcdf files
		ArrayList<String> paths = NetcdfUtils.listPaths(hdfsuri, inputDir);
		ArrayList<String> trainpaths = new ArrayList<String>();
		for (int i = train_start_ind; i <= train_end_ind; i++) {
			trainpaths.add(paths.get(i));
		}

		// get dimensions of netCDF files
		int[] dims = NetcdfUtils.getDimLens(hdfsuri, paths.get(train_start_ind).toString());
		final int timeLen = dims[0];
		final int latLen = dims[1];
		final int longLen = dims[2];

		// divide the world map into blocks
		Tuple2<int[], ArrayList<int[]>> temp = NetcdfUtils.listOrigins(latLen, longLen, d);
		final int[] shape = temp._1;
		List<int[]> listOrigin = temp._2;

		// List<int[]> listOrigin = new ArrayList<>();
		// int[] selectedSections = {7000,7001,6880,6881};
		// for (int i = 0; i < selectedSections.length; i++) {
		// listOrigin.add(temp._2.get(selectedSections[i]));
		// }

		// initialize
		SparkConf conf = new SparkConf().setAppName("Train");
		JavaSparkContext sc = new JavaSparkContext(conf);
		final Broadcast<ArrayList<String>> brPaths = sc.broadcast(trainpaths);
		final String varName = "tasmax";

		// Training...
		JavaRDD<Tuple2<Tuple2<int[], int[]>, double[]>> logHypersRdd = sc.parallelize(listOrigin, numSlices)
				.map(new Function<int[], Tuple2<Tuple2<int[], int[]>, double[]>>() {

					private static final long serialVersionUID = -1646630789792520423L;

					@Override
					public Tuple2<Tuple2<int[], int[]>, double[]> call(int[] origin) throws Exception {
						int[] origin_i = { 0, origin[0], origin[1] };
						int[] shape_i = { timeLen, shape[0], shape[1] };

						List<String> arrPaths = brPaths.getValue();

						// load data
                        Printer.printArrayInt(origin);
						double[][] data = NetcdfUtils.getDataSafe(hdfsuri, arrPaths, varName, origin_i, shape_i);

						// training
						System.out.println("Training...");
						double[] loghypers = { (double) Math.log(1), (double) Math.log(1), (double) Math.log(1),
								(double) Math.log(1) };
						CovSEEPNoiseiso cse = new CovSEEPNoiseiso(loghypers, loghypers.length);

						GPChange gpc = new GPChange(cse);
						int omega = timeLen;

						gpc.train(data, train_maxit, 1, train_method, -1, omega);

						Tuple2<int[], int[]> location = new Tuple2<int[], int[]>(origin, shape);
						
						return new Tuple2<Tuple2<int[], int[]>, double[]>(location, gpc.getCovFunc().getLogHypers());
					}
				});

		// save logHypers
		logHypersRdd.saveAsObjectFile(logHypersFile);
				
		sc.close();
	}
}
