package edu.buffalo.webglobe.server.pagecode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.buffalo.webglobe.server.db.DBUtils;
import edu.buffalo.webglobe.server.netcdf.*;
import edu.buffalo.webglobe.server.utils.Constants;
import edu.buffalo.webglobe.server.utils.Utils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Progressable;
import ucar.ma2.MAMath;

/**
 * @author chandola
 * @version $Id$
 */

/**
 * Servlet implementation class UploadDataset
 */
@WebServlet("/UploadDataset")
public class UploadDataset extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String userName;
    private String dataName;
    private String url;
    private String dataInfo;
    private String dataInfoURL;
    private int stride;
    private int jobId = -1;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public UploadDataset() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
        this.url = data.get("url").getAsString();
        this.dataName = data.get("dataName").getAsString();
        this.dataInfo = data.get("dataInfo").getAsString();
        this.dataInfoURL = data.get("dataInfoURL").getAsString();
        this.stride = data.get("stride").getAsInt();
        Map<String, String> responseData = new HashMap<String, String>();
        String userName;
        if(Utils.configuration.getValue("AUTHENTICATION_TYPE").equalsIgnoreCase("GLOBUS")){
            userName = data.get("username").getAsString();
        }else{
            userName = request.getUserPrincipal().getName();
        }


        if(userName == null){
            responseData.put("message", "Error getting username");
        }else {
            Connection conn;
            Statement stmt;
            try {
                conn = DBUtils.getConnection();
                stmt = conn.createStatement();

                SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String curDate = dt.format(new Date());
                String cmd = "INSERT INTO submitted_upload_jobs (user_name,dataset_url,dataset_name,status,submission_time,finish_time,priority) VALUES (\"" +
                        userName + "\",\"" +
                        url + "\",\"" +
                        dataName + "\",\"" +
                        "RUNNING" + "\",\"" +
                        curDate + "\"," +
                        "NULL" + ",1)";

                ResultSet rs = DBUtils.executeInsert(conn, stmt, cmd);

                if (rs.next()) {
                    jobId = rs.getInt(1);
                }
                Thread loadingThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        uploadDataset();
                    }
                });
                loadingThread.start();
                responseData.put("message", "Loading job started. Status of job is available under the user information panel. On success, the data set will be available under the climate data sets panel.");
                stmt.close();
                conn.close();

            } catch (SQLException e) {
                responseData.put("message", "Error starting job");
            }
        }
        String responseJson = new Gson().toJson(responseData);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseJson);
    }

    public void uploadDataset(){


        Connection conn;
        Statement stmt;
        ResultSet rset;
        int status = 0;
        int datasetId = -1;

        try {
            // query database

            conn = DBUtils.getConnection();
            stmt = conn.createStatement();
            String cmd = "select * from netcdf_datasets where name = '" + dataName+"'";
            rset = DBUtils.executeQuery(conn,stmt,cmd);
            if (rset.next()) {
                Utils.logger.severe("Error: Dataset already exists in database");
                status = -1;
            }
            //open netcdfdata directory and find all Geogrid variables
            if (status != -1) {
                Vector<String []> tokens = Utils.parseURL(url);
                //check if every entry is a valid file
                boolean isValid = true;
                for(String [] tks: tokens){
                    if(!Utils.isNCFile(tks[2])) {
                        isValid = false;
                        break;
                    }
                }
                if(isValid){
                    NetcdfDataSource netcdfDataSource;
                    netcdfDataSource = new NetcdfDataSource(tokens,stride);
                    netcdfDataSource.initialize();

                    if (netcdfDataSource.isInitialized()) {
                        try {
                            int lon_num = netcdfDataSource.getLonLen();

                            double lon_min = netcdfDataSource.getLonArr().getFloat(0);
                            double lon_max = netcdfDataSource.getLonArr().getFloat(lon_num-1);
                            int lat_num = netcdfDataSource.getLatLen();
                            double lat_min = netcdfDataSource.getLatArr().getFloat(0);
                            double lat_max = netcdfDataSource.getLatArr().getFloat(lat_num-1);
                            int time_num = netcdfDataSource.getTimeLen();

                            SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                            String time_min = dt.format(netcdfDataSource.getDates().get(0).toDate());
                            String time_max = dt.format(netcdfDataSource.getDates().get(time_num-1).toDate());

                            cmd = "INSERT INTO netcdf_datasets (name,user,info,info_url,is_accessible,lon_min,lon_max,lon_num,"+
                                    "lat_min,lat_max,lat_num,time_min,time_max,time_num) VALUES (\"" +
                                    dataName + "\",\""+userName+"\",\""+dataInfo+"\",\""+dataInfoURL+"\",0,"+lon_min+","
                                    +lon_max+","+lon_num+","+lat_min+","+lat_max+","+lat_num+",\""+time_min+"\",\""+time_max+"\","+time_num+")";

                            rset = DBUtils.executeInsert(conn,stmt,cmd);
                            if(rset.next()){
                                datasetId = rset.getInt(1);
                                for (int j = 0; j < netcdfDataSource.getVariables().size(); j++) {
                                    String variable = netcdfDataSource.getVariables().get(j);
                                    NetcdfVariable netcdfVariable = new NetcdfVariable(netcdfDataSource, variable);
                                    String hdfsFileName = Utils.configuration.getValue("HDFS_BASEDIR")+"/"+Utils.cleanFileName(dataName)+"/"+netcdfVariable.getVariableName()+".csv";

                                    //dump data to HDFS
                                    Utils.logger.info("Writing out data to HDFS");
                                    Configuration configuration = new Configuration();
                                    FileSystem hdfs = FileSystem.get( new URI( Utils.configuration.getValue("HDFS_SERVER") ), configuration );

                                    Path file = new Path(hdfsFileName);

                                    if ( hdfs.exists( file )) { hdfs.delete( file, true ); }
                                    OutputStream os = hdfs.create( file,
                                            new Progressable() {
                                                public void progress() {
                                                    Utils.logger.info(".");
                                                } });
                                    BufferedWriter br = new BufferedWriter( new OutputStreamWriter( os, "UTF-8" ) );
                                    try{
                                        long st = System.currentTimeMillis();
                                        netcdfVariable.writeToHDFS(br);
                                        long en = System.currentTimeMillis();
                                        br.close();
                                        hdfs.close();
                                        Utils.logger.info("Finished writing out data to HDFS in "+(en - st)/1000 + " seconds");
                                        //get minmax values
                                        MAMath.MinMax minmax = new MAMath.MinMax(netcdfVariable.getMin(),netcdfVariable.getMax());


                                        cmd = "INSERT INTO netcdf_dataset_fields (dataset_id,field_name,units,max_value,min_value,hdfs_path) VALUES (" +
                                                datasetId + ",\"" + variable +
                                                "\",\"" + netcdfDataSource.getUnits().get(j) + "\"," +
                                                minmax.max + "," + minmax.min+ ",\""+hdfsFileName+"\")";
                                        Statement stmt1 = conn.createStatement();
                                        DBUtils.executeInsert(conn,stmt1,cmd);

                                        status = 1;
                                    }catch(Exception e){
                                        br.close();
                                        hdfs.close();
                                        Utils.logger.severe("Error in writing out data to HDFS");
                                        status = -1;
                                    }
                                }

                            }else{
                                Utils.logger.severe("Error: Could not create a new dataset record.");
                                status = -1;
                            }
                        } catch (Exception e) {
                            Utils.logger.severe(e.getMessage());
                            Utils.logger.severe("Error: Unable to open netcdf file.");
                            status = -1;
                        }

                    }else{
                        Utils.logger.severe("Error initializing the netcdf data source");
                        status = -1;
                    }
                }else{
                    Utils.logger.severe("Error: Invalid entries in the supplied list.");
                    status = -1;
                }
            }
            stmt.close();
            conn.close();
        }
        catch(Exception e){
            Utils.logger.severe(e.getMessage());
            Utils.logger.severe("Error: Unable to connect to the database");
            status = -1;
        }

        try {
            conn = DBUtils.getConnection();
            stmt = conn.createStatement();
            String cmd,cmd1 = null;
            SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String curDate = dt.format(new Date());
            if(status == 1){
                cmd = "UPDATE submitted_upload_jobs SET status='DONE', finish_time= \""+curDate+"\" where id=" + jobId;
                if(datasetId != -1)
                    cmd1 = "UPDATE netcdf_datasets SET is_accessible = 1 where id ="+datasetId;
            }else{
                cmd = "UPDATE submitted_upload_jobs SET status='FAILED', finish_time= \""+curDate+"\"  where id=" + jobId;
                if(datasetId != -1)
                    cmd1 = "DELETE from netcdf_datasets WHERE id="+datasetId;
            }
            DBUtils.executeUpdate(conn,stmt,cmd);
            if(cmd1 != null)
                DBUtils.executeUpdate(conn,stmt,cmd1);
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            Utils.logger.severe(e.getMessage());
        }
    }
}

