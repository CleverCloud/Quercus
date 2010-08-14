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

import java.util.HashMap;
import java.util.Iterator;

/**
 * Implementation class for a JavaScript Object.
 */
public class ESObject extends ESBase {
  static ESId TO_STRING = ESId.intern("toString");
  static ESId VALUE_OF = ESId.intern("valueOf");
  static ESId CALL = ESId.intern("call");
  static ESId CONSTRUCT = ESId.intern("construct");
  static int DIRTY = 0;
  static int CLEAN = DIRTY + 1;
  static int COW = CLEAN + 1;

  int copyState = DIRTY;

  ESString []propNames;
  ESBase []propValues;
  ESBase []propWatch;
  int []propFlags;
  int size;
  int fill;
  int mask;

  int mark; // for printing
  protected boolean snapPrototype;

  protected ESObject()
  {
  }

  /**
   * Simple constructor for parentless objects.
   */
  public ESObject(String className, ESBase proto)
  {
    init(className, proto, 16);
  }

  protected ESObject(String className, ESBase proto, int hashSize)
  {
    init(className, proto, hashSize < 16 ? 16 : hashSize);
  }

  private void init(String className, ESBase proto, int hashSize)
  {
    if (proto == null) {
      Global resin = Global.getGlobalProto();
      proto = resin == null ? null : resin.objProto;
    }
    if (className == null && proto != null)
      className = proto.className;
    prototype = proto;

    propNames = new ESString[hashSize];
    propValues = new ESBase[hashSize];
    propFlags = new int[hashSize];
    mask = propNames.length - 1;
    size = 0;
    fill = 0;
    copyState = DIRTY;

    this.className = className == null ? "Object" : className;
  }

  void init(String className, ESBase proto)
  {
    init(className, proto, 16);
  }

  void setClean()
  {
    copyState = CLEAN;
  }

  /**
   * Expands the property table
   */
  private void resize(int newSize)
  {
    ESString []newNames = new ESString[newSize];
    ESBase []newValues = new ESBase[newSize];
    int []newFlags = new int[newSize];
    ESBase []newWatch = null;
    if (propWatch != null)
      newWatch = new ESBase[newSize];

    mask = newNames.length - 1;

    for (int i = 0; i < propNames.length; i++) {
      if (propValues[i] == null && (propFlags[i] & WATCH) == 0)
        continue;

      int hash = propNames[i].hashCode() & mask;

      while (true) {
        if (newNames[hash] == null) {
          newNames[hash] = propNames[i];
          newValues[hash] = propValues[i];
          newFlags[hash] = propFlags[i];
          if (newWatch != null)
            newWatch[hash] = propWatch[i];
          break;
        }
        hash = (hash + 1) & mask;
      }
    }

    propNames = newNames;
    propValues = newValues;
    propFlags = newFlags;
    propWatch = newWatch;
    fill = size;
  }

  private void refill()
  {
    for (int i = 0; i < propNames.length; i++) {
      if (propValues[i] == null && (propFlags[i] & WATCH) == 0) {
        propNames[i] = null;
        continue;
      }

      int hash = propNames[i].hashCode() & mask;

      while (true) {
        if (propValues[hash] == null && (propFlags[hash] & WATCH) == 0) {
          propNames[hash] = propNames[i];
          propValues[hash] = propValues[i];
          propFlags[hash] = propFlags[i];
          propNames[i] = null;
          propValues[i] = null;
          propFlags[i] = 0;
          break;
        }
        hash = (hash + 1) & mask;
      }
    }

    fill = size;
  }

  /**
   * Gets a property value.
   */
  public ESBase getProperty(ESString name) throws Throwable
  {
    int hash = name.hashCode() & mask;

    while (true) {
      ESString propName = propNames[hash];

      if (propName == name || name.equals(propName)) {
        ESBase value = propValues[hash];
        return value == null ? prototype.getProperty(name) : value;
      }
      else if (propName == null) {
        ESBase value = prototype.getProperty(name);
        if (snapPrototype)
          setProperty(name, value);
        return value;
      }

      hash = (hash + 1) & mask;
    }
  }

