package edu.buffalo.webglobe.server.pagecode;

import com.google.gson.Gson;
import edu.buffalo.webglobe.server.db.DBUtils;
import edu.buffalo.webglobe.server.utils.NetcdfDir;
import ucar.ma2.Array;
import ucar.nc2.time.CalendarDate;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Servlet implementation class GetTimeSeriesData
 */
@WebServlet("/GetTimeSeriesData")
public class GetTimeSeriesData extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * @see javax.servlet.http.HttpServlet#HttpServlet()
     */
    public GetTimeSeriesData() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Logger logger = Logger.getLogger("WEBGLOBE.LOGGER");
        logger.info("~~~~~~~~~~~~~~~~~Coming in here ~~~~~~~~~~~~~~~~~"+request.getParameter("id"));
		int datasetId = Integer.parseInt(request.getParameter("id"));
        String fieldName = request.getParameter("fieldname");
        float x = Float.parseFloat(request.getParameter("lon"));
        float y = Float.parseFloat(request.getParameter("lon"));
		ArrayList<String> values = new ArrayList<String>();
        ArrayList<String> dates = new ArrayList<String>();
        ArrayList<String> unitStrings = new ArrayList<String>();
		HashMap<String, ArrayList<String>> responseData = new HashMap<String, ArrayList<String>>();
		try {
			Connection conn = DBUtils.getConnection();
            Statement stmt = conn.createStatement();
            String cmd = "SELECT D.url,F.units from netcdf_datasets as D,netcdf_dataset_fields F where D.id = "+datasetId+
                    " and D.id = F.dataset_id and F.field_name = \""+fieldName+"\"";
            ResultSet rset = DBUtils.executeQuery(conn,stmt,cmd);
            if(rset.next()) {
                String hdfsURL = rset.getString(1);
                unitStrings.add(rset.getString(2));
                NetcdfDir netcdfDir = new NetcdfDir(hdfsURL, fieldName);
                List<Array> arrayList = netcdfDir.getTimeSeriesData(x,y);
                for(Array array: arrayList){
                    for(int i = 0; i < array.getSize(); i++){
                        values.add((new Float(array.getFloat(i))).toString());
                    }
                }
                for(CalendarDate d: netcdfDir.getDates()){
                    dates.add(d.toString());
                }

            }
		} catch (SQLException e) {
            e.printStackTrace();
        }
        responseData.put("values", values);
        responseData.put("dates", dates);
        responseData.put("unitString", unitStrings);
        String responseJson = new Gson().toJson(responseData);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseJson);
    }

}
