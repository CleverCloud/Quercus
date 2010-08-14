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

import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.InputStream;
import java.util.*;

/**
 * ServletContexts encapsulate applications.  Applications are generalized
 * virtual hosts; a URL prefix defines a distinct application.
 * So /myapp and /yourapp could define different applications. As a
 * degenerate case, each virtual host has its own ServletContext.
 *
 * <p>Each application is entirely distinct.  Each has its own:
 * <ul>
 * <li>Class loader -- each application gets its own beans and servlets.
 * <li>ServletContext attributes
 * <li>Servlets and servlet mappings (e.g. *.jsp could map to different
 * servlets in different applications.)
 * <li>File root
 * <li>Mime mapping
 * <li>Real-path mapping (aliases)
 * </ul>
 *
 * <p>URIs are relative to the application root (e.g. /myapp) for
 * most ServletContext methods.  So you can define user workspaces with
 * identical JSP files and servlets in different applications.
 *
 * <h4>Including and forwarding</h4>
 *
 * <p>Forwarding and including files, the Servlet equivalent of SSI are
 * handled by the RequestDispatcher methods.
 *
 * <h4>Global initialization</h4>
 *
 * <p>There is no direct equivalent of a global.jsa.  To initialize
 * and cleanup shared classes on start and stop, use a load-on-startup
 * servlet.  The init() method will be called when the application starts
 * and the destroy() method will be called when the application finishes.
 *
 * <pre><code>
 *   &lt;servlet servlet-name='global'
 *            servlet-class='test.InitServlet'
 *            load-on-startup/>
 * </code></pre>
 *
 * <h4>Basic configuration</h4>
 *
 * In the resin.conf, to define the /myapp application with a document
 * root in /www/myweb, add the following to the resin.conf.
 *
 * <pre><code>
 *   &lt;web-app id='/myapp' app-dir='/www/myweb'/>
 * </code></pre>
 *
 * <h4>Servlet and Bean locations (class loaders)</h4>
 *
 * Each application has its own directories to load application servlets
 * and beans.  By default these are WEB-APP/classes and WEB-APP/lib.
 * To add a servlet test.MyServlet, create the java file:
 * <center>/www/myweb/WEB-APP/classes/test/MyServlet.java</center>
 *
 * <h4>Load balancing</h4>
 *
 * When using load balancing with a web server, each JVM will have its own
 * application object.  The attributes are not shared.  In contrast,
 * sessions are always sent to the same JVM.
 *
 * <p>So the application object is best used as a cache rather than
 * as a way for servlets to communicate.
 */
public interface ServletContext {
  public static final String ORDERED_LIBS = "javax.servlet.context.orderedLibs"; 
  public static final String TEMPDIR = "javax.servlet.context.tempdir"; 

  /**
   * Returns the URL prefix for the ServletContext.
   */
  public String getServletContextName();

  /**
   * Returns a server-specific string identifying the servlet engine.
   */
  public String getServerInfo();

  /**
   * Returns the major version of the servlet API.
   */
  public int getMajorVersion();

  /**
   *
   * @return major version of the spec the app is based on
   * @Since Servlet 3.0
   */
  public int getEffectiveMajorVersion();

  /**
   * Returns the minor version of the servlet API.
   */
  public int getMinorVersion();

  /**
   * @return minor version of the spec the app is based on
   * @Since Servlet 3.0
   */
  public int getEffectiveMinorVersion();

  /**
   * Returns the value of an initialization parameter from the configuration
   * file.
   *
   * The Resin configuration looks something like:
   * <pre><code>
   * &lt;web-app id='/myapp' app-dir='/www/myapp'>
   *   &lt;context-param name1='value1'/>
   *   &lt;context-param name2='value2'/>
   * &lt;/web-app>
   * </code></pre>
   *
   * @param name init parameter name
   * @return init parameter value
   */
  public String getInitParameter(String name);

  /**
   * Returns an enumeration of all init parameter names.
   */
  public Enumeration<String> getInitParameterNames();

  /**
   * Returns the ServletContext for the uri.
   * Note: the uri is <em>not</em> relative to the application.T
   *
   * @param uri path relative to the root
   * @return the ServletContext responsible for the given uri.
   */
  public ServletContext getContext(String uri);

  /**
   * Returns the context-path for the web-application.
   */
  public String getContextPath();

  /**
   * Returns the real file path for the given uri.  The file path will
   * be in native path format (with native path separators.)
   *
   * <p>See ServletRequest to return the real path relative to the
   * request uri.
   *
   * @param uri path relative to the application root to be translated.
   * @return native file path for the uri.
   */
  public String getRealPath(String uri);

  /**
   * Returns a request dispatcher for later inclusion or forwarding.  This
   * is the servlet API equivalent to SSI includes.  The uri is relative
   * to the application root.
   *
   * <p>The following example includes the result of executing inc.jsp
   * into the output stream.  If the context path is /myapp, the equivalent
   * uri is /myapp/inc.jsp
   *
   * <code><pre>
   *   RequestDispatcher disp;
   *   disp = getRequestDispatcher("/inc.jsp?a=b");
   *   disp.include(request, response);
   * </pre></code>
   *
   * <p>See ServletRequest to return a request dispatcher relative to the
   * request uri.
   *
   * @param uri path relative to the app root (including query string)
   * for the included file.
   * @return RequestDispatcher for later inclusion or forwarding.
   */
  public RequestDispatcher getRequestDispatcher(String uri);

