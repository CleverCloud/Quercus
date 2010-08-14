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

public class CallableStatementWrapper
  implements CallableStatement
{
  private final CallableStatement _callableStatement;
  private final ProfilerPoint _profilerPoint;

  public CallableStatementWrapper(ProfilerPoint profilerPoint,
                                  CallableStatement callableStatement)
  {
    _profilerPoint = profilerPoint;
    _callableStatement = callableStatement;
  }

  public void registerOutParameter(int parameterIndex, int sqlType)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.registerOutParameter(parameterIndex, sqlType);
    }
    finally {
      profiler.finish();
    }
  }

  public void registerOutParameter(int parameterIndex, int sqlType, int scale)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.registerOutParameter(parameterIndex, sqlType, scale);
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
      return _callableStatement.wasNull();
    }
    finally {
      profiler.finish();
    }
  }

  public String getString(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getString(parameterIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean getBoolean(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getBoolean(parameterIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public byte getByte(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getByte(parameterIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public short getShort(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getShort(parameterIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public int getInt(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getInt(parameterIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public long getLong(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getLong(parameterIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public float getFloat(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getFloat(parameterIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public double getDouble(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getDouble(parameterIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public BigDecimal getBigDecimal(int parameterIndex, int scale)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getBigDecimal(parameterIndex, scale);
    }
    finally {
      profiler.finish();
    }
  }

  public byte[] getBytes(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getBytes(parameterIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public Date getDate(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getDate(parameterIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public Time getTime(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getTime(parameterIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public Timestamp getTimestamp(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getTimestamp(parameterIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public Object getObject(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getObject(parameterIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public BigDecimal getBigDecimal(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getBigDecimal(parameterIndex);
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
      return _callableStatement.getObject(i, map);
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
      return _callableStatement.getRef(i);
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
      return _callableStatement.getBlob(i);
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
      return _callableStatement.getClob(i);
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
      return _callableStatement.getArray(i);
    }
    finally {
      profiler.finish();
    }
  }

  public Date getDate(int parameterIndex, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getDate(parameterIndex, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public Time getTime(int parameterIndex, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getTime(parameterIndex, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public Timestamp getTimestamp(int parameterIndex, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getTimestamp(parameterIndex, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public void registerOutParameter(int paramIndex, int sqlType, String typeName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.registerOutParameter(paramIndex, sqlType, typeName);
    }
    finally {
      profiler.finish();
    }
  }

  public void registerOutParameter(String parameterName, int sqlType)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.registerOutParameter(parameterName, sqlType);
    }
    finally {
      profiler.finish();
    }
  }

  public void registerOutParameter(String parameterName, int sqlType, int scale)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.registerOutParameter(parameterName, sqlType, scale);
    }
    finally {
      profiler.finish();
    }
  }

  public void registerOutParameter(String parameterName,
                                   int sqlType,
                                   String typeName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.registerOutParameter(parameterName, sqlType, typeName);
    }
    finally {
      profiler.finish();
    }
  }

  public URL getURL(int parameterIndex)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getURL(parameterIndex);
    }
    finally {
      profiler.finish();
    }
  }

  public void setURL(String parameterName, URL val)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setURL(parameterName, val);
    }
    finally {
      profiler.finish();
    }
  }

  public void setNull(String parameterName, int sqlType)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setNull(parameterName, sqlType);
    }
    finally {
      profiler.finish();
    }
  }

  public void setBoolean(String parameterName, boolean x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setBoolean(parameterName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setByte(String parameterName, byte x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setByte(parameterName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setShort(String parameterName, short x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setShort(parameterName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setInt(String parameterName, int x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setInt(parameterName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setLong(String parameterName, long x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setLong(parameterName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setFloat(String parameterName, float x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setFloat(parameterName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setDouble(String parameterName, double x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setDouble(parameterName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setBigDecimal(String parameterName, BigDecimal x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setBigDecimal(parameterName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setString(String parameterName, String x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setString(parameterName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setBytes(String parameterName, byte[] x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setBytes(parameterName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setDate(String parameterName, Date x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setDate(parameterName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setTime(String parameterName, Time x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setTime(parameterName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setTimestamp(String parameterName, Timestamp x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setTimestamp(parameterName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setAsciiStream(String parameterName, InputStream x, int length)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setAsciiStream(parameterName, x, length);
    }
    finally {
      profiler.finish();
    }
  }

  public void setBinaryStream(String parameterName, InputStream x, int length)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setBinaryStream(parameterName, x, length);
    }
    finally {
      profiler.finish();
    }
  }

  public void setObject(String parameterName,
                        Object x,
                        int targetSqlType,
                        int scale)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setObject(parameterName, x, targetSqlType, scale);
    }
    finally {
      profiler.finish();
    }
  }

  public void setObject(String parameterName, Object x, int targetSqlType)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setObject(parameterName, x, targetSqlType);
    }
    finally {
      profiler.finish();
    }
  }

  public void setObject(String parameterName, Object x)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setObject(parameterName, x);
    }
    finally {
      profiler.finish();
    }
  }

  public void setCharacterStream(String parameterName,
                                 Reader reader,
                                 int length)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setCharacterStream(parameterName, reader, length);
    }
    finally {
      profiler.finish();
    }
  }

  public void setDate(String parameterName, Date x, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setDate(parameterName, x, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public void setTime(String parameterName, Time x, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setTime(parameterName, x, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public void setTimestamp(String parameterName, Timestamp x, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setTimestamp(parameterName, x, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public void setNull(String parameterName, int sqlType, String typeName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _callableStatement.setNull(parameterName, sqlType, typeName);
    }
    finally {
      profiler.finish();
    }
  }

  public String getString(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getString(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean getBoolean(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getBoolean(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public byte getByte(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getByte(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public short getShort(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getShort(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public int getInt(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getInt(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public long getLong(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getLong(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public float getFloat(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getFloat(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public double getDouble(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getDouble(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public byte[] getBytes(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getBytes(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public Date getDate(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getDate(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public Time getTime(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getTime(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public Timestamp getTimestamp(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getTimestamp(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public Object getObject(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getObject(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public BigDecimal getBigDecimal(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getBigDecimal(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public Object getObject(String parameterName, Map<String, Class<?>> map)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getObject(parameterName, map);
    }
    finally {
      profiler.finish();
    }
  }

  public Ref getRef(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getRef(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public Blob getBlob(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getBlob(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public Clob getClob(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getClob(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public Array getArray(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getArray(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public Date getDate(String parameterName, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getDate(parameterName, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public Time getTime(String parameterName, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getTime(parameterName, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public Timestamp getTimestamp(String parameterName, Calendar cal)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getTimestamp(parameterName, cal);
    }
    finally {
      profiler.finish();
    }
  }

  public URL getURL(String parameterName)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.getURL(parameterName);
    }
    finally {
      profiler.finish();
    }
  }

  public ResultSet executeQuery()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.executeQuery();
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
      return _callableStatement.executeUpdate();
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
      _callableStatement.setNull(parameterIndex, sqlType);
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
      _callableStatement.setBoolean(parameterIndex, x);
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
      _callableStatement.setByte(parameterIndex, x);
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
      _callableStatement.setShort(parameterIndex, x);
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
      _callableStatement.setInt(parameterIndex, x);
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
      _callableStatement.setLong(parameterIndex, x);
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
      _callableStatement.setFloat(parameterIndex, x);
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
      _callableStatement.setDouble(parameterIndex, x);
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
      _callableStatement.setBigDecimal(parameterIndex, x);
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
      _callableStatement.setString(parameterIndex, x);
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
      _callableStatement.setBytes(parameterIndex, x);
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
      _callableStatement.setDate(parameterIndex, x);
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
      _callableStatement.setTime(parameterIndex, x);
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
      _callableStatement.setTimestamp(parameterIndex, x);
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
      _callableStatement.setAsciiStream(parameterIndex, x, length);
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
      _callableStatement.setUnicodeStream(parameterIndex, x, length);
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
      _callableStatement.setBinaryStream(parameterIndex, x, length);
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
      _callableStatement.clearParameters();
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
      _callableStatement.setObject(parameterIndex, x, targetSqlType, scale);
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
      _callableStatement.setObject(parameterIndex, x, targetSqlType);
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
      _callableStatement.setObject(parameterIndex, x);
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
      return _callableStatement.execute();
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
      _callableStatement.addBatch();
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
      _callableStatement.setCharacterStream(parameterIndex, reader, length);
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
      _callableStatement.setRef(i, x);
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
      _callableStatement.setBlob(i, x);
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
      _callableStatement.setClob(i, x);
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
      _callableStatement.setArray(i, x);
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
      return _callableStatement.getMetaData();
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
      _callableStatement.setDate(parameterIndex, x, cal);
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
      _callableStatement.setTime(parameterIndex, x, cal);
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
      _callableStatement.setTimestamp(parameterIndex, x, cal);
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
      _callableStatement.setNull(paramIndex, sqlType, typeName);
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
      _callableStatement.setURL(parameterIndex, x);
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
      return _callableStatement.getParameterMetaData();
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
      return _callableStatement.executeQuery(sql);
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
      return _callableStatement.executeUpdate(sql);
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
      _callableStatement.close();
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
      return _callableStatement.getMaxFieldSize();
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
      _callableStatement.setMaxFieldSize(max);
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
      return _callableStatement.getMaxRows();
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
      _callableStatement.setMaxRows(max);
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
      _callableStatement.setEscapeProcessing(enable);
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
      return _callableStatement.getQueryTimeout();
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
      _callableStatement.setQueryTimeout(seconds);
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
      _callableStatement.cancel();
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
      return _callableStatement.getWarnings();
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
      _callableStatement.clearWarnings();
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
      _callableStatement.setCursorName(name);
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
      return _callableStatement.execute(sql);
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
      return _callableStatement.getResultSet();
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
      return _callableStatement.getUpdateCount();
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
      return _callableStatement.getMoreResults();
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
      _callableStatement.setFetchDirection(direction);
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
      return _callableStatement.getFetchDirection();
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
      _callableStatement.setFetchSize(rows);
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
      return _callableStatement.getFetchSize();
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
      return _callableStatement.getResultSetConcurrency();
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
      return _callableStatement.getResultSetType();
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
      _callableStatement.addBatch(sql);
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
      _callableStatement.clearBatch();
    }
    finally {
      profiler.finish();
    }
  }

  public int[] executeBatch()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _callableStatement.executeBatch();
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
      return _callableStatement.getConnection();
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
      return _callableStatement.getMoreResults(current);
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
      return _callableStatement.getGeneratedKeys();
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
      return _callableStatement.executeUpdate(sql, autoGeneratedKeys);
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
      return _callableStatement.executeUpdate(sql, columnIndexes);
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
      return _callableStatement.executeUpdate(sql, columnNames);
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
      return _callableStatement.execute(sql, autoGeneratedKeys);
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
      return _callableStatement.execute(sql, columnIndexes);
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
      return _callableStatement.execute(sql, columnNames);
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
      return _callableStatement.getResultSetHoldability();
    }
    finally {
      profiler.finish();
    }
  }

  public String toString()
  {
    return "CallableStatementWrapper[" + _profilerPoint.getName() + "]";
  }

    public RowId getRowId(int parameterIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public RowId getRowId(String parameterName) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setRowId(String parameterName, RowId x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNString(String parameterName, String value) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNClob(String parameterName, NClob value) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NClob getNClob(int parameterIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NClob getNClob(String parameterName) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SQLXML getSQLXML(String parameterName) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getNString(int parameterIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getNString(String parameterName) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Reader getNCharacterStream(String parameterName) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Reader getCharacterStream(String parameterName) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBlob(String parameterName, Blob x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setClob(String parameterName, Clob x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setClob(String parameterName, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setNClob(String parameterName, Reader reader) throws SQLException {
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
