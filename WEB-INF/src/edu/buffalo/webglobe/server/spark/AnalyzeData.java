package edu.buffalo.webglobe.server.spark;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.buffalo.webglobe.server.utils.Utils;
import edu.buffalo.webglobe.server.spark.Correlation;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

@WebServlet("/AnalyzeData")
public class AnalyzeData extends HttpServlet{
    private static final long serialVersionUID = 1L;
    private String username;
    private String message;
    private String datasetId;
    private String fieldname;
    private String to;
    private String from;
    private String year;
    private String coordinates1;
    private String coordinates2;
    private String analysisoutputname;

    public AnalyzeData(){
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        System.out.println("Hi");
        JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
        this.username = data.get("username").getAsString();
        System.out.println(this.username);
        this.message = data.get("message").getAsString();
        System.out.println(this.message);
        this.datasetId = data.get("datasetId").getAsString();
        System.out.println(this.datasetId);
//        int datasetId1 = Integer.valueOf(this.datasetId);
        this.fieldname = data.get("fieldname").getAsString();
        System.out.println(this.fieldname);
        this.to = data.get("to").getAsString();
        System.out.println(this.to);
        this.from = data.get("from").getAsString();
        System.out.println(this.from);
        this.year = data.get("year").getAsString();
        System.out.println(this.year);
        this.coordinates1 = data.get("coordinates1").getAsString();
        System.out.println(this.coordinates1);
        this.coordinates2 = data.get("coordinates2").getAsString();
        System.out.println(this.coordinates2);
        this.analysisoutputname = data.get("analysisoutputname").getAsString();
        System.out.println(this.analysisoutputname);
        int datasetId = Integer.valueOf("37");
        String fieldname = "air";
        String from = "1948-01-01";
        String to = "2010-03-01";

//        Correlation correlation = new Correlation(datasetId,fieldname,from,to);
        HashMap<String,ArrayList<String>> responsemap = new HashMap<String,ArrayList<String>>();
        Correlation correlation = new Correlation(datasetId,this.fieldname,this.from,this.to);
//        List<Date> dates = correlation.getDates().subList(correlation.getStartTimeIndex(),correlation.getEndTimeIndex());
        ArrayList<String> _dates = new ArrayList<String>();
        int j = 0;
        for(int i=1948;i <= 2010;i++){
            _dates.add(j,String.valueOf(i));
            j++;
        }
//        for(Date d : dates){
//            _dates.add(new SimpleDateFormat("yyyy-MM-dd").format(d));
//        }
        responsemap.put("dates",_dates);

        Utils.logger.info("Starting to load data from HDFS");
//        String location1 = "327.5,-17.5";
//        String location2 = "57.5,-2.5";
        int year = Integer.valueOf(this.year);
        ArrayList<String> corrmap = correlation.readData(this.coordinates1,this.coordinates2,year,this.analysisoutputname);
        Utils.logger.info("Done loading data from HDFS");

//        PrintWriter writer = new PrintWriter("/Users/arun/Desktop/finalmap5.txt", "UTF-8");
//        for (Map.Entry<String, ArrayList<String>> entry : corrmap.entrySet()) {
//            System.out.println(entry.getKey()+" : "+entry.getValue());
////            writer.println(entry.getKey()+" : "+entry.getValue());
//        }
        responsemap.put("data",corrmap);
        try{
            String responseJson = new Gson().toJson(responsemap);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(responseJson);
        }catch (OutOfMemoryError e){
            Utils.logger.severe(e.getMessage());
        }

    }


}
