package edu.buffalo.webglobe.server.pagecode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.buffalo.webglobe.server.utils.NetcdfDir;
import edu.buffalo.webglobe.server.utils.NetcdfDirNoVar;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
/**
 * @author chandola
 * @version $Id$
 */

/**
 * Servlet implementation class ProbeDataset
 */
@WebServlet("/ProbeDataset")
public class ProbeDataset extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Logger logger;
    private String hdfsURL;

    /**
     * @see javax.servlet.http.HttpServlet#HttpServlet()
     */
    public ProbeDataset() {
        super();
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        logger = Logger.getLogger("WEBGLOBE.LOGGER");
        logger.severe("Starting this");
        JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
        this.hdfsURL = data.get("hdfsURL").getAsString();
        logger.info("Starting the probe job "+this.hdfsURL);
        Map<String, String> responseData =  probeDataset();
        responseData.put("message", "Probing job started.");

        String responseJson = new Gson().toJson(responseData);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseJson);
    }

    public Map<String, String> probeDataset(){
        Map<String, String> responseData = new HashMap<String, String>();
        // query database
        int status = 0;
        //open netcdfdata directory and find all plottable variables
        NetcdfDirNoVar ncDir;
        try {
            ncDir = new NetcdfDirNoVar(hdfsURL);
            if (ncDir.getVariables() == null) {
                logger.severe("Error: Unable to parse server address.");
                status = -1;
            } else {
                responseData.put("numvars",Integer.toString(ncDir.getVariables().size()));
                responseData.put("name",ncDir.getDatasetName());
                responseData.put("info",ncDir.getDataDescription());
                for (int j = 0; j < ncDir.getVariables().size(); j++) {
                    String variable = ncDir.getVariables().get(j);
                    NetcdfDir netcdfDir = new NetcdfDir(hdfsURL, variable);
                    responseData.put("var"+j,netcdfDir.getVariableName());
                    status = 1;
                }
            }
        } catch (Exception e) {
            status = -1;
        }
        responseData.put("status",(Integer.toString(status)));
        return responseData;
    }

}

