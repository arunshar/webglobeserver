package edu.buffalo.webglobe.server.pagecode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.buffalo.webglobe.server.db.DBUtils;
import edu.buffalo.webglobe.server.netcdf.NetcdfDirectory;
import edu.buffalo.webglobe.server.netcdf.NetcdfFile;
import edu.buffalo.webglobe.server.netcdf.NetcdfSource;
import edu.buffalo.webglobe.server.netcdf.NetcdfVariable;
import edu.buffalo.webglobe.server.utils.Utils;
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

import static edu.buffalo.webglobe.server.utils.Constants.VALID_EXTENSIONS;


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
        HashMap<String, HashMap<String, String>> responseData = new HashMap<String, HashMap<String, String>>();
        try {
            Connection conn = DBUtils.getConnection();
            Statement stmt = conn.createStatement();
            String cmd = "SELECT D.url,F.units from netcdf_datasets as D,netcdf_dataset_fields F where D.id = " + datasetId +
                    " and D.id = F.dataset_id and F.field_name = \"" + fieldName + "\"";
            ResultSet rset = DBUtils.executeQuery(conn, stmt, cmd);
            if (rset.next()) {
                String url = rset.getString(1);
                unitString = rset.getString(2);
                String[] tokens = Utils.parseURL(url);
                String protocol = tokens[0];
                String uri = tokens[1];
                String dir = tokens[2];

                //check if the URL points to a file or a directory
                NetcdfSource ncDir = null;
                if (dir.contains(".")) {
                    if (VALID_EXTENSIONS.contains(dir.substring(dir.lastIndexOf(".") + 1,dir.length()))) {
                        ncDir = new NetcdfFile(protocol, uri, dir);
                    }
                } else {
                    if (protocol.equalsIgnoreCase("hdfs")) {
                        ncDir = new NetcdfDirectory(protocol, uri, dir);
                    }
                }
                if (ncDir != null) {
                    NetcdfVariable netcdfVariable = new NetcdfVariable(ncDir, fieldName);
                    List<Array> arrayList = netcdfVariable.getTimeSeriesData(x, y);
                    for (Array array : arrayList) {
                        for (int i = 0; i < array.getSize(); i++) {
                            values.add((new Float(array.getFloat(i))).toString());
                        }
                    }
                    for (CalendarDate d : ncDir.getDates()) {
                        dates.add(CalendarDateFormatter.toDateString(d));
                    }
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < dates.size(); i++) {
            HashMap<String, String> info = new HashMap<String, String>();
            info.put("date", dates.get(i));
            info.put("value", values.get(i));
            if (i == 0) {
                info.put("unitString", unitString);
            }
            responseData.put((new Integer(i)).toString(), info);
        }
        String responseJson = new Gson().toJson(responseData);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseJson);
    }
}
