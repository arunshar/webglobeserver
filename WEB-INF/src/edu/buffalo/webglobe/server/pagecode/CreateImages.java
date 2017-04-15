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

import ucar.ma2.Array;
import ucar.ma2.MAMath;
import edu.buffalo.webglobe.server.utils.LocalFileServer;
import edu.buffalo.webglobe.server.utils.NetcdfDir;
import edu.buffalo.webglobe.server.utils.Utils;

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
		String hdfsDir = hdfsAddress.substring(hdfsAddress.indexOf("/user"), hdfsAddress.length());
		String from = dataJson.get("from").getAsString();
		String to = dataJson.get("to").getAsString();
        NetcdfDir netcdfDir = new NetcdfDir(hdfsAddress);
        String saveDir = hdfsDir + "/variable/" + netcdfDir.getVariableName();

		File[] listOfFiles = Utils.createImages(netcdfDir, saveDir, hdfsAddress,hdfsDir,from,to);

		Map<String, String> responseData = new HashMap<String, String>();
		responseData.put("imagesAddress", saveDir);

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
