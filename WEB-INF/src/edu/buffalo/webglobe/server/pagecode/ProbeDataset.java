package edu.buffalo.webglobe.server.pagecode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.buffalo.webglobe.server.netcdf.NetcdfFile;
import edu.buffalo.webglobe.server.netcdf.NetcdfSource;
import edu.buffalo.webglobe.server.netcdf.NetcdfDirectory;
import edu.buffalo.webglobe.server.utils.Utils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static edu.buffalo.webglobe.server.utils.Constants.VALID_EXTENSIONS;
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
    private String url;

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
        JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
        this.url = data.get("url").getAsString();
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
        int status;
        //open netcdfdata directory and find all plottable variables
        NetcdfSource ncDir;
        try {
            Utils.logger.info("&&&&& "+url);
            String [] tokens = Utils.parseURL(url);
            Utils.logger.info("&&&&& PARSING SUCCESSFUL "+url);

            String protocol = tokens[0];
            String uri = tokens[1];
            String dir = tokens[2];

            //check if the URL points to a file or a directory
            if(dir.contains(".")){
             if(VALID_EXTENSIONS.contains(dir.substring(dir.lastIndexOf(".")+1,dir.length())) ) {
                 ncDir = new NetcdfFile(protocol,uri,dir);
             }else {
                 status = -1;
                 responseData.put("status",(Integer.toString(status)));
                 return responseData;
             }
            }else {
                if(!protocol.equalsIgnoreCase("hdfs")){
                    status = -1;
                    responseData.put("status",(Integer.toString(status)));
                    return responseData;
                }
                ncDir = new NetcdfDirectory(protocol,uri,dir);
            }

            if (ncDir.getVariables() == null) {
                Utils.logger.severe("Error: Unable to parse server address.");
                status = -1;
            } else {
                responseData.put("numvars",Integer.toString(ncDir.getVariables().size()));
                responseData.put("name",ncDir.getDatasetName());
                responseData.put("info",ncDir.getDataDescription());
                for (int j = 0; j < ncDir.getVariables().size(); j++) {
                    String variable = ncDir.getVariables().get(j);
                    responseData.put("var"+j, variable);
                }
                status = 1;
            }
        } catch (Exception e) {
            status = -1;
        }
        responseData.put("status",(Integer.toString(status)));
        return responseData;
    }

}

