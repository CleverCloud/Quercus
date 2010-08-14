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

package com.caucho.server.http;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * Wraps a servlet request in another request.  Filters may
 * use ServletRequestWrapper to modify the headers passed to the servlet.
 *
 * <p/>The default methods just call the wrapped request methods.
 *
 * @since servlet 2.3
 */
public class RequestWrapper implements ServletRequest {
  // the wrapped request
  protected HttpServletRequest _request;
  
  /**
   * Create a new ServletRequestWrapper wrapping the enclosed request.
   */
  public RequestWrapper()
  {
  }
  
  /**
   * Create a new ServletRequestWrapper wrapping the enclosed request.
   */
  public RequestWrapper(HttpServletRequest request)
  {
    setRequest(request);
  }
  
  /**
   * Sets the request object being wrapped.
   *
   * @exception IllegalArgumentException if the request is null
   */
  public void setRequest(HttpServletRequest request)
  {
    _request = request;
  }
  
  /**
   * Gets the request object being wrapped.
   *
   * @return the wrapped response
   */
  public HttpServletRequest getRequest()
  {
    return _request;
  }
  /**
   * Returns the prococol, e.g. "HTTP/1.1"
   */
  public String getProtocol()
  {
    return getRequest().getProtocol();
  }
  /**
   * Returns the request scheme, e.g. "http"
   */
  public String getScheme()
  {
    return getRequest().getScheme();
  }
  /**
   * Returns the server name handling the request.  When using virtual hosts,
   * this returns the virtual host name, e.g. "vhost1.caucho.com".
   */
  public String getServerName()
  {
    return getRequest().getServerName();
  }
  /**
   * Returns the server port handling the request, e.g. 80.
   */
  public int getServerPort()
  {
    return getRequest().getServerPort();
  }
  /**
   * Returns the IP address of the remote host, i.e. the client browser.
   */
  public String getRemoteAddr()
  {
    return getRequest().getRemoteAddr();
  }
  
  /**
   * Returns the DNS hostname of the remote host, i.e. the client browser.
   */
  public String getRemoteHost()
  {
    return getRequest().getRemoteHost();
  }
  
  /**
   * Returns the remote port
   *
   * @since 2.4
   */
  public int getRemotePort()
  {
    return getRequest().getRemotePort();
  }
  
  /**
   * Returns the IP address of the local host, i.e. the server.
   */
  public String getLocalAddr()
  {
    return getRequest().getLocalAddr();
  }
  
  /**
   * Returns the local host name.
   */
  public String getLocalName()
  {
    return getRequest().getLocalName();
  }
  
  /**
   * Returns the local port
   */
  public int getLocalPort()
  {
    return getRequest().getLocalPort();
  }
  
