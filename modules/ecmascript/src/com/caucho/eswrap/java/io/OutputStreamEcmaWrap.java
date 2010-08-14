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
import com.caucho.vfs.ReadWritePair;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OutputStreamEcmaWrap {
  public static void writeByte(OutputStream os, int ch)
  throws Throwable
  {
    os.write(ch);
  }

  public static void write(OutputStream os, Call call, int length)
  throws Throwable
  {
    for (int i = 0; i < length; i++) {
      String string = call.getArgString(i, length);

      if (string == null)
        string = "null";

      byte []bytes = string.getBytes();

      os.write(bytes, 0, bytes.length);
    }
  }

  public static void writeBytes(OutputStream os, byte []buffer, int offset, int length)
  throws Throwable
  {
    os.write(buffer, offset, length);
  }

  public static void writeln(OutputStream os, Call call, int length)
  throws Throwable
  {
    for (int i = 0; i < length; i++) {
      String string = call.getArgString(i, length);

      if (string == null)
        string = "null";

      byte []bytes = string.getBytes();

      os.write(bytes, 0, bytes.length);
    }

    os.write('\n');
  }

  public static void printf(OutputStream os, Call eval, int length)
    throws Throwable
  {
    if (length == 0)
      return;

    String result = eval.printf(length);
    byte []bytes = result.getBytes();
    
    os.write(bytes, 0, bytes.length);
  }

  public static void writeFile(OutputStream os, Path path)
    throws IOException
  {
    ReadStream stream = path.openRead();
    
    try {
      stream.writeToStream(os);
    } finally {
      stream.close();
    }
  }

  public static void writeStream(OutputStream os, Call call, int length)
  throws Throwable
  {
    if (length < 1)
      return;

    char []buf = new char[256];
    int len;

    Object obj = call.getArgObject(0, length);
    if (obj instanceof ReadStream) {
      ReadStream is = (ReadStream) obj;
      is.writeToStream(os);
    }
    else if (obj instanceof ReadWritePair) {
      ((ReadWritePair) obj).getReadStream().writeToStream(os);
    }
    else if (obj instanceof InputStream) {
      if (os instanceof WriteStream) {
        ((WriteStream) os).writeStream((InputStream) obj);
      }
      else {
        int ch;
        InputStream is = (InputStream) obj;
        while ((ch = is.read()) >= 0)
          os.write(ch);
      }
    }
    else
      throw new IllegalArgumentException("expected stream at " +
                                         obj.getClass().getName());
  }
}

