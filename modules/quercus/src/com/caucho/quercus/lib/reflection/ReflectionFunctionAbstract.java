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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.reflection;

import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.expr.ParamRequiredExpr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.Arg;

public abstract class ReflectionFunctionAbstract
{
  private AbstractFunction _fun;
  
  protected ReflectionFunctionAbstract(AbstractFunction fun)
  {
    _fun = fun;
    
    if (fun == null)
      throw new NullPointerException();
  }
  
  protected AbstractFunction getFunction()
  {
    return _fun;
  }
  
  private void __clone()
  {
  }
  
  public String getName()
  {
    return _fun.getName();
  }
    
  public boolean isInternal()
  {
    return false;
  }
  
  public boolean isUserDefined()
  {
    return false;
  }
  
  public String getFileName()
  {
    return _fun.getLocation().getFileName();
  }
  
  public int getStartLine()
  {
    return _fun.getLocation().getLineNumber();
  }
  
  public int getEndLine()
  {
    // TODO
    return _fun.getLocation().getLineNumber();
  }
  
  @ReturnNullAsFalse
  public String getDocComment()
  {
    return _fun.getComment();
  }
  
  public ArrayValue getStaticVariables()
  {
    // TODO
    return null; 
  }
  
  public boolean returnsReference()
  {
    return _fun.isReturnsReference();
  }
  
  public ArrayValue getParameters(Env env)
  {
    ArrayValue array = new ArrayValueImpl();
    
    Arg []args = _fun.getArgs();
    
    for (int i = 0; i < args.length; i++) {
      array.put(env.wrapJava(new ReflectionParameter(_fun, args[i])));
    }
    
    return array;
  }
  
  public int getNumberOfParameters()
  {
    return _fun.getArgs().length;
  }
  
  public int getNumberOfRequiredParameters()
  {
    Arg []args = _fun.getArgs();
    
    int requiredParams = 0;
    for (int i = 0; i < args.length; i++) {
      if (args[i].getDefault() instanceof ParamRequiredExpr)
        requiredParams++;
    }
    
    return requiredParams;
  }
}
