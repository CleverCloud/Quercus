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
 * JavaScript object
 */
class ESJavaWrapper extends ESObject {
  public Object value;

  public ESString toStr() throws Throwable
  {
    return ESString.create(value == null ? "null" : value.toString());
  }

  public ESString toSource(IntMap map, boolean isLoopPath) throws Throwable
  {
    if (isLoopPath)
      return null;
    else
      return toStr();
  }

  public ESBase toPrimitive(int hint) throws Throwable
  {
    if (value instanceof ESBase)
      return (ESBase) value;
    else
      return toStr();
  }

  public Object toJavaObject() { return value; }
  
  public ESJavaWrapper wrap(Object value)
  {
    if (value == null)
      throw new NullPointerException();

    ESJavaWrapper child = (ESJavaWrapper) dup();
    child.value = value;
    child.init(className, prototype);

    return child;
  }

  public boolean ecmaEquals(ESBase b)
  {
    if (! (b instanceof ESJavaWrapper))
      return false;
    else
      return value == ((ESJavaWrapper) b).value;
  }

  protected ESJavaWrapper() {}
  protected ESObject dup() { return new ESJavaWrapper(); }

  protected void copy(HashMap refs, Object newObj)
  {
    ESJavaWrapper obj = (ESJavaWrapper) newObj;

    super.copy(refs, obj);

    obj.value = value;
  }

  protected ESJavaWrapper(ESBase proto, Object value)
  {
    super("javaWrapper", proto);

    this.value = value;
  }
}
