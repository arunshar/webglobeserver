package edu.buffalo.gpchange.spark;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import edu.buffalo.webglobe.server.netcdf.NetcdfColorMap;
import edu.buffalo.webglobe.server.netcdf.NetcdfDirectory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkFiles;

import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

public class Utils {

	public static String createAlarmImage(int[][] alarms, int time, String varName, String colorFileName,
			String hdfsuri, String outputDir) throws Exception {
		int numLatitudes = alarms.length;
		int numLongitudes = alarms[0].length;

		MAMath.MinMax minMax = new MAMath.MinMax(-1, 2);

		NetcdfColorMap ncColormap = NetcdfColorMap.createColorMap("rgb_" + varName, (float) minMax.min,
				(float) minMax.max, SparkFiles.get(colorFileName));

		BufferedImage bufferedImage = new BufferedImage(numLongitudes, numLatitudes, BufferedImage.TYPE_INT_ARGB);
		int[] pixelArray = ((DataBufferInt) bufferedImage.getRaster().getDataBuffer()).getData();

		int cnt = 0;
		for (int i = numLatitudes - 1; i >= 0; i--) {
			for (int j = numLongitudes / 2; j < numLongitudes; j++) {
				double key = 0;
				try {
					key = alarms[i][j];
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
					key = alarms[i][j];
				} catch (ArrayIndexOutOfBoundsException e1) {
					e1.printStackTrace();
				}
				if (!Double.isNaN(key) && !Double.isInfinite(key))
					pixelArray[cnt] = ncColormap.getColor((double) key).getRGB();
				cnt++;
			}
		}

		String filename = varName + "-" + time + ".png";
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

		return hdfsOutputFile;
	}

	public static String createNetCDFfile(int[][] data, int timeIndex, String varName, NetcdfDirectory inputDir,
			String outputDir) {
		String filename = inputDir.getDateFromIndex(timeIndex) + ".nc";
		String tempFile = "/tmp/" + filename;
		try {
			if (Files.exists(Paths.get(tempFile)))
				Files.delete(Paths.get(tempFile));
			
			NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, tempFile, null);
			Group rootGroup = writer.addGroup(null, "");

			Dimension timeDim = writer.addDimension(rootGroup, "time", 1);
			Dimension latDim = writer.addDimension(rootGroup, "lat", inputDir.getLatLen());
			Dimension lonDim = writer.addDimension(rootGroup, "lon", inputDir.getLonLen());

			ArrayList<Dimension> dims = new ArrayList<Dimension>();
			dims.add(timeDim);
			Variable time = writer.addVariable(rootGroup, "time", DataType.DOUBLE, dims);

			dims = new ArrayList<Dimension>();
			dims.add(latDim);
			Variable lat = writer.addVariable(rootGroup, "lat", DataType.FLOAT, dims);

			dims = new ArrayList<Dimension>();
			dims.add(lonDim);
			Variable lon = writer.addVariable(rootGroup, "lon", DataType.FLOAT, dims);

			dims = new ArrayList<Dimension>();
			dims.add(timeDim);
			dims.add(latDim);
			dims.add(lonDim);
			Variable changeDetection = writer.addVariable(rootGroup, "ChangeDetection", DataType.FLOAT, dims);

			int[] timeShape = { 1 };
			ArrayDouble timeArr = new ArrayDouble(timeShape);
			timeArr.setDouble(0, inputDir.getTimeFromInd(timeIndex));

			int[] cdShape = { 1, inputDir.getLatLen(), inputDir.getLonLen() };
			ArrayDouble changeDetectionArr = new ArrayDouble(cdShape);
			
			for (int i = 0; i < data.length; i++) {
				for (int j = 0; j < data[i].length; j++) {
					changeDetectionArr.setFloat(i*data[i].length + j, data[i][j]);
				}
			}

			writer.create();
			writer.write(time, timeArr);
			writer.write(lat, inputDir.getLatArr());
			writer.write(lon, inputDir.getLonArr());
			writer.write(changeDetection, changeDetectionArr);
			writer.close();

			String hdfsOutputFileName = outputDir + "/" + filename;
			Path filePath = new Path(hdfsOutputFileName);
			FileSystem fsys = FileSystem.get(new URI(outputDir), new Configuration());
			if (fsys.exists(filePath))
				fsys.delete(filePath, false);
			fsys.copyFromLocalFile(false, new Path(tempFile), filePath);

			return hdfsOutputFileName;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidRangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
}
