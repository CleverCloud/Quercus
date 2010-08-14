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

import java.io.IOException;
import java.util.ArrayList;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Var;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.util.L10N;

/**
 * Represents a PHP field reference.
 */
public class ThisFieldExpr extends AbstractVarExpr {
  private static final L10N L = new L10N(ThisFieldExpr.class);

  protected final ThisExpr _qThis;
  
  protected final StringValue _name;

  public ThisFieldExpr(ThisExpr qThis, StringValue name)
  {
    _qThis = qThis;
    _name = name;
  }

  private Value cannotUseThisError(Env env)
  {
    return env.error(getLocation(),
                     "Cannot use '$this' when not in object context.");
  }
  
  //
  // function call creation
  //

  /**
   * Creates a function call expression
   */
  @Override
  public Expr createCall(QuercusParser parser,
                         Location location,
                         ArrayList<Expr> args)
    throws IOException
  {
    ExprFactory factory = parser.getExprFactory();
    
    return factory.createThisMethod(location, _qThis, _name.toString(), args);
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
    Value obj = env.getThis();

    if (obj.isNull())
      return cannotUseThisError(env);
    
    return obj.getThisField(env, _name);
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
    Value obj = env.getThis();

    if (obj.isNull())
      return cannotUseThisError(env);
    
    return obj.getThisField(env, _name).copy();
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
    Value obj = env.getThis();

    if (obj.isNull()) {
      cannotUseThisError(env);
      
      return new Var();
    }
    
    return obj.getThisFieldVar(env, _name);
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
    Value obj = env.getThis();

    if (obj.isNull())
      return cannotUseThisError(env);
    
    return obj.getThisFieldArg(env, _name);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value evalAssignValue(Env env, Value value)
  {
    Value obj = env.getThis();

    if (obj.isNull())
      cannotUseThisError(env);
    
    obj.putThisField(env, _name, value);
    
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
  public Value evalAssignRef(Env env, Value value)
  {
    Value obj = env.getThis();

    if (obj.isNull())
      cannotUseThisError(env);
    
    obj.putThisField(env, _name, value);
    
    return value;
  }
  
  /**
   * Evaluates as an array index assign ($a[index] = value).
   */
  @Override
  public Value evalArrayAssign(Env env, Value index, Value value)
  {
    Value obj = env.getThis();

    if (obj.isNull())
      cannotUseThisError(env);
    
    Value fieldVar = obj.getThisFieldVar(env, _name);

    // php/03mm
    return fieldVar.put(index, value);
  }

  /**
   * Evaluates the expression, creating an array if the value is unset..
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
  {
    Value obj = env.getThis();

    if (obj.isNull())
      return cannotUseThisError(env);
    
    return obj.getThisFieldArray(env, _name);
  }

  /**
   * Evaluates the expression, creating an array if the value is unset..
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalObject(Env env)
  {
    Value obj = env.getThis();

    if (obj.isNull())
      return cannotUseThisError(env);
    
    return obj.getThisFieldObject(env, _name);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalUnset(Env env)
  {
    Value obj = env.getThis();

    if (obj.isNull())
      cannotUseThisError(env);
    
    obj.unsetThisField(_name);
  }
  
  public String toString()
  {
    return "$this->" + _name;
  }
}

