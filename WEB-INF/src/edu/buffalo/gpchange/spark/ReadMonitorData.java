package edu.buffalo.gpchange.spark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import edu.buffalo.gpchange.EWMASmoother;
import edu.buffalo.gpchange.GPMonitor;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;


import scala.Tuple4;

public class ReadMonitorData {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		final String monitorDataFile = args[0];
		final String outputFile = args[1];

		// initialize
		SparkConf conf = new SparkConf().setAppName("ReadMonitorDataFile");
		JavaSparkContext sc = new JavaSparkContext(conf);

		JavaRDD<Tuple4<int[], int[][][], EWMASmoother, GPMonitor>> alarmsRdd = sc.objectFile(monitorDataFile);

		File file = new File(outputFile);
		try {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file));
//			List<Tuple2<Tuple2<int[], int[]>, double[]>> listLogHypers = logHypersRdd.collect();
//			for (Tuple2<Tuple2<int[], int[]>, double[]> el : listLogHypers) {
//				writer.write(el._1._1[0] + "," + el._1._1[1]  + "," + el._2[0] + "," + el._2[1] + "," + el._2[2] + "," + el._2[3] + "\n");
//			}
			Tuple4<int[], int[][][], EWMASmoother, GPMonitor> el = alarmsRdd.collect().get(1);
			GPMonitor gpm = el._4();
			EWMASmoother smoother = el._3();
			writer.write("Y,Yhat,Vhat,Scorelow,Scorehigh,Alarms\n");
			int n = gpm.getYcorr().length;
			for (int i = 0; i < n; i++) {
				writer.write(gpm.getYcorr()[i][0] + "," + gpm.getYhat()[i][0] + "," + gpm.getVhat()[i] + "," + 
						smoother.getScoresLow()[i][0] + "," + smoother.getScoresHigh()[i][0] + "," + smoother.getAlarms()[i][0] + "\n");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		sc.close();
	}
}
