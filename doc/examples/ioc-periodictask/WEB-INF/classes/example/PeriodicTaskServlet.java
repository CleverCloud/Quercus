package example;

import com.caucho.util.ThreadPool;
import com.caucho.util.ThreadTask;

import java.io.*;

import java.text.DateFormat;
import java.text.NumberFormat;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.concurrent.Executor;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.inject.Inject;

/**
 * A Servlet that provides a user interface for managing a PeriodicTask.
 */
public class PeriodicTaskServlet extends HttpServlet {
  static protected final Logger log = 
    Logger.getLogger(PeriodicTaskServlet.class.getName());

  int _refreshRate = 5;

  @Inject
  private Executor _executor;

  @Inject
  private PeriodicTask _periodicTask;

  private NumberFormat _numberFormat ;
  private DateFormat _dateFormat ;

  public PeriodicTaskServlet()
  {
  }

  /**
   * The refresh rate in seconds to send to the browser to cause automatic
   * refresh, default 5, <= 0 disables.
   */
  public void setRefreshRate(int refreshRate)
  {
    _refreshRate = 5;
  }

  public void init()
    throws ServletException
  {
    _numberFormat = NumberFormat.getInstance();
    _dateFormat = DateFormat.getInstance();

    String p;

    p = getInitParameter("refresh-rate");
    if (p != null)
      setRefreshRate(Integer.parseInt(p));
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    final PeriodicTask task = _periodicTask;

    String msg = null;

    if (request.getParameter("RUN") != null) {
      if (task.isActive())
        msg = "Already active.";
      else {
        // It's tricky to start another Thread from a Servlet.  Here
        // the Resin ThreadPool class is used.  ThreadPool will interrupt a
        // thread and stop it if it runs for too long.
        ThreadTask threadTask = new ThreadTask() {
          public void run()
          {
            task.run();
          }
        };
        _executor.execute(threadTask);
        Thread.yield();
        response.sendRedirect(request.getRequestURI());
      }
    }

    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    // stop browser from caching the page
    response.setHeader("Cache-Control","no-cache,post-check=0,pre-check=0");
    response.setHeader("Pragma","no-cache");
    response.setHeader("Expires","Thu,01Dec199416:00:00GMT");

    if (_refreshRate > 0) {
      response.addHeader( "refresh", String.valueOf(_refreshRate) + "; URL=" + request.getRequestURI());
    }

    out.println("<html>");
    out.println("<head><title>PeriodicTask</title></head>");
    out.println("<body>");
    out.println("<h1>PeriodicTask</h1>");

    if (msg != null) {
      out.print("<p><b>");
      printSafeHtml(out,msg);
      out.println("</b></p>");
    }

    out.println("<table border='0'>");

    printHeading(out,"configuration");

    printPeriod(out,"estimated-average-time:", task.getEstimatedAverageTime());

    printHeading(out,"statistics");

    printField(out,"active", task.isActive());
    printPeriod(out,"estimated-time-remaining",task.getEstimatedTimeRemaining());
    printDate(out,"last-active-time", task.getLastActiveTime());
    printField(out,"total-active-count", task.getTotalActiveCount());
    printPeriod(out,"total-active-time", task.getTotalActiveTime());
    printPeriod(out,"average-active-time", task.getAverageActiveTime());

    printHeading(out,"actions");
    printActions(out,new String[][] { {"RUN", "Run"} });

    out.println("</table border='0'>");


    out.println("</body>");
    out.println("</html>");
  }

  protected void printHeading(PrintWriter out, String heading)
  {
    out.print("<tr><td colspan='2'><h2>");
    printSafeHtml(out,heading);
    out.println("</h2></td></tr>");
  }
  
  protected void printField(PrintWriter out, String name, String value)
  {
    out.print("<tr><td>");
    printSafeHtml(out,name);
    out.print("</td><td>");
    printSafeHtml(out,value);
    out.print("</td></tr>");
  }

  protected void printField(PrintWriter out, String name, boolean value)
  {
    printField(out,name, value == true ? "true" : "false");
  }

  protected void printField(PrintWriter out, String name, long value)
  {
    printField(out,name, _numberFormat.format(value));
  }
  protected void printDate(PrintWriter out, String name, long date)
  {
    printField(out,name, _dateFormat.format(new Date(date)));
  }

  protected void printPeriod(PrintWriter out, String name, long millis)
  {
    double sec = millis / 1000;
    printField(out,name, _numberFormat.format(millis) + "sec");
  }

  protected void printActions(PrintWriter out, String[][] actions)
  {
    out.println("<tr><td colspan='2'><form>");
    for (int i = 0; i < actions.length; i++) {
      out.print("<input type='submit' name='");
      printSafeHtml(out,actions[i][0]);
      out.print("' value='");
      printSafeHtml(out,actions[i][1]);
      out.println("'/>");
    }
    out.println("</form></td></tr>");
  }

  protected void printSafeHtml(PrintWriter out, String text)
  {
    int len = text.length();

    for (int i = 0; i < len; i++) {
      char ch = text.charAt(i);
      switch (ch) {
        case '<':
          out.print("&lt;");
          break;
        case '>':
          out.print("&gt;");
          break;
        case '&':
          out.print("&amp;");
          break;
        default:
          out.print(ch);
      }
    }
  }
}
