package edu.buffalo.gpchange.spark;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import edu.buffalo.gpchange.*;
import edu.buffalo.webglobe.server.netcdf.NetcdfUtils;
import edu.buffalo.webglobe.server.netcdf.NetcdfDirectory;
import edu.buffalo.webglobe.server.utils.Printer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.broadcast.Broadcast;


import scala.Tuple2;
import scala.Tuple4;

public class MonitorGlobalHypers {

	private static Tuple2<int[][][],EWMASmoother> getAlarms(GPMonitor gpm, int[] shape, boolean ifSmoothSpacially, int l) {
		int[][][] output = null;
		EWMASmoother smoother = new EWMASmoother();
		
		if (gpm != null) {
			smoother.smooth(gpm.getZ(), 0.30, 0.20, 7, 6, 1);

			output = new int[gpm.getZ().length][shape[0] - 2 * l][shape[1] - 2 * l];
			for (int k = 0; k < gpm.getZ().length; k++) {
				int[][] alarmData = new int[shape[0]][shape[1]];
				for (int i = 0; i < shape[0]; i++)
					for (int j = 0; j < shape[1]; j++)
						alarmData[i][j] = (smoother.getAlarms())[k][i * shape[1] + j];

				if (ifSmoothSpacially)
					alarmData = SpatialSmoother.smooth(alarmData, l, 1, 0.7);
				else
					l = 0;

				for (int i = l; i < shape[0] - l; i++)
					for (int j = l; j < shape[1] - l; j++)
						output[k][i - l][j - l] = alarmData[i][j];
			}
		}
		
		return new Tuple2<int[][][], EWMASmoother>(output, smoother);
	}

