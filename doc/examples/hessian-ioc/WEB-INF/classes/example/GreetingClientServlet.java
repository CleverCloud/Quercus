package example;

import java.io.PrintWriter;
import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;

import javax.inject.Inject;

/**
 * The greeting client calls the GreetingAPI client.
 */
public class GreetingClientServlet extends GenericServlet {
  @Inject
  private GreetingAPI _greeting;

  /**
   * Runs the servlet
   */
  public void service(ServletRequest req, ServletResponse res)
    throws IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    out.println("Greeting: " + _greeting.greeting());
  }
}
