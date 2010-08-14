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

package com.caucho.eswrap.com.caucho.vfs;

import com.caucho.es.Call;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.ReadWritePair;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class ReadWritePairEcmaWrap {
  public static void writeByte(ReadWritePair s, int ch)
  throws Throwable
  {
    s.getWriteStream().write(ch);
  }

  public static void write(ReadWritePair s, Call call, int length)
  throws Throwable
  {
    WriteStream os = s.getWriteStream();
    for (int i = 0; i < length; i++) {
      String string = call.getArgString(i, length);

      os.print(string);
    }
  }

  public static void writeln(ReadWritePair s, Call call, int length)
  throws Throwable
  {
    WriteStream os = s.getWriteStream();
    for (int i = 0; i < length; i++) {
      String string = call.getArgString(i, length);

      os.print(string);
    }

    os.println();
  }

  public static void writeStream(ReadWritePair s, Call call, int length)
  throws Throwable
  {
    if (length < 1)
      return;

    WriteStream os = s.getWriteStream();
    Object obj = call.getArgObject(0, length);

    if (obj instanceof InputStream)
      os.writeStream((InputStream) obj);
    else if (obj instanceof ReadWritePair)
      os.writeStream(((ReadWritePair) obj).getReadStream());
    else
      throw new IllegalArgumentException("expected read stream at " +
                                         obj.getClass().getName());
  }

  public static WriteStream getOutputStream(ReadWritePair s)
  {
    return s.getWriteStream();
  }

  public static void printf(ReadWritePair s, Call eval, int length)
    throws Throwable
  {
    if (length == 0)
      return;

    WriteStream os = s.getWriteStream();
    String result = eval.printf(length);
    
    os.print(result);
  }

  public static int readByte(ReadWritePair s)
  throws IOException
  {
    ReadStream is = s.getReadStream();
    return is.read();
  }

  public static String read(ReadWritePair s)
  throws IOException
  {
    ReadStream is = s.getReadStream();
    int ch = is.readChar();

    if (ch < 0)
      return null;

    return String.valueOf((char) ch);
  }

  public static String read(ReadWritePair s, int length)
  throws IOException
  {
    ReadStream is = s.getReadStream();
    CharBuffer cb = new CharBuffer();

    for (; length > 0; length--) {
      int ch = is.readChar();

      if (ch < 0)
        break;

      cb.append((char) ch);
    }

    if (cb.length() == 0)
      return null;

    return cb.toString();
  }

  public static int getAvailable(ReadWritePair s)
    throws IOException
  {
    return s.getReadStream().available();
  }

  public static int available(ReadWritePair s)
    throws IOException
  {
    return s.getReadStream().available();
  }

  public static String readAvailable(ReadWritePair s, int length)
    throws IOException
  {
    ReadStream is = s.getReadStream();
    CharBuffer cb = new CharBuffer();

    while (is.getAvailable() > 0) {
      int ch = is.readChar();

      if (ch < 0)
        break;

      cb.append((char) ch);
    }

    if (cb.length() == 0)
      return null;

    return cb.toString();
  }

  public static String readln(ReadWritePair s)
  throws IOException
  {
    ReadStream is = s.getReadStream();
    CharBuffer cb = new CharBuffer();

    if (! is.readln(cb))
      return null;

    return cb.toString();
  }

  public static ReadStream getInputStream(ReadWritePair s)
  {
    return s.getReadStream();
  }

  public static void setAttribute(ReadWritePair s, String key, Object value)
    throws IOException
  {
    s.getWriteStream().setAttribute(key, value);
  }

  public static void removeAttribute(ReadWritePair s, String key)
    throws IOException
  {
    s.getWriteStream().removeAttribute(key);
  }

  public static Object getAttribute(ReadWritePair s, String key)
    throws IOException
  {
    return s.getReadStream().getAttribute(key);
  }

  public static Iterator getAttributeNames(ReadWritePair s)
    throws IOException
  {
    return s.getReadStream().getAttributeNames();
  }

  public static void flush(ReadWritePair s)
  throws IOException
  {
    s.getWriteStream().flush();
  }

  public static void close(ReadWritePair s)
  throws IOException
  {
    s.getWriteStream().close();
    s.getReadStream().close();
  }
}

