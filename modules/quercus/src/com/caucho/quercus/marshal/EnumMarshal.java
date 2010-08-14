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

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.net.URL;

import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.util.L10N;

/**
 * Code for marshalling arguments.
 */
public class EnumMarshal extends Marshal {
  private Class _enumClass;

  public EnumMarshal(Class enumClass)
  {
    _enumClass = enumClass;
  }

  public Object marshal(Env env, Expr expr, Class argClass)
  {
    String name = expr.evalString(env);

    return Enum.valueOf(_enumClass, name);
  }
  
  public Object marshal(Env env, Value value, Class argClass)
  {
    String name = value.toString();

    return Enum.valueOf(_enumClass, name);
  }
  
  public Value unmarshal(Env env, Object value)
  {
    if (value == null)
      return NullValue.NULL;
    else
      return env.createString(value.toString());
  }
  
  @Override
  public Class getExpectedClass()
  {
    return _enumClass;
  }
}

