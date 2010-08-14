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
 * Implementation class representing the global object.
 */
abstract public class ESGlobal extends ESObject implements ESCallable {
  private Global resin;

  /**
   * Null constructor
   */
  protected ESGlobal(Global resin)
  {
    super("Global", resin);
    //snapPrototype = true;

    this.resin = resin;
  }

  /**
   * Returns the string representation of the type.
   */
  public ESBase typeof() throws ESException
  {
    return ESString.create("function");
  }

  /**
   * returns the string representation
   */
  public ESString toStr() throws ESException
  {
    return ESString.create("[global]");
  }

  /**
   * returns a primitive
   */
  public ESBase toPrimitive(int hint) throws ESException
  {
    return toStr();
  }

  public void setProperty(String name, ESBase value)
    throws Throwable
  {
    setProperty(ESId.intern(name), value);
  }

  public Object toJavaObject()
    throws ESException
  {
    Object o = prototype.toJavaObject();
    return (o == null) ? this : o;
  }

  /**
   * Execute the static function
   */
  ESBase execute()
    throws Throwable
  {
    Global resin = (Global) prototype;

    try {
      Call call = resin.getCall();
      call.caller = call;
      call.setArg(0, this);
      call.top = 1;

      call.scopeLength = 1;
      call.scope[0] = this;
      call.global = this;

      ESBase value = null;

      try {
        value = call(0, call, 0);
      } finally {
        resin.freeCall(call);
      }

      return value;
    } catch (ESException e) {
      throw e;
    } catch (Throwable e) {
      throw new ESWrapperException(e);
    }
  }


  public void export(ESObject dest)
    throws Throwable
  {

  }

  /**
   * Wraps the java object in an ES wrapper
   */
  public ESBase wrap(Object obj)
    throws Throwable
  {
    return resin.wrap(obj);
  }

  public abstract ESBase call(int n, Call call, int length)
    throws Throwable;
}
