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

import com.caucho.util.L10N;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

public final class DataSourceWrapper
  implements DataSource
{
  private static final L10N L = new L10N(DataSourceWrapper.class);

  private final DataSource _dataSource;
  private final ProfilerPoint _profilerPoint;

  public DataSourceWrapper(ProfilerPoint profilerPoint, DataSource dataSource)
  {
    _profilerPoint = profilerPoint;
    _dataSource = dataSource;
  }

  private Connection wrap(Connection connection)
  {
    return new ConnectionWrapper(_profilerPoint, connection);
  }

  public Connection getConnection()
    throws SQLException
  {
    return wrap(_dataSource.getConnection());
  }

  public Connection getConnection(String username, String password)
    throws SQLException
  {
    return wrap(_dataSource.getConnection(username, password));
  }

  public PrintWriter getLogWriter()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _dataSource.getLogWriter();
    }
    finally {
      profiler.finish();
    }
  }

  public void setLogWriter(PrintWriter out)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _dataSource.setLogWriter(out);
    }
    finally {
      profiler.finish();
    }
  }

  public void setLoginTimeout(int seconds)
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _dataSource.setLoginTimeout(seconds);
    }
    finally {
      profiler.finish();
    }
  }

  public int getLoginTimeout()
    throws SQLException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _dataSource.getLoginTimeout();
    }
    finally {
      profiler.finish();
    }
  }

  public String toString()
  {
    return "DataSourceWrapper[" + _profilerPoint.getName() + "]";
  }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
