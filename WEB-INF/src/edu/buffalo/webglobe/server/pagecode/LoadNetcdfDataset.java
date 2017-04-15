package edu.buffalo.webglobe.server.pagecode;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import ucar.nc2.time.CalendarDateFormatter;
import edu.buffalo.webglobe.server.utils.Constants;
import edu.buffalo.webglobe.server.utils.NetcdfDir;

/**
 * Servlet implementation class LoadNetcdfDataset
 */
@WebServlet("/LoadNetcdfDataset")
public class LoadNetcdfDataset extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public LoadNetcdfDataset() {
        super();
        // TODO Auto-generated constructor stub
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
        String hdfsAddress = data.get("url").getAsString();
        Map<String, Map<String, String>> responseData = new HashMap<String, Map<String, String>>();

        String hdfsuri = hdfsAddress.substring(0, hdfsAddress.indexOf("/user"));
        FileSystem fs = null;
        try {

            NetcdfDir netcdfDir = new NetcdfDir(hdfsAddress+"/netCDFs");
            String saveDir = netcdfDir.getDir() + "/variable/" + netcdfDir.getVariableName();
            CalendarDateFormatter dateFormatter = new CalendarDateFormatter("yyyy-MM-dd");

            Map<String, String> variableInfo = new HashMap<String, String>();
            variableInfo.put("name", netcdfDir.getVariableName());
            variableInfo.put("address", hdfsAddress+"/netCDFs");
            variableInfo.put("minDate", dateFormatter.toString(netcdfDir.getStartDate()));
            variableInfo.put("maxDate", dateFormatter.toString(netcdfDir.getEndDate()));

            File folder = new File(Constants.LOCAL_DIRECTORY + saveDir);
            System.out.println(folder.getAbsolutePath());
            if (folder.exists()) {
                variableInfo.put("imagesAddress", saveDir);
                File[] listOfFiles = folder.listFiles();
                String fName = listOfFiles[0].getName();
                variableInfo.put("imageMinDate", fName.substring(0, fName.lastIndexOf('.')));
                fName = listOfFiles[listOfFiles.length-1].getName();
                variableInfo.put("imageMaxDate", fName.substring(0, fName.lastIndexOf('.')));
            } else {
                variableInfo.put("imagesAddress", "");
                variableInfo.put("imageMinDate", "");
                variableInfo.put("imageMaxDate", "");
            }

            responseData.put("variable", variableInfo);

            fs = FileSystem.get(new URI(hdfsuri), new Configuration());
            Path analysisPath = new Path(hdfsAddress + "/analysis");
            Map<String, String> analysisInfo = new HashMap<String, String>();
            if (fs.exists(analysisPath)) {
                Path analysisImageDir = fs.listStatus(analysisPath)[0].getPath();
                analysisInfo.put("name", analysisImageDir.getName());
                analysisInfo.put("address", analysisImageDir.toString());
                NetcdfDir analysisDir = new NetcdfDir(analysisImageDir.toString());
                saveDir = analysisDir.getDir() + "/variable/" + analysisDir.getVariableName();
                analysisInfo.put("minDate", dateFormatter.toString(analysisDir.getStartDate()));
                analysisInfo.put("maxDate", dateFormatter.toString(analysisDir.getEndDate()));

                folder = new File(Constants.LOCAL_DIRECTORY + saveDir);

                if (folder.exists()) {
                    analysisInfo.put("imagesAddress", saveDir);
                    File[] listOfFiles = folder.listFiles();
                    Arrays.sort(listOfFiles);
                    String fName = listOfFiles[0].getName();
                    analysisInfo.put("imageMinDate", fName.substring(0, fName.lastIndexOf('.')));
                    fName = listOfFiles[listOfFiles.length-1].getName();
                    analysisInfo.put("imageMaxDate", fName.substring(0, fName.lastIndexOf('.')));
                } else {
                    analysisInfo.put("imagesAddress", "");
                    analysisInfo.put("imageMinDate", "");
                    analysisInfo.put("imageMaxDate", "");
                }
            }

            responseData.put("analysis", analysisInfo);

            String responseJson = new Gson().toJson(responseData);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(responseJson);
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            fs.close();
        }

    }

}
