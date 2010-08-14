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

import com.caucho.util.Exit;

/**
 * ScriptClosure lets Java programs call JavaScript functions.  It contains
 * the state of an executing JavaScript program. 
 */
final public class ScriptClosure {
  private Script script;
  private Global resin;
  private ESGlobal global;

  ScriptClosure(Global resin, ESGlobal global, Script script)
  {
    this.script = script;
    this.resin = resin;
    this.global = global;
  }

  /**
   * Returns the lastModified time of the script.  The last modified
   * time is the maximum of all imported script modified times.
   *
   * <p>getLastModified is vital for dynamic applications like JSP
   * which need to reload the script when it changes.
   */
  public boolean isModified()
  {
    return script.isModified();
  }

  /**
   * Calls the JavaScript function 'name' with no arguments.
   *
   * @param name JavaScript function name.
   * @return The Java object returned by the JavaScript function.
   */
  public synchronized Object call(String name)
    throws Throwable
  {
    Call call = resin.getCall();
    call.caller = call;
    call.top = 1;
    call.setArg(-1, global);

    Object value = call(getFunction(name), call, 0);

    resin.freeCall(call);

    return value;
  }

  /**
   * Calls the JavaScript function 'name' with a single argument.
   * <p>Arguments are automatically wrapped, and return values automatically
   * unwrapped.
   *
   * @param name JavaScript function name.
   * @param a First argument passed to JavaScript.
   * @return The Java object returned by the JavaScript function.
   */
  public synchronized Object call(String name, Object a)
    throws Throwable
  {
    Call call = resin.getCall();
    call.caller = call;
    call.top = 1;
    call.setArg(-1, global);
    call.setArg(0, resin.objectWrap(a));

    Object value = call(getFunction(name), call, 1);

    resin.freeCall(call);

    return value;
  }

  /**
   * Calls the JavaScript function 'name' with two arguments.
   * <p>Arguments are automatically wrapped, and return values automatically
   * unwrapped.
   *
   * @param name JavaScript function name.
   * @param a First argument passed to JavaScript.
   * @param b Second argument passed to JavaScript.
   * @return The Java object returned by the JavaScript function.
   */
  public synchronized Object call(String name, Object a, Object b)
    throws Throwable
  {
    Call call = resin.getCall();
    call.caller = call;
    call.top = 1;
    call.setArg(-1, global);
    call.setArg(0, resin.objectWrap(a));
    call.setArg(1, resin.objectWrap(b));

    Object value = call(getFunction(name), call, 2);

    resin.freeCall(call);

    return value;
  }

  /**
   * Calls the JavaScript function 'name' with three arguments.
   * <p>Arguments are automatically wrapped, and return values automatically
   * unwrapped.
   *
   * @param name JavaScript function name.
   * @param a First argument passed to JavaScript.
   * @param b Second argument passed to JavaScript.
   * @param c Third argument passed to JavaScript.
   * @return The Java object returned by the JavaScript function.
   */
  public synchronized Object call(String name, Object a, Object b, Object c)
    throws Throwable
  {
    Call call = resin.getCall();
    call.caller = call;
    call.top = 1;
    call.setArg(-1, global);
    call.setArg(0, resin.objectWrap(a));
    call.setArg(1, resin.objectWrap(b));
    call.setArg(2, resin.objectWrap(c));

    Object value = call(getFunction(name), call, 3);

    resin.freeCall(call);

    return value;
  }

  /**
   * Calls the JavaScript function 'name' with four arguments.
   * <p>Arguments are automatically wrapped, and return values automatically
   * unwrapped.
   *
   * @param name JavaScript function name.
   * @param a First argument passed to JavaScript.
   * @param b Second argument passed to JavaScript.
   * @param c Third argument passed to JavaScript.
   * @param d Fourth argument passed to JavaScript.
   * @return The Java object returned by the JavaScript function.
   */
  public synchronized Object call(String name, Object a, Object b,
                                  Object c, Object d)
    throws Throwable
  {
    Call call = resin.getCall();
    call.caller = call;
    call.top = 1;
    call.setArg(-1, global);
    call.setArg(0, resin.objectWrap(a));
    call.setArg(1, resin.objectWrap(b));
    call.setArg(2, resin.objectWrap(c));
    call.setArg(3, resin.objectWrap(d));

    Object value = call(getFunction(name), call, 4);

    resin.freeCall(call);

    return value;
  }

  /**
   * Calls the JavaScript function 'name' with an array of arguments.
   * <p>Arguments are automatically wrapped, and return values automatically
   * unwrapped.
   *
   * @param name JavaScript function name.
   * @param args Arguments to pass to the JavaScript function.
   * @return The Java object returned by the JavaScript function.
   */
  public synchronized Object call(String name, Object []args)
    throws Throwable
  {
    Call call = resin.getCall();
    call.caller = call;
    call.setArg(0, global);
    call.top = 1;
    for (int i = 0; i < args.length; i++)
      call.setArg(i, resin.objectWrap(args[i]));

    Object value = call(getFunction(name), call, args.length);

    resin.freeCall(call);

    return value;
  }

  private ESClosure getFunction(String name)
    throws Throwable
  {
    ESBase fun = global.hasProperty(ESString.create(name));

    if (fun == null)
      throw new ESException("no such function `" + name + "'");

    if (! (fun instanceof ESClosure))
      throw new ESException(name + " should be function: " + fun);

    return (ESClosure) fun;
  }

  private Object call(ESClosure closure, Call call, int length)
    throws Throwable
  {
    int scopeLength = closure.scopeLength;
    call.scopeLength = scopeLength;
    for (int i = 0; i < scopeLength; i++)
      call.scope[i] = closure.scope[i];

    /* XXX: use this to test the test.
    call.scopeLength = closure.scopeLength;
    call.scope = closure.scope;
    */

    boolean doExit = Exit.addExit();
    Global old = resin.begin();
    try {
      ESBase value = closure.call(call, length);

      return value == null ? null : value.toJavaObject();
    } finally {
      resin.end(old);
      if (doExit)
        Exit.exit();
    }
  }

  /**
   * Returns a global property of the closure.
   *
   * @param name name of the global property
   * @return unwrapped Java object of the global property.
   */
  public synchronized Object getProperty(String name)
  {
    try {
      ESBase object = global.getProperty(name);

      return object.toJavaObject();
    } catch (Throwable e) {
      return null;
    }
  }

  /**
   * Sets a global property of the closure.
   *
   * @param name name of the global property
   * @param value Java object to assign to the global property.
   */
  public synchronized void setProperty(String name, Object value)
  {
    try {
      global.setProperty(name, resin.objectWrap(value));
    } catch (Throwable e) {
    }
  }
}
