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
import com.caucho.es.ESBase;
import com.caucho.es.ESException;
import com.caucho.util.Exit;
import com.caucho.util.ExitListener;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.ReadWritePair;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

public class PathEcmaWrap {
  public static ReadStream openRead(Path p)
    throws IOException
  {
    ReadStream s = p.openRead();
    Exit.addExit(exitInputStream, s);
    return s;
  }

  public static WriteStream openWrite(Path p)
    throws IOException
  {
    WriteStream s = p.openWrite();
    Exit.addExit(exitOutputStream, s);
    return s;
  }

  public static ReadWritePair openReadWrite(Path p)
    throws IOException
  {
    ReadWritePair s = p.openReadWrite();
    Exit.addExit(exitStream, s);
    return s;
  }

  public static WriteStream openAppend(Path p)
    throws IOException
  {
    WriteStream s = p.openAppend();
    Exit.addExit(exitOutputStream, s);
    return s;
  }

  public static boolean remove(Path p)
    throws IOException
  {
    return p.remove();
  }

  public static boolean renameTo(Path p, ESBase dst)
    throws IOException
  {
    Object value;

    try {
      value = dst.toJavaObject();
    } catch (ESException e) {
      return false;
    }

    if (value == null)
      return false;

    if (value instanceof Path)
      return p.renameTo((Path) value);
    else {
      Path top = p.getParent().lookup(value.toString());
      return p.renameTo(top);
    }
  }

  // XXX: bogus for javascript
  public static Path call(Path p, String name)
    throws IOException
  {
    Path path = p.lookup(name);

    return path;
  }

  public static Path call(Path p)
    throws IOException
  {
    return p;
  }

  public static Iterator keys(Path p)
  {
    try {
      return p.iterator();
    } catch (IOException e) {
      return null;
    }
  }

  public static void write(Path path, Call call, int length)
    throws Throwable
  {
    WriteStream s = path.openWrite();

    try {
      for (int i = 0; i < length; i++) {
        String string = call.getArgString(i, length);

        s.print(string);
      }
    } finally {
      s.close();
    }
  }

  public static void writeln(Path path, Call call, int length)
  throws Throwable
  {
    WriteStream s = path.openWrite();

    try {
      for (int i = 0; i < length; i++) {
        String string = call.getArgString(i, length);

        s.print(string);
      }

      s.print('\n');
    } finally {
      s.close();
    }
  }

  public static void writeStream(Path path, InputStream is)
  throws Throwable
  {
    WriteStream s = path.openWrite();

    try {
      s.writeStream(is);
    } finally {
      s.close();
    }
  }

  public static void writeFile(Path path, Path file)
  throws Throwable
  {
    WriteStream s = path.openWrite();

    try {
      s.writeFile(file);
    } finally {
      s.close();
    }
  }

  public static void append(Path path, Call call, int length)
  throws Throwable
  {
    WriteStream s = path.openAppend();

    try {
      for (int i = 0; i < length; i++) {
        String string = call.getArgString(i, length);

        s.print(string);
      }
    } finally {
      s.close();
    }
  }

  public static void appendln(Path path, Call call, int length)
  throws Throwable
  {
    WriteStream s = path.openAppend();

    try {
      for (int i = 0; i < length; i++) {
        String string = call.getArgString(i, length);

        s.print(string);
      }

      s.print('\n');
    } finally {
      s.close();
    }
  }

  public static void appendStream(Path path, InputStream is)
  throws Throwable
  {
    WriteStream s = path.openAppend();

    try {
      s.writeStream(is);
    } finally {
      s.close();
    }
  }

  public static void appendFile(Path path, Path file)
  throws Throwable
  {
    WriteStream s = path.openAppend();

    try {
      s.writeFile(file);
    } finally {
      s.close();
    }
  }

  private static ExitListener exitStream = new ExitListener() {
    public void handleExit(Object o)
    {
      ReadWritePair pair = (ReadWritePair) o;

      pair.getReadStream().close();

      try {
        pair.getWriteStream().close();
      } catch (IOException e) {
      }
    }
  };

  private static ExitListener exitInputStream = new ExitListener() {
    public void handleExit(Object o)
    {
      InputStream stream = (InputStream) o;

      try {
        stream.close();
      } catch (IOException e) {
      }
    }
  };

  private static ExitListener exitOutputStream = new ExitListener() {
    public void handleExit(Object o)
    {
      OutputStream stream = (OutputStream) o;

      try {
        stream.close();
      } catch (IOException e) {
      }
    }
  };
}
