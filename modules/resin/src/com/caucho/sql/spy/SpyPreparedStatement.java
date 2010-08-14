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

package com.caucho.sql.spy;

import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.L10N;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.logging.*;

/**
 * Spying on a statement;
 */
public class SpyPreparedStatement extends SpyStatement
  implements java.sql.PreparedStatement {
  private final static Logger log
    = Logger.getLogger(SpyPreparedStatement.class.getName());
  protected static L10N L = new L10N(SpyPreparedStatement.class);

  private String _sql;
  protected PreparedStatement _pstmt;

  SpyPreparedStatement(String id, SpyConnection conn,
                       PreparedStatement stmt, String sql)
  {
    super(id, conn, stmt);

    _pstmt = stmt;
    _sql = sql;
  }

  public java.sql.ResultSet executeQuery()
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":executeQuery(" + _sql + ")");

      ResultSet rs = _pstmt.executeQuery();

      return rs;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-executeQuery(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public int executeUpdate()
    throws SQLException
  {
    try {
      int result = _pstmt.executeUpdate();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":executeUpdate(" + _sql + ") -> " + result);

      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-executeUpdate(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public boolean execute()
    throws SQLException
  {
    try {
      boolean result = _pstmt.execute();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":execute(" + _sql + ") -> " + result);

      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-execute(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void addBatch()
    throws SQLException
  {
    try {
      _pstmt.addBatch();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":addBatch()");
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-addBatch(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void clearParameters()
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":clearParameters()");

      _pstmt.clearParameters();
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-clearParameters(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public ResultSetMetaData getMetaData()
    throws SQLException
  {
    try {
      ResultSetMetaData result = _pstmt.getMetaData();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":getMetaData() -> " + result);

      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-getMetaData(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public ParameterMetaData getParameterMetaData()
    throws SQLException
  {
    return _pstmt.getParameterMetaData();
  }

  public void setNull(int parameterIndex, int sqlType)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setNull(" + parameterIndex + ",type=" + sqlType + ")");

      _pstmt.setNull(parameterIndex, sqlType);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setNull(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setNull(int parameterIndex, int sqlType, String typeName)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setNull(" + parameterIndex + ",type=" + sqlType +
              ",typeName=" + typeName + ")");

      _pstmt.setNull(parameterIndex, sqlType, typeName);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setNull(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setBoolean(int index, boolean value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setBoolean(" + index + "," + value + ")");

      _pstmt.setBoolean(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setBoolean(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setByte(int index, byte value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setByte(" + index + "," + value + ")");

      _pstmt.setByte(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setByte(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setShort(int index, short value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setShort(" + index + "," + value + ")");

      _pstmt.setShort(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setShort(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setInt(int index, int value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setInt(" + index + "," + value + ")");

      _pstmt.setInt(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setInt(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setLong(int index, long value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setLong(" + index + "," + value + ")");

      _pstmt.setLong(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setLong(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setFloat(int index, float value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setFloat(" + index + "," + value + ")");

      _pstmt.setFloat(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setFloat(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setDouble(int index, double value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setDouble(" + index + "," + value + ")");

      _pstmt.setDouble(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setDouble(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setBigDecimal(int index, BigDecimal value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setBigDecimal(" + index + "," + value + ")");

      _pstmt.setBigDecimal(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setBigDecimal(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setString(int index, String value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setString(" + index + "," + value + ")");

      _pstmt.setString(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setString(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setBytes(int index, byte []value)
    throws SQLException
  {
    try {
      if (value != null)
        if (log.isLoggable(Level.FINE))
          log.fine(getId() + ":setBytes(" + index + ",len=" + value.length + ")");
      else
        if (log.isLoggable(Level.FINE))
          log.fine(getId() + ":setBytes(" + index + ",null");

      _pstmt.setBytes(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setBytes(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setDate(int index, Date value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setDate(" + index + "," + value + ")");

      _pstmt.setDate(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setDate(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setDate(int index, Date value, Calendar cal)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setDate(" + index + "," + value + ",cal=" + cal + ")");

      _pstmt.setDate(index, value, cal);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setDate(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setTime(int index, Time value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setTime(" + index + "," + value + ")");

      _pstmt.setTime(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setTime(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setTime(int index, Time value, Calendar cal)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setTime(" + index + "," + value + ",cal=" + cal + ")");

      _pstmt.setTime(index, value, cal);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setTime(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setTimestamp(int index, Timestamp value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setTimestamp(" + index + "," + value + ")");

      _pstmt.setTimestamp(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setTimestamp(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setTimestamp(int index, Timestamp value, Calendar cal)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setTimestamp(" + index + "," + value + ",cal=" + cal + ")");

      _pstmt.setTimestamp(index, value, cal);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setTimestamp(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setAsciiStream(int index, InputStream value, int length)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setAsciiStream(" + index + "," + value + ",len=" + length + ")");

      _pstmt.setAsciiStream(index, value, length);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setAsciiStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setUnicodeStream(int index, InputStream value, int length)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setUnicodeStream(" + index + "," + value + ",len=" + length + ")");

      _pstmt.setUnicodeStream(index, value, length);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setUnicodeStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setBinaryStream(int index, InputStream value, int length)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setBinaryStream(" + index + "," + value + ",len=" + length + ")");

      _pstmt.setBinaryStream(index, value, length);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setBinaryStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setCharacterStream(int index, Reader value, int length)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setCharacterStream(" + index + "," + value + ",len=" + length + ")");

      _pstmt.setCharacterStream(index, value, length);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setCharacterStream(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setObject(int index, Object value, int type, int scale)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setObject(" + index + "," + value +
              ",type=" + type + ",scale=" + scale + ")");

      _pstmt.setObject(index, value, type, scale);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setObject(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setObject(int index, Object value, int type)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setObject(" + index + "," + value +
              ",type=" + type +  ")");

      _pstmt.setObject(index, value, type);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setObject(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setObject(int index, Object value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setObject(" + index + "," + value + ")");

      _pstmt.setObject(index, value);
    } catch (Throwable e) {
      e.printStackTrace();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setObject(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setRef(int index, Ref value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setRef(" + index + "," + value + ")");

      _pstmt.setRef(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setRef(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setBlob(int index, Blob value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setBlob(" + index + "," + value + ")");

      _pstmt.setBlob(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setBlob(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setClob(int index, Clob value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setClob(" + index + "," + value + ")");

      _pstmt.setClob(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setClob(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setArray(int index, Array value)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setArray(" + index + "," + value + ")");

      _pstmt.setArray(index, value);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setArray(" + e + ")");

      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setURL(int index, URL value)
    throws SQLException
  {
    throw new UnsupportedOperationException();
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
}