	public static void main(String[] args) throws IOException, URISyntaxException {

		final String hdfsuri = args[0];
		final String inputDir = args[1];
		final String logHypersFile = args[2];
		final String outputDir = args[3];
		final String monitorDataFile = args[4];
		final String colorFileName = args[5];
		final int numSlices = Integer.parseInt(args[6]);
		final int d = Integer.parseInt(args[7]);
		final int monitor_start_ind = Integer.parseInt(args[8]);
		final int monitor_end_ind = Integer.parseInt(args[9]);
		final double noYearTrain = Double.parseDouble(args[10]);
		final int monitor_method = Integer.parseInt(args[11]);
		final boolean ifSmoothSpacially = Integer.parseInt(args[12]) != 0;
		final int k = Integer.parseInt(args[13]); // neighborhood extent
		
		ArrayList<String> paths = NetcdfUtils.listPaths(hdfsuri, inputDir);
		int[] dims = NetcdfUtils.getDimLens(hdfsuri, paths.get(0).toString());
		final int timeLen = dims[0];
		final int latLen = dims[1];
		final int longLen = dims[2];

		SparkConf conf = new SparkConf().setAppName("MonitorGlobalHypers");
		JavaSparkContext sc = new JavaSparkContext(conf);
		final Broadcast<ArrayList<String>> brPaths = sc.broadcast(paths);
		final String varName = "tasmax";

		JavaRDD<double[]> logHypersRdd = sc.objectFile(logHypersFile);
		final double[] loghypers = logHypersRdd.first();

		Tuple2<int[], ArrayList<int[]>> temp = NetcdfUtils.listOrigins(latLen, longLen, d);
		final int[] shape = temp._1;
		List<int[]> listOrigin = temp._2;
		//List<int[]> listOrigin = temp._2.subList(60, 62);

		JavaRDD<Tuple4<int[], int[][][], EWMASmoother, GPMonitor>> alarmsRdd = sc.parallelize(listOrigin, numSlices)
				.map(new Function<int[], Tuple4<int[], int[][][], EWMASmoother, GPMonitor>>() {

					private static final long serialVersionUID = -1646630789792520423L;

					@Override
					public Tuple4<int[], int[][][], EWMASmoother, GPMonitor> call(int[] origin) throws Exception {
						Printer.printArrayInt(origin);
						List<String> arrPaths = brPaths.getValue().subList(monitor_start_ind, monitor_end_ind + 1);

						int[] origin_i = { 0, origin[0], origin[1] };
						int[] shape_i = { timeLen, shape[0], shape[1] };
						double[][] data = null;
						if (ifSmoothSpacially)
							data = NetcdfUtils.getDataWithNeighbor(hdfsuri, arrPaths, varName, latLen, longLen,
                                    origin_i, shape_i, k);
						else
							data = NetcdfUtils.getDataSafe(hdfsuri, arrPaths, varName, origin_i, shape_i);

						CovSEEPNoiseiso cse = new CovSEEPNoiseiso(loghypers, loghypers.length);

						GPChange gpc = new GPChange(cse);
						int[] shape_s = shape.clone();
						if (ifSmoothSpacially) {
							shape_s[0] += 2 * k;
							shape_s[1] += 2 * k;
						}
						int omega = timeLen;
						int numSeries = shape_s[0] * shape_s[1];
						int trainLen = (int) (noYearTrain * timeLen);
						System.out.println("Monitoring...");
						GPMonitor gpm;
						switch (monitor_method) {
						case 1:
							System.out.println("Fast...");
							gpm = gpc.monitorFast(data, numSeries, omega, 0.0001, null, trainLen);
							break;
						case 2:
							System.out.println("Fast Inc...");
							gpm = gpc.monitorFastInc1(data, numSeries, omega, 0.0001, null, trainLen);
							break;

						default:
							try {
								System.out.println("...");
								gpm = gpc.monitor(data, numSeries, omega, 0.0001, null, trainLen);
							} catch (Exception e) {
								System.out.println(e.getMessage());
								gpm = null;
							}
							break;
						}
						System.out.println("Done monitoring...");

						 Tuple2<int[][][],EWMASmoother>  output = getAlarms(gpm, shape_s, ifSmoothSpacially, k);
						return new Tuple4<int[], int[][][], EWMASmoother, GPMonitor>(origin, output._1, output._2, gpm);
					}
				});

		// System.out.println(alarmsRdd.count());
		//alarmsRdd.saveAsObjectFile(monitorDataFile);

		JavaRDD<String> creatingImageRDD = alarmsRdd
				.flatMapToPair(new PairFlatMapFunction<Tuple4<int[], int[][][], EWMASmoother, GPMonitor>, Integer, Tuple2<int[], int[][]>>() {

					private static final long serialVersionUID = -1423644074810958586L;

					@Override
					public Iterable<Tuple2<Integer, Tuple2<int[], int[][]>>> call(Tuple4<int[], int[][][], EWMASmoother, GPMonitor> e)
							throws Exception {
						int[] origin = { e._1()[0], e._1()[1] };
						int[][][] data = e._2();
						ArrayList<Tuple2<Integer, Tuple2<int[], int[][]>>> pairs = new ArrayList<Tuple2<Integer, Tuple2<int[], int[][]>>>();

						for (int i = (int) (noYearTrain * timeLen); i < data.length; ++i) {
							Integer key = new Integer(i + monitor_start_ind * timeLen);
							Tuple2<int[], int[][]> value = new Tuple2<int[], int[][]>(origin, data[i]);
							pairs.add(new Tuple2<Integer, Tuple2<int[], int[][]>>(key, value));
						}
						return pairs;
					}

				}).groupByKey()
				.mapToPair(new PairFunction<Tuple2<Integer, Iterable<Tuple2<int[], int[][]>>>, Integer, int[][]>() {

					private static final long serialVersionUID = 7287648727669012190L;

					@Override
					public Tuple2<Integer, int[][]> call(Tuple2<Integer, Iterable<Tuple2<int[], int[][]>>> t)
							throws Exception {

						int[][] acc = new int[latLen][longLen];
						for (int i = 0; i < acc.length; ++i)
							for (int j = 0; j < acc[i].length; ++j)
								acc[i][j] = -1;

						for (Tuple2<int[], int[][]> e : t._2) {
							int[] origin = e._1;
							int[][] data = e._2;
							for (int i = 0; i < data.length; ++i)
								for (int j = 0; j < data[i].length; ++j) {
									acc[i + origin[0]][j + origin[1]] = data[i][j];
								}
						}

						return new Tuple2<Integer, int[][]>(t._1, acc);
					}
				}).map(new Function<Tuple2<Integer, int[][]>, String>() {

					private static final long serialVersionUID = 9005495294388288031L;

					@Override
					public String call(Tuple2<Integer, int[][]> e) throws Exception {
						int time = e._1;
						int[][] alarms = e._2;
						//return Utils.createAlarmImage(alarms, time, varName, colorFileName, hdfsuri, outputDir);
						
						NetcdfDirectory netcdfInputDir = new NetcdfDirectory("hdfs",hdfsuri, inputDir);
						
						return Utils.createNetCDFfile(alarms, time, "ChangeDetection", netcdfInputDir, outputDir);
					}
				});

		System.out.println(creatingImageRDD.count());

		sc.close();
	}

}
