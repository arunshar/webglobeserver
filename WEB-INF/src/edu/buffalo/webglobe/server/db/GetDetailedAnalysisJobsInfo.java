package edu.buffalo.webglobe.server.db;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet implementation class GetSubmittedAnalysisJobsInfo
 */
@WebServlet("/GetSubmittedAnalysisJobsInfo")
public class GetDetailedAnalysisJobsInfo extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see javax.servlet.http.HttpServlet#HttpServlet()
	 */
	public GetDetailedAnalysisJobsInfo() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
        JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
        int jobId = data.get("jobId").getAsInt();
		// query database
        Connection conn;
		Statement stmt;
		ResultSet rset;
        Map<String, String> detailedAnalysisInfo = new HashMap<String, String>();

        try {
			// Step 1: Allocate a database Connection object
            conn = DBUtils.getConnection();

			stmt = conn.createStatement();
			String cmd = "select J.id as id,J.dataset_id as dataset_id,J.analysis as analysis,J.field as field,J.status as status,J.submission_time as submission_time, J.finish_time as finish_time, J.result_loc as result_loc, J.priority as priority, N.name as name from submitted_analysis_jobs J, netcdf_datasets N where J.id = "+jobId;
            rset = DBUtils.executeQuery(conn, stmt, cmd);

            if(rset.next()){

				int id = rset.getInt("id");
				String analysis = rset.getString("analysis");
				String field = rset.getString("field");
				String status = rset.getString("status");
				Timestamp submission_time = rset.getTimestamp("submission_time");
				Timestamp finish_time = rset.getTimestamp("finish_time");
				String result_loc = rset.getString("result_loc");
				String priority = rset.getString("priority");
				String name = rset.getString("name");

				detailedAnalysisInfo.put("id", (new Integer(id)).toString());
				detailedAnalysisInfo.put("analysis", analysis);
				detailedAnalysisInfo.put("field", field);
				detailedAnalysisInfo.put("status", status);
                detailedAnalysisInfo.put("submission_time", submission_time.toString());
                if(finish_time == null)
                    detailedAnalysisInfo.put("finish_time", "");
                else
                    detailedAnalysisInfo.put("finish_time", finish_time.toString());
				detailedAnalysisInfo.put("result_loc", result_loc);
				detailedAnalysisInfo.put("priority", priority);
				detailedAnalysisInfo.put("name", name);

			}
			rset.close();
			stmt.close();
			conn.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		String responseJson = new Gson().toJson(detailedAnalysisInfo);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(responseJson);
	}

}
