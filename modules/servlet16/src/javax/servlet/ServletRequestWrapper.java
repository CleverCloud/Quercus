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

package javax.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
public class ServletRequestWrapper implements ServletRequest {
  // the wrapped request
  private ServletRequest _request;
  /**
   * Create a new ServletRequestWrapper wrapping the enclosed request.
   */
  public ServletRequestWrapper(ServletRequest request)
  {
    if (request == null)
      throw new IllegalArgumentException();
    
    _request = request;
  }
  
  /**
   * Sets the request object being wrapped.
   *
   * @exception IllegalArgumentException if the request is null
   */
  public void setRequest(ServletRequest request)
  {
    if (request == null)
      throw new IllegalArgumentException();
    
    _request = request;
  }
  
  /**
   * Gets the request object being wrapped.
   *
   * @return the wrapped response
   */
  public ServletRequest getRequest()
  {
    return _request;
  }
  /**
   * Returns the prococol, e.g. "HTTP/1.1"
   */
  public String getProtocol()
  {
    return _request.getProtocol();
  }
  /**
   * Returns the request scheme, e.g. "http"
   */
  public String getScheme()
  {
    return _request.getScheme();
  }
  /**
   * Returns the server name handling the request.  When using virtual hosts,
   * this returns the virtual host name, e.g. "vhost1.caucho.com".
   */
  public String getServerName()
  {
    return _request.getServerName();
  }
  /**
   * Returns the server port handling the request, e.g. 80.
   */
  public int getServerPort()
  {
    return _request.getServerPort();
  }
  /**
   * Returns the IP address of the remote host, i.e. the client browser.
   */
  public String getRemoteAddr()
  {
    return _request.getRemoteAddr();
  }
  
  /**
   * Returns the DNS hostname of the remote host, i.e. the client browser.
   */
  public String getRemoteHost()
  {
    return _request.getRemoteHost();
  }
  
  /**
   * Returns the remote port
   *
   * @since 2.4
   */
  public int getRemotePort()
  {
    return _request.getRemotePort();
  }
  
  /**
   * Returns the IP address of the local host, i.e. the server.
   */
  public String getLocalAddr()
  {
    return _request.getLocalAddr();
  }
  
  /**
   * Returns the local host name.
   */
  public String getLocalName()
  {
    return _request.getLocalName();
  }
  
