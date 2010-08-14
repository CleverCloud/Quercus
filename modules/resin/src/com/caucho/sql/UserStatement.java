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

import com.caucho.env.meter.ActiveTimeSensor;
import com.caucho.util.L10N;
import com.caucho.util.Alarm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.logging.Logger;


/**
 * User-view of a statement;
 */
public class UserStatement implements Statement {
  private final static Logger log
    = Logger.getLogger(UserStatement.class.getName());
  protected final static L10N L = new L10N(UserStatement.class);

  // The connection
  protected UserConnection _conn;

  // The underlying connection
  protected Statement _stmt;

  // True if the statement is changed in a way that forbids its caching.
  protected boolean _isPoolable = true;

  private final ActiveTimeSensor _timeProbe;

  UserStatement(UserConnection conn, Statement stmt)
  {
    _conn = conn;
    _stmt = stmt;
    _timeProbe = conn.getTimeProbe();
  }

  public void setPoolable(boolean poolable)
    throws SQLException
  {
    if (! poolable)
      _isPoolable = false;
  }

  public boolean isPoolable()
    throws SQLException
  {
    return _isPoolable;
  }

  /**
   * Returns the underlying statement.
   */
  public Statement getStatement()
  {
    Statement stmt = _stmt;

    if (stmt instanceof com.caucho.sql.spy.SpyStatement) {
      stmt = ((com.caucho.sql.spy.SpyStatement)stmt).getStatement();
    }

    return stmt;
  }

