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

package com.caucho.config.el;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.el.ValueExpression;

import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.OwnerCreationalContext;

/**
 * Variable resolution for webbeans variables
 */
@SuppressWarnings("serial")
public class CandiValueExpression extends ValueExpression {
  private final ValueExpression _expr;
  
  public CandiValueExpression(ValueExpression expr)
  {
    _expr = expr;
  }

  @Override
  public Class<?> getExpectedType()
  {
    return _expr.getExpectedType();
  }

  @Override
  public Class<?> getType(ELContext context)
    throws PropertyNotFoundException,
           ELException
  {
    return _expr.getType(context);
  }

  @Override
  public Object getValue(ELContext context) throws PropertyNotFoundException,
      ELException
  {
    CandiConfigResolver.startContext();
    
    try {
      return _expr.getValue(context);
    } finally {
      CandiConfigResolver.finishContext();
    }
  }

  @Override
  public boolean isReadOnly(ELContext context)
      throws PropertyNotFoundException, ELException
  {
    return _expr.isReadOnly(context);
  }

  @Override
  public void setValue(ELContext context, Object value)
      throws PropertyNotFoundException, PropertyNotWritableException,
      ELException
  {
    _expr.setValue(context, value);
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof CandiValueExpression) {
      CandiValueExpression expr = (CandiValueExpression) obj;
      
      return _expr.equals(expr._expr);
    }
    else
      return _expr.equals(obj);
  }

  @Override
  public String getExpressionString()
  {
    return _expr.getExpressionString();
  }

  @Override
  public int hashCode()
  {
    return _expr.hashCode();
  }

  @Override
  public boolean isLiteralText()
  {
    return _expr.isLiteralText();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getExpressionString() + "]";
  }
}
