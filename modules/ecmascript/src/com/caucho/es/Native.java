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
 * JavaScript function.  The global object is represented as one so
 * the top level script can be called.
 */
class Native extends ESBase {
  static ESId CONSTRUCTOR = ESId.intern("constructor");
  static ESId LENGTH = ESId.intern("length");
  static ESId PROTOTYPE = ESId.intern("prototype");
  static final int NEW = 1;

  String name;
  ESString id;
  ESString []formals;
  int length;
  protected int n;
  protected int newN;

  protected Native(String name, int len)
  {
    prototype = esBase;
    this.name = name;
    this.length = len;
    className = "Function";
    id = ESId.intern(name);
  }

  /**
   * Null constructor
   */
  public ESBase getProperty(ESString key)
  {
    if (key.equals(LENGTH))
      return ESNumber.create(length);
    else
      return esEmpty;
  }

  public ESBase delete(ESString key)
  {
    return ESBoolean.create(false);
  }

  public ESBase typeof() throws ESException
  {
    return ESString.create("function");
  }

  public ESString toStr()
  {
    return ESString.create(decompile());
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

  public double toNum()
  {
    return 0.0/0.0;
  }

  public boolean toBoolean()
  {
    return true;
  }

  public ESObject toObject()
  {
    throw new RuntimeException();
    //return new NativeWrapper(Global.getGlobal(), this);
  }

  public ESBase construct(Call eval, int length) throws Throwable
  {
    if (n != newN)
      return super.construct(eval, length);

    try {
      return (ESBase) call(eval, length);
    } catch (ClassCastException e) {
      throw new ESException("cannot create " + name);
    }
  }

  protected Native create(String name, int n, int len)
  {
    throw new RuntimeException("create not specialized");
  }
}
