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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.Date;
import java.util.ArrayList;

import javax.management.*;

import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.DatabaseMXBean;
import com.caucho.management.server.JdbcDriverMXBean;
import com.caucho.transaction.ConnectionPool;

public class DatabaseAdmin extends AbstractManagedObject
  implements DatabaseMXBean
{
  private final DBPool _dbPool;
  private final ConnectionPool _jcaPool;

  public DatabaseAdmin(DBPool dbPool, ConnectionPool jcaPool)
  {
    _dbPool = dbPool;
    _jcaPool = jcaPool;
  }

  public String getUrl()
  {
    return _dbPool.getURL();
  }

  /**
   * Returns true if spy is enabled
   */
  public boolean isSpy()
  {
    return _dbPool.isSpy();
  }

  /**
   * Returns the pool's jdbc drivers
   */
  public JdbcDriverMXBean []getDrivers()
  {
    return _dbPool.getDriverAdmin();
  }
  
  @Override
  public String getName()
  {
    return _dbPool.getName();
  }

  //
  // ConnectionPoolMXBean
  //

  /**
   * Returns the maximum number of connections.
   */
  public int getMaxConnections()
  {
    return _jcaPool.getMaxConnections();
  }
  
  /**
   * Returns the number of overflow connections.
   */
  public int getMaxOverflowConnections()
  {
    return _jcaPool.getMaxOverflowConnections();
  }
  
  /**
   * Returns the max number of connections trying to connect.
   */
  public int getMaxCreateConnections()
  {
    return _jcaPool.getMaxCreateConnections();
  }
  
  /**
   * Returns the pool idle time in milliseconds.
   */
  public long getMaxIdleTime()
  {
    return _jcaPool.getMaxIdleTime();
  }
  
  /**
   * Returns the maximum number of idle connections
   */
  public int getMaxIdleCount()
  {
    return _jcaPool.getMaxIdleCount();
  }
  
  /**
   * Returns the pool active time in milliseconds.
   */
  public long getMaxActiveTime()
  {
    return _jcaPool.getMaxActiveTime();
  }
  
  /**
   * Returns the pool time in milliseconds.
   */
  public long getMaxPoolTime()
  {
    return _jcaPool.getMaxPoolTime();
  }
  
  /**
   * How long to wait for connections when timed out.
   */
  public long getConnectionWaitTime()
  {
    return _jcaPool.getConnectionWaitTime();
  }
  
  /**
   * Returns true for the JCA shared attribute.
   */
  public boolean isShareable()
  {
    return _jcaPool.isShareable();
  }
  
  /**
   * Returns true if the local-transaction-optimization is allowed
   */
  public boolean isLocalTransactionOptimization()
  {
    return _jcaPool.isLocalTransactionOptimization();
  }
  
  //
  // Statistics
  //
  
  /**
   * Returns the total number of connections.
   */
  public int getConnectionCount()
  {
    return _jcaPool.getConnectionCount();
  }

  /**
   * Returns the number of active connections.
   */
  public int getConnectionActiveCount()
  {
    return _jcaPool.getConnectionActiveCount();
  }

  /**
   * Returns the number of idle connections.
   */
  public int getConnectionIdleCount()
  {
    return _jcaPool.getConnectionIdleCount();
  }

  /**
   * Returns the total number of connections.
   */
  public long getConnectionCountTotal()
  {
    return _jcaPool.getConnectionCountTotal();
  }

  /**
   * Returns the total number of created connections.
   */
  public long getConnectionCreateCountTotal()
  {
    return _jcaPool.getConnectionCreateCountTotal();
  }

  /**
   * Returns the total number of failed connections.
   */
  public long getConnectionFailCountTotal()
  {
    return _jcaPool.getConnectionFailCountTotal();
  }

  /**
   * Returns the last failed connection time.
   */
  public Date getLastFailTime()
  {
    return _jcaPool.getLastFailTime();
  }

  //
  // Operations
  //

  /**
   * Clears all idle connections in the pool.
   */
  public void clear()
  {
    _jcaPool.clear();
  }

  void register()
  {
    registerSelf();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getObjectName() + "]";
  }
}
