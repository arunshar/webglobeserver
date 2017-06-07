package edu.buffalo.webglobe.server.netcdf;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import edu.buffalo.webglobe.server.utils.Constants;
import edu.buffalo.webglobe.server.utils.Utils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.io.IOUtils;
import scala.Tuple2;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

import edu.buffalo.webglobe.server.spark.HDFSRandomAccessFile;

public class NetCDFUtils {
	/**
	 * Converts the native ma2.Array from the NetCDF library to a one
	 * dimensional Java Array of Doubles.
	 *
	 * Two copies of the array are made, since NetCDF does not have any API to
	 * tell what type the arrays are. Once the initial array of the loading is
	 * completed, a type check is used to appropriately convert the values into
	 * doubles. This involves a second copy.
	 */
	public static double[] convertMa2Arrayto1DJavaArray(Array ma2Array) {

		double[] array = null;
		// First copy of array
		Object javaArray = ma2Array.copyTo1DJavaArray();

		try {
			// Second copy of Array
			if (!(javaArray instanceof double[])) {
				float[] farray = (float[]) javaArray;
				array = new double[farray.length];
				for (int i = 0; i < farray.length; i++)
					array[i] = (double) farray[i];
			} else {
				array = (double[]) javaArray;
			}
			for (int i = 0; i < array.length; i++)
				if (array[i] == -9999.0)
					array[i] = 0.0;
		} catch (Exception ex) {
			Utils.logger.severe(
                    "Error while converting a netcdf.ucar.ma2 to a 1D array. Most likely occurred with casting");
		}

		return array;
	}

    /**
     * Loads a NetCDF Dataset from HTTP URL
     */
    public static NetcdfDataset loadHTTPNetcdfDataSet(String url, String target){
        NetcdfDataset.setUseNaNs(false);

        try{
            return new NetcdfDataset(NetcdfFile.open(url+"/"+target));
        } catch (IOException e) {
            Utils.logger.severe(("Couldn't open dataset " +url+"/"+target));        }
        return null;
    }
	/**
	 * Loads a NetCDF Dataset from HDFS.
	 *
     * @param dfsUri
     *            HDFS URI(eg. hdfs://master:9000/)
     * @param location
*            File path on HDFS
     * @param bufferSize
     */
	public static NetcdfDataset loadDFSNetCDFDataSet(String dfsUri, String location, int bufferSize) {
		NetcdfDataset.setUseNaNs(false);

		try {
			HDFSRandomAccessFile raf = new HDFSRandomAccessFile(dfsUri, location, bufferSize);
			return (new NetcdfDataset(NetcdfFile.open(raf, location, null, null)));
		} catch (IOException ex) {
			Utils.logger.severe(("Cannot open dataset " + dfsUri + "" + location));
			return null;
		} catch (Exception ex) {
            Utils.logger.severe(("Cannot open dataset " + dfsUri + "" + location));
            return null;
		}
	}

	/**
	 * Return a list of paths of files in a HDFS directory.
	 *
	 * @param dfsuri
	 *            HDFS URI(eg. hdfs://master:9000/)
	 * @param dir
	 *            Directory path on HDFS
	 */
	public static ArrayList<String> listPaths(String dfsuri, String dir) throws IOException, URISyntaxException {

		FileSystem fs = FileSystem.get(new URI(dfsuri), new Configuration());
		Path dirPath = new Path(dir);
		FileStatus[] fstatus = fs.listStatus(dirPath);
        ArrayList<String> paths = new ArrayList<String>();

		for (int i = 0; i < fstatus.length; i++)
			paths.add(fstatus[i].getPath().toString());

		return paths;
	}

