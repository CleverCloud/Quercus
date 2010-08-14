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

import com.caucho.es.parser.Parser;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JavaScript object
 */
class NativeFunction extends Native {
  static final int NEW = 1;
  static final int TO_STRING = NEW + 1;
  static final int CALL = TO_STRING + 1;
  static final int APPLY = CALL + 1;

  static ESId LENGTH = ESId.intern("length");

  /**
   * Create a new object based on a prototype
   */
  private NativeFunction(String name, int n, int len)
  {
    super(name, len);

    this.n = n;
  }

  /**
   * Creates the native Function object
   *
   * XXX: incomplete
   */
  static NativeWrapper create(Global resin)
  {
    ESBase []scope = new ESBase[] { resin.getGlobalProto() };
    ESClosure funProto = new ESClosure(scope, 1);
    funProto.name = ESId.intern("Function");

    Native natFunction = new NativeFunction("Function", NEW, 1);
    NativeWrapper function = new NativeWrapper(resin, natFunction,
                                               funProto, ESThunk.FUN_THUNK);
    resin.funProto = funProto;
    
    put(funProto, "toString", TO_STRING, 0);
    put(funProto, "call", CALL, 1);
    put(funProto, "apply", APPLY, 2);
    funProto.prototype = resin.objProto;

    funProto.setClean();
    function.setClean();

    return function;
  }

  private static void put(ESObject proto, String name, int n, int len)
  {
    ESId id = ESId.intern(name);

    proto.put(id, new NativeFunction(name, n, len), DONT_ENUM);
  }

  public ESBase call(Call eval, int length) throws Throwable
  {
    switch (n) {
    case NEW:
      return createAnonymous(eval, length);

      // Object prototype stuff
    case TO_STRING:
      // XXX: Is this correct?  Test.
      if (eval.getThis() instanceof ESClosure) {
        ESClosure closure = (ESClosure) eval.getThis();

        return ESString.create(closure.decompile());
      } else if (eval.getThis() instanceof NativeWrapper) {
        NativeWrapper wrapper = (NativeWrapper) eval.getThis();

        return wrapper.fun.toStr();
      } else
      throw new ESException("to string bound to function: " +
                          eval.getThis().getClass());

    case CALL:
      int oldTop = eval.top;
      ESBase fun = eval.getArg(-1);
      ESBase callThis = null;

      try {
        if (length > 0) {
          callThis = eval.getArg(0);
        } else
          callThis = esNull;

        if (callThis == esNull || callThis == esUndefined ||
            callThis == esEmpty)
          eval.setArg(0, eval.getGlobal());
        else
          eval.setArg(0, callThis.toObject());
        eval.top++;

        return fun.call(eval, length > 0 ? length - 1 : 0);
      } finally {
        eval.top = oldTop;
      }

    case APPLY:
      return apply(eval, length);

    default:
      throw new ESException("Unknown object function");
    }
  }

  /**
   * Create a new parser instance.
   */
  private ESClosure parseFunction(Call eval, int length) throws Throwable
  {
    StringBuffer sbuf = new StringBuffer();

    sbuf.append("function anonymous(");
    ArrayList argList = new ArrayList();
    for (int i = 0; i < length - 1; i++) {
      if (i != 0)
        sbuf.append(",");
      String str = eval.getArg(i).toString();
      int j = 0;
      int p = 0;
      
      while ((p = str.indexOf(',', j)) >= 0 ||
             (p = str.indexOf(' ', j)) >= 0) {
        if (j < p)
          argList.add(ESId.intern(str.substring(j, p)));
        j = p + 1;
      }
      if (j < str.length())
        argList.add(ESId.intern(str.substring(j)));
      
      sbuf.append(str);
    }
    ESId []args = new ESId[argList.size()];
    argList.toArray(args);
    sbuf.append("){");
    if (length > 0)
      sbuf.append(eval.getArg(length - 1).toString());
    sbuf.append("}\n");
    sbuf.append("return anonymous();");

    Global resin = Global.getGlobalProto();
    Script script = null;
    try {
      Parser parser = new Parser();
      ReadStream is = Vfs.openString(sbuf.toString());
      script = parser.parse(is, "anonymous", 1);
      is.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    ESCallable jsClass = script.initClass(resin, eval.getGlobal());

    // It's known that the function will be #2.
    ESClosure fun = new ESClosure(ESId.intern("anonymous"), jsClass,
                                  null, 2, args, eval.getGlobal());
    
    return fun;
  }

  private ESBase createAnonymous(Call eval, int length) throws Throwable
  {
    return parseFunction(eval, length);
  }

  private ESBase apply(Call eval, int length) throws Throwable
  {
    Global resin = Global.getGlobalProto();
    Call call = eval.getCall();

    call.top = 1;
    call.global = eval.global;
    call.caller = eval;

    ESBase fun = eval.getArg(-1);
    ESBase callThis = null;

    if (length > 0) {
      callThis = eval.getArg(0);
    } else
      callThis = esNull;

    if (callThis == esNull || callThis == esUndefined ||
        callThis == esEmpty)
      call.setArg(-1, eval.getGlobal());
    else
      call.setArg(-1, callThis.toObject());

    int j = 0;
    for (int i = 1; i < length; i++) {
      ESBase arg = eval.getArg(i);

      if (arg == esNull || arg == esUndefined || arg == esEmpty)
        continue;

      ESBase arglen = arg.hasProperty(LENGTH);

      if (arglen == null)
        call.setArg(j++, arg);
      else {
        int len = arglen.toInt32();

        if (j + len > call.stack.length - 2)
          throw new ESException("stack overflow");

        for (int k = 0; k < len; k++)
          call.setArg(j++, arg.getProperty(ESString.create(k)));

        if (len < 0)
          call.setArg(j++, arg);
      }
    }

    ESBase value = fun.call(call, j);

    resin.freeCall(call);

    return value;
  }
}
