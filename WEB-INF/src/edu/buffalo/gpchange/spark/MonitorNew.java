package edu.buffalo.gpchange.spark;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import edu.buffalo.gpchange.CovSEEPNoiseiso;
import edu.buffalo.gpchange.EWMASmoother;
import edu.buffalo.gpchange.GPChange;
import edu.buffalo.gpchange.GPMonitor;
import edu.buffalo.webglobe.server.netcdf.NetCDFUtils;
import edu.buffalo.webglobe.server.netcdf.NetcdfColorMap;
import edu.buffalo.webglobe.server.utils.Printer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkFiles;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;
import scala.Tuple4;
import ucar.ma2.MAMath;

public class MonitorNew {
	

	private static double[][][] getAlarms(GPMonitor gpm, int[] shape, boolean ifExcludeNeighbor, int l) {
		double[][][] output = null;
		if (gpm != null) {
			if (!ifExcludeNeighbor)
				l = 0;

			output = new double[gpm.getZ().length][shape[0]-2*l][shape[1]-2*l];
			EWMASmoother smoother = new EWMASmoother();
			smoother.smooth(gpm.getZ(), 0.30, 0.20, 3, 2, 1);
			for (int k = 0; k < gpm.getZ().length; k++)
				for (int i = l; i < shape[0] - l; i++)
					for (int j = l; j < shape[1] - l; j++)
						output[k][i-l][j-l] = (smoother.getAlarms())[k][i * shape[1] + j];
		}
		return output;
	}

