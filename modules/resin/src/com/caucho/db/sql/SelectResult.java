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

package com.caucho.db.sql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

import com.caucho.db.blob.BlobInputStream;
import com.caucho.db.block.BlockStore;
import com.caucho.db.table.TableIterator;
import com.caucho.db.table.Column.ColumnType;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.CharBuffer;
import com.caucho.util.FreeList;
import com.caucho.util.IntArray;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.vfs.TempBuffer;

public class SelectResult {
  private static final L10N L = new L10N(SelectResult.class);

  private static final FreeList<SelectResult> _freeList
    = new FreeList<SelectResult>(32);

  private static final int SIZE = TempBuffer.SIZE;

  private static QDate _date = new QDate();

  private CharBuffer _cb = new CharBuffer();
  private byte []_blob = new byte[128];

  private Expr []_exprs;
  private BlockStore []_stores = new BlockStore[32];

  private TableIterator []_rows = new TableIterator[16];

  private Order _order;
  private IntArray _orderIndex;

  private TempBuffer []_tempBuffers = new TempBuffer[128];
  private byte [][]_buffers = new byte[128][];
  private int _length;
  private int _rowCount;

  private int _row;

  private int _offset;
  private int _rowOffset;
  private int _columnOffset;
  private int _column;

  private boolean _wasNull;

  private SelectResult()
  {
  }

  public static SelectResult create(Expr []exprs, Order order)
  {
    SelectResult rs = _freeList.allocate();

    if (rs == null)
      rs = new SelectResult();

    rs.init(exprs, order);

    return rs;
  }

  /**
   * Initialize the iterator.
   */
  TableIterator []initRows(FromItem []fromItems)
  {
    if (_rows.length < fromItems.length)
      _rows = new TableIterator[fromItems.length];

    for (int i = 0; i < fromItems.length; i++) {
      if (_rows[i] == null)
        _rows[i] = new TableIterator();
      _rows[i].init(fromItems[i].getTable());
    }

    return _rows;
  }

  /**
   * Initialize based on the exprs.
   */
  private void init(Expr []exprs, Order order)
  {
    _exprs = exprs;
    _order = order;

    if (order != null)
      _orderIndex = new IntArray();

    if (_stores.length < _exprs.length) {
      _stores = new BlockStore[exprs.length];
    }

    for (int i = 0; i < exprs.length; i++)
      _stores[i] = exprs[i].getTable();

    _length = 0;
    _rowCount = 0;
  }

  void initRead()
    throws SQLException
  {
    if (_order != null)
      _order.sort(this, _orderIndex);

    _row = -1;
    _offset = 0;
    _column = 0;
    _rowOffset = 0;
    _columnOffset = 0;
  }

  /**
   * Moves to the next row, returning true if the row has data.
   */
  public boolean next()
    throws SQLException
  {
    if (++_row < _rowCount) {
      if (_orderIndex != null) {
        _offset = _orderIndex.get(_row);
      }
      else if (_row != 0) {
        _offset = _columnOffset;
        skipColumns(_exprs.length - _column);
      }

      _column = 0;
      _rowOffset = _offset;
      _columnOffset = _rowOffset;

      return true;
    }
    else
      return false;
  }

  /**
   * Returns the expressions.
   */
  public Expr []getExprs()
  {
    return _exprs;
  }

  /**
   * Returns the column index with the given name.
   */
  public int findColumnIndex(String name)
    throws SQLException
  {
    for (int i = 0; i < _exprs.length; i++) {
      if (_exprs[i].getName().equals(name))
        return i + 1;
    }

    throw new SQLException(L.l("column `{0}' does not exist.", name));
  }

