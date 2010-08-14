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

/**
 * Represents a conditional expression
 */
public class ConditionalExpr extends Expr {
  private Expr _test;
  private Expr _trueExpr;
  private Expr _falseExpr;

  /**
   * Creates the conditional expression.
   *
   * @param test the conditional expressions test.
   * @param trueExpr the true subexpression
   * @param falseExpr the false subexpression
   */
  public ConditionalExpr(Expr test, Expr trueExpr, Expr falseExpr)
  {
    _test = test;
    _trueExpr = trueExpr;
    _falseExpr = falseExpr;
  }

  /**
   * Returns true if this is a constant expression.
   */
  @Override
  public boolean isConstant()
  {
    return (_test.isConstant() &&
            _trueExpr.isConstant() &&
            _falseExpr.isConstant());
  }
  
  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable environment
   *
   * @return the result as an object
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    if (_test.evalBoolean(env))
      return _trueExpr.getValue(env);
    else
      return _falseExpr.getValue(env);
  }
  
  /**
   * Evaluate the expression as a long
   *
   * @param env the variable environment
   *
   * @return the result as an long
   */
  @Override
  public long evalLong(ELContext env)
    throws ELException
  {
    if (_test.evalBoolean(env))
      return _trueExpr.evalLong(env);
    else
      return _falseExpr.evalLong(env);
  }
  
  /**
   * Evaluate the expression as a double
   *
   * @param env the variable environment
   *
   * @return the result as an double
   */
  @Override
  public double evalDouble(ELContext env)
    throws ELException
  {
    if (_test.evalBoolean(env))
      return _trueExpr.evalDouble(env);
    else
      return _falseExpr.evalDouble(env);
  }
  
  /**
   * Evaluate the expression as a string
   *
   * @param env the variable environment
   *
   * @return the result as a string
   */
  @Override
  public String evalString(ELContext env)
    throws ELException
  {
    if (_test.evalBoolean(env))
      return _trueExpr.evalString(env);
    else
      return _falseExpr.evalString(env);
  }
  
  /**
   * Evaluate the expression as a boolean
   *
   * @param env the variable environment
   *
   * @return the result as a boolean
   */
  @Override
  public boolean evalBoolean(ELContext env)
    throws ELException
  {
    if (_test.evalBoolean(env))
      return _trueExpr.evalBoolean(env);
    else
      return _falseExpr.evalBoolean(env);
  }

  /**
   * Prints the Java code to recreate the expr
   *
   * @param os the output stream to the *.java file
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.ConditionalExpr(");
    _test.printCreate(os);
    os.print(", ");
    _trueExpr.printCreate(os);
    os.print(", ");
    _falseExpr.printCreate(os);
    os.print(")");
  }

  /**
   * Returns true for equal strings.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof ConditionalExpr))
      return false;

    ConditionalExpr expr = (ConditionalExpr) o;

    return (_test == expr._test &&
            _trueExpr.equals(expr._trueExpr) &&
            _falseExpr.equals(expr._falseExpr));
  }
  
  /**
   * Returns a readable representation of the expr.
   */
  public String toString()
  {
    return "(" + _test + " ? " + _trueExpr + " : " + _falseExpr + ")";
  }
}
