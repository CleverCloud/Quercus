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

import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.FreeList;
import com.caucho.util.QDate;
import com.caucho.util.L10N;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

/**
 * The JDBC statement implementation.
 */
abstract public class AbstractResultSet implements java.sql.ResultSet {
  private final static L10N L = new L10N(AbstractResultSet.class);

  private final static FreeList<QDate> _freeDate = new FreeList<QDate>(16);

  private int _rowNumber;

  abstract public java.sql.Statement getStatement()
    throws SQLException;

  abstract public java.sql.ResultSetMetaData getMetaData()
    throws SQLException;

  abstract public boolean next()
    throws SQLException;

  abstract public boolean wasNull()
    throws SQLException;

  abstract public int findColumn(String columnName)
    throws SQLException;

  abstract public String getString(int columnIndex)
    throws SQLException;

  public boolean absolute(int row)
    throws SQLException
  {
    if (row < getRow())
      return false;

    while (getRow() < row) {
      if (! next())
        return false;
    }

    return true;
  }

  public void afterLast()
  {
  }

  public void beforeFirst()
  {
  }

  public void cancelRowUpdates()
  {
  }

  public void clearWarnings()
  {
  }

  public void deleteRow()
  {
  }

  public int getRow()
    throws SQLException
  {
    if (_rowNumber < 0)
      throw new SQLException("can't call getRow() after close()");

    return _rowNumber;
  }

  public boolean isBeforeFirst()
    throws SQLException
  {
    return getRow() == 0;
  }

  public boolean isAfterLast()
  {
    return false;
  }

  public boolean isFirst()
    throws SQLException
  {
    return _rowNumber == 1;
  }

  public boolean first()
    throws SQLException
  {
    return isFirst();
  }

  public boolean isLast()
    throws SQLException
  {
    return false;
  }

  public boolean last()
    throws SQLException
  {
    return isLast();
  }

  public int getConcurrency()
  {
    return 0;
  }

  public String getCursorName()
  {
    return null;
  }

  public int getType()
  {
    return 0;
  }

  public BigDecimal getBigDecimal(int columnIndex)
    throws SQLException
  {
    return null;
  }

  public BigDecimal getBigDecimal(int columnIndex, int scale)
    throws SQLException
  {
    return getBigDecimal(columnIndex).setScale(scale);
  }

  public BigDecimal getBigDecimal(String columnName)
    throws SQLException
  {
    return getBigDecimal(findColumn(columnName));
  }

  public BigDecimal getBigDecimal(String columnName, int scale)
    throws SQLException
  {
    return getBigDecimal(findColumn(columnName), scale);
  }

  /**
   * Returns the boolean value for the column.
   */
  public boolean getBoolean(int columnIndex)
    throws SQLException
  {
    String v = getString(columnIndex);

    return (v != null && ! v.equals("") && ! v.equals("0") && ! v.equals("n"));
  }

  /**
   * Returns the boolean value for the named column.
   */
  public boolean getBoolean(String columnName)
    throws SQLException
  {
    return getBoolean(findColumn(columnName));
  }

  public byte getByte(int columnIndex)
    throws SQLException
  {
    return (byte) getInt(columnIndex);
  }

  public byte getByte(String columnName)
    throws SQLException
  {
    return getByte(findColumn(columnName));
  }

