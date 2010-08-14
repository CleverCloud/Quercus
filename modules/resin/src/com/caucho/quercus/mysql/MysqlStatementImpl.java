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

import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import javax.sql.*;

/**
 * Special Quercus Mysql connection.
 */
public class MysqlStatementImpl implements java.sql.Statement {
  private static final Logger log
    = Logger.getLogger(MysqlStatementImpl.class.getName());
  private static final L10N L = new L10N(MysqlStatementImpl.class);

  private MysqlConnectionImpl _conn;
  private MysqlResultImpl _result;

  MysqlStatementImpl(MysqlConnectionImpl conn)
    throws SQLException
  {
    if (conn == null)
      throw new NullPointerException();

    _conn = conn;

    _result = new MysqlResultImpl(this);
  }

  /**
   * executes the given sql.
   */
  public boolean execute(String sql)
    throws SQLException
  {
    _conn.writeQuery(sql);

    _conn.readResult(_result);

    return _result.isResultSet();
  }

  /**
   * queries the database with the given sql.
   */
  public ResultSet executeQuery(String sql)
    throws SQLException
  {
    _conn.writeQuery(sql);

    _conn.readResult(_result);

    return _result;
  }

  /**
   * updates the database with the given sql.
   */
  public int executeUpdate(String sql)
    throws SQLException
  {
    _conn.writeQuery(sql);

    _conn.readResult(_result);

    return _result.getUpdateCount();
  }

  public java.sql.ResultSet getResultSet()
    throws SQLException
  {
    return _result;
  }

  public int getUpdateCount()
    throws SQLException
  {
    return _result.getUpdateCount();
  }

  public MysqlConnectionImpl getConnection()
    throws SQLException
  {
    return _conn;
  }

  public void clearWarnings()
    throws SQLException
  {
  }

  /**
   * Returns the current sql warnings.
   */
  public SQLWarning getWarnings()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public boolean isClosed() throws SQLException
  {
    return _conn == null;
  }

  public void close()
    throws SQLException
  {
    _conn = null;
  }

  //
  // stubbed because not used by mysql
  //

  public void addBatch(String sql)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void cancel()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void clearBatch()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Execute an update with the given result type.
   */
  public int executeUpdate(String query, int resultType)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Execute an update checking the given columns for primary keys.
   */
  public int executeUpdate(String query, int []columns)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Execute an update checking the given columns for primary keys.
   */
  public int executeUpdate(String query, String []columns)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * executes the given query with its result type.
   */
  public boolean execute(String query, int resultType)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * executes the given query with the columns given for
   * primary key generation.
   */
  public boolean execute(String query, int []columns)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * executes the given query with the columns given for
   * primary key generation.
   */
  public boolean execute(String query, String []columns)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Executes the batched sql.
   */
  public int[]executeBatch()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the current fetch direction.
   */
  public int getFetchDirection()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the fetch direction.
   */
  public void setFetchDirection(int direction)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the fetch size.
   */
  public int getFetchSize()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the fetch size.
   */
  public void setFetchSize(int rows)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the maximum field size.
   */
  public int getMaxFieldSize()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the maximum field size.
   */
  public void setMaxFieldSize(int max)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the maximum rows returned by a query.
   */
  public int getMaxRows()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the maximum rows returned by a query.
   */
  public void setMaxRows(int max)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if more results are available.
   */
  public boolean getMoreResults()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the current query timeout.
   */
  public int getQueryTimeout()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the query timeout.
   */
  public void setQueryTimeout(int seconds)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the statement's result set concurrency setting.
   */
  public int getResultSetConcurrency()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the statement's result set type.
   */
  public int getResultSetType()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the current cursor name.
   */
  public void setCursorName(String name)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Enables escape processing.
   */
  public void setEscapeProcessing(boolean enable)
    throws SQLException
  {
  }

  /**
   * Returns the next count results.
   */
  public boolean getMoreResults(int count)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the generated keys for the update.
   */
  public java.sql.ResultSet getGeneratedKeys()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the result set holdability.
   */
  public int getResultSetHoldability()
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public <T> T unwrap(Class<T> iface)
    throws SQLException
  {
    return null;
  }

  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    return false;
  }

  public boolean isPoolable()
  {
    return false;
  }

  public void setPoolable(boolean isPoolable)
  {
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _conn + "]";
  }
}
