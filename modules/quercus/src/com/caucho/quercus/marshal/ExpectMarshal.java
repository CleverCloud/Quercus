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

package com.caucho.quercus.marshal;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.UnexpectedValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;

public class ExpectMarshal extends Marshal
{
  protected enum Type {
    STRING, NUMERIC, BOOLEAN
  }

  private Type _type;
  
  public static final Marshal MARSHAL_EXPECT_STRING
    = new ExpectMarshal(Type.STRING);
  public static final Marshal MARSHAL_EXPECT_NUMERIC
    = new ExpectMarshal(Type.NUMERIC);
  public static final Marshal MARSHAL_EXPECT_BOOLEAN
    = new ExpectMarshal(Type.BOOLEAN);
  
  protected ExpectMarshal(Type type)
  {
    _type = type;
  }
  
  protected Value expect(Env env, Value value)
  {
    if (_type == Type.STRING)
      return env.expectString(value);
    else if (_type == Type.NUMERIC)
      return env.expectNumeric(value);
    else
      return env.expectBoolean(value);
  }

  public boolean isReadOnly()
  {
    return false;
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
    return expect(env, expr.eval(env));
  }

  public Object marshal(Env env, Value value, Class expectedClass)
  {
    return expect(env, value.toValue());
  }

  public Value unmarshal(Env env, Object value)
  {
    return (Value) value;
  }

  @Override
  protected int getMarshalingCostImpl(Value argValue)
  {
    return Marshal.COST_VALUE;
  }

  @Override
  public Class getExpectedClass()
  {
    return Value.class;
  }
}
