package edu.buffalo.webglobe.server.db;

import com.google.gson.Gson;

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
 * Servlet implementation class GetSubmittedUploadJobsInfo
 */
@WebServlet("/GetSubmittedUploadJobsInfo")
public class GetSubmittedUploadJobsInfo extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see javax.servlet.http.HttpServlet#HttpServlet()
	 */
	public GetSubmittedUploadJobsInfo() {
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
		// TODO Auto-generated method stub
		Map<String, Map<String, String>> responseData = new HashMap<String, Map<String, String>>();
		String userName = request.getUserPrincipal().getName();
		// query database
        Connection conn;
		Statement stmt;
		ResultSet rset;
		try {
			// Step 1: Allocate a database Connection object
            conn = DBUtils.getConnection();

			stmt = conn.createStatement();
			String cmd = "select J.id as id,J.dataset_name as dataset_name,J.status as status,J.submission_time as submission_time, J.finish_time as finish_time, J.priority as priority from submitted_image_creation_jobs J where J.user_name = \""
					+ userName + "\" order by J.submission_time";
			rset = DBUtils.executeQuery(conn, stmt, cmd);
			int i = 0;
			while (rset.next()) {
				Map<String, String> submittedJobInfo = new HashMap<String, String>();
				int id = rset.getInt("id");
				String status = rset.getString("status");
				Timestamp submission_time = rset.getTimestamp("submission_time");
				Timestamp finish_time = rset.getTimestamp("finish_time");
				String priority = rset.getString("priority");
				String name = rset.getString("dataset_name");

				submittedJobInfo.put("id", (new Integer(id)).toString());
				submittedJobInfo.put("status", status);
				submittedJobInfo.put("submission_time", submission_time.toString());
                if(finish_time == null)
                    submittedJobInfo.put("finish_time", "");
                else
                    submittedJobInfo.put("finish_time", finish_time.toString());
				submittedJobInfo.put("priority", priority);
				submittedJobInfo.put("dataset_name", name);
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
