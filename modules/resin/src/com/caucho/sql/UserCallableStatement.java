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

package com.caucho.sql;

import com.caucho.util.L10N;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Logger;

/**
 * User-view of prepared statements
 */
public class UserCallableStatement extends UserPreparedStatement
  implements CallableStatement {
  protected final static Logger log
    = Logger.getLogger(UserCallableStatement.class.getName());
  protected static L10N L = new L10N(UserCallableStatement.class);

  protected CallableStatement _cstmt;
  private boolean _isClosed;

  UserCallableStatement(UserConnection conn,
                        CallableStatement cStmt)
  {
    super(conn, cStmt);
    
    _cstmt = cStmt;

    if (cStmt == null)
      throw new NullPointerException();
  }

  /**
   * The array value
   */
  public Array getArray(int i)
    throws SQLException
  {
    return _cstmt.getArray(i);
  }

  /**
   * The array value
   */
  public Array getArray(String name)
    throws SQLException
  {
    return _cstmt.getArray(name);
  }

  /**
   * The big decimal value
   */
  public BigDecimal getBigDecimal(int i)
    throws SQLException
  {
    return _cstmt.getBigDecimal(i);
  }

  /**
   * The bigDecimal value
   */
  public BigDecimal getBigDecimal(String name)
    throws SQLException
  {
    return _cstmt.getBigDecimal(name);
  }

  /**
   * The big decimal value
   */
  public BigDecimal getBigDecimal(int i, int scale)
    throws SQLException
  {
    return _cstmt.getBigDecimal(i, scale);
  }

  /**
   * The blob value
   */
  public Blob getBlob(int i)
    throws SQLException
  {
    return _cstmt.getBlob(i);
  }

  /**
   * The blob value
   */
  public Blob getBlob(String name)
    throws SQLException
  {
    return _cstmt.getBlob(name);
  }

  /**
   * The boolean value
   */
  public boolean getBoolean(int i)
    throws SQLException
  {
    return _cstmt.getBoolean(i);
  }

  /**
   * The boolean value
   */
  public boolean getBoolean(String name)
    throws SQLException
  {
    return _cstmt.getBoolean(name);
  }

  /**
   * The byte value
   */
  public byte getByte(int i)
    throws SQLException
  {
    return _cstmt.getByte(i);
  }

  /**
   * The byte value
   */
  public byte getByte(String name)
    throws SQLException
  {
    return _cstmt.getByte(name);
  }

  /**
   * The bytes value
   */
  public byte []getBytes(int i)
    throws SQLException
  {
    return _cstmt.getBytes(i);
  }

  /**
   * The bytes value
   */
  public byte []getBytes(String name)
    throws SQLException
  {
    return _cstmt.getBytes(name);
  }
  
  /**
   * The clob value
   */
  public Clob getClob(int i)
    throws SQLException
  {
    return _cstmt.getClob(i);
  }

  /**
   * The clob value
   */
  public Clob getClob(String name)
    throws SQLException
  {
    return _cstmt.getClob(name);
  }
  
  /**
   * The date value
   */
  public Date getDate(int i)
    throws SQLException
  {
    return _cstmt.getDate(i);
  }

  /**
   * The date value
   */
  public Date getDate(String name)
    throws SQLException
  {
    return _cstmt.getDate(name);
  }
  
  /**
   * The date value
   */
  public Date getDate(int i, Calendar cal)
    throws SQLException
  {
    return _cstmt.getDate(i, cal);
  }

  /**
   * The date value
   */
  public Date getDate(String name, Calendar cal)
    throws SQLException
  {
    return _cstmt.getDate(name);
  }
  
  /**
   * The double value
   */
  public double getDouble(int i)
    throws SQLException
  {
    return _cstmt.getDouble(i);
  }

  /**
   * The double value
   */
  public double getDouble(String name)
    throws SQLException
  {
    return _cstmt.getDouble(name);
  }
  
  /**
   * The float value
   */
  public float getFloat(int i)
    throws SQLException
  {
    return _cstmt.getFloat(i);
  }

  /**
   * The float value
   */
  public float getFloat(String name)
    throws SQLException
  {
    return _cstmt.getFloat(name);
  }
  
  /**
   * The int value
   */
  public int getInt(int i)
    throws SQLException
  {
    return _cstmt.getInt(i);
  }

  /**
   * The int value
   */
  public int getInt(String name)
    throws SQLException
  {
    return _cstmt.getInt(name);
  }
  
  /**
   * The long value
   */
  public long getLong(int i)
    throws SQLException
  {
    return _cstmt.getLong(i);
  }

  /**
   * The long value
   */
  public long getLong(String name)
    throws SQLException
  {
    return _cstmt.getLong(name);
  }
  
  /**
   * The object value
   */
  public Object getObject(int i)
    throws SQLException
  {
    return _cstmt.getObject(i);
  }

  /**
   * The object value
   */
  public Object getObject(String name)
    throws SQLException
  {
    return _cstmt.getObject(name);
  }
  
  /**
   * The object value
   */
  public Object getObject(int i, Map<String,Class<?>> map)
    throws SQLException
  {
    return _cstmt.getObject(i);
  }

  /**
   * The object value
   */
  public Object getObject(String name, Map<String,Class<?>> map)
    throws SQLException
  {
    return _cstmt.getObject(name);
  }
  
  /**
   * The ref value
   */
  public Ref getRef(int i)
    throws SQLException
  {
    return _cstmt.getRef(i);
  }

  /**
   * The ref value
   */
  public Ref getRef(String name)
    throws SQLException
  {
    return _cstmt.getRef(name);
  }
  
  /**
   * The short value
   */
  public short getShort(int i)
    throws SQLException
  {
    return _cstmt.getShort(i);
  }

  /**
   * The short value
   */
  public short getShort(String name)
    throws SQLException
  {
    return _cstmt.getShort(name);
  }
  
  /**
   * The string value
   */
  public String getString(int i)
    throws SQLException
  {
    return _cstmt.getString(i);
  }

  /**
   * The string value
   */
  public String getString(String name)
    throws SQLException
  {
    return _cstmt.getString(name);
  }
  
  /**
   * The time value
   */
  public Time getTime(int i)
    throws SQLException
  {
    return _cstmt.getTime(i);
  }

  /**
   * The time value
   */
  public Time getTime(String name)
    throws SQLException
  {
    return _cstmt.getTime(name);
  }
  
  /**
   * The time value
   */
  public Time getTime(int i, Calendar cal)
    throws SQLException
  {
    return _cstmt.getTime(i, cal);
  }

  /**
   * The time value
   */
  public Time getTime(String name, Calendar cal)
    throws SQLException
  {
    return _cstmt.getTime(name);
  }
  
  /**
   * The timestamp value
   */
  public Timestamp getTimestamp(int i)
    throws SQLException
  {
    return _cstmt.getTimestamp(i);
  }

  /**
   * The timestamp value
   */
  public Timestamp getTimestamp(String name)
    throws SQLException
  {
    return _cstmt.getTimestamp(name);
  }
  
  /**
   * The timestamp value
   */
  public Timestamp getTimestamp(int i, Calendar cal)
    throws SQLException
  {
    return _cstmt.getTimestamp(i, cal);
  }

  /**
   * The timestamp value
   */
  public Timestamp getTimestamp(String name, Calendar cal)
    throws SQLException
  {
    return _cstmt.getTimestamp(name);
  }
  
  /**
   * The URL value
   */
  public URL getURL(int i)
    throws SQLException
  {
    return _cstmt.getURL(i);
  }

  /**
   * The URL value
   */
  public URL getURL(String name)
    throws SQLException
  {
    return _cstmt.getURL(name);
  }

  /**
   * Registers the out parameter.
   */
  public void registerOutParameter(int parameterIndex, int sqlType)
    throws SQLException
  {
    _cstmt.registerOutParameter(parameterIndex, sqlType);
  }

  /**
   * Registers the out parameter.
   */
  public void registerOutParameter(int parameterIndex, int sqlType, int scale)
    throws SQLException
  {
    _cstmt.registerOutParameter(parameterIndex, sqlType, scale);
  }

  /**
   * Registers the out parameter.
   */
  public void registerOutParameter(int parameterIndex, int sqlType,
                                   String typeName)
    throws SQLException
  {
    _cstmt.registerOutParameter(parameterIndex, sqlType, typeName);
  }

  /**
   * Registers the out parameter.
   */
  public void registerOutParameter(String parameterName, int sqlType)
    throws SQLException
  {
    _cstmt.registerOutParameter(parameterName, sqlType);
  }

  /**
   * Registers the out parameter.
   */
  public void registerOutParameter(String parameterName, int sqlType, int scale)
    throws SQLException
  {
    _cstmt.registerOutParameter(parameterName, sqlType, scale);
  }

  /**
   * Registers the out parameter.
   */
  public void registerOutParameter(String parameterName, int sqlType,
                                   String typeName)
    throws SQLException
  {
    _cstmt.registerOutParameter(parameterName, sqlType, typeName);
  }

  /**
   * Sets the asciiStream
   */
  public void setAsciiStream(String parameterName,
                             InputStream x,
                             int length)
    throws SQLException
  {
    _cstmt.setAsciiStream(parameterName, x, length);
  }

  /**
   * Sets the bigDecimal
   */
  public void setBigDecimal(String parameterName,
                            BigDecimal x)
    throws SQLException
  {
    _cstmt.setBigDecimal(parameterName, x);
  }

  /**
   * Sets the binaryStream
   */
  public void setBinaryStream(String parameterName,
                              InputStream x,
                              int length)
    throws SQLException
  {
    _cstmt.setBinaryStream(parameterName, x, length);
  }

  /**
   * Sets the boolean
   */
  public void setBoolean(String parameterName,
                         boolean x)
    throws SQLException
  {
    _cstmt.setBoolean(parameterName, x);
  }

  /**
   * Sets the byte
   */
  public void setByte(String parameterName,
                      byte x)
    throws SQLException
  {
    _cstmt.setByte(parameterName, x);
  }

  /**
   * Sets the bytes
   */
  public void setBytes(String parameterName,
                       byte []x)
    throws SQLException
  {
    _cstmt.setBytes(parameterName, x);
  }

  /**
   * Sets the character stream
   */
  public void setCharacterStream(String parameterName,
                                 Reader reader,
                                 int length)
    throws SQLException
  {
    _cstmt.setCharacterStream(parameterName, reader, length);
  }

  /**
   * Sets the date
   */
  public void setDate(String parameterName,
                      Date x)
    throws SQLException
  {
    _cstmt.setDate(parameterName, x);
  }

  /**
   * Sets the date
   */
  public void setDate(String parameterName,
                      Date x,
                      Calendar cal)
    throws SQLException
  {
    _cstmt.setDate(parameterName, x, cal);
  }

  /**
   * Sets the double
   */
  public void setDouble(String parameterName,
                        double x)
    throws SQLException
  {
    _cstmt.setDouble(parameterName, x);
  }

  /**
   * Sets the float
   */
  public void setFloat(String parameterName,
                        float x)
    throws SQLException
  {
    _cstmt.setFloat(parameterName, x);
  }

  /**
   * Sets the int
   */
  public void setInt(String parameterName,
                        int x)
    throws SQLException
  {
    _cstmt.setInt(parameterName, x);
  }

  /**
   * Sets the long
   */
  public void setLong(String parameterName,
                        long x)
    throws SQLException
  {
    _cstmt.setLong(parameterName, x);
  }

  /**
   * Sets the null
   */
  public void setNull(String parameterName,
                      int sqlType)
    throws SQLException
  {
    _cstmt.setNull(parameterName, sqlType);
  }

  /**
   * Sets the null
   */
  public void setNull(String parameterName,
                      int sqlType,
                      String typeName)
    throws SQLException
  {
    _cstmt.setNull(parameterName, sqlType, typeName);
  }

  /**
   * Sets the object
   */
  public void setObject(String parameterName,
                        Object x)
    throws SQLException
  {
    _cstmt.setObject(parameterName, x);
  }

  /**
   * Sets the object
   */
  public void setObject(String parameterName,
                        Object x, int type)
    throws SQLException
  {
    _cstmt.setObject(parameterName, x, type);
  }

  /**
   * Sets the object
   */
  public void setObject(String parameterName,
                        Object x, int type, int scale)
    throws SQLException
  {
    _cstmt.setObject(parameterName, x, type, scale);
  }

  /**
   * Sets the short
   */
  public void setShort(String parameterName,
                        short x)
    throws SQLException
  {
    _cstmt.setShort(parameterName, x);
  }

  /**
   * Sets the string
   */
  public void setString(String parameterName,
                        String x)
    throws SQLException
  {
    _cstmt.setString(parameterName, x);
  }

  /**
   * Sets the time
   */
  public void setTime(String parameterName,
                      Time x)
    throws SQLException
  {
    _cstmt.setTime(parameterName, x);
  }

  /**
   * Sets the time
   */
  public void setTime(String parameterName,
                      Time x,
                      Calendar cal)
    throws SQLException
  {
    _cstmt.setTime(parameterName, x, cal);
  }

  /**
   * Sets the timestamp
   */
  public void setTimestamp(String parameterName,
                           Timestamp x)
    throws SQLException
  {
    _cstmt.setTimestamp(parameterName, x);
  }

  /**
   * Sets the timestamp
   */
  public void setTimestamp(String parameterName,
                      Timestamp x,
                      Calendar cal)
    throws SQLException
  {
    _cstmt.setTimestamp(parameterName, x, cal);
  }

  /**
   * Sets the URL
   */
  public void setURL(String parameterName,
                           URL x)
    throws SQLException
  {
    _cstmt.setURL(parameterName, x);
  }

  /**
   * Returns true if the last out parameter was null.
   */
  public boolean wasNull()
    throws SQLException
  {
    return _cstmt.wasNull();
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
    
    super.close();
  }

  public RowId getRowId(int parameterIndex)
    throws SQLException
  {
    return _cstmt.getRowId(parameterIndex);
  }

  public RowId getRowId(String parameterName)
    throws SQLException
  {
    return _cstmt.getRowId(parameterName);
  }

  public void setRowId(String parameterName, RowId x)
    throws SQLException
  {
    _cstmt.setRowId(parameterName, x);
  }

  public void setNString(String parameterName, String value)
    throws SQLException
  {
    _cstmt.setNString(parameterName, value);
  }

  public void setNCharacterStream(String parameterName,
                                  Reader value,
                                  long length)
    throws SQLException
  {
    _cstmt.setNCharacterStream(parameterName, value, length);
  }

  public void setNClob(String parameterName, NClob value)
    throws SQLException
  {
    _cstmt.setNClob(parameterName, value);
  }

  public void setClob(String parameterName, Reader reader, long length)
    throws SQLException
  {
    _cstmt.setClob(parameterName, reader, length);
  }

  public void setBlob(String parameterName,
                      InputStream inputStream,
                      long length)
    throws SQLException
  {
    _cstmt.setBlob(parameterName, inputStream, length);
  }

  public void setNClob(String parameterName, Reader reader, long length)
    throws SQLException
  {
    _cstmt.setNClob(parameterName, reader, length);
  }

  public NClob getNClob(int parameterIndex)
    throws SQLException
  {
    return _cstmt.getNClob(parameterIndex);
  }

  public NClob getNClob(String parameterName)
    throws SQLException
  {
    return _cstmt.getNClob(parameterName);
  }

  public void setSQLXML(String parameterName, SQLXML xmlObject)
    throws SQLException
  {
    _cstmt.setSQLXML(parameterName, xmlObject);
  }

  public SQLXML getSQLXML(int parameterIndex)
    throws SQLException
  {
    return _cstmt.getSQLXML(parameterIndex);
  }

  public SQLXML getSQLXML(String parameterName)
    throws SQLException
  {
    return _cstmt.getSQLXML(parameterName);
  }

  public String getNString(int parameterIndex)
    throws SQLException
  {
    return _cstmt.getNString(parameterIndex);
  }

  public String getNString(String parameterName)
    throws SQLException
  {
    return _cstmt.getNString(parameterName);
  }

  public Reader getNCharacterStream(int parameterIndex)
    throws SQLException
  {
    return _cstmt.getNCharacterStream(parameterIndex);
  }

  public Reader getNCharacterStream(String parameterName)
    throws SQLException
  {
    return _cstmt.getNCharacterStream(parameterName);
  }

  public Reader getCharacterStream(int parameterIndex)
    throws SQLException
  {
    return _cstmt.getCharacterStream(parameterIndex);
  }

  public Reader getCharacterStream(String parameterName)
    throws SQLException
  {
    return _cstmt.getCharacterStream(parameterName);
  }

  public void setBlob(String parameterName, Blob x)
    throws SQLException
  {
    _cstmt.setBlob(parameterName, x);
  }

  public void setClob(String parameterName, Clob x)
    throws SQLException
  {
    _cstmt.setClob(parameterName, x);
  }

  public void setAsciiStream(String parameterName,
                             InputStream x,
                             long length)
    throws SQLException
  {
    _cstmt.setAsciiStream(parameterName, x, length);
  }

  public void setBinaryStream(String parameterName,
                              InputStream x,
                              long length)
    throws SQLException
  {
    _cstmt.setBinaryStream(parameterName, x, length);
  }

  public void setCharacterStream(String parameterName,
                                 Reader reader,
                                 long length)
    throws SQLException
  {
    _cstmt.setCharacterStream(parameterName, reader, length);
  }

  public void setAsciiStream(String parameterName, InputStream x)
    throws SQLException
  {
    _cstmt.setAsciiStream(parameterName, x);
  }

  public void setBinaryStream(String parameterName, InputStream x)
    throws SQLException
  {
    _cstmt.setBinaryStream(parameterName, x);
  }

  public void setCharacterStream(String parameterName, Reader reader)
    throws SQLException
  {
    _cstmt.setCharacterStream(parameterName, reader);
  }

  public void setNCharacterStream(String parameterName, Reader value)
    throws SQLException
  {
    _cstmt.setNCharacterStream(parameterName, value);
  }

  public void setClob(String parameterName, Reader reader)
    throws SQLException
  {
    _cstmt.setClob(parameterName, reader);
  }

  public void setBlob(String parameterName, InputStream inputStream)
    throws SQLException
  {
    _cstmt.setBlob(parameterName, inputStream);
  }

  public void setNClob(String parameterName, Reader reader)
    throws SQLException
  {
    _cstmt.setNClob(parameterName, reader);
  }
}
