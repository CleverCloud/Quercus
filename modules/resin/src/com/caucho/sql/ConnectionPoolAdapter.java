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

import com.caucho.util.L10N;

import java.io.PrintWriter;
import java.sql.*;
import java.util.Map;
import javax.sql.*;

/**
 * Adapter for DataSource used as a Driver.
 */
public class ConnectionPoolAdapter implements ConnectionPoolDataSource {
  private static final L10N L = new L10N(ConnectionPoolAdapter.class);
  
  private DataSource _dataSource;

  /**
   * Creates a new SpyDataSource
   */
  public ConnectionPoolAdapter(DataSource dataSource)
  {
    _dataSource = dataSource;
  }

  public int getLoginTimeout()
    throws SQLException
  {
    return _dataSource.getLoginTimeout();
  }

  public void setLoginTimeout(int loginTimeout)
    throws SQLException
  {
    _dataSource.setLoginTimeout(loginTimeout);
  }

  public PrintWriter getLogWriter()
    throws SQLException
  {
    return _dataSource.getLogWriter();
  }

  public void setLogWriter(PrintWriter log)
    throws SQLException
  {
    _dataSource.setLogWriter(log);
  }

  public PooledConnection getPooledConnection()
    throws SQLException
  {
    return new PooledConnectionAdapter(_dataSource.getConnection());
  }

  public PooledConnection getPooledConnection(String user, String password)
    throws SQLException
  {
    Connection conn = _dataSource.getConnection(user, password);
    
    return new PooledConnectionAdapter(conn);
  }

  static class PooledConnectionAdapter implements PooledConnection {
    private Connection _conn;

    PooledConnectionAdapter(Connection conn)
    {
      _conn = conn;
    }

    public Connection getConnection()
      throws SQLException
    {
      if (_conn != null)
        return _conn;
      else
        throw new SQLException(L.l("connection is not available because it has been closed."));
    }

    public void addConnectionEventListener(ConnectionEventListener listener)
    {
    }

    public void removeConnectionEventListener(ConnectionEventListener listener)
    {
    }

    public void addStatementEventListener(StatementEventListener listener)
    {
    }

    public void removeStatementEventListener(StatementEventListener listener)
    {
    }

    public void close()
      throws SQLException
    {
      Connection conn = _conn;
      _conn = null;

      if (conn != null)
        conn.close();
    }
  }
}
