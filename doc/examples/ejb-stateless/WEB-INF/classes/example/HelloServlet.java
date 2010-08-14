package example;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HelloServlet extends HttpServlet {
  // Dependency injection for the hello bean
  @Inject
  private Hello _hello;

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException
  {
    res.setContentType("text/html");

    PrintWriter out = res.getWriter();

    out.println("<pre>");
    out.println(_hello.greeting1());
    out.println(_hello.greeting2());
    out.println("</pre>");
  }
}
