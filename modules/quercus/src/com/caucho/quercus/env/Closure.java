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

package com.caucho.quercus.env;

import com.caucho.quercus.program.Arg;
import com.caucho.quercus.program.Function;

/**
 * Represents a call to a function.
 */
public class Closure extends Callback {
  private static final Value []NULL_ARGS = new Value[0];
  
  private static final StringValue INVOKE = MethodIntern.intern("__invoke");
  
  private Function _fun;
  private Value []_args;

  public Closure(Env env, Function fun)
  {
    _fun = fun;
    
    Arg []args = fun.getClosureUseArgs();
    if (args != null && args.length > 0) {
      _args = new Value[args.length];
      
      for (int i = 0; i < args.length; i++) {
        Arg arg = args[i];
        
        if (arg.isReference())
          _args[i] = env.getRef(arg.getName());
        else
          _args[i] = env.getValue(arg.getName());
      }
    }
  }
  
  public boolean isCallable(Env env, boolean isSyntax)
  {
    return true;
  }
  
  @Override
  public Callable toCallable(Env env)
  {
    return this;
  }
  
  @Override
  public boolean isObject()
  {
    return true;    
  }
  
  @Override
  public String getType()
  {
    return "object";
  }
  
  @Override
  public Value call(Env env, Value []args)
  {
    return _fun.callImpl(env, args, false, _fun.getClosureUseArgs(), _args);
  }

  @Override
  public String getCallbackName()
  {
    return _fun.getName();
  }

  @Override
  public boolean isInternal(Env env)
  {
    return false;
  }

  @Override
  public boolean isValid(Env env)
  {
    return true;
  }
  
  //
  // special methods
  //
  
  @Override
  public Value callMethod(Env env, 
                          StringValue methodName, int hash, 
                          Value []args)
  {
    if (methodName == INVOKE || INVOKE.equals(methodName))
      return call(env, args);
    else
      return super.callMethod(env, methodName, hash, args);
  }

  public String toString()
  {
    return "Closure[" + _fun.getName() + "]";
  }
}

