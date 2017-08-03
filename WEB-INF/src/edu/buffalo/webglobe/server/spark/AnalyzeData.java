package edu.buffalo.webglobe.server.spark;

import edu.buffalo.webglobe.server.utils.Utils;

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

    public AnalyzeData(){
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        System.out.println("Hi");
        int datasetId = Integer.valueOf("37");
        String fieldname = "air";
        String from = "1948-01-01";
        String to = "2010-03-01";
//        int year = Integer.parseInt(request.getParameter("analysisoutputname"));
//        System.out.println("Year is : "+year);
        Correlation correlation = new Correlation(datasetId,fieldname,from,to);
        HashMap<String, ArrayList<String>> responseData = new HashMap<String, ArrayList<String>>();
        List<Date> dates = correlation.getDates().subList(correlation.getStartTimeIndex(),correlation.getEndTimeIndex());
        ArrayList<String> _dates = new ArrayList<String>();
        for(Date d : dates){
            _dates.add(new SimpleDateFormat("yyyy-MM-dd").format(d));
        }
        responseData.put("dates",_dates);
        ArrayList<String> bounds = new ArrayList<String>();
        float[] _bounds = correlation.getBounds();
        for(float b: _bounds){
            bounds.add((new Float(b)).toString());
        }
        responseData.put("bounds",bounds);

        ArrayList<String> shape = new ArrayList<String>();
        shape.add(Integer.toString(correlation.getLatNum()));
        shape.add(Integer.toString(correlation.getLonNum()));
        shape.add(Integer.toString(correlation.getBoundedTimeNum()));
        responseData.put("shape",shape);
        ArrayList<String> limits = new ArrayList<String> ();
        limits.add(Float.toString(correlation.getMinValue()));
        limits.add(Float.toString(correlation.getMaxValue()));
        responseData.put("limits",limits);
        Utils.logger.info("Starting to load data from HDFS");
        HashMap<String, ArrayList<String>> data = correlation.readData();
        Utils.logger.info("Done loading data from HDFS");
//        responseData.put("data",data);

//        PrintWriter writer = new PrintWriter("/Users/arun/Desktop/finalmap5.txt", "UTF-8");
        for (Map.Entry<String, ArrayList<String>> entry : data.entrySet()) {
            System.out.println(entry.getKey()+" : "+entry.getValue());
//            writer.println(entry.getKey()+" : "+entry.getValue());
        }

    }


}
