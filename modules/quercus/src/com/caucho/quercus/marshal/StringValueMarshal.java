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
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;

public class StringValueMarshal extends Marshal
{
  public static final Marshal MARSHAL = new StringValueMarshal();

  public boolean isReadOnly()
  {
    return true;
  }

  /**
   * Return true if is a Value.
   */
  @Override
  public boolean isValue()
  {
    return true;
  }

  public Object marshal(Env env, Expr expr, Class expectedClass)
  {
    return expr.eval(env).toStringValue(env);
  }

  public Object marshal(Env env, Value value, Class expectedClass)
  {
    return value.toStringValue(env);
  }

  public Value unmarshal(Env env, Object value)
  {
    if (value instanceof StringValue)
      return (StringValue) value;
    else if (value instanceof Value)
      return ((Value) value).toStringValue(env);
    else if (value != null)
      return env.createString(String.valueOf(value));
    else
      return null;
  }

  @Override
  protected int getMarshalingCostImpl(Value argValue)
  {
    return argValue.toStringValueMarshalCost();

    /*
    if (argValue.isString()) {
      if (argValue.isUnicode())
        return Marshal.UNICODE_STRING_VALUE_COST;
      else if (argValue.isBinary())
        return Marshal.BINARY_STRING_VALUE_COST;
      else
        return Marshal.PHP5_STRING_VALUE_COST;
    }
    else if (! (argValue.isArray() || argValue.isObject()))
      return Marshal.THREE;
    else
      return Marshal.FOUR;
    */
  }

  public int getMarshalingCost(Expr expr)
  {
    if (expr.isString())
      return Marshal.ZERO;
    else
      return Marshal.FOUR;
  }

  @Override
  public Class getExpectedClass()
  {
    return StringValue.class;
  }
}
