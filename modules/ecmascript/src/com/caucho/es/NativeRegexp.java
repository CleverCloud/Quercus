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
 * JavaScript object
 */
class NativeRegexp extends Native {
  static ESId INDEX = ESId.intern("index");
  static ESId INPUT = ESId.intern("input");

  static final int NEW = 1;
  static final int COMPILE = NEW + 1;
  static final int EXEC = COMPILE + 1;
  static final int TEST = EXEC + 1;
  static final int TO_STRING = TEST + 1;

  /**
   * Create a new object based on a prototype
   */
  private NativeRegexp(String name, int n, int len)
  {
    super(name, len);

    this.n = n;
  }

  /**
   * Creates the native Regexp object
   */
  static ESRegexpWrapper create(Global resin)
  {
    NativeRegexp nativeRegexp = new NativeRegexp("Regexp", NEW, 1);
    ESRegexp proto;

    try {
      proto = new ESRegexp("", "");
    } catch (Exception e) {
      throw new RuntimeException();
    }
    proto.prototype = resin.objProto;
    resin.regexpProto = proto;
    ESRegexpWrapper regexp = new ESRegexpWrapper(resin, nativeRegexp, proto);

    put(proto, "exec", EXEC, 1);
    put(proto, "compile", COMPILE, 2);
    put(proto, "test", TEST, 1);
    put(proto, "toString", TO_STRING, 0);

    proto.setClean();
    regexp.setClean();

    return regexp;
  }

  static private void put(ESObject proto, String name, int n, int len)
  {
    ESId id = ESId.intern(name);
    NativeRegexp fun = new NativeRegexp(name, n, len);

    proto.put(id, fun, DONT_ENUM);
  }

  public ESBase call(Call eval, int length) throws Throwable
  {
    switch (n) {
    case NEW:
      return create(eval, length);

    case TO_STRING:
      try {
        ESRegexp regexp = (ESRegexp) eval.getThis();
        String s = regexp.pattern.toString();
        String f = regexp.flags.toString();

        return ESString.create("/" + s + "/" + f);
      } catch (ClassCastException e) {
        throw new ESException("toString expected regexp object");
      }

    case EXEC:
      return exec(eval, length);

    case COMPILE:
      return compile(eval, length);

    case TEST:
      return test(eval, length);

    default:
      throw new ESException("Unknown object function");
    }
  }

  private ESBase create(Call eval, int length) throws Throwable
  {
    ESString pattern;
    ESString flags = null;

    if (length == 0)
      pattern = ESString.NULL;
    else
      pattern = eval.getArg(0).toStr();

    if (length > 1)
      flags =  eval.getArg(1).toStr();
    else
      flags = ESString.NULL;

    ESObject obj;
    obj = new ESRegexp(pattern, flags);

    return obj;
  }

  private ESBase compile(Call eval, int length) throws Throwable
  {
    ESString pattern;
    ESString flags = null;
    ESBase arg = eval.getArg(-1);

    if (arg instanceof ESThunk)
      arg = ((ESThunk) arg).toObject();

    if (! (arg instanceof ESRegexp))
      throw new ESException("compile must be bound to regexp");
    ESRegexp regexp = (ESRegexp) arg;

    if (length == 0)
      return esUndefined;
    else
      pattern = eval.getArg(0).toStr();

    if (length > 1)
      flags =  eval.getArg(1).toStr();
    else
      flags = ESString.NULL;

    regexp.compile(pattern, flags);

    return regexp;
  }

  static ESBase exec(Call eval, int length) throws Throwable
  {
    ESBase reg = eval.getArg(-1);
    ESRegexp regexp;

    if (reg instanceof ESThunk)
      reg = ((ESThunk) reg).toObject();

    if (reg instanceof ESRegexp)
      regexp = (ESRegexp) reg;
    else
      regexp = new ESRegexp(reg.toStr(), ESString.NULL);
    if (regexp.prototype == null)
      throw new RuntimeException();

    ESString string;

    Global global = Global.getGlobalProto();
    if (length == 0)
      string = global.getRegexp().getProperty(global.getRegexp().INPUT).toStr();
    else
      string = eval.getArg(0).toStr();

    global.getRegexp().setRegexp(regexp);
    if (! regexp.exec(string))
      return esNull;

    return esNull;
    /* java.util.regex
      
    ESArray array = global.createArray();
    for (int i = 0; i < regexp.regexp.length(); i++) {
      int begin = regexp.regexp.getBegin(i);
      int end = regexp.regexp.getEnd(i);
      if (begin < end && begin >= 0)
        array.setProperty(i, string.substring(begin, end));
      else
        array.setProperty(i, ESString.create("")); // XXX: possible?
    }

    // java.util.regex
    // array.setProperty(INDEX, ESNumber.create(regexp.regexp.getBegin(0)));
    array.setProperty(INPUT, string);

    return array;
    */
  }

  private ESBase test(Call eval, int length) throws Throwable
  {
    ESBase reg = eval.getArg(-1);
    ESRegexp regexp;

    if (reg instanceof ESThunk)
      reg = ((ESThunk) reg).toObject();

    if (reg instanceof ESRegexp)
      regexp = (ESRegexp) reg;
    else
      regexp = new ESRegexp(reg.toStr(), ESString.NULL);
    if (regexp.prototype == null)
      throw new RuntimeException();

    ESString string;
    ESRegexpWrapper globalRegexp = Global.getGlobalProto().getRegexp();
    if (length == 0)
      string = globalRegexp.getProperty(globalRegexp.INPUT).toStr();
    else
      string = eval.getArg(0).toStr();

    globalRegexp.setRegexp(regexp);
    return ESBoolean.create(regexp.exec(string));
  }
}
