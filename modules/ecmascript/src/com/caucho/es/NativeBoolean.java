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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.es;

/**
 * JavaScript object
 */
class NativeBoolean extends Native {
  static final int NEW = 1;
  static final int TO_STRING = NEW + 1;
  static final int VALUE_OF = TO_STRING + 1;

  /**
   * Create a new object based on a prototype
   */
  private NativeBoolean(String name, int n, int len)
  {
    super(name, len);

    this.n = n;
  }

  /**
   * Creates the initial native Boolean object
   */
  static ESObject create(Global resin)
  {
    Native nativeBool = new NativeBoolean("Boolean", NEW, 1);
    ESWrapper boolProto = new ESWrapper("Boolean", resin.objProto,
                                        ESBoolean.FALSE);
    NativeWrapper bool = new NativeWrapper(resin, nativeBool,
                                           boolProto, ESThunk.BOOL_THUNK);
    resin.boolProto = boolProto;

    put(boolProto, "toString", TO_STRING, 0, DONT_ENUM);
    put(boolProto, "valueOf", VALUE_OF, 0, DONT_ENUM);

    bool.setClean();
    boolProto.setClean();

    return bool;
  }

  private static void put(ESObject obj, String name, int n, int len,
                          int flags)
  {
    obj.put(name, new NativeBoolean(name, n, len), flags);
  }

  public ESBase call(Call eval, int length) throws Throwable
  {
    switch (n) {
    case NEW:
      if (length == 0)
        return ESBoolean.FALSE;
      else
        return ESBoolean.create(eval.getArg(0).toBoolean());

    case TO_STRING:
      try {
        return ((ESBase) ((ESWrapper) eval.getArg(-1)).value).toStr();
      } catch (ClassCastException e) {
        if (eval.getArg(-1) instanceof ESBoolean)
          return eval.getArg(-1);
        if (eval.getArg(-1) instanceof ESThunk)
          return ((ESBase) ((ESWrapper) ((ESThunk) eval.getArg(-1)).getObject()).value).toStr();

        throw new ESException("toString expected boolean object");
      }

    case VALUE_OF:
      try {
        return (ESBase) ((ESWrapper) eval.getArg(-1)).value;
      } catch (ClassCastException e) {
        if (eval.getArg(-1) instanceof ESBoolean)
          return eval.getArg(-1);
        if (eval.getArg(-1) instanceof ESThunk)
          return (ESBase) ((ESWrapper) ((ESThunk) eval.getArg(-1)).getObject()).value;

        throw new ESException("valueOf expected boolean object");
      }

    default:
      throw new RuntimeException("Unknown object function");
    }
  }

  public ESBase construct(Call eval, int length) throws Throwable
  {
    if (n != NEW)
      return super.construct(eval, length);

    ESBase value;

    if (length == 0)
      value = ESBoolean.FALSE;
    else
      value = ESBoolean.create(eval.getArg(0).toBoolean());

    return value.toObject();
  }
}
