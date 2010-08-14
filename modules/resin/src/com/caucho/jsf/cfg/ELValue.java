/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.cfg;

import java.lang.reflect.*;
import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.validator.*;

import com.caucho.config.*;
import com.caucho.config.types.*;
import com.caucho.jsf.el.*;
import com.caucho.util.*;

public class ELValue implements AbstractValue
{
  private static final L10N L = new L10N(ELValue.class);
  
  private static final HashMap<String,Integer> _implicitMap
    = new HashMap<String,Integer>();

  private final String _exprString;
  private final Class _type;
  
  private ValueExpression _expr;

  private int _scope = Integer.MAX_VALUE;

  ELValue(String exprString, Class type)
  {
    _exprString = exprString;
    _type = type;
  }
  
  public Object getValue(FacesContext context)
  {
    ELContext elContext = context.getELContext();
    
    if (_expr == null) {
      Application app = context.getApplication();
      ExpressionFactory factory = app.getExpressionFactory();

      factory.createValueExpression(new ScopeELContext(elContext),
                                    _exprString, _type);

      _expr = factory.createValueExpression(elContext, _exprString, _type);
    }

    if (_scope < Integer.MAX_VALUE) {
      ManagedBeanELResolver.Scope scope;
      scope = (ManagedBeanELResolver.Scope) elContext.getContext(ManagedBeanELResolver.Scope.class);

      if (scope != null && scope.getScope() < _scope)
        throw new ELException(L.l("implicit scope is too short."));
    }

    Object value = _expr.getValue(elContext);
    
    if (elContext.isPropertyResolved())
      return value;
    else
      return null;
  }

  class ScopeELContext extends ELContext
  {
    private ELContext _elContext;
  
    ScopeELContext(ELContext elContext)
    {
      _elContext = elContext;
    }
    
    public ELResolver getELResolver()
    {
      return _elContext.getELResolver();
    }

    public javax.el.FunctionMapper getFunctionMapper()
    {
      return _elContext.getFunctionMapper();
    }

    public javax.el.VariableMapper getVariableMapper()
    {
      return new ImplicitVariableMapper();
    }
  }

  class ImplicitVariableMapper extends VariableMapper {
    public ValueExpression resolveVariable(String variable)
    {
      Integer objValue = _implicitMap.get(variable);

      if (objValue != null) {
        int value = objValue;

        if (value < _scope)
          _scope = value;
      }
      
      return null;
    }
  
    public ValueExpression setVariable(String variable,
                                       ValueExpression expr)
    {
      return expr;
    }
  }

  static {
    _implicitMap.put("application", 1);
    _implicitMap.put("applicationScope", 1);
      
    _implicitMap.put("session", 2);
    _implicitMap.put("sessionScope", 2);
      
    _implicitMap.put("request", 3);
    _implicitMap.put("requestScope", 3);
    _implicitMap.put("view", 3);
    _implicitMap.put("cookie", 3);
    _implicitMap.put("param", 3);
    _implicitMap.put("paramValues", 3);
    _implicitMap.put("header", 3);
    _implicitMap.put("headerValues", 3);
  }
}
