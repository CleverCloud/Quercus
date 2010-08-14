package example;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.inject.Inject;

import com.caucho.servlet.comet.*;

public class TestCometServlet extends GenericServlet
{
  @Inject
  private TimerService _timerService;

  private ArrayList<CometState> _itemList
    = new ArrayList<CometState>();

  @Override
  public void service(ServletRequest request,
                      ServletResponse response)
    throws IOException, ServletException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    if (req.getAttribute("comet") != null) {
      resume(request, response, req.getAsyncContext());
      return;
    }

    req.setAttribute("comet", true);

    PrintWriter out = res.getWriter();
    res.setHeader("Cache-Control", "no-cache, must-revalidate");
    res.setHeader("Expires", "Mon, 27 Jul 1997 05:00:00 GMT");

    res.setContentType("text/html");

    out.println("<html><body>");

    // Padding needed because Safari needs at least 1k data before
    // it will start progressive rendering.
    for (int i = 0; i < 100; i++) {
      out.println("<span></span>");
    }

    out.println("<script type='text/javascript'>");
    out.println("var comet_update = window.parent.comet_update;");
    out.println("</script>");

    AsyncContext async = request.startAsync();
    CometState state = new CometState(request);

    // Add the comet state to the controller
    _timerService.addCometState(state);
  }

  private void resume(ServletRequest request,
                      ServletResponse response,
                      AsyncContext async)
    throws IOException, ServletException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    PrintWriter out = res.getWriter();

    Object count = req.getAttribute("comet.count");

    out.println("<script type='text/javascript'>");
    out.println("comet_update(" + count + ");");
    out.println("</script>");

    Integer iCount = (Integer) count;

    if (iCount != null && iCount < 10)
      req.startAsync();
    else
      req.setAttribute("comet.complete", true);
  }
}
