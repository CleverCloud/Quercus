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

class NativeWrapper extends ESObject {
  static ESId CONSTRUCTOR = ESId.intern("constructor");
  static ESId LENGTH = ESId.intern("length");
  static ESId PROTOTYPE = ESId.intern("prototype");

  Native fun;
  Native constructor;

  NativeWrapper(Global resin, Native fun, ESObject proto, int thunk)
  {
    super("Function", resin.funProto);

    if (proto == null)
      throw new RuntimeException();

    this.fun = fun;
    className = "Function";

    fun.newN = fun.n;
    constructor = fun;

    proto.put(CONSTRUCTOR, new ESThunk(thunk), DONT_ENUM);

    put(PROTOTYPE, new ESThunk(thunk - 1), DONT_ENUM|DONT_DELETE|READ_ONLY);

    put(LENGTH, ESNumber.create(fun.length), DONT_ENUM|DONT_DELETE|READ_ONLY);
  }

  protected NativeWrapper()
  {
  }

  public ESBase typeof() throws ESException
  {
    return ESString.create("function");
  }

  public ESBase getProperty(ESString name) throws Throwable
  {
    ESBase value = fun.getProperty(name);

    if (value != esEmpty && value != null)
      return value;
    else {
      value = super.getProperty(name);

      return value;
    }
  }

  public void setProperty(ESString name, ESBase value) throws Throwable
  {
    fun.setProperty(name, value);

    super.setProperty(name, value);
  }

  public ESBase toPrimitive(int hint) throws Throwable
  {
    return fun.toStr();
  }

  public ESBase call(Call eval, int length) throws Throwable
  {
    return fun.call(eval, length);
  }

  public ESBase construct(Call eval, int length) throws Throwable
  {
    if (constructor != null)
      return constructor.construct(eval, length);
    else
      return super.construct(eval, length);
  }

  protected void copy(Object obj)
  {
    NativeWrapper dup = (NativeWrapper) obj;

    super.copy(dup);
    dup.fun = fun;
    dup.constructor = constructor;
  }

  ESObject resinCopy()
  {
    NativeWrapper dup = (NativeWrapper) dup();

    copy(dup);

    return dup;
  }

  protected ESObject dup()
  {
    return new NativeWrapper();
  }
}