  /**
   * Returns the string value of the given index.
   */
  public String getString(int index)
    throws SQLException
  {
    _wasNull = false;

    setColumn(index);

    int type = read();
    
    if (type < 0)
      return null;
    
    ColumnType cType = ColumnType.values()[type];

    switch (cType) {
    case NONE:
      _wasNull = true;
      return null;

    case SHORT:
      {
        int value = (short) ((read() << 8) + (read()));

        return String.valueOf(value);
      }

    case INT:
      {
        int value = ((read() << 24) +
                     (read() << 16) +
                     (read() << 8) +
                     (read()));

        return String.valueOf(value);
      }

    case LONG:
      {
        long value = (((long) read() << 56) +
                      ((long) read() << 48) +
                      ((long) read() << 40) +
                      ((long) read() << 32) +
                      ((long) read() << 24) +
                      ((long) read() << 16) +
                      ((long) read() << 8) +
                      ((long) read()));

        return String.valueOf(value);
      }

    case DOUBLE:
      {
        long value = (((long) read() << 56) +
                      ((long) read() << 48) +
                      ((long) read() << 40) +
                      ((long) read() << 32) +
                      ((long) read() << 24) +
                      ((long) read() << 16) +
                      ((long) read() << 8) +
                      ((long) read()));

        return String.valueOf(Double.longBitsToDouble(value));
      }

    case DATE:
      {
        long value = (((long) read() << 56) +
                      ((long) read() << 48) +
                      ((long) read() << 40) +
                      ((long) read() << 32) +
                      ((long) read() << 24) +
                      ((long) read() << 16) +
                      ((long) read() << 8) +
                      ((long) read()));

        return QDate.formatISO8601(value);
      }

    case VARCHAR:
      return readString();

    case BLOB:
      return readBlobString();

    case BINARY:
      {
        int len = read();

        char []chars = new char[len];

        for (int i = 0; i < len; i++) {
          chars[i] = (char) (read() & 0xff);
        }

        return new String(chars);
      }


    default:
      throw new RuntimeException("unknown column type:" + type + " column:" + index);
    }
  }

  /**
   * Returns the string value of the given index.
   */
  public byte [] getBytes(int index)
    throws SQLException
  {
    _wasNull = false;

    setColumn(index);

    int type = read();
    
    if (type < 0)
      return null;
    
    ColumnType cType = ColumnType.values()[type];

    switch (cType) {
    case NONE:
      _wasNull = true;
      return null;

    case BINARY:
      {
        int len = read();

        byte []bytes = new byte[len];

        read(bytes, 0, len);

        return bytes;
      }

    case BLOB:
      return readBlobBytes();

    default:
      throw new RuntimeException("unknown column type:" + type + " column:" + index);
    }
  }

  /**
   * Returns the integer value of the column.
   */
  public int getInt(int index)
    throws SQLException
  {
    _wasNull = false;
    setColumn(index);

    int type = read();
    
    if (type < 0)
      return 0;
    
    ColumnType cType = ColumnType.values()[type];
    
    switch (cType) {
    case NONE:
      _wasNull = true;
      return 0;

    case SHORT:
      {
        int value = (short) ((read() << 8)
                             + (read()));

        return value;
      }

    case INT:
      {
        int value = ((read() << 24) +
                     (read() << 16) +
                     (read() << 8) +
                     (read()));

        return value;
      }

    case LONG:
    case DATE:
      {
        long value = (((long) read() << 56) +
                      ((long) read() << 48) +
                      ((long) read() << 40) +
                      ((long) read() << 32) +
                      ((long) read() << 24) +
                      ((long) read() << 16) +
                      ((long) read() << 8) +
                      ((long) read()));

        return (int) value;
      }

    case DOUBLE:
      {
        long value = (((long) read() << 56) +
                      ((long) read() << 48) +
                      ((long) read() << 40) +
                      ((long) read() << 32) +
                      ((long) read() << 24) +
                      ((long) read() << 16) +
                      ((long) read() << 8) +
                      ((long) read()));

        return (int) Double.longBitsToDouble(value);
      }

    case VARCHAR:
      return Integer.parseInt(readString());

    case BLOB:
      return Integer.parseInt(readBlobString());

    default:
      return 0;
    }
  }

