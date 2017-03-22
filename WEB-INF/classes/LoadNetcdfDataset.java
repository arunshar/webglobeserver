

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hadoop.fs.FileSystem;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import ucar.nc2.time.CalendarDateFormatter;
import utils.LocalFileServer;
import utils.NetcdfDir;

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
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
		String hdfsAddress = data.get("url").getAsString();
		String variableName = data.get("fieldname").getAsString();
		Map<String, Map<String, String>> responseData = new HashMap<>();
		
		FileSystem fs = null;
		try {
		
			NetcdfDir netcdfDir = new NetcdfDir(hdfsAddress+"/"+variableName, variableName);
			String saveDir = netcdfDir.getDir() + "/variable/" + netcdfDir.getVariableName();
			CalendarDateFormatter dateFormatter = new CalendarDateFormatter("yyyy-MM-dd");
			
			Map<String, String> variableInfo = new HashMap<>();
			variableInfo.put("name", netcdfDir.getVariableName());
			variableInfo.put("address", hdfsAddress+"/"+variableName);
			variableInfo.put("minDate", dateFormatter.toString(netcdfDir.getStartDate()));
			variableInfo.put("maxDate", dateFormatter.toString(netcdfDir.getEndDate()));
			
			File folder = new File(LocalFileServer.LOCAL_DIRECTORY + saveDir);
			
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
						
		    String responseJson = new Gson().toJson(responseData);
		    response.setContentType("application/json");
		    response.setCharacterEncoding("UTF-8");
		    response.getWriter().write(responseJson);
		} finally {
			if (fs != null)
				fs.close();
		}
	}

}