  protected boolean canPut(ESString name)
  {
    int hash = name.hashCode() & mask;

    while (true) {
      ESString propName = propNames[hash];

      if (name.equals(propName) && propValues[hash] != null)
        return (propFlags[hash] & READ_ONLY) == 0;
      else if (propName == null) {
        if (prototype instanceof ESObject)
          return ((ESObject) prototype).canPut(name);
        else
          return true;
      }

      hash = (hash + 1) & mask;
    }
  }

  /*
  public ESBase callWatch(ESString name, int hash, ESBase value)
  throws Exception
  {
    Global resin = Global.getGlobalProto();
    Call call = resin.getCall();
    call.top = 1;

    if (propWatch[hash] instanceof ESClosure)
      call.setArg(-1, ((ESClosure) propWatch[hash]).scope[0]);
    else
      call.setArg(-1, null);
    call.setArg(0, name);
    call.setArg(1, propValues[hash]);
    call.setArg(2, value);
    value = propWatch[hash].call(call, 3);
    resin.freeCall(call);

    return value;
  }
  */

  /**
   * Puts a new value in the property table with the appropriate flags
   */
  public void setProperty(ESString name, ESBase value) throws Throwable
  {
    if (copyState != DIRTY) {
      if (copyState == COW)
        copyAll();
      copyState = DIRTY;
    }

    if (value == esEmpty)
      value = esUndefined;

    int hash = name.hashCode() & mask;

    while (true) {
      ESString propName = propNames[hash];

      if (propValues[hash] == null) {
        if (! prototype.canPut(name))
          return;

        if (propName == null)
          fill++;

        propNames[hash] = name;
        /*
        if ((propFlags[hash] & WATCH) != 0)
          value = callWatch(name, hash, value);
        */
        propValues[hash] = value;
        propFlags[hash] = 0;

        size++;

        if (propNames.length <= 4 * size) {
          resize(4 * propNames.length);
        }
        else if (propNames.length <= 2 * fill)
          refill();

        return;
      }
      else if (propName != name && ! propName.equals(name)) {
        hash = (hash + 1) & mask;
        continue;
      }
      else if ((propFlags[hash] & READ_ONLY) != 0)
        return;
      else {
        /*
        if ((propFlags[hash] & WATCH) != 0)
          value = callWatch(name, hash, value);
        */
        propValues[hash] = value;
        return;
      }
    }
  }

  public void put(ESString name, ESBase value, int flags)
  {
    int hash = name.hashCode() & mask;

    while (true) {
      ESString propName = propNames[hash];

      if (propName == null ||
          propValues[hash] == null || propName.equals(name)) {
        if (propName == null)
          fill++;
        if (propValues[hash] == null)
          size++;

        propNames[hash] = name;
        propValues[hash] = value;
        propFlags[hash] = flags;

        if (propNames.length <= 4 * size) {
          resize(4 * propNames.length);
        }
        else if (propNames.length <= 2 * fill)
          refill();

        return;
      }

      hash = (hash + 1) & mask;
    }
  }

  public void put(String name, ESBase value, int flags)
  {
    ESId id = ESId.intern(name);

    put(id, value, flags);
  }

  /**
   * Deletes the entry.  Returns true if successful.
   */
  public ESBase delete(ESString name) throws Throwable
  {
    if (copyState != DIRTY) {
      if (copyState == COW)
        copyAll();
      copyState = DIRTY;
    }

    int hash = name.hashCode() & mask;

    while (true) {
      ESString hashName = propNames[hash];

      if (hashName == null)
        return ESBoolean.FALSE;
      else if (propValues[hash] != null && hashName.equals(name)) {
        if ((propFlags[hash] & DONT_DELETE) != 0)
          return ESBoolean.FALSE;
        else {
          propValues[hash] = null;
          size--;
          return ESBoolean.TRUE;
        }
      }

      hash = (hash + 1) & mask;
    }
  }

