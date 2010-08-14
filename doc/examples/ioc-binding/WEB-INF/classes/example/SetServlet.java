package example;

import javax.inject.Inject;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

public class SetServlet extends HttpServlet {
  // service pattern
  private @Inject MyService _service;

  // resource pattern
  private @Inject @Blue MyResource _blueResource;
  private @Inject @Red MyResource _redResource;

  // plugin/extension pattern
  private @Inject @Any Instance<MyResource> _resources;
  
  // startup pattern
  private @Inject StartupResourceBean _startupResource;
  
  @Override
  public void service(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    response.setContentType("text/html; charset=utf-8");
    PrintWriter out = response.getWriter();

    String color = request.getParameter("color");

    // service and resource patterns
    if ("red".equals(color))
      _service.setMessage(_redResource.getMessage());
    else if ("blue".equals(color))
      _service.setMessage(_blueResource.getMessage());

    writeHeader(out);

    // service pattern
    out.println("<h2>Service Pattern</h2>");

    out.println("<table class='deftable'>");
    out.println("<tr><th>Binding<th>Value");
    out.println("<tr><td>@Inject MyService<td>" + _service);
    out.println("</table>");
    
    // resource pattern
    out.println("<h2>Resource Pattern</h2>");

    out.println("<table class='deftable'>");
    out.println("<tr><th>Binding<th>Value");
    out.println("<tr><td>@Red MyResource<td>" + _redResource);
    out.println("<tr><td>@Blue MyResource<td>" + _blueResource);
    out.println("</table>");

    // plugin/extension pattern
    out.println("<h2>Plugin/Extension Pattern</h2>");
    
    out.println("<ol>");
    for (MyResource resource : _resources) {
      out.println("<li>" + resource);
    }
    out.println("</ol>");
    
    // startup pattern
    out.println("<h2>Startup Pattern</h2>");

    out.println("<table class='deftable'>");
    out.println("<tr><th>Binding<th>Value");
    out.println("<tr><td>@Inject StartupResourceBean<td>" + _startupResource);
    out.println("</table>");

    writeFooter(out);
  }

  private void writeHeader(PrintWriter out)
    throws IOException
  {
    out.println("<title>CanDI Pattern Tutorial: SetServlet</title>");
    out.println("<head><link rel='STYLESHEET' type='text/css' href='../../css/default.css'/></head>");

    out.println("<div class='breadcrumb'>");
    out.println("  <a href='../../'>docs</a>");
    out.println("  / <a href='../'>examples</a>");
    out.println("  / <a href='index.xtp'>CanDI pattern tutorial</a>");
    out.println("</div>");
    
    out.println("<h1>CanDI Pattern Tutorial: SetServlet</h1>");

    out.println("<p>The SetServlet demonstrates the four Java Injection");
    out.println("patterns by injecting the services and resources and");
    out.println("displaying their values in tables.");

    out.println("<p>Click on the 'SetServlet blue' link, the");
    out.println("'SetServlet red' link, and the 'SetServlet' link ");
    out.println("to check that both servlets share the same MyService");
    out.println("instance");
    
    out.println("<h2>Demo Links</h2>");

    out.println("<ul>");
    out.println("<li><a href='set?color=blue'>SetServlet blue (/set?color=blue)</a></li>");
    out.println("<li><a href='set?color=red'>SetServlet red (/set?color=red)</a></li>");
    out.println("<li><a href='get'>GetServlet (/get)</a></li>");
    out.println("<li><a href='index.jsp'>index.jsp</a></li>");
    out.println("<li><a href='index.php'>index.php</a></li>");
    out.println("<li><a href='index.xtp'>CanDI pattern tutorial</a></li>");
    out.println("</ul>");
  }

  private void writeFooter(PrintWriter out)
    throws IOException
  {
    out.println("<h2>Demo Architecture</h2>");
    
    out.println("<img src='../../images/ioc-binding.png'>");
    
    out.println("<h2>SetServlet Code</h2>");
    
    out.println("<div class='example'><pre>");
    out.println("public class SetServlet extends HttpServlet {");
    out.println("  // service pattern");
    out.println("  private @Inject MyService _service;");
    out.println();
    out.println("  // resource pattern");
    out.println("  private @Red MyResource _redService;");
    out.println("  private @Blue MyResource _blueService;");
    out.println();
    out.println("  // plugin/extension pattern");
    out.println("  private @Any Instance&lt;MyResource> _resources;");
    out.println();
    out.println("  // startup pattern");
    out.println("  private @Inject StartupResourceBean _startupResource;");
    out.println();
    out.println("  ...");
    out.println("}");
    out.println("</pre></div>");
  }
}
