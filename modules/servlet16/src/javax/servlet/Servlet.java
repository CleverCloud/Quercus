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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.servlet;

import java.io.IOException;

/**
 * A servlet is any Java class with a null-arg constructor that
 * implements the Servlet API.
 *
 * <p>Simple servlets should extend HttpServlet to create servlets.
 *
 * <p>Servlets that need full control should extend GenericServlet.
 *
 * <h4>Location</h4>
 *
 * Servlets are usually loaded from WEB-INF/classes under the application's
 * root.  To add a servlet test.MyServlet, create the java file:
 * <center>/www/myweb/WEB-APP/classes/test/MyServlet.java</center>
 *
 * <p>Servlets can also live in the global classpath.
 *
 * <h4>Configuration</h4>
 *
 * Servlet configuration for Resin is in the resin.conf file.
 *
 * <pre><code>
 * &lt;servlet servlet-name='hello'
 *          servlet-class='test.HelloServlet'
 *          load-on-startup>
 *   &lt;init-param param1='value1'/>
 *   &lt;init-param param2='value2'/>
 * &lt;/servlet>
 * </code></pre>
 *
 * <h4>Dispatch</h4>
 *
 * The servlet engine selects servlets based on the
 * <code>servlet-mapping</code> configuration.  Servlets can use the
 * special 'invoker' servlet or they can be configured to execute directly.
 *
 * <p>To get a path info, your servlet needs to use a wildcard.  In the
 * following example, /Hello will match the 'hello' servlet, but
 * /Hello/there will match the 'defaultServlet' servlet with a pathinfo
 * of /Hello/there.
 *
 * <pre><code>
 * &lt;servlet-mapping url-pattern='/'
 *                  servlet-name='defaultServlet'/>
 *
 * &lt;servlet-mapping url-pattern='/Hello'
 *                  servlet-name='hello'/>
 *
 * &lt;servlet-mapping url-pattern='/servlet/*'
 *                  servlet-name='invoker'/>
 *
 * &lt;servlet-mapping url-pattern='*.jsp'
 *                  servlet-name='com.caucho.jsp.JspServlet'/>
 * </code></pre>
 *
 * <h4>Life cycle</h4>
 *
 * Servlets are normally initialized when they are first loaded.  You can
 * force loading on startup using the 'load-on-startup' attribute to the
 * servlet configuration.  This is a useful technique for the equivalent
 * of the global.jsa file.
 *
 * <p>A servlet can count on having only one instance per
 * application (JVM) unless it implements SingleThreadedModel.
 *
 * <p>Servlet requests are handed by the <code>service</code> routine.
 * Since the servlet engine is multithreaded, multiple threads may call
 * <code>service</code> simultaneously.
 *
 * <p>When the application closes, the servlet engine will call
 * <code>destroy</code>.  Note, applications always close and are restarted
 * whenever a servlet changes.  So <code>init</code> and <code>destroy</code>
 * may be called many times while the server is still up.
 */
public interface Servlet {
  /**
   * Returns an information string about the servlet.
   */
  public String getServletInfo();
  /**
   * Initialize the servlet.  ServletConfig contains servlet parameters
   * from the configuration file.  GenericServlet will store the config
   * for later use.
   *
   * @param config information from the configuration file.
   */
  public void init(ServletConfig config) throws ServletException;

  /**
   * Returns the servlet configuration, usually the same value as passed
   * to the init routine.
   */
  public ServletConfig getServletConfig();

  /**
   * Service a request.  Since the servlet engine is multithreaded,
   * many threads may execute <code>service</code> simultaneously.  Normally,
   * <code>req</code> and <code>res</code> will actually be
   * <code>HttpServletRequest</code> and <code>HttpServletResponse</code>
   * classes.
   *
   * @param req request information.  Normally servlets will cast this
   * to <code>HttpServletRequest</code>
   * @param res response information.  Normally servlets will cast this
   * to <code>HttpServletRequest</code>
   */
  public void service(ServletRequest req, ServletResponse res)
    throws IOException, ServletException;

  /**
   * Called when the servlet shuts down.  Servlets can use this to close
   * database connections, etc.  Servlets generally only shutdown when
   * the application closes.
   */
  public void destroy();
}
