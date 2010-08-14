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

import java.util.Enumeration;

/**
 * ServletConfig encapsulates servlet configuration and gives access to
 * the application (servlet context) object.
 *
 * Servlet initialization parameters appear in the servlet configuration
 * file.  Each servlet class may have several different servlet instances,
 * one for each servlet parameters:
 *
 * <pre><code>
 * &lt;servlet servlet-name='my1'
 *          servlet-class='test.MyServlet'>
 *   &lt;init-param param1='my1-1'/>
 *   &lt;init-param param2='my1-2'/>
 * &lt;/servlet>
 *
 * &lt;servlet servlet-name='my2'
 *          servlet-class='test.MyServlet'>
 *   &lt;init-param param1='my2-1'/>
 *   &lt;init-param param2='my2-2'/>
 * &lt;/servlet>
 * </code></pre>
 */
public interface ServletConfig {
  /**
   * Returns the servlet name for this configuration.  For example,
   * 'myservlet' in the following configuration:
   *
   * <pre><code>
   * &lt;servlet servlet-name='myservlet'
   *          servlet-class='test.MyServlet'/>
   * </code></pre>
   */
  public String getServletName();
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
  public String getInitParameter(String name);
  /**
   * Returns an enumeration of the init-parameter names
   */
  public Enumeration<String> getInitParameterNames();
  /**
   * Returns the ServletContext for the servlet or filter.
   */
  public ServletContext getServletContext();
}
