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

package com.caucho.bytecode;

import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a generic attribute
 */
public class ExceptionsAttribute extends Attribute {
  private ArrayList<String> _exceptions = new ArrayList<String>();

  ExceptionsAttribute(String name)
  {
    super(name);
  }

  /**
   * Adds an exception
   */
  public void addException(String exn)
  {
    _exceptions.add(exn);
  }

  /**
   * Returns the exceptions.
   */
  public ArrayList<String> getExceptionList()
  {
    return _exceptions;
  }

  /**
   * Writes the field to the output.
   */
  public void read(ByteCodeParser in)
    throws IOException
  {
    int length = in.readInt();
    
    int exnCount = in.readShort();

    for (int i = 0; i < exnCount; i++) {
      int index = in.readShort();

      if (index == 0)
        _exceptions.add(null);
      
      _exceptions.add(in.getConstantPool().getClass(index).getName());
    }
  }

  /**
   * Writes the field to the output.
   */
  public void write(ByteCodeWriter out)
    throws IOException
  {
    out.writeUTF8Const(getName());

    TempStream ts = new TempStream();
    ts.openWrite();
    WriteStream ws = new WriteStream(ts);
    ByteCodeWriter o2 = new ByteCodeWriter(ws, out.getJavaClass());

    o2.writeShort(_exceptions.size());
    for (int i = 0; i < _exceptions.size(); i++) {
      String exn = _exceptions.get(i);

      o2.writeClass(exn);
    }
    
    ws.close();
    
    out.writeInt(ts.getLength());
    
    TempBuffer ptr = ts.getHead();

    for (; ptr != null; ptr = ptr.getNext())
      out.write(ptr.getBuffer(), 0, ptr.getLength());

    ts.destroy();
  }

  /**
   * Clones the attribute
   */
  public Attribute export(JavaClass cl, JavaClass target)
  {
    ConstantPool cp = target.getConstantPool();

    cp.addUTF8(getName());
    
    ExceptionsAttribute attr = new ExceptionsAttribute(getName());

    for (int i = 0; i < _exceptions.size(); i++) {
      String exn = _exceptions.get(i);

      cp.addClass(exn);

      attr.addException(exn);
    }

    return attr;
  }

  public String toString()
  {
    return "ExceptionsAttribute[" + getName() + "]";
  }
}
