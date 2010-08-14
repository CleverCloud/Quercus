/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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

package com.caucho.doc;

import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.WebAppController;
import com.caucho.util.L10N;
import com.caucho.util.URLUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Find a local javadoc and use it, or present option to use javadoc from
 * Caucho website. 
 */
public class JavadocRedirectServlet extends HttpServlet {
  static protected final Logger log = 
    Logger.getLogger(JavadocRedirectServlet.class.getName());
  static final L10N L = new L10N(JavadocRedirectServlet.class);

  public void service(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    String query = request.getParameter("query");
    if (query == null || query.length() == 0)
       query = request.getPathInfo(); 
    if (query == null)
      query = "";

    query = URLUtil.encodeURL(query);

    // see if the resin-javadoc web-app is available
    WebApp app = (WebApp) getServletContext();

    WebApp japp = null;

    ArrayList appControllers = app.getParent().getWebAppList();

    for (int i = 0; i < appControllers.size(); i++)
    {
      WebAppController appController = (WebAppController) appControllers.get(i);

      String contextPath = appController.getContextPath();

      if (contextPath.startsWith("/resin-javadoc")) {
        japp = appController.getWebApp();
        break;
      }
    }

    if (japp != null) {
      String href = japp.getContextPath() + "?query=" + query;
      if (log.isLoggable(Level.FINER))
        log.finer(L.l("javadoc redirect to {0}",href));
      response.sendRedirect(response.encodeRedirectURL(href));
    } 
    else {
      if (log.isLoggable(Level.FINER))
        log.finer(L.l("javadoc no local javadoc"));
      String href = "http://www.caucho.com/resin-javadoc/?query=" + query;

      PrintWriter out = response.getWriter();
      out.println("<html>");
      out.println("<head><title>Resin&#174; Javadoc Not Found</title></head>");
      out.println("<body>");
      out.println("<h1 style='background: #ccddff'>Resin&#174; Javadoc Not Found</h1>");
      out.println("A local copy of the Resin javadoc could not be found.");
      out.println("<ul>");
      out.println("<li><a href='" + href + "'>use Caucho website</a>");
      out.println("</ul>");

      out.println("</body>");
      out.println("</html>");
      out.close();
    }
  }

}

