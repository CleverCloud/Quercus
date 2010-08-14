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
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Var;

/**
 * Represents a PHP variable expression.
 */
public class VarVarExpr extends AbstractVarExpr {
  private static final NullValue NULL = NullValue.create();

  protected final Expr _var;

  public VarVarExpr(Location location, Expr var)
  {
    super(location);
    _var = var;
  }

  public VarVarExpr(Expr var)
  {
    _var = var;
  }

  public Expr getExpr()
  {
    return _var;
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
    StringValue varName = _var.evalStringValue(env);

    Value value = env.getValue(varName);

    if (value != null)
      return value;
    else
      return NULL;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value evalAssignRef(Env env, Value value)
  {
    StringValue varName = _var.evalStringValue(env);

    // php/0d63
    env.setRef(varName, value);
    
    return value;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public void evalUnset(Env env)
  {
    StringValue varName = _var.evalStringValue(env);

    env.unsetVar(varName);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Var evalVar(Env env)
  {
    StringValue varName = _var.evalStringValue(env);

    return env.getVar(varName);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value evalArg(Env env, boolean isTop)
  {
    StringValue varName = _var.evalStringValue(env);

    Value value = env.getVar(varName);

    if (value != null)
      return value;
    else
      return NULL;
  }

  /**
   * Evaluates the expression, converting to an array if necessary.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
  {
    StringValue varName = _var.evalStringValue(env);

    Value value = env.getVar(varName);

    if (value != null)
      return value.getArray();
    else {
      ArrayValue array = new ArrayValueImpl();

      env.setRef(varName, array);

      return array;
    }
  }

  public String toString()
  {
    return "$" + _var;
  }
}

