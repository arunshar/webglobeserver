package edu.buffalo.webglobe.server.db;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import edu.buffalo.webglobe.server.spark.HDFSDataSet;
import edu.buffalo.webglobe.server.utils.Utils;

/**
 * Servlet implementation class LoadData
 */
@WebServlet("/LoadData")
public class LoadData extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public LoadData() {
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
		int datasetId = Integer.parseInt(request.getParameter("datasetId"));
        String fieldName = request.getParameter("fieldname");
		String from = request.getParameter("from");
		String to = request.getParameter("to");
        HDFSDataSet hdfsDataSet = new HDFSDataSet(datasetId,fieldName,from,to);
        HashMap<String, ArrayList<String>> responseData = new HashMap<String, ArrayList<String>>();
        List<Date> dates = hdfsDataSet.getDates().subList(hdfsDataSet.getStartTimeIndex(),hdfsDataSet.getEndTimeIndex()+1);
        ArrayList<String> _dates = new ArrayList<String>();
        for(Date d : dates){
            _dates.add(new SimpleDateFormat("yyyy-MM-dd").format(d));
        }
        responseData.put("dates",_dates);
        //send bounding box
        ArrayList<String> bounds = new ArrayList<String>();
        float[] _bounds = hdfsDataSet.getBounds();
        for(float b: _bounds){
            bounds.add((new Float(b)).toString());
        }
        responseData.put("bounds",bounds);
        //get sizes
        ArrayList<String> shape = new ArrayList<String>();
        shape.add(Integer.toString(hdfsDataSet.getLatNum()));
        shape.add(Integer.toString(hdfsDataSet.getLonNum()));
        shape.add(Integer.toString(hdfsDataSet.getBoundedTimeNum()));
        responseData.put("shape",shape);
        ArrayList<String> limits = new ArrayList<String> ();
        limits.add(Float.toString(hdfsDataSet.getMinValue()));
        limits.add(Float.toString(hdfsDataSet.getMaxValue()));
        responseData.put("limits",limits);
        Utils.logger.info("Starting to load data from HDFS");
        //get data    -- for each time slice it will be a vector of length numlat x numlon in a lat first format
        ArrayList<String> data = hdfsDataSet.readData();
        Utils.logger.info("Done loading data from HDFS");
        responseData.put("data",data);
        try {
            String responseJson = new Gson().toJson(responseData);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(responseJson);
        }catch(OutOfMemoryError e){
            Utils.logger.severe(e.getMessage());
        }

    }
}
