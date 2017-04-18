package edu.buffalo.webglobe.server.pagecode;

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
import edu.buffalo.webglobe.server.utils.NetcdfDirNoVar;
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
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        String message = null;
        String status = "-1";
        try {
            conn = DBUtils.getConnection();
            stmt = conn.createStatement();
            String cmd = "select * from netcdf_datasets where name = '"+dataName+"' AND url = '"+hdfsURL+"'";
            logger.warning(cmd);
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
                logger.warning("In HERE -- Entering constructor **********************"+userName);
                ncDir = new NetcdfDirNoVar(hdfsURL);
                logger.warning("Out HERE -- Exiting constructor **********************"+userName);
            }catch(Exception e){
                message = "Error: Unable to open HDFS file.";
                status = "-1";
                responseData.put("message", message);
                responseData.put("status", status);
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

