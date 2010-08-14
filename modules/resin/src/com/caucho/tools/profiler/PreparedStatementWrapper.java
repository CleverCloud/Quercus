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

public class PreparedStatementWrapper
  implements PreparedStatement
{
  private final PreparedStatement _preparedStatement;
  private ProfilerPoint _profilerPoint;

  public PreparedStatementWrapper(ProfilerPoint profilerPoint,
                                  PreparedStatement preparedStatement)
  {
    _profilerPoint = profilerPoint;
    _preparedStatement = preparedStatement;
  }

  public ResultSet executeQuery()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.executeQuery();
    }
    finally {
      profiler.finish();
    }
  }

  public int executeUpdate()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.executeUpdate();
    }
    finally {
      profiler.finish();
    }
  }

  public void setNull(int parameterIndex, int sqlType)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setNull(parameterIndex, sqlType);
    }
    finally {
      profiler.finish();
    }
  }

  public void setBoolean(int parameterIndex, boolean x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setBoolean(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setByte(int parameterIndex, byte x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setByte(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setShort(int parameterIndex, short x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setShort(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setInt(int parameterIndex, int x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setInt(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setLong(int parameterIndex, long x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setLong(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setFloat(int parameterIndex, float x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setFloat(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setDouble(int parameterIndex, double x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setDouble(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setBigDecimal(int parameterIndex, BigDecimal x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setBigDecimal(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setString(int parameterIndex, String x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setString(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setBytes(int parameterIndex, byte[] x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setBytes(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setDate(int parameterIndex, Date x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setDate(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setTime(int parameterIndex, Time x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setTime(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setTimestamp(int parameterIndex, Timestamp x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setTimestamp(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setAsciiStream(int parameterIndex, InputStream x, int length)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setAsciiStream(parameterIndex, x, length);
    }
    finally {
      profiler.finish();
    }
  }

  public void setUnicodeStream(int parameterIndex, InputStream x, int length)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setUnicodeStream(parameterIndex, x, length);
    }
    finally {
      profiler.finish();
    }
  }

  public void setBinaryStream(int parameterIndex, InputStream x, int length)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setBinaryStream(parameterIndex, x, length);
    }
    finally {
      profiler.finish();
    }
  }

  public void clearParameters()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.clearParameters();
    }
    finally {
      profiler.finish();
    }
  }

  public void setObject(int parameterIndex,
                        Object x,
                        int targetSqlType,
                        int scale)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setObject(parameterIndex, x, targetSqlType, scale);
    }
    finally {
      profiler.finish();
    }
  }

  public void setObject(int parameterIndex, Object x, int targetSqlType)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setObject(parameterIndex, x, targetSqlType);
    }
    finally {
      profiler.finish();
    }
  }

  public void setObject(int parameterIndex, Object x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setObject(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean execute()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.execute();
    }
    finally {
      profiler.finish();
    }
  }

  public void addBatch()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.addBatch();
    }
    finally {
      profiler.finish();
    }
  }

  public void setCharacterStream(int parameterIndex, Reader reader, int length)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setCharacterStream(parameterIndex, reader, length);
    }
    finally {
      profiler.finish();
    }
  }

  public void setRef(int i, Ref x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setRef(i, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setBlob(int i, Blob x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setBlob(i, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setClob(int i, Clob x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setClob(i, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setArray(int i, Array x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setArray(i, x);
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
      return _preparedStatement.getMetaData();
    }
    finally {
      profiler.finish();
    }
  }

  public void setDate(int parameterIndex, Date x, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setDate(parameterIndex, x, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public void setTime(int parameterIndex, Time x, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setTime(parameterIndex, x, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setTimestamp(parameterIndex, x, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public void setNull(int paramIndex, int sqlType, String typeName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setNull(paramIndex, sqlType, typeName);
    }
    finally {
      profiler.finish();
    }
  }

  public void setURL(int parameterIndex, URL x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setURL(parameterIndex, x);
    }
    finally {
      profiler.finish();
    }
  }

  public ParameterMetaData getParameterMetaData()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.getParameterMetaData();
    }
    finally {
      profiler.finish();
    }
  }

  public ResultSet executeQuery(String sql)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.executeQuery(sql);
    }
    finally {
      profiler.finish();
    }
  }

  public int executeUpdate(String sql)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.executeUpdate(sql);
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
      _preparedStatement.close();
    }
    finally {
      profiler.finish();
    }
  }

  public int getMaxFieldSize()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.getMaxFieldSize();
    }
    finally {
      profiler.finish();
    }
  }

  public void setMaxFieldSize(int max)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setMaxFieldSize(max);
    }
    finally {
      profiler.finish();
    }
  }

  public int getMaxRows()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.getMaxRows();
    }
    finally {
      profiler.finish();
    }
  }

  public void setMaxRows(int max)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setMaxRows(max);
    }
    finally {
      profiler.finish();
    }
  }

  public void setEscapeProcessing(boolean enable)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setEscapeProcessing(enable);
    }
    finally {
      profiler.finish();
    }
  }

  public int getQueryTimeout()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.getQueryTimeout();
    }
    finally {
      profiler.finish();
    }
  }

  public void setQueryTimeout(int seconds)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setQueryTimeout(seconds);
    }
    finally {
      profiler.finish();
    }
  }

  public void cancel()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.cancel();
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
      return _preparedStatement.getWarnings();
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
      _preparedStatement.clearWarnings();
    }
    finally {
      profiler.finish();
    }
  }

  public void setCursorName(String name)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.setCursorName(name);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean execute(String sql)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.execute(sql);
    }
    finally {
      profiler.finish();
    }
  }

  public ResultSet getResultSet()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.getResultSet();
    }
    finally {
      profiler.finish();
    }
  }

  public int getUpdateCount()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.getUpdateCount();
    }
    finally {
      profiler.finish();
    }
  }

  public boolean getMoreResults()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.getMoreResults();
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
      _preparedStatement.setFetchDirection(direction);
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
      return _preparedStatement.getFetchDirection();
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
      _preparedStatement.setFetchSize(rows);
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
      return _preparedStatement.getFetchSize();
    }
    finally {
      profiler.finish();
    }
  }

  public int getResultSetConcurrency()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.getResultSetConcurrency();
    }
    finally {
      profiler.finish();
    }
  }

  public int getResultSetType()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.getResultSetType();
    }
    finally {
      profiler.finish();
    }
  }

  public void addBatch(String sql)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.addBatch(sql);
    }
    finally {
      profiler.finish();
    }
  }

  public void clearBatch()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _preparedStatement.clearBatch();
    }
    finally

    {
      profiler.finish();
    }
  }

  public int[] executeBatch()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.executeBatch();
    }
    finally {
      profiler.finish();
    }
  }

  public Connection getConnection()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.getConnection();
    }
    finally {
      profiler.finish();
    }
  }

  public boolean getMoreResults(int current)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.getMoreResults(current);
    }
    finally {
      profiler.finish();
    }
  }

  public ResultSet getGeneratedKeys()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.getGeneratedKeys();
    }
    finally {
      profiler.finish();
    }
  }

  public int executeUpdate(String sql, int autoGeneratedKeys)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.executeUpdate(sql, autoGeneratedKeys);
    }
    finally {
      profiler.finish();
    }
  }

  public int executeUpdate(String sql, int[] columnIndexes)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.executeUpdate(sql, columnIndexes);
    }
    finally {
      profiler.finish();
    }
  }

  public int executeUpdate(String sql, String[] columnNames)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.executeUpdate(sql, columnNames);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean execute(String sql, int autoGeneratedKeys)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.execute(sql, autoGeneratedKeys);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean execute(String sql, int[] columnIndexes)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.execute(sql, columnIndexes);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean execute(String sql, String[] columnNames)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.execute(sql, columnNames);
    }
    finally {
      profiler.finish();
    }
  }

  public int getResultSetHoldability()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _preparedStatement.getResultSetHoldability();
    }
    finally {
      profiler.finish();
    }
  }

  public String toString()
  {
    return "PreparedStatementWrapper[" + _profilerPoint.getName() + "]";
  }

    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNString(int parameterIndex, String value) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isClosed() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setPoolable(boolean poolable) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isPoolable() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
