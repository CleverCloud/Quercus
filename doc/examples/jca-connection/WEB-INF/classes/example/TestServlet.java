package example;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.naming.InitialContext;
import javax.naming.Context;

/**
 * Implementation of the test servlet.
 */
public class TestServlet extends HttpServlet {
  // Reference to the factory
  private ConnectionFactoryImpl _factory;

  /**
   * <code>init()</code> stores the factory for efficiency since JNDI
   * is relatively slow.
   */
  public void init()
    throws ServletException
  {
    try {
      Context ic = new InitialContext();

      _factory = (ConnectionFactoryImpl) ic.lookup("java:comp/env/factory");
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  /**
   * Use the connection.  All JCA connections must use the following
   * pattern to ensure the connection is closed even when exceptions
   * occur.
   */
  public void service(HttpServletRequest request,
		      HttpServletResponse response)
    throws IOException, ServletException
  {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    ConnectionImpl conn = null;
    
    try {
      out.println("Factory: " + _factory + "<br>");

      conn = _factory.getConnection();

      out.println("Connection: " + conn + "<br>");
    } catch (Exception e) {
      throw new ServletException(e);
    } finally {
      // it is very important to put this close in the finally block
      if (conn != null)
	conn.close();
    }
  }
}
