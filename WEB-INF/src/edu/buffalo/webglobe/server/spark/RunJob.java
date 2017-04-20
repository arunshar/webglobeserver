package edu.buffalo.webglobe.server.spark;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.buffalo.webglobe.server.db.DBUtils;
import edu.buffalo.webglobe.server.utils.Utils;

/**
 * Servlet implementation class RunJob
 */
@WebServlet("/RunJob")
public class RunJob extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String userName;
    private int datasetId;
    private String url;
    private String analysisOutputName;
    private String fieldName;
    private String analysisName;
    private int jobId;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public RunJob() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        this.userName = request.getUserPrincipal().getName();
        JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
        this.datasetId = data.get("datasetid").getAsInt();
        this.url =  data.get("url").getAsString();
        this.analysisName =  data.get("analysisname").getAsString().replace(" ", "");
        this.fieldName =  data.get("fieldname").getAsString();
        this.analysisOutputName = data.get("analysisoutputname").getAsString().replace(" ", "");
        if(this.analysisOutputName=="")
            this.analysisOutputName="defaultanalysisname";

        HashMap<String,String> responseData = new HashMap<String, String>();
        Connection conn;
        Statement stmt;
        try {
            conn = DBUtils.getConnection();
            stmt = conn.createStatement();

            //add an entry to the submitted_analysis_jobs table
            SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String curDate = dt.format(new Date()).toString();
            String cmd = "INSERT INTO submitted_analysis_jobs (user_name,dataset_id,analysis,field,status,submission_time,finish_time,result_loc,priority) VALUES (\"" +
                    userName + "\"," +
                    datasetId + ",\"" +
                    analysisName + "\",\"" +
                    fieldName + "\",\"" +
                    "RUNNING" + "\",\"" +
                    curDate + "\"," +
                    "NULL" + ",\"" +
                    "NULL" + "\",\"1\")";
            ResultSet rs = DBUtils.executeInsert(conn,stmt,cmd);

            if (rs.next()) {
                jobId = rs.getInt(1);
            }
            Thread loadingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    runJob();
                }
            });
            loadingThread.start();
            responseData.put("message", "Analysis job started. Status of job is available under the user information panel. On success, the data set will be available for upload.");

        }catch(SQLException e){
            responseData.put("message", "Error starting job");
        }
        String responseJson = new Gson().toJson(responseData);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseJson);
    }
    public void runJob(){
        //run the job
        String[] outputs = RunSparkJob.createSparkCluster("/home/centos/bash-scripts/sparkcluster.sh", 10, "m1.medium");

        String hdfsuri = Utils.parseHDFSURL(url)[0];
        String inputDir = url;
        String logHypersFile = url + "/"+userName+"/globalLogHypersFile";
        String outputDir = url + "/"+userName+"/analysis/"+analysisOutputName;
        String monitorDataFile = url + "/"+userName+"/monitorDataFile";
        String colorFileName = "fullcolorvalues.xml";
        String numSlices = "3600";
        String d = "60";
        String monitor_start_ind = "0";
        String monitor_end_ind = "1";
        String noYearTrain = "0.5";
        String monitor_method = "2";
        String ifSmoothSpatially = "1";
        String k = "4"; // neighborhood extent
        String[] arg = {
                hdfsuri,
                inputDir,
                logHypersFile,
                outputDir,
                monitorDataFile,
                colorFileName,
                numSlices,
                d,
                monitor_start_ind,
                monitor_end_ind,
                noYearTrain,
                monitor_method,
                ifSmoothSpatially,
                k};
        String[] jars = {
                "/home/ubuntu/jars/gpchange.jar",
                "/home/ubuntu/jars/jama-1.0.3.jar",
                "/home/ubuntu/jars/netcdfAll-4.6.4.jar",
                "/home/ubuntu/jars/worldwind.jar",
        };

        RunSparkJob.runSparkJob("/home/ubuntu/jars/GPChangeSpark-0.0.1-SNAPSHOT.jar",
                        "gpchange.spark.main.MonitorGlobalHypers", arg, "/home/ubuntu/spark-1.6.1",outputs[2], "2g", "2g", jars);

        RunSparkJob.terminateCluster("/home/centos/bash-scripts/terminatecluster.sh", outputs[0]);


        try {
            Connection conn = DBUtils.getConnection();
            Statement stmt = conn.createStatement();
            //update the entry in the submitted_analysis_jobs table
            SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String curDate = dt.format(new Date()).toString();
            String cmd = "UPDATE submitted_analysis_jobs SET finish_time=\""+curDate+"\", status='DONE',result_loc=\""+outputDir+"\" where id=" + jobId;
            DBUtils.executeUpdate(conn,stmt,cmd);
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }
}
