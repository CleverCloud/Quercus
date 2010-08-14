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

/**
 * Implementation class serving as the base for wrapped Java objects.
 */
public class ESBeanWrapper extends ESBase {
  static ESId CALL = ESId.intern("call");
  protected static ESId LENGTH = ESId.intern("length");

  public int set;
  public IntMap hasDispatch;
  public IntMap setDispatch;
  public IntMap []subGets;
  public IntMap []subSets;
  public HashMap methods;
  public IntMap methodDispatch;

  protected Object value;
  protected String name;
  ESString []formals;
  int length;
  public int n = -3;
  int newN;

  protected ESBeanWrapper()
  {
  }

  public long getVersionId()
  {
    return 0;
  }

  private String decompile()
  {
    StringBuffer sbuf = new StringBuffer();

    sbuf.append("function ");
    sbuf.append(name);
    sbuf.append("(");
    for (int i = 0; formals != null && i < formals.length; i++) {
      if (i != 0)
        sbuf.append(", ");
      sbuf.append(formals[i]);
    }
      
    sbuf.append(") ");

    sbuf.append("{ ");
    sbuf.append("[native code]");
    sbuf.append(" }");

    return sbuf.toString();
  }

  public ESString toStr() throws ESException
  {
    if (n == -1)
      return ESString.create(value == null ? "null" : value.toString());
    else
      return ESString.create(decompile());
  }

  public double toNum() throws ESException
  {
    if (value instanceof Number)
      return ((Number) value).doubleValue();
    else
      throw new ESException("no number: " + getClass().getName());
  }

  public ESBase getProperty(ESString name) throws Throwable
  {
    ESBase value = hasProperty(name);

    if (value != null)
      return value;
    else
      return esEmpty;
  }

  public ESBase hasProperty(ESString name) throws Throwable
  {
    return null;
  }

  public ESString toSource(IntMap map, boolean isLoopPath) throws ESException
  {
    if (isLoopPath)
      return null;
    else
      return toStr();
  }

  public ESBase toPrimitive(int hint) throws ESException
  {
    if (value instanceof ESBase)
      return (ESBase) value;
    else
      return toStr();
  }

  public Object toJavaObject()
  {
    return value != null ? value : this;
  }

  public boolean toBoolean()
  {
    return true;
  }
  
  protected ESBeanWrapper dup()
  {
    throw new UnsupportedOperationException();
  }

  protected ESBeanWrapper dup(int set)
  {
    ESBeanWrapper child = dup();

    child.value = value;
    child.set = set;
    child.hasDispatch = subGets[set];
    child.setDispatch = subSets[set];
    child.subGets = subGets;
    child.subSets = subSets;
    child.methods = methods;

    return child;
  }

  public ESBeanWrapper wrap(Object value)
  {
    throw new RuntimeException();
  }

  public ESBeanWrapper wrapStatic()
  {
    throw new RuntimeException();
  }

  public boolean ecmaEquals(ESBase b)
  {
    if (! (b instanceof ESBeanWrapper))
      return false;
    else
      return value.equals(((ESBeanWrapper) b).value);
  }

  public Object copy(HashMap refs)
  {
    return this;
  }

  public ESBase typeof()
  {
    return ESString.create("object");
  }

  public ESBase call(Call eval, int length, int n) throws Throwable
  {
    throw new ESNullException(toStr() + " is not a function");
  }

  public ESBase call(Call eval, int length) throws Throwable
  {
    return call(eval, length, n);
  }

  public ESBase call(Call eval, int length, ESString key) throws Throwable
  {
    int n = methodDispatch.get(key);
    if (n < 0)
      throw new ESUndefinedException(getClass().getName() + ": undefined call `" + key + "'");

    return call(eval, length, n);
  }

  public ESBase construct(Call eval, int length) throws Throwable
  {
    if (n != newN) {
      throw new ESException("cannot create " + name);
    }

    ESBase value = call(eval, length);

    if (value == esUndefined || value == null)
      throw new ESException("cannot create " + name);

    return value;
  }

  public boolean isModified()
  {
    return true;
  }
}
