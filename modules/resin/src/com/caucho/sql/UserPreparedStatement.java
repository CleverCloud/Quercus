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

package com.caucho.sql;

import com.caucho.env.meter.ActiveTimeSensor;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.logging.Logger;

/**
 * User-view of prepared statements
 */
public class UserPreparedStatement extends UserStatement
  implements PreparedStatement {
  private final static Logger log
    = Logger.getLogger(UserPreparedStatement.class.getName());
  protected static L10N L = new L10N(UserPreparedStatement.class);

  protected PreparedStatement _pstmt;
  protected PreparedStatementCacheItem _cacheItem;

  private boolean _isClosed;

  private ActiveTimeSensor _timeProbe;
  
  UserPreparedStatement(UserConnection conn,
                        PreparedStatement pStmt,
                        PreparedStatementCacheItem cacheItem)
  {
    super(conn, pStmt);
    
    _pstmt = pStmt;
    _cacheItem = cacheItem;
    _timeProbe = conn.getTimeProbe();

    if (pStmt == null)
      throw new NullPointerException();
  }
  
  UserPreparedStatement(UserConnection conn,
                        PreparedStatement pStmt)
  {
    this(conn, pStmt, null);
  }

  /**
   * Returns the underlying statement.
   */
  public PreparedStatement getPreparedStatement()
  {
    return _pstmt;
  }

  /**
   * Executes the prepared statement's query.
   */
  public ResultSet executeQuery()
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _pstmt.executeQuery();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    } finally {
      _timeProbe.end(startTime);
    }
  }

  /**
   * Executes the prepared statement's sql as an update
   */
  public int executeUpdate()
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _pstmt.executeUpdate();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    } finally {
      _timeProbe.end(startTime);
    }
  }

  /**
   * Executes the prepared statement's sql as an update or query
   */
  public boolean execute()
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _pstmt.execute();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    } finally {
      _timeProbe.end(startTime);
    }
  }

  /**
   * Adds the statement as a batch.
   */
  public void addBatch()
    throws SQLException
  {
    try {
      _pstmt.addBatch();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Clears the statement's parameters.
   */
  public void clearParameters()
    throws SQLException
  {
    try {
      _pstmt.clearParameters();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns the result metadata.
   */
  public ResultSetMetaData getMetaData()
    throws SQLException
  {
    try {
      return _pstmt.getMetaData();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns the prepared statement's meta data.
   */
  public ParameterMetaData getParameterMetaData()
    throws SQLException
  {
    try {
      return _pstmt.getParameterMetaData();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a null.
   */
  public void setNull(int parameterIndex, int sqlType)
    throws SQLException
  {
    try {
      _pstmt.setNull(parameterIndex, sqlType);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a null.
   */
  public void setNull(int parameterIndex, int sqlType, String typeName)
    throws SQLException
  {
    try {
      _pstmt.setNull(parameterIndex, sqlType, typeName);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a boolean.
   */
  public void setBoolean(int index, boolean value)
    throws SQLException
  {
    try {
      _pstmt.setBoolean(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a byte.
   */
  public void setByte(int index, byte value)
    throws SQLException
  {
    try {
      _pstmt.setByte(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a short.
   */
  public void setShort(int index, short value)
    throws SQLException
  {
    try {
      _pstmt.setShort(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as an int
   */
  public void setInt(int index, int value)
    throws SQLException
  {
    try {
      _pstmt.setInt(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a long
   */
  public void setLong(int index, long value)
    throws SQLException
  {
    try {
      _pstmt.setLong(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a float
   */
  public void setFloat(int index, float value)
    throws SQLException
  {
    try {
      _pstmt.setFloat(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a double
   */
  public void setDouble(int index, double value)
    throws SQLException
  {
    try {
      _pstmt.setDouble(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a BigDecimal
   */
  public void setBigDecimal(int index, BigDecimal value)
    throws SQLException
  {
    try {
      _pstmt.setBigDecimal(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a string
   */
  public void setString(int index, String value)
    throws SQLException
  {
    try {
      _pstmt.setString(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a byte array.
   */
  public void setBytes(int index, byte []value)
    throws SQLException
  {
    try {
      _pstmt.setBytes(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a date
   */
  public void setDate(int index, Date value)
    throws SQLException
  {
    try {
      _pstmt.setDate(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a time
   */
  public void setDate(int index, Date value, Calendar cal)
    throws SQLException
  {
    try {
      _pstmt.setDate(index, value, cal);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a time
   */
  public void setTime(int index, Time value)
    throws SQLException
  {
    try {
      _pstmt.setTime(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a time
   */
  public void setTime(int index, Time value, Calendar cal)
    throws SQLException
  {
    try {
      _pstmt.setTime(index, value, cal);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a timestamp
   */
  public void setTimestamp(int index, Timestamp value)
    throws SQLException
  {
    try {
      _pstmt.setTimestamp(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a timestamp
   */
  public void setTimestamp(int index, Timestamp value, Calendar cal)
    throws SQLException
  {
    try {
      _pstmt.setTimestamp(index, value, cal);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as an ascii stream.
   */
  public void setAsciiStream(int index, InputStream value, int length)
    throws SQLException
  {
    try {
      _pstmt.setAsciiStream(index, value, length);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a unicode stream.
   */
  public void setUnicodeStream(int index, InputStream value, int length)
    throws SQLException
  {
    try {
      _pstmt.setUnicodeStream(index, value, length);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a binary stream.
   */
  public void setBinaryStream(int index, InputStream value, int length)
    throws SQLException
  {
    try {
      _pstmt.setBinaryStream(index, value, length);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as an character stream.
   */
  public void setCharacterStream(int index, Reader value, int length)
    throws SQLException
  {
    try {
      _pstmt.setCharacterStream(index, value, length);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as an object with the given type and scale.
   */
  public void setObject(int index, Object value, int type, int scale)
    throws SQLException
  {
    try {
      _pstmt.setObject(index, value, type, scale);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  public void setAsciiStream(int parameterIndex,
                             InputStream x,
                             long length)
    throws SQLException
  {
    _pstmt.setAsciiStream(parameterIndex, x, length);
  }

  public void setBinaryStream(int parameterIndex,
                              InputStream x,
                              long length)
    throws SQLException
  {
    _pstmt.setBinaryStream(parameterIndex, x, length);
  }

  public void setCharacterStream(int parameterIndex,
                                 Reader reader,
                                 long length)
    throws SQLException
  {
    _pstmt.setCharacterStream(parameterIndex, reader, length);
  }

  public void setAsciiStream(int parameterIndex, InputStream x)
    throws SQLException
  {
    _pstmt.setAsciiStream(parameterIndex, x);
  }

  public void setBinaryStream(int parameterIndex, InputStream x)
    throws SQLException
  {
    _pstmt.setBinaryStream(parameterIndex, x);
  }

  public void setCharacterStream(int parameterIndex, Reader reader)
    throws SQLException
  {
    _pstmt.setCharacterStream(parameterIndex, reader);
  }

  public void setNCharacterStream(int parameterIndex, Reader value)
    throws SQLException
  {
    _pstmt.setNCharacterStream(parameterIndex, value);
  }

  public void setClob(int parameterIndex, Reader reader)
    throws SQLException
  {
    _pstmt.setClob(parameterIndex, reader);
  }

  public void setBlob(int parameterIndex, InputStream inputStream)
    throws SQLException
  {
    _pstmt.setBlob(parameterIndex, inputStream);
  }

  public void setNClob(int parameterIndex, Reader reader)
    throws SQLException
  {
    _pstmt.setNClob(parameterIndex, reader);
  }

  /**
   * Sets the parameter as an object with the given type.
   */
  public void setObject(int index, Object value, int type)
    throws SQLException
  {
    try {
      _pstmt.setObject(index, value, type);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a object.
   */
  public void setObject(int index, Object value)
    throws SQLException
  {
    try {
      _pstmt.setObject(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets teh parameter as a ref.
   */
  public void setRef(int index, Ref value)
    throws SQLException
  {
    try {
      _pstmt.setRef(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a blob.
   */
  public void setBlob(int index, Blob value)
    throws SQLException
  {
    try {
      _pstmt.setBlob(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a clob.
   */
  public void setClob(int index, Clob value)
    throws SQLException
  {
    try {
      _pstmt.setClob(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as an array
   */
  public void setArray(int index, Array value)
    throws SQLException
  {
    try {
      _pstmt.setArray(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the parameter as a URL.
   */
  public void setURL(int index, URL value)
    throws SQLException
  {
    try {
      _pstmt.setURL(index, value);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Closes the prepared statement.
   */
  public void close()
    throws SQLException
  {
    synchronized (this) {
      if (_isClosed)
        return;
      _isClosed = true;
    }
    
    clearParameters();

    if (_cacheItem == null)
      super.close();
    else if (! isPoolable())
      _cacheItem.destroy();
    else
      _cacheItem.toIdle();
  }

  public String toString()
  {
    return getClass().getSimpleName() + '[' + _pstmt + ']';
  }

  public void setRowId(int parameterIndex, RowId x)
    throws SQLException
  {
    _pstmt.setRowId(parameterIndex, x);
  }

  public void setNString(int parameterIndex, String value)
    throws SQLException
  {
    _pstmt.setNString(parameterIndex, value);
  }

  public void setNCharacterStream(int parameterIndex,
                                  Reader value,
                                  long length)
    throws SQLException
  {
    _pstmt.setNCharacterStream(parameterIndex, value, length);
  }

  public void setNClob(int parameterIndex, NClob value)
    throws SQLException
  {
    _pstmt.setNClob(parameterIndex, value);
  }

  public void setClob(int parameterIndex, Reader reader, long length)
    throws SQLException
  {
    _pstmt.setClob(parameterIndex, reader, length);
  }

  public void setBlob(int parameterIndex,
                      InputStream inputStream,
                      long length)
    throws SQLException
  {
    _pstmt.setBlob(parameterIndex, inputStream, length);
  }

  public void setNClob(int parameterIndex, Reader reader, long length)
    throws SQLException
  {
    _pstmt.setNClob(parameterIndex, reader, length);
  }

  public void setSQLXML(int parameterIndex, SQLXML xmlObject)
    throws SQLException
  {
    _pstmt.setSQLXML(parameterIndex, xmlObject);
  }
}