  /**
   * Returns the long value of the column.
   */
  public long getLong(int index)
    throws SQLException
  {
    _wasNull = false;
    setColumn(index);

    int type = read();

    if (type < 0)
      return 0;
    
    ColumnType cType = ColumnType.values()[type];

    switch (cType) {
    case NONE:
      _wasNull = true;
      return 0;

    case SHORT:
      {
        int value = (short) ((read() << 8)
                             + (read()));

        return value;
      }

    case INT:
      {
        int value = ((read() << 24) +
                     (read() << 16) +
                     (read() << 8) +
                     (read()));

        return value;
      }

    case LONG:
    case DATE:
      {
        long value = (((long) read() << 56) +
                      ((long) read() << 48) +
                      ((long) read() << 40) +
                      ((long) read() << 32) +
                      ((long) read() << 24) +
                      ((long) read() << 16) +
                      ((long) read() << 8) +
                      ((long) read()));

        return value;
      }

    case DOUBLE:
      {
        long value = (((long) read() << 56) +
                      ((long) read() << 48) +
                      ((long) read() << 40) +
                      ((long) read() << 32) +
                      ((long) read() << 24) +
                      ((long) read() << 16) +
                      ((long) read() << 8) +
                      ((long) read()));

        return (long) Double.longBitsToDouble(value);
      }

    case VARCHAR:
      return Long.parseLong(readString());

    case BLOB:
      return Long.parseLong(readBlobString());

    default:
      return 0;
    }
  }

  /**
   * Returns a double value from this column.
   */
  public double getDouble(int index)
    throws SQLException
  {
    _wasNull = false;
    setColumn(index);

    int type = read();

    if (type < 0)
      return 0;
    
    ColumnType cType = ColumnType.values()[type];
    
    switch (cType) {
    case NONE:
      _wasNull = true;
      return 0;

    case SHORT:
      {
        int value = (short) ((read() << 8)
                             + (read()));

        return value;
      }

    case INT:
      {
        int value = ((read() << 24) +
                     (read() << 16) +
                     (read() << 8) +
                     (read()));

        return value;
      }

    case LONG:
    case DATE:
      {
        long value = (((long) read() << 56) +
                      ((long) read() << 48) +
                      ((long) read() << 40) +
                      ((long) read() << 32) +
                      ((long) read() << 24) +
                      ((long) read() << 16) +
                      ((long) read() << 8) +
                      ((long) read()));

        return value;
      }

    case DOUBLE:
      {
        long value = (((long) read() << 56) +
                      ((long) read() << 48) +
                      ((long) read() << 40) +
                      ((long) read() << 32) +
                      ((long) read() << 24) +
                      ((long) read() << 16) +
                      ((long) read() << 8) +
                      ((long) read()));

        return Double.longBitsToDouble(value);
      }

    case VARCHAR:
      return Double.parseDouble(readString());

    case BLOB:
      return Double.parseDouble(readBlobString());

    default:
      return 0;
    }
  }

  public long getDate(int index)
    throws SQLException
  {
    _wasNull = false;
    setColumn(index);

    int type = read();

    if (type < 0)
      return 0;
    
    ColumnType cType = ColumnType.values()[type];
    
    switch (cType) {
    case NONE:
      _wasNull = true;
      return 0;

    case LONG:
    case DATE:
      {
        long value = (((long) read() << 56) +
                      ((long) read() << 48) +
                      ((long) read() << 40) +
                      ((long) read() << 32) +
                      ((long) read() << 24) +
                      ((long) read() << 16) +
                      ((long) read() << 8) +
                      ((long) read()));

        return value;
      }

    case INT:
      {
        long value = (((long) read() << 24) +
                      ((long) read() << 16) +
                      ((long) read() << 8) +
                      ((long) read()));

        return value;
      }

    case VARCHAR:
      {
        String value = readString();

        synchronized (_date) {
          try {
            return _date.parseDate(value);
          } catch (Exception e) {
            throw new SQLExceptionWrapper(e);
          }
        }
      }

    case BLOB:
      {
        String value = readBlobString();

        synchronized (_date) {
          try {
            return _date.parseDate(value);
          } catch (Exception e) {
            throw new SQLExceptionWrapper(e);
          }
        }
      }

    default:
      throw new SQLException("unknown type: " + type);
    }
  }

