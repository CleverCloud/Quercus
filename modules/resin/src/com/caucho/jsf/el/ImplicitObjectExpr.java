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

package com.caucho.jsf.el;

import com.caucho.el.Expr;
import com.caucho.vfs.WriteStream;

import javax.el.ELContext;
import javax.el.ELException;
import javax.faces.context.*;
import javax.faces.component.UIComponent;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ImplicitObjectExpr extends Expr {
  private static final HashMap<String,ImplicitObjectExpr> _exprMap
    = new HashMap<String,ImplicitObjectExpr>();

  private String _id;
  private ImplicitEnum _code;

  private ImplicitObjectExpr(String id, ImplicitEnum code)
  {
    _id = id;
    _code = code;
  }

  static ImplicitObjectExpr create(String name)
  {
    return _exprMap.get(name);
  }

  /**
   * Evaluate the expr as an object.
   *
   * @param env the page context
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    FacesContext context
      = (FacesContext) env.getContext(FacesContext.class);
    
    if (context == null)
      return null;

    switch (_code) {
    case APPLICATION:
      return context.getExternalContext().getContext();
    case APPLICATION_SCOPE:
      return context.getExternalContext().getApplicationMap();
//    case COMPONENT: {
//      try {
//        return context.getAttributes().get("component");
//      }
//      catch (UnsupportedOperationException e) {
//        if (log.isLoggable(Level.FINEST))
//          log.log(Level.FINEST, e.getMessage(), e);
//      }
//
//      return null;
//    }
    case COOKIE:
      return context.getExternalContext().getRequestCookieMap();
    case FACES_CONTEXT:
      return context;
    case HEADER:
      return context.getExternalContext().getRequestHeaderMap();
    case HEADER_VALUES:
      return context.getExternalContext().getRequestHeaderValuesMap();
    case INIT_PARAM:
      return context.getExternalContext().getInitParameterMap();
    case PARAM:
      return context.getExternalContext().getRequestParameterMap();
    case PARAM_VALUES:
      return context.getExternalContext().getRequestParameterValuesMap();
    case REQUEST:
      return context.getExternalContext().getRequest();
    case REQUEST_SCOPE:
      return context.getExternalContext().getRequestMap();
    case SESSION:
      return context.getExternalContext().getSession(true);
    case SESSION_SCOPE:
      return context.getExternalContext().getSessionMap();
    case VIEW:
      return context.getViewRoot();
    }

    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return _id;
  }

  /**
   * Prints the code to create an IdExpr.
   */
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("com.caucho.jsf.el.ImplicitObjectExpr.create(\"");
    printEscapedString(os, _id);
    os.print("\")");
  }

  enum ImplicitEnum {
    APPLICATION,
    APPLICATION_SCOPE,
//    COMPONENT,
    COOKIE,
    FACES_CONTEXT,
    HEADER,
    HEADER_VALUES,
    INIT_PARAM,
    PARAM,
    PARAM_VALUES,
    RESOURCE,
    REQUEST,
    REQUEST_SCOPE,
    SESSION,
    SESSION_SCOPE,
    VIEW,
  };

  static {
    _exprMap.put("application",
                 new ImplicitObjectExpr("application",
                                        ImplicitEnum.APPLICATION));
    _exprMap.put("applicationScope",
                 new ImplicitObjectExpr("applicationScope",
                                        ImplicitEnum.APPLICATION_SCOPE));
//    _exprMap.put("component",
//                 new ImplicitObjectExpr("component",
//                                        ImplicitEnum.COMPONENT));
    _exprMap.put("cookie",
                 new ImplicitObjectExpr("cookie",
                                        ImplicitEnum.COOKIE));
    _exprMap.put("facesContext",
                 new ImplicitObjectExpr("facesContext",
                                        ImplicitEnum.FACES_CONTEXT));
    _exprMap.put("header",
                 new ImplicitObjectExpr("header",
                                        ImplicitEnum.HEADER));
    _exprMap.put("headerValues",
                 new ImplicitObjectExpr("headerValues",
                                        ImplicitEnum.HEADER_VALUES));
    _exprMap.put("initParam",
                 new ImplicitObjectExpr("initParam",
                                        ImplicitEnum.INIT_PARAM));
    _exprMap.put("param",
                 new ImplicitObjectExpr("param",
                                        ImplicitEnum.PARAM));
    _exprMap.put("paramValues",
                 new ImplicitObjectExpr("paramValues",
                                        ImplicitEnum.PARAM_VALUES));
    _exprMap.put("request",
                 new ImplicitObjectExpr("request",
                                        ImplicitEnum.REQUEST));
    _exprMap.put("requestScope",
                 new ImplicitObjectExpr("requestScope",
                                        ImplicitEnum.REQUEST_SCOPE));
    _exprMap.put("resource",
                 new ImplicitObjectExpr("resource",
                                        ImplicitEnum.RESOURCE));
    _exprMap.put("session",
                 new ImplicitObjectExpr("session",
                                        ImplicitEnum.SESSION));
    _exprMap.put("sessionScope",
                 new ImplicitObjectExpr("sessionScope",
                                        ImplicitEnum.SESSION_SCOPE));
    _exprMap.put("view",
                 new ImplicitObjectExpr("view",
                                        ImplicitEnum.VIEW));
  }
}
