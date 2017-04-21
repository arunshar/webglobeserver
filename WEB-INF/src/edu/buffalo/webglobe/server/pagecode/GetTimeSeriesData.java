package edu.buffalo.webglobe.server.pagecode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.buffalo.webglobe.server.db.DBUtils;
import edu.buffalo.webglobe.server.utils.NetcdfDir;
import ucar.ma2.Array;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;

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
        JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
        int datasetId = data.get("datasetid").getAsInt();
        String fieldName = data.get("fieldname").getAsString();
        float x = data.get("lon").getAsFloat();
        float y = data.get("lat").getAsFloat();

		ArrayList<String> values = new ArrayList<String>();
        ArrayList<String> dates = new ArrayList<String>();
        String unitString = null;
		HashMap<String, HashMap<String, String>> responseData = new HashMap<String, HashMap<String,String>>();
		try {
			Connection conn = DBUtils.getConnection();
            Statement stmt = conn.createStatement();
            String cmd = "SELECT D.url,F.units from netcdf_datasets as D,netcdf_dataset_fields F where D.id = "+datasetId+
                    " and D.id = F.dataset_id and F.field_name = \""+fieldName+"\"";
            ResultSet rset = DBUtils.executeQuery(conn,stmt,cmd);
            if(rset.next()) {
                String hdfsURL = rset.getString(1);
                unitString = rset.getString(2);
                NetcdfDir netcdfDir = new NetcdfDir(hdfsURL, fieldName);
                List<Array> arrayList = netcdfDir.getTimeSeriesData(x,y);
                for(Array array: arrayList){
                    for(int i = 0; i < array.getSize(); i++){
                        values.add((new Float(array.getFloat(i))).toString());
                    }
                }
                for(CalendarDate d: netcdfDir.getDates()){
                    dates.add(CalendarDateFormatter.toDateString(d));
                }

            }
		} catch (SQLException e) {
            e.printStackTrace();
        }

        for(int i = 0; i < dates.size(); i++){
            HashMap<String, String> info = new HashMap<String, String>();
            info.put("date",dates.get(i));
            info.put("value",values.get(i));
            if(i == 0){
                info.put("unitString",unitString);
            }
            responseData.put((new Integer(i)).toString(), info);
        }
        String responseJson = new Gson().toJson(responseData);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseJson);
    }

}
