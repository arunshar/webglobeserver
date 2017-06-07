package edu.buffalo.gpchange.spark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

public class ReadLogHypersFile {

	public static void main(String[] args) {
		final String logHypersFile = args[0];

		// initialize
		SparkConf conf = new SparkConf().setAppName("ReadLogHypersFile");
		JavaSparkContext sc = new JavaSparkContext(conf);

		// get the trained hyper-parameters
		JavaRDD<double[]> logHypersRdd = sc.objectFile(logHypersFile);

		File file = new File("/home/dtran/Work/Aristotle/globalLogHypersFile.csv");
		try {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file));
//			List<Tuple2<Tuple2<int[], int[]>, double[]>> listLogHypers = logHypersRdd.collect();
//			for (Tuple2<Tuple2<int[], int[]>, double[]> el : listLogHypers) {
//				writer.write(el._1._1[0] + "," + el._1._1[1]  + "," + el._2[0] + "," + el._2[1] + "," + el._2[2] + "," + el._2[3] + "\n");
//			}
			List<double[]> listLogHypers = logHypersRdd.collect();
			for (double[] el : listLogHypers) {
				writer.write(el[0] + "," + el[1] + "," + el[2] + "," + el[3] + "\n");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		sc.close();
	}

}
