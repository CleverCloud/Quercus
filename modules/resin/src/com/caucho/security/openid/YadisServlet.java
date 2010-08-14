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

package com.caucho.security.openid;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

public class YadisServlet extends GenericServlet
{
  private static final Logger log
    = Logger.getLogger(YadisServlet.class.getName());

  private HashMap<String,YadisXrd> _userMap
    = new HashMap<String,YadisXrd>();

  public void addUser(YadisXrd user)
  {
    _userMap.put(user.getId(), user);
  }
  
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    PrintWriter out = res.getWriter();

    String method = req.getMethod();

    String userId = req.getPathInfo();

    if (userId != null && userId.startsWith("/"))
      userId = userId.substring(1);

    YadisXrd xrd = _userMap.get(userId);

    if (xrd == null) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    if ("HEAD".equals(method) || "GET".equals(method)) {
      doGet(req, res, out, xrd);
    }
    else {
      res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
      return;
    }
  }

  private void doGet(HttpServletRequest req,
                     HttpServletResponse res,
                     PrintWriter out,
                     YadisXrd xrd)
    throws IOException, ServletException
  {
    if (xrd.getLocation() != null) {
      res.setHeader("X-XRDS-Location", xrd.getLocation());
      res.setHeader("Content-Type", "text/html");

      out.println("<html>");
      out.println("<head>");
      out.println("<meta http-equiv='X-XRDS-Location'"
                  + " content='" + xrd.getLocation() + "'>");
      out.println("</head>");
      out.println("<body>");
      out.println("<p>XRDS Location: " + xrd.getLocation() + "</p>");
      out.println("</body>");

      return;
    }

    res.setHeader("Content-Type", "application/xrds+xml");
    res.setHeader("Vary", "Accept-Encoding");
    
    xrd.print(out);
  }
}
