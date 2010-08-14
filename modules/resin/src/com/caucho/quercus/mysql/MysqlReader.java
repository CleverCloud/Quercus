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
class MysqlReader {
  protected static final Logger log
    = Logger.getLogger(MysqlReader.class.getName());
  protected static final L10N L = new L10N(MysqlReader.class);

  private MysqlConnectionImpl _conn;
  private ReadStream _is;

  private int _packetLength;
  private int _packetNumber;
  private int _packetOffset;

  private byte []_header = new byte[4];

  MysqlReader(MysqlConnectionImpl conn, ReadStream is)
  {
    _conn = conn;
    _is = is;
  }

  void readPacket()
    throws SQLException
  {
    try {
      if (_packetLength > 0)
        skipToEnd();

      int len = _is.readAll(_header, 0, _header.length);

      if (len < _header.length)
        throw new SQLException("end of file");

      _packetLength = (((_header[0] & 0xff) << 0)
                       + ((_header[1] & 0xff) << 8)
                       + ((_header[2] & 0xff) << 16));

      _packetNumber = _header[3];
      _packetOffset = 0;

      assert(_packetOffset <= _packetLength);
    } catch (IOException e) {
      throw new SQLException(e);
    }
  }

  String readTailString()
    throws IOException
  {
    StringBuilder sb = new StringBuilder();

    int len = getPacketLength() - getPacketOffset();
    for (int i = 0; i < len; i++) {
      int ch = readByte();

      if (ch > 0)
        sb.append((char) ch);
    }

    return sb.toString();
  }

  int readByte()
    throws IOException
  {
    int value = _is.read();

    _packetOffset++;

    assert(_packetOffset <= _packetLength);

    return value;
  }

  String readNullTermString()
    throws IOException
  {
    StringBuilder sb = new StringBuilder();

    ReadStream is = _is;
    int packetOffset = _packetOffset;

    int ch;
    while ((ch = is.read()) > 0) {
      sb.append((char) ch);
      packetOffset++;
    }

    if (ch == 0)
      packetOffset++;

    _packetOffset = packetOffset;

    if (ch < 0)
      eof();

    assert(_packetOffset <= _packetLength);

    return sb.toString();
  }

  int readInt()
    throws IOException
  {
    ReadStream is = _is;

    int ch1 = is.read();
    int ch2 = is.read();
    int ch3 = is.read();
    int ch4 = is.read();

    if (ch4 < 0)
      eof();

    _packetOffset += 4;

    assert(_packetOffset <= _packetLength);

    return ((ch1 << 0)
            + (ch2 << 8)
            + (ch3 << 16)
            + (ch4 << 24));
  }

  int readShort()
    throws IOException
  {
    ReadStream is = _is;

    int ch1 = is.read();
    int ch2 = is.read();

    if (ch2 < 0)
      eof();

    _packetOffset += 2;

    assert(_packetOffset <= _packetLength);

    return ((ch1 << 0)
            + (ch2 << 8));
  }

  long readLengthCodedBinary()
    throws IOException
  {
    ReadStream is = _is;

    int ch = is.read();

    _packetOffset++;

    if (ch <= 250) {
      assert(_packetOffset <= _packetLength);

      return ch;
    }

    return readLengthCodedBinary(ch);
  }

  long readLengthCodedBinary(int ch)
    throws IOException
  {
    ReadStream is = _is;

    if (ch <= 250) {
      return ch;
    }
    else if (ch == 252) {
      _packetOffset += 2;

      assert(_packetOffset <= _packetLength);

      int len = (is.read() | (is.read() << 8));

      return len;
    }
    else if (ch == 253) {
      _packetOffset += 3;

      assert(_packetOffset <= _packetLength);

      int len = (is.read()
                 | (is.read() << 8)
                 | (is.read() << 16));

      return len;
    }
    else if (ch == 254) {
      _packetOffset += 8;

      assert(_packetOffset <= _packetLength);

      int len = (is.read()
                 | (is.read() << 8)
                 | (is.read() << 16)
                 | (is.read() << 24)
                 | (is.read() << 32)
                 | (is.read() << 40)
                 | (is.read() << 48)
                 | (is.read() << 56));

      return len;
    }
    else
      throw new IllegalStateException();
  }

  int readAll(byte []buffer, int offset, int length)
    throws IOException
  {
    int len = _is.readAll(buffer, offset, length);

    if (len < length)
      eof();

    _packetOffset += len;

    assert(_packetOffset <= _packetLength);

    return len;
  }

  int readAll(char []buffer, int offset, int length)
    throws IOException
  {
    int len = _is.readAll(buffer, offset, length);

    if (len < length)
      eof();

    _packetOffset += len;

    assert(_packetOffset <= _packetLength);

    return len;
  }

  int readData(OutputStream os, int length)
    throws IOException
  {
    _is.writeToStream(os, length);

    _packetOffset += length;

    return length;
  }

  void skip(int length)
    throws IOException
  {
    _is.skip(length);

    _packetOffset += length;

    assert(_packetOffset <= _packetLength);
  }

  int getPacketOffset()
  {
    return _packetOffset;
  }

  int getPacketLength()
  {
    return _packetLength;
  }

  private void eof()
    throws IOException
  {
    throw new IOException("unexpected end of file");
  }

  void endPacket()
    throws SQLException
  {
    skipToEnd();
  }

  private void skipToEnd()
    throws SQLException
  {
    try {
      int len = _packetLength - _packetOffset;

      _is.skip(len);

      _packetOffset = 0;
      _packetLength = 0;
    } catch (IOException e) {
      throw new SQLException(e);
    }
  }

  public void close()
  {
    ReadStream is = _is;
    _is = null;

    IoUtil.close(is);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _is + "]";
  }
}
