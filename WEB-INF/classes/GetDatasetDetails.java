
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

import java.sql.Connection;
import java.sql.DriverManager;
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
		Map<String, Map<String, String>> responseData = new HashMap<>();
		// query database

		Connection conn = null;
		Statement stmt = null, stmt1 = null;
		ResultSet rset = null, rset1 = null;
		try {
			// Step 1: Allocate a database Connection object
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/webglobeserver", "root", "mysql");
			//conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/webglobeserver", "root", "0pt1musPr1me@");
			stmt = conn.createStatement();
			String cmd = "select id,name,url,available,info,info_url from netcdf_datasets where available = \"all\" or available = \""
					+ userName + "\"";
			rset = stmt.executeQuery(cmd);
			int i = 0;
			while (rset.next()) {
				Map<String, String> datasetInfo = new HashMap<>();
				String name = rset.getString("name");
				String url = rset.getString("url");
				String available = rset.getString("available");
				String info = rset.getString("info");
				String info_url = rset.getString("info_url");
				int id = rset.getInt("id");
				datasetInfo.put("id", (new Integer(id)).toString());
				datasetInfo.put("name", name);
				datasetInfo.put("url", url);
				datasetInfo.put("available", available);
				datasetInfo.put("info", info);
				datasetInfo.put("info_url", info_url);
				// get fields
				String cmd1 = "select field_name from netcdf_dataset_fields where dataset_id = " + id;
				stmt1 = conn.createStatement();
				rset1 = stmt1.executeQuery(cmd1);
				int j = 0;
				while (rset1.next()) {
					Map<String, String> fieldInfo = new HashMap<>();
					datasetInfo.put("field" + j, rset1.getString("field_name"));
					j++;
				}
				datasetInfo.put("fieldcount", (new Integer(j)).toString());
				responseData.put("dataset" + i, datasetInfo);
				rset1.close();
				stmt1.close();
				i++;
			}
			Map<String, String> countInfo = new HashMap<>();
			countInfo.put("value", (new Integer(i)).toString());
			responseData.put("count", countInfo);
			rset.close();
			stmt.close();
			conn.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		String responseJson = new Gson().toJson(responseData);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(responseJson);
	}

}
