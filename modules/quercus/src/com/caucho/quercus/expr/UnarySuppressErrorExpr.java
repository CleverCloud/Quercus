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
import com.caucho.quercus.parser.QuercusParser;

import java.io.IOException;

/**
 * Represents a PHP error suppression
 */
public class UnarySuppressErrorExpr extends AbstractUnaryExpr {
  public UnarySuppressErrorExpr(Location location, Expr expr)
  {
    super(location, expr);
  }

  public UnarySuppressErrorExpr(Expr expr)
  {
    super(expr);
  }

  /**
   * Creates the assignment.
   */
  public Expr createAssign(QuercusParser parser, Expr value)
    throws IOException
  {
    // php/03j2

    return new UnarySuppressErrorExpr(parser.getLocation(),
                                 getExpr().createAssign(parser, value));
  }

  /**
   * Creates the assignment.
   */
  public Expr createAssignRef(QuercusParser parser,
                              Expr value
  )
    throws IOException
  {
    // php/03j2

    return new UnarySuppressErrorExpr(parser.getLocation(),
                                 getExpr().createAssignRef(parser, value));
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
    int oldErrorMask = env.setErrorMask(0);

    try {
      return _expr.eval(env);
    } finally {
      env.setErrorMask(oldErrorMask);
    }
  }

  /**
   * Evaluates the expression as a boolean.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public boolean evalBoolean(Env env)
  {
    int oldErrorMask = env.setErrorMask(0);

    try {
      return _expr.evalBoolean(env);
    } finally {
      env.setErrorMask(oldErrorMask);
    }
  }

  /**
   * Evaluates the expression as a string.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public String evalString(Env env)
  {
    int oldErrorMask = env.setErrorMask(0);

    try {
      return _expr.evalString(env);
    } finally {
      env.setErrorMask(oldErrorMask);
    }
  }

  /**
   * Evaluates the expression, copying the results as necessary
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalCopy(Env env)
  {
    int oldErrorMask = env.setErrorMask(0);

    try {
      return _expr.evalCopy(env);
    } finally {
      env.setErrorMask(oldErrorMask);
    }
  }

  public String toString()
  {
    return "@" + _expr;
  }
}

