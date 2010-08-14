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
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Does an internal forward of the request.
 */
public class ForwardFilterChain implements FilterChain {
  private static final Logger log
    = Logger.getLogger(ForwardFilterChain.class.getName());
  
  // servlet
  private String _url;
  private RequestDispatcher _disp;

  /**
   * Create the forward filter chain servlet.
   *
   * @param url the request dispatcher to forward to.
   */
  public ForwardFilterChain(String url)
  {
    _url = url;
  }

  /**
   * Create the forward filter chain servlet.
   *
   * @param disp the request dispatcher to forward to.
   */
  public ForwardFilterChain(RequestDispatcher disp)
  {
    _disp = disp;
  }

  /**
   * Forwards to the dispatch
   *
   * @param request the servlet request
   * @param response the servlet response
   */
  public void doFilter(ServletRequest request,
                       ServletResponse response)
    throws ServletException, IOException
  {
    try {
      if (_disp != null)
        _disp.forward(request, response);
      else {
        HttpServletRequest req = (HttpServletRequest) request;

        RequestDispatcher disp = req.getRequestDispatcher(_url);

        disp.forward(request, response);
      }
    } catch (FileNotFoundException e) {
      log.log(Level.FINER, e.toString(), e);
      
      HttpServletResponse res = (HttpServletResponse) response;
      
      res.sendError(404);
    }
  }

  public String toString()
  {
    if (_disp != null)
      return "ForwardFilterChain[" + _disp + "]";
    else
      return "ForwardFilterChain[" + _url + "]";
  }
}
