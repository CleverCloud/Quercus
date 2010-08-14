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

import com.caucho.util.CharBuffer;
import com.caucho.util.IntMap;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * JavaScript object
 */
class ESArray extends ESObject {
  static ESId LENGTH = ESId.intern("length");

  /**
   * Create a new object based on a prototype
   */
  ESArray()
  {
    super("Array", null);
    put(LENGTH, ESNumber.create(0), DONT_ENUM|DONT_DELETE);
  }

  public void setProperty(int i, ESBase value) throws Throwable
  {
    super.setProperty(ESString.create(i), value);

    int length = getProperty(LENGTH).toInt32();
    if (i >= length)
      super.setProperty(LENGTH, ESNumber.create(i + 1));
  }

  public void setProperty(ESString key, ESBase value) throws Throwable
  {
    if (key.equals(LENGTH)) {
      int oldLength;
      int newLength;

      try { 
        oldLength = getProperty(LENGTH).toInt32();
        newLength = value.toInt32();
      } catch (ESException e) {
        return;
      }
 
      for (int i = newLength; i < oldLength; i++) {
        try {
          delete(ESString.create(i));
        } catch (Exception e) {
        }
      }

      if (newLength < 0)
        newLength = 0;

      super.setProperty(LENGTH, ESNumber.create(newLength));
      return;
    }

    super.setProperty(key, value);

    try {
      int keyValue = Integer.parseInt(key.toString());
      int length = getProperty(LENGTH).toInt32();

      if (keyValue >= length)
        super.setProperty(LENGTH, ESNumber.create(keyValue + 1));
    } catch (Exception e) {
    }
  }

  static ESString arrayToSource(ESObject obj, IntMap map, boolean isLoopPass)
    throws Throwable
  {
    Global resin = Global.getGlobalProto();
    CharBuffer cb = new CharBuffer();

    int mark = map.get(obj);
    
    if (mark > 0 && isLoopPass)
      return null;
    else if (mark > 0) {
      cb.append("#" + mark + "=");
      map.put(obj, -mark);
    } else if (mark == 0 && isLoopPass) {
      map.put(obj, resin.addMark());
      return null;
    } else if (mark < 0 && ! isLoopPass) {
      return ESString.create("#" + -mark + "#");
    }

    if (isLoopPass)
      map.put(obj, 0);

    int len = obj.getProperty(LENGTH).toInt32();

    boolean noValue = true;
    cb.append("[");
    for (int i = 0; i < len; i++) {
      if (i != 0)
        cb.append(", ");

      ESBase value = obj.hasProperty(i);

      if (value != null && value != esNull &&
          value != esUndefined && value != esEmpty) {
        if (isLoopPass)
          value.toSource(map, isLoopPass);
        else
          cb.append(value.toSource(map, isLoopPass));
      }
      else if (i + 1 == len)
        cb.append(",");
    }
    cb.append("]");

    return ESString.create(cb.toString());
  }

  public Iterator keys() throws ESException
  {
    ArrayList values = new ArrayList();

    try {
      int len = getProperty(LENGTH).toInt32();
      for (int i = 0; i < len; i++) {
        Object prop = getProperty(i);
        // spec says add the key integers
        values.add(String.valueOf(i));
      }
    } catch (Throwable e) {
      throw ESWrapperException.create(e);
    }

    return values.iterator();
  }

  public ESString toSource(IntMap map, boolean isLoopPass) throws Throwable
  {
    return arrayToSource(this, map, isLoopPass);
  }

  public ESObject dup()
  {
    return new ESArray(false);
  }

  protected ESArray(boolean dummy) {}
}
