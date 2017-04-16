package edu.buffalo.webglobe.server.pagecode;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.buffalo.webglobe.server.utils.Constants;
import edu.buffalo.webglobe.server.utils.NetcdfDir;
import edu.buffalo.webglobe.server.utils.Utils;
import ucar.ma2.Array;
import ucar.ma2.MAMath;

import java.util.logging.Logger;
/**
 * Servlet implementation class CreateImages
 */
@WebServlet("/CreateImages")
public class CreateImages extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CreateImages() {
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

        JsonObject dataJson = new Gson().fromJson(request.getReader(), JsonObject.class);

        String hdfsAddress = dataJson.get("url").getAsString();
        String variableName = dataJson.get("fieldname").getAsString();
        String hdfsDir = hdfsAddress.substring(hdfsAddress.indexOf("/user"), hdfsAddress.length());
        String from = dataJson.get("from").getAsString();
        String to = dataJson.get("to").getAsString();

        NetcdfDir netcdfDir = new NetcdfDir(hdfsAddress, variableName);

        String saveDir = hdfsDir + "/variable/" + netcdfDir.getVariableName();
        File folder = new File(Constants.LOCAL_DIRECTORY + saveDir);
        folder.mkdirs();

        int startIndex = Math.max(netcdfDir.getIndexFromDate(from),0);
        int endIndex = Math.min(netcdfDir.getIndexFromDate(to),netcdfDir.getFilepaths().size()*netcdfDir.getTimeLen()-1);
        for (int i = startIndex; i <= endIndex; ++i) {
            Array src = netcdfDir.getData(i);
            float[][] data = ((float[][][]) src.copyToNDJavaArray())[0];
            MAMath.MinMax minmax;
            if (netcdfDir.getVariableName().equals("tasmax")) {
                minmax = new MAMath.MinMax(200, 373);
            } else if (netcdfDir.getVariableName().equals("ChangeDetection")) {
                minmax = new MAMath.MinMax(-1, 2);
            } else {
                minmax = MAMath.getMinMax(src);
            }

            Utils.createImage(data, (float) minmax.min, (float) minmax.max, Constants.LOCAL_DIRECTORY + saveDir + "/" + netcdfDir.getDateFromIndex(i) + ".png");
        }

        Map<String, String> responseData = new HashMap<String, String>();
        responseData.put("imagesAddress", saveDir);


        File[] listOfFiles = folder.listFiles();
        Arrays.sort(listOfFiles);
        String fName = listOfFiles[0].getName();
        responseData.put("imageMinDate", fName.substring(0, fName.lastIndexOf('.')));
        fName = listOfFiles[listOfFiles.length-1].getName();
        responseData.put("imageMaxDate", fName.substring(0, fName.lastIndexOf('.')));

        String responseJson = new Gson().toJson(responseData);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseJson);
    }

}
