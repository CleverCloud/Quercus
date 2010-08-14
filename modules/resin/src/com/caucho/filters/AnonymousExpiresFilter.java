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

package com.caucho.filters;

import com.caucho.config.types.Period;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Caches the servlet output for anonymous users.  This filter adds
 * an Expires header and the Cache-Control: x-anonymous header.
 *
 * <p>Requests from anonymous users, i.e. users with no JSESSIONID cookie
 * or ;jsessionid= URL-rewriting, will be cached.  So logged-in users
 * will have customized pages, but anonymous users will see a cached page.
 * Since there are generally more anonymous users, using the
 * AnonymousExpiresFilter can dramatically improve performance without
 * losing the ability to customize pages.
 *
 * <p>Pages should call <code>request.getSession(false)</code> to get their
 * sessions, because a page that creates a session will not be cached.
 * For the same reason, JSP pages would set
 * <code>&lt;jsp:directive.page session='false'/></code>.
 *
 * <p>The cache-time init-parameter configures how long the page should be
 * cached:
 *
 * <pre>
 * &lt;filter>
 *   &lt;filter-name>anonymous-cache&lt;/filter-name>
 *   &lt;filter-class>com.caucho.http.filter.AnonymousExpiresFilter&lt;/filter-class>
 *   &lt;init-param cache-time='10s'/>
 * &lt;/filter>
 * </pre>
 *
 * The cache-time allows the standard extensions:
 *
 * <table>
 * <tr><td>s<td>seconds
 * <tr><td>m<td>minutes
 * <tr><td>h<td>hours
 * <tr><td>D<td>days
 * <tr><td>W<td>weeks
 * <tr><td>M<td>months
 * <tr><td>Y<td>years
 * </table>
 *
 * @since Resin 2.0.5
 */
public class AnonymousExpiresFilter implements Filter {
  /**
   * How long to cache the file for.
   */
  private long _cacheTime = 2000;

  /**
   * Sets the file cache time.
   */
  public void setCacheTime(Period period)
  {
    _cacheTime = period.getPeriod();
  }
  
  /**
   * Filter init reads the filter configuration
   */
  public void init(FilterConfig config)
    throws ServletException
  {
    String time = config.getInitParameter("cache-time");

    if (time != null) {
      try {
        _cacheTime = Period.toPeriod(time);
      } catch (Exception e) {
        throw new ServletException(e);
      }
    }
  }
  
  /**
   * The filter add an Expires time in the future and adds
   * the x-anonymous Cache-Control directive.
   */
  public void doFilter(ServletRequest request, ServletResponse response,
                       FilterChain nextFilter)
    throws ServletException, IOException
  {
    if (_cacheTime > 0) {
      HttpServletResponse res = (HttpServletResponse) response;
      
      res.addHeader("Vary", "Cookie");
      res.addHeader("Cache-Control", "s-maxage=" + _cacheTime);
    }

    nextFilter.doFilter(request, response);
  }
  
  /**
   * Any cleanup for the filter.
   */
  public void destroy()
  {
  }
}
