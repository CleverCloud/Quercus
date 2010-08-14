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

package com.caucho.quercus.marshal;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;

public class JavaArrayMarshal extends Marshal
{
  private Class _expectedClass;
  
  public JavaArrayMarshal()
  {
    _expectedClass = Object[].class;
  }
  
  public JavaArrayMarshal(Class expectedClass)
  {
    _expectedClass = expectedClass;
  }
  
  public Object marshal(Env env, Expr expr, Class expectedClass)
  {
    return marshal(env, expr.eval(env), expectedClass);
  }

  public Object marshal(Env env, Value value, Class expectedClass)
  {
    /*
    if (! value.isset()) {
      if (_isNotNull) {
        env.warning(L.l(
          "null is an unexpected argument, expected {0}",
          shortName(expectedClass)));
      }

      return null;
    }
    */

    Class<?> componentType = expectedClass.getComponentType();
    Object array = value.valuesToArray(env, componentType);
    /*
    if (array == null && _isNotNull) {
      env.warning(L.l(
        "null is an unexpected argument, expected {0}",
        shortName(expectedClass)));
    }
    */
    
    return array;
  }

  public Value unmarshal(Env env, Object value)
  {
    return env.wrapJava(value);
  }
  
  @Override
  protected int getMarshalingCostImpl(Value argValue)
  {
    if (argValue.isArray()) {
      if (Value[].class.equals(_expectedClass)
          || Object[].class.equals(_expectedClass))
        return Marshal.ONE;
      else
        return Marshal.THREE;
    }
    else
      return Marshal.FOUR;
  }
  
  @Override
  public Class getExpectedClass()
  {
    return _expectedClass;
  }
}
