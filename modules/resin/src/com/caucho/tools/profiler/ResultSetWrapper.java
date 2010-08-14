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
 * @author Sam
 */


package com.caucho.tools.profiler;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

public final class ResultSetWrapper
  implements ResultSet
{
  private final ProfilerPoint _profilerPoint;
  private final ResultSet _resultSet;

  public ResultSetWrapper(ProfilerPoint profilerPoint, ResultSet resultSet)
  {
    _profilerPoint = profilerPoint;
    _resultSet = resultSet;
  }

  public boolean next()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.next();
    }
    finally {
      profiler.finish();
    }
  }

  public void close()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.close();
    }
    finally {
      profiler.finish();
    }
  }

  public boolean wasNull()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.wasNull();
    }
    finally {
      profiler.finish();
    }
  }

  public String getString(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getString(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean getBoolean(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getBoolean(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public byte getByte(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getByte(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public short getShort(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getShort(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public int getInt(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getInt(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public long getLong(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getLong(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public float getFloat(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getFloat(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public double getDouble(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getDouble(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public BigDecimal getBigDecimal(int columnIndex, int scale)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getBigDecimal(columnIndex, scale);
    }
    finally {
      profiler.finish();
    }
  }

  public byte[] getBytes(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getBytes(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public Date getDate(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getDate(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public Time getTime(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getTime(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public Timestamp getTimestamp(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getTimestamp(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public InputStream getAsciiStream(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getAsciiStream(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  /**
   * @deprecated
   */
  public InputStream getUnicodeStream(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getUnicodeStream(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public InputStream getBinaryStream(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getBinaryStream(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public String getString(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getString(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean getBoolean(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getBoolean(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public byte getByte(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getByte(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public short getShort(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getShort(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public int getInt(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getInt(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public long getLong(String columnName)
    throws SQLException
  {
    return _resultSet.getLong(columnName);
  }

  public float getFloat(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getFloat(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public double getDouble(String columnName)
    throws SQLException
  {
    return _resultSet.getDouble(columnName);
  }

  public BigDecimal getBigDecimal(String columnName, int scale)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getBigDecimal(columnName, scale);
    }
    finally {
      profiler.finish();
    }
  }

  public byte[] getBytes(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getBytes(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public Date getDate(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getDate(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public Time getTime(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getTime(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public Timestamp getTimestamp(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getTimestamp(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public InputStream getAsciiStream(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getAsciiStream(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public InputStream getUnicodeStream(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getUnicodeStream(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public InputStream getBinaryStream(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getBinaryStream(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public SQLWarning getWarnings()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getWarnings();
    }
    finally {
      profiler.finish();
    }
  }

  public void clearWarnings()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.clearWarnings();
    }
    finally {
      profiler.finish();
    }
  }

  public String getCursorName()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getCursorName();
    }
    finally {
      profiler.finish();
    }
  }

  public ResultSetMetaData getMetaData()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getMetaData();
    }
    finally {
      profiler.finish();
    }
  }

  public Object getObject(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getObject(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public Object getObject(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getObject(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public int findColumn(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.findColumn(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public Reader getCharacterStream(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getCharacterStream(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public Reader getCharacterStream(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getCharacterStream(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public BigDecimal getBigDecimal(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getBigDecimal(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public BigDecimal getBigDecimal(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getBigDecimal(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean isBeforeFirst()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.isBeforeFirst();
    }
    finally {
      profiler.finish();
    }
  }

  public boolean isAfterLast()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.isAfterLast();
    }
    finally {
      profiler.finish();
    }
  }

  public boolean isFirst()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.isFirst();
    }
    finally {
      profiler.finish();
    }
  }

  public boolean isLast()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.isLast();
    }
    finally {
      profiler.finish();
    }
  }

  public void beforeFirst()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.beforeFirst();
    }
    finally {
      profiler.finish();
    }
  }

  public void afterLast()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.afterLast();
    }
    finally {
      profiler.finish();
    }
  }

  public boolean first()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.first();
    }
    finally {
      profiler.finish();
    }
  }

  public boolean last()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.last();
    }
    finally {
      profiler.finish();
    }
  }

  public int getRow()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getRow();
    }
    finally {
      profiler.finish();
    }
  }

  public boolean absolute(int row)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.absolute(row);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean relative(int rows)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.relative(rows);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean previous()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.previous();
    }
    finally {
      profiler.finish();
    }
  }

  public void setFetchDirection(int direction)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.setFetchDirection(direction);
    }
    finally {
      profiler.finish();
    }
  }

  public int getFetchDirection()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getFetchDirection();
    }
    finally {
      profiler.finish();
    }
  }

  public void setFetchSize(int rows)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.setFetchSize(rows);
    }
    finally {
      profiler.finish();
    }
  }

  public int getFetchSize()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getFetchSize();
    }
    finally {
      profiler.finish();
    }
  }

  public int getType()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getType();
    }
    finally {
      profiler.finish();
    }
  }

  public int getConcurrency()
    throws SQLException
  {
    return _resultSet.getConcurrency();
  }

  public boolean rowUpdated()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.rowUpdated();
    }
    finally {
      profiler.finish();
    }
  }

  public boolean rowInserted()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.rowInserted();
    }
    finally {
      profiler.finish();
    }
  }

  public boolean rowDeleted()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.rowDeleted();
    }
    finally {
      profiler.finish();
    }
  }

  public void updateNull(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateNull(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateBoolean(int columnIndex, boolean x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateBoolean(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateByte(int columnIndex, byte x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateByte(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateShort(int columnIndex, short x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateShort(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateInt(int columnIndex, int x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateInt(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateLong(int columnIndex, long x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateLong(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateFloat(int columnIndex, float x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateFloat(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateDouble(int columnIndex, double x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateDouble(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateBigDecimal(int columnIndex, BigDecimal x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateBigDecimal(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateString(int columnIndex, String x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateString(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateBytes(int columnIndex, byte[] x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateBytes(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateDate(int columnIndex, Date x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateDate(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateTime(int columnIndex, Time x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateTime(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateTimestamp(int columnIndex, Timestamp x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateTimestamp(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateAsciiStream(int columnIndex, InputStream x, int length)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateAsciiStream(columnIndex, x, length);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateBinaryStream(int columnIndex, InputStream x, int length)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateBinaryStream(columnIndex, x, length);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateCharacterStream(int columnIndex, Reader x, int length)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateCharacterStream(columnIndex, x, length);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateObject(int columnIndex, Object x, int scale)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateObject(columnIndex, x, scale);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateObject(int columnIndex, Object x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateObject(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateNull(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateNull(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateBoolean(String columnName, boolean x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateBoolean(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateByte(String columnName, byte x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateByte(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateShort(String columnName, short x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateShort(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateInt(String columnName, int x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateInt(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateLong(String columnName, long x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateLong(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateFloat(String columnName, float x)
    throws SQLException
  {
    _resultSet.updateFloat(columnName, x);
  }

  public void updateDouble(String columnName, double x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateDouble(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateBigDecimal(String columnName, BigDecimal x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateBigDecimal(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateString(String columnName, String x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateString(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateBytes(String columnName, byte[] x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateBytes(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateDate(String columnName, Date x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateDate(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateTime(String columnName, Time x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateTime(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateTimestamp(String columnName, Timestamp x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateTimestamp(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateAsciiStream(String columnName, InputStream x, int length)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateAsciiStream(columnName, x, length);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateBinaryStream(String columnName, InputStream x, int length)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateBinaryStream(columnName, x, length);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateCharacterStream(String columnName,
                                    Reader reader,
                                    int length)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateCharacterStream(columnName, reader, length);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateObject(String columnName, Object x, int scale)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateObject(columnName, x, scale);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateObject(String columnName, Object x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateObject(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void insertRow()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.insertRow();
    }
    finally {
      profiler.finish();
    }
  }

  public void updateRow()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateRow();
    }
    finally {
      profiler.finish();
    }
  }

  public void deleteRow()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.deleteRow();
    }
    finally {
      profiler.finish();
    }
  }

  public void refreshRow()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.refreshRow();
    }
    finally {
      profiler.finish();
    }
  }

  public void cancelRowUpdates()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.cancelRowUpdates();
    }
    finally {
      profiler.finish();
    }
  }

  public void moveToInsertRow()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.moveToInsertRow();
    }
    finally {
      profiler.finish();
    }
  }

  public void moveToCurrentRow()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.moveToCurrentRow();
    }
    finally {
      profiler.finish();
    }
  }

  public Statement getStatement()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getStatement();
    }
    finally {
      profiler.finish();
    }
  }

  public Object getObject(int i, Map<String, Class<?>> map)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getObject(i, map);
    }
    finally {
      profiler.finish();
    }
  }

  public Ref getRef(int i)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getRef(i);
    }
    finally {
      profiler.finish();
    }
  }

  public Blob getBlob(int i)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getBlob(i);
    }
    finally {
      profiler.finish();
    }
  }

  public Clob getClob(int i)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getClob(i);
    }
    finally {
      profiler.finish();
    }
  }

  public Array getArray(int i)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getArray(i);
    }
    finally {
      profiler.finish();
    }
  }

  public Object getObject(String colName, Map<String, Class<?>> map)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getObject(colName, map);
    }
    finally {
      profiler.finish();
    }
  }

  public Ref getRef(String colName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getRef(colName);
    }
    finally {
      profiler.finish();
    }
  }

  public Blob getBlob(String colName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getBlob(colName);
    }
    finally {
      profiler.finish();
    }
  }

  public Clob getClob(String colName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getClob(colName);
    }
    finally {
      profiler.finish();
    }
  }

  public Array getArray(String colName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getArray(colName);
    }
    finally {
      profiler.finish();
    }
  }

  public Date getDate(int columnIndex, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getDate(columnIndex, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public Date getDate(String columnName, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getDate(columnName, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public Time getTime(int columnIndex, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getTime(columnIndex, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public Time getTime(String columnName, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getTime(columnName, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public Timestamp getTimestamp(int columnIndex, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getTimestamp(columnIndex, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public Timestamp getTimestamp(String columnName, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getTimestamp(columnName, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public URL getURL(int columnIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getURL(columnIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public URL getURL(String columnName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _resultSet.getURL(columnName);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateRef(int columnIndex, Ref x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateRef(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateRef(String columnName, Ref x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateRef(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateBlob(int columnIndex, Blob x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateBlob(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateBlob(String columnName, Blob x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateBlob(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateClob(int columnIndex, Clob x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateClob(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateClob(String columnName, Clob x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateClob(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateArray(int columnIndex, Array x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateArray(columnIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void updateArray(String columnName, Array x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _resultSet.updateArray(columnName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public String toString()
  {
    return "ResultSetWrapper[" + _profilerPoint.getName() + "]";
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
}
