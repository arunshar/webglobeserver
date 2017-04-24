package edu.buffalo.webglobe.server.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import edu.buffalo.webglobe.server.utils.Utils;

/**
 * Servlet implementation class LoadImages
 */
@WebServlet("/LoadImages")
public class LoadImages extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public LoadImages() {
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
		ArrayList<String> imageUrls = new ArrayList<String>();
        ArrayList<String> imageDates = new ArrayList<String>();
		HashMap<String, ArrayList<String>> responseData = new HashMap<String, ArrayList<String>>();
		try {
			Connection conn = DBUtils.getConnection();
            Statement stmt = conn.createStatement();
            String cmd = "SELECT D.timestamp,D.img_location from netcdf_dataset_images as D, netcdf_dataset_fields as F where D.dataset_id = "+
                    datasetId+" and D.field_id = F.id and F.field_name=\""+fieldName+
                    "\" and str_to_date(D.timestamp,'%Y-%m-%d') >= \""+from+"\" and str_to_date(D.timestamp,'%Y-%m-%d') <= \""+to+"\"";
            ResultSet rset = DBUtils.executeQuery(conn,stmt,cmd);
            while(rset.next()){
                imageDates.add(rset.getDate(1).toString());
                imageUrls.add(rset.getString(2));
            }
            responseData.put("imageUrls",imageUrls);
            responseData.put("imageDates",imageDates);
            String responseJson = new Gson().toJson(responseData);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(responseJson);
            stmt.close();
            conn.close();
		} catch (SQLException e) {
            Utils.logger.severe(e.getMessage());
        }
    }

}
