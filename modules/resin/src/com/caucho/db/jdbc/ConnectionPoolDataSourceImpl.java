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

import com.caucho.db.Database;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Driver for the internal database.
 */
public class ConnectionPoolDataSourceImpl implements ConnectionPoolDataSource {
  private static final Logger log
    = Logger.getLogger(ConnectionPoolDataSourceImpl.class.getName());
  private static final L10N L = new L10N(ConnectionPoolDataSourceImpl.class);

  private Database _database;

  private boolean _createDatabase;
  private boolean _isInit;

  /**
   * Creates a new data source
   */
  public ConnectionPoolDataSourceImpl()
  {
    _database = new Database();
  }

  /**
   * Sets the path to the database.
   */
  public void setPath(Path path)
  {
    _database.setPath(path);
  }

  /**
   * Sets the url to the database.
   */
  public void setURL(String url)
  {
    if (url.startsWith("jdbc:"))
      url = url.substring(5);
    
    if (url.startsWith("resin:"))
      url = url.substring(6);

    _database.setPath(Vfs.lookup(url));
  }

  public String getURL()
  {
    return "jdbc:resin:" + _database.getPath().getFullPath();
  }

  /**
   * If true, creates the database on init.
   */
  public void setCreateDatabase(boolean create)
  {
    _createDatabase = create;
  }

  /**
   * If true, removes bad tables on init.
   */
  public void setRemoveOnError(boolean remove)
  {
    _database.setRemoveOnError(remove);
  }

  /**
   * Initialize the data source.
   */
  public void init()
    throws SQLException
  {
    synchronized (this) {
      if (_isInit)
        return;

      try {
        _database.init();
      } finally {
        _isInit = true;
      }
    }
  }

  public int getLoginTimeout()
  {
    return 0;
  }

  public void setLoginTimeout(int foo)
  {
  }

  public PrintWriter getLogWriter()
  {
    return null;
  }

  public void setLogWriter(PrintWriter log)
  {
  }
  
  /**
   * Driver interface to create a new connection.
   */
  public PooledConnection getPooledConnection(String user, String password)
    throws SQLException
  {
    return getPooledConnection();
  }
  
  /**
   * Driver interface to create a new connection.
   */
  public PooledConnection getPooledConnection()
    throws SQLException
  {
    init();
      
    return new PooledConnectionImpl(_database);
  }

  public String toString()
  {
    return "ConnectionPoolDataSourceImpl[" + _database.getPath() + "]";
  }

  protected void finalize()
    throws Throwable
  {
    super.finalize();

    _database.close();
  }
}
