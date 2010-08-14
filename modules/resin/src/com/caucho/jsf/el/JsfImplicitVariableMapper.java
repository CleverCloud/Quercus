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

package com.caucho.jsf.el;

import java.util.*;
import javax.el.*;
import com.caucho.jsp.el.*;

/**
 * Represents implicit JSF variables.
 */
public class JsfImplicitVariableMapper extends VariableMapper
{
  public static final JsfImplicitVariableMapper MAPPER
    = new JsfImplicitVariableMapper();
  
  private static final HashMap<String,ValueExpression> _map
    = new HashMap<String,ValueExpression>();
  
  public ValueExpression resolveVariable(String variable)
  {
    return _map.get(variable);
  }
  
  public ValueExpression setVariable(String variable,
                                     ValueExpression expr)
  {
    return expr;
  }

  static {
    _map.put("application", ApplicationExpression.EXPR);
    _map.put("applicationScope", ApplicationScopeExpression.EXPR);
    _map.put("cookie", CookieExpression.EXPR);
    _map.put("header", HeaderExpression.EXPR);
    _map.put("headerValues", HeaderValuesExpression.EXPR);
    _map.put("initParam", InitParamExpression.EXPR);
    _map.put("pageContext", PageContextExpression.EXPR);
    _map.put("param", ParamExpression.EXPR);
    _map.put("paramValues", ParamValuesExpression.EXPR);
    _map.put("request", RequestExpression.EXPR);
    _map.put("requestScope", RequestScopeExpression.EXPR);
    _map.put("session", SessionExpression.EXPR);
    _map.put("sessionScope", SessionScopeExpression.EXPR);
  }
}
