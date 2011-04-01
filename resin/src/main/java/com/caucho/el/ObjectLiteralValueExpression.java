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
 * @author Alex Rojkov
 */
package com.caucho.el;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;

/**
 * Wraps object into a value javax.el.ValueExpression, optionally converts
 *
 * @see javax.el.ExpressionFactory#createValueExpression(Object, Class)
 */
public class ObjectLiteralValueExpression extends ValueExpression {

  private Object _object;
  private Class<?> _expectedType;

  public ObjectLiteralValueExpression(Object object, Class<?> expectedType)
  {
    _object = object;
    _expectedType = expectedType;
  }

  public Class<?> getExpectedType()
  {
    return _expectedType;
  }

  public Class<?> getType(ELContext context)
    throws PropertyNotFoundException,
    ELException
  {
    if (_object == null)
      return null;

    return _object.getClass();
  }

  public Object getValue(ELContext context)
    throws PropertyNotFoundException,
    ELException
  {
    return Expr.coerceToType(_object, _expectedType);
  }

  public boolean isReadOnly(ELContext context)
    throws PropertyNotFoundException,
    ELException
  {
    return true;
  }

  public void setValue(ELContext context, Object value)
    throws PropertyNotFoundException,
    PropertyNotWritableException,
    ELException
  {
    throw new PropertyNotWritableException("can't change underlying object");
  }

  @Override
  public String getExpressionString()
  {
    if (_object == null)
      return null;

    return _object.toString();
  }

  @Override
  public boolean isLiteralText()
  {
    return true;
  }

  @Override
  public int hashCode()
  {
    if (_object == null)
      return 0;

    return _object.hashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == this)
      return true;

    if (! (obj instanceof ObjectLiteralValueExpression))
      return false;

    ObjectLiteralValueExpression valExpr = (ObjectLiteralValueExpression) obj;

    if (! _expectedType.equals(valExpr._expectedType))
      return false;

    if (_object == valExpr._object)
      return true;
    else if (_object != null && _object.equals(valExpr._object))
      return true;

    return false;
  }
}
