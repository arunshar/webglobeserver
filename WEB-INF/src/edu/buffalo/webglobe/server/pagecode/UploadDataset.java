package edu.buffalo.webglobe.server.pagecode;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
import ucar.ma2.Array;
import ucar.ma2.MAMath;

import static edu.buffalo.webglobe.server.utils.Constants.VALID_EXTENSIONS;
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
    private String visualizationOnly;
    private String selectedColormap;
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
        this.visualizationOnly = data.get("visualizationOnly").getAsString();
        this.selectedColormap = data.get("selectedColormap").getAsString();
        Map<String, String> responseData = new HashMap<String, String>();
        this.userName = request.getUserPrincipal().getName();

        Connection conn;
        Statement stmt;
        try {
            conn = DBUtils.getConnection();
            stmt = conn.createStatement();

            SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String curDate = dt.format(new Date());
            String cmd = "INSERT INTO submitted_image_creation_jobs (user_name,dataset_url,dataset_name,status,submission_time,finish_time,priority) VALUES (\"" +
                    userName + "\",\"" +
                    url + "\",\"" +
                    dataName + "\",\"" +
                    "RUNNING" + "\",\"" +
                    curDate + "\"," +
                    "NULL" + ",1)";

            ResultSet rs = DBUtils.executeInsert(conn,stmt,cmd);

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

        }catch(SQLException e){
            responseData.put("message", "Error starting job");
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
        int isAnalyzable = 1;

        try {
            // query database
            String[] tokens;
            tokens = Utils.parseURL(url);

            String protocol = tokens[0];
            String uri = tokens[1];
            String dir = tokens[2];

            if(this.visualizationOnly.equalsIgnoreCase("on")) {
                isAnalyzable = 0;
            }else{
                if(!protocol.equalsIgnoreCase("hdfs")){
                    //copy the file to hdfs
                    url = NetCDFUtils.copyRemoteFileToHDFS(url, this.userName);
                }
            }
            conn = DBUtils.getConnection();
            stmt = conn.createStatement();
            String cmd = "select * from netcdf_datasets where name = '" + dataName + "' AND url = '" + url + "'";
            rset = DBUtils.executeQuery(conn,stmt,cmd);
            if (rset.next()) {
                Utils.logger.severe("Error: Dataset already exists in database");
                status = -1;
            }
            //open netcdfdata directory and find all Geogrid variables
            if (status != -1) {
                NetcdfSource ncDir = null;
                try {

                    //check if the URL points to a file or a directory
                    if(Utils.isNCFile(dir)) {
                        ncDir = new NetcdfFile(protocol,uri,dir);
                        ncDir.initialize();
                    } else {
                        if(!protocol.equalsIgnoreCase("hdfs")){
                            status = -1;
                        }else {
                            ncDir = new NetcdfDirectory(protocol, uri, dir);
                            ncDir.initialize();
                        }
                    }

                    if ((ncDir != null) && (ncDir.isInitialized()) ) {


                        cmd = "INSERT INTO netcdf_datasets (name,url,available,info,info_url,is_accessible,is_analyzable) VALUES (\"" +
                                dataName + "\",\"" + url+"\",\""+userName+"\",\""+dataInfo+"\",\""+dataInfoURL+"\",0,"+isAnalyzable+")";

                        rset = DBUtils.executeInsert(conn,stmt,cmd);
                        if(rset.next()){
                            datasetId = rset.getInt(1);
                            Utils.logger.info("STARTING IMAGE CREATION PROCESS ");
                            for (int j = 0; j < ncDir.getVariables().size(); j++) {
                                String variable = ncDir.getVariables().get(j);
                                NetcdfVariable netcdfVariable = new NetcdfVariable(ncDir, variable);
                                //get minmax values
                                MAMath.MinMax minmax;
                                //TODO - Change this to accept minmax ranges from the user
                                if (variable.equals("tasmax")) {
                                    minmax = new MAMath.MinMax(200, 373);
                                } else if (variable.equals("ChangeDetection")) {
                                    minmax = new MAMath.MinMax(-1, 2);
                                } else {
                                    Array src1 = netcdfVariable.getData(0);
                                    minmax = MAMath.getMinMax(src1);
                                }
                                cmd = "INSERT INTO netcdf_dataset_fields (dataset_id,field_name,units,max_value,min_value) VALUES (" +
                                        datasetId + ",\"" + variable +
                                        "\",\"" + ncDir.getUnits().get(j) + "\"," +
                                        minmax.max + "," + minmax.min+")";
                                Statement stmt1 = conn.createStatement();
                                ResultSet rset1 = DBUtils.executeInsert(conn,stmt1,cmd);

                                int fieldId = -1;
                                if(rset1.next())
                                    fieldId = rset1.getInt(1);
                                String saveDir = netcdfVariable.getNetcdfSource().getTarget() + "/variable/" + variable;
                                File folder = new File(Constants.LOCAL_DIRECTORY + saveDir);
                                folder.mkdirs();

                                int startIndex = 0;
                                int endIndex = netcdfVariable.getNetcdfSource().getTotalTimeLength();

                                for (int i = startIndex; i < endIndex; ++i) {
                                    Array src = netcdfVariable.getData(i);
                                    float[][] data = ((float[][][]) src.copyToNDJavaArray())[0];
                                    String imgLocation = Constants.LOCAL_DIRECTORY + saveDir + "/" + netcdfVariable.getNetcdfSource().getDateFromIndex(i) + ".png";
                                    String imgURL = Constants.PUBLIC_ADDRESS +"/" + saveDir + "/" + netcdfVariable.getNetcdfSource().getDateFromIndex(i) + ".png";
                                    Utils.createImage(data, (float) minmax.min, (float) minmax.max, imgLocation);
                                    //create an entry in the database
                                    //datasetId, fieldId, timeindex, timestamp, imgLocation
                                    cmd = "INSERT INTO netcdf_dataset_images (dataset_id, field_id, time_index,timestamp, img_location)"+
                                            "VALUES("+ datasetId +","+fieldId+","+i+",\""+ netcdfVariable.getNetcdfSource().getDateFromIndex(i)+"\",\""+imgURL+"\")";
                                    DBUtils.executeUpdate(conn,stmt1,cmd);
                                }
                                status = 1;

                            }
                            Utils.logger.info("ENDED IMAGE CREATION PROCESS");

                        }else{
                            Utils.logger.severe("Error: Could not create a new dataset record.");
                            status = -1;
                        }
                    } else {
                        Utils.logger.severe("Error: Unable to parse server address.");
                        status = -1;
                    }

                } catch (Exception e) {
                    Utils.logger.severe(e.getMessage());
                    Utils.logger.severe("Error: Unable to open HDFS file.");
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
                cmd = "UPDATE submitted_image_creation_jobs SET status='DONE', finish_time= \""+curDate+"\" where id=" + jobId;
                if(datasetId != -1)
                    cmd1 = "UPDATE netcdf_datasets SET is_accessible = 1 where id ="+datasetId;
            }else{
                cmd = "UPDATE submitted_image_creation_jobs SET status='FAILED', finish_time= \""+curDate+"\"  where id=" + jobId;
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

