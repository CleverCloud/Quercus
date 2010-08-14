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
 * Implementation class for JavaScript booleans.
 */
public class ESBoolean extends ESBase {
  public final static ESBoolean TRUE = new ESBoolean(true);
  public final static ESBoolean FALSE = new ESBoolean(false);

  boolean value;

  public ESBase typeof() throws ESException
  {
    return ESString.create("boolean");
  }

  public Class getJavaType()
  {
    return boolean.class;
  }

  public boolean isBoolean()
  {
    return true;
  }

  public boolean toBoolean()
  {
    return value;
  }

  public double toNum()
  {
    return value ? 1.0 : 0.0 ;
  }

  public ESBase getProperty(ESString key) throws Throwable
  {
    return Global.getGlobalProto().boolProto.getProperty(key);
  }

  /**
   * Returns this as a string.
   */
  public ESString toStr()
  {
    return ESString.create(value ? "true" : "false");
  }

  public ESObject toObject() throws ESException
  {
    return new ESWrapper("Boolean", Global.getGlobalProto().boolProto, this);
  }

  public Object toJavaObject()
  {
    return new Boolean(value);
  }

  public boolean ecmaEquals(ESBase b) throws Throwable
  {
    if (b == esNull || b == esEmpty || b == esUndefined)
      return false;
    else
      return toNum() == b.toNum();
  }

  /**
   * Returns the proper boolean
   */
  public static ESBoolean create(boolean value)
  {
    return value ? TRUE : FALSE;
  }

  /**
   * Create a new object based on a prototype
   */
  private ESBoolean(boolean value)
  {
    super(null);

    this.value = value;
  }
}



