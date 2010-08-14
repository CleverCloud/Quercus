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

import java.util.Iterator;

/**
 * JavaScript object
 */
class ESArrayWrap extends ESJavaWrapper {
  static ESId LENGTH = ESId.intern("length");

  protected ESArrayWrap() {}

  protected int length() { return 0; }
  public ESBase getProperty(int i) throws Throwable { return esEmpty; }
  public ESBase delete(int i) throws ESException { return ESBoolean.FALSE; }
  public void setProperty(int i, ESBase value) throws Throwable { }

  public ESBase getProperty(ESString name) throws Throwable
  {
    if (name.equals(LENGTH))
      return ESNumber.create(length());

    try { // XXX: to fix mips bugs
      double value = name.toNum();
      int iValue = (int) value;

      if (iValue == value)
        return getProperty(iValue);

      return super.getProperty(name);
    } catch (Exception e) {
      return super.getProperty(name);
    }
  }

  public void setProperty(ESString name, ESBase value) throws Throwable
  {
    if (name.equals(LENGTH))
      return;
    
    try { // XXX: to fix mips bugs
      double dIndex = name.toNum();
      int index = (int) dIndex;

      if (index == dIndex)
        setProperty(index, value);
      else
        super.setProperty(name, value);
    } catch (Exception e) {
      super.setProperty(name, value);
    }
  }

  public ESBase delete(ESString name) throws Throwable
  {
    if (name.equals(LENGTH))
      return ESBoolean.FALSE;

    try {
      double dIndex = name.toNum();
      int index = (int) dIndex;

      if (index == dIndex)
        return delete(index);
      else
        return super.delete(name);
    } catch (Exception e) {
      return super.delete(name);
    }
  }
  
  class ArrayIterator implements Iterator {
    int length;
    int i;

    public boolean hasNext() { return i < length; }
    public Object next()
    {
      try {
        return i < length ? getProperty(i++) : ESBase.esNull;
      } catch (Throwable e) {
        return ESBase.esNull;
      }
    }

    public void remove() { }

    ArrayIterator()
    {
      length = length();
    }
  }

  public Iterator keys() throws ESException
  {
    return new ArrayIterator();
  }

  public ESString toStr() throws Throwable
  {
    return (ESString) NativeArray.toString(this);
  }

  public ESString toSource(IntMap map, boolean isLoopPass) throws Throwable
  {
    return ESArray.arrayToSource(this, map, isLoopPass);
  }

  protected ESArrayWrap(ESBase proto, Object value)
  {
    super(proto, value);
  }
}
