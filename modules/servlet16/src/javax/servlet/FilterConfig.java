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
 *
 * $Id: FilterConfig.java,v 1.2 2004/09/29 00:12:46 cvs Exp $
 */

package javax.servlet;

import java.util.Enumeration;

/**
 * Configuration for a filter.  The container will fill the
 * FilterConfig with the init parameters before passing it to
 * the filter.
 *
 * <p>Filter configuration in the web.xml looks like:
 * <pre><code>
 * &lt;filter filter-name='myservlet'
 *          filter-class='test.MyServlet'>
 *   &lt;init-param param1='value1'/>
 *   &lt;init-param param2='value2'/>
 * &lt;/filter>
 * </code></pre>
 *
 * @since Servlet 2.3
 */
public interface FilterConfig {
  /**
   * Returns the name of the filter.
   */
  public String getFilterName();
  
  /**
   * Returns an initialization parameter.  Initialization parameters
   * are defined in the filter configuration (in resin.conf).
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
