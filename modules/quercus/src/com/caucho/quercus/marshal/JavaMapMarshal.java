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
import com.caucho.quercus.env.JavaListAdapter;
import com.caucho.quercus.env.JavaMapAdapter;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.util.L10N;

/**
 * Code for marshalling arguments.
 */
public class JavaMapMarshal extends JavaMarshal {
  private static final L10N L = new L10N(JavaMarshal.class);

  public JavaMapMarshal(JavaClassDef def,
                      boolean isNotNull)
  {
    this(def, isNotNull, false);
  }

  public JavaMapMarshal(JavaClassDef def,
                      boolean isNotNull,
                      boolean isUnmarshalNullAsFalse)
  {
    super(def, isNotNull, isUnmarshalNullAsFalse);
  }

  public Object marshal(Env env, Value value, Class argClass)
  {
    if (! value.isset()) {
      if (_isNotNull) {
        env.warning(L.l("null is an unexpected argument, expected {0}",
                        shortName(argClass)));
      }

      return null;
    }

    Object obj = value.toJavaMap(env, argClass);

    if (obj == null) {
      if (_isNotNull) {
        env.warning(L.l("null is an unexpected argument, expected {0}",
                        shortName(argClass)));
      }

      return null;
    }
    else if (! argClass.isAssignableFrom(obj.getClass())) {
      env.warning(L.l(
        "'{0}' of type '{1}' is an unexpected argument, expected {2}",
        value,
        shortName(value.getClass()),
        shortName(argClass)));
      return null;
    }

    return obj;
  }
  @Override
  protected int getMarshalingCostImpl(Value argValue)
  {
    if (argValue instanceof JavaMapAdapter
        && getExpectedClass()
      .isAssignableFrom(argValue.toJavaObject().getClass()))
      return Marshal.ZERO;
    else if (argValue.isArray())
      return Marshal.THREE;
    else
      return Marshal.FOUR;
  }
}