  /**
   * Returns the blob value of the given index.
   */
  public Blob getBlob(int index)
    throws SQLException
  {
    _wasNull = false;

    setColumn(index);

    int type = read();

    if (type < 0)
      return null;
    
    ColumnType cType = ColumnType.values()[type];

    switch (cType) {
    case NONE:
      _wasNull = true;
      return null;

    case BLOB:
      return getBlob();

    default:
      throw new RuntimeException("column can't be retrieved as a blob:" + type + " column:" + index);
    }
  }

  /**
   * Returns the clob value of the given index.
   */
  public Clob getClob(int index)
    throws SQLException
  {
    _wasNull = false;

    setColumn(index);

    int type = read();

    if (type < 0)
      return null;
    
    ColumnType cType = ColumnType.values()[type];

    switch (cType) {
    case NONE:
      _wasNull = true;
      return null;

    case BLOB:
      return getClob();

    default:
      throw new RuntimeException("column can't be retrieved as a clob:" + type + " column:" + index);
    }
  }

  /**
   * Returns true if the last column read was null.
   */
  public boolean wasNull()
  {
    return _wasNull;
  }

  /**
   * Returns the string value for the result set.
   */
  private String readString()
    throws SQLException
  {
    int length = ((read() << 24)
                  + (read() << 16)
                  + (read() << 8)
                  + (read()));

    int len = length >> 1;

    CharBuffer cb = _cb;
    cb.ensureCapacity(len);
    char []cBuf = cb.getBuffer();
    int cLen = 0;

    for (; len > 0; len--) {
      int ch1 = read();
      int ch2 = read();

      cBuf[cLen++] = (char) (((ch1 & 0xff) << 8) + (ch2 & 0xff));
    }

    return new String(cBuf, 0, cLen);
  }

  /**
   * Returns the blob value for the result set.
   */
  private Blob getBlob()
    throws SQLException
  {
    BlobImpl blob = new BlobImpl();

    blob.setStore(_stores[_column]);

    byte []inode = blob.getInode();

    read(inode, 0, 128);

    return blob;
  }

  /**
   * Returns the clob value for the result set.
   */
  private Clob getClob()
    throws SQLException
  {
    ClobImpl clob = new ClobImpl();

    clob.setStore(_stores[_column]);

    byte []inode = clob.getInode();

    read(inode, 0, 128);

    return clob;
  }

  /**
   * Returns the string value for the result set.
   */
  private String readBlobString()
    throws SQLException
  {
    read(_blob, 0, 128);

    CharBuffer cb = _cb;
    cb.clear();

    BlobInputStream is = null;
    try {
      is = new BlobInputStream(_stores[_column], _blob, 0);

      int ch;
      while ((ch = is.read()) >= 0) {
        if (ch < 0x80)
          cb.append((char) ch);
      }
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }

    return cb.toString();
  }

  /**
   * Returns the string value for the result set.
   */
  private byte []readBlobBytes()
    throws SQLException
  {
    read(_blob, 0, 128);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    BlobInputStream is = null;
    try {
      is = new BlobInputStream(_stores[_column], _blob, 0);

      int ch;
      while ((ch = is.read()) >= 0) {
        bos.write(ch);
      }
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }

    return bos.toByteArray();
  }

  /**
   * Set the column in the current row.
   */
  private void setColumn(int column)
  {
    if (column < _column) {
      _offset = _rowOffset;
      skipColumns(column);
    }
    else {
      _offset = _columnOffset;
      skipColumns(column - _column);
    }

    _column = column;
    _columnOffset = _offset;
  }

  /**
   * Set the column in the current row.
   */
  void setRow(int rowOffset)
  {
    _rowOffset = rowOffset;
    _offset = rowOffset;
    _column = 0;
    _columnOffset = rowOffset;
  }

  /**
   * Skips the specified number of columns.
   */
  private void skipColumns(int count)
  {
    for (; count > 0; count--) {

      int type = read();

      if (type < 0)
        return;
      
      ColumnType cType = ColumnType.values()[type];

      int sublen;

      switch (cType) {
      case NONE:
        break;

      case VARCHAR:
        int l0 = read();
        int l1 = read();
        int l2 = read();
        int l3 = read();

        sublen = ((l0 << 24) +
                  (l1 << 16) +
                  (l2 << 8) +
                  (l3));

        _offset += sublen;
        break;

      case BINARY:
        sublen = read();

        _offset += sublen;
        break;

      case SHORT:
        _offset += 2;
        break;
      case INT:
        _offset += 4;
        break;
      case LONG:
      case DOUBLE:
      case DATE:
        _offset += 8;
        break;

      case BLOB:
        _offset += 128;
        break;

      default:
        throw new RuntimeException("Unknown column type: " + type);
      }
    }
  }

