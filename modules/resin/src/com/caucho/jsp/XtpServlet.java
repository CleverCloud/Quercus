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

package com.caucho.jsp;

import com.caucho.server.webapp.WebApp;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.jsp.JspFactory;

/**
 * Handles XTP pages.  Most of the work is done in the JspManager and QServlet.
 *
 * @see JspManager
 */
public class XtpServlet extends QServlet {
  static final String COPYRIGHT =
    "Copyright (c) 1998-2010 Caucho Technology.  All rights reserved.";

  /**
   * Initializes the servlet.  Primarily, this sets the PageManager to the
   * correct XtpManager.
   *
   * <p>The servlet parameter 'strict-xsl' forces XSL stylesheets to
   * follow the strict specification.  By default, XSL stylesheets follow
   * the looser 'XSLT-lite.'
   *
   * <p>The servlet parameter 'strict-xml' forces XTP pages to
   * follow strict XML.  By default, they're LooseHtml.
   *
   * <p>The servlet parameter 'default-stylesheet' sets the default
   * stylesheet.  The initial value is 'default.xsl'.
   */
  public void init(ServletConfig conf)
    throws ServletException
  {
    super.init(conf);

    XtpManager manager = new XtpManager();
    manager.initWebApp((WebApp) getServletContext());
      
    setManager(manager);

    String strictXslValue = conf.getInitParameter("strict-xsl");
    if (strictXslValue != null &&
        ! strictXslValue.equals("false") &&
        ! strictXslValue.equals("no"))
      manager.setStrictXsl(true);
  
    String strictXmlValue = conf.getInitParameter("strict-xml");
    if (strictXmlValue != null &&
        ! strictXmlValue.equals("false") &&
        ! strictXmlValue.equals("no"))
      manager.setStrictXml(true);
      
    String entitiesAsText = conf.getInitParameter("entities-as-text");
    if (entitiesAsText != null &&
        ! entitiesAsText.equals("false") &&
        ! entitiesAsText.equals("no"))
      manager.setEntitiesAsText(true);
      
    String toLower = conf.getInitParameter("html-to-lower");
    if (toLower != null &&
        (toLower.equals("no") || toLower.equals("false")))
      manager.setToLower(false);

    String defaultStylesheet = getInitParameter("default-stylesheet");
    if (defaultStylesheet != null && ! defaultStylesheet.equals(""))
      manager.setDefaultStylesheet(defaultStylesheet);

    if (JspFactory.getDefaultFactory() == null)
      JspFactory.setDefaultFactory(QJspFactory.create());
  }

  public String getServletInfo()
  {
    return "XTP";
  }
}
