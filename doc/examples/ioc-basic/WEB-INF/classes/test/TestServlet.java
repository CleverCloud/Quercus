package test;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.inject.Inject;

/**
 * A simple servlet that uses the resource.
 */
public class TestServlet extends HttpServlet {
  private static final Logger log =
    Logger.getLogger(TestServlet.class.getName());

  /**
   * The saved resource from JNDI.
   */
  @Inject private TestResource _resource;

  /**
   * The doGet method just prints out the resource.
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    PrintWriter out = res.getWriter();

    out.println("Resource: " + _resource);
  }
}