  /**
   * Starts a row
   */
  public void startRow()
  {
    if (_orderIndex != null)
      _orderIndex.add(_length);

    _rowCount++;
  }

  /**
   * Writes a null.
   */
  public void writeNull()
  {
    write(ColumnType.NONE.ordinal());
  }

  /**
   * Writes a string.
   */
  public void writeString(String s)
  {
    write(ColumnType.VARCHAR.ordinal());
    int stringLength = s.length();
    int length = 2 * stringLength;
    write(length >> 24);
    write(length >> 16);
    write(length >> 8);
    write(length);

    for (int i = 0; i < stringLength; i++) {
      char ch = s.charAt(i);

      write(ch << 8);
      write(ch);
    }
  }

  /**
   * Writes a string.
   */
  public void writeString(byte []buffer, int offset, int stringLength)
  {
    int rLength = _length;

    int rOffset = rLength % SIZE;
    int rBlockId = rLength / SIZE;

    if (_buffers[rBlockId] == null) {
      TempBuffer tempBuffer = TempBuffer.allocate();
      _tempBuffers[rBlockId] = tempBuffer;
      _buffers[rBlockId] = tempBuffer.getBuffer();
    }

    byte []rBuffer = _buffers[rBlockId];
    rBuffer[rOffset] = (byte) ColumnType.VARCHAR.ordinal();

    int length = 2 * stringLength;

    if (rOffset + 5 < rBuffer.length) {
      rBuffer[rOffset + 1] = (byte) (length >> 24);
      rBuffer[rOffset + 2] = (byte) (length >> 16);
      rBuffer[rOffset + 3] = (byte) (length >> 8);
      rBuffer[rOffset + 4] = (byte) length;

      if (rOffset + 5 + length < SIZE) {
        System.arraycopy(buffer, offset, rBuffer, rOffset + 5, length);

        _length = rLength + 5 + length;
      }
      else {
        _length = rLength + 5;
        write(buffer, offset, length);
      }
    }
    else {
      _length = rLength + 1;

      write(length >> 24);
      write(length >> 16);
      write(length >> 8);
      write(length);

      write(buffer, offset, length);
    }
  }

  /**
   * Writes a binary.
   */
  public void writeBinary(byte []buffer, int offset, int length)
  {
    write(ColumnType.BINARY.ordinal());
    write(length);
    write(buffer, offset, length);
  }

  /**
   * Writes a string.
   */
  public void writeBlock(int code, byte []buffer, int offset, int length)
  {
    write(code);
    write(buffer, offset, length);
  }

  /**
   * Writes a double.
   */
  public void writeDouble(double dValue)
  {
    write(ColumnType.DOUBLE.ordinal());

    long value = Double.doubleToLongBits(dValue);

    write((int) (value >> 56));
    write((int) (value >> 48));
    write((int) (value >> 40));
    write((int) (value >> 32));
    write((int) (value >> 24));
    write((int) (value >> 16));
    write((int) (value >> 8));
    write((int) value);
  }

  /**
   * Writes a long.
   */
  public void writeLong(long value)
  {
    write(ColumnType.LONG.ordinal());
    write((int) (value >> 56));
    write((int) (value >> 48));
    write((int) (value >> 40));
    write((int) (value >> 32));
    write((int) (value >> 24));
    write((int) (value >> 16));
    write((int) (value >> 8));
    write((int) value);
  }

  /**
   * Writes a date.
   */
  public void writeDate(long value)
  {
    write(ColumnType.DATE.ordinal());
    write((int) (value >> 56));
    write((int) (value >> 48));
    write((int) (value >> 40));
    write((int) (value >> 32));
    write((int) (value >> 24));
    write((int) (value >> 16));
    write((int) (value >> 8));
    write((int) value);
  }

