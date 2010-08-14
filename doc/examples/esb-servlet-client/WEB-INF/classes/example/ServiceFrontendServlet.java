package example;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.inject.Inject;
import javax.inject.Named;

public class ServiceFrontendServlet extends HttpServlet {
  @Inject @Named("hessian")
  private HelloService _helloService;

  public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    out.println("service result: " + _helloService.hello());
  }
}

