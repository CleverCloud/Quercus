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

import com.caucho.vfs.WriteStream;

import java.io.IOException;

/**
 * JavaScript object
 */
class NativeFile extends Native {
  static ESId IN = ESId.intern("in");
  static ESId OUT = ESId.intern("out");

  static final int WRITE = 2;
  static final int WRITELN = 3;
  static final int FLUSH = 4;
  static final int CLOSE = 5;

  private NativeFile(String name, int n, int len)
  {
    super(name, len);

    this.n = n;
  }

  static void create(Global resin)
  {
    put(resin, "write", WRITE, 0, DONT_ENUM);
    put(resin, "writeln", WRITELN, 0, DONT_ENUM);
    put(resin, "flush", FLUSH, 0, DONT_ENUM);
    put(resin, "close", CLOSE, 0, DONT_ENUM);
  }
  
  private static void put(Global resin, String name, int n, int len, 
                          int flags)
  {
    ESId id = ESId.intern(name);

    resin.addProperty(id, new NativeFile(name, n, len));
  }

  public ESBase call(Call eval, int length) throws Throwable
  {
    ESBase evalThis = eval.getArg(-1);
    WriteStream stream = null;

    try {
      stream = (WriteStream) evalThis.toJavaObject();
    } catch (Exception e) {
    }

    if (stream == null) {
      ESBase out = evalThis.hasProperty(OUT);

      if (out != null) {
        eval.setThis(out);
        return out.call(eval, length, id);
      }
    }
    
    switch (n) {
    case WRITE:
      return write(eval, length);

    case WRITELN:
      return writeln(eval, length);

    case FLUSH:
      return flush(eval, length);

    case CLOSE:
      return close(eval, length);

    default:
      throw new ESException("Unknown file function");
    }
  }

  private static WriteStream getWriteStream(Call eval) throws Throwable
  {
    ESBase evalThis = eval.getArg(-1);
    WriteStream stream = null;

    try {
      stream = (WriteStream) evalThis.toJavaObject();
    } catch (Exception e) {
    }

    if (stream != null)
      return stream;

    ESBase obj = evalThis.hasProperty(OUT);

    try {
      stream = (WriteStream) obj.toJavaObject();
    } catch (Exception e) {
    }

    obj = Global.getGlobalProto().getGlobal().hasProperty(OUT);

    try {
      stream = (WriteStream) obj.toJavaObject();
    } catch (Exception e) {
    }

    if (stream == null)
      throw new ESException("expected file as `this' or as value of `" + OUT + "'");

    return stream;
  }

  static public ESBase write(Call eval, int length) throws Throwable
  {
    WriteStream stream = getWriteStream(eval);

    try {
      for (int i = 0; i < length; i++)
        stream.print(eval.getArg(i).toString());
    } catch (IOException e) {
      return ESBoolean.FALSE;
    }

    return eval.getArg(-1);
  }

  static public ESBase writeln(Call eval, int length) throws Throwable
  {
    WriteStream stream = getWriteStream(eval);

    try {
      for (int i = 0; i < length; i++)
        stream.print(eval.getArg(i).toString());
      stream.println();
    } catch (IOException e) {
      return ESBoolean.FALSE;
    }

    return eval.getArg(-1);
  }

  static public ESBase flush(Call eval, int length) throws Throwable
  {
    WriteStream stream = getWriteStream(eval);

    try {
      stream.flush();
    } catch (IOException e) {
      return ESBoolean.FALSE;
    }

    return eval.getArg(-1);
  }

  static public ESBase close(Call eval, int length) throws Throwable
  {
    WriteStream stream = getWriteStream(eval);

    try {
      stream.close();
    } catch (IOException e) {
      return ESBoolean.FALSE;
    }

    return eval.getArg(-1);
  }
}
