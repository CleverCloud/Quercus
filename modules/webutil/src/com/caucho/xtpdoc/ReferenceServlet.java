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

public class ReferenceServlet extends HttpServlet {
  private static Logger log 
    = Logger.getLogger(ReferenceServlet.class.getName());

  private XMLOutputFactory _outputFactory;
  private String _encoding = "utf-8";
  private ReferenceDocument _referenceDocument;
  private Path _referencePath;

  public void setReferenceDocument(Path referencePath)
  {
    _referencePath = referencePath;
  }

  public void init(ServletConfig servletConfig)
    throws ServletException
  {
    super.init(servletConfig);

    try {
      _outputFactory = XMLOutputFactory.newInstance();
    }
    catch (FactoryConfigurationError e) {
      throw new ServletException("Error configuring factory", e);
    }

    if (_outputFactory == null)
      throw new ServletException("Error configuring factory");

    String contextPath 
      = servletConfig.getServletContext().getServletContextName();

    _referenceDocument = new ReferenceDocument(_referencePath, contextPath);
    _referenceDocument.setJavascriptEnabled(false);

    Config config = new Config();
    config.setEL(false);

    try {
      config.configure(_referenceDocument, _referencePath);
    }
    catch (IOException e) {
      throw new ServletException(e);
    }
  }

  public void service(HttpServletRequest request,
                      HttpServletResponse response)
    throws ServletException, IOException
  {
    OutputStream os = response.getOutputStream();

    try {
      String ref = (String) request.getParameter("ref");
      Defun defun = _referenceDocument.getDefun(ref);

      if (defun == null) {
        response.sendError(404);

        return;
      }

      response.setContentType("text/html");

      XMLStreamWriter xmlOut
        = _outputFactory.createXMLStreamWriter(os, _encoding);

      defun.writeHtml(xmlOut);

      xmlOut.flush();
    }
    catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
      
      log.warning(e.toString());

      response.sendError(404);
    }
    catch (Exception e) {
      throw new ServletException("Error configuring document", e);
    }
  }
}