	public static void main(String[] args) throws IOException, URISyntaxException {
		final String hdfsuri = args[0];
		final String inputDir = args[1];
		final String chunksFile = args[2];
		final String logHypersFile = args[3];
		final String outputDir = args[4];
		final String colorFileName = args[5];
		final int numSlices = Integer.parseInt(args[6]);
		final int monitor_start_ind = Integer.parseInt(args[7]);
		final int monitor_end_ind = Integer.parseInt(args[8]);
		final int noYearTrain = Integer.parseInt(args[9]);
		final int monitor_method = Integer.parseInt(args[10]);
		final boolean ifSmoothSpacially = Integer.parseInt(args[11])!=0;
		final int k = Integer.parseInt(args[12]); //neighborhood extent

		SparkConf conf = new SparkConf().setAppName("GPChange");

		JavaSparkContext sc = new JavaSparkContext(conf);
		final String varName = "tasmax";
		ArrayList<String> paths = NetCDFUtils.listPaths(hdfsuri, inputDir);
		int[] dims = NetCDFUtils.getDimLens(hdfsuri, paths.get(0).toString());
		final int timeLen = dims[0];
		final int latLen = dims[1];
		final int longLen = dims[2];

		JavaRDD<Tuple2<Tuple2<int[], int[]>, double[][]>> chunksRdd = sc.objectFile(chunksFile, numSlices);
		JavaPairRDD<Tuple4<Integer,Integer,Integer,Integer>, double[][]> chunksPairRdd = chunksRdd
				.mapToPair(new PairFunction<Tuple2<Tuple2<int[],int[]>,double[][]>, Tuple4<Integer,Integer,Integer,Integer>, double[][]>() {

					/**
					 * 
					 */
					private static final long serialVersionUID = -2780747528783739363L;

					@Override
					public Tuple2<Tuple4<Integer, Integer, Integer, Integer>, double[][]> call(
							Tuple2<Tuple2<int[], int[]>, double[][]> t) throws Exception {
						Tuple4<Integer, Integer, Integer, Integer> key = 
								new Tuple4<Integer, Integer, Integer, Integer>(t._1._1[0], t._1._1[1], t._1._2[0], t._1._2[1]);
						
						return new Tuple2<Tuple4<Integer,Integer,Integer,Integer>, double[][]>(key, t._2);
					}
		});
		
		JavaRDD<Tuple2<Tuple2<int[], int[]>, double[]>> logHypersRdd = sc.objectFile(logHypersFile, numSlices);
		JavaPairRDD<Tuple4<Integer,Integer,Integer,Integer>, double[]> logHypersPairRdd = logHypersRdd
				.mapToPair(new PairFunction<Tuple2<Tuple2<int[],int[]>,double[]>, Tuple4<Integer,Integer,Integer,Integer>, double[]>() {

					/**
					 * 
					 */
					private static final long serialVersionUID = -7929807861332623473L;

					@Override
					public Tuple2<Tuple4<Integer, Integer, Integer, Integer>, double[]> call(
							Tuple2<Tuple2<int[], int[]>, double[]> t) throws Exception {
						Tuple4<Integer, Integer, Integer, Integer> key = 
								new Tuple4<Integer, Integer, Integer, Integer>(t._1._1[0], t._1._1[1], t._1._2[0], t._1._2[1]);
						
						return new Tuple2<Tuple4<Integer,Integer,Integer,Integer>, double[]>(key, t._2);
					}
		});
		
		JavaPairRDD<Tuple4<Integer, Integer, Integer, Integer>, Tuple2<double[][], double[]>> joinRdd = chunksPairRdd.join(logHypersPairRdd);
		
		JavaRDD<Tuple2<int[], double[][][]>> alarmsRdd = joinRdd
				.map(new Function<Tuple2<Tuple4<Integer,Integer,Integer,Integer>,Tuple2<double[][],double[]>>, Tuple2<int[], double[][][]>>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 8185845166037009277L;

			@Override
			public Tuple2<int[], double[][][]> call(
					Tuple2<Tuple4<Integer, Integer, Integer, Integer>, Tuple2<double[][], double[]>> v1)
					throws Exception {
				int[] origin = {v1._1._1(),v1._1._2()};
				int[] shape = {v1._1._3(),v1._1._4()};
				
				Printer.printArrayInt(origin);
				double[][] data = v1._2._1;
				double[] loghypers = v1._2._2;

				double [][] monitorData = new double [monitor_end_ind-monitor_start_ind+1][data[0].length];
				for (int i = 0; i < monitorData.length; i++) {
					for (int j = 0; j < monitorData[i].length; j++) {
						monitorData[i][j] = data[monitor_start_ind+i][j];
					}
				}
				
				CovSEEPNoiseiso cse = new CovSEEPNoiseiso(loghypers, loghypers.length);

				GPChange gpc = new GPChange(cse);
				int[] shape_s = shape.clone();
				if (ifSmoothSpacially) {
					shape_s[0] += 2 * k;
					shape_s[1] += 2 * k;
				}

				int omega = timeLen;
				int numSeries = shape[0] * shape[1];
				int trainLen = noYearTrain * timeLen;
				System.out.println("Monitoring...");
				GPMonitor gpm;
				switch (monitor_method) {
				case 1:
					gpm = gpc.monitorFast(monitorData, numSeries, omega, 0.0001, null, trainLen);
					break;
				case 2:
					gpm = gpc.monitorFastInc(monitorData, numSeries, omega, 0.0001, null, trainLen);
					break;

				default:
					try {
						gpm = gpc.monitor(monitorData, numSeries, omega, 0.0001, null, trainLen);
					} catch (Exception e) {
						System.out.println(e.getMessage());
						gpm = null;
					}
					break;
				}

				double[][][] output = getAlarms(gpm, shape_s, ifSmoothSpacially, k);
				return new Tuple2<int[], double[][][]>(origin, output);
			}
		});

