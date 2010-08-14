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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

public final class StatementWrapper
  implements Statement
{
  private final ProfilerPoint _parentProfilerPoint;
  private final Statement _statement;

  private ProfilerPoint _profilerPoint;

  public StatementWrapper(ProfilerPoint profilerPoint, Statement statement)
  {
    _parentProfilerPoint = profilerPoint;
    _profilerPoint = profilerPoint;
    _statement = statement;
  }

  private void setSql(String sql)
  {
    _profilerPoint = _parentProfilerPoint.addProfilerPoint(sql);
  }

  private ResultSet wrap(ResultSet resultSet)
  {
    return new ResultSetWrapper(_profilerPoint, resultSet);
  }

  public ResultSet executeQuery(String sql)
    throws SQLException
  {
    setSql(sql);

    Profiler profiler = _profilerPoint.start();

    try {
      return wrap(_statement.executeQuery(sql));
    }
    finally {
      profiler.finish();
    }
  }

  public int executeUpdate(String sql)
    throws SQLException
  {
    setSql(sql);

    Profiler profiler = _profilerPoint.start();

    try {
      return _statement.executeUpdate(sql);
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
      _statement.close();
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
      return _statement.getMaxFieldSize();
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
      _statement.setMaxFieldSize(max);
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
      return _statement.getMaxRows();
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
      _statement.setMaxRows(max);
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
      _statement.setEscapeProcessing(enable);
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
      return _statement.getQueryTimeout();
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
      _statement.setQueryTimeout(seconds);
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
      _statement.cancel();
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
      return _statement.getWarnings();
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
      _statement.clearWarnings();
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
      _statement.setCursorName(name);
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
      return _statement.execute(sql);
    }
    finally {
      profiler.finish();
    }
  }

  public ResultSet getResultSet()
    throws SQLException
  {
    return wrap(_statement.getResultSet());
  }

  public int getUpdateCount()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _statement.getUpdateCount();
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
      return _statement.getMoreResults();
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
      _statement.setFetchDirection(direction);
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
      return _statement.getFetchDirection();
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
      _statement.setFetchSize(rows);
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
      return _statement.getFetchSize();
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
      return _statement.getResultSetConcurrency();
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
      return _statement.getResultSetType();
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
      _statement.addBatch(sql);
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
      _statement.clearBatch();
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
      return _statement.executeBatch();
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
      return _statement.getConnection();
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
      return _statement.getMoreResults(current);
    }
    finally {
      profiler.finish();
    }
  }

  public ResultSet getGeneratedKeys()
    throws SQLException
  {
    return wrap(_statement.getGeneratedKeys());
  }

  public int executeUpdate(String sql, int autoGeneratedKeys)
    throws SQLException
  {
    setSql(sql);

    Profiler profiler = _profilerPoint.start();

    try {
      return _statement.executeUpdate(sql, autoGeneratedKeys);
    }
    finally {
      profiler.finish();
    }
  }

  public int executeUpdate(String sql, int[] columnIndexes)
    throws SQLException
  {
    setSql(sql);

    Profiler profiler = _profilerPoint.start();

    try {
      return _statement.executeUpdate(sql, columnIndexes);
    }
    finally {
      profiler.finish();
    }
  }

  public int executeUpdate(String sql, String[] columnNames)
    throws SQLException
  {
    setSql(sql);

    Profiler profiler = _profilerPoint.start();

    try {
      return _statement.executeUpdate(sql, columnNames);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean execute(String sql, int autoGeneratedKeys)
    throws SQLException
  {
    setSql(sql);

    Profiler profiler = _profilerPoint.start();

    try {
      return _statement.execute(sql, autoGeneratedKeys);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean execute(String sql, int[] columnIndexes)
    throws SQLException
  {
    setSql(sql);

    Profiler profiler = _profilerPoint.start();

    try {
      return _statement.execute(sql, columnIndexes);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean execute(String sql, String[] columnNames)
    throws SQLException
  {
    setSql(sql);

    Profiler profiler = _profilerPoint.start();

    try {
      return _statement.execute(sql, columnNames);
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
      return _statement.getResultSetHoldability();
    }
    finally {
      profiler.finish();
    }
  }

  public String toString()
  {
    return "StatementWrapper[" + _profilerPoint.getName() + "]";
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
