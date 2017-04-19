package edu.buffalo.webglobe.server.pagecode;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.buffalo.webglobe.server.db.DBUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
/**
 * Servlet implementation class LoadNetcdfDataset
 */
@WebServlet("/LoadNetcdfDataset")
public class LoadNetcdfDataset extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public LoadNetcdfDataset() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
        int id = data.get("id").getAsInt();
        String variableName = data.get("fieldname").getAsString();
        Map<String, Map<String, String>> responseData = new HashMap<String, Map<String, String>>();

        Connection conn;
        Statement stmt;
        ResultSet rset;
        Map<String, String> variableInfo = new HashMap<String, String>();
        try {
            conn = DBUtils.getConnection();
            stmt = conn.createStatement();
            String cmd = "select f.id as field_id from netcdf_datasets as d and netcdf_dataset_fields as f where f.dataset_id="+id+" and f.fieldname = \""+variableName+"\"";
            rset = DBUtils.executeQuery(conn,stmt,cmd);
            if(rset.next()){
                int fieldId = rset.getInt("field_id");
                cmd = "select timestamp, time_index from netcdf_dataset_images where dataset_id="+id+
                        " and field_id="+fieldId+"and (time_index = max(time_index) or time_index = min(time_index)";
                rset = DBUtils.executeQuery(conn,stmt,cmd);
                CalendarDateFormatter dateFormatter = new CalendarDateFormatter("yyyy-MM-dd");
                rset.next();
                int ind1 = rset.getInt(2);
                String ts1 = rset.getString(1);
                rset.next();
                int ind2 = rset.getInt(2);
                String ts2 = rset.getString(1);
                CalendarDate d1 = dateFormatter.parse(ts1);
                CalendarDate d2 = dateFormatter.parse(ts2);
                if(ind1 < ind2){
                    variableInfo.put("minDate", dateFormatter.toString(d1));
                    variableInfo.put("maxDate", dateFormatter.toString(d2));
                }else{
                    variableInfo.put("minDate", dateFormatter.toString(d2));
                    variableInfo.put("maxDate", dateFormatter.toString(d1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        responseData.put("variable", variableInfo);

        String responseJson = new Gson().toJson(responseData);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseJson);

    }

}
