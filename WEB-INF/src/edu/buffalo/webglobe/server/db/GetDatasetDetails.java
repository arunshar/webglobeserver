package edu.buffalo.webglobe.server.db;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.lang.Integer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.buffalo.webglobe.server.utils.Constants;
import edu.buffalo.webglobe.server.utils.Utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Servlet implementation class GetDatasetDetails
 */
@WebServlet("/GetDatasetDetails")
public class GetDatasetDetails extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public GetDatasetDetails() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
        String userName;
        JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
        if(Constants.AUTHENTICATION_TYPE.equalsIgnoreCase("GLOBUS")){
            userName = data.get("username").getAsString();
        }else{
            userName = request.getUserPrincipal().getName();
        }

        if(userName == null){
            Utils.logger.severe("Error getting username");
            return;
        }

		Map<String, Map<String, String>> responseData = new HashMap<String, Map<String, String>>();
		// query database

		Connection conn;
		Statement stmt, stmt1;
		ResultSet rset, rset1;
		try {
			// Step 1: Allocate a database Connection object
            conn = DBUtils.getConnection();
			stmt = conn.createStatement();
			String cmd = "select id,name,info,info_url,time_min,time_max from netcdf_datasets where is_accessible = 1";
			rset = DBUtils.executeQuery(conn,stmt,cmd);
			int i = 0;
			while (rset.next()) {
				Map<String, String> datasetInfo = new HashMap<String, String>();
				String name = rset.getString("name");
				String info = rset.getString("info");
				String info_url = rset.getString("info_url");

                Date tsmin = rset.getTimestamp("time_min");
                Date tsmax = rset.getTimestamp("time_max");


                datasetInfo.put("mindate", new SimpleDateFormat("yyyy-MM-dd").format(tsmin));
                datasetInfo.put("maxdate", new SimpleDateFormat("yyyy-MM-dd").format(tsmax));


                int id = rset.getInt("id");
				datasetInfo.put("id", (new Integer(id)).toString());
				datasetInfo.put("name", name);
				datasetInfo.put("info", info);
				datasetInfo.put("info_url", info_url);

				// get fields
				String cmd1 = "select id,field_name from netcdf_dataset_fields where dataset_id = " + id;
				stmt1 = conn.createStatement();
				rset1 = DBUtils.executeQuery(conn,stmt1,cmd1);
				int j = 0;
				while (rset1.next()) {
					datasetInfo.put("field" + j, rset1.getString("field_name"));
                    j++;
				}
				datasetInfo.put("fieldcount", (new Integer(j)).toString());
				responseData.put("dataset" + i, datasetInfo);
				rset1.close();
				stmt1.close();
				i++;
			}
			Map<String, String> countInfo = new HashMap<String, String>();
			countInfo.put("value", (new Integer(i)).toString());
			responseData.put("count", countInfo);
			rset.close();
			stmt.close();
			conn.close();
		} catch (SQLException ex) {
            Utils.logger.severe(ex.getMessage());
		}
		String responseJson = new Gson().toJson(responseData);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(responseJson);
	}

}
