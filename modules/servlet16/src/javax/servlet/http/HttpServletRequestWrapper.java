/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.servlet.http;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.io.IOException;

/**
 * Wraps a servlet request in another request.  Filters may
 * use ServletRequestWrapper to modify the headers passed to the servlet.
 *
 * <p/>The default methods just call the wrapped request methods.
 *
 * @since servlet 2.3
 */
public class HttpServletRequestWrapper extends ServletRequestWrapper
  implements HttpServletRequest {
  // the wrapped request
  private HttpServletRequest request;
  /**
   * Creates a new request wrapper
   *
   * @param request the wrapped request
   */
  public HttpServletRequestWrapper(HttpServletRequest request)
  {
    super(request);

    this.request = request;
  }

  /**
   * Sets the request object for the wrapper.
   *
   * @param request the wrapped request
   */
  public void setRequest(ServletRequest request)
  {
    super.setRequest(request);

    this.request = (HttpServletRequest) request;
  }
  /**
   * Returns the HTTP method, e.g. "GET" or "POST"
   *
   * <p/>Equivalent to CGI's <code>REQUEST_METHOD</code>
   */
  public String getMethod()
  {
    return this.request.getMethod();
  }
  /**
   * Returns the entire request URI
   */
  public String getRequestURI()
  {
    return request.getRequestURI();
  }
  /**
   * Reconstructs the URL the client used for the request.
   *
   * @since Servlet 2.3
   */
  public StringBuffer getRequestURL()
  {
    return request.getRequestURL();
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
    return request.getContextPath();
  }
  /**
   * Returns the URI part corresponding to the selected servlet.
   * The URI is relative to the application.
   *
   * <p/>Corresponds to CGI's <code>SCRIPT_NAME</code>
   *
   * <code>getServletPath()</code> is /servlet/Hello for the uri
   * /myapp/servlet/Hello/foo.
   *
   * <code>getServletPath()</code> is /dir/hello.jsp
   * for the uri /myapp/dir/hello.jsp/foo,
   */
  public String getServletPath()
  {
    return request.getServletPath();
  }
  /**
   * Returns the URI part after the selected servlet and null if there
   * is no suffix.
   *
   * <p/>Corresponds to CGI's <code>PATH_INFO</code>
   *
   * <p><code>getPathInfo()</code> is /foo for
   * the uri /myapp/servlet/Hello/foo.
   *
   * <code>getPathInfo()</code> is /hello.jsp for for the uri
   * /myapp/dir/hello.jsp/foo.
   */
  public String getPathInfo()
  {
    return request.getPathInfo();
  }
  /**
   * Returns the physical path name for the path info.
   *
   * <p/>Corresponds to CGI's <code>PATH_TRANSLATED</code>
   *
   * @return null if there is no path info.
   */
  public String getPathTranslated()
  {
    return request.getPathTranslated();
  }
  /**
   * Returns the request's query string.  Form based servlets will use
   * <code>ServletRequest.getParameter()</code> to decode the form values.
   *
   * <p/>Corresponds to CGI's <code>PATH_TRANSLATED</code>
   */
  public String getQueryString()
  {
    return request.getQueryString();
  }
  /**
   * Returns the first value for a request header.
   *
   * <p/>Corresponds to CGI's <code>HTTP_*</code>
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
    return request.getHeader(name);
  }
  /**
   * Returns all the values for a request header.  In some rare cases,
   * like cookies, browsers may return multiple headers.
   *
   * @param name the header name
   * @return an enumeration of the header values.
   */
  public Enumeration<String> getHeaders(String name)
  {
    return request.getHeaders(name);
  }
  /**
   * Returns an enumeration of all headers sent by the client.
   */
  public Enumeration<String> getHeaderNames()
  {
    return request.getHeaderNames();
  }
  /**
   * Converts a header value to an integer.
   *
   * @param name the header name
   * @return the header value converted to an integer
   */
  public int getIntHeader(String name)
  {
    return request.getIntHeader(name);
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
    return request.getDateHeader(name);
  }
  /**
   * Returns an array of all cookies sent by the client.
   */
  public Cookie []getCookies()
  {
    return request.getCookies();
  }
  /**
   * Returns a session.  If no session exists and create is true, then
   * create a new session, otherwise return null.
   *
   * @param create If true, then create a new session if none exists.
   */
  public HttpSession getSession(boolean create)
  {
    return request.getSession(create);
  }
  /**
   * Returns the current session, creating one if necessary.
   * Sessions are a convenience for keeping user state
   * across requests.
   */
  public HttpSession getSession()
  {
    return getSession(true);
  }
  /**
   * Returns the session id.  Sessions are a convenience for keeping
   * user state across requests.
   *
   * <p/>The session id is the value of the JSESSION cookie.
   */
  public String getRequestedSessionId()
  {
    return request.getRequestedSessionId();
  }
  /**
   * Returns true if the session is valid.
   */
  public boolean isRequestedSessionIdValid()
  {
    return request.isRequestedSessionIdValid();
  }
  /**
   * Returns true if the session came from a cookie.
   */
  public boolean isRequestedSessionIdFromCookie()
  {
    return request.isRequestedSessionIdFromCookie();
  }
  /**
   * Returns true if the session came URL-encoding.
   */
  public boolean isRequestedSessionIdFromURL()
  {
    return request.isRequestedSessionIdFromURL();
  }
  /**
   * Returns the auth type, e.g. basic.
   */
  public String getAuthType()
  {
    return request.getAuthType();
  }
  /**
   * Returns the remote user if authenticated.
   */
  public String getRemoteUser()
  {
    return request.getRemoteUser();
  }
  /**
   * Returns true if the user is in the given role.
   */
  public boolean isUserInRole(String role)
  {
    return request.isUserInRole(role);
  }
  /**
   * Returns the equivalent principal object for the authenticated user.
   */
  public Principal getUserPrincipal()
  {
    return request.getUserPrincipal();
  }
  /**
   * @deprecated
   */
  public boolean isRequestedSessionIdFromUrl()
  {
    return request.isRequestedSessionIdFromUrl();
  }

  /**
   * @since Servlet 3.0
   */
  public boolean authenticate(HttpServletResponse response)
    throws IOException, ServletException
  {
    return request.authenticate(response);
  }

  /**
   * @since Servlet 3.0
   */
  public Part getPart(String name)
    throws IOException, ServletException
  {
    return request.getPart(name);
  }

  /**
   * @since Servlet 3.0
   */
  public Collection<Part> getParts()
    throws IOException, ServletException
  {
    return request.getParts();
  }

  /**
   * @since Servlet 3.0
   */
  public void login(String username, String password)
    throws ServletException
  {
    request.login(username, password);
  }

  /**
   * @since Servlet 3.0
   */
  public void logout()
    throws ServletException
  {
    request.logout();
  }
}
