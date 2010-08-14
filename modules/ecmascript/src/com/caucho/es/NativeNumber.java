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
class NativeNumber extends Native {
  static final int NEW = 1;
  static final int TO_STRING = NEW + 1;
  static final int VALUE_OF = TO_STRING + 1;

  /**
   * Create a new object based on a prototype
   */
  private NativeNumber(String name, int n, int len)
  {
    super(name, len);

    this.n = n;
  }

  /**
   * Creates the native Object object
   */
  static ESObject create(Global resin)
  {
    Native nativeNum = new NativeNumber("Number", NEW, 1);
    ESWrapper numProto = new ESWrapper("Number", resin.objProto,
                                       ESNumber.create(0));
    NativeWrapper num = new NativeWrapper(resin, nativeNum, numProto,
                                          ESThunk.NUM_THUNK);
    resin.numProto = numProto;

    int flags = DONT_ENUM;
    int allflags = (DONT_ENUM|DONT_DELETE|READ_ONLY);

    numProto.put(ESId.intern("toString"),
                 new NativeNumber("toString", TO_STRING, 0),
                 flags);
    numProto.put(ESId.intern("valueOf"),
                 new NativeNumber("valueOf", VALUE_OF, 0),
                 flags);

    num.put("length", ESNumber.create(1), allflags);
    num.put("MAX_VALUE", ESNumber.create(Double.MAX_VALUE), allflags);
    num.put("MIN_VALUE", ESNumber.create(Double.MIN_VALUE), allflags);
    num.put("NaN", ESNumber.create(0.0/0.0), allflags);
    num.put("NEGATIVE_INFINITY", ESNumber.create(-1.0/0.0), allflags);
    num.put("POSITIVE_INFINITY", ESNumber.create(1.0/0.0), allflags);

    numProto.setClean();
    num.setClean();

    return num;
  }

  public ESBase call(Call eval, int length) throws Throwable
  {
    ESBase argThis;

    switch (n) {
    case NEW:
      if (length == 0)
        return ESNumber.create(0);
      else
        return ESNumber.create(eval.getArg(0).toNum());

    case TO_STRING:
      try {
        return ((ESBase) ((ESWrapper) eval.getArg(-1)).value).toStr();
      } catch (ClassCastException e) {
        if (eval.getArg(-1) instanceof ESNumber)
          return eval.getArg(-1);
        if (eval.getArg(-1) instanceof ESThunk)
          return ((ESBase) ((ESWrapper) ((ESThunk) eval.getArg(-1)).getObject()).value).toStr();

        throw new ESException("toString expected number object");
      }

    case VALUE_OF:
      try {
        return (ESBase) ((ESWrapper) eval.getArg(-1)).value;
      } catch (ClassCastException e) {
        if (eval.getArg(-1) instanceof ESNumber)
          return eval.getArg(-1);
        if (eval.getArg(-1) instanceof ESThunk)
          return (ESBase) ((ESWrapper) ((ESThunk) eval.getArg(-1)).getObject()).value;

        throw new ESException("valueOf expected number object");
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
      value = ESNumber.create(0);
    else
      value = ESNumber.create(eval.getArg(0).toNum());

    return value.toObject();
  }
}
