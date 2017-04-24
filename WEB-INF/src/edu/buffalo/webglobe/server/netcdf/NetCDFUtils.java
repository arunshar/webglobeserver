package edu.buffalo.webglobe.server.netcdf;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import ucar.ma2.Array;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

import edu.buffalo.webglobe.server.spark.HDFSRandomAccessFile;

public class NetCDFUtils {
    public static final Logger logger = Logger.getLogger("WEBGLOBE.LOGGER");
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
			logger.severe(
                    "Error while converting a netcdf.ucar.ma2 to a 1D array. Most likely occurred with casting");
		}

		return array;
	}

    /**
     * Loads a NetCDF Dataset from HTTP URL
     */
    public static NetcdfDataset loadHTTPNetcdfDataSet(String protocol, String url, String target){
        NetcdfDataset.setUseNaNs(false);

        try{
            return new NetcdfDataset(NetcdfFile.open(protocol+"://"+url+"/"+target));
        } catch (IOException e) {
            logger.severe(("Couldn't open dataset " +url));        }
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
			logger.severe(("Cannot open dataset " + dfsUri + "" + location));
			return null;
		} catch (Exception ex) {
            logger.severe(("Cannot open dataset " + dfsUri + "" + location));
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


}
