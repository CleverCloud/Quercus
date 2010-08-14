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
 * @author Sam
 */


package com.caucho.tools.profiler;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a {@link ProfilerPoint} for each invocation of {@link #doFilter}.
 */
public class ProfilerFilter
  implements Filter
{
  private static final L10N L = new L10N(ProfilerFilter.class);
  private static final Logger log
    = Logger.getLogger(ProfilerFilter.class.getName());

  // can do this because an instance of Filter is created for each environment
  private final ProfilerManager _profilerManager = ProfilerManager.getLocal();

  private boolean _isUseQuery = false;

  public ProfilerFilter()
  {
  }

  /**
   * If true, use the query portion of the url to distinguish requests, default
   * is false.
   */
  public void setUseQuery(boolean useQuery)
  {
    _isUseQuery = useQuery;
  }

  public boolean isUseQuery()
  {
    return _isUseQuery;
  }

  public void init(FilterConfig filterConfig)
    throws ServletException
  {
  }

  public void doFilter(ServletRequest servletRequest,
                       ServletResponse servletResponse,
                       FilterChain chain)
    throws ServletException, IOException
  {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    String uri = request.getRequestURI();

    String servletPath = (String) request.getAttribute(
      "javax.servlet.include.servlet_path");
    String pathInfo;
    String queryString;

    if (servletPath == null) {
      servletPath = request.getServletPath();
      pathInfo = request.getPathInfo();

      if (isUseQuery())
        queryString = request.getQueryString();
      else
        queryString = null;
    }
    else {
      pathInfo = (String) request.getAttribute("javax.servlet.include.path_info");

      if (isUseQuery())
        queryString = (String) request.getAttribute(
          "javax.servlet.include.query_string");
      else
        queryString = null;
    }

    String name;

    if (pathInfo == null && queryString == null)
      name = servletPath;
    else {
      CharBuffer nameBuilder = new CharBuffer();

      nameBuilder.append(servletPath);

      if (pathInfo != null)
        nameBuilder.append(pathInfo);

      if (queryString != null) {
        nameBuilder.append('?');
        nameBuilder.append(queryString);
      }

      name = nameBuilder.toString();
    }

    ProfilerPoint profilerPoint = _profilerManager.getProfilerPoint(name);

    if (log.isLoggable(Level.FINEST))
      log.finest(profilerPoint.toString());

    Profiler profiler = profilerPoint.start();

    try {
      chain.doFilter(request, response);
    }
    finally {
      profiler.finish();
    }
  }

  public void destroy()
  {
  }
}