  /**
   * Returns the local port
   */
  public int getLocalPort()
  {
    return _request.getLocalPort();
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
    return _request.getParameter(name);
  }
  /**
   * Returns the parameter map request parameters.  By default, returns
   * the underlying request's map.
   */
  public Map<String,String[]> getParameterMap()
  {
    return _request.getParameterMap();
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
    return _request.getParameterValues(name);
  }
  /**
   * Returns an enumeration of all form parameter names.
   *
   * <code><pre>
   * Enumeration e = _request.getParameterNames();
   * while (e.hasMoreElements()) {
   *   String name = (String) e.nextElement();
   *   out.println(name + ": " + request.getParameter(name));
   * }
   * </pre></code>
   */
  public Enumeration<String> getParameterNames()
  {
    return _request.getParameterNames();
  }
  /**
   * Returns an InputStream to retrieve POST data from the request.
   * The stream will automatically end when the end of the POST data
   * is complete.
   */
  public ServletInputStream getInputStream()
    throws IOException
  {
    return _request.getInputStream();
  }
  /**
   * Returns a reader to read POSTed data.  Character encoding is
   * based on the request data and is the same as
   * <code>getCharacterEncoding()</code>
   */
  public BufferedReader getReader()
    throws IOException, IllegalStateException
  {
    return _request.getReader();
  }
  /**
   * Returns the character encoding of the POSTed data.
   */
  public String getCharacterEncoding()
  {
    return _request.getCharacterEncoding();
  }
  /**
   * Sets the character encoding to be used for forms and getReader.
   */
  public void setCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    _request.setCharacterEncoding(encoding);
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
    return _request.getContentLength();
  }
  /**
   * Returns the request's mime-type.
   */
  public String getContentType()
  {
    return _request.getContentType();
  }
  /**
   * Returns the request's preferred locale.
   */
  public Locale getLocale()
  {
    return _request.getLocale();
  }
  /**
   * Returns an enumeration of all locales acceptable by the client.
   */
  public Enumeration<Locale> getLocales()
  {
    return _request.getLocales();
  }
  /**
   * Returns true if the connection is secure, e.g. it uses SSL.
   */
  public boolean isSecure()
  {
    return _request.isSecure();
  }
  /**
   * Returns an attribute value.
   *
   * @param name the attribute name
   * @return the attribute value
   */
  public Object getAttribute(String name)
  {
    return _request.getAttribute(name);
  }
  /**
   * Sets an attribute value.
   *
   * @param name the attribute name
   * @param o the attribute value
   */
  public void setAttribute(String name, Object o)
  {
    _request.setAttribute(name, o);
  }
  /**
   * Enumerates all attribute names in the request.
   */
  public Enumeration<String> getAttributeNames()
  {
    return _request.getAttributeNames();
  }
  /**
   * Removes the given attribute.
   *
   * @param name the attribute name
   */
  public void removeAttribute(String name)
  {
    _request.removeAttribute(name);
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
    return _request.getRequestDispatcher(uri);
  }
  
  /**
   * Returns the real path.
   */
  @SuppressWarnings("deprecation")
  public String getRealPath(String uri)
  {
    return _request.getRealPath(uri);
  }

  //
  // Servlet 3.0
  //

  /**
   * Returns the servlet context for the request
   *
   * @since Servlet 3.0
   */
  public ServletContext getServletContext()
  {
    return _request.getServletContext();
  }

  /**
   * Returns the async context for the request
   *
   * @since Servlet 3.0
   */
  @Override
  public AsyncContext getAsyncContext()
  {
    return _request.getAsyncContext();
  }

  /**
   * Returns true if the request is in async.
   *
   * @since Servlet 3.0
   */
  @Override
  public boolean isAsyncStarted()
  {
    return _request.isAsyncStarted();
  }

  /**
   * Returns true if the request supports async
   *
   * @since Servlet 3.0
   */
  @Override
  public boolean isAsyncSupported()
  {
    return _request.isAsyncSupported();
  }

  /**
   * Starts an async mode
   *
   * @since Servlet 3.0
   */
  @Override
  public AsyncContext startAsync()
    throws IllegalStateException
  {
    return _request.startAsync();
  }

  /**
   * Starts an async mode
   *
   * @since Servlet 3.0
   */
  @Override
  public AsyncContext startAsync(ServletRequest servletRequest,
                                 ServletResponse servletResponse)
    throws IllegalStateException
  {
    return _request.startAsync(servletRequest, servletResponse);
  }

  /**
   * Returns the dispatcherType (request, include, etc) for the current
   * request.
   */
  @Override
  public DispatcherType getDispatcherType() 
  {
    return _request.getDispatcherType();
  }

  /**
   * @since Servlet 3.0
   */
  public boolean isWrapperFor(ServletRequest wrapped)
  {
    if (_request == wrapped)
      return true;
    else if (_request instanceof ServletRequestWrapper)
      return ((ServletRequestWrapper) _request).isWrapperFor(wrapped);
    else
      return false;
  }

  /**
   * @since Servlet 3.0
   */
  public boolean isWrapperFor(Class wrappedType)
  {
    if (!ServletRequest.class.isAssignableFrom(wrappedType))
      throw new IllegalArgumentException(
        "expected instance of javax.servlet.ServletRequest at `"
          + wrappedType
          + "'");
    else if (wrappedType.isAssignableFrom(_request.getClass()))
      return true;
    else if (_request instanceof ServletRequestWrapper)
      return ((ServletRequestWrapper) _request).isWrapperFor(wrappedType);
    else
      return false;
  }
}
