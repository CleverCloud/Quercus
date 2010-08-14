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
package com.caucho.db.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

import com.caucho.db.sql.Query;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.xa.Transaction;

/**
 * The JDBC statement implementation.
 */
public class PreparedStatementImpl extends StatementImpl
  implements PreparedStatement {

  private Query _query;
  private boolean _wasResultSet;
  private ResultSet _resultSet;

  private boolean _isReturnGeneratedKeys;
  private int _count;

  PreparedStatementImpl(ConnectionImpl conn, Query query)
  {
    super(conn);

    _query = query;
  }

  void setReturnGeneratedKeys(boolean isReturnGeneratedKeys)
  {
    _isReturnGeneratedKeys = isReturnGeneratedKeys;
  }

  public java.sql.ResultSetMetaData getMetaData()
  {
    return null;
  }

  public void clearParameters()
    throws SQLException
  {
    getQueryContext().clearParameters();
    // throw new UnsupportedOperationException();
  }

  public void setNull(int parameter, int sqlType)
    throws SQLException
  {
    getQueryContext().setString(parameter, null);
  }

  public void setNull(int parameter, int sqlType, String typeName)
    throws SQLException
  {
    getQueryContext().setString(parameter, null);
  }

  public void setBoolean(int parameter, boolean x)
    throws SQLException
  {
    getQueryContext().setBoolean(parameter, x);
  }

  public void setByte(int parameter, byte x)
    throws SQLException
  {
    getQueryContext().setLong(parameter, x);
  }

  public void setShort(int parameter, short x)
    throws SQLException
  {
    getQueryContext().setLong(parameter, x);
  }

  public void setInt(int parameter, int x)
    throws SQLException
  {
    getQueryContext().setLong(parameter, x);
  }

  public void setLong(int parameter, long x)
    throws SQLException
  {
    getQueryContext().setLong(parameter, x);
  }

  public void setFloat(int parameter, float x)
    throws SQLException
  {
    getQueryContext().setDouble(parameter, x);
  }

  public void setDouble(int parameter, double x)
    throws SQLException
  {
    getQueryContext().setDouble(parameter, x);
  }

  public void setBigDecimal(int parameter, java.math.BigDecimal x)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public void setString(int parameter, String x)
    throws SQLException
  {
    getQueryContext().setString(parameter, x);
  }

  public void setBytes(int parameter, byte []x)
    throws SQLException
  {
    if (x != null) {
      getQueryContext().setBytes(parameter, x);
    }
    else
      setNull(parameter, 0);
  }

  public void setDate(int parameter, java.sql.Date x, Calendar calendar)
    throws SQLException
  {
    setDate(parameter, x);
  }

  public void setDate(int parameter, java.sql.Date x)
    throws SQLException
  {
    if (x != null)
      setTime(parameter, x.getTime());
    else
      setNull(parameter, 0);
  }

  public void setTime(int parameter, Time x, Calendar calendar)
    throws SQLException
  {
    setTime(parameter, x);
  }

  public void setTime(int parameter, Time x)
    throws SQLException
  {
    if (x != null)
      setTime(parameter, x.getTime());
    else
      setNull(parameter, 0);
  }

  public void setTimestamp(int parameter, Timestamp x, Calendar calendar)
    throws SQLException
  {
    setTimestamp(parameter, x);
  }

  public void setTimestamp(int parameter, Timestamp x)
    throws SQLException
  {
    if (x != null)
      setTime(parameter, x.getTime());
    else
      setNull(parameter, 0);
  }

  private void setTime(int parameter, long now)
    throws SQLException
  {
    getQueryContext().setDate(parameter, now);
  }

  public void setAsciiStream(int parameter, InputStream is, int len)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public void setUnicodeStream(int parameter, InputStream is, int len)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public void setBinaryStream(int parameter, InputStream is, int len)
    throws SQLException
  {
    getQueryContext().setBinaryStream(parameter, is, len);
  }

  public void setCharacterStream(int parameter, Reader is, int len)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public void setObject(int parameter, Object x, int target, int scale)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public void setObject(int parameter, Object x, int target)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public void setObject(int parameter, Object x)
    throws SQLException
  {
    if (x instanceof String)
      setString(parameter, (String) x);
    else if (x instanceof Number) {
      Number number = (Number) x;

      if (x instanceof Double)
        setDouble(parameter, number.doubleValue());
      else if (x instanceof java.lang.Float)
        setDouble(parameter, number.doubleValue());
      else
        setLong(parameter, number.longValue());
    }
    else if (x instanceof java.sql.Time)
      setTime(parameter, (java.sql.Time) x);
    else if (x instanceof java.sql.Timestamp)
      setTimestamp(parameter, (java.sql.Timestamp) x);
    else if (x instanceof java.sql.Date)
      setDate(parameter, (java.sql.Date) x);
    else {
      throw new UnsupportedOperationException();
    }
  }

  public void setRef(int parameter, Ref x)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public void setBlob(int parameter, Blob x)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public void setClob(int parameter, Clob x)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public void setArray(int parameter, Array x)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public void addBatch()
    throws SQLException
  {
  }

  public java.sql.ResultSet executeQuery()
    throws SQLException
  {
    execute();

    if (_wasResultSet)
      return _resultSet;
    else
      throw new SQLException("missing result set");
  }

  public int executeUpdate()
    throws SQLException
  {
    execute();

    return getUpdateCount();
  }

  public boolean execute()
    throws SQLException
  {
    _count++;

    Transaction xa = null;

    try {
      if (_count != 1)
        throw new IllegalStateException("Multithreading execute");

      xa = _conn.getTransaction();
      QueryContext queryContext = getQueryContext();

      if (_query.isSelect()) {
        _query.execute(queryContext, xa);

        _wasResultSet = true;
        _resultSet = new ResultSetImpl(this, queryContext.getResult());

        return true;
      }
      else {
        queryContext.setReturnGeneratedKeys(_isReturnGeneratedKeys);

        _query.execute(queryContext, xa);

        _wasResultSet = false;
        return false;
      }
    } finally {
      _count--;

      if (xa != null && xa.isAutoCommit())
        xa.rollback();
    }
  }

  // JDK 1.4
  public void setURL(int foo, java.net.URL url)
  {
    throw new UnsupportedOperationException();
  }

  public ParameterMetaData getParameterMetaData()
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
