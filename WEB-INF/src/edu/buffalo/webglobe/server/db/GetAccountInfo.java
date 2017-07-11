package edu.buffalo.webglobe.server.db;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.buffalo.webglobe.server.utils.Constants;
import edu.buffalo.webglobe.server.utils.Utils;


/**
 * Servlet implementation class GetAccountInfo
 */
@WebServlet("/GetAccountInfo")
public class GetAccountInfo extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetAccountInfo() {
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
	    Map<String,  Map<String,String> > responseData = new HashMap<String,  Map<String,String> >();
        String userName;
        JsonObject data = new Gson().fromJson(request.getReader(), JsonObject.class);
        if(Utils.configuration.getValue("AUTHENTICATION_TYPE").equalsIgnoreCase("GLOBUS")){
            userName = data.get("username").getAsString();
        }else{
            userName = request.getUserPrincipal().getName();
        }

        if(userName == null){
            Utils.logger.severe("Error getting username");
            return;
        }
	    Map<String, String> userInfo = new HashMap<String, String>();
	    userInfo.put("userName",userName);
	    responseData.put("userInfo",userInfo);
	    String responseJson = new Gson().toJson(responseData);
	    response.setContentType("application/json");
	    response.setCharacterEncoding("UTF-8");
	    response.getWriter().write(responseJson);
	}

}
