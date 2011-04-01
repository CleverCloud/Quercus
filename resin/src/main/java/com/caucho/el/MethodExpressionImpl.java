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

package com.caucho.el;

import com.caucho.util.L10N;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.MethodExpression;
import javax.el.MethodInfo;
import javax.el.MethodNotFoundException;
import javax.el.PropertyNotFoundException;
import java.util.logging.Logger;

/**
 * Implementation of the method expression.
 */
public class MethodExpressionImpl extends MethodExpression
  implements java.io.Serializable
{
  protected static final Logger log
    = Logger.getLogger(MethodExpressionImpl.class.getName());
  protected static final L10N L = new L10N(MethodExpressionImpl.class);

  private final String _expressionString;
  private final Expr _expr;
  private final Class _expectedType;
  private final Class []_expectedArgs;

  // XXX: for serialization
  public MethodExpressionImpl()
  {
    _expressionString = "";
    _expr = null;
    _expectedType = null;
    _expectedArgs = null;
  }

  public MethodExpressionImpl(Expr expr,
                              String expressionString,
                              Class<?> expectedType,
                              Class<?> []expectedArgs)
  {
    if (expectedArgs == null)
      throw new NullPointerException();

    _expr = expr;
    _expressionString = expressionString;
    _expectedType = expectedType;
    _expectedArgs = expectedArgs;
  }

  public boolean isLiteralText()
  {
    return _expr.isLiteralText();
  }

  public String getExpressionString()
  {
    return _expressionString;
  }
  
  public MethodInfo getMethodInfo(ELContext context)
    throws PropertyNotFoundException,
           MethodNotFoundException,
           ELException
  {
    return _expr.getMethodInfo(context, _expectedType, _expectedArgs);
  }

  public Object invoke(ELContext context,
                       Object []params)
    throws PropertyNotFoundException,
           MethodNotFoundException,
           ELException
  {
    if (params == null && _expectedArgs.length != 0
        || params != null && params.length != _expectedArgs.length) {
      throw new IllegalArgumentException(L.l("'{0}' expected arguments ({1}) do not match actual arguments ({2})", _expr.toString(),
                                             _expectedArgs.length,
                                             (params != null ? params.length : 0)));
    }

    if (void.class.equals(_expectedType ) && _expr.isLiteralText()) {
      throw new ELException("String literal can not be coerced to void");
    }
    
    Object value = _expr.invoke(context, _expectedArgs, params);
      
    return Expr.coerceToType(value, _expectedType);
  }

  public int hashCode()
  {
    return _expr.hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof MethodExpressionImpl))
      return false;

    MethodExpressionImpl expr = (MethodExpressionImpl) o;

    return _expr.equals(expr._expr);
  }

  public String toString()
  {
    return getClass().getName() + "[" + getExpressionString() + "]";
  }
}