    public static String copyRemoteFileToHDFS(String uri, String dir) throws IOException, URISyntaxException {
        //first copy the file to local directory
        URL url = new URL(uri);
        String [] tokens = Utils.parseURL(uri);
        String localTarget = Constants.LOCAL_TMP_DIRECTORY+'/'+tokens[2];
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(localTarget);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        //next copy the local file to HDFS
        FileSystem fs = FileSystem.get(new URI(Constants.HDFS_SERVER), new Configuration());
        Path dirPath = new Path(Constants.HDFS_BASEDIR+"/"+dir);
        fs.mkdirs(dirPath);
        Path destPath = new Path(Constants.HDFS_BASEDIR+"/"+dir+tokens[2]);
        OutputStream os = fs.create(destPath);
        InputStream is = new BufferedInputStream(new FileInputStream(localTarget));
        IOUtils.copyBytes(is, os, 4096, false);
        //finally delete the local file
        File localFile = new File(localTarget);
        localFile.delete();
        return Constants.HDFS_BASEDIR+"/"+dir+tokens[2];
    }

	public static int[] getDimLens(String hdfsuri, String filePath) throws IOException {
		NetcdfDataset dataset = NetCDFUtils.loadDFSNetCDFDataSet(hdfsuri, filePath, 10000);

		NetcdfFile cdfFile = dataset.getReferencedFile();

		List<Dimension> dims = cdfFile.getDimensions();

		int[] dimLens = new int[dims.size()];
		for (int i = 0; i < dimLens.length; i++) {
			dimLens[i] = dims.get(i).getLength();
		}
		dataset.close();

		return dimLens;
	}


    public static String extractInfo(NetcdfDataset dataset) {
        for(String name: Constants.VALID_DESCRIPTION_TAGS){
            if(dataset.findGlobalAttributeIgnoreCase(name) != null)
                return dataset.findGlobalAttributeIgnoreCase(name).getStringValue();
        }
        return null;
    }

    public static String extractTitle(NetcdfDataset dataset) {
        for(String name: Constants.VALID_TITLE_TAGS){
            if(dataset.findGlobalAttributeIgnoreCase(name) != null)
                return dataset.findGlobalAttributeIgnoreCase(name).getStringValue();
        }
        return null;
    }

    public static String extractInfoURL(NetcdfDataset dataset) {
        if(dataset.findGlobalAttributeIgnoreCase("references") != null) {
            String infoURL = dataset.findGlobalAttributeIgnoreCase("references").getStringValue();
            if(infoURL.startsWith("http")||(infoURL.startsWith("www"))){
                return infoURL;
            }
        }
        return null;
    }

    public static Tuple2<int[], ArrayList<int[]>> listOrigins(int latLen, int lonLen, int d) throws IOException {

        ArrayList<int[]> listOrigins = new ArrayList<int[]>();
        int[] shape = { latLen / d, lonLen / (2 * d) };
        for (int i = 0; i < d; i++)
            for (int j = 0; j < 2 * d; j++) {
                int[] origins = { i * shape[0], j * shape[1] };
                listOrigins.add(origins);
            }

        return new Tuple2<int[], ArrayList<int[]>>(shape, listOrigins);
    }

    public static ArrayList<Tuple2<int[], float[][][]>> partition(String hdfsuri, String path, String varName, int d)
            throws IOException, InvalidRangeException {

        NetcdfDataset dataset = NetCDFUtils.loadDFSNetCDFDataSet(hdfsuri, path, 10000);
        NetcdfFile cdfFile = dataset.getReferencedFile();
        Array src = cdfFile.findVariable(varName).read();
        dataset.close();

        int timeLen = src.getShape()[0];
        int latLen = src.getShape()[1];
        int lonLen = src.getShape()[2];

        Tuple2<int[], ArrayList<int[]>> tuple = NetCDFUtils.listOrigins(latLen, lonLen, d);
        final int[] shape = { timeLen, tuple._1[0], tuple._1[1] };
        ArrayList<int[]> listOrigins = tuple._2;

        ArrayList<Tuple2<int[], float[][][]>> listSections = new ArrayList<Tuple2<int[], float[][][]>>();
        for (int[] origin : listOrigins) {
            int[] curOri = { 0, origin[0], origin[1] };
            float[][][] section = (float[][][]) src.section(curOri, shape).copyToNDJavaArray();

            listSections.add(new Tuple2<int[], float[][][]>(curOri, section));
        }

        return listSections;
    }

