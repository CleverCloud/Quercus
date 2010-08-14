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

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.lib.regexp.RegexpModule;
import com.caucho.quercus.lib.regexp.Regexp;
import com.caucho.quercus.expr.Expr;

import java.util.logging.*;

/**
 * Code for marshaling (PHP to Java) and unmarshaling (Java to PHP) arguments.
 */
public class RegexpMarshal extends StringMarshal {
  private static final Logger log
    = Logger.getLogger(RegexpModule.class.getName());
  
  public static final RegexpMarshal MARSHAL = new RegexpMarshal();

  public Object marshal(Env env, Expr expr, Class expectedClass)
  {
    try {
      return RegexpModule.createRegexp(env, expr.evalStringValue(env));
    } catch (QuercusException e) {
      env.warning(e);

      return null;
    }
  }

  public Object marshal(Env env, Value value, Class expectedClass)
  {
    try {
      return RegexpModule.createRegexp(env, value.toStringValue(env));
    } catch (QuercusException e) {
      // php/153t
      env.warning(e);

      return null;
    }
  }

  public Value unmarshal(Env env, Object value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  protected int getMarshalingCostImpl(Value argValue)
  {
    if (argValue.isString())
      return Marshal.ZERO;
    else
      return Marshal.MAX;
  }
  
  @Override
  public int getMarshalingCost(Expr expr)
  {
    if (expr.isString())
      return Marshal.ZERO;
    else
      return Marshal.MAX;
  }
  
  @Override
  public Class getExpectedClass()
  {
    return Regexp.class;
  }
}
