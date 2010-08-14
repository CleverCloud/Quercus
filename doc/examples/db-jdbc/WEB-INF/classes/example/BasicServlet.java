package example;

import java.io.PrintWriter;
import java.io.IOException;

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
 * The BasicServlet executes a simple JDBC query.
 *
 * The DataSource saved on initialization to save the JNDI lookup
 * time.
 */
public class BasicServlet extends HttpServlet {
  /**
   * The saved DataSource for the database
   */
  @Inject @Named("jdbc/basic")
  private DataSource _ds;

  /**
   * Respond to a request by doing a query and returning the results.
   */
  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    res.setContentType("text/html");
    
    PrintWriter out = res.getWriter();

    try {
      doQuery(out);
    } catch (SQLException e) {
      throw new ServletException(e);
    }
  }

  /**
   * Typical pattern for database use.
   */
  public void doQuery(PrintWriter out)
    throws IOException, SQLException
  {
    Connection conn = _ds.getConnection();

    try {
      String sql = "SELECT name, cost FROM jdbc_basic_brooms ORDER BY cost DESC";
      
      Statement stmt = conn.createStatement();

      ResultSet rs = stmt.executeQuery(sql);

      out.println("<table border='3'>");
      
      while (rs.next()) {
	out.println("<tr><td>" + rs.getString(1));
	out.println("    <td>" + rs.getString(2));
      }

      out.println("</table>");
      
      rs.close();
      stmt.close();
    } finally {
      conn.close();
    }
  }
}
