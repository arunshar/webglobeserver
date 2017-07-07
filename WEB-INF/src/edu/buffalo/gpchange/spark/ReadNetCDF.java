package edu.buffalo.gpchange.spark;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import edu.buffalo.webglobe.server.netcdf.NetcdfUtils;
import edu.buffalo.webglobe.server.utils.Printer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.broadcast.Broadcast;

import scala.Tuple2;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

public class ReadNetCDF {

	public static void main(String[] args) throws IOException, InvalidRangeException, URISyntaxException {

		final String hdfsuri = args[0];
		final String inputDir = args[1];
		final String chunksFile = args[2];
		final int numSlices = Integer.parseInt(args[3]);
		final int d = Integer.parseInt(args[4]);
		
		ArrayList<String> paths = NetcdfUtils.listPaths(hdfsuri, inputDir);
		int[] dims = NetcdfUtils.getDimLens(hdfsuri, paths.get(0).toString());
		final int latLen = dims[1];
		final int longLen = dims[2];

		Tuple2<int[], ArrayList<int[]>> temp = NetcdfUtils.listOrigins(latLen, longLen, d);
		final int[] shape = temp._1;
		List<int[]> listOrigin = temp._2.subList(0, 1);

		SparkConf conf = new SparkConf().setAppName("GPChange");

		JavaSparkContext sc = new JavaSparkContext(conf);
		final Broadcast<ArrayList<String>> brPaths = sc.broadcast(paths);
		final String varName = "tasmax";
		
		JavaRDD<Tuple2<Tuple2<int[],int[]>, double[][]>> chunksRdd = sc.parallelize(listOrigin, numSlices)
				.map(new Function<int[], Tuple2<Tuple2<int[],int[]>, double[][]>>() {

					private static final long serialVersionUID = -1646630789792520423L;

					@Override
					public Tuple2<Tuple2<int[],int[]>, double[][]> call(int[] origin) throws Exception {
						Printer.printArrayInt(origin);

						int timeLen = 365; //need to revise
						int[] origin_i = {0, origin[0], origin[1]};
						int[] shape_i = {timeLen, shape[0], shape[1]};
						
						
						ArrayList<String> arrPaths = brPaths.getValue();
						double[][] data = new double[shape_i[0]*arrPaths.size()][shape_i[1] * shape_i[2]];
						
						for (int i = 0; i < arrPaths.size(); i++) {
							NetcdfDataset dataset = NetcdfUtils.loadDFSNetCDFDataSet(hdfsuri, arrPaths.get(i), 10000);
							NetcdfFile cdfFile = dataset.getReferencedFile();
							Array src = cdfFile.findVariable(varName).read(origin_i, shape_i);
							float[][][] srcJavaArr = (float[][][]) src.copyToNDJavaArray();

							for (int l = 0; l < shape_i[0]; l++)
								for (int j = 0; j < shape_i[1]; j++)
									for (int k = 0; k < shape_i[2]; k++) {
										data[i * shape_i[0] + l][j * shape_i[2] + k] = srcJavaArr[l][j][k];
									}

							dataset.close();
						}

						Tuple2<int[],int[]> location = new Tuple2<int[], int[]>(origin, shape);
						
						return new Tuple2<Tuple2<int[],int[]>, double[][]>(location, data);
					}
				});		
		
		chunksRdd.saveAsObjectFile(chunksFile);
				
		sc.close();
	}


}
