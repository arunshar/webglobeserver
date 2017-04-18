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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Servlet implementation class GetSubmittedJobsInfo
 */
@WebServlet("/GetSubmittedJobsInfo")
public class GetSubmittedJobsInfo extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public GetSubmittedJobsInfo() {
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
		Map<String, Map<String, String>> responseData = new HashMap<String, Map<String, String>>();
		String userName = request.getUserPrincipal().getName();
		// query database
        Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;
		try {
			// Step 1: Allocate a database Connection object
            conn = DBUtils.getConnection();

			stmt = conn.createStatement();
			String cmd = "select J.id as id,J.dataset_id as dataset_id,J.analysis as analysis,J.field as field,J.status as status,J.submission_time as submission_time, J.finish_time as finish_time, J.result_loc as result_loc, J.priority as priority, N.name as name from submitted_analysis_jobs J, netcdf_datasets N where J.dataset_id = N.id and J.user_name = \""
					+ userName + "\" order by J.submission_time";
			rset = stmt.executeQuery(cmd);
			int i = 0;
			while (rset.next()) {
				Map<String, String> submittedJobInfo = new HashMap<String, String>();
				int id = rset.getInt("id");
				String analysis = rset.getString("analysis");
				String field = rset.getString("field");
				String status = rset.getString("status");
				Timestamp submission_time = rset.getTimestamp("submission_time");
				Timestamp finish_time = rset.getTimestamp("finish_time");
				String result_loc = rset.getString("result_loc");
				String priority = rset.getString("priority");
				String name = rset.getString("name");

				submittedJobInfo.put("id", (new Integer(id)).toString());
				submittedJobInfo.put("analysis", analysis);
				submittedJobInfo.put("field", field);
				submittedJobInfo.put("status", status);
				submittedJobInfo.put("submission_time", submission_time.toString());
				submittedJobInfo.put("result_loc", result_loc);
				submittedJobInfo.put("priority", priority);
				submittedJobInfo.put("name", name);
				responseData.put((new Integer(i)).toString(), submittedJobInfo);
				i++;
			}
			rset.close();
			stmt.close();
			conn.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		String responseJson = new Gson().toJson(responseData);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(responseJson);
	}

}
