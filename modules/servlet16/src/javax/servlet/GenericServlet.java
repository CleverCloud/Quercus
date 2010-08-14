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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.servlet;

import java.util.Enumeration;
import java.util.logging.Logger;

/**
 * GenericServlet is a convenient abstract class for defining Servlets.
 * Servlets which need more control than HttpServlet can extend
 * GenericServlet.
 *
 * <p>In addition, GenericServlet implements ServletConfig to make
 * it easier to get configuration information.
 *
 * <p>Servlet configuration looks like the following:
 *
 * <pre><code>
 * &lt;servlet servlet-name='myservlet'
 *          servlet-class='test.MyServlet'>
 *   &lt;init-param param1='value1'/>
 *   &lt;init-param param2='value2'/>
 * &lt;/servlet>
 * </code></pre>
 */
public abstract class GenericServlet
  implements Servlet, ServletConfig, java.io.Serializable
{
  private static final Logger log
    = Logger.getLogger(GenericServlet.class.getName());

  private transient ServletConfig _config;

  /**
   * Initialize the servlet.  Most servlets should override the zero
   * parameter <code>init()</code> instead.
   *
   * @param config the servlet's configuration
   */
  public void init(ServletConfig config) throws ServletException
  {
    _config = config;
    // log("init");
    init();
  }
  
  /**
   * Initialize the servlet.  Servlets should override this method
   * if they need any initialization like opening pooled
   * database connections.
   */
  public void init() throws ServletException
  {
  }
  
  /**
   * Returns this servlet's configuration.
   */
  public ServletConfig getServletConfig()
  {
    return _config;
  }
  
  /**
   * Returns the servlet name for this configuration.  For example,
   * 'myservlet' in the following configuration:
   *
   * <pre><code>
   * &lt;servlet servlet-name='myservlet'
   *          servlet-class='test.MyServlet'/>
   * </code></pre>
   */
  public String getServletName()
  {
    ServletConfig config = getServletConfig();
    
    return config != null ? config.getServletName() : getClass().getName();
  }
  
  /**
   * Returns an initialization parameter.  Initialization parameters
   * are defined in the servlet configuration (in resin.conf) as follows:
   *
   * <pre><code>
   * &lt;servlet servlet-name='myservlet'
   *          servlet-class='test.MyServlet'>
   *   &lt;init-param param1='value1'/>
   *   &lt;init-param param2='value2'/>
   * &lt;/servlet>
   * </code></pre>
   *
   * @param name of the parameter
   * @return the init parameter value
   */
  public String getInitParameter(String name)
  {
    ServletConfig config = getServletConfig();
    
    return config != null ? config.getInitParameter(name) : null;
  }
  
  /**
   * Enumerates all the initialization parameter.
   */
  public Enumeration<String> getInitParameterNames()
  {
    ServletConfig config = getServletConfig();
    
    return config != null ? config.getInitParameterNames() : null;
  }
  
  /**
   * Returns the application (servlet context) that the servlet
   * belongs to.  The application provides several useful methods, e.g.
   * including other files, forwarding, and translating physical paths.
   */
  public ServletContext getServletContext()
  {
    ServletConfig config = getServletConfig();
    
    return config != null ? config.getServletContext() : null;
  }
  
  /**
   * Returns a string describing the servlet.
   */
  public String getServletInfo()
  {
    return "";
  }
  
  /**
   * Logs an error message in the application's log.
   */
  public void log(String message)
  {
    log.config(getServletName() + ": " + message);
  }
  
  /**
   * Logs an error message and an exception trace in the application's log.
   */
  public void log(String message, Throwable cause)
  {
    ServletContext app = getServletContext();
    
    if (app != null)
      app.log(getClass().getName() + ": " + message, cause);
  }
  
  /**
   * Called when the servlet (and the application) shuts down.
   */
  public void destroy()
  {
    // log("destroy");
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getServletContext() + "]";
  }
}