    public static double[][] getDataSafe(String hdfsuri, String path, String varName, int[] origin, int[] shape)
            throws IOException, InvalidRangeException {
        double[][] data = new double[shape[0]][shape[1] * shape[2]];
        NetcdfDataset dataset = NetCDFUtils.loadDFSNetCDFDataSet(hdfsuri, path, 10000);
        NetcdfFile cdfFile = dataset.getReferencedFile();
        Array src = cdfFile.findVariable(varName).read(origin, shape);

        for (int l = 0; l < shape[0]; l++)
            for (int j = 0; j < shape[1]; j++)
                for (int k = 0; k < shape[2]; k++) {
                    data[l][j * shape[2] + k] = src.getFloat(k + j * shape[2] + l * shape[1] * shape[2]);
                }

        dataset.close();
        return data;
    }

    public static double[][] getDataSafe(String hdfsuri, List<String> paths, String varName, int[] origini,
                                         int[] shapei) throws IOException, InvalidRangeException {
        int[] origin = { 0, 0, 0 };
        int[] shape = { shapei[0] * paths.size(), shapei[1], shapei[2] };
        double[][] data = new double[shape[0]][shape[1] * shape[2]];

        for (int i = 0; i < paths.size(); i++) {
            origin[0] = shapei[0] * i;
            double[][] datai = getDataSafe(hdfsuri, paths.get(i), varName, origini, shapei);
            copy(data, origin, shape, datai, shapei);
        }
        return data;
    }

    public static void copy(double[][] dataDest, int[] origin, int[] shapeDest, double[][] dataSrc, int[] shapeSrc) {
        for (int i = 0; i < shapeSrc[0]; i++) {
            for (int j = 0; j < shapeSrc[1]; j++) {
                for (int k = 0; k < shapeSrc[2]; k++) {
                    dataDest[i + origin[0]][(j + origin[1]) * shapeDest[2] + k + origin[2]] = dataSrc[i][j * shapeSrc[2]
                            + k];
                }
            }
        }
    }

