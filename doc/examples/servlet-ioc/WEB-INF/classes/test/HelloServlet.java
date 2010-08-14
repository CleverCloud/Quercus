package test;

import java.io.*;

import javax.servlet.http.*;
import javax.servlet.*;

/**
 * Bean-style initialization servlet.  The greeting parameter is configured
 * in the &lt;init> section of the &lt;servlet>.  The <code>setXXX</code>
 * methods are called before the <code>init()</code> method.
 *
 * <code><pre>
 * &lt;servlet servlet-name='hello'
 *          servlet-class='test.HelloServlet'>
 *   &lt;init>
 *     &lt;greeting>Hello, world&lt;/greeting>
 *   &lt;/init>
 * &lt;/servlet>
 * </pre></code>
 */
public class HelloServlet extends HttpServlet {
  private String _greeting = "Default Greeting";

  /**
   * Sets the greeting.
   */
  public void setGreeting(String greeting)
  {
    _greeting = greeting;
  }

  /**
   * Returns the greeting.
   */
  public String getGreeting()
  {
    return _greeting;
  }
  
  /**
   * Implements the HTTP GET method.  The GET method is the standard
   * browser method.
   *
   * @param request the request object, containing data from the browser
   * @param repsonse the response object to send data to the browser
   */
  public void doGet (HttpServletRequest request,
                     HttpServletResponse response)
    throws ServletException, IOException
  {
    // Returns a writer to write to the browser
    PrintWriter out = response.getWriter();

    // Writes the string to the browser.
    out.println(_greeting);
    out.close();
  }
}
