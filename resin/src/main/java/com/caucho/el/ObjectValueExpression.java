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
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;
import java.util.logging.Logger;

/**
 * Abstract implementation class for an expression.
 */
public class ObjectValueExpression extends ValueExpression
{
  protected static final Logger log
    = Logger.getLogger(ObjectValueExpression.class.getName());
  protected static final L10N L = new L10N(Expr.class);

  private final Expr _expr;
  private final String _expressionString;
  private final Class _expectedType;

  public ObjectValueExpression(Expr expr,
                               String expressionString,
                               Class<?> expectedType)
  {
    _expr = expr;
    _expressionString = expressionString;
    _expectedType = expectedType;
  }

  public ObjectValueExpression(Expr expr, String expressionString)
  {
    _expr = expr;
    _expressionString = expressionString;
    _expectedType = Object.class;
  }

  public ObjectValueExpression(Expr expr)
  {
    _expr = expr;
    _expressionString = _expr.toString();
    _expectedType = Object.class;
  }

  /**
   * For serialization
   */
  public ObjectValueExpression()
  {
    _expr = null;
    _expressionString = null;
    _expectedType = Object.class;
  }

  public boolean isLiteralText()
  {
    return _expr.isLiteralText();
  }

  public String getExpressionString()
  {
    return _expressionString;
  }

  public Class<?> getExpectedType()
  {
    return _expectedType;
  }

  public Class<?> getType(ELContext context)
    throws PropertyNotFoundException,
           ELException
  {
    if (context == null)
      throw new NullPointerException("context can't be null");

    return _expr.getType(context);
  }

  @Override
  public Object getValue(ELContext context)
    throws PropertyNotFoundException,
           ELException
  {
    if (context == null)
      throw new NullPointerException("context can't be null");

    Object rawValue = _expr.getValue(context);

    Object value = Expr.coerceToType(rawValue, _expectedType);

    return value;
  }

  @Override
  public boolean isReadOnly(ELContext context)
    throws PropertyNotFoundException,
           ELException
  {
    if (context == null)
      throw new NullPointerException("context can't be null");

    return _expr.isReadOnly(context);
  }

  public void setValue(ELContext context, Object value)
    throws PropertyNotFoundException,
           PropertyNotWritableException,
           ELException
  {
    if (context == null)
      throw new NullPointerException("context can't be null");

    _expr.setValue(context, value);
  }

  public int hashCode()
  {
    return _expr.hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof ObjectValueExpression))
      return false;

    ObjectValueExpression expr = (ObjectValueExpression) o;

    return _expr.equals(expr._expr);
  }

  public String toString()
  {
    return "ObjectValueExpression[" + getExpressionString() + "]";
  }
}
