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

import com.caucho.el.*;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

/**
 * Represents an EL expression factory
 */
public class JsfExpressionFactoryImpl extends ExpressionFactory {
  private static final HashMap<Class,CoerceType> _coerceMap
    = new HashMap<Class,CoerceType>();

  public JsfExpressionFactoryImpl()
  {
  }
  
  public Object coerceToType(Object obj, Class<?> targetType)
    throws ELException
  {
    CoerceType type = _coerceMap.get(targetType);

    if (type == null)
      return obj;

    switch (type) {
    case BOOLEAN:
      return Expr.toBoolean(obj, null) ? Boolean.FALSE : Boolean.TRUE;
    case CHARACTER:
      return Expr.toCharacter(obj, null);
    case BYTE:
      return new Byte((byte) Expr.toLong(obj, null));
    case SHORT:
      return new Short((short) Expr.toLong(obj, null));
    case INTEGER:
      return new Integer((int) Expr.toLong(obj, null));
    case LONG:
      return new Long(Expr.toLong(obj, null));
    case FLOAT:
      return new Float((float) Expr.toDouble(obj, null));
    case DOUBLE:
      return new Double(Expr.toDouble(obj, null));
    case STRING:
      if (obj == null)
        return "";
      else
        return obj.toString();
    case BIG_DECIMAL:
      return Expr.toBigDecimal(obj, null);
    case BIG_INTEGER:
      return Expr.toBigInteger(obj, null);
    }

    return null;
  }

  public MethodExpression
    createMethodExpression(ELContext context,
                           String expression,
                           Class<?> expectedReturnType,
                           Class<?>[] expectedParamTypes)
    throws ELException
  {
    JsfELParser parser = new JsfELParser(context, expression);

    Expr expr = parser.parse();

    return new MethodExpressionImpl(expr, expression,
                                    expectedReturnType,
                                    expectedParamTypes);
  }

  public ValueExpression
    createValueExpression(ELContext context,
                          String expression,
                          Class<?> expectedType)
    throws ELException
  {
    JsfELParser parser = new JsfELParser(context, expression);

    Expr expr = parser.parse();

    return createValueExpression(expr, expression, expectedType);
  }

  public static ValueExpression createValueExpression(Expr expr,
                                                      String expression,
                                                      Class<?> expectedType)
  {
    CoerceType type = _coerceMap.get(expectedType);

    if (type == null)
      return new ObjectValueExpression(expr, expression, expectedType);

    switch (type) {
    case BOOLEAN:
      return new BooleanValueExpression(expr, expression);
    case CHARACTER:
      return new CharacterValueExpression(expr, expression);
    case BYTE:
      return new ByteValueExpression(expr, expression);
    case SHORT:
      return new ShortValueExpression(expr, expression);
    case INTEGER:
      return new IntegerValueExpression(expr, expression);
    case LONG:
      return new LongValueExpression(expr, expression);
    case FLOAT:
      return new FloatValueExpression(expr, expression);
    case DOUBLE:
      return new DoubleValueExpression(expr, expression);
    case STRING:
      return new StringValueExpression(expr, expression);
    case BIG_DECIMAL:
      return new BigDecimalValueExpression(expr, expression);
    case BIG_INTEGER:
      return new BigIntegerValueExpression(expr, expression);
    }

    return new ObjectValueExpression(expr, expression);
  }

  public ValueExpression
    createValueExpression(Object instance,
                          Class<?> expectedType)
    throws ELException
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "JsfExpressionFactoryImpl[]";
  }

  private enum CoerceType {
    BOOLEAN,
    CHARACTER,
    STRING,
    INTEGER,
    DOUBLE,
    LONG,
    FLOAT,
    SHORT,
    BYTE,
    BIG_INTEGER,
    BIG_DECIMAL,
    VOID
  };

  static {
    _coerceMap.put(boolean.class, CoerceType.BOOLEAN);
    _coerceMap.put(Boolean.class, CoerceType.BOOLEAN);
    
    _coerceMap.put(byte.class, CoerceType.BYTE);
    _coerceMap.put(Byte.class, CoerceType.BYTE);
    
    _coerceMap.put(short.class, CoerceType.SHORT);
    _coerceMap.put(Short.class, CoerceType.SHORT);
    
    _coerceMap.put(int.class, CoerceType.INTEGER);
    _coerceMap.put(Integer.class, CoerceType.INTEGER);
    
    _coerceMap.put(long.class, CoerceType.LONG);
    _coerceMap.put(Long.class, CoerceType.LONG);
    
    _coerceMap.put(float.class, CoerceType.FLOAT);
    _coerceMap.put(Float.class, CoerceType.FLOAT);
    
    _coerceMap.put(double.class, CoerceType.DOUBLE);
    _coerceMap.put(Double.class, CoerceType.DOUBLE);
    
    _coerceMap.put(char.class, CoerceType.CHARACTER);
    _coerceMap.put(Character.class, CoerceType.CHARACTER);
    
    _coerceMap.put(String.class, CoerceType.STRING);
    
    _coerceMap.put(BigDecimal.class, CoerceType.BIG_DECIMAL);
    _coerceMap.put(BigInteger.class, CoerceType.BIG_INTEGER);
    
    _coerceMap.put(void.class, CoerceType.VOID);
  }
}
