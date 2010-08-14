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
import java.util.Collection;
import java.util.Map;

/**
 * Represents a unary expression.
 */
public class UnaryExpr extends Expr {
  private int _op;
  private Expr _expr;

  /**
   * Create a new unary expression.
   *
   * @param op the lexical code for the operation
   * @param expr the base expression
   */
  private UnaryExpr(int op, Expr expr)
  {
    _op = op;
    _expr = expr;
  }

  public static Expr create(int op, Expr expr)
  {
    switch (op) {
    case MINUS:
      return new MinusExpr(expr);
    }

    return new UnaryExpr(op, expr);
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
    switch (_op) {
    case NOT:
      return new Boolean(! _expr.evalBoolean(env));

    case MINUS:
    {
      Object obj = _expr.getValue(env);

      if (obj == null)
        return new Long(0);
      else if (obj instanceof Double || obj instanceof Float)
        return new Double(- ((Number) obj).doubleValue());
      else if (obj instanceof Number)
        return new Long(- ((Number) obj).longValue());
      else if (obj instanceof String) {
        String s = (String) obj;

        if (s.indexOf('.') < 0)
          return new Long(- toLong(obj, env));
        else
          return new Double(- toDouble(obj, env));
      }
      else
        return new Double(- toDouble(obj, env));
    }

    case EMPTY:
      if (evalBoolean(env))
        return Boolean.TRUE;
      else
        return Boolean.FALSE;
    }

    throw new UnsupportedOperationException();
  }

  /**
   * Evaluate the expression as a boolean.
   *
   * @param env the variable resolver
   *
   * @return the value as a boolean
   */
  @Override
  public boolean evalBoolean(ELContext env)
    throws ELException
  {
    switch (_op) {
    case NOT:
      return ! _expr.evalBoolean(env);

    case EMPTY:
    {
      Object obj = _expr.getValue(env);

      if (obj == null)
        return true;
      else if (obj instanceof String)
        return "".equals(obj);
      else if (obj instanceof Collection)
        return ((Collection) obj).isEmpty();
      else if (obj instanceof Map)
        return ((Map) obj).isEmpty();
      else if (obj.getClass().isArray())
        return java.lang.reflect.Array.getLength(obj) == 0;
      else
        return false;
    }
    }

    ELException e = new ELException(L.l("can't compare."));

    error(e, env);

    return false;
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
    if (_op != MINUS) {
      ELException e = new ELException(L.l("'not' and 'empty' operations can not be converted to long values."));
      error(e, env);

      return 0;
    }

    // _op == MINUS
    return - _expr.evalLong(env);
  }

  /**
   * Evaluate the expression as a double
   */
  @Override
  public double evalDouble(ELContext env)
    throws ELException
  {
    if (_op != MINUS) {
      ELException e = new ELException(L.l("'not' and 'empty' operations can not be converted to double values."));

      error(e, env);

      return 0;
    }

    // _op == MINUS
    return - _expr.evalDouble(env);
  }

  /**
   * Prints the Java code to recreate the UnaryExpr.
   */
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("com.caucho.el.UnaryExpr.create(");
    os.print(_op + ", ");
    _expr.printCreate(os);
    os.print(")");
  }

  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof UnaryExpr))
      return false;

    UnaryExpr uexpr = (UnaryExpr) o;

    return (_op == uexpr._op && _expr.equals(uexpr._expr));
  }


  /**
   * Returns a readable representation of the expr.
   */
  public String toString()
  {
    String op;

    switch (_op) {
    case MINUS:
      op = " -";
      break;
    case NOT:
      op = " not ";
      break;
    case EMPTY:
      op = " empty ";
      break;
    default:
      op = " unknown(" + _op + ") ";
      break;
    }

    return op + _expr;
  }
}