  /**
   * Returns a form parameter.  When the form contains several parameters
   * of the same name, <code>getParameter</code> returns the first.
   *
   * <p>For example, calling <code>getParameter("a")</code> with the
   * the query string <code>a=1&a=2</code> will return "1".
   *
   * @param name the form parameter to return
   * @return the form value or null if none matches.
   */
  public String getParameter(String name)
  {
    return getRequest().getParameter(name);
  }
  /**
   * Returns the parameter map request parameters.  By default, returns
   * the underlying request's map.
   */
  public Map getParameterMap()
  {
    return getRequest().getParameterMap();
  }
  /**
   * Returns all values of a form parameter.
   *
   * <p>For example, calling <code>getParameterValues("a")</code>
   * with the the query string <code>a=1&a=2</code> will
   * return ["1", "2"].
   *
   * @param name the form parameter to return
   * @return an array of matching form values or null if none matches.
   */
  public String []getParameterValues(String name)
  {
    return getRequest().getParameterValues(name);
  }
  /**
   * Returns an enumeration of all form parameter names.
   *
   * <code><pre>
   * Enumeration e = getRequest().getParameterNames();
   * while (e.hasMoreElements()) {
   *   String name = (String) e.nextElement();
   *   out.println(name + ": " + request.getParameter(name));
   * }
   * </pre></code>
   */
  public Enumeration getParameterNames()
  {
    return getRequest().getParameterNames();
  }
  /**
   * Returns an InputStream to retrieve POST data from the request.
   * The stream will automatically end when the end of the POST data
   * is complete.
   */
  public ServletInputStream getInputStream()
    throws IOException
  {
    return getRequest().getInputStream();
  }
  /**
   * Returns a reader to read POSTed data.  Character encoding is
   * based on the request data and is the same as
   * <code>getCharacterEncoding()</code>
   */
  public BufferedReader getReader()
    throws IOException, IllegalStateException
  {
    return getRequest().getReader();
  }
  /**
   * Returns the character encoding of the POSTed data.
   */
  public String getCharacterEncoding()
  {
    return getRequest().getCharacterEncoding();
  }
  /**
   * Sets the character encoding to be used for forms and getReader.
   */
  public void setCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    getRequest().setCharacterEncoding(encoding);
  }
  /**
   * Returns the content length of the data.  This value may differ from
   * the actual length of the data.  For newer browsers, i.e.
   * those supporting HTTP/1.1, can support "chunked" encoding which does
   * not make the content length available.
   *
   * <p>The upshot is, rely on the input stream to end when the data
   * completes.
   */
  public int getContentLength()
  {
    return getRequest().getContentLength();
  }
  /**
   * Returns the request's mime-type.
   */
  public String getContentType()
  {
    return getRequest().getContentType();
  }
  /**
   * Returns the request's preferred locale.
   */
  public Locale getLocale()
  {
    return getRequest().getLocale();
  }
  /**
   * Returns an enumeration of all locales acceptable by the client.
   */
  public Enumeration getLocales()
  {
    return getRequest().getLocales();
  }
  /**
   * Returns true if the connection is secure, e.g. it uses SSL.
   */
  public boolean isSecure()
  {
    return getRequest().isSecure();
  }
  /**
   * Returns an attribute value.
   *
   * @param name the attribute name
   * @return the attribute value
   */
  public Object getAttribute(String name)
  {
    return getRequest().getAttribute(name);
  }
  /**
   * Sets an attribute value.
   *
   * @param name the attribute name
   * @param o the attribute value
   */
  public void setAttribute(String name, Object o)
  {
    getRequest().setAttribute(name, o);
  }
  
  /**
   * Enumerates all attribute names in the request.
   */
  public Enumeration getAttributeNames()
  {
    return getRequest().getAttributeNames();
  }
  /**
   * Removes the given attribute.
   *
   * @param name the attribute name
   */
  public void removeAttribute(String name)
  {
    getRequest().removeAttribute(name);
  }
  /**
   * Returns a request dispatcher for later inclusion or forwarding.  This
   * is the servlet API equivalent to SSI includes.  <code>uri</code>
   * is relative to the request URI.  Absolute URIs are relative to
   * the application prefix (<code>getContextPath()</code>).
   *
   * <p>If <code>getRequestURI()</code> is /myapp/dir/test.jsp and the 
   * <code>uri</code> is "inc.jsp", the resulting page is
   * /myapp/dir/inc.jsp.

   * <code><pre>
   *   RequestDispatcher disp;
   *   disp = getRequestDispatcher("inc.jsp?a=b");
   *   disp.include(request, response);
   * </pre></code>
   *
   * @param uri path relative to <code>getRequestURI()</code>
   * (including query string) for the included file.
   * @return RequestDispatcher for later inclusion or forwarding.
   */
  public RequestDispatcher getRequestDispatcher(String uri)
  {
    return getRequest().getRequestDispatcher(uri);
  }
  
  /**
   * Returns the real path.
   */
  public String getRealPath(String uri)
  {
    return getRequest().getRealPath(uri);
  }
  
  /**
   * Returns the HTTP method, e.g. "GET" or "POST"
   *
   * <p/>Equivalent to CGI's <code>REQUEST_METHOD</code>
   */
  public String getMethod()
  {
    return getRequest().getMethod();
  }
  /**
   * Returns the entire request URI
   */
  public String getRequestURI()
  {
    return getRequest().getRequestURI();
  }
  /**
   * Reconstructs the URL the client used for the request.
   *
   * @since Servlet 2.3
   */
  public StringBuffer getRequestURL()
  {
    return getRequest().getRequestURL();
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
    return getRequest().getContextPath();
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
    return getRequest().getServletPath();
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
    return getRequest().getPathInfo();
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
    return getRequest().getPathTranslated();
  }
  /**
   * Returns the request's query string.  Form based servlets will use
   * <code>ServletRequest.getParameter()</code> to decode the form values.
   *
   * <p/>Corresponds to CGI's <code>PATH_TRANSLATED</code>
   */
  public String getQueryString()
  {
    return getRequest().getQueryString();
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
    return getRequest().getHeader(name);
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
    return getRequest().getHeaders(name);
  }
  /**
   * Returns an enumeration of all headers sent by the client.
   */
  public Enumeration getHeaderNames()
  {
    return getRequest().getHeaderNames();
  }
  /**
   * Converts a header value to an integer.
   *
   * @param name the header name
   * @return the header value converted to an integer
   */
  public int getIntHeader(String name)
  {
    return getRequest().getIntHeader(name);
  }
  /**
   * Converts a date header to milliseconds since the epoch.
   *
   * <pre><code>
   * long mod = getRequest().getDateHeader("If-Modified-Since");
   * </code></pre>
   *
   * @param name the header name
   * @return the header value converted to an date
   */
  public long getDateHeader(String name)
  {
    return getRequest().getDateHeader(name);
  }
  /**
   * Returns an array of all cookies sent by the client.
   */
  public Cookie []getCookies()
  {
    return getRequest().getCookies();
  }
  /**
   * Returns a session.  If no session exists and create is true, then
   * create a new session, otherwise return null.
   *
   * @param create If true, then create a new session if none exists.
   */
  public HttpSession getSession(boolean create)
  {
    return getRequest().getSession(create);
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
    return getRequest().getRequestedSessionId();
  }
  /**
   * Returns true if the session is valid.
   */
  public boolean isRequestedSessionIdValid()
  {
    return getRequest().isRequestedSessionIdValid();
  }
  /**
   * Returns true if the session came from a cookie.
   */
  public boolean isRequestedSessionIdFromCookie()
  {
    return getRequest().isRequestedSessionIdFromCookie();
  }
  /**
   * Returns true if the session came URL-encoding.
   */
  public boolean isRequestedSessionIdFromURL()
  {
    return getRequest().isRequestedSessionIdFromURL();
  }
  /**
   * Returns the auth type, e.g. basic.
   */
  public String getAuthType()
  {
    return getRequest().getAuthType();
  }
  /**
   * Returns the remote user if authenticated.
   */
  public String getRemoteUser()
  {
    return getRequest().getRemoteUser();
  }
  /**
   * Returns true if the user is in the given role.
   */
  public boolean isUserInRole(String role)
  {
    return getRequest().isUserInRole(role);
  }
  
  /**
   * Returns the equivalent principal object for the authenticated user.
   */
  public Principal getUserPrincipal()
  {
    return getRequest().getUserPrincipal();
  }
  
  /**
   * @deprecated
   */
  public boolean isRequestedSessionIdFromUrl()
  {
    return getRequest().isRequestedSessionIdFromUrl();
  }

  /**
   * Returns the servlet context for the request
   *
   * @since Servlet 3.0
   */
  public ServletContext getServletContext()
  {
    return null;
  }

  /**
   * Returns the servlet response for the request
   *
   * @since Servlet 3.0
   */
  public ServletResponse getServletResponse()
  {
    return null;
  }


  //
  // servlet 3.0
  //

  /**
   * Adds an async listener for this request
   *
   * @since Servlet 3.0
   */
  public void addAsyncListener(AsyncListener listener)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Adds an async listener for this request
   *
   * @since Servlet 3.0
   */
  public void addAsyncListener(AsyncListener listener,
                               ServletRequest request,
                               ServletResponse response)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the async context for the request
   *
   * @since Servlet 3.0
   */
  public AsyncContext getAsyncContext()
  {
    return getRequest().getAsyncContext();
  }

  /**
   * Returns true if the request is in async.
   *
   * @since Servlet 3.0
   */
  public boolean isAsyncStarted()
  {
    return getRequest().isAsyncStarted();
  }

  /**
   * Returns true if the request supports async
   *
   * @since Servlet 3.0
   */
  public boolean isAsyncSupported()
  {
    return getRequest().isAsyncSupported();
  }

  /**
   * Starts an async mode
   *
   * @since Servlet 3.0
   */
  public AsyncContext startAsync()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Starts an async mode
   *
   * @since Servlet 3.0
   */
  public AsyncContext startAsync(ServletRequest servletRequest,
                                 ServletResponse servletResponse)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Suspend the request
   *
   * @since Servlet 3.0
   */
  public void suspend(long timeout)
  {
  }

  /**
   * Suspend the request
   *
   * @since Servlet 3.0
   */
  public void suspend()
  {
  }

  /**
   * Resume the request
   *
   * @since Servlet 3.0
   */
  public void resume()
  {
  }

  /**
   * Complete the request
   *
   * @since Servlet 3.0
   */
  public void complete()
  {
  }

  /**
   * Returns true if the servlet is suspended
   *
   * @since Servlet 3.0
   */
  public boolean isSuspended()
  {
    return false;
  }

  /**
   * Returns true if the servlet is resumed
   *
   * @since Servlet 3.0
   */
  public boolean isResumed()
  {
    return false;
  }

  /**
   * Returns true if the servlet timed out
   *
   * @since Servlet 3.0
   */
  public boolean isTimeout()
  {
    return false;
  }

  /**
   * Returns true for the initial dispatch
   *
   * @since Servlet 3.0
   */
  public boolean isInitial()
  {
    return true;
  }

  public DispatcherType getDispatcherType()
  {
    return getRequest().getDispatcherType();
  }

  /**
   * Clears the wrapper.
   */
  protected void free()
  {
    _request = null;
  }

}
