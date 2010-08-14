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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Represents the final servlet in a filter chain.
 */
public class MovedFilterChain implements FilterChain {
  private int _code;
  private String _url;
  private String _queryString;

  /**
   * Create the redirect filter chain servlet.
   *
   * @param url the URL to redirect to.
   */
  public MovedFilterChain(int code, String url)
  {
    _code = code;
    _url = url;
  }

  /**
   * Create the redirect filter chain servlet.
   *
   * @param url the URL to redirect to.
   */
  public MovedFilterChain(int code, String url, String queryString)
  {
    _code = code;
    _url = url;
    _queryString = queryString;
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
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    String queryString = _queryString;

    if (queryString == null)
      queryString = req.getQueryString();

    String url;

    if (queryString != null && _url.indexOf('?') < 0)
      url = res.encodeURL(_url + '?' + queryString);
    else
      url = res.encodeURL(_url);

    res.setHeader("Location", url);

    res.setStatus(_code);

    PrintWriter out = response.getWriter();

    out.println("The URL has moved to <a href=\"" + url + "\">" + url + "</a>");
  }
}
