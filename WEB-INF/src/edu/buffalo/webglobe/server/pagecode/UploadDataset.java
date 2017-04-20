package edu.buffalo.webglobe.server.pagecode;

import java.io.File;
import java.io.IOException;
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
import edu.buffalo.webglobe.server.utils.Constants;
import edu.buffalo.webglobe.server.utils.NetcdfDir;
import edu.buffalo.webglobe.server.utils.NetcdfDirNoVar;
import edu.buffalo.webglobe.server.utils.Utils;
import scala.collection.immutable.Stream;
import ucar.ma2.Array;
import ucar.ma2.MAMath;

import java.util.logging.Level;
import java.util.logging.Logger;
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
    private Logger logger;
    private String userName;
    private String dataName;
    private String hdfsURL;
    private String dataInfo;
    private String dataInfoURL;
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

        logger = Logger.getLogger("WEBGLOBE.LOGGER");
        JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
        this.hdfsURL = data.get("hdfsURL").getAsString();
        this.dataName = data.get("dataName").getAsString();
        this.dataInfo = data.get("dataInfo").getAsString();
        this.dataInfoURL = data.get("dataInfoURL").getAsString();
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
                    hdfsURL + "\",\"" +
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

        }catch(SQLException e){
            responseData.put("message", "Error starting job");
        }
        String responseJson = new Gson().toJson(responseData);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseJson);
    }

    public void uploadDataset(){
        // query database
        Connection conn;
        Statement stmt;
        ResultSet rset;
        int status = 0;
        try {
            conn = DBUtils.getConnection();
            stmt = conn.createStatement();
            String cmd = "select * from netcdf_datasets where name = '" + dataName + "' AND url = '" + hdfsURL + "'";
            rset = DBUtils.executeQuery(conn,stmt,cmd);
            if (rset.next()) {
                logger.severe("Error: Dataset already exists in database");
                status = -1;
            }
            //open netcdfdata directory and find all Geogrid variables
            if (status != -1) {
                NetcdfDirNoVar ncDir;
                try {
                    ncDir = new NetcdfDirNoVar(hdfsURL);
                    if (ncDir.getVariables() == null) {
                        logger.severe("Error: Unable to parse server address.");
                        status = -1;
                    } else {
                        cmd = "INSERT INTO netcdf_datasets (name,url,available,info,info_url) VALUES (\"" +
                                dataName + "\",\"" + hdfsURL+"\",\""+userName+"\",\""+dataInfo+"\",\""+dataInfoURL+"\")";

                        rset = DBUtils.executeInsert(conn,stmt,cmd);
                        if(rset.next()){
                            int datasetId = rset.getInt(1);
                            logger.info("STARTING IMAGE CREATION PROCESS "+ncDir.getVariables().size());
                            for (int j = 0; j < ncDir.getVariables().size(); j++) {
                                String variable = ncDir.getVariables().get(j);
                                NetcdfDir netcdfDir = new NetcdfDir(hdfsURL, variable);
                                //get minmax values
                                MAMath.MinMax minmax;
                                //TODO - Change this to accept minmax ranges from the user
                                if (variable.equals("tasmax")) {
                                    minmax = new MAMath.MinMax(200, 373);
                                } else if (variable.equals("ChangeDetection")) {
                                    minmax = new MAMath.MinMax(-1, 2);
                                } else {
                                    Array src1 = netcdfDir.getData(0);
                                    minmax = MAMath.getMinMax(src1);
                                }
                                cmd = "INSERT INTO netcdf_dataset_fields (dataset_id,field_name,field_description,units,max_value,min_value) VALUES (" +
                                        datasetId + ",\"" + variable + "\",\"" + ncDir.getDescriptions().get(j) +
                                        "\",\"" + ncDir.getUnits().get(j) + "\"," +
                                        minmax.max + "," + minmax.min+")";
                                Statement stmt1 = conn.createStatement();
                                ResultSet rset1 = DBUtils.executeInsert(conn,stmt1,cmd);

                                int fieldId = -1;
                                if(rset1.next())
                                    fieldId = rset1.getInt(1);
                                String saveDir = netcdfDir.getDir() + "/variable/" + variable;
                                File folder = new File(Constants.LOCAL_DIRECTORY + saveDir);
                                folder.mkdirs();

                                int startIndex = 0;
                                int endIndex = netcdfDir.getFilepaths().size() * netcdfDir.getTimeLen() - 1;
                                for (int i = startIndex; i <= endIndex; ++i) {
                                    Array src = netcdfDir.getData(i);
                                    float[][] data = ((float[][][]) src.copyToNDJavaArray())[0];
                                    String imgLocation = Constants.LOCAL_DIRECTORY + saveDir + "/" + netcdfDir.getDateFromIndex(i) + ".png";
                                    String imgURL = Constants.PUBLIC_ADDRESS +"/" + saveDir + "/" + netcdfDir.getDateFromIndex(i) + ".png";
                                    Utils.createImage(data, (float) minmax.min, (float) minmax.max, imgLocation);
                                    //create an entry in the database
                                    //datasetId, fieldId, timeindex, timestamp, imgLocation
                                    cmd = "INSERT INTO netcdf_dataset_images (dataset_id, field_id, time_index,timestamp, img_location)"+
                                           "VALUES("+ datasetId +","+fieldId+","+i+",\""+netcdfDir.getDateFromIndex(i)+"\",\""+imgURL+"\")";
                                    DBUtils.executeUpdate(conn,stmt1,cmd);
                                }
                                status = 1;
                            }
                            logger.info("ENDED IMAGE CREATION PROCESS");

                        }else{
                            logger.severe("Error: Could not create a new dataset record.");
                            status = -1;
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Message", e);
                    logger.severe("Error: Unable to open HDFS file.");
                    status = -1;
                }
            }
            stmt.close();
            conn.close();
        }
        catch(SQLException e){
            logger.log(Level.SEVERE, "Message", e);
            logger.severe("Error: Unable to connect to the database");
            status = -1;
        }
        conn = DBUtils.getConnection();
        try {
            stmt = conn.createStatement();
            String cmd;
            SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String curDate = dt.format(new Date());
            if(status == 1){
                cmd = "UPDATE submitted_image_creation_jobs SET status='DONE', finish_time= \""+curDate+"\" where id=" + jobId;
            }else{
                cmd = "UPDATE submitted_image_creation_jobs SET status='FAILED', finish_time= \""+curDate+"\"  where id=" + jobId;
            }
            DBUtils.executeUpdate(conn,stmt,cmd);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Message", e);
        }
    }
}

