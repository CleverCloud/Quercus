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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.jdbc;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

/**
 * Amber wrapper of a statement.
 */
public class AmberStatementImpl implements java.sql.Statement {
  // The owning connection
  protected AmberConnectionImpl _conn;
  
  // The underlying statement
  protected Statement _stmt;

  AmberStatementImpl(AmberConnectionImpl conn, Statement stmt)
  {
    _conn = conn;
    _stmt = stmt;
  }

  /**
   * Returns the underlying statement.
   */
  public Statement getStatement()
  {
    return _stmt;
  }

  public void addBatch(String sql)
    throws SQLException
  {
    _stmt.addBatch(sql);
  }

  public void cancel()
    throws SQLException
  {
    _stmt.cancel();
  }
  
  public void clearBatch()
    throws SQLException
  {
    _stmt.clearBatch();
  }

  public void clearWarnings()
    throws SQLException
  {
    _stmt.clearWarnings();
  }

  public void close()
    throws SQLException
  {
    _stmt.close();
  }

  public java.sql.ResultSet executeQuery(String sql)
    throws SQLException
  {
    return _stmt.executeQuery(sql);
  }

  public int executeUpdate(String sql)
    throws SQLException
  {
    return _stmt.executeUpdate(sql);
  }

  public boolean execute(String sql)
    throws SQLException
  {
    return _stmt.execute(sql);
  }

  public int[]executeBatch()
    throws SQLException
  {
    return _stmt.executeBatch();
  }

  public java.sql.ResultSet getResultSet()
    throws SQLException
  {
    return _stmt.getResultSet();
  }

  public int getUpdateCount()
    throws SQLException
  {
    return _stmt.getUpdateCount();
  }

  public java.sql.Connection getConnection()
    throws SQLException
  {
    return _conn;
  }

  public int getFetchDirection()
    throws SQLException
  {
    return _stmt.getFetchDirection();
  }

  public int getFetchSize()
    throws SQLException
  {
    return _stmt.getFetchSize();
  }

  public int getMaxFieldSize()
    throws SQLException
  {
    return _stmt.getMaxFieldSize();
  }

  public int getMaxRows()
    throws SQLException
  {
    return _stmt.getMaxRows();
  }
  
  public void setMaxRows(int max)
    throws SQLException
  {
    _stmt.setMaxRows(max);
  }

  public boolean getMoreResults()
    throws SQLException
  {
    return _stmt.getMoreResults();
  }

  public int getQueryTimeout()
    throws SQLException
  {
    return _stmt.getQueryTimeout();
  }

  public int getResultSetConcurrency()
    throws SQLException
  {
    return _stmt.getResultSetConcurrency();
  }

  public int getResultSetType()
    throws SQLException
  {
    return _stmt.getResultSetType();
  }

  public SQLWarning getWarnings()
    throws SQLException
  {
    return _stmt.getWarnings();
  }

  public void setCursorName(String name)
    throws SQLException
  {
    _stmt.setCursorName(name);
  }

  public void setEscapeProcessing(boolean enable)
    throws SQLException
  {
    _stmt.setEscapeProcessing(enable);
  }

  public void setFetchDirection(int direction)
    throws SQLException
  {
    _stmt.setFetchDirection(direction);
  }

  public void setFetchSize(int rows)
    throws SQLException
  {
    _stmt.setFetchSize(rows);
  }

  public void setMaxFieldSize(int max)
    throws SQLException
  {
    _stmt.setMaxFieldSize(max);
  }
  
  public void setQueryTimeout(int seconds)
    throws SQLException
  {
    _stmt.setQueryTimeout(seconds);
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
