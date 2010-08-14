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

package com.caucho.db.sql;

import com.caucho.db.blob.BlobInputStream;
import com.caucho.db.block.BlockStore;
import com.caucho.db.table.Column;
import com.caucho.db.table.TableIterator;
import com.caucho.db.table.Column.ColumnType;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.CharBuffer;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.sql.SQLException;

public class SelectResultSetImpl extends ResultSetImpl {
  private static final L10N L = new L10N(SelectResultSetImpl.class);
  
  private static final FreeList<SelectResultSetImpl> _freeList =
    new FreeList<SelectResultSetImpl>(16);
  
  private final WriteStream _ws;
  private final TempBuffer _buf;
  private final ReadStream _rs;
  private final byte []_buffer;
  private final CharBuffer _cb;
  private final TempStream _ts;

  private static QDate _date = new QDate();
  
  private Expr []_exprs;
  private ColumnType []_types = new ColumnType[32];
  private int []_offsets = new int[32];
  private int []_lengths = new int[32];
  private BlockStore []_stores = new BlockStore[32];

  private TableIterator []_rows;

  private int _lastColumn;

  private SelectResultSetImpl()
  {
    _ws = new WriteStream();
    _ws.setReuseBuffer(true);
    _ts = new TempStream();
    _rs = new ReadStream();
    _rs.setReuseBuffer(true);
    _buf = TempBuffer.allocate();
    _buffer = _buf.getBuffer();
    _cb = new CharBuffer();

    _rows = new TableIterator[0];
  }

  public static SelectResultSetImpl create(Expr []exprs)
  {
    SelectResultSetImpl rs = _freeList.allocate();

    if (rs == null)
      rs = new SelectResultSetImpl();

    rs.init(exprs);

    return rs;
  }

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
    

  private void init(Expr []exprs)
  {
    _exprs = exprs;
    
    if (_offsets.length < _exprs.length) {
      _offsets = new int[exprs.length];
      _lengths = new int[exprs.length];
      _types = new ColumnType[exprs.length];
      _stores = new BlockStore[exprs.length];
    }

    for (int i = 0; i < exprs.length; i++) {
      _stores[i] = exprs[i].getTable();
    }
  }

  void initRead()
    throws IOException
  {
    _ts.openRead(_rs);
  }

  WriteStream getWriteStream()
  {
    _ts.openWrite();
    _ws.init(_ts);
    
    return _ws;
  }
  
