package edu.buffalo.webglobe.server.utils;

import edu.buffalo.webglobe.server.db.DBUtils;
import edu.buffalo.webglobe.server.netcdf.NetcdfDataSource;
import edu.buffalo.webglobe.server.netcdf.NetcdfVariable;
import edu.buffalo.webglobe.server.spark.HDFSDataSet;
import edu.buffalo.webglobe.server.spark.RunJobSerial;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Progressable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @author chandola
 * @version $Id$
 */
public class Tester {
    public static void main(String [] args) throws Exception{

        /*double[] one = new double[3];
        one[0] = 3.1;
        one[1] = 0.4;
        one[2] = 0.8;
        double [] two = new double[3];
        two[0] = 0.2;
        two[1] = -0.3;
        two[2] = 9.0;
        double res = new PearsonsCorrelation().correlation(one,two);
        System.out.println(res); */

        Connection conn;
        Statement stmt;
        String cmd;
        try{
            conn =  DBUtils.getConnection();
            stmt = conn.createStatement();
            //cmd = "INSERT INTO netcdf_datasets (name,user,info,info_url,is_accessible,lon_min,lon_max,lon_num,lat_min,lat_max,lat_num,time_min,time_max,time_num) VALUES (\"hlkhklhlksdf\",\"chandola\",\"Running correlationanalysis on /ubds/docs/air.mon.mean.nc with arguments: 1948;43.09;-79.13\",\"\",1,0.0,357.5,144,90.0,-90.0,73,\"1948-01-31\",\"2010-02-04\",756)";
            //ResultSet rset = DBUtils.executeInsert(conn,stmt,cmd);
            cmd = "SELECT time_min,time_max FROM netcdf_datasets WHERE id = 10";
            ResultSet rs = DBUtils.executeQuery(conn,stmt,cmd);
            String fromYear="",toYear="";
            if (rs.next()) {
                fromYear = rs.getDate(1).toString();
                toYear = rs.getDate(2).toString();
            }
            stmt.close();
            conn.close();
            //create hdfsdataset
            HDFSDataSet hdfsDataSet = new HDFSDataSet(10,"correlationanalysis",fromYear,toYear);
            ArrayList<String> d = hdfsDataSet.readData();
            System.out.println(d.size());
            //Date d = hdfsDataSet.getDates().get(0);
            //System.out.println(d);
            //HashMap<double[], double[]> result = RunJobSerial.getPearsonCorrelation(hdfsDataSet,41.21,-80.32,1948);
            //System.out.println(result.size());
            //read data
            //HashMap<float [],float[]> data = hdfsDataSet.readYearSlice(1948);
            //System.out.println(data.size());
            //double []data = hdfsDataSet.readLocationSlice(41.21,-80.32);
            //System.out.println(data.length);
            //analyze
            //output new data
        } catch (SQLException e) {
            Utils.logger.severe(e.getMessage());
        }

        /*
        String url = "https://www.cse.buffalo.edu/ubds/docs/air.mon.mean.nc";
        Vector<String []> tokens = Utils.parseURL(url);
        System.out.println(tokens.get(0)[0]);
        NetcdfDataSource netcdfDataSource = new NetcdfDataSource(tokens,1);
        netcdfDataSource.initialize();
        */
        /*
        try {

            NetcdfDataset f = new NetcdfDataset((NetcdfFile.open(url)));
            System.out.println(f.getConventionUsed());
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        //
        /*String url = "https://www.cse.buffalo.edu/ubds/docs/air.mon.mean.nc";
        Vector<String []> tokens = null;
        try {
            tokens = Utils.parseURL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }

        //check if every entry is a valid file
        boolean isValid = true;
        for(String [] tks: tokens){
            if(!Utils.isNCFile(tks[2])) {
                isValid = false;
                break;
            }
        }
        if(isValid) {
            NetcdfDataSource netcdfDataSource = new NetcdfDataSource(tokens, 1);
            try {
                netcdfDataSource.initialize();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        */



       /*
        String [] locs = new String[3];
        locs[0] = "file";
        //locs[1] = "/Users/chandola/Downloads";
        //locs[2] = "gfs_4_20170213_1800_384.grb2.nc";
        locs[1] = "/Users/chandola/";
        //locs[2] = "air.mon.mean.nc";
        locs[2] = "tasmin_day_BCSD_rcp45_r1i1p1_CCSM4_2014.nc";
        Vector<String []> tokens = new Vector<String[]>();
        tokens.add(locs);
        String name = "airmonmeantest";

        try{
            NetcdfDataSource netcdfDataSource = new NetcdfDataSource(tokens,30);
            netcdfDataSource.initialize();
            String variable = netcdfDataSource.getVariables().get(0);
            NetcdfVariable netcdfVariable = new NetcdfVariable(netcdfDataSource, variable);
            String hdfsFileName = Constants.HDFS_BASEDIR+"/"+name+"/"+netcdfVariable.getVariableName()+".csv";

            //dump data to HDFS
            Utils.logger.info("Writing out data to HDFS");
            Configuration configuration = new Configuration();
            FileSystem hdfs = FileSystem.get( new URI( Constants.HDFS_SERVER ), configuration );


            Path file = new Path(hdfsFileName);

            if ( hdfs.exists( file )) { hdfs.delete( file, true ); }
            OutputStream os = hdfs.create( file,
                    new Progressable() {
                        public void progress() {
                            Utils.logger.info(".");
                        } });
            BufferedWriter br = new BufferedWriter( new OutputStreamWriter( os, "UTF-8" ) );

            long st = System.currentTimeMillis();
            netcdfVariable.writeToHDFS(br);
            long en = System.currentTimeMillis();
            br.close();
            hdfs.close();
            System.out.println("Writing to HDFS took "+(en-st)/1000 + " seconds.");

        } catch (Exception e) {
            e.printStackTrace();
        }
        */
        /*
        HDFSDataSet hdfsDataSet = new HDFSDataSet(33,"air","1947-12-31","2009-12-31");
        System.out.println(hdfsDataSet.getBoundedTimeNum());
        List<Date> dates = hdfsDataSet.getDates().subList(hdfsDataSet.getStartTimeIndex(),hdfsDataSet.getEndTimeIndex());
        ArrayList<String> data = hdfsDataSet.readData();
        System.out.println(data.size());
          */
        System.out.println(Utils.configuration.getValue("HDFS_SERVER"));
    }
}
