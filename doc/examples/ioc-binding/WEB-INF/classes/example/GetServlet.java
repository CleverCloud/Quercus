package example;

import javax.inject.Inject;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

public class GetServlet extends HttpServlet {
  // service pattern
  private @Inject MyService _service;

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

    writeHeader(out);

    // service pattern
    out.println("<h2>CanDI Service Pattern</h2>");

    out.println("<table class='deftable'>");
    out.println("<tr><th>Binding<th>Value");
    out.println("<tr><td>@Inject MyService<td>" + _service);
    out.println("</table>");

    // plugin/extension pattern
    out.println("<h2>CanDI Plugin/Extension Pattern</h2>");
    
    out.println("<ol>");
    for (MyResource resource : _resources) {
      out.println("<li>" + resource);
    }
    out.println("</ol>");
    
    // startup pattern
    out.println("<h2>CanDI Startup Pattern</h2>");

    out.println("<table class='deftable'>");
    out.println("<tr><th>Binding<th>Value");
    out.println("<tr><td>@Inject StartupResourceBean<td>" + _startupResource);
    out.println("</table>");
    
    writeFooter(out);
  }

  private void writeHeader(PrintWriter out)
    throws IOException
  {
    out.println("<title>CanDI Pattern Tutorial: GetServlet</title>");
    out.println("<head><link rel='STYLESHEET' type='text/css' href='../../css/default.css'/></head>");

    out.println("<div class='breadcrumb'>");
    out.println("  <a href='../../'>docs</a>");
    out.println("  / <a href='../'>examples</a>");
    out.println("  / <a href='index.xtp'>CanDI pattern tutorial</a>");
    out.println("</div>");
    
    out.println("<h1>CanDI Pattern Tutorial: GetServlet</h1>");

    out.println("<p>GetServlet retrieves the service pattern, demonstrating");
    out.println("that the service is shared between all injecting classes");
    
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
    
    out.println("<h2>Code</h2>");

    out.println("<div class='example'><pre>");
    out.println("public class GetServlet extends HttpServlet {");
    out.println("  // service pattern");
    out.println("  private @Inject MyService _service;");
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
