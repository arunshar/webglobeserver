package edu.buffalo.webglobe.server.db;

import edu.buffalo.webglobe.server.utils.Constants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author chandola
 * @version $Id$
 */
public class DBUtils {

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("ALERT: WEBGLOBESERVER: "+Constants.DB_PASSWORD+" "+Constants.DB_USER_NAME);
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/webglobeserver", Constants.DB_USER_NAME, Constants.DB_PASSWORD);
        }  catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return conn;
    }

}
