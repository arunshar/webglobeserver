package edu.buffalo.gpchange.spark;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import edu.buffalo.webglobe.server.netcdf.NetcdfUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;
import scala.Tuple3;
import ucar.ma2.InvalidRangeException;

public class LoadNetCDF {

	private static class MapSection implements
			PairFunction<Tuple2<int[], float[][][]>, Tuple3<Integer, Integer, Integer>, ArrayList<ArrayList<Double>>> {
		private static final long serialVersionUID = -6570019203954336959L;

		@Override
		public Tuple2<Tuple3<Integer, Integer, Integer>, ArrayList<ArrayList<Double>>> call(
				Tuple2<int[], float[][][]> t) throws Exception {
			int timeLen = t._2.length;
			int latLen = t._2[0].length;
			int lonLen = t._2[0][0].length;

			Tuple3<Integer, Integer, Integer> key = new Tuple3<Integer, Integer, Integer>(t._1[0], t._1[1], t._1[2]);
			ArrayList<ArrayList<Double>> value = new ArrayList<ArrayList<Double>>();
			for (int i = 0; i < latLen; i++) {
				for (int j = 0; j < lonLen; j++) {
					ArrayList<Double> timeseries = new ArrayList<Double>();
					for (int k = 0; k < timeLen; k++) {
						timeseries.add((double) t._2[k][i][j]);
					}
				}
			}
			return new Tuple2<Tuple3<Integer, Integer, Integer>, ArrayList<ArrayList<Double>>>(key, value);
		}

	}

	private static class MapJoinTimeSeries implements
			Function<Tuple2<ArrayList<ArrayList<Double>>, ArrayList<ArrayList<Double>>>, ArrayList<ArrayList<Double>>> {
		private static final long serialVersionUID = 543438599369006300L;

		@Override
		public ArrayList<ArrayList<Double>> call(Tuple2<ArrayList<ArrayList<Double>>, ArrayList<ArrayList<Double>>> v1)
				throws Exception {
			ArrayList<ArrayList<Double>> value = new ArrayList<ArrayList<Double>>();
			for (int j = 0; j < v1._1.size(); j++) {
				ArrayList<Double> elem = new ArrayList<Double>();
				elem.addAll(v1._1.get(j));
				elem.addAll(v1._2.get(j));
				value.add(elem);
			}
			return value;
		}
	}

	public static void main(String[] args) throws IOException, URISyntaxException, InvalidRangeException {
		String hdfsuri = args[0];
		String inputDir = args[1];
		int d = Integer.parseInt(args[2]);

		// init
		final String varName = "tasmax";

		// read list of files in the input directory
		ArrayList<String> paths = NetcdfUtils.listPaths(hdfsuri, inputDir);

		// get spark context
		SparkConf conf = new SparkConf().setAppName("OnlineMonitor");
		JavaSparkContext sc = new JavaSparkContext(conf);

		// read data
		ArrayList<Tuple2<int[], float[][][]>> listSections = NetcdfUtils.partition(hdfsuri, paths.get(0), varName, d);
		JavaPairRDD<Tuple3<Integer, Integer, Integer>, ArrayList<ArrayList<Double>>> sectionsRDD = sc
				.parallelize(listSections, listSections.size()).mapToPair(new MapSection());

		System.out.println(sectionsRDD.count());
		
		for (int i = 1; i < paths.size(); i++) {
			ArrayList<Tuple2<int[], float[][][]>> listSectionsi = NetcdfUtils.partition(hdfsuri, paths.get(i), varName, d);
			JavaPairRDD<Tuple3<Integer, Integer, Integer>, ArrayList<ArrayList<Double>>> sectionsRDDi = sc
					.parallelize(listSectionsi, listSectionsi.size()).mapToPair(new MapSection());
			sectionsRDD = sectionsRDD.join(sectionsRDDi).mapValues(new MapJoinTimeSeries());
			System.out.println(sectionsRDD.count());
		}

		sc.close();
	}

}