  public boolean next()
    throws SQLException
  {
    try {
      ReadStream rs = _rs;

      _lastColumn = 0;
      
      int hasData = rs.read();
      if (hasData <= 0)
        return false;

      int length = 0;
      int fields = _exprs.length;

      byte []buffer = _buffer;
      for (int i = 0; i < fields; i++) {
        int type = rs.read();

        int sublen = 0;

        if (type < 0)
          return false;

        ColumnType cType = ColumnType.values()[type];

        switch (cType) {
        case NONE:
          sublen = -1;
          break;

        case VARCHAR:
          int l0 = rs.read();
          int l1 = rs.read();
          int l2 = rs.read();
          int l3 = rs.read();

          sublen = ((l0 << 24) +
                    (l1 << 16) +
                    (l2 << 8) +
                    (l3));
          break;

        case INT:
          sublen = 4;
          break;
        case LONG:
        case DOUBLE:
        case DATE:
          sublen = 8;
          break;

        case BLOB:
          sublen = 128;
          break;

        default:
          throw new SQLException("Unknown column: " + type);
        }

        _types[i] = cType;
        _offsets[i] = length;
        _lengths[i] = sublen;

        if (sublen > 0) {
          rs.read(buffer, length, sublen);

          length += sublen;
        }
      }

      return true;
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
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
    _lastColumn = index;
    
    byte []buffer = _buffer;
    int offset = _offsets[index];
    int length = _lengths[index];
    
    switch (_types[index]) {
    case NONE:
      return null;
      
    case INT:
      {
        int value = (((buffer[offset] & 0xff) << 24) +
                     ((buffer[offset + 1] & 0xff) << 16) +
                     ((buffer[offset + 2] & 0xff) << 8) +
                     ((buffer[offset + 3] & 0xff)));

        return String.valueOf(value);
      }
      
    case LONG:
      {
        long value = (((buffer[offset + 0] & 0xffL) << 56) +
                      ((buffer[offset + 1] & 0xffL) << 48) +
                      ((buffer[offset + 2] & 0xffL) << 40) +
                      ((buffer[offset + 3] & 0xffL) << 32) +
                      ((buffer[offset + 4] & 0xffL) << 24) +
                      ((buffer[offset + 5] & 0xffL) << 16) +
                      ((buffer[offset + 6] & 0xffL) << 8) +
                      ((buffer[offset + 7] & 0xffL)));
        return String.valueOf(value);
      }
      
    case DOUBLE:
      {
        long value = (((buffer[offset + 0] & 0xffL) << 56) +
                      ((buffer[offset + 1] & 0xffL) << 48) +
                      ((buffer[offset + 2] & 0xffL) << 40) +
                      ((buffer[offset + 3] & 0xffL) << 32) +
                      ((buffer[offset + 4] & 0xffL) << 24) +
                      ((buffer[offset + 5] & 0xffL) << 16) +
                      ((buffer[offset + 6] & 0xffL) << 8) +
                      ((buffer[offset + 7] & 0xffL)));
        return String.valueOf(Double.longBitsToDouble(value));
      }
      
    case DATE:
      {
        long value = (((buffer[offset + 0] & 0xffL) << 56) +
                      ((buffer[offset + 1] & 0xffL) << 48) +
                      ((buffer[offset + 2] & 0xffL) << 40) +
                      ((buffer[offset + 3] & 0xffL) << 32) +
                      ((buffer[offset + 4] & 0xffL) << 24) +
                      ((buffer[offset + 5] & 0xffL) << 16) +
                      ((buffer[offset + 6] & 0xffL) << 8) +
                      ((buffer[offset + 7] & 0xffL)));
        return QDate.formatGMT(value);
      }

    case VARCHAR:
      return getStringValue(index);

    case BLOB:
      return getBlobString(index);

    default:
      return null;
    }
  }

  /**
   * Returns the string value for the result set.
   */
  private String getStringValue(int index)
    throws SQLException
  {
    _lastColumn = index;
    
    int length = _lengths[index];
    int offset = _offsets[index];
    
    if (length < 0)
      return null;

    CharBuffer cb = _cb;
    cb.clear();

    byte []buffer = _buffer;
    for (; length > 0; length--) {
      cb.append((char) buffer[offset++]);
    }

    return cb.toString();
  }

  /**
   * Returns the string value for the result set.
   */
  private String getBlobString(int index)
    throws SQLException
  {
    _lastColumn = index;
    
    int offset = _offsets[index];

    CharBuffer cb = _cb;
    cb.clear();

    BlobInputStream is = null;
    try {
      is = new BlobInputStream(_stores[index], _buffer, offset);

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

  public int getInt(int index)
    throws SQLException
  {
    _lastColumn = index;
    
    byte []buffer = _buffer;
    int offset = _offsets[index];
    int length = _lengths[index];
    
    switch (_types[index]) {
    case NONE:
      return 0;
      
    case INT:
      return (((buffer[offset + 0] & 0xff) << 24) +
              ((buffer[offset + 1] & 0xff) << 16) +
              ((buffer[offset + 2] & 0xff) << 8) +
              ((buffer[offset + 3] & 0xff)));
      
    case LONG:
      {
        long value = (((buffer[offset + 0] & 0xffL) << 56) +
                      ((buffer[offset + 1] & 0xffL) << 48) +
                      ((buffer[offset + 2] & 0xffL) << 40) +
                      ((buffer[offset + 3] & 0xffL) << 32) +
                      ((buffer[offset + 4] & 0xffL) << 24) +
                      ((buffer[offset + 5] & 0xffL) << 16) +
                      ((buffer[offset + 6] & 0xffL) << 8) +
                      ((buffer[offset + 7] & 0xffL)));
        return (int) value;
      }
      
    case DOUBLE:
      {
        long value = (((buffer[offset + 0] & 0xffL) << 56) +
                      ((buffer[offset + 1] & 0xffL) << 48) +
                      ((buffer[offset + 2] & 0xffL) << 40) +
                      ((buffer[offset + 3] & 0xffL) << 32) +
                      ((buffer[offset + 4] & 0xffL) << 24) +
                      ((buffer[offset + 5] & 0xffL) << 16) +
                      ((buffer[offset + 6] & 0xffL) << 8) +
                      ((buffer[offset + 7] & 0xffL)));
        return (int) Double.longBitsToDouble(value);
      }

    case VARCHAR:
      return Integer.parseInt(getString(index));

    default:
      return 0;
    }
  }

  public long getLong(int index)
    throws SQLException
  {
    _lastColumn = index;
    
    byte []buffer = _buffer;
    int offset = _offsets[index];
    int length = _lengths[index];
    
    switch (_types[index]) {
    case NONE:
      return 0;
      
    case INT:
      return (((buffer[offset] & 0xff) << 24) +
              ((buffer[offset + 1] & 0xff) << 16) +
              ((buffer[offset + 2] & 0xff) << 8) +
              ((buffer[offset + 3] & 0xff)));
      
    case LONG:
    case DATE:
      {
        long value = (((buffer[offset + 0] & 0xffL) << 56) +
                      ((buffer[offset + 1] & 0xffL) << 48) +
                      ((buffer[offset + 2] & 0xffL) << 40) +
                      ((buffer[offset + 3] & 0xffL) << 32) +
                      ((buffer[offset + 4] & 0xffL) << 24) +
                      ((buffer[offset + 5] & 0xffL) << 16) +
                      ((buffer[offset + 6] & 0xffL) << 8) +
                      ((buffer[offset + 7] & 0xffL)));
        return value;
      }
      
    case DOUBLE:
      {
        long value = (((buffer[offset + 0] & 0xffL) << 56) +
                      ((buffer[offset + 1] & 0xffL) << 48) +
                      ((buffer[offset + 2] & 0xffL) << 40) +
                      ((buffer[offset + 3] & 0xffL) << 32) +
                      ((buffer[offset + 4] & 0xffL) << 24) +
                      ((buffer[offset + 5] & 0xffL) << 16) +
                      ((buffer[offset + 6] & 0xffL) << 8) +
                      ((buffer[offset + 7] & 0xffL)));
        return (long) Double.longBitsToDouble(value);
      }

    case VARCHAR:
      return Long.parseLong(getString(index));

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
    _lastColumn = index;
    
    byte []buffer = _buffer;
    int offset = _offsets[index];
    int length = _lengths[index];
    
    switch (_types[index]) {
    case NONE:
      return 0;
      
    case INT:
      return (((buffer[offset + 0] & 0xff) << 24) +
              ((buffer[offset + 1] & 0xff) << 16) +
              ((buffer[offset + 2] & 0xff) << 8) +
              ((buffer[offset + 3] & 0xff)));
      
    case LONG:
      {
        long value = (((buffer[offset + 0] & 0xffL) << 56) +
                      ((buffer[offset + 1] & 0xffL) << 48) +
                      ((buffer[offset + 2] & 0xffL) << 40) +
                      ((buffer[offset + 3] & 0xffL) << 32) +
                      ((buffer[offset + 4] & 0xffL) << 24) +
                      ((buffer[offset + 5] & 0xffL) << 16) +
                      ((buffer[offset + 6] & 0xffL) << 8) +
                      ((buffer[offset + 7] & 0xffL)));
        return value;
      }
      
    case DOUBLE:
      {
        long value = (((buffer[offset + 0] & 0xffL) << 56) +
                      ((buffer[offset + 1] & 0xffL) << 48) +
                      ((buffer[offset + 2] & 0xffL) << 40) +
                      ((buffer[offset + 3] & 0xffL) << 32) +
                      ((buffer[offset + 4] & 0xffL) << 24) +
                      ((buffer[offset + 5] & 0xffL) << 16) +
                      ((buffer[offset + 6] & 0xffL) << 8) +
                      ((buffer[offset + 7] & 0xffL)));
        return Double.longBitsToDouble(value);
      }

    case VARCHAR:
      return Double.parseDouble(getString(index));

    default:
      return 0;
    }
  }

  public long getDate(int index)
    throws SQLException
  {
    _lastColumn = index;
    
    byte []buffer = _buffer;
    int offset = _offsets[index];
    int length = _lengths[index];
    
    switch (_types[index]) {
    case NONE:
      return 0;
      
    case LONG:
    case DATE:
      {
        long value = (((buffer[offset + 0] & 0xffL) << 56) +
                      ((buffer[offset + 1] & 0xffL) << 48) +
                      ((buffer[offset + 2] & 0xffL) << 40) +
                      ((buffer[offset + 3] & 0xffL) << 32) +
                      ((buffer[offset + 4] & 0xffL) << 24) +
                      ((buffer[offset + 5] & 0xffL) << 16) +
                      ((buffer[offset + 6] & 0xffL) << 8) +
                      ((buffer[offset + 7] & 0xffL)));
        return value;
      }

    case VARCHAR:
    case BLOB:
      {
        String value = getString(index);

        if (value == null)
          return 0;

        synchronized (_date) {
          try {
            return _date.parseDate(value);
          } catch (Exception e) {
            throw new SQLExceptionWrapper(e);
          }
        }
      }

    default:
      throw new SQLException("unknown type:" + _types[index]);
    }
  }

  /**
   * Returns true if the last column read was null.
   */
  public boolean wasNull()
  {
    if (_lastColumn < 0)
      return false;
    else
      return _lengths[_lastColumn] < 0;
  }
  
  public void close()
  {
    _freeList.free(this);
  }
}
