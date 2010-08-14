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
 * Represents a PHP assignment expression.
 */
public class BinaryAssignRefExpr extends Expr {
  protected final AbstractVarExpr _var;
  protected final Expr _value;

  public BinaryAssignRefExpr(Location location, AbstractVarExpr var, Expr value)
  {
    super(location);
    _var = var;
    _value = value;
  }

  public BinaryAssignRefExpr(AbstractVarExpr var, Expr value)
  {
    _var = var;
    _value = value;
  }

  /**
   * Returns true if a static false value.
   */
  @Override
  public boolean isAssign()
  {
    return true;
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value eval(Env env)
  {
    // value can be a Value or Var
    Value value = _value.evalRef(env);
    
    _var.evalAssignRef(env, value);

    return value.toValue();
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value evalCopy(Env env)
  {
    // value can be a Value or Var
    Value value = _value.evalRef(env);
    
    _var.evalAssignRef(env, value);

    return value.toValue().copy();
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value evalRef(Env env)
  {
    // value can be a Value or Var
    Value value = _value.evalRef(env);
    
    _var.evalAssignRef(env, value);

    return value;
  }

  public String toString()
  {
    return "(" + _var + " =& " + _value + ")";
  }
}

