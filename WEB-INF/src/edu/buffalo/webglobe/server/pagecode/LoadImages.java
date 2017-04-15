package edu.buffalo.webglobe.server.pagecode;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.buffalo.webglobe.server.utils.LocalFileServer;

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
		String imagesAddress = request.getParameter("url");
		String from = request.getParameter("from");
		String to = request.getParameter("to");
				
		String imageUrls = "";
			
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		
		try {
			Date startDate = dateFormatter.parse(from);
			Date endDate = dateFormatter.parse(to);
			
			Calendar start = Calendar.getInstance();
			start.setTime(startDate);
			Calendar end = Calendar.getInstance();
			end.setTime(endDate);

			for (Date date = start.getTime(); start.before(end) || start.equals(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
				String filePath = imagesAddress + "/" + dateFormatter.format(date) + ".png";
				if (new File(LocalFileServer.LOCAL_DIRECTORY + filePath).exists())
					imageUrls += LocalFileServer.PUBLIC_ADDRESS + filePath + ","; 
			}
			
			response.getWriter().append(imageUrls);

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
