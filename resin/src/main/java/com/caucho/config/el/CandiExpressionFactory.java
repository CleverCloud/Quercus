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
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;

/**
 * Variable resolution for webbeans variables
 */
public class CandiExpressionFactory extends ExpressionFactory {
  private final ExpressionFactory _factory;
  
  public CandiExpressionFactory(ExpressionFactory factory)
  {
    _factory = factory;
  }

  @Override
  public Object coerceToType(Object obj, Class<?> targetType)
      throws ELException
  {
    return _factory.coerceToType(obj, targetType);
  }

  @Override
  public MethodExpression createMethodExpression(ELContext context,
                                                 String expression,
                                                 Class<?> expectedReturnType,
                                                 Class<?>[] expectedParamTypes)
      throws ELException
  {
    MethodExpression expr 
      = _factory.createMethodExpression(context, expression,
                                        expectedReturnType, expectedParamTypes);
      
    return expr;
  }

  @Override
  public ValueExpression createValueExpression(ELContext context,
                                               String expression,
                                               Class<?> expectedType)
      throws ELException
  {
    ValueExpression expr 
    = _factory.createValueExpression(context, expression, expectedType);
  
    return new CandiValueExpression(expr);
  }

  @Override
  public ValueExpression createValueExpression(Object instance,
                                               Class<?> expectedType)
      throws ELException
  {
    ValueExpression expr
      = _factory.createValueExpression(instance, expectedType);
    
    return new CandiValueExpression(expr);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _factory + "]";
  }
}
