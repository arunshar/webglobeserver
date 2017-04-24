package edu.buffalo.webglobe.server.db;

import edu.buffalo.webglobe.server.utils.Constants;
import edu.buffalo.webglobe.server.utils.Utils;

import java.sql.*;

/**
 * @author chandola
 * @version $Id$
 */
public class DBUtils {
    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/webglobeserver", Constants.DB_USER_NAME, Constants.DB_PASSWORD);
        }  catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public static ResultSet executeQuery(Connection conn, Statement stmt, String query) throws SQLException {
        Utils.logger.info("Executing "+query);
        stmt.executeQuery(query);
        ResultSet resultSet = stmt.getResultSet();
        return resultSet;
    }

    public static void executeUpdate(Connection conn, Statement stmt, String query) throws SQLException {
        Utils.logger.info("Executing "+query);
        stmt.executeUpdate(query);
    }

    public static ResultSet executeInsert(Connection conn, Statement stmt, String query) throws SQLException {
        Utils.logger.info("Executing "+query);
        stmt.executeUpdate(query,Statement.RETURN_GENERATED_KEYS);
        ResultSet resultSet = stmt.getGeneratedKeys();
        return resultSet;
    }
}
