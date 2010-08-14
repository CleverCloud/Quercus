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

package com.caucho.server.dispatch;

import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import java.io.IOException;

/**
 * Represents the final servlet in a filter chain.
 */
public class SingleThreadServletFilterChain implements FilterChain {
  // servlet config
  private ServletConfigImpl _config;
  // servlet
  private Servlet _servlet;

  /**
   * Create the filter chain servlet.
   *
   * @param servlet the underlying servlet
   */
  public SingleThreadServletFilterChain(ServletConfigImpl config)
  {
    if (config == null)
      throw new NullPointerException();
    
    _config = config;
  }
  
  /**
   * Invokes the final servlet at the end of the chain.
   *
   * @param request the servlet request
   * @param response the servlet response
   *
   * @since Servlet 2.3
   */
  public void doFilter(ServletRequest request,
                       ServletResponse response)
    throws ServletException, IOException
  {
    Servlet servlet = null;

    synchronized (this) {
      servlet = _servlet;
      _servlet = null;
    }
    
    if (servlet == null) {
      try {
        servlet = (Servlet) _config.createServlet(true);
      } catch (ServletException e) {
        throw e;
      } catch (Exception e) {
        throw new ServletException(e);
      }
    }
    
    try {
      servlet.service(request, response);
    } catch (UnavailableException e) {
      _config.setInitException(e);
      // application.killServlet(servlet, config);

      throw e;
    }

    synchronized (this) {
      if (_servlet == null) {
        _servlet = servlet;
        servlet = null;
      }
    }

    if (servlet != null)
      servlet.destroy();
  }
}
