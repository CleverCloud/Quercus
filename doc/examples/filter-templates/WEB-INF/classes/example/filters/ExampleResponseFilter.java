package example.filters;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A cut-and-paste template for implementing a Filter that wraps the Response
 */

public class ExampleResponseFilter implements Filter {
  private static final Logger log = Logger.getLogger("example.filters.ExampleResponseFilter");

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
    
    // "wrap" the response object.  Any filter or servlet or jsp that
    // follows in the chain will get this response object 
    // instead of the original Response.
    res = new ExampleResponseWrapper(res);
    
    // call the next filter in the chain
    nextFilter.doFilter(req, res);
  }
  
  /**
   * Any cleanup for the filter.  This will only happen once, right
   * before the Filter is released by Resin for garbage collection.
   */
  public void destroy()
  {
  }
  

  /**
   * This example response wrapper includes all of the methods you
   * could possibly want to implement.  The implementaions here just
   * call the method in the super class, implement the ones you want
   * and remove the ones you don't need to change.
   */
  class ExampleResponseWrapper extends HttpServletResponseWrapper {
    ExampleResponseWrapper(HttpServletResponse response) 
    {
      super(response);
    }

    /**
     * Sets the HTTP status
     *
     * @param sc the HTTP status code
     */
    public void setStatus(int sc)
    {
      super.setStatus(sc);
    }

    public void setStatus(int sc, String msg)
    {
      super.setStatus(sc, msg);
    }

    /**
     * Sends an HTTP error page based on the status code
     *
     * @param sc the HTTP status code
     */
    public void sendError(int sc, String msg)
      throws IOException
    {
      super.sendError(sc, msg);
    }
    
    /**
     * Sends an HTTP error page based on the status code
     *
     * @param sc the HTTP status code
     */
    public void sendError(int sc)
      throws IOException
    {
      super.sendError(sc);
    }
    
    /**
     * Redirects the client to another page.
     *
     * @param location the location to redirect to.
     */
    public void sendRedirect(String location)
      throws IOException
    {
      super.sendRedirect(location);
    }
    
    /**
     * Sets a header.  This will override a previous header
     * with the same name.
     *
     * @param name the header name
     * @param value the header value
     */
    public void setHeader(String name, String value)
    {
      super.setHeader(name, value);
    }
    
    /**
     * Adds a header.  If another header with the same name exists, both
     * will be sent to the client.
     *
     * @param name the header name
     * @param value the header value
     */
    public void addHeader(String name, String value)
    {
      super.addHeader(name, value);
    }
    
    /**
     * Returns true if the output headers include <code>name</code>
     *
     * @param name the header name to test
     */
    public boolean containsHeader(String name)
    {
      return super.containsHeader(name);
    }
    
    /**
     * Sets a header by converting a date to a string.
     *
     * <p>To set the page to expire in 15 seconds use the following:
     * <pre><code>
     * long now = System.currentTime();
     * response.setDateHeader("Expires", now + 15000);
     * </code></pre>
     *
     * @param name name of the header
     * @param date the date in milliseconds since the epoch.
     */
    public void setDateHeader(String name, long date)
    {
      super.setDateHeader(name, date);
    }
    
    /**
     * Adds a header by converting a date to a string.
     *
     * @param name name of the header
     * @param date the date in milliseconds since the epoch.
     */
    public void addDateHeader(String name, long date)
    {
      super.addDateHeader(name, date);
    }
    
    /**
     * Sets a header by converting an integer value to a string.
     *
     * @param name name of the header
     * @param value the value as an integer
     */
    public void setIntHeader(String name, int value)
    {
      super.setIntHeader(name, value);
    }
    
    /**
     * Adds a header by converting an integer value to a string.
     *
     * @param name name of the header
     * @param value the value as an integer
     */
    public void addIntHeader(String name, int value)
    {
      super.addIntHeader(name, value);
    }
    
    /**
     * Sends a new cookie to the client.
     */
    public void addCookie(Cookie cookie)
    {
      super.addCookie(cookie);
    }
    
    /**
     * Encodes session information in a URL. Calling this will enable
     * sessions for users who have disabled cookies.
     *
     * @param url the url to encode
     * @return a url with session information encoded
     */
    public String encodeURL(String url)
    {
      return super.encodeURL(url);
    }
    
    /**
     * Encodes session information in a URL suitable for
     * <code>sendRedirect()</code> 
     *
     * @param url the url to encode
     * @return a url with session information encoded
     */
    public String encodeRedirectURL(String name)
    {
      return super.encodeRedirectURL(name);
    }
  
  }
}
