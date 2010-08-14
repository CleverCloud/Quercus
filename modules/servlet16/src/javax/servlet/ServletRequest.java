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
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * ServletRequest encapsulates client request data.  See HttpServletRequest
 * for HTTP-specific information.
 *
 * <p>Requests have user-defined attributes.  Servlets can communicate
 * to included or forwarded pages using the request attributes.  For example,
 * a servlet may calculate results and place them in the request attributes.
 * It may then forward the call to a JSP template to format the result.
 *
 * <p>Form parameters are retrieved using <code>getParameter</code>
 */
public interface ServletRequest {
  /**
   * Returns the prococol, e.g. "HTTP/1.1"
   */
  public String getProtocol();

  /**
   * Returns the request scheme, e.g. "http"
   */
  public String getScheme();

  /**
   * Returns the server name handling the request.  When using virtual hosts,
   * this returns the virtual host name, e.g. "vhost1.caucho.com".
   *
   * This call returns the host name as the client sees it, which means that
   * if ipchains, load balancing, or proxying is involved this call returns the
   * correct call for forming urls, but may not contain the host that Resin is
   * actually listening on.
   */
  public String getServerName();

  /**
   * Returns the server port used by the client, e.g. 80.
   *
   * This call returns the port number as the client sees it, which means that
   * if ipchains, load balancing, or proxying is involved this call returns the
   * correct call for forming urls, but may not return the actual port that
   * Resin is listening on.
   *
   * This call should not be used to test for an ssl connection
   * (getServerPort() == 443), {@link #isSecure()} is provided for that purpose.
   */
  public int getServerPort();

  /**
   * Returns the IP address of the remote host, i.e. the client browser.
   */
  public String getRemoteAddr();

  /**
   * Returns the DNS hostname of the remote host, i.e. the client browser.
   */
  public String getRemoteHost();

  /**
   * Returns the port of the remote host, i.e. the client browser.
   *
   * @since 2.4
   */
  public int getRemotePort();

  /**
   * This call returns the ip of the host actually used to connect to the Resin
   * server,  which means that if ipchains, load balancing, or proxying is
   * involved this call <i>does not</i> return the correct host for
   * forming urls.
   *
   * @since 2.4
   */
  public String getLocalAddr();

  /**
   * Returns the IP address of the local host, i.e. the server.
   *
   * This call returns the name of the host actaully used to connect to the
   * Resin server,  which means that if ipchains, load balancing, or proxying
   * is involved this call <i>does not</i> return the correct host for
   * forming urls.
   *
   * @since 2.4
   */
  public String getLocalName();

  /**
   * Returns the port of the local host.
   *
   * This call returns the port number actually used to connect to the Resin
   * server,  which means that if ipchains, load balancing, or proxying is
   * involved this call <i>does not</i> return the correct port for
   * forming urls.
   *
   * This call should not be used to test for an ssl connection
   * (getServerPort() == 443), {@link #isSecure()} is provided for that purpose.
   *
   * @since 2.4
   */
  public int getLocalPort();

  /**
   * Overrides the character encoding specified in the request.
   * <code>setCharacterEncoding</code> must be called before calling
   * <code>getReader</code> or reading any parameters.
   */
  public void setCharacterEncoding(String encoding)
    throws java.io.UnsupportedEncodingException;

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
  public String getParameter(String name);

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
  public String []getParameterValues(String name);

  /**
   * Returns an enumeration of all form parameter names.
   *
   * <code><pre>
   * Enumeration e = request.getParameterNames();
   * while (e.hasMoreElements()) {
   *   String name = (String) e.nextElement();
   *   out.println(name + ": " + request.getParameter(name));
   * }
   * </pre></code>
   */
  public Enumeration<String> getParameterNames();

  /**
   * Returns a Map of the form parameters.  The map is immutable.
   * The keys are the form keys as returned by <code>getParameterNames</code>
   * and the values are String arrays as returned by
   * <code>getParameterValues</code>.
   */
  public Map<String,String[]> getParameterMap();

  /**
   * Returns an InputStream to retrieve POST data from the request.
   * The stream will automatically end when the end of the POST data
   * is complete.
   */
  public ServletInputStream getInputStream()
    throws IOException;

  /**
   * Returns a reader to read POSTed data.  Character encoding is
   * based on the request data and is the same as
   * <code>getCharacterEncoding()</code>
   */
  public BufferedReader getReader()
    throws IOException, IllegalStateException;

  /**
   * Returns the character encoding of the POSTed data.
   */
  public String getCharacterEncoding();

  /**
   * Returns the content length of the data.  This value may differ from
   * the actual length of the data.  Newer browsers
   * supporting HTTP/1.1 may use "chunked" encoding which does
   * not make the content length available.
   *
   * <p>The upshot is, rely on the input stream to end when the data
   * completes.
   */
  public int getContentLength();

  /**
   * Returns the request's mime-type.
   */
  public String getContentType();

  /**
   * Returns the request's preferred locale, based on the Accept-Language
   * header.  If unspecified, returns the server's default locale.
   */
  public Locale getLocale();

  /**
   * Returns an enumeration of all locales acceptable by the client.
   */
  public Enumeration<Locale> getLocales();

  /**
   * Returns true if the connection is secure, e.g. it uses SSL.
   */
  public boolean isSecure();

  /**
   * Returns an attribute value.
   *
   * @param name the attribute name
   * @return the attribute value
   */
  public Object getAttribute(String name);

  /**
   * Sets an attribute value.
   *
   * @param name the attribute name
   * @param o the attribute value
   */
  public void setAttribute(String name, Object o);

  /**
   * Enumerates all attribute names in the request.
   */
  public Enumeration<String> getAttributeNames();

  /**
   * Removes the given attribute.
   *
   * @param name the attribute name
   */
  public void removeAttribute(String name);

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
  public RequestDispatcher getRequestDispatcher(String uri);

  /**
   * Returns the path of the URI.
   * 
   * @deprecated
   */
  public String getRealPath(String uri);

  //
  // Servlet 3.0
  //

  /**
   * Returns DispatcherType
   * @return
   */
  public DispatcherType getDispatcherType();

  /**
   * Returns the servlet context for the request
   *
   * @since Servlet 3.0
   */
  public ServletContext getServletContext();
  
  //
  // Servlet 3.0 async
  //

  /**
   * Starts an async/comet mode
   *
   * @since Servlet 3.0
   */
  public AsyncContext startAsync()
    throws IllegalStateException;

  /**
   * Starts an async mode
   *
   * @since Servlet 3.0
   */
  public AsyncContext startAsync(ServletRequest servletRequest,
                                 ServletResponse servletResponse)
    throws IllegalStateException;

  /**
   * Returns the async context for the request
   *
   * @since Servlet 3.0
   */
  public AsyncContext getAsyncContext();

  /**
   * Returns true if the request is in async.
   *
   * @since Servlet 3.0
   */
  public boolean isAsyncStarted();

  /**
   * Returns true if the request supports async
   *
   * @since Servlet 3.0
   */
  public boolean isAsyncSupported();
}
