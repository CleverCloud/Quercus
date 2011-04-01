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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.el;

import com.caucho.vfs.WriteStream;

import javax.el.ELContext;
import javax.el.ELException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Represents a unary minus expression.
 */
public class MinusExpr extends Expr {
  private final Expr _expr;

  /**
   * Create a new minus expression.
   *
   * @param op the lexical code for the operation
   * @param expr the base expression
   */
  public MinusExpr(Expr expr)
  {
    _expr = expr;
  }

  /**
   * Returns true if this is a constant expression.
   */
  @Override
  public boolean isConstant()
  {
    return _expr.isConstant();
  }

  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable resolver
   *
   * @return the value as an object
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    Object obj = _expr.getValue(env);

    if (obj == null)
      return new Long(0);

    Class type = obj.getClass();
    if (Long.class == type)
      return new Long(- ((Number) obj).longValue());
    else if (Double.class == type)
      return new Double(- ((Number) obj).doubleValue());
    else if (Integer.class == type)
      return new Integer(- ((Number) obj).intValue());
    else if (Short.class == type)
      return new Short((short) (- ((Number) obj).shortValue()));
    else if (Byte.class == type)
      return new Byte((byte) (- ((Number) obj).byteValue()));
    else if (Float.class == type)
      return new Float(- ((Number) obj).floatValue());
    else if (BigDecimal.class == type)
      return ((BigDecimal) obj).negate();
    else if (BigInteger.class == type)
      return ((BigInteger) obj).negate();
    else if (String.class == type && isDouble(obj))
      return new Double(- toDouble(obj, env));
    else if (String.class == type)
      return new Long(- toLong(obj, env));
    else
      throw new ELException(L.l("Can't convert {0} to number", obj));
  }

  /**
   * Evaluate the expression as a long
   *
   * @param env the variable resolver
   *
   * @return the value as a long
   */
  @Override
  public long evalLong(ELContext env)
    throws ELException
  {
    return - _expr.evalLong(env);
  }

  /**
   * Evaluate the expression as a double
   */
  @Override
  public double evalDouble(ELContext env)
    throws ELException
  {
    return - _expr.evalDouble(env);
  }

  /**
   * Prints the Java code to recreate the UnaryExpr.
   */
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.MinusExpr(");
    _expr.printCreate(os);
    os.print(")");
  }

  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof MinusExpr))
      return false;

    MinusExpr uexpr = (MinusExpr) o;

    return (_expr.equals(uexpr._expr));
  }


  /**
   * Returns a readable representation of the expr.
   */
  public String toString()
  {
    return "-" + _expr;
  }
}
