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

package com.caucho.servlets.ssi;

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serves server-side include files.
 */
public class SSIServlet extends HttpServlet
{
  private static final Logger log
    = Logger.getLogger(SSIServlet.class.getName());

  private SSIFactory _factory;

  /**
   * Set's the SSIFactory, default is a factory that handles
   * the standard Apache SSI commands.
   */
  public void setFactory(SSIFactory factory)
  {
    _factory = factory;
  }

  public void init()
    throws ServletException
  {
    super.init();

    if (_factory == null)
      _factory = new SSIFactory();
  }

  public void doGet(HttpServletRequest request,
                    HttpServletResponse response)
    throws ServletException, IOException
  {
    String servletPath;
    String pathInfo;
    
    servletPath = (String) request.getAttribute("javax.servlet.include.servlet_path");
    pathInfo = (String) request.getAttribute("javax.servlet.include.path_info");

    if (servletPath == null && pathInfo == null) {
      servletPath = request.getServletPath();
      pathInfo = request.getPathInfo();
    }

    String fullPath;

    if (pathInfo != null)
      fullPath = servletPath + pathInfo;
    else
      fullPath = servletPath;

    // XXX: check cache

    String realPath = request.getRealPath(servletPath);

    Path path = Vfs.lookup().lookup(realPath);

    if (! path.canRead() || path.isDirectory()) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    response.setContentType("text/html");

    Statement stmt = new SSIParser(_factory).parse(path);

    WriteStream out = Vfs.openWrite(response.getOutputStream());

    try {
      stmt.apply(out, request, response);
      out.close();
    } catch (Exception e) {
      String errmsg = (String) request.getAttribute("caucho.ssi.errmsg");

      if (errmsg != null && ! response.isCommitted()) {
        log.log(Level.FINE, e.toString(), e);

        response.setStatus(500, errmsg);
        response.setContentType("text/html");

        out.clearWrite();
        out.println("<html><head>");
        out.println("<title>" + errmsg + "</title>");
        out.println("</head>");

        out.println("<h1>" + errmsg + "</h1>");
        out.println("</html>");
        out.close();
      }
      else if (e instanceof RuntimeException)
        throw (RuntimeException) e;
      else if (e instanceof IOException)
        throw (IOException) e;
      else if (e instanceof ServletException)
        throw (ServletException) e;
      else
        throw new ServletException(e);
    }
  }
}