		JavaRDD<String> creatingImageRDD = alarmsRdd.flatMapToPair(
				new PairFlatMapFunction<Tuple2<int[], double[][][]>, Integer, Tuple2<int[], double[][]>>() {

					private static final long serialVersionUID = -1423644074810958586L;

					@Override
					public Iterable<Tuple2<Integer, Tuple2<int[], double[][]>>> call(Tuple2<int[], double[][][]> e)
							throws Exception {
						int[] origin = { e._1[0], e._1[1] };
						double[][][] data = e._2;
						ArrayList<Tuple2<Integer, Tuple2<int[], double[][]>>> pairs = new ArrayList<Tuple2<Integer, Tuple2<int[], double[][]>>>();

						for (int i = noYearTrain*timeLen; i < data.length; ++i) {
							Integer key = new Integer(i + monitor_start_ind*timeLen);
							Tuple2<int[], double[][]> value = new Tuple2<int[], double[][]>(origin, data[i]);
							pairs.add(new Tuple2<Integer, Tuple2<int[], double[][]>>(key, value));
						}
						return pairs;
					}

				}).groupByKey().mapToPair(
						new PairFunction<Tuple2<Integer, Iterable<Tuple2<int[], double[][]>>>, Integer, double[][]>() {

							private static final long serialVersionUID = 7287648727669012190L;

							@Override
							public Tuple2<Integer, double[][]> call(
									Tuple2<Integer, Iterable<Tuple2<int[], double[][]>>> t) throws Exception {

								double[][] acc = new double[latLen][longLen];
								for (int i = 0; i < acc.length; ++i)
									for (int j = 0; j < acc[i].length; ++j)
										acc[i][j] = -1;

								for (Tuple2<int[], double[][]> e : t._2) {
									int[] origin = e._1;
									double[][] data = e._2;
									for (int i = 0; i < data.length; ++i)
										for (int j = 0; j < data[i].length; ++j) {
											acc[i + origin[0]][j + origin[1]] = data[i][j];
										}
								}

								return new Tuple2<Integer, double[][]>(t._1, acc);
							}
						})
				.map(new Function<Tuple2<Integer, double[][]>, String>() {

					private static final long serialVersionUID = 9005495294388288031L;

					@Override
					public String call(Tuple2<Integer, double[][]> e) throws Exception {

						double[][] data = e._2;
						int numLatitudes = data.length;
						int numLongitudes = data[0].length;

						MAMath.MinMax minMax = new MAMath.MinMax(-1, 2);

						NetcdfColorMap ncColormap = NetcdfColorMap.createColorMap("rgb_" + varName, (float) minMax.min,
								(float) minMax.max, SparkFiles.get(colorFileName));

						BufferedImage bufferedImage = new BufferedImage(numLongitudes, numLatitudes,
								BufferedImage.TYPE_INT_ARGB);
						int[] pixelArray = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();

						int cnt = 0;
						for (int i = 0; i < numLatitudes; i++) {
							for (int j = numLongitudes / 2; j < numLongitudes; j++) {
								double key = 0;
								try {
									key = data[i][j];
								} catch (ArrayIndexOutOfBoundsException e1) {
									e1.printStackTrace();
								}
								if (!Double.isNaN(key) && !Double.isInfinite(key))
									pixelArray[cnt] = ncColormap.getColor((double) key).getRGB();
								cnt++;
							}
							for (int j = 0; j < numLongitudes / 2; j++) {
								double key = 0;
								try {
									key = data[i][j];
								} catch (ArrayIndexOutOfBoundsException e1) {
									e1.printStackTrace();
								}
								if (!Double.isNaN(key) && !Double.isInfinite(key))
									pixelArray[cnt] = ncColormap.getColor((double) key).getRGB();
								cnt++;
							}
						}

						String filename = varName + "-" + e._1 + ".png";
						File f = new File("/tmp/" + filename);
						try {
							ImageIO.write(bufferedImage, "PNG", f);

						} catch (IOException exp) {
							exp.printStackTrace();
						}

						String hdfsOutputFile = outputDir + "/" + varName + "/" + filename;
						Path hdfsDest = new Path(hdfsOutputFile);
						FileSystem fsys = FileSystem.get(new URI(hdfsuri), new Configuration());
						if (fsys.exists(hdfsDest))
							fsys.delete(hdfsDest, false);
						fsys.copyFromLocalFile(false, new Path("/tmp/" + filename), hdfsDest);
						fsys.close();

						return hdfsOutputFile;
					}
				});

		System.out.println(creatingImageRDD.count());

		sc.close();
	}

}
