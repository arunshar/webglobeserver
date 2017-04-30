package edu.buffalo.webglobe.server.db;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;
import java.lang.Integer;

import com.google.gson.Gson;
import edu.buffalo.webglobe.server.utils.Utils;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;

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
		String userName = request.getUserPrincipal().getName();
		Map<String, Map<String, String>> responseData = new HashMap<String, Map<String, String>>();
		// query database

		Connection conn;
		Statement stmt, stmt1;
		ResultSet rset, rset1;
		try {
			// Step 1: Allocate a database Connection object
            conn = DBUtils.getConnection();
			stmt = conn.createStatement();
			String cmd = "select id,name,url,available,info,info_url,is_analyzable from netcdf_datasets where is_accessible = 1 and (available = \"all\" or available = \""
					+ userName + "\")";
			rset = DBUtils.executeQuery(conn,stmt,cmd);
			int i = 0;
			while (rset.next()) {
				Map<String, String> datasetInfo = new HashMap<String, String>();
				String name = rset.getString("name");
				String url = rset.getString("url");
				String available = rset.getString("available");
				String info = rset.getString("info");
				String info_url = rset.getString("info_url");
                int is_analyzable = rset.getInt("is_analyzable");
				int id = rset.getInt("id");
				datasetInfo.put("id", (new Integer(id)).toString());
				datasetInfo.put("name", name);
				datasetInfo.put("url", url);
				datasetInfo.put("available", available);
				datasetInfo.put("info", info);
				datasetInfo.put("info_url", info_url);
                datasetInfo.put("is_analyzable", Integer.toString(is_analyzable));
				// get fields
				String cmd1 = "select id,field_name from netcdf_dataset_fields where dataset_id = " + id;
				stmt1 = conn.createStatement();
				rset1 = DBUtils.executeQuery(conn,stmt1,cmd1);
				int j = 0;
				while (rset1.next()) {
					datasetInfo.put("field" + j, rset1.getString("field_name"));
                    // get time limits
                    CalendarDateFormatter dateFormatter = new CalendarDateFormatter("yyyy-MM-dd");

                    int fieldId = rset1.getInt("id");
                    String cmd2;
                    Statement stmt2;
                    ResultSet rset2;
                    cmd2 = "select timestamp from netcdf_dataset_images where dataset_id="+id+
                            " and field_id="+fieldId+" and time_index = (select min(time_index) from netcdf_dataset_images where dataset_id="+id+" and field_id="+fieldId+")";
                    stmt2 = conn.createStatement();
                    rset2 = DBUtils.executeQuery(conn,stmt2,cmd2);
                    rset2.next();
                    String tsmin = rset.getString(1);

                    cmd2 = "select timestamp from netcdf_dataset_images where dataset_id="+id+
                            " and field_id="+fieldId+" and time_index = (select max(time_index) from netcdf_dataset_images where dataset_id="+id+" and field_id="+fieldId+")";
                    stmt2 = conn.createStatement();
                    rset2 = DBUtils.executeQuery(conn,stmt2,cmd2);
                    rset2.next();
                    String tsmax = rset.getString(1);

                    CalendarDate d1 = dateFormatter.parse(tsmin);
                    CalendarDate d2 = dateFormatter.parse(tsmax);
                    datasetInfo.put("field"+j+"_mindate", dateFormatter.toString(d1));
                    datasetInfo.put("field"+j+"_maxdate", dateFormatter.toString(d2));
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
