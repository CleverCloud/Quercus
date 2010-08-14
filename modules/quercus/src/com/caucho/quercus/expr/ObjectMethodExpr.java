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
import com.caucho.quercus.env.MethodIntern;
import com.caucho.quercus.env.MethodMap;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.StringValue;
import com.caucho.util.L10N;

import java.util.ArrayList;

/**
 * Represents a PHP function expression.
 */
public class ObjectMethodExpr extends AbstractMethodExpr {
  private static final L10N L = new L10N(ObjectMethodExpr.class);

  protected final Expr _objExpr;
  
  protected final StringValue _methodName;
  
  protected final Expr []_args;

  public ObjectMethodExpr(Location location,
                          Expr objExpr,
                          String name,
                          ArrayList<Expr> args)
  {
    super(location);
    
    _objExpr = objExpr;
    
    _methodName = MethodIntern.intern(name);

    _args = new Expr[args.size()];
    args.toArray(_args);
  }

  public ObjectMethodExpr(Expr objExpr, String name, ArrayList<Expr> args)
  {
    this(Location.UNKNOWN, objExpr, name, args);
  }

  public String getName()
  {
    return _methodName.toString();
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
    env.checkTimeout();

    Value obj = _objExpr.eval(env);
    
    StringValue methodName = _methodName;
    int hash = methodName.hashCodeCaseInsensitive();
    
    return eval(env, obj, methodName, hash, _args);
  }
  
  public String toString()
  {
    return _objExpr + "->" + _methodName + "()";
  }
}

