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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.mysql;

import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import javax.sql.*;

/**
 * Special Quercus Mysql connection.
 */
class MysqlWriter {
  protected static final Logger log
    = Logger.getLogger(MysqlWriter.class.getName());
  protected static final L10N L = new L10N(MysqlWriter.class);

  private MysqlConnectionImpl _conn;
  private WriteStream _out;

  private int _packetLength;
  private int _packetNumber;

  MysqlWriter(MysqlConnectionImpl conn, WriteStream out)
  {
    _conn = conn;
    _out = out;
  }

  void startVariablePacket()
    throws IOException
  {
    WriteStream out = _out;

    out.flush();

    assert(out.getBufferOffset() == 0);
    out.setBufferOffset(4);
  }

  void endVariablePacket()
    throws IOException
  {
    WriteStream out = _out;

    int len = out.getBufferOffset() - 4;

    byte []buffer = out.getBuffer();

    buffer[0] = (byte) len;
    buffer[1] = (byte) (len >> 8);
    buffer[2] = (byte) (len >> 16);
    buffer[3] = 1; // packet id
  }

  void writeByte(int value)
    throws IOException
  {
    WriteStream out = _out;

    out.write(value);
  }

  void writeInt(int value)
    throws IOException
  {
    WriteStream out = _out;

    out.write(value);
    out.write(value >> 8);
    out.write(value >> 16);
    out.write(value >> 24);
  }

  void writeZero(int len)
    throws IOException
  {
    WriteStream out = _out;

    for (int i = 0; i < len; i++)
      out.write(0);
  }

  void writeNullTermString(String s)
    throws IOException
  {
    WriteStream out = _out;

    int len = s.length();

    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      out.write((byte) ch);
    }
    out.write(0);
  }

  void write(String s)
    throws IOException
  {
    WriteStream out = _out;

    out.printLatin1(s);
  }

  void writeLengthCodedBinary(long value)
    throws IOException
  {
    WriteStream out = _out;

    if (value <= 250)
      out.write((int) value);
    else if (value <= 0xffff) {
      out.write(252);
      out.write((int) (value));
      out.write((int) (value >> 8));
    }
    else if (value <= 0xffffff) {
      out.write(253);
      out.write((int) (value));
      out.write((int) (value >> 8));
      out.write((int) (value >> 16));
    }
    else {
      out.write(254);
      out.write((int) value);
      out.write((int) (value >> 8));
      out.write((int) (value >> 16));
      out.write((int) (value >> 24));
      out.write((int) (value >> 32));
      out.write((int) (value >> 40));
      out.write((int) (value >> 48));
      out.write((int) (value >> 56));
    }
  }

  void write(byte []buffer, int offset, int length)
    throws IOException
  {
    WriteStream out = _out;

    out.write(buffer, offset, length);
  }

  void flush()
    throws IOException
  {
    WriteStream out = _out;

    out.flush();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _out + "]";
  }
}
