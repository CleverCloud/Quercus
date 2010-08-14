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

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import java.io.PrintWriter;
import java.sql.SQLException;

public final class ConnectionPoolDataSourceWrapper
  implements ConnectionPoolDataSource
{
  private final ProfilerPoint _profilerPoint;
  private final ConnectionPoolDataSource _dataSource;

  public ConnectionPoolDataSourceWrapper(ProfilerPoint profilerPoint,
                                         ConnectionPoolDataSource dataSource)
  {
    _profilerPoint = profilerPoint;
    _dataSource = dataSource;
  }

  public PooledConnection getPooledConnection()
    throws SQLException
  {
    return wrap(_dataSource.getPooledConnection());
  }

  private PooledConnection wrap(PooledConnection pooledConnection)
  {
    return new PooledConnectionWrapper(_profilerPoint, pooledConnection);
  }

  public PooledConnection getPooledConnection(String user, String password)
    throws SQLException
  {
    return wrap(_dataSource.getPooledConnection(user, password));
  }

  public PrintWriter getLogWriter()
    throws SQLException
  {
    return _dataSource.getLogWriter();
  }

  public void setLogWriter(PrintWriter out)
    throws SQLException
  {
    _dataSource.setLogWriter(out);
  }

  public void setLoginTimeout(int seconds)
    throws SQLException
  {
    _dataSource.setLoginTimeout(seconds);
  }

  public int getLoginTimeout()
    throws SQLException
  {
    return _dataSource.getLoginTimeout();
  }

  public String toString()
  {
    return "ConnectionPoolDataSourceWrapper[" + _profilerPoint.getName() + "]";
  }
}
