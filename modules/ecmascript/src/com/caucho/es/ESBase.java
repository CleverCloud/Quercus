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
 * Implementation class for the base of the JavaScript object hierarchy.
 */
public class ESBase {
  private static ESFactory factory;

  String className;
  ESBase prototype;

  public static ESBase esBase;
  public static ESBase esNull;
  public static ESBase esUndefined;
  public static ESBase esEmpty;

  final static int NONE = 0;
  final static int STRING = 1;
  final static int NUMBER = 2;

  public static final int READ_ONLY   = 0x1;
  public static final int DONT_DELETE = 0x2;
  public static final int DONT_ENUM   = 0x4;
  static final int WATCH       = 0x8;

  static void init(ESFactory factory)
  {
    if (esBase != null)
      return;

    esBase = new ESBase(null);
    esBase.prototype = esBase;
    esBase.className = "base";

    ESBase.factory = factory;
    
    if (factory != null) {
      esNull = factory.createNull();

      esUndefined = factory.createUndefined();
      esEmpty = factory.createUndefined();
    } else {
      esNull = new ESNull();

      esUndefined = new ESUndefined();
      esEmpty = new ESUndefined();
    }
  }

  /**
   * Create a new object based on a prototype
   */
  protected ESBase()
  {
  }

  /**
   * Create a new object based on a prototype
   */
  ESBase(ESBase prototype)
  {
    if (prototype == null)
      prototype = esBase;

    this.prototype = prototype;
  }

  public ESBase typeof() throws ESException
  {
    throw new ESException("no typeof");
  }

  public Class getJavaType()
  {
    return void.class;
  }

  public ESBase getProperty(ESString key) throws Throwable
  {
    return esEmpty;
  }

  boolean canPut(ESString name)
  {
    return true;
  }

  /**
   * Sets the named property
   */
  public void setProperty(ESString key, ESBase value) throws Throwable
  { 
  }

  public ESBase delete(ESString key) throws Throwable
  {
    return ESBoolean.TRUE;
  }

  public ESBase toPrimitive(int type) throws Throwable
  {
    return this;
  }

  public ESBase toPrimitive() throws Throwable
  {
    return toPrimitive(NONE);
  }

  public boolean isBoolean()
  {
    return false;
  }

  public boolean toBoolean()
  {
    return false;
  }

  public boolean isNum()
  {
    return false;
  }

  public double toNum() throws Throwable
  {
    throw new ESException("no number: " + getClass().getName());
  }

  public boolean isString()
  {
    return false;
  }

  public ESString toStr() throws Throwable
  {
    throw new ESException("no string: " + getClass().getName());
  }

  public ESBase valueOf() throws Throwable
  {
    return toPrimitive(NONE);
  }

  public ESString toSource(IntMap map, boolean isLoopPass) throws Throwable
  {
    if (isLoopPass)
      return null;

    return toStr();
  }

  public ESObject toObject() throws ESException
  {
    throw new ESNullException(className + " has no properties");
  }

  public Object toJavaObject() throws ESException
  {
    return null;
  }

  Object copy(HashMap refs)
  {
    return this;
  }

  public ESBase call(Call eval, int length) throws Throwable
  {
    throw new ESNullException(toStr() + " is not a function");
  }

  public ESBase call(Call eval, int length, ESString key) throws Throwable
  {
    ESBase call = hasProperty(key);

    if (call != null) {
      eval.callee = call;
      return call.call(eval, length);
    }

    if (prototype != null && prototype != this)
      return prototype.call(eval, length, key);
    else
      throw new ESUndefinedException("undefined call `" + key + "'");
  }

  public ESBase construct(Call eval, int length) throws Throwable
  {
    throw new ESNullException(toStr() + " is not a constructor");
  }

  public Iterator keys() throws Throwable
  {
    return toObject().keys();
  }

  // useful junk

  String getClassName()
  {
    return className;
  }

  public ESBase hasProperty(ESString key) throws Throwable
  {
    ESBase value = getProperty(key);

    return value == esEmpty ? null : value;
  }
      
  ESBase hasProperty(int i) throws Throwable
  {
    return hasProperty(ESString.create(i));
  }

  /**
   * Returns the text object for the lexeme.
   */
  public ESBase getProperty(String key) throws Throwable
  { 
    return getProperty(ESString.create(key));
  }

  /**
   * Returns the text object for the lexeme.
   */
  ESBase getProperty(int i) throws Throwable
  { 
    return getProperty(ESString.create(i));
  }

  public void setProperty(String key, ESBase value) throws Throwable
  {
    setProperty(ESString.create(key), value);
  }

  /**
   * Sets the named property
   */
  public void setProperty(int i, ESBase value) throws Throwable
  { 
    setProperty(ESString.create(i), value);
  }

  ESBase delete(String key) throws Throwable
  {
    return delete(ESString.create(key));
  }

  ESBase delete(int i) throws Throwable
  {
    return delete(ESString.create(i));
  }

  public ESBase plus(ESBase b) throws Throwable
  {
    ESBase primA = toPrimitive(NONE);
    ESBase primB = b.toPrimitive(NONE);

    if (primA instanceof ESString || primB instanceof ESString) {
      return ESString.create(primA.toStr().toString() + 
                             primB.toStr().toString());
    }
    else {
      return ESNumber.create(primA.toNum() + primB.toNum());
    }
  }

  public boolean lessThan(ESBase ob, boolean neg) throws Throwable
  {
    ESBase a = toPrimitive(NONE);
    ESBase b = ob.toPrimitive(NONE);
    
    if (a instanceof ESString && b instanceof ESString) {
      return ((((ESString) a).compareTo((ESString) b) < 0) != neg);
    } else {
      double da = a.toNum();
      double db = b.toNum();

      if (Double.isNaN(da) || Double.isNaN(db))
        return false;
      else
        return (da < db) != neg;
    }
  }

  public boolean greaterThan(ESBase ob, boolean neg) throws Throwable
  {
    return ob.lessThan(this, neg);
  }
  
  public int toInt32() throws Throwable
  {
    double value = toNum();

    if (Double.isInfinite(value))
      return 0;
    else
      return (int) ((long) value & 0xffffffffL);
  }

  public String toString()
  {
    try {
      return toStr().toString();
    } catch (Throwable e) {
      System.out.println("Exception: " + e);
      e.printStackTrace();
      return "";
    }
  }

  public String toJavaString() throws Throwable
  {
    return toStr().toString();
  }

  public boolean ecmaEquals(ESBase b) throws Throwable
  {
    return this == b;
  }
}