    public static double[][] getData(String hdfsuri, String path, String var, int row, int col, int[] o, int[] s)
            throws IOException, InvalidRangeException {
        if (o[2] < 0) {
            if (o[2] + s[2] <= 0) {
                int[] ot = o.clone();
                ot[2] = ot[2] + col;
                double[][] data = getData(hdfsuri, path, var, row, col, ot, s);
                return data;

            } else {
                double[][] data = new double[s[0]][s[1] * s[2]];
                int[] ot = o.clone();
                ot[2] = 0;
                int[] st = s.clone();
                st[2] = s[2] + o[2];
                double[][] dt = getData(hdfsuri, path, var, row, col, ot, st);
                int[] od = { 0, 0, -o[2] };
                copy(data, od, s, dt, st);

                ot[2] = col + o[2];
                st[2] = -o[2];
                dt = getData(hdfsuri, path, var, row, col, ot, st);
                od[2] = 0;
                copy(data, od, s, dt, st);
                return data;
            }
        } else if (o[2] + s[2] > col) {
            if (o[2] >= col) {
                int[] ot = o.clone();
                ot[2] = ot[2] % col;
                double[][] data = getData(hdfsuri, path, var, row, col, ot, s);
                return data;

            } else {
                double[][] data = new double[s[0]][s[1] * s[2]];
                int[] ot = o.clone();
                int[] st = s.clone();
                st[2] = col - ot[2];
                double[][] dt = getData(hdfsuri, path, var, row, col, ot, st);
                int[] od = { 0, 0, 0 };
                copy(data, od, s, dt, st);

                ot[2] = 0;
                st[2] = (o[2] + s[2]) - col;
                dt = getData(hdfsuri, path, var, row, col, ot, st);
                od[2] = col - o[2];
                copy(data, od, s, dt, st);
                return data;
            }

        } else if (o[1] < 0) {
            double[][] data = new double[s[0]][s[1] * s[2]];
            int[] ot = o.clone();
            ot[1] = 0;
            int[] st = s.clone();
            st[1] = s[1] + o[1];
            double[][] dt = getData(hdfsuri, path, var, row, col, ot, st);
            int[] od = { 0, -o[1], 0 };
            copy(data, od, s, dt, st);

            ot[1] = 0;
            ot[2] = o[2] - s[2] / 2;
            st[1] = -o[1];
            st[2] = s[2] / 2;
            dt = getData(hdfsuri, path, var, row, col, ot, st);
            od[1] = 0;
            copy(data, od, s, dt, st);

            ot[1] = 0;
            ot[2] = o[2] + s[2];
            st[1] = -o[1];
            st[2] = s[2] / 2;
            dt = getData(hdfsuri, path, var, row, col, ot, st);
            od[2] = s[2] / 2;
            copy(data, od, s, dt, st);
            return data;

        } else if (o[1] + s[1] > row) {
            double[][] data = new double[s[0]][s[1] * s[2]];
            int[] ot = o.clone();
            int[] st = s.clone();
            st[1] = row - o[1];
            double[][] dt = getData(hdfsuri, path, var, row, col, ot, st);
            int[] od = { 0, 0, 0 };
            copy(data, od, s, dt, st);

            ot[1] = row - (o[1] + s[1] - row);
            ot[2] = o[2] - s[2] / 2;
            st[1] = s[1] - (row - o[1]);
            st[2] = s[2] / 2;
            dt = getData(hdfsuri, path, var, row, col, ot, st);
            od[1] = row - o[1];
            copy(data, od, s, dt, st);

            ot[1] = row - (o[1] + s[1] - row);
            ot[2] = o[2] + s[2];
            st[1] = s[1] - (row - o[1]);
            st[2] = s[2] / 2;
            dt = getData(hdfsuri, path, var, row, col, ot, st);
            od[2] = s[2] / 2;
            copy(data, od, s, dt, st);
            return data;
        } else {
            double[][] data = getDataSafe(hdfsuri, path, var, o, s);
            return data;
        }
    }

    public static double[][] getDataWithNeighbor(String hdfsuri, String path, String varName, int row, int col,
                                                 int[] origin, int[] shape, int extent) throws IOException, InvalidRangeException {
        int[] origin_e = origin.clone();
        int[] shape_e = shape.clone();
        origin_e[1] = origin_e[1] - extent;
        origin_e[2] = origin_e[2] - extent;
        shape_e[1] = shape_e[1] + 2 * extent;
        shape_e[2] = shape_e[2] + 2 * extent;

        double[][] data = getData(hdfsuri, path, varName, row, col, origin_e, shape_e);
        return data;
    }

    public static double[][] getDataWithNeighbor(String hdfsuri, List<String> paths, String varName, int row, int col,
                                                 int[] origini, int[] shapei, int extent) throws IOException, InvalidRangeException {
        int[] origin = { 0, 0, 0 };
        int[] shapeiex = { shapei[0], shapei[1] + 2 * extent, shapei[2] + 2 * extent };
        int[] shape = { shapei[0] * paths.size(), shapeiex[1], shapeiex[2] };
        double[][] data = new double[shape[0]][shape[1] * shape[2]];

        for (int i = 0; i < paths.size(); i++) {
            origin[0] = shapei[0] * i;
            double[][] datai = getDataWithNeighbor(hdfsuri, paths.get(i), varName, row, col, origini, shapei, extent);
            copy(data, origin, shape, datai, shapeiex);
        }
        return data;
    }

}
