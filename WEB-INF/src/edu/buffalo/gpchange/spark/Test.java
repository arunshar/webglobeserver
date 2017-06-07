package edu.buffalo.gpchange.spark;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub		
//		String hdfsAddress = "hdfs://localhost:9000/user/dtran/BCSD/netCDFs";
//		String hdfsDir = hdfsAddress.substring(hdfsAddress.indexOf("/user"), hdfsAddress.length());
//		String from = "2014-01-01";
//		String to = "2014-02-01";
//
//		NetCDFDir netcdfDir = new NetCDFDir(hdfsAddress, hdfsDir);
//				
//		int startIndex = Math.max(netcdfDir.getIndexFromDate(from),0);
//		int endIndex = Math.min(netcdfDir.getIndexFromDate(to),netcdfDir.getFilepaths().size()*365-1);
//		for (int i = startIndex; i <= endIndex; ++i) {
//			float[][] data = netcdfDir.getData(i);
//			Utils.createNetCDFfile(data, i, "ChangeDetection", netcdfDir, "hdfs://localhost:9000/user/dtran/BCSD/analysis/ChangeDetection");
//		}

	}

}
