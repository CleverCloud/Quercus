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

import com.caucho.util.IntMap;

/**
 * JavaScript object
 */
class NativeObject extends Native {
  static final int TO_OBJECT = 2;
  static final int TO_STRING = TO_OBJECT + 1;
  static final int VALUE_OF = TO_STRING + 1;
  static final int TO_SOURCE = VALUE_OF + 1;
  static final int WATCH = TO_SOURCE + 1;
  static final int UNWATCH = WATCH + 1;

  /**
   * Create a new object based on a prototype
   */
  private NativeObject(String name, int n, int len)
  {
    super(name, len);

    this.n = n;
  }

  /**
   * Creates the native Object object
   */
  static ESObject create(Global resin)
  {
    Native nativeObj = new NativeObject("Object", TO_OBJECT, 1);
    resin.objProto = new ESObject("Object", esBase);

    NativeWrapper obj = new NativeWrapper(resin, nativeObj,
                                          resin.objProto, ESThunk.OBJ_THUNK);

    put(resin.objProto, "toString", TO_STRING, 0, DONT_ENUM);
    put(resin.objProto, "valueOf", VALUE_OF, 0, DONT_ENUM);

    put(resin.objProto, "toSource", TO_SOURCE, 0, DONT_ENUM);
    put(resin.objProto, "watch", WATCH, 0, DONT_ENUM);
    put(resin.objProto, "unwatch", UNWATCH, 0, DONT_ENUM);

    resin.objProto.setClean();
    obj.setClean();

    return obj;
  }
  
  private static void put(ESObject obj, String name, int n, int len, 
                          int flags)
  {
    ESId id = ESId.intern(name);
    NativeObject fun = new NativeObject(name, n, len);
    
    try {
      obj.put(id, fun, flags);
    } catch (Exception e) {
      throw new RuntimeException();
    }
  }

  public ESBase call(Call eval, int length) throws Throwable
  {
    switch (n) {
      // Object prototype stuff
    case TO_STRING:
      // XXX: Is this correct?  Test.
      ESBase arg = eval.getArg(-1);

      if (arg instanceof ESObject)
        return toString((ESObject) arg);
      else
        return toString(arg.toObject());

    case VALUE_OF:
      arg = eval.getArg(-1);
      if (arg instanceof ESWrapper) {
        ESWrapper obj = (ESWrapper) arg;

        if (obj.value instanceof ESBase)
          return (ESBase) obj.value;
        else
          return obj.toStr();
      }
      return arg;

    case TO_OBJECT:
      if (length <= 0 ||
          (arg = eval.getArg(0)) == ESBase.esNull ||
          arg == ESBase.esUndefined ||
          arg == ESBase.esEmpty)
        return Global.getGlobalProto().createObject();

      else if (length > 1)
        return createObjectLiteral(eval, length);

      else
        return arg.toObject();

    case TO_SOURCE:
      arg = eval.getThis();
      Global.getGlobalProto().clearMark();
      IntMap map = new IntMap();

      arg.toSource(map, true);
      return arg.toSource(map, false);

    case WATCH:
      if (length < 2)
        throw new ESException("watch expects two arguments");

      ESBase obj = eval.getThis();

      ESString key = eval.getArg(0).toStr();
      ESBase fun = eval.getArg(1);
      if (! (fun instanceof ESClosure) && ! (fun instanceof Native))
        throw new ESException("watch requires function");

      ((ESObject) obj).watch(key, fun);

      return esUndefined;

    case UNWATCH:
      if (length < 1)
        throw new ESException("unwatch expects one argument");

      obj = eval.getThis();

      key = eval.getArg(0).toStr();

      ((ESObject) obj).unwatch(key);

      return esUndefined;

    default:
      throw new RuntimeException("Unknown object function");
    }
  }

  public ESBase construct(Call eval, int length) throws Throwable
  {
    if (n != TO_OBJECT)
      return super.construct(eval, length);

    if (length == 0 || eval.getArg(0) == esNull || 
        eval.getArg(0) == esUndefined || eval.getArg(0) == esEmpty) {
      return Global.getGlobalProto().createObject();
    }
    if (length > 1) {
      return createObjectLiteral(eval, length);
    }

    return eval.getArg(0).toObject();
  }

  private ESBase createObjectLiteral(Call call, int length)
    throws Throwable
  {
    ESObject obj = Global.getGlobalProto().createObject();

    for (int i = 0; i + 1 < length; i += 2) {
      ESString key = call.getArg(i, length).toStr();
      ESBase value = call.getArg(i + 1, length);

      obj.setProperty(key, value);
    }

    return obj;
  }
  
  static public ESBase toString(ESObject obj) throws ESException
  {
    return ESString.create("[object " + obj.getClassName() + "]");
  }
}
