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

import com.caucho.server.util.CauchoSystem;

import java.util.Hashtable;

/**
 * JavaScript object representing a Java package or class.
 */
class ESPackage extends ESBase {
  ESString name;
  Class cl;
  Hashtable cache = new Hashtable();

  /**
   * Creates a new package object with the given first segment.
   *
   * @param name the first segment of a Java package/class.
   */
  private ESPackage(String name)
  {
    this.className = "Package";
    this.prototype = esBase;

    this.name = ESString.create(name);
  }

  /**
   * Creates an empty package object.
   */
  static ESPackage create()
  {
    return new ESPackage("");
  }

  public ESBase getProperty(ESString name) throws Throwable
  {
    ESBase value = null;

    if (name == null)
      return this;

    value = (ESBase) cache.get(name);

    if (value != null)
      return value;

    String subname;
    if (this.name.equals(ESString.NULL))
      subname = name.toString();
    else
      subname = this.name.toString() + "." + name.toString();

    try {
      Global global = Global.getGlobalProto();
      ClassLoader loader = global.getParentLoader();

      Class cl = CauchoSystem.loadClass(subname, false, loader);

      value = global.classWrap(cl);
    } catch (ClassNotFoundException e) {
      value = new ESPackage(subname);
    }

    cache.put(name, value);

    return value;
  }

  public ESString toStr() { return new ESString("[Package " + name + "]"); }

  public double toNum()
  {
    return 0.0/0.0;
  }

  /**
   * Converts the package to a java object.  For classes, returns the class.
   * For a package, returns null.
   */
  public Object toJavaObject()
  {
    return null;
  }

  ESBase create(String string)
  {
    try {
      return getProperty(ESString.create(string));
    } catch (Throwable e) {
      return ESBase.esNull;
    }
  }
}
