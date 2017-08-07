package edu.buffalo.webglobe.server.spark;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.buffalo.webglobe.server.db.DBUtils;
import edu.buffalo.webglobe.server.utils.Utils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
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
                            runSerialJob();
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
    public void runSerialJob(){
        //run the job
        String cmd;
        Connection conn;
        Statement stmt;
        Utils.logger.severe("INSIDE RUNSERIALJOB");
        boolean success = false;
        //get spark url for the datasetid
        String fromYear = "";
        String toYear = "";
        try{
            conn =  DBUtils.getConnection();
            stmt = conn.createStatement();

            cmd = "SELECT time_min,time_max FROM netcdf_datasets WHERE id = "+this.datasetId;
            ResultSet rs = DBUtils.executeQuery(conn,stmt,cmd);
            if (rs.next()) {
                fromYear = rs.getDate(1).toString();
                toYear = rs.getDate(2).toString();
            }
            stmt.close();
            conn.close();
            //create hdfsdataset
            HDFSDataSet hdfsDataSet = new HDFSDataSet(this.datasetId,fieldName,fromYear,toYear);
            //read data


            //analyze
            //output new data
            success = true;
        } catch (SQLException e) {
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
}
