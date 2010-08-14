package example;

import java.io.PrintWriter;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.inject.Named;
import javax.inject.Inject;

/**
 * The InitServlet initializes the database.
 *
 */
public class InitServlet extends HttpServlet {
  /**
   * The DataSource for the table.
   */
  @Inject @Named("jdbc/basic")
  private DataSource _ds;

  /**
   * Initializes the reference to the CourseBean home interface.
   */
  public void init()
    throws ServletException
  {
    try {
      Connection conn = _ds.getConnection();

      try {
	Statement stmt = conn.createStatement();

	try {
	  ResultSet rs = stmt.executeQuery("SELECT id FROM jdbc_basic_brooms");

	  if (rs.next()) {
	    rs.close();
	    stmt.close();
	    return;  // already initialized
	  }
	} catch (SQLException e) {
	}

	stmt.executeUpdate("CREATE TABLE jdbc_basic_brooms (" +
	                   "  id INTEGER PRIMARY KEY auto_increment," +
	                   "  name VARCHAR(128)," +
	                   "  cost INTEGER" +
	                   ")");
	stmt.executeUpdate("INSERT INTO jdbc_basic_brooms (name, cost) " +
			   "VALUES ('firebolt', 4000)");
	stmt.executeUpdate("INSERT INTO jdbc_basic_brooms (name, cost) " +
			   "VALUES ('nimbus 2001', 500)");
	stmt.executeUpdate("INSERT INTO jdbc_basic_brooms (name, cost) " +
			   "VALUES ('nimbus 2000', 300)");
	stmt.executeUpdate("INSERT INTO jdbc_basic_brooms (name, cost) " +
			   "VALUES ('cleansweep 7', 150)");
	stmt.executeUpdate("INSERT INTO jdbc_basic_brooms (name, cost) " +
			   "VALUES ('cleansweep 5', 100)");
	stmt.executeUpdate("INSERT INTO jdbc_basic_brooms (name, cost) " +
			   "VALUES ('shooting star', 50)");

	stmt.close();
      } finally {
	conn.close();
      }
    } catch (SQLException e) {
      throw new ServletException(e);
    }
  }

  /**
   * 
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    throw new UnsupportedOperationException();
  }
}
