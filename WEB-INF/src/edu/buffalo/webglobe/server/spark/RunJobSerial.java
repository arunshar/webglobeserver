package edu.buffalo.webglobe.server.spark;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.buffalo.webglobe.server.db.DBUtils;
import edu.buffalo.webglobe.server.utils.Utils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Progressable;
import ucar.ma2.MAMath;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Servlet implementation class RunJob
 */
@WebServlet("/RunJobSerial")
public class RunJobSerial extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String userName;
    private String args;
    private String analysisOutputName;
    private String fieldName;
    private String analysisName;
    private int datasetId;
    private int jobId;

    /**
     * @see javax.servlet.http.HttpServlet#HttpServlet()
     */
    public RunJobSerial() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
        if(Utils.configuration.getValue("AUTHENTICATION_TYPE").equalsIgnoreCase("GLOBUS")){
            userName = data.get("username").getAsString();
        }else{
            userName = request.getUserPrincipal().getName();
        }

        HashMap<String,String> responseData = new HashMap<String, String>();
        if(userName == null){
            responseData.put("message", "Error getting username");
        }else {
            this.datasetId = data.get("datasetid").getAsInt();
            this.analysisName = data.get("analysisname").getAsString().replace(" ", "").toLowerCase();

            if(!analysisName.equalsIgnoreCase("correlationanalysis")){
                responseData.put("message", "Unsupported Analysis Method ("+analysisName+")");
            }else {
                this.fieldName = data.get("fieldname").getAsString();
                this.analysisOutputName = data.get("analysisoutputname").getAsString();
                if (this.analysisOutputName.equals(""))
                    this.analysisOutputName = "defaultanalysisname";
                this.args = data.get("args").getAsString();
                Connection conn;
                Statement stmt;
                try {
                    conn = DBUtils.getConnection();
                    stmt = conn.createStatement();

                    //add an entry to the submitted_analysis_jobs table
                    SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    String curDate = dt.format(new Date());
                    String cmd = "INSERT INTO submitted_analysis_jobs (user_name,dataset_id,analysis,field,status,submission_time,finish_time,result_loc,priority) VALUES (\"" +
                            userName + "\"," +
                            datasetId + ",\"" +
                            analysisName + "\",\"" +
                            fieldName + "\",\"" +
                            "RUNNING" + "\",\"" +
                            curDate + "\"," +
                            "NULL" + ",\"" +
                            "NULL" + "\",\"1\")";
                    ResultSet rs = DBUtils.executeInsert(conn, stmt, cmd);

                    if (rs.next()) {
                        jobId = rs.getInt(1);
                    }
                    Thread analysisThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            runJobSerial();
                        }
                    });
                    analysisThread.start();

                    responseData.put("message", "Analysis job started. Status of job is available under the user information panel. On success, the data set will be available for visualization.");
                    stmt.close();
                    conn.close();
                } catch (SQLException e) {
                    responseData.put("message", "Error starting job");
                }
            }
        }
        String responseJson = new Gson().toJson(responseData);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseJson);
    }
    //a hacked together implementation for correlation analysis
    public void runJobSerial(){
        //run the job
        String cmd;
        Connection conn;
        Statement stmt;
        Utils.logger.severe("INSIDE RUNSERIALJOB");
        boolean success = false;
        //get spark url for the datasetid
        String fromYear = "";
        String toYear = "";
        String name = "";
        try{
            conn =  DBUtils.getConnection();
            stmt = conn.createStatement();

            cmd = "SELECT time_min,time_max,name FROM netcdf_datasets WHERE id = "+this.datasetId;
            ResultSet rs = DBUtils.executeQuery(conn,stmt,cmd);
            if (rs.next()) {
                fromYear = rs.getDate(1).toString();
                toYear = rs.getDate(2).toString();
                name = rs.getString(3);
            }
            stmt.close();
            conn.close();
            //create hdfsdataset
            HDFSDataSet hdfsDataSet = new HDFSDataSet(this.datasetId,fieldName,fromYear,toYear);
            String[] tokens = this.args.split(";");
            int targetYear = Integer.parseInt(tokens[0]);
            double targetLat = Double.parseDouble(tokens[1]);
            double targetLon = Double.parseDouble(tokens[2]);
            HashMap<double[], double[]> results = RunJobSerial.getPearsonCorrelation(hdfsDataSet,targetLat,targetLon,targetYear);
            //output new data to HDFS
            Utils.logger.info("Writing out data to HDFS");

            Configuration configuration = new Configuration();
            FileSystem hdfs = FileSystem.get( new URI( Utils.configuration.getValue("HDFS_SERVER") ), configuration );
            String hdfsFileName = Utils.configuration.getValue("HDFS_BASEDIR")+"/"+Utils.cleanFileName(this.analysisOutputName)+"/"+Utils.cleanFileName(this.analysisName)+".csv";
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
                //write out the map to the buffered writer
                int numyears = 0;
                for(double[] key: results.keySet()){
                    double [] v = results.get(key);
                    String s = "";
                    for(int i = 0;i < v.length; i++)
                        s += v[i]+" ,";
                    numyears = v.length;
                    br.write(key[0]+","+key[1]+":["+s.substring(0,s.length()-2)+"]\n");
                }
                long en = System.currentTimeMillis();
                br.close();
                hdfs.close();
                Utils.logger.info("Finished writing out data to HDFS in "+(en - st)/1000 + " seconds");

                //create a meta data entry into netcdf_datasets and netcdf_dataset_fields
                String dataInfo = "Running "+this.analysisName+" on "+name+ " with arguments: "+args;
                String st_d = new SimpleDateFormat("yyyy-MM-dd").format(hdfsDataSet.getDates().get(hdfsDataSet.getStartTimeIndex()));
                String en_d = new SimpleDateFormat("yyyy-MM-dd").format(hdfsDataSet.getDates().get(hdfsDataSet.getEndTimeIndex()));
                conn =  DBUtils.getConnection();
                stmt = conn.createStatement();

                cmd = "INSERT INTO netcdf_datasets (name,user,info,info_url,is_accessible,lon_min,lon_max,lon_num,"+
                        "lat_min,lat_max,lat_num,time_min,time_max,time_num) VALUES (\"" +
                        this.analysisOutputName + "\",\""+userName+"\",\""+dataInfo+"\",\"\",1,"+hdfsDataSet.getLonMin()+","
                        +hdfsDataSet.getLonMax()+","+hdfsDataSet.getLonNum()+","+hdfsDataSet.getLatMin()+","+hdfsDataSet.getLatMax()+","
                        +hdfsDataSet.getLatNum()+",\""+st_d+"\",\""+en_d+"\","+numyears+")";

                ResultSet rset = DBUtils.executeInsert(conn,stmt,cmd);
                if(rset.next()) {
                    int dId = rset.getInt(1);
                    cmd = "INSERT INTO netcdf_dataset_fields (dataset_id,field_name,units,max_value,min_value,hdfs_path) VALUES (" +
                            dId + ",\"" + this.analysisName +
                            "\",\"\",1,-1,\"" + hdfsFileName + "\")";
                    Statement stmt1 = conn.createStatement();
                    DBUtils.executeInsert(conn, stmt1, cmd);

                    success = true;
                }
                stmt.close();
                conn.close();
            }catch(Exception e){
                br.close();
                hdfs.close();
                Utils.logger.severe("Error in writing out data to HDFS");
            }


        } catch (Exception e) {
            Utils.logger.severe(e.getMessage());
        }


        try {
            conn =  DBUtils.getConnection();
            stmt = conn.createStatement();
            //update the entry in the submitted_analysis_jobs table
            SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String curDate = dt.format(new Date());
            if(success) {
                cmd = "UPDATE submitted_analysis_jobs SET finish_time=\"" + curDate + "\", status='DONE',result_loc=\"" + this.analysisOutputName + "\" where id=" + jobId;
            }else{
                cmd = "UPDATE submitted_analysis_jobs SET finish_time=\"" + curDate + "\", status='FAILED',result_loc=\"" + this.analysisOutputName + "\" where id=" + jobId;
            }
            DBUtils.executeUpdate(conn,stmt,cmd);
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            Utils.logger.severe(e.getMessage());
        }
    }

    public static HashMap<double[], double[]> getPearsonCorrelation(HDFSDataSet hdfsDataSet, double targetLat, double targetLon, int targetYear){
        //read data for the target location
        double [] targetData = hdfsDataSet.readLocationSlice(targetLat,targetLon);
        //read data for all locations for the target year
        HashMap<double[] , double[] > allData = hdfsDataSet.readYearSlice(targetYear);
        //find number of years
        int l = 1;
        for(double[] k :allData.keySet()) {
            l = allData.get(k).length;
            break;
        }
        int numYears = targetData.length/l;
        //split target data by years
        ArrayList<double[]> yearlyTargetData = new ArrayList<double[]>();
        for(int i = 0; i < numYears; i++){
            double[] currData = new double[l];
            for(int j = 0; j < l; j++){
                currData[j] = targetData[i*l + j];
            }
            yearlyTargetData.add(currData);
        }
        HashMap<double[] , double[] > results = new HashMap<double[], double[]>();
        //analyze
        for(double [] k: allData.keySet()){
            double[] one = allData.get(k);
            double[] res = new double[numYears];
            for(int i = 0; i < numYears; i++){
                double[] two = yearlyTargetData.get(i);
                res[i] = new PearsonsCorrelation().correlation(one,two);
            }
            results.put(k,res);
        }
        return results;
    }
}
