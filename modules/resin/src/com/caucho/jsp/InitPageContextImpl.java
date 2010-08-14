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

import com.caucho.el.EL;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.HttpServletResponseImpl;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;

import javax.el.ELContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.ErrorData;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.tagext.BodyContent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import java.util.HashMap;

public class InitPageContextImpl extends PageContextImpl {
  private ELContext _elContext;

  public InitPageContextImpl(WebApp webApp, Servlet servlet)
  {
    super(webApp, servlet);
  }

  public InitPageContextImpl(WebApp webApp, HashMap<String,Method> functionMap)
  {
    super(webApp, functionMap);
  }

  /**
   * Finds an attribute in any of the scopes from page to webApp.
   *
   * @param name the attribute name.
   *
   * @return the attribute value
   */
  @Override
  public Object resolveVariable(String name)
    throws javax.el.ELException
  {
    if (_elContext == null)
      _elContext = EL.getEnvironment();

    return _elContext.getELResolver().getValue(_elContext, null, name);
  }
}