  public void watch(ESString name, ESBase fun)
  {
    if (copyState != DIRTY) {
      if (copyState == COW)
        copyAll();
      copyState = DIRTY;
    }

    int hash = name.hashCode() & mask;

    while (true) {
      ESString propName = propNames[hash];

      if (propValues[hash] == null) {
        if (! prototype.canPut(name))
          return;

        if (propName == null)
          fill++;

        propNames[hash] = name;
        propValues[hash] = esEmpty;
        propFlags[hash] = WATCH;
        if (propWatch == null)
          propWatch = new ESBase[propFlags.length];
        propWatch[hash] = fun;

        size++;

        if (propNames.length <= 4 * size)
          resize(4 * propNames.length);
        else if (propNames.length <= 2 * fill)
          refill();

        return;
      }
      else if (propName != name && ! propName.equals(name)) {
        hash = (hash + 1) & mask;
        continue;
      }
      else if ((propFlags[hash] & READ_ONLY) != 0)
        return;
      else {
        propFlags[hash] |= WATCH;
        if (propWatch == null)
          propWatch = new ESBase[propFlags.length];

        propWatch[hash] = fun;
        return;
      }
    }
  }

  public void unwatch(ESString name)
  {
    if (copyState != DIRTY) {
      if (copyState == COW)
        copyAll();
      copyState = DIRTY;
    }

    int hash = name.hashCode() & mask;

    while (true) {
      ESString propName = propNames[hash];

      if (propName == null)
        return;
      else if (propName.equals(name)) {
        propFlags[hash] &= ~WATCH;

        return;
      }
    }
  }

  /**
   * Sets the named property
   */
  public void put(int i, ESBase value, int flags)
  { 
    put(ESString.create(i), value, flags);
  }

  public Iterator keys() throws ESException
  {
    return new PropertyEnumeration(this);
  }

  public ESBase typeof() throws ESException
  {
    return ESString.create("object");
  }

  /**
   * XXX: not right
   */
  public ESBase toPrimitive(int hint) throws Throwable
  {
    Global resin = Global.getGlobalProto();
    Call eval = resin.getCall();
    eval.global = resin.getGlobal();

    try {
      ESBase fun = hasProperty(hint == STRING ? TO_STRING : VALUE_OF);

      if (fun instanceof ESClosure || fun instanceof Native) {
        eval.stack[0] = this;
        eval.top = 1;
        ESBase value = fun.call(eval, 0);

        if (value instanceof ESBase && ! (value instanceof ESObject))
          return value;
      }

      fun = hasProperty(hint == STRING ? VALUE_OF : TO_STRING);

      if (fun instanceof ESClosure || fun instanceof Native) {
        eval.stack[0] = this;
        eval.top = 1;
        ESBase value = fun.call(eval, 0);

        if (value instanceof ESBase && ! (value instanceof ESObject))
          return value;
      }

      throw new ESException("cannot convert object to primitive type");
    } finally {
      resin.freeCall(eval);
    }
  }

  public ESObject toObject() { return this; }

  public Object toJavaObject() throws ESException
  {
    return this;
  }

  /**
   * Returns a string rep of the object
   */
  public double toNum() throws Throwable
  {
    ESBase value = toPrimitive(NUMBER);

    if (value instanceof ESObject)
      throw new ESException("toPrimitive must return primitive");

    return value.toNum();
  }

  /**
   * Returns a string rep of the object
   */
  public ESString toStr() throws Throwable
  {
    ESBase prim = toPrimitive(STRING);

    if (prim instanceof ESObject)
      throw new ESException("toPrimitive must return primitive");

    return prim.toStr();
  }

  public ESString toSource(IntMap map, boolean isLoopPass) throws Throwable
  {
    CharBuffer cb = new CharBuffer();
    Global resin = Global.getGlobalProto();

    int mark = map.get(this);
    
    if (mark > 0 && isLoopPass)
      return null;
    else if (mark > 0) {
      cb.append("#" + mark + "=");
      map.put(this, -mark);
    } else if (mark == 0 && isLoopPass) {
      map.put(this, resin.addMark());
      return null;
    } else if (mark < 0 && ! isLoopPass) {
      return ESString.create("#" + -mark + "#");
    }

    cb.append("{");

    if (isLoopPass)
      map.put(this, 0);

    Iterator e = keys();

    boolean isFirst = true;
    while (e.hasNext()) {
      if (! isFirst)
        cb.append(", ");
      isFirst = false;

      ESString key = (ESString) e.next();

      cb.append(key);
      cb.append(":");
      ESBase value = getProperty(key);
      if (isLoopPass)
        value.toSource(map, isLoopPass);
      else
        cb.append(value.toSource(map, isLoopPass));
    }

    cb.append("}");

    return new ESString(cb.toString());
  }

  public boolean toBoolean() { return true; }
  
