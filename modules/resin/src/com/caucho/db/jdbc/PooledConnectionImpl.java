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

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.StatementEventListener;

/**
 * The JDBC connection implementation.
 */
class PooledConnectionImpl implements PooledConnection {
  private final static L10N L = new L10N(PooledConnectionImpl.class);
  
  private final Database _db;
  
  private ArrayList<ConnectionEventListener> _listeners =
    new ArrayList<ConnectionEventListener>();

  private boolean _isClosed;
  
  PooledConnectionImpl(Database db)
  {
    _db = db;

    if (_db == null)
      throw new NullPointerException();
  }

  /**
   * Returns the database.
   */
  Database getDatabase()
  {
    return _db;
  }  

  /**
   * Returns a new connection implementation.
   */
  public Connection getConnection()
    throws SQLException
  {
    if (_isClosed)
      throw new IllegalStateException(L.l("getting connection after close"));

    // PooledConnectionImpl conn = new PooledConnectionImpl(this);

    return new ConnectionImpl(this);
  }

  /**
   * Adds a new listener.
   */
  public void addConnectionEventListener(ConnectionEventListener listener)
  {
    if (! _listeners.contains(listener))
      _listeners.add(listener);
  }

  /**
   * Removes the new listener.
   */
  public void removeConnectionEventListener(ConnectionEventListener listener)
  {
    _listeners.remove(listener);
  }

  void closeEvent(Connection conn)
  {
    ConnectionEvent event = new ConnectionEvent(this);
    
    for (int i = 0; i < _listeners.size(); i++) {
      ConnectionEventListener listener = _listeners.get(i);

      listener.connectionClosed(event);
    }
  }

  public void close()
    throws SQLException
  {
    _isClosed = true;
  }

    public void addStatementEventListener(StatementEventListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeStatementEventListener(StatementEventListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
