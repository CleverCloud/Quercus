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

package com.caucho.el;

import javax.el.ELContext;
import javax.el.ELException;
import java.util.HashMap;

/**
 * Marshalls an expression.
 */
abstract public class Marshall {
  private static final HashMap<Class,Marshall> ARG_MAP
    = new HashMap<Class,Marshall>();

  public static Marshall create(Class arg)
  {
    Marshall marshall = ARG_MAP.get(arg);

    if (marshall != null)
      return marshall;
    else
      return OBJECT;
  }
  
  abstract public Object marshall(Expr expr, ELContext env)
    throws ELException;

  public static final Marshall BOOLEAN = new Marshall() {
      public Object marshall(Expr expr, ELContext env)
        throws ELException
      {
        return new Boolean((boolean) expr.evalBoolean(env));
      }
    };

  public static final Marshall BYTE = new Marshall() {
      public Object marshall(Expr expr, ELContext env)
        throws ELException
      {
        return new Byte((byte) expr.evalLong(env));
      }
    };

  public static final Marshall SHORT = new Marshall() {
      public Object marshall(Expr expr, ELContext env)
        throws ELException
      {
        return new Short((short) expr.evalLong(env));
      }
    };

  public static final Marshall INTEGER = new Marshall() {
      public Object marshall(Expr expr, ELContext env)
        throws ELException
      {
        return new Integer((int) expr.evalLong(env));
      }
    };

  public static final Marshall LONG = new Marshall() {
      public Object marshall(Expr expr, ELContext env)
        throws ELException
      {
        return new Long(expr.evalLong(env));
      }
    };

  public static final Marshall FLOAT = new Marshall() {
      public Object marshall(Expr expr, ELContext env)
        throws ELException
      {
        return new Float((float) expr.evalDouble(env));
      }
    };

  public static final Marshall DOUBLE = new Marshall() {
      public Object marshall(Expr expr, ELContext env)
        throws ELException
      {
        return new Double(expr.evalDouble(env));
      }
    };

  public static final Marshall STRING = new Marshall() {
      public Object marshall(Expr expr, ELContext env)
        throws ELException
      {
        return expr.evalString(env);
      }
    };

  public static final Marshall CHARACTER = new Marshall() {
      public Object marshall(Expr expr, ELContext env)
        throws ELException
      {
        String s = expr.evalString(env);

        if (s == null || s.length() == 0)
          return null;
        else
          return new Character(s.charAt(0));
      }
    };

  public static final Marshall OBJECT = new Marshall() {
      public Object marshall(Expr expr, ELContext env)
        throws ELException
      {
        return expr.getValue(env);
      }
    };

  static {
    ARG_MAP.put(boolean.class, BOOLEAN);
    ARG_MAP.put(Boolean.class, BOOLEAN);
    
    ARG_MAP.put(byte.class, BYTE);
    ARG_MAP.put(Byte.class, BYTE);
    
    ARG_MAP.put(short.class, SHORT);
    ARG_MAP.put(Short.class, SHORT);
    
    ARG_MAP.put(int.class, INTEGER);
    ARG_MAP.put(Integer.class, INTEGER);
    
    ARG_MAP.put(long.class, LONG);
    ARG_MAP.put(Long.class, LONG);
    
    ARG_MAP.put(float.class, FLOAT);
    ARG_MAP.put(Float.class, FLOAT);
    
    ARG_MAP.put(double.class, DOUBLE);
    ARG_MAP.put(Double.class, DOUBLE);
    
    ARG_MAP.put(char.class, CHARACTER);
    ARG_MAP.put(Character.class, CHARACTER);
    
    ARG_MAP.put(String.class, STRING);
  }
}
