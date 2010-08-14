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

package com.caucho.jsp;

import com.caucho.server.webapp.WebApp;
import com.caucho.util.FreeList;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspEngineInfo;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

public class QJspFactory extends JspFactory {
  private static JspEngineInfo _engineInfo = new EngineInfo();

  private static QJspFactory _factory;

  static public QJspFactory create()
  {
    if (_factory == null)
      _factory = new QJspFactory();

    return _factory;
  }

  public PageContext getPageContext(Servlet servlet,
                                    ServletRequest request,
                                    ServletResponse response,
                                    String errorPageURL,
                                    boolean needsSession,
                                    int buffer,
                                    boolean autoFlush)
  {
    // WebApp webApp = (WebApp) request.getServletContext();

    PageContextImpl pc = new PageContextImpl();

    try {
      pc.initialize(servlet, request, response, errorPageURL,
                    needsSession, buffer, autoFlush);
    } catch (Exception e) {
    }

    return pc;
  }

  /**
   * Frees a page context after the JSP page is done with it.
   *
   * @param pc the PageContext to free
   */
  public void releasePageContext(PageContext pc)
  {
    if (pc != null) {
      pc.release();
    }
  }

  // This doesn't appear in the spec, but apparantly exists in some jsdk.jar
  public String getSpecificationVersion()
  {
    return getEngineInfo().getSpecificationVersion();
  }

  public JspEngineInfo getEngineInfo()
  {
    return _engineInfo;
  }

  public JspApplicationContext getJspApplicationContext(ServletContext context)
  {
    return ((WebApp) context).getJspApplicationContext();
  }

  static class EngineInfo extends JspEngineInfo {
    public String getSpecificationVersion()
    {
      return "2.1";
    }
  }
}
