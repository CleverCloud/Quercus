package example.filters;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;
/**
 * A cut-and-paste template for implementing a Filter that set's response headers 
 */

public class ExampleResponseHeadersFilter implements Filter {
  private static final Logger log = Logger.getLogger("example.filters.ExampleResponseHeadersFilter");

  /** 
   * Called once to initialize the Filter.  If init() does not
   * complete successfully (it throws an exception, or takes a really
   * long time to return), the Filter will not be placed into service.
   */
  public void init(FilterConfig config)
    throws ServletException
  {
    ServletContext app = config.getServletContext();

    // an example of getting an init-param
    String myParam = config.getInitParameter("my-param");
    if (log.isLoggable(Level.CONFIG))
      log.log(Level.CONFIG,"my-param value is `" + myParam + "'");
  }

  /**
   * Called by Resin each time a request/response pair is passed
   * through the chain due to a client request for a resource at the
   * end of the chain.  The FilterChain parameter is used by the
   * Filter to pass on the request and response to the next Filter in
   * the chain.
   */
  public void doFilter(ServletRequest request, ServletResponse response,
                       FilterChain nextFilter)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    

    // call the next filter in the chain
    nextFilter.doFilter(req, res);

    // directly set headers on the response after invokation of the
    // filter chain

    // this example stops the browser from caching the page
    log.log(Level.FINER,"setting response headers to stop browser caching");

    res.setHeader("Cache-Control","no-cache,post-check=0,pre-check=0,no-store");
    res.setHeader("Pragma","no-cache");
    res.setHeader("Expires","Thu,01Dec199416:00:00GMT");
  }
  
  /**
   * Any cleanup for the filter.  This will only happen once, right
   * before the Filter is released by Resin for garbage collection.
   */

  public void destroy()
  {
  }
}