  /**
   * Returns a request dispatcher based on a servlet name.
   *
   * @param servletName the servlet name to include or forward to.
   * @return RequestDispatcher for later inclusion or forwarding.
   */
  public RequestDispatcher getNamedDispatcher(String servletName);

  /**
   * Returns the mime type for the given uri.
   *
   * @param uri path relative to the application root.
   */
  public String getMimeType(String uri);

  /**
   * Returns an attribute value.
   *
   * @param name of the attribute.
   * @return stored value
   */
  public Object getAttribute(String name);

  /**
   * Returns an enumeration of all the attribute names.
   */
  public Enumeration<String> getAttributeNames();

  /**
   * Sets an attribute value.  Because servlets are multithreaded,
   * setting ServletContext attributes will generally need synchronization.
   *
   * <p>A typical initialization of an application attribute will look like:
   * <code><pre>
   * ServletContext app = getServletContext();
   * Object value;
   * synchronized (app) {
   *   value = app.getAttribute("cache");
   *   if (value == null) {
   *     value = new Cache();
   *     app.setAttribute("cache", value);
   *   }
   * }
   * </pre></code>
   *
   * @param name of the attribute.
   * @param value value to store
   */
  public void setAttribute(String name, Object value);

  /**
   * Removes an attribute.  Because servlets are multithreaded,
   * removing ServletContext attributes will generally need synchronization.
   *
   * @param name of the attribute.
   */
  public void removeAttribute(String name);

  /**
   * Logs a message.
   */
  public void log(String msg);

  /**
   * Logs a message and a stack trace.
   */
  public void log(String message, Throwable throwable);

  /**
   * Returns the resource for the given uri.  In general, the
   * RequestDispatcher routines are more useful.
   *
   * @param uri path relative to the application root.
   */
  public java.net.URL getResource(String uri)
    throws java.net.MalformedURLException;

  /**
   * Returns the set all resources held by the application.
   */
  public Set<String> getResourcePaths(String prefix);

  /**
   * Returns the resource as a stream.  In general, the
   * RequestDispatcher routines are more useful.
   *
   * @param path path relative to the application root.
   * @return InputStream to the resource.
   */
  public InputStream getResourceAsStream(String path);

  /**
   * @deprecated
   */
  public Servlet getServlet(String name)
    throws ServletException;

  /**
   * @deprecated
   */
  public Enumeration<Servlet> getServlets();

  /**
   * @deprecated
   */
  public Enumeration<String> getServletNames();

  /**
   * @deprecated
   */
  public void log(Exception exception, String msg);

  /**
   * Sets the session cookie configuration
   *
   * @Since Servlet 3.0
   */
  public SessionCookieConfig getSessionCookieConfig();

  /**
   * The session tracking mode
   *
   * @Since Servlet 3.0
   */
  public void setSessionTrackingModes(Set<SessionTrackingMode> modes);

  /**
   * The session tracking mode
   *
   * @Since Servlet 3.0
   */
  public Set<SessionTrackingMode> getDefaultSessionTrackingModes();

  /**
   * The session tracking mode
   *
   * @Since Servlet 3.0
   */
  public Set<SessionTrackingMode> getEffectiveSessionTrackingModes();

  /**
   * Sets init parameter
   * @param name
   * @param value
   * @return
   */
  public boolean setInitParameter(String name, String value);

  /**
   * Adds a servlet with the given className to context
  */
  public ServletRegistration.Dynamic addServlet(String servletName, String className);

  /**
   * Adds a servlet to context
   */
  public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet);

  /**
   * Adds a servlet class to the servlet container.
   */
  public ServletRegistration.Dynamic addServlet(String servletName,
                                                Class <? extends Servlet> servletClass);

  /**
   * Creates a servlet instance using the web-apps injection.
   */
  public <T extends Servlet> T createServlet(Class<T> c)
      throws ServletException;

  /** Returs servlet registration using servletName
   * @param servletName
   * @return a ServletRegistration object
   */
  public ServletRegistration getServletRegistration(String servletName);

  /**
   * Returns servlet registrations
   * @return
   */
  public Map<String, ? extends ServletRegistration> getServletRegistrations();

  /**
   * Adds a dynamic filter registration using className
   * @param filterName
   * @param className
   * @return
   */
  public FilterRegistration.Dynamic addFilter(String filterName, String className);

  /**
   * Adds a dynamic filter registration using filter
   *
   * @param filterName
   * @param filter
   * @return
   */
  public FilterRegistration.Dynamic addFilter(String filterName, Filter filter);

  /**
   * Adds a filter using filterClass
   * @param filterName
   * @param filterClass
   * @return
   */
  public FilterRegistration.Dynamic addFilter(String filterName,
                                              Class<? extends Filter> filterClass);

  /**
   * Create a filter using class
   * @param c
   * @param <T>
   * @return
   * @throws ServletException
   */
  public <T extends Filter> T createFilter(Class<T> c)
      throws ServletException;

  /**
   * Returns filter registration sing filterName
   * @param filterName
   * @return
   */
  public FilterRegistration getFilterRegistration(String filterName);

  /**
   * Returns filter registrations
   * @return
   */
  public Map<String, ? extends FilterRegistration> getFilterRegistrations();

  public void addListener(String className);

  public <T extends EventListener> void addListener(T t);

  public void addListener(Class <? extends EventListener> listenerClass);

  public <T extends EventListener> T createListener(Class<T> listenerClass)
    throws ServletException;

  public JspConfigDescriptor getJspConfigDescriptor();

  public ClassLoader getClassLoader();

  public void declareRoles(String... roleNames);
}
