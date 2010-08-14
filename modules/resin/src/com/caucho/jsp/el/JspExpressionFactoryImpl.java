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

package com.caucho.jsp.el;

import com.caucho.el.ExpressionFactoryImpl;
import com.caucho.inject.Module;
import com.caucho.server.webapp.WebApp;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;

/**
 * Represents an EL expression factory
 */
@Module class JspExpressionFactoryImpl extends ExpressionFactory {

  private final JspApplicationContextImpl _jspApplicationContext;
  private final ExpressionFactory _factory;

  public JspExpressionFactoryImpl(JspApplicationContextImpl jspApplicationContext)
  {
    _jspApplicationContext = jspApplicationContext;
    // _factory = ExpressionFactory.newInstance();
    _factory = new ExpressionFactoryImpl();
  }

  public Object coerceToType(Object obj, Class<?> targetType)
    throws ELException
  {
    return _factory.coerceToType(obj, targetType);
  }

  public MethodExpression
  createMethodExpression(ELContext context,
                         String expression,
                         Class<?> expectedReturnType,
                         Class<?>[] expectedParamTypes)
    throws ELException
  {
    return _factory.createMethodExpression(context,
                                           expression,
                                           expectedReturnType,
                                           expectedParamTypes);
  }

  public ValueExpression
  createValueExpression(ELContext context,
                        String expression,
                        Class<?> expectedType)
    throws ELException
  {
    return _factory.createValueExpression(context, expression, expectedType);
  }

  public ValueExpression
  createValueExpression(Object instance,
                        Class<?> expectedType)
    throws ELException
  {
    return _factory.createValueExpression(instance, expectedType);
  }

  public String toString()
  {
    WebApp webApp = null;
    if (_jspApplicationContext != null)
      webApp = _jspApplicationContext.getWebApp();

    return "JspExpressionFactoryImpl["
      + webApp
      + ", "
      + _factory.getClass().getName()
      + "]";
  }
}
