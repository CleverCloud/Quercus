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
 * @author Emil Ong
 */

package com.caucho.xtpdoc;

import com.caucho.config.*;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.util.logging.*;

public class ResinDocServlet extends HttpServlet {
  private static Logger log = Logger.getLogger(ResinDocServlet.class.getName());

  private String _contextPath;
  private Config _config;
  private Path _pwd;
  private XMLOutputFactory _outputFactory;
  private String _encoding = "utf-8";
  private boolean _isDisableAction;

  public void setDocumentEncoding(String encoding)
  {
    _encoding = encoding;
  }

  public void setDocContextPath(String contextPath)
  {
    _contextPath = contextPath;
  }

  public void setDisableAction(boolean isDisableAction)
  {
    _isDisableAction = isDisableAction;
  }

  public boolean isDisableAction()
  {
    return _isDisableAction;
  }

  public void init(ServletConfig config)
    throws ServletException
  {
    super.init(config);

    _config = new Config();
    _config.setEL(false);
    _pwd = Vfs.lookup().createRoot();

    if (_contextPath == null)
      _contextPath = config.getServletContext().getServletContextName();

    try {
      _outputFactory = XMLOutputFactory.newInstance();
    } catch (FactoryConfigurationError e) {
      throw new ServletException("Error configuring factory", e);
    }

    if (_outputFactory == null)
      throw new ServletException("Error configuring factory");
  }

  public void service(HttpServletRequest request,
                      HttpServletResponse response)
    throws ServletException, IOException
  {
    OutputStream os = response.getOutputStream();
    String servletPath = request.getServletPath();

    Path path = Vfs.lookup(request.getRealPath(servletPath));

    Document document = new Document(getServletContext(),
                                     path, _contextPath,
                                     request.getContextPath() + servletPath,
                                     _encoding);

    document.setDisableAction(isDisableAction());

    try {
      response.setContentType("text/html");

      XMLStreamWriter xmlOut
        = _outputFactory.createXMLStreamWriter(os, _encoding);

      _config.configure(document, path);

      if (document.getRedirect() != null) {
        response.sendRedirect(document.getRedirect());
        return;
      }
      
      document.writeHtml(xmlOut);

      xmlOut.flush();
    } catch (ConfigException e) {
      throw e;
    } catch (FileNotFoundException e) {
      log.log(Level.FINER, e.toString(), e);

      response.sendError(404);
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
      
      log.warning(e.toString());

      response.sendError(404);
    } catch (Exception e) {
      throw new ServletException("Error configuring document", e);
    }
  }
}