  public void addBatch(String sql)
    throws SQLException
  {
    try {
      _stmt.addBatch(sql);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  public void cancel()
    throws SQLException
  {
    try {
      _stmt.cancel();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  public void clearBatch()
    throws SQLException
  {
    try {
      _stmt.clearBatch();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  public void clearWarnings()
    throws SQLException
  {
    try {
      Statement stmt = _stmt;
      
      if (stmt != null)
        stmt.clearWarnings();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Closes the statement.
   */
  public void close()
    throws SQLException
  {
    try {
      Statement stmt = _stmt;
      _stmt = null;

      if (stmt != null) {
        _conn.closeStatement(stmt);

        stmt.close();
      }
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * queries the database with the given sql.
   */
  public ResultSet executeQuery(String sql)
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _stmt.executeQuery(sql);
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
   * updates the database with the given sql.
   */
  public int executeUpdate(String sql)
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _stmt.executeUpdate(sql);
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
   * Execute an update with the given result type.
   */
  public int executeUpdate(String query, int resultType)
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _stmt.executeUpdate(query, resultType);
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
   * Execute an update checking the given columns for primary keys.
   */
  public int executeUpdate(String query, int []columns)
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _stmt.executeUpdate(query, columns);
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
   * Execute an update checking the given columns for primary keys.
   */
  public int executeUpdate(String query, String []columns)
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _stmt.executeUpdate(query, columns);
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
   * executes the given sql.
   */
  public boolean execute(String sql)
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _stmt.execute(sql);
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
   * executes the given query with its result type.
   */
  public boolean execute(String query, int resultType)
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _stmt.execute(query, resultType);
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
   * executes the given query with the columns given for
   * primary key generation.
   */
  public boolean execute(String query, int []columns)
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _stmt.execute(query, columns);
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
   * executes the given query with the columns given for
   * primary key generation.
   */
  public boolean execute(String query, String []columns)
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _stmt.execute(query, columns);
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
   * Executes the batched sql.
   */
  public int[]executeBatch()
    throws SQLException
  {
    long startTime = _timeProbe.start();
    
    try {
      return _stmt.executeBatch();
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
   * Returns the result set of the last query.
   */
  public java.sql.ResultSet getResultSet()
    throws SQLException
  {
    try {
      return _stmt.getResultSet();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns the update count of the last query.
   */
  public int getUpdateCount()
    throws SQLException
  {
    try {
      return _stmt.getUpdateCount();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns the underlying connection.
   */
  public Connection getConnection()
    throws SQLException
  {
    return _conn;
  }

  /**
   * Returns the current fetch direction.
   */
  public int getFetchDirection()
    throws SQLException
  {
    try {
      return _stmt.getFetchDirection();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the fetch direction.
   */
  public void setFetchDirection(int direction)
    throws SQLException
  {
    try {
      setPoolable(false);

      _stmt.setFetchDirection(direction);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns the fetch size.
   */
  public int getFetchSize()
    throws SQLException
  {
    try {
      return _stmt.getFetchSize();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the fetch size.
   */
  public void setFetchSize(int rows)
    throws SQLException
  {
    try {
      setPoolable(false);

      _stmt.setFetchSize(rows);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns the maximum field size.
   */
  public int getMaxFieldSize()
    throws SQLException
  {
    try {
      return _stmt.getMaxFieldSize();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the maximum field size.
   */
  public void setMaxFieldSize(int max)
    throws SQLException
  {
    try {
      setPoolable(false);

      _stmt.setMaxFieldSize(max);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns the maximum rows returned by a query.
   */
  public int getMaxRows()
    throws SQLException
  {
    try {
      return _stmt.getMaxRows();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the maximum rows returned by a query.
   */
  public void setMaxRows(int max)
    throws SQLException
  {
    try {
      setPoolable(false);

      _stmt.setMaxRows(max);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns true if more results are available.
   */
  public boolean getMoreResults()
    throws SQLException
  {
    try {
      return _stmt.getMoreResults();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns the current query timeout.
   */
  public int getQueryTimeout()
    throws SQLException
  {
    try {
      return _stmt.getQueryTimeout();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the query timeout.
   */
  public void setQueryTimeout(int seconds)
    throws SQLException
  {
    try {
      setPoolable(false);

      _stmt.setQueryTimeout(seconds);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns the statement's result set concurrency setting.
   */
  public int getResultSetConcurrency()
    throws SQLException
  {
    try {
      return _stmt.getResultSetConcurrency();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns the statement's result set type.
   */
  public int getResultSetType()
    throws SQLException
  {
    try {
      return _stmt.getResultSetType();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns the current sql warnings.
   */
  public SQLWarning getWarnings()
    throws SQLException
  {
    try {
      return _stmt.getWarnings();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Sets the current cursor name.
   */
  public void setCursorName(String name)
    throws SQLException
  {
    try {
      setPoolable(false);

      _stmt.setCursorName(name);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Enables escape processing.
   */
  public void setEscapeProcessing(boolean enable)
    throws SQLException
  {
    try {
      setPoolable(false);

      _stmt.setEscapeProcessing(enable);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns the next count results.
   */
  public boolean getMoreResults(int count)
    throws SQLException
  {
    try {
      return _stmt.getMoreResults(count);
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns the generated keys for the update.
   */
  public java.sql.ResultSet getGeneratedKeys()
    throws SQLException
  {
    try {
      return _stmt.getGeneratedKeys();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  /**
   * Returns the result set holdability.
   */
  public int getResultSetHoldability()
    throws SQLException
  {
    try {
      return _stmt.getResultSetHoldability();
    } catch (RuntimeException e) {
      killPool();
      throw e;
    } catch (SQLException e) {
      killPool();
      throw e;
    }
  }

  public boolean isClosed() throws SQLException
  {
    return _stmt == null;
  }

  public <T> T unwrap(Class<T> iface)
    throws SQLException
  {
    return (T) _stmt;
  }

  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    return (iface.isAssignableFrom(_stmt.getClass()));
  }

  /**
   * Marks the connection as non-poolable.  When the connection is closed,
   * it will actually be closed, not returned to the idle pool.
   */
  protected void killPool()
  {
    if (_conn != null)
      _conn.killPool();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _stmt + "]";
  }
}
