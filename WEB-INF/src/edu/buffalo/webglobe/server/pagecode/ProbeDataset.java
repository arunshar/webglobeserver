package edu.buffalo.webglobe.server.pagecode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.buffalo.webglobe.server.netcdf.NetcdfDataSource;
import edu.buffalo.webglobe.server.utils.Utils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

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
        NetcdfDataSource netcdfDataSource;
        try {
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
                netcdfDataSource = new NetcdfDataSource(tokens,1);
                netcdfDataSource.initialize();

                if ((netcdfDataSource.isInitialized()) && (netcdfDataSource.getVariables() == null)){
                    Utils.logger.severe("Error: Unable to parse server address.");
                    status = -1;
                } else {
                    responseData.put("numvars",Integer.toString(netcdfDataSource.getVariables().size()));
                    responseData.put("name",tokens.get(0)[2]);
                    responseData.put("info",netcdfDataSource.getDatasetName()+"\n\n"+netcdfDataSource.getDataDescription());
                    responseData.put("infoURL",netcdfDataSource.getDatasetInfoURL());
                    for (int j = 0; j < netcdfDataSource.getVariables().size(); j++) {
                        String variable = netcdfDataSource.getVariables().get(j);
                        responseData.put("var"+j, variable);
                    }
                    status = 1;
                }
            }else{
                Utils.logger.severe("Error: Invalid entries in the supplied list.");
                status = -1;
            }
        } catch (Exception e) {
            status = -1;
        }
        responseData.put("status",(Integer.toString(status)));
        return responseData;
    }

}

