package edu.buffalo.webglobe.server.pagecode;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import ucar.ma2.Array;
import ucar.ma2.MAMath;

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

    /**
     * @see HttpServlet#HttpServlet()
     */
    public UploadDataset() {
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

        logger = Logger.getLogger("WEBGLOBE.LOGGER");
        JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
        String hdfsURL = data.get("hdfsURL").getAsString();
        String dataName = data.get("dataName").getAsString();
        String dataInfo = data.get("dataInfo").getAsString();
        String dataInfoURL = data.get("dataInfoURL").getAsString();
        Map<String, String> responseData = new HashMap<String, String>();
        String userName = request.getUserPrincipal().getName();

        responseData = this.uploadDataset(userName,dataName,hdfsURL,dataInfo,dataInfoURL,responseData);
        String responseJson = new Gson().toJson(responseData);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseJson);
    }

    public Map<String,String> uploadDataset(String userName, String dataName, String hdfsURL, String dataInfo, String dataInfoURL, Map<String, String> responseData){
        // query database
        Connection conn;
        Statement stmt;
        ResultSet rset;
        String message;
        String status;
        try {
            conn = DBUtils.getConnection();
            stmt = conn.createStatement();
            String cmd = "select * from netcdf_datasets where name = '"+dataName+"' AND url = '"+hdfsURL+"'";
            rset = stmt.executeQuery(cmd);
            while(rset.next()){
                message = "Error: Dataset already exists in database";
                status = "-1";
                responseData.put("message", message);
                responseData.put("status", status);
                return responseData;
            }
            //open netcdfdata directory and find all Geogrid variables

            NetcdfDirNoVar ncDir = null;
            try {
                ncDir = new NetcdfDirNoVar(hdfsURL);
                if(ncDir.getVariables() == null){
                    message = "Error: Unable to parse server address.";
                    status = "-1";
                    responseData.put("message", message);
                    responseData.put("status", status);
                    return responseData;
                }
                for(String variable: ncDir.getVariables()){
                    NetcdfDir netcdfDir = new NetcdfDir(hdfsURL,variable);
                    String saveDir = netcdfDir.getDir() + "/variable/" + netcdfDir.getVariableName();
                    File folder = new File(Constants.LOCAL_DIRECTORY + saveDir);
                    folder.mkdirs();
                    logger.info("STARTING PROCESS");
                    int startIndex = 0;
                    int endIndex = netcdfDir.getFilepaths().size()*netcdfDir.getTimeLen()-1;
                    for (int i = startIndex; i <= endIndex; ++i) {
                        Array src = netcdfDir.getData(i);
                        float[][] data = ((float[][][]) src.copyToNDJavaArray())[0];
                        MAMath.MinMax minmax;
                        //TODO - Change this to accept minmax ranges from the user
                        if (netcdfDir.getVariableName().equals("tasmax")) {
                            minmax = new MAMath.MinMax(200, 373);
                        } else if (netcdfDir.getVariableName().equals("ChangeDetection")) {
                            minmax = new MAMath.MinMax(-1, 2);
                        } else {
                            minmax = MAMath.getMinMax(src);
                        }

                        logger.info("Creating image with index = "+i+ "in "+Constants.LOCAL_DIRECTORY + saveDir + "/" + netcdfDir.getDateFromIndex(i)+".png");
                        Utils.createImage(data, (float) minmax.min, (float) minmax.max, Constants.LOCAL_DIRECTORY + saveDir + "/" + netcdfDir.getDateFromIndex(i) + ".png");
                        logger.info("Created image with index = "+i+ "in "+Constants.LOCAL_DIRECTORY + saveDir + "/" + netcdfDir.getDateFromIndex(i)+".png");

                    }
                    logger.info("ENDED PROCESS");
                    // create Images

                }
            }catch(Exception e){
                message = "Error: Unable to open HDFS file.";
                status = "-1";
                responseData.put("message", message);
                responseData.put("status", status);
                return responseData;
            }
            responseData.put("message","Found "+ncDir.getVariables().size()+" variables.");
            responseData.put("status","1");
        }
        catch(SQLException e){
            message = "Error: Unable to connect to the database";
            status = "-1";
            responseData.put("message", message);
            responseData.put("status", status);
            return responseData;
       }
        //4. - find the geogrid variables
        //5. - Create images
        //6. - Enter details into DB (
       return responseData;
    }

}

