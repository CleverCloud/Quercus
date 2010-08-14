package example.filters;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

import java.util.Enumeration;
import java.security.Principal;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A cut-and-paste template for implementing a Filter that wraps the request
 */

public class ExampleRequestFilter implements Filter {
  private static final Logger log = Logger.getLogger("example.filters.ExampleRequestFilter");

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
    
    // "wrap" the request object.  Any filter or servlet or jsp that
    // follows in the chain will get the values returned from the
    // wrapper instead of from the original Request.
    req = new ExampleRequestWrapper(req);

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
   * This example request wrapper includes all of the methods you
   * could possibly want to implement.  The implementaions here just
   * call the method in the super class, implement the ones you want
   * and remove the ones you don't need to change.
   */
  static class ExampleRequestWrapper extends HttpServletRequestWrapper {
    ExampleRequestWrapper(HttpServletRequest request) 
    {
      super(request);
    }

    /**
     * Returns the HTTP method, e.g. "GET" or "POST"
     */
    public String getMethod()
    {
      return super.getMethod();
    }

    /**
     * Returns the entire request URI
     */
    public String getRequestURI()
    {
      return super.getRequestURI();
    }

    /**
     * Reconstructs the URL the client used for the request.
     */
    public StringBuffer getRequestURL()
    {
      return super.getRequestURL();
    }

    /**
     * Returns the part of the URI corresponding to the application's
     * prefix.  The first part of the URI selects applications
     * (ServletContexts).
     *
     * <p><code>getContextPath()</code> is /myapp for the uri
     * /myapp/servlet/Hello, 
     */
    public String getContextPath()
    {
      return super.getContextPath();
    }

    /**
     * Returns the URI part corresponding to the selected servlet.
     * The URI is relative to the application.
     *
     * <code>getServletPath()</code> is /servlet/Hello for the uri
     * /myapp/servlet/Hello/foo.
     *
     * <code>getServletPath()</code> is /dir/hello.jsp
     * for the uri /myapp/dir/hello.jsp/foo,
     */
    public String getServletPath()
    {
      return super.getServletPath();
    }

    /**
     * Returns the URI part after the selected servlet and null if there
     * is no suffix.
     *
     * <p><code>getPathInfo()</code> is /foo for
     * the uri /myapp/servlet/Hello/foo.
     *
     * <code>getPathInfo()</code> is /hello.jsp for for the uri
     * /myapp/dir/hello.jsp/foo.
     */
    public String getPathInfo()
    {
      return super.getPathInfo();
    }

    /**
     * Returns the physical path name for the path info.
     *
     * @return null if there is no path info.
     */
    public String getPathTranslated()
    {
      return super.getPathTranslated();
    }

    /**
     * Returns the request's query string.  Form based servlets will use
     * <code>ServletRequest.getParameter()</code> to decode the form values.
     *
     */
    public String getQueryString()
    {
      return super.getQueryString();
    }

    /**
     * Returns the first value for a request header.
     *
     * <code><pre>
     * String userAgent = request.getHeader("User-Agent");
     * </pre></code>
     *
     * @param name the header name
     * @return the header value
     */
    public String getHeader(String name)
    {
      return super.getHeader(name);
    }

    /**
     * Returns all the values for a request header.  In some rare cases,
     * like cookies, browsers may return multiple headers.
     *
     * @param name the header name
     * @return an enumeration of the header values.
     */
    public Enumeration getHeaders(String name)
    {
      return super.getHeaders(name);
    }

    /**
     * Returns an enumeration of all headers sent by the client.
     */
    public Enumeration getHeaderNames()
    {
      return super.getHeaderNames();
    }

    /**
     * Converts a header value to an integer.
     *
     * @param name the header name
     * @return the header value converted to an integer
     */
    public int getIntHeader(String name)
    {
      return super.getIntHeader(name);
    }

    /**
     * Converts a date header to milliseconds since the epoch.
     *
     * <pre><code>
     * long mod = request.getDateHeader("If-Modified-Since");
     * </code></pre>
     *
     * @param name the header name
     * @return the header value converted to an date
     */
    public long getDateHeader(String name)
    {
      return super.getDateHeader(name);
    }

    /**
     * Returns an array of all cookies sent by the client.
     */
    public Cookie []getCookies()
    {
      return super.getCookies();
    }

    /**
     * Returns a session.  If no session exists and create is true, then
     * create a new session, otherwise return null.
     *
     * @param create If true, then create a new session if none exists.
     */
    public HttpSession getSession(boolean create)
    {
      return super.getSession(create);
    }

    /**
     * Returns the session id.  Sessions are a convenience for keeping
     * user state across requests.
     *
     * <p/>The session id is the value of the JSESSION cookie.
     */
    public String getRequestedSessionId()
    {
      return super.getRequestedSessionId();
    }
    
    /**
     * Returns true if the session is valid.
     */
    public boolean isRequestedSessionIdValid()
    {
      return super.isRequestedSessionIdValid();
    }
    
    /**
     * Returns true if the session came from a cookie.
     */
    public boolean isRequestedSessionIdFromCookie()
    {
      return super.isRequestedSessionIdFromCookie();
    }
    
    /**
     * Returns true if the session came URL-encoding.
     */
    public boolean isRequestedSessionIdFromURL()
    {
      return super.isRequestedSessionIdFromURL();
    }
    
    /**
     * Returns the auth type, e.g. basic.
     */
    public String getAuthType()
    {
      return super.getAuthType();
    }
    
    /**
     * Returns the remote user if authenticated.
     */
    public String getRemoteUser()
    {
      return super.getRemoteUser();
    }
    
    /**
     * Returns true if the user is in the given role.
     */
    public boolean isUserInRole(String role)
    {
      return super.isUserInRole(role);
    }
    
    /**
     * Returns the equivalent principal object for the authenticated user.
     */
    public Principal getUserPrincipal()
    {
      return super.getUserPrincipal();
    }
  }
}

