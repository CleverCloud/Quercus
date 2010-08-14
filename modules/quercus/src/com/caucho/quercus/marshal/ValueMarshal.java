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

public class ValueMarshal extends Marshal
{
  public static final Marshal MARSHAL = new ValueMarshal(false);
  public static final Marshal MARSHAL_PASS_THRU = new ValueMarshal(true);

  private boolean _isPassThru;

  protected ValueMarshal(boolean isPassThru)
  {
    _isPassThru = isPassThru;
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
    return expr.eval(env);
  }

  public Object marshal(Env env, Value value, Class expectedClass)
  {
    if (_isPassThru) {
      // php/0433
      return value.toLocalRef();
    }
    else {
      // php/3c81
      // return value.toLocalValue();
      return value.toLocalValueReadOnly(); // non-copy
    }
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

  /*
  @Override
  public int getMarshalingCost(Expr expr)
  {
    return Marshal.FOUR;
  }
  */

  @Override
  public Class getExpectedClass()
  {
    return Value.class;
  }
}