  /**
   * Returns the byte value for the column.
   */
  public byte []getBytes(int columnIndex)
    throws SQLException
  {
    try {
      Blob blob = getBlob(columnIndex);

      if (blob == null)
        return null;

      int length = (int) blob.length();

      byte []bytes = new byte[length];

      InputStream is = blob.getBinaryStream();

      try {
        int offset = 0;
        int sublen;

        while (length > 0 && (sublen = is.read(bytes, offset, length)) > 0) {
          offset += sublen;
          length -= sublen;
        }
      } finally {
        is.close();
      }

      return bytes;
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
  }

  public byte []getBytes(String columnName)
    throws SQLException
  {
    return getBytes(findColumn(columnName));
  }

  public Reader getCharacterStream(int columnIndex)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public Reader getCharacterStream(String columnName)
    throws SQLException
  {
    return getCharacterStream(findColumn(columnName));
  }

  public java.sql.Date getDate(int columnIndex)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public java.sql.Date getDate(String columnName)
    throws SQLException
  {
    return getDate(findColumn(columnName));
  }

  public java.sql.Date getDate(int columnIndex, Calendar cal)
    throws SQLException
  {
    return getDate(columnIndex);
  }

  public java.sql.Date getDate(String columnName, Calendar cal)
    throws SQLException
  {
    return getDate(findColumn(columnName), cal);
  }

  /**
   * Returns the double value for the column.
   */
  public double getDouble(int columnIndex)
    throws SQLException
  {
    String v = getString(columnIndex);

    if (v == null || v.length() == 0)
      return 0;
    else
      return Double.parseDouble(v);
  }

  /**
   * Returns the double value for the named column.
   */
  public double getDouble(String columnName)
    throws SQLException
  {
    return getDouble(findColumn(columnName));
  }

  public int getFetchDirection()
  {
    return 0;
  }

  public int getFetchSize()
  {
    return 0;
  }

  /**
   * Returns the float value for the column.
   */
  public float getFloat(int columnIndex)
    throws SQLException
  {
    return (float) getDouble(columnIndex);
  }

  /**
   * Returns the float value for the named column.
   */
  public float getFloat(String columnName)
    throws SQLException
  {
    return (float) getDouble(findColumn(columnName));
  }

  /**
   * Returns the integer value for the column.
   */
  public int getInt(int columnIndex)
    throws SQLException
  {
    String v = getString(columnIndex);

    if (v == null || v.length() == 0)
      return 0;
    else
      return Integer.parseInt(v);
  }

  /**
   * Returns the integer value for the named column.
   */
  public int getInt(String columnName)
    throws SQLException
  {
    return getInt(findColumn(columnName));
  }

  /**
   * Returns the long value for the column.
   */
  public long getLong(int columnIndex)
    throws SQLException
  {
    String v = getString(columnIndex);

    if (v == null || v.length() == 0)
      return 0;
    else
      return Long.parseLong(v);
  }

  /**
   * Returns the long value for the named column.
   */
  public long getLong(String columnName)
    throws SQLException
  {
    return getLong(findColumn(columnName));
  }

  public Object getObject(int columnIndex)
    throws SQLException
  {
    return getString(columnIndex);
  }

  public Object getObject(String columnName)
    throws SQLException
  {
    return getObject(findColumn(columnName));
  }

  public Object getObject(int columnIndex, Map<String,Class<?>> map)
    throws SQLException
  {
    return getObject(columnIndex);
  }

  public Object getObject(String columnName, Map<String,Class<?>> map)
    throws SQLException
  {
    return getObject(findColumn(columnName), map);
  }

  public short getShort(int columnIndex)
    throws SQLException
  {
    return (short) getInt(columnIndex);
  }

  public short getShort(String columnName)
    throws SQLException
  {
    return getShort(findColumn(columnName));
  }

  public String getString(String columnName)
    throws SQLException
  {
    return getString(findColumn(columnName));
  }

  public Time getTime(int columnIndex)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public Time getTime(String columnName)
    throws SQLException
  {
    return getTime(findColumn(columnName));
  }

  public Time getTime(int columnIndex, Calendar calendar)
    throws SQLException
  {
    return getTime(columnIndex);
  }

  public Time getTime(String columnName, Calendar calendar)
    throws SQLException
  {
    return getTime(findColumn(columnName), calendar);
  }

  public Timestamp getTimestamp(int columnIndex)
    throws SQLException
  {
    String value = getString(columnIndex);

    if (value == null || value.length() == 0)
      return null;

    try {
      QDate qDate = _freeDate.allocate();

      if (qDate == null)
        qDate = new QDate();

      long date = qDate.parseLocalDate(value);

      _freeDate.free(qDate);

      return new Timestamp(date);
    } catch (Exception e) {
      e.printStackTrace();
      throw new SQLException(e);
    }
  }

  public Timestamp getTimestamp(String columnName)
    throws SQLException
  {
    return getTimestamp(findColumn(columnName));
  }

  public Timestamp getTimestamp(int columnIndex, Calendar calendar)
    throws SQLException
  {
    return getTimestamp(columnIndex);
  }

  public Timestamp getTimestamp(String columnName, Calendar calendar)
    throws SQLException
  {
    return getTimestamp(findColumn(columnName), calendar);
  }

  public InputStream getBinaryStream(int columnIndex)
    throws SQLException
  {
    Blob blob = getBlob(columnIndex);

    if (blob != null)
      return blob.getBinaryStream();
    else
      return null;
  }

  public InputStream getBinaryStream(String columnName)
    throws SQLException
  {
    return getBinaryStream(findColumn(columnName));
  }

  public InputStream getAsciiStream(int columnIndex)
    throws SQLException
  {
    return getBinaryStream(columnIndex);
  }

  public InputStream getAsciiStream(String columnName)
    throws SQLException
  {
    return getAsciiStream(findColumn(columnName));
  }

  public InputStream getUnicodeStream(int columnIndex)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public InputStream getUnicodeStream(String columnName)
    throws SQLException
  {
    return getUnicodeStream(findColumn(columnName));
  }

  public Array getArray(int columnIndex)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public Array getArray(String columnName)
    throws SQLException
  {
    return getArray(findColumn(columnName));
  }

  public Blob getBlob(int columnIndex)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public Blob getBlob(String columnName)
    throws SQLException
  {
    return getBlob(findColumn(columnName));
  }

  public Clob getClob(int columnIndex)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public Clob getClob(String columnName)
    throws SQLException
  {
    return getClob(findColumn(columnName));
  }

  public Ref getRef(int columnIndex)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public Ref getRef(String columnName)
    throws SQLException
  {
    return getRef(findColumn(columnName));
  }

  public SQLWarning getWarnings()
  {
    return null;
  }

  public void insertRow()
  {
  }

  public void moveToCurrentRow()
  {
  }

  public void moveToInsertRow()
  {
  }

  public boolean previous()
  {
    return false;
  }

  public void refreshRow()
  {
  }

  public boolean relative(int rows)
    throws SQLException
  {
    while (rows-- > 0) {
      next();
    }

    return false;
  }

  public boolean rowDeleted()
  {
    return false;
  }

  public boolean rowInserted()
  {
    return false;
  }

  public boolean rowUpdated()
  {
    return false;
  }

  public void setFetchDirection(int direction)
  {
  }

  public void setFetchSize(int rows)
  {
  }

  public void updateRow()
  {
  }

  public void updateObject(int columnIndex, Object o)
  {
  }

  public void updateObject(String columnName, Object o)
    throws SQLException
  {
    updateObject(findColumn(columnName), o);
  }

  public void updateObject(int columnIndex, Object o, int scale)
  {
  }

  public void updateObject(String columnName, Object o, int scale)
    throws SQLException
  {
    updateObject(findColumn(columnName), o, scale);
  }

  public void updateNull(int columnIndex)
  {
  }

  public void updateNull(String columnName)
    throws SQLException
  {
    updateNull(findColumn(columnName));
  }

  public void updateAsciiStream(int columnIndex, InputStream x, int length)
  {
  }

  public void updateAsciiStream(String columnName, InputStream x, int length)
    throws SQLException
  {
    updateAsciiStream(findColumn(columnName), x, length);
  }

  public void updateBigDecimal(int columnIndex, BigDecimal x)
  {
  }

  public void updateBigDecimal(String columnName, BigDecimal x)
    throws SQLException
  {
    updateBigDecimal(findColumn(columnName), x);
  }

  public void updateBinaryStream(int columnIndex, InputStream x, int length)
  {
  }

  public void updateBinaryStream(String columnName, InputStream x, int length)
    throws SQLException
  {
    updateBinaryStream(findColumn(columnName), x, length);
  }

  public void updateBoolean(int columnIndex, boolean x)
  {
  }

  public void updateBoolean(String columnName, boolean x)
    throws SQLException
  {
    updateBoolean(findColumn(columnName), x);
  }

  public void updateByte(int columnIndex, byte x)
  {
  }

  public void updateByte(String columnName, byte x)
    throws SQLException
  {
    updateByte(findColumn(columnName), x);
  }

  public void updateShort(int columnIndex, short x)
  {
  }

  public void updateShort(String columnName, short x)
    throws SQLException
  {
    updateShort(findColumn(columnName), x);
  }

  public void updateInt(int columnIndex, int x)
  {
  }

  public void updateInt(String columnName, int x)
    throws SQLException
  {
    updateInt(findColumn(columnName), x);
  }

  public void updateLong(int columnIndex, long x)
  {
  }

  public void updateLong(String columnName, long x)
    throws SQLException
  {
    updateLong(findColumn(columnName), x);
  }

  public void updateFloat(int columnIndex, float x)
  {
  }

  public void updateFloat(String columnName, float x)
    throws SQLException
  {
    updateFloat(findColumn(columnName), x);
  }

  public void updateDouble(int columnIndex, double x)
  {
  }

  public void updateDouble(String columnName, double x)
    throws SQLException
  {
    updateDouble(findColumn(columnName), x);
  }

  public void updateString(int columnIndex, String x)
  {
  }

  public void updateString(String columnName, String x)
    throws SQLException
  {
    updateString(findColumn(columnName), x);
  }

  public void updateBytes(int columnIndex, byte[] x)
  {
  }

  public void updateBytes(String columnName, byte[] x)
    throws SQLException
  {
    updateBytes(findColumn(columnName), x);
  }

  public void updateCharacterStream(int columnIndex, Reader x, int length)
  {
  }

  public void updateCharacterStream(String columnName, Reader x, int length)
    throws SQLException
  {
    updateCharacterStream(findColumn(columnName), x, length);
  }

  public void updateTime(int columnIndex, Time time)
    throws SQLException
  {
  }

  public void updateTime(String columnName, Time time)
    throws SQLException
  {
    updateTime(findColumn(columnName), time);
  }

  public void updateDate(int columnIndex, java.sql.Date date)
    throws SQLException
  {
  }

  public void updateDate(String columnName, java.sql.Date date)
    throws SQLException
  {
    updateDate(findColumn(columnName), date);
  }

  public void updateTimestamp(int columnIndex, Timestamp time)
    throws SQLException
  {
  }

  public void updateTimestamp(String columnName, Timestamp time)
    throws SQLException
  {
    updateTimestamp(findColumn(columnName), time);
  }

  public void updateArray(int columnIndex, Array value)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public void updateArray(String columnName, Array value)
    throws SQLException
  {
    updateArray(findColumn(columnName), value);
  }

  public void updateBlob(int columnIndex, Blob value)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public void updateBlob(String columnName, Blob value)
    throws SQLException
  {
    updateBlob(findColumn(columnName), value);
  }

  public void updateClob(int columnIndex, Clob value)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public void updateClob(String columnName, Clob value)
    throws SQLException
  {
    updateClob(findColumn(columnName), value);
  }

  public void updateRef(int columnIndex, Ref value)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public void updateRef(String columnName, Ref value)
    throws SQLException
  {
    updateRef(findColumn(columnName), value);
  }

  public java.net.URL getURL(int column)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public java.net.URL getURL(String column)
    throws SQLException
  {
    return getURL(findColumn(column));
  }

    public RowId getRowId(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public RowId getRowId(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getHoldability() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isClosed() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNString(int columnIndex, String nString) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNString(String columnLabel, String nString) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NClob getNClob(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NClob getNClob(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getNString(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getNString(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

  public void close()
    throws SQLException
  {
  }
}
