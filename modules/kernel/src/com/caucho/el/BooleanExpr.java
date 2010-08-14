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

import com.caucho.vfs.WriteStream;

import javax.el.ELContext;
import javax.el.ELException;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Represents a binary boolean expression.
 */
public class BooleanExpr extends AbstractBooleanExpr {
  private int _op;
  private Expr _left;
  private Expr _right;

  /**
   * Constructs a new Boolean expression.
   *
   * @param op the lexeme code for the boolean operation.
   * @param left the left expression
   * @param right the right expression
   */
  public BooleanExpr(int op, Expr left, Expr right)
  {
    _op = op;
    _left = left;
    _right = right;
  }

  /**
   * Returns true if this is a constant expression.
   */
  @Override
  public boolean isConstant()
  {
    return _left.isConstant() && _right.isConstant();
  }
  
  /**
   * Evaluate the expression as a boolean.
   *
   * @param env the variable environment
   *
   * @return the result as a boolean
   */
  @Override
  public boolean evalBoolean(ELContext env)
    throws ELException
  {
    if (_op == AND)
      return _left.evalBoolean(env) && _right.evalBoolean(env);
    else if (_op == OR)
      return _left.evalBoolean(env) || _right.evalBoolean(env);

    ELException e = new ELException(L.l("can't compare."));
    log.log(Level.FINE, e.getMessage(), e);

    return false;
  }

  /**
   * Prints the Java code to recreate a BooleanExpr
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.BooleanExpr(");
    os.print(_op + ", ");
    _left.printCreate(os);
    os.print(", ");
    _right.printCreate(os);
    os.print(")");
  }

  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof BooleanExpr))
      return false;

    BooleanExpr expr = (BooleanExpr) o;

    return (_op == expr._op &&
            _left.equals(expr._left) &&
            _right.equals(expr._right));
  }

  /**
   * Returns a readable representation of the expr.
   */
  public String toString()
  {
    String op;

    switch (_op) {
    case OR:
      op = " or ";
      break;
    case AND:
      op = " and ";
      break;
    default:
      op = " unknown(" + _op + ") ";
      break;
    }
        
    return "(" + _left + op + _right + ")";
  }
}