  /**
   * Writes an long.
   */
  public void writeInt(int value)
  {
    write(ColumnType.INT.ordinal());
    write(value >> 24);
    write(value >> 16);
    write(value >> 8);
    write(value);
  }

  /**
   * Writes a short
   */
  public void writeShort(int value)
  {
    write(ColumnType.SHORT.ordinal());
    write(value >> 8);
    write(value);
  }

  /**
   * Writes a blob.
   */
  public void writeBlob(byte []buffer, int offset)
  {
    write(ColumnType.BLOB.ordinal());

    write(buffer, offset, 128);
  }

  /**
   * Reads the next byte.
   */
  private int read()
  {
    int offset = _offset;

    if (_length <= offset)
      return -1;

    _offset = offset + 1;

    byte []buf = _buffers[offset / SIZE];

    return buf[offset % SIZE] & 0xff;
  }

  /**
   * Reads the next byte.
   */
  private int read(byte []buffer, int bufOffset, int bufLength)
  {
    int offset = _offset;
    int length = _length;
    byte [][]buffers = _buffers;

    for (int i = bufLength; i > 0; i--) {
      if (length <= offset) {
        _offset = offset;
        return -1;
      }

      byte []buf = buffers[offset / SIZE];

      buffer[bufOffset] = buf[offset % SIZE];

      offset++;
      bufOffset++;
    }

    _offset = offset;

    return bufLength;
  }

  /**
   * Writes the next byte.
   */
  public void write(int value)
  {
    int length = _length;
    int rOffset = length % SIZE;
    int blockId = length / SIZE;

    while (_buffers.length <= blockId) {
      byte [][]newBuffers = new byte[2 * _buffers.length][];

      System.arraycopy(_buffers, 0, newBuffers, 0, _buffers.length);
      _buffers = newBuffers;

      TempBuffer []newTempBuffers = new TempBuffer[newBuffers.length];
      System.arraycopy(_tempBuffers, 0, newTempBuffers, 0,
                       _tempBuffers.length);
      _tempBuffers = newTempBuffers;
    }

    byte []buffer = _buffers[blockId];

    if (buffer == null) {
      TempBuffer tempBuffer = TempBuffer.allocate();
      _tempBuffers[blockId] = tempBuffer;
      _buffers[blockId] = tempBuffer.getBuffer();

      buffer = _buffers[blockId];
    }

    buffer[rOffset] = (byte) value;

    _length = length + 1;
  }

  /**
   * Writes a buffer
   */
  public void write(byte []buffer, int offset, int length)
  {
    int rLength = _length;

    while (length > 0) {
      int rOffset = rLength % SIZE;

      int rBufferId = rLength / SIZE;

      if (rOffset == 0) {
        TempBuffer tempBuffer = TempBuffer.allocate();
        if (_tempBuffers.length <= rBufferId) {
          int len = _tempBuffers.length;

          TempBuffer []newTempBuffers = new TempBuffer[len + 32];
          System.arraycopy(_tempBuffers, 0, newTempBuffers, 0, len);
          _tempBuffers = newTempBuffers;

          byte [][]newBuffers = new byte[len + 32][];
          System.arraycopy(_buffers, 0, newBuffers, 0, len);
          _buffers = newBuffers;

        }

        _tempBuffers[rBufferId] = tempBuffer;
        _buffers[rBufferId] = tempBuffer.getBuffer();
      }

      byte []rBuffer = _buffers[rBufferId];

      int sublen = rBuffer.length - rOffset;

      if (length < sublen)
        sublen = length;

      System.arraycopy(buffer, offset, rBuffer, rOffset, sublen);

      length -= sublen;
      offset += sublen;
      rLength += sublen;
    }

    _length = rLength;
  }

  public void close()
  {
    for (int i = 0; i < _buffers.length; i++) {
      TempBuffer buffer = _tempBuffers[i];

      if (buffer != null)
        TempBuffer.free(buffer);

      _tempBuffers[i] = null;
      _buffers[i] = null;
    }

    _order = null;
    _orderIndex = null;

    _freeList.free(this);
  }
}
