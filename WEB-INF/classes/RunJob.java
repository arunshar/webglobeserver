

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import utils.RunSparkJob;

/**
 * Servlet implementation class RunJob
 */
@WebServlet("/RunJob")
public class RunJob extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public RunJob() {
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
		// TODO Auto-generated method stub
		String userName = request.getUserPrincipal().getName();
		JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
		String datasetid = data.get("datasetid").getAsString();
		String datasetname = data.get("datasetname").getAsString();
		String url =  data.get("url").getAsString();
		String analysisname =  data.get("analysisname").getAsString().replace(" ", "");
		String fieldname =  data.get("fieldname").getAsString();
		
		
		Connection conn = null;
		Statement stmt = null;
		try {
			// Step 1: Allocate a database Connection object
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/webglobeserver", "root", "mysql");
			//conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/webglobeserver", "root", "0pt1musPr1me@");
			stmt = conn.createStatement();
			
			//add an entry to the submitted_jobs table
			SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			String curDate = dt.format(new Date()).toString();
			String cmd = "INSERT INTO submitted_jobs (user_name,dataset_id,analysis,field,status,submission_time,finish_time,result_loc,priority) VALUES (\"" +
						userName + "\"," +
						datasetid + ",\"" +
						analysisname + "\",\"" +
						fieldname + "\",\"" + 
						"RUNNING" + "\",\"" +
						curDate + "\"," +
						"NULL" + ",\"" +
						url + "\",\"1\")";
						
			stmt.executeUpdate(cmd, Statement.RETURN_GENERATED_KEYS);
			
			ResultSet rs = stmt.getGeneratedKeys();
			int id;
			if (rs.next()){
			    id=rs.getInt(1);
			    
			  //run the job
				String[] outputs = RunSparkJob.createSparkCluster("/home/centos/bash-scripts/sparkcluster.sh", 10, "m1.medium");
				
				String hdfsuri = url.substring(0, url.indexOf("/user"));
				String inputDir = url + "/" + fieldname;
				String logHypersFile = url + "/globalLogHypersFile";
				String outputDir = url + "/analysis/" + fieldname + "/" + analysisname;
				String monitorDataFile = url + "/monitorDataFile";
				String colorFileName = "fullcolorvalues.xml";
				String numSlices = "3600";
				String d = "60";
				String monitor_start_ind = "0";
				String monitor_end_ind = "1";
				String noYearTrain = "0.5";
				String monitor_method = "2";
				String ifSmoothSpacially = "1";
				String k = "4"; // neighborhood extent
				String[] arg = {
						hdfsuri,
						inputDir,
						logHypersFile,
						outputDir,
						monitorDataFile,
						colorFileName,
						numSlices,
						d,
						monitor_start_ind,
						monitor_end_ind,
						noYearTrain,
						monitor_method,
						ifSmoothSpacially,
						k};
				String[] jars = {
						"/home/ubuntu/jars/gpchange.jar",
						"/home/ubuntu/jars/jama-1.0.3.jar",
						"/home/ubuntu/jars/netcdfAll-4.6.4.jar",
						"/home/ubuntu/jars/worldwind.jar",
				};

				RunSparkJob
						.runSparkJob(
								"/home/ubuntu/jars/GPChangeSpark-0.0.1-SNAPSHOT.jar",
								"gpchange.spark.main.MonitorGlobalHypers", arg, "/home/ubuntu/spark-1.6.1",
								outputs[2], "2g", "2g", jars);
				
				RunSparkJob.terminateCluster("/home/centos/bash-scripts/terminatecluster.sh", outputs[0]);
			    
				cmd = "UPDATE submitted_jobs SET status='DONE' where id=" + id;
				stmt.executeUpdate(cmd);
				
				cmd = "INSERT INTO netcdf_datasets (name,url,available,info,info_url) VALUES (\"" +
						analysisname + " analysis" + "\",\"" +
						url + "/analysis/" + fieldname + "\",\"" +
						userName + "\",\"" +
						analysisname + " analysis for " + datasetname + "\",\"" + 
						"" + "\")";
			
				stmt.executeUpdate(cmd, Statement.RETURN_GENERATED_KEYS);

				rs = stmt.getGeneratedKeys();
				if (rs.next()){
				    id=rs.getInt(1);
				    
				    cmd = "INSERT INTO netcdf_dataset_fields (dataset_id,field_name,field_description,units,max_value,min_value) VALUES (" +
				    		id + ",\"" +
				    		analysisname + "\",\"" +
				    		analysisname + "\",\"" + 
				    		"" + "\"," +
				    		"0" + "," +
				    		"2" + ")";
				    stmt.executeUpdate(cmd);
				}				
			}
						
			stmt.close();
			conn.close();
		} catch (SQLException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

}
