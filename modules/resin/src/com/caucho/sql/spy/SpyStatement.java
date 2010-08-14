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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.logging.*;

/**
 * Spying on a statement;
 */
public class SpyStatement implements java.sql.Statement {
  protected final static Logger log
    = Logger.getLogger(SpyStatement.class.getName());
  protected final static L10N L = new L10N(SpyConnection.class);

  protected String _id;
  
  // The spy connection
  protected SpyConnection _conn;
  
  // The underlying connection
  protected Statement _stmt;

  SpyStatement(String id, SpyConnection conn, Statement stmt)
  {
    _id = id;

    _conn = conn;
    _stmt = stmt;
  }

  public String getId()
  {
    if (_id == null)
      _id = _conn.createStatementId();

    return _id;
  }

  public Statement getStatement()
  {
    return _stmt;
  }

  public void addBatch(String sql)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":addBatch(" + sql + ")");
      
      _stmt.addBatch(sql);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-addBatch(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void cancel()
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":cancel()");
      
      _stmt.cancel();
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-cancel(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void clearBatch()
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":clearBatch()");
      
      _stmt.clearBatch();
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-clearBatch(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void clearWarnings()
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":clearWarnings()");
      
      _stmt.clearWarnings();
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-clearWarnings(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  @Override
  public void close()
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":close()");
      
      Statement stmt = _stmt;
      _stmt = null;
      
      if (stmt != null)
        stmt.close();
    } catch (RuntimeException e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-close(" + e + ")");
      
    } catch (Exception e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-close(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public java.sql.ResultSet executeQuery(String sql)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":executeQuery(" + sql + ")");
      
      ResultSet rs = _stmt.executeQuery(sql);

      return rs;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-executeQuery(" + sql + ") -> " + e);
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int executeUpdate(String sql)
    throws SQLException
  {
    try {
      int count = _stmt.executeUpdate(sql);

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":executeUpdate(" + sql + ") -> " + count);
      
      return count;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-executeUpdate(" + sql + ") -> " + e);
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public boolean execute(String sql)
    throws SQLException
  {
    try {
      boolean hasResult = _stmt.execute(sql);

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":execute(" + sql + ") -> " + hasResult);
      
      return hasResult;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-execute(" + sql + ") -> " + e);
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int[]executeBatch()
    throws SQLException
  {
    try {
      int []result = _stmt.executeBatch();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":executeBatch()");
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-executeBatch(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public java.sql.ResultSet getResultSet()
    throws SQLException
  {
    try {
      ResultSet result = _stmt.getResultSet();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":getResultSet() -> " + (result != null ? result.getClass().getName() : ""));
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-getResultSet(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int getUpdateCount()
    throws SQLException
  {
    try {
      int updateCount = _stmt.getUpdateCount();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":getUpdateCount() -> " + updateCount);
      
      return updateCount;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-getUpdateCount(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public java.sql.Connection getConnection()
    throws SQLException
  {
    int updateCount = _stmt.getUpdateCount();

    if (log.isLoggable(Level.FINE))
      log.fine(getId() + ":getConnection()");
      
    return _conn;
  }

  public int getFetchDirection()
    throws SQLException
  {
    try {
      int result = _stmt.getFetchDirection();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":getFetchDirection() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-getFetchDirection(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int getFetchSize()
    throws SQLException
  {
    try {
      int result = _stmt.getFetchSize();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":getFetchSize() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-getFetchSize(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int getMaxFieldSize()
    throws SQLException
  {
    try {
      int result = _stmt.getMaxFieldSize();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":getMaxFieldSize() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-getMaxFieldSize(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int getMaxRows()
    throws SQLException
  {
    try {
      int result = _stmt.getMaxRows();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":getMaxRows() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-getMaxRows(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }
  
  public void setMaxRows(int max)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setMaxRows(" + max + ")");

      _stmt.setMaxRows(max);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setMaxRows(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public boolean getMoreResults()
    throws SQLException
  {
    try {
      boolean result = _stmt.getMoreResults();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":getMoreResults() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-getMoreResults(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int getQueryTimeout()
    throws SQLException
  {
    try {
      int result = _stmt.getQueryTimeout();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":getQueryTimeout() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-getQueryTimeout(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int getResultSetConcurrency()
    throws SQLException
  {
    try {
      int result = _stmt.getResultSetConcurrency();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":getResultSetConcurrency() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-getResultSetConcurrency(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public int getResultSetType()
    throws SQLException
  {
    try {
      int result = _stmt.getResultSetType();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":getResultSetType() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-getResultSetType(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public SQLWarning getWarnings()
    throws SQLException
  {
    try {
      SQLWarning result = _stmt.getWarnings();

      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":getWarnings() -> " + result);
      
      return result;
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-getWarnings(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setCursorName(String name)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setCursorName(" + name + ")");

      _stmt.setCursorName(name);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setCursorName(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setEscapeProcessing(boolean enable)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setEscapeProcessing(" + enable + ")");

      _stmt.setEscapeProcessing(enable);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setEscapeProcessing(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setFetchDirection(int direction)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setFetchDirection(" + direction + ")");

      _stmt.setFetchDirection(direction);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setFetchDirection(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setFetchSize(int rows)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setFetchSize(" + rows + ")");

      _stmt.setFetchSize(rows);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setFetchSize(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  public void setMaxFieldSize(int max)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setMaxFieldSize(" + max + ")");

      _stmt.setMaxFieldSize(max);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setMaxFieldSize(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }
  
  public void setQueryTimeout(int seconds)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":setQueryTimeout(" + seconds + ")");

      _stmt.setQueryTimeout(seconds);
    } catch (Throwable e) {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":exn-setQueryTimeout(" + e + ")");
      
      throw SQLExceptionWrapper.create(e);
    }
  }

  // jdk 1.4
  public boolean getMoreResults(int count)
    throws SQLException
  {
    return _stmt.getMoreResults(count);
  }
  
  public java.sql.ResultSet getGeneratedKeys()
    throws SQLException
  {
    return _stmt.getGeneratedKeys();
  }
  
  public int executeUpdate(String query, int resultType)
    throws SQLException
  {
    return _stmt.executeUpdate(query, resultType);
  }
  
  public int executeUpdate(String query, int []columns)
    throws SQLException
  {
    return _stmt.executeUpdate(query, columns);
  }
  
  public int executeUpdate(String query, String []columns)
    throws SQLException
  {
    return _stmt.executeUpdate(query, columns);
  }
  
  public boolean execute(String query, int resultType)
    throws SQLException
  {
    return _stmt.execute(query, resultType);
  }
  
  public boolean execute(String query, int []columns)
    throws SQLException
  {
    return _stmt.execute(query, columns);
  }
  
  public boolean execute(String query, String []columns)
    throws SQLException
  {
    return _stmt.execute(query, columns);
  }

  public int getResultSetHoldability()
    throws SQLException
  {
    return _stmt.getResultSetHoldability();
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
