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

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

/**
 * Represents a conditional expression.
 */
public class ConditionalExpr extends Expr {
  protected final Expr _test;
  protected final Expr _trueExpr;
  protected final Expr _falseExpr;

  public ConditionalExpr(Location location,
                         Expr test,
                         Expr trueExpr,
                         Expr falseExpr)
  {
    super(location);
    _test = test;

    _trueExpr = trueExpr;
    _falseExpr = falseExpr;
  }

  public ConditionalExpr(Expr test, Expr trueExpr, Expr falseExpr)
  {
    _test = test;

    _trueExpr = trueExpr;
    _falseExpr = falseExpr;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
  {
    if (_test.evalBoolean(env))
      return _trueExpr.eval(env);
    else
      return _falseExpr.eval(env);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public boolean evalBoolean(Env env)
  {
    if (_test.evalBoolean(env))
      return _trueExpr.evalBoolean(env);
    else
      return _falseExpr.evalBoolean(env);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalCopy(Env env)
  {
    if (_test.evalBoolean(env))
      return _trueExpr.evalCopy(env);
    else
      return _falseExpr.evalCopy(env);
  }

  public String toString()
  {
    return "(" + _test + " ? " + _trueExpr + " : " + _falseExpr + ")";
  }
}

