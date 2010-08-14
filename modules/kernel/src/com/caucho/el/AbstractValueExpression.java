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
abstract public class AbstractValueExpression extends ValueExpression
{
  protected static final Logger log
    = Logger.getLogger(AbstractValueExpression.class.getName());
  protected static final L10N L = new L10N(AbstractValueExpression.class);

  protected final Expr _expr;
  private final String _expressionString;

  protected AbstractValueExpression()
  {
    _expr = null;
    _expressionString = "";
  }

  protected AbstractValueExpression(Expr expr,
                                    String expressionString)
  {
    _expr = expr;
    _expressionString = expressionString;
  }

  protected AbstractValueExpression(Expr expr)
  {
    _expr = expr;

    if (_expr != null)
      _expressionString = _expr.toString();
    else
      _expressionString = "";
  }

  public boolean isLiteralText()
  {
    return _expr.isLiteralText();
  }

  public String getExpressionString()
  {
    return _expressionString;
  }

  abstract public Class<?> getExpectedType();

  public Class<?> getType(ELContext context)
    throws PropertyNotFoundException,
           ELException
  {
    if (context == null)
      throw new NullPointerException("context can't be null");
    
    Object value = getValue(context);

    if (value == null)
      return null;
    else
      return value.getClass();
  }

  @Override
  public Object getValue(ELContext context)
    throws PropertyNotFoundException,
           ELException
  {
    if (context == null)
      throw new NullPointerException("context can't be null");

    return _expr.getValue(context);
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
    else if (! (o instanceof AbstractValueExpression))
      return false;

    AbstractValueExpression expr = (AbstractValueExpression) o;

    return _expr.equals(expr._expr);
  }

  public String toString()
  {
    String name = getClass().getName();
    int p = name.lastIndexOf('.');
    name = name.substring(p + 1);
    
    return name + "[" + getExpressionString() + "]";
  }
}
