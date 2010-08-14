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
import javax.servlet.ServletException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.io.IOException;

/**
 * HttpServletRequest encapsulates client request data.
 *
 * <h4>URI</h4>
 *
 * The servlet engine splits the URI into three useful
 * sections: the context path (application prefix), the servlet path,
 * and the path info.
 *
 * <p>For example, given an application prefix of '/myapp', the api
 * <code>/myapp/dir/test.jsp/data</code> will be split as follows:
 *
 * <table>
 * <tr><td>/myapp/dir/test.jsp/data<td>getRequestURI()
 * <tr><td>/myapp<td>getContextPath()
 * <tr><td>/dir/test.jsp<td>getServletPath()
 * <tr><td>/data<td>getPathInfo()
 * </table>
 *
 * <h4>Useful headers</h4>
 *
 * <table>
 * <tr><td>User-Agent<td>String describing the browser's version
 * </table>
 *
 * <h4>CGI equivalents</h4>
 *
 * <table>
 * <tr><td>AUTH_TYPE<td>getAuthType()
 * <tr><td>CONTENT_TYPE<td>getContentType()
 * <tr><td>CONTENT_LENGTH<td>getContentLength()
 * <tr><td>PATH_INFO<td>getPathInfo()
 * <tr><td>PATH_TRANSLATED<td>getPathTranslated()
 * <tr><td>QUERY_STRING<td>getQueryString()
 * <tr><td>REMOTE_ADDR<td>getRemoteAddr()
 * <tr><td>REMOTE_HOST<td>getRemoteHost()
 * <tr><td>REMOTE_USER<td>getRemoteUser()
 * <tr><td>REQUEST_METHOD<td>getMethod()
 * <tr><td>SCRIPT_NAME<td>getServletPath()
 * <tr><td>SERVER_NAME<td>getServerName()
 * <tr><td>SERVER_PROTOCOL<td>getProtocol()
 * <tr><td>SERVER_PORT<td>getServerPort()
 * </table>
 *
 * <h4>Form data</h4>
 *
 * For form data, see <code>ServletRequest.getParameter()</code>
 */

public interface HttpServletRequest extends ServletRequest {
  /**
   * String identifier for basic authentication. Value "BASIC".
   */
  public final static String BASIC_AUTH = "BASIC";
  /**
   * String identifier for client cert authentication. Value "CLIENT_CERT".
   */
  public final static String CLIENT_CERT_AUTH = "CLIENT_CERT";
  /**
   * String identifier for digest authenciation. Value "DIGEST".
   */
  public final static String DIGEST_AUTH = "DIGEST";
  /**
   * String identifier for form authenciation. Value "FORM".
   */
  public final static String FORM_AUTH = "FORM";
  /**
   * Returns the HTTP method, e.g. "GET" or "POST"
   *
   * <p/>Equivalent to CGI's <code>REQUEST_METHOD</code>
   */
  public String getMethod();
  /**
   * Returns the entire request URI
   */
  public String getRequestURI();
  /**
   * Reconstructs the URL the client used for the request.
   *
   * @since Servlet 2.3
   */
  public StringBuffer getRequestURL();
  /**
   * Returns the part of the URI corresponding to the application's
   * prefix.  The first part of the URI selects applications
   * (ServletContexts).
   *
   * <p><code>getContextPath()</code> is /myapp for the uri
   * /myapp/servlet/Hello,
   */
  public String getContextPath();
  /**
   * Returns the URI part corresponding to the selected servlet.
   * The URI is relative to the application.
   *
   * Returns an emtpy string for a servlet matched on /*
   *
   * <p/>Corresponds to CGI's <code>SCRIPT_NAME</code>
   *
   * <code>getServletPath()</code> is /servlet/Hello for the uri
   * /myapp/servlet/Hello/foo.
   *
   * <code>getServletPath()</code> is /dir/hello.jsp
   * for the uri /myapp/dir/hello.jsp/foo,
   */
  public String getServletPath();
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
  public String getPathInfo();
  /**
   * Returns the physical path name for the path info.
   *
   * <p/>Corresponds to CGI's <code>PATH_TRANSLATED</code>
   *
   * @return null if there is no path info.
   */
  public String getPathTranslated();
  /**
   * Returns the request's query string.  Form based servlets will use
   * <code>ServletRequest.getParameter()</code> to decode the form values.
   *
   * <p/>Corresponds to CGI's <code>PATH_TRANSLATED</code>
   */
  public String getQueryString();
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
  public String getHeader(String name);
  /**
   * Returns all the values for a request header.  In some rare cases,
   * like cookies, browsers may return multiple headers.
   *
   * @param name the header name
   * @return an enumeration of the header values.
   */
  public Enumeration<String> getHeaders(String name);
  /**
   * Returns an enumeration of all headers sent by the client.
   */
  public Enumeration<String> getHeaderNames();
  /**
   * Converts a header value to an integer.
   *
   * @param name the header name
   * @return the header value converted to an integer
   */
  public int getIntHeader(String name);
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
  public long getDateHeader(String name);
  /**
   * Returns an array of all cookies sent by the client.
   */
  public Cookie []getCookies();
  /**
   * Returns a session.  If no session exists and create is true, then
   * create a new session, otherwise return null.
   *
   * @param create If true, then create a new session if none exists.
   */
  public HttpSession getSession(boolean create);
  /**
   * Returns the current session, creating one if necessary.
   * Sessions are a convenience for keeping user state
   * across requests.
   */
  public HttpSession getSession();
  /**
   * Returns the session id.  Sessions are a convenience for keeping
   * user state across requests.
   *
   * <p/>The session id is the value of the JSESSION cookie.
   */
  public String getRequestedSessionId();
  /**
   * Returns true if the session is valid.
   */
  public boolean isRequestedSessionIdValid();
  /**
   * Returns true if the session came from a cookie.
   */
  public boolean isRequestedSessionIdFromCookie();
  /**
   * Returns true if the session came URL-encoding.
   */
  public boolean isRequestedSessionIdFromURL();
  /**
   * Returns the auth type, i.e. BASIC, CLIENT-CERT, DIGEST, or FORM.
   */
  public String getAuthType();
  /**
   * Returns the remote user if authenticated.
   */
  public String getRemoteUser();
  /**
   * Returns true if the user is in the given role.
   */
  public boolean isUserInRole(String role);
  /**
   * Returns the equivalent principal object for the authenticated user.
   */
  public Principal getUserPrincipal();
  /**
   * @deprecated
   */
  public boolean isRequestedSessionIdFromUrl();

  /**
   * @since Servlet 3.0
   */
  public boolean authenticate(HttpServletResponse response)
      throws IOException,ServletException;

  /**
   * @since Servlet 3.0
   */
  public void login(String username, String password)
    throws ServletException;

  /**
   * @since Servlet 3.0
   */
  public void logout()
    throws ServletException;


  /**
   * @since Servlet 3.0
   */
  public Collection<Part> getParts()
    throws IOException, ServletException;


  /**
   * @since Servlet 3.0
   */
  public Part getPart(String name)
    throws IOException, ServletException;
}
