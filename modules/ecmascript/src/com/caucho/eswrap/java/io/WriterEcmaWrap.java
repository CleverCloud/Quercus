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

package com.caucho.eswrap.java.io;

import com.caucho.es.Call;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

public class WriterEcmaWrap {
  public static void writeByte(Writer os, int ch)
  throws Exception
  {
    os.write((char) ch);
  }

  public static void write(Writer os, Call call, int length)
  throws Exception
  {
    for (int i = 0; i < length; i++) {
      os.write(call.getArg(i, length).toString());
    }
  }

  public static void writeln(Writer os, Call call, int length)
  throws Exception
  {
    for (int i = 0; i < length; i++) {
      os.write(call.getArg(i, length).toString());
    }

    os.write('\n');
  }

  public static void printf(Writer os, Call eval, int length)
    throws Throwable
  {
    if (length == 0)
      return;

    String result = eval.printf(length);
    
    os.write(result);
  }

  public static void writeFile(Writer os, Path path)
    throws IOException
  {
    char []buf = new char[256];

    ReadStream stream = path.openRead();
    try {
      int length;
      while ((length = stream.read(buf, 0, buf.length)) > 0) {
        os.write(buf, 0, length);
      }
    } finally {
      stream.close();
    }
  }

  public static void writeStream(Writer os, Call call, int length)
  throws Throwable
  {
    if (length < 1)
      return;

    char []buf = new char[256];
    int len;

    Object obj = call.getArgObject(0, length);
    if (obj instanceof ReadStream) {
      ReadStream is = (ReadStream) obj;
      while ((len = is.read(buf, 0, buf.length)) > 0) {
        os.write(buf, 0, len);
      }
    }
    else if (obj instanceof InputStream) {
      int ch;
      InputStream is = (InputStream) obj;
      while ((ch = is.read()) >= 0)
        os.write(ch);
    }
    else if (obj instanceof Reader) {
      int ch;
      Reader is = (Reader) obj;
      while ((len = is.read(buf, 0, buf.length)) > 0) {
        os.write(buf, 0, len);
      }
    }
    else
      throw new IllegalArgumentException("expected stream at " +
                                         obj.getClass().getName());
  }
}

