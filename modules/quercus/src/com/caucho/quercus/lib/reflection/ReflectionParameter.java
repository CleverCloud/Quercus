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

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.expr.ParamRequiredExpr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.Arg;
import com.caucho.util.L10N;

public class ReflectionParameter
  implements Reflector
{
  private static final L10N L = new L10N(ReflectionParameter.class);
  
  private String _clsName;
  private AbstractFunction _fun;
  private Arg _arg;
  
  protected ReflectionParameter(AbstractFunction fun, Arg arg)
  {
    _fun = fun;
    _arg = arg;
  }
  
  protected ReflectionParameter(String clsName,
                                AbstractFunction fun,
                                Arg arg)
  {
    this(fun, arg);
    
    _clsName = clsName;
  }
  
  final private void __clone()
  {
  }
  
  public static ReflectionParameter __construct(Env env,
                                                String funName,
                                                StringValue paramName)
  {
    AbstractFunction fun = env.findFunction(funName);
    
    Arg []args = fun.getArgs();
    
    for (int i = 0; i < args.length; i++) {
      if (args[i].getName().equals(paramName))
        return new ReflectionParameter(fun, args[i]);
    }
    
    throw new ReflectionException(
        L.l("cannot find parameter '{0}'", paramName));
  }
  
  public static String export(Env env,
                              Value function,
                              Value parameter,
                              boolean isReturn)
  {
    return null;
  }
  
  public StringValue getName()
  {
    return _arg.getName();
  }
  
  public boolean isPassedByReference()
  {
    return _arg.isReference();
  }
  
  public ReflectionClass getDeclaringClass(Env env)
  {
    if (_clsName != null) {
      QuercusClass cls = env.findClass(_clsName);
      QuercusClass parent = cls.getParent();
      
      if (parent == null || parent.findFunction(_fun.getName()) != _fun)
        return new ReflectionClass(cls);
      else
        return getDeclaringClass(env, parent);
    }
    else
      return null;
  }
  
  private ReflectionClass getDeclaringClass(Env env, QuercusClass cls)
  {
    if (cls == null)
      return null;
    
    ReflectionClass refClass = getDeclaringClass(env, cls.getParent());
    
    if (refClass != null)
      return refClass;
    else if (cls.findFunction(_fun.getName()) != null)
      return new ReflectionClass(cls);
    else
      return null;
  }
  
  public ReflectionClass getClass(Env env)
  {
    return null;
  }
  
  public boolean isArray()
  {
    return false;
  }
  
  public boolean allowsNull()
  {
    return false;
  }
  
  public boolean isOptional()
  {
    return ! (_arg.getDefault() instanceof ParamRequiredExpr);
  }
  
  public boolean isDefaultValueAvailable()
  {
    return isOptional();
  }
  
  public Value getDefaultValue(Env env)
  {
    //XXX: more specific exception
    if (! isOptional())
      throw new ReflectionException(
          L.l("parameter '{0}' is not optional", _arg.getName()));
    
    return _arg.getDefault().eval(env);
  }
  
  public String toString()
  {
    return "ReflectionParameter["
        + _fun.getName() + "(" + _arg.getName() + ")]";
  }
}