  ESObject dup() { return new ESObject(); }
  
  public Object copy(HashMap refs)
  {
    Object ref = refs.get(this);
    if (ref != null)
      return ref;

    ESObject copy = dup();
    refs.put(this, copy);

    copy(refs, copy);

    return copy;
  }

  private void copyAll()
  {
    copyState = DIRTY;

    int len = propValues.length;

    ESString []newPropNames = new ESString[len];
    int []newPropFlags = new int[len];
    ESBase []newPropValues = new ESBase[len];

    System.arraycopy(propNames, 0, newPropNames, 0, len);
    System.arraycopy(propFlags, 0, newPropFlags, 0, len);
    System.arraycopy(propValues, 0, newPropValues, 0, len);

    propNames = newPropNames;
    propFlags = newPropFlags;
    propValues = newPropValues;

    if (propWatch != null) {
      ESBase []newPropWatch = new ESBase[len];
      System.arraycopy(propWatch, 0, newPropWatch, 0, len);
      propWatch = newPropWatch;
    }
  }

  ESObject resinCopy()
  {
    ESObject obj = dup();

    copy(obj);

    return obj;
  }

  protected void copy(Object newObj)
  {
    ESObject obj = (ESObject) newObj;

    obj.prototype = prototype;
    obj.className = className;

    obj.propNames = propNames;
    obj.propValues = propValues;
    obj.propFlags = propFlags;
    obj.propWatch = propWatch;
    obj.size = size;
    obj.fill = fill;
    obj.mask = mask;
    obj.copyState = copyState;

    if (obj.copyState == DIRTY) {
      throw new RuntimeException();
    }
    else if (copyState == CLEAN) {
      copyState = COW;
      obj.copyState = COW;
    }
  }

  protected void copy(HashMap refs, Object newObj)
  {
    ESObject obj = (ESObject) newObj;

    obj.prototype = (ESBase) prototype.copy(refs);
    obj.className = className;

    obj.propNames = propNames;
    obj.propValues = propValues;
    obj.propFlags = propFlags;
    obj.propWatch = propWatch;
    obj.size = size;
    obj.fill = fill;
    obj.mask = mask;
    obj.copyState = copyState;

    if (obj.copyState == DIRTY) {
      obj.copyAll();
    }
    else if (copyState == CLEAN) {
      copyState = COW;
      obj.copyState = COW;
    }
  }

  ESObject shallowCopy()
  {
    ESObject obj = dup();

    shallowCopy(obj);

    return obj;
  }

  protected void shallowCopy(Object newObj)
  {
    ESObject obj = (ESObject) newObj;

    obj.prototype = prototype;
    obj.className = className;

    int len = propValues.length;

    if (propWatch != null) {
      obj.propWatch = new ESBase[len];
      System.arraycopy(propWatch, 0, obj.propWatch, 0, len);
    }

    obj.propNames = new ESString[len];
    obj.propFlags = new int[len];
    obj.propValues = new ESBase[len];

    ESString []newNames = obj.propNames;
    ESString []oldNames = propNames;
    ESBase []newValues = obj.propValues;
    ESBase []oldValues = propValues;
    int []newFlags = obj.propFlags;
    int []oldFlags = propFlags;
    for (int i = 0; i < len; i++) {
      newNames[i] = oldNames[i];
      newValues[i] = oldValues[i];
      newFlags[i] = oldFlags[i];
    }

    obj.size = size;
    obj.mask = mask;
    obj.fill = fill;
    obj.copyState = DIRTY;
  }

  public boolean ecmaEquals(ESBase b) throws Throwable
  {
    if (b instanceof ESObject || b instanceof ESThunk)
      return this == b;
    else
      return toPrimitive(NONE).ecmaEquals(b);
  }

  public ESBase call(Call call, int length) throws Throwable
  {
    ESBase callFun = hasProperty(CALL);

    if (callFun != null) {
      call.setThis(this);
      return callFun.call(call, length);
    }

    throw new ESNullException(toStr() + " is not a function");
  }

  public ESBase construct(Call call, int length) throws Throwable
  {
    ESBase callFun = hasProperty(CONSTRUCT);

    if (callFun != null) {
      call.setThis(this);
      return callFun.construct(call, length);
    }

    throw new ESNullException(toStr() + " is not a constructor");
  }
}
