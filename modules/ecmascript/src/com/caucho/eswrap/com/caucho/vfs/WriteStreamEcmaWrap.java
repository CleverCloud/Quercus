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
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.ReadWritePair;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;

public class WriteStreamEcmaWrap {
  public static void writeByte(WriteStream os, int ch)
  throws Throwable
  {
    os.write(ch);
  }

  public static void write(WriteStream os, Call call, int length)
  throws Throwable
  {
    for (int i = 0; i < length; i++) {
      String string = call.getArgString(i, length);

      if (string == null)
        string = "null";

      os.print(string);
    }
  }

  public static void writeln(WriteStream os, Call call, int length)
  throws Throwable
  {
    for (int i = 0; i < length; i++) {
      String string = call.getArgString(i, length);

      if (string == null)
        string = "null";

      if (i + 1 == length)
        os.println(string);
      else
        os.print(string);
    }

    if (length == 0)
      os.println();
  }

  public static void printf(WriteStream os, Call eval, int length)
    throws Throwable
  {
    if (length == 0)
      return;

    String result = eval.printf(length);
    
    os.print(result);
  }

  public static void writeFile(WriteStream os, Path path)
    throws IOException
  {
    ReadStream stream = path.openRead();
    
    try {
      os.writeStream(stream);
    } finally {
      stream.close();
    }
  }

  public static void writeStream(WriteStream os, Call call, int length)
  throws Throwable
  {
    if (length < 1)
      return;

    char []buf = new char[256];
    int len;

    Object obj = call.getArgObject(0, length);
    if (obj instanceof ReadStream) {
      ReadStream is = (ReadStream) obj;
      os.writeStream(is);
    }
    else if (obj instanceof ReadWritePair) {
      os.writeStream(((ReadWritePair) obj).getReadStream());
    }
    else if (obj instanceof InputStream) {
      os.writeStream((InputStream) obj);
    }
    else
      throw new IllegalArgumentException("expected stream at " +
                                         obj.getClass().getName());
  }
}

