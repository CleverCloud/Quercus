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

package com.caucho.server.dispatch;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import com.caucho.server.webapp.*;
import com.caucho.server.host.*;

/**
 * Represents the final servlet in a filter chain.
 */
public class RedirectSecureFilterChain implements FilterChain {
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
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    WebApp webApp = (WebApp) req.getServletContext();
    String path = req.getContextPath();
    String servletPath = req.getServletPath();
    String pathInfo = req.getPathInfo();

    if (servletPath != null)
      path += servletPath;

    if (pathInfo != null)
      path += pathInfo;
    
    String queryString = req.getQueryString();

    if (queryString != null)
      path += "?" + queryString;

    Host host = (Host) webApp.getParent();
    String secureHostName = req.getServerName();

    if (host != null && host.getSecureHostName() != null)
      secureHostName = host.getSecureHostName();
    
    res.sendRedirect(res.encodeURL("https://" + secureHostName + path));
  }
}
