package edu.buffalo.gpchange.spark;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import edu.buffalo.gpchange.CovSEEPNoiseiso;
import edu.buffalo.gpchange.GPChange;
import edu.buffalo.webglobe.server.netcdf.NetcdfUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

public class TrainGlobalHypers {

	public static void main(String[] args) throws IOException, InvalidRangeException, URISyntaxException {
		final String hdfsuri = args[0];
		final String inputDir = args[1];
		final String logHypersFile = args[2];
		final int d = Integer.parseInt(args[3]);
		final int train_start_ind = Integer.parseInt(args[4]);
		final int train_end_ind = Integer.parseInt(args[5]);
		final int train_method = Integer.parseInt(args[6]);
		final int train_maxit = Integer.parseInt(args[7]);
		final String varName = "tasmax";
		final int yearLen = 365;

		ArrayList<String> paths = NetcdfUtils.listPaths(hdfsuri, inputDir);
		int[] dims = NetcdfUtils.getDimLens(hdfsuri, paths.get(0).toString());
		final int latLen = dims[1];
		final int longLen = dims[2];

		Tuple2<int[], ArrayList<int[]>> temp = NetcdfUtils.listOrigins(latLen, longLen, d);
		final int[] shape = temp._1;
		List<int[]> listOrigin = temp._2;

		System.out.println("Loading data...");
		double[][] data = new double[(train_end_ind - train_start_ind + 1) * yearLen][listOrigin.size()];

		for (int i = train_start_ind; i <= train_end_ind; i++) {

			NetcdfDataset dataset = NetcdfUtils.loadDFSNetCDFDataSet(hdfsuri, paths.get(i), 10000);
			NetcdfFile cdfFile = dataset.getReferencedFile();
			int[] shape_i = { yearLen, 1, 1 };
			for (int m = 0; m < listOrigin.size(); m++) {
				int[] origin_i = { 0, listOrigin.get(m)[0] + shape[0] / 2, listOrigin.get(m)[1] + shape[1] / 2 };
				Array src = cdfFile.findVariable(varName).read(origin_i, shape_i);
				float[][][] srcJavaArr = (float[][][]) src.copyToNDJavaArray();

				for (int l = 0; l < shape_i[0]; l++)
					data[(i - train_start_ind) * shape_i[0] + l][m] = srcJavaArr[l][0][0];
			}

			dataset.close();
		}

		double[] loghypers = { (double) Math.log(1), (double) Math.log(1), (double) Math.log(1), (double) Math.log(1) };
		CovSEEPNoiseiso cse = new CovSEEPNoiseiso(loghypers, loghypers.length);

		GPChange gpc = new GPChange(cse);
		int omega = yearLen;

		System.out.println("Training...");
		gpc.train(data, train_maxit, 1, train_method, -1, omega);

		ArrayList<double []> logHypersList = new ArrayList<double []>();
		logHypersList.add(gpc.getCovFunc().getLogHypers());
		
		SparkConf conf = new SparkConf().setAppName("Train");

		JavaSparkContext sc = new JavaSparkContext(conf);

		JavaRDD<double[]> logHypersRDD = sc.parallelize(logHypersList);
		logHypersRDD.saveAsObjectFile(logHypersFile);
		
		sc.close();
		
	}

}
