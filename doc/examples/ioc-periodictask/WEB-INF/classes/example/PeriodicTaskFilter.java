package example;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.inject.Inject;

/**
 * Filter to show a maintenance page if the MaintenanceRunner is active.
 *
 * <h3>init parameters</h3> 
 * <dl>
 * <dt>url
 * <dd>The url to forward to when active, if not set then a 503
 *     (SERVICE_UNAVAILABLE) error code is returned.
 * <dt>min-estimated-time
 * <dd>The minimum amount of time in seconds for an estimate of when the task
 *     will complete, default is 5 seconds.
 * </dl>
 */
public class PeriodicTaskFilter implements Filter {
  static protected final Logger log = 
    Logger.getLogger(PeriodicTaskFilter.class.getName());

  private String _url = null;
  private int _minEstimatedTime = 5;;

  @Inject private PeriodicTask _periodicTask;

  public PeriodicTaskFilter()
  {
  }

  public void setPeriodicTask(PeriodicTask periodicTask)
  {
    _periodicTask = periodicTask;
  }

  /** 
   * The url to forward to when active, if not set then a 503
   * (SERVICE_UNAVAILABLE) error code is returned.
   */ 
  public void setUrl(String url)
  {
    _url = url;
  }

  /** 
   * The minimum amount of time in seconds for an estimate of when the task
   * will complete, default is 5 seconds.
   */ 
  public void setMinEstimatedTime(int seconds)
  {
    _minEstimatedTime = seconds;
  }

  public void init(FilterConfig filterConfig)
    throws ServletException
  {
    String p;

    p = filterConfig.getInitParameter("url");
    if (p != null)
      setUrl(p);

    p = filterConfig.getInitParameter("min-estimated-time");
    if (p != null)
      setMinEstimatedTime(Integer.parseInt(p));
  }

  public void doFilter(ServletRequest request,
      ServletResponse response,
      FilterChain chain)
    throws ServletException, IOException
  {
    if (_periodicTask.isActive()) {
      dispatch( (HttpServletRequest) request, (HttpServletResponse) response);
    }
    else {
      chain.doFilter(request,response);
    }
  }

  /**
   * Disptach to a page that shows a "temporarily unavailable" message, or
   * respond with 503.
   */
  protected void dispatch(HttpServletRequest request,
			  HttpServletResponse response)
    throws ServletException, IOException
  { 
    long remaining = _periodicTask.getEstimatedTimeRemaining();

    // convert to seconds
    remaining = ( (1000L + remaining) / 1000L ) - 1;

    if (remaining < _minEstimatedTime)
      remaining = _minEstimatedTime;

    response.addHeader("Cache-Control", "max-age=" + remaining);
    response.addHeader("refresh", String.valueOf(remaining));

    if (_url != null)
      request.getRequestDispatcher(_url).forward(request,response);
    else
      response.sendError(response.SC_SERVICE_UNAVAILABLE);
  }

  public void destroy()
  {
  }
}

