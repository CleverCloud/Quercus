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

import com.caucho.config.ConfigException;
import com.caucho.jsp.Page;
import com.caucho.jsp.QServlet;
import com.caucho.make.AlwaysModified;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the final servlet in a filter chain.
 */
public class PrecompilePageFilterChain implements FilterChain {
  private static final Logger log = Log.open(PageFilterChain.class);
  private static final L10N L = new L10N(PageFilterChain.class);
  
  private QServlet _servlet;
  private ServletContext _servletContext;

  /**
   * Create the filter chain servlet.
   *
   * @param servlet the JSP servlet
   */
  PrecompilePageFilterChain(QServlet servlet)
  {
    _servlet = servlet;
  }

  static FilterChain create(ServletInvocation invocation,
                            PageFilterChain pageChain)
  {
    String query = invocation.getQueryString();

    if (query == null)
      return pageChain;

    int p = query.indexOf("jsp_precompile");

    if (p < 0)
      return pageChain;

    String tail = query.substring(p + "jsp_precompile".length());

    if (tail.startsWith("=\"true\"") ||
        tail.startsWith("=true") ||
        ! tail.startsWith("=")) {
      if (invocation instanceof Invocation) {
        Invocation inv = (Invocation) invocation;

        inv.setDependency(AlwaysModified.create());
      }
      
      return new PrecompilePageFilterChain(pageChain.getServlet());
    }
    else if (tail.startsWith("=\"false\"") ||
             tail.startsWith("=false")) {
      // jsp/1910
      return pageChain;
    }
    else
      return new ExceptionFilterChain(new ConfigException("jsp_precompile requires a true or false value at '" + tail + "'."));
  }

  /**
   * Sets the servlet context.
   */
  public void setServletContext(ServletContext servletContext)
  {
    _servletContext = servletContext;
  }

  /**
   * Gets the servlet context.
   */
  public ServletContext getServletContext()
  {
    return _servletContext;
  }
  
  /**
   * Invokes the final servlet at the end of the chain.
   *
   * @param req the servlet request
   * @param res the servlet response
   */
  public void doFilter(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    
    try {
      Page page = _servlet.getPage(req, res);

      if (page == null) {
        res.sendError(res.SC_NOT_FOUND);
        return;
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
        
      throw new ServletException(e);
    }
    
    PrintWriter out = res.getWriter();
    out.println("Precompiled page.");
  }
}
