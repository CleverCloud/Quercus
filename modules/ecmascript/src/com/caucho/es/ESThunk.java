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

import java.util.HashMap;
import java.util.Iterator;

/**
 * JavaScript object
 */
class ESThunk extends ESBase {
  static final int OBJ_PROTO_THUNK = 0;
  static final int OBJ_THUNK = OBJ_PROTO_THUNK + 1;
  static final int FUN_PROTO_THUNK = OBJ_THUNK + 1;
  static final int FUN_THUNK = FUN_PROTO_THUNK + 1;
  static final int ARRAY_PROTO_THUNK = FUN_THUNK + 1;
  static final int ARRAY_THUNK = ARRAY_PROTO_THUNK + 1;
  static final int STRING_PROTO_THUNK = ARRAY_THUNK + 1;
  static final int STRING_THUNK = STRING_PROTO_THUNK + 1;
  static final int BOOL_PROTO_THUNK = STRING_THUNK + 1;
  static final int BOOL_THUNK = BOOL_PROTO_THUNK + 1;
  static final int NUM_PROTO_THUNK = BOOL_THUNK + 1;
  static final int NUM_THUNK = NUM_PROTO_THUNK + 1;
  static final int DATE_PROTO_THUNK = NUM_THUNK + 1;
  static final int DATE_THUNK = DATE_PROTO_THUNK + 1;
  static final int MATH_THUNK = DATE_THUNK + 1;
  static final int REGEXP_PROTO_THUNK = MATH_THUNK + 1;
  static final int REGEXP_THUNK = REGEXP_PROTO_THUNK + 1;

  private int index;

  /**
   * Simple constructor for parentless objects.
   */
  ESThunk(int index)
  {
    this.index = index;
  }

  ESBase getObject()
  {
    return getThunk(index);
  }

  /**
   * Puts a new value in the property table with the appropriate flags
   */
  public ESBase getProperty(ESString name) throws Throwable
  {
    ESBase object = getThunk(index);

    return object.getProperty(name);
  }
  /**
   * Puts a new value in the property table with the appropriate flags
   */
  public ESBase hasProperty(ESString name) throws Throwable
  {
    ESBase object = getThunk(index);

    return object.hasProperty(name);
  }

  /**
   * Puts a new value in the property table with the appropriate flags
   */
  public void setProperty(ESString name, ESBase value) throws Throwable
  {
    ESBase object = getThunk(index);

    object.setProperty(name, value);
  }

  /**
   * Deletes the entry.  Returns true if successful.
   */
  public ESBase delete(ESString name) throws Throwable
  {
    ESBase object = getThunk(index);

    return object.delete(name);
  }

  public Iterator keys() throws Throwable
  {
    ESBase object = getThunk(index);

    return object.keys();
  }

  public ESBase typeof() throws ESException
  {
    ESBase object = getThunk(index);

    return object.typeof();
  }

  /**
   * XXX: not right
   */
  public ESBase toPrimitive(int hint) throws Throwable
  {
    ESBase object = getThunk(index);

    return object.toPrimitive(hint);
  }

  public ESObject toObject() throws ESException
  {
    ESBase object = getThunk(index);

    return object.toObject();
  }

  public Object toJavaObject() throws ESException
  {
    ESBase object = getThunk(index);

    return object.toJavaObject();
  }

  /**
   * Returns a string rep of the object
   */
  public double toNum() throws Throwable
  {
    ESBase object = getThunk(index);

    return object.toNum();
  }

  /**
   * Returns a string rep of the object
   */
  public ESString toStr() throws Throwable
  {
    ESBase object = getThunk(index);

    return object.toStr();
  }

  public ESString toSource(IntMap map, boolean isLoopPass) throws Throwable
  {
    ESBase object = getThunk(index);

    return object.toSource(map, isLoopPass);
  }

  public boolean toBoolean()
  {
    ESBase object = getThunk(index);

    return object.toBoolean();
  }
  
  public Object copy(HashMap refs)
  {
    return this;
  }

  public boolean ecmaEquals(ESBase b) throws Throwable
  {
    ESBase object = getThunk(index);

    if (this == b)
      return true;
    else
      return object.ecmaEquals(b);
  }

  public ESBase call(Call call, int length) throws Throwable
  {
    ESBase object = getThunk(index);

    return object.call(call, length);
  }

  public ESBase construct(Call call, int length) throws Throwable
  {
    ESBase object = getThunk(index);

    return object.construct(call, length);
  }

  static ESBase getThunk(int index)
  {
    Global resin = Global.getGlobalProto();

    switch (index) {
    case OBJ_PROTO_THUNK:
      return resin.objProto;

    case OBJ_THUNK:
      return resin.object;

    case FUN_PROTO_THUNK:
      return resin.funProto;

    case FUN_THUNK:
      return resin.fun;

    case ARRAY_PROTO_THUNK:
      return resin.arrayProto;
      
    case ARRAY_THUNK:
      return resin.array;

    case STRING_PROTO_THUNK:
      return resin.stringProto;

    case STRING_THUNK:
      return resin.string;

    case BOOL_PROTO_THUNK:
      return resin.boolProto;

    case BOOL_THUNK:
      return resin.bool;

    case NUM_PROTO_THUNK:
      return resin.numProto;

    case NUM_THUNK:
      return resin.num;

    case DATE_PROTO_THUNK:
      return resin.dateProto;

    case DATE_THUNK:
      return resin.date;

    case MATH_THUNK:
      return resin.math;

    case REGEXP_PROTO_THUNK:
      return resin.getRegexpProto();

    case REGEXP_THUNK:
      return resin.getRegexp();

    default:
      throw new RuntimeException();
    }
  }
}
