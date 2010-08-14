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

import com.caucho.cloud.network.ClusterServer;
import com.caucho.server.cluster.*;
import com.caucho.server.resin.Resin;
import com.caucho.transaction.IdlePoolSet;
import com.caucho.util.L10N;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ValidatingManagedConnectionFactory;
import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The managed factory implementation.
 */
public class ManagedFactoryImpl
  implements ManagedConnectionFactory, ValidatingManagedConnectionFactory
{
  protected static final Logger log
    = Logger.getLogger(ManagedFactoryImpl.class.getName());
  private static final L10N L = new L10N(ManagedFactoryImpl.class);

  private DBPoolImpl _dbPool;
  private DriverConfig []_drivers;
  private DriverConfig []_backupDrivers;

  private long _roundRobin;

  ManagedFactoryImpl(DBPoolImpl dbPool,
                     DriverConfig []drivers,
                     DriverConfig []backupDrivers)
  {
    _dbPool = dbPool;
    _drivers = drivers;
    _backupDrivers = backupDrivers;

    Server server = Server.getCurrent();
    if (server != null) {
      ClusterServer selfServer = server.getSelfServer();

      if (selfServer != null && _drivers.length > 1)
        _roundRobin = selfServer.getIndex() % _drivers.length;
    }
  }

  /**
   * Returns the DB pool.
   */
  public DBPoolImpl getDBPool()
  {
    return _dbPool;
  }

  /**
   * Returns the primary URL
   */
  public String getURL()
  {
    if (_drivers.length > 0)
      return _drivers[0].getURL();
    else if (_backupDrivers.length > 0)
      return _backupDrivers[0].getURL();
    else
      return null;
  }

  /**
   * Returns the connection config.
   */
  ConnectionConfig getConnectionConfig()
  {
    return _dbPool.getConnectionConfig();
  }

  /**
   * Creates the data source the user sees.
   */
  public Object createConnectionFactory(ConnectionManager connManager)
    throws ResourceException
  {
    return new DataSourceImpl(this, connManager);
  }

  /**
   * Creates the data source the user sees.  Not needed in this case,
   * since ManagedFactoryImpl is only allowed in Resin.
   */
  public Object createConnectionFactory()
    throws ResourceException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates the underlying managed connection.
   */
  public ManagedConnection
    createManagedConnection(Subject subject,
                            ConnectionRequestInfo requestInfo)
    throws ResourceException
  {
    Credential credential = (Credential) requestInfo;

    SQLException exn = null;

    for (int i = 0; i < _drivers.length; i++) {
      int index = (int) (_roundRobin++ % _drivers.length);

      DriverConfig driver = _drivers[index];

      try {
        return new ManagedConnectionImpl(this,
                                         driver,
                                         _dbPool.getConnectionConfig(),
                                         credential);
      } catch (SQLException e) {
        exn = e;
      }
    }

    for (int i = 0; i < _backupDrivers.length; i++) {
      int index = (int) (_roundRobin++ % _backupDrivers.length);

      DriverConfig driver = _backupDrivers[index];

      try {
        return new ManagedConnectionImpl(this,
                                         driver,
                                         _dbPool.getConnectionConfig(),
                                         credential);
      } catch (SQLException e) {
        exn = e;
      }
    }

    if (exn != null)
      throw new ResourceException(exn);
    else
      throw new ResourceException(L.l("Can't create a database connection because no drivers have been defined."));
  }

  /**
   * Creates the underlying managed connection.
   */
  public ManagedConnection
    matchManagedConnections(Set connSet,
                            Subject subject,
                            ConnectionRequestInfo requestInfo)
    throws ResourceException
  {
    if (connSet instanceof IdlePoolSet) {
      IdlePoolSet idle = (IdlePoolSet) connSet;

      ManagedConnectionImpl mConn = (ManagedConnectionImpl) idle.first();

      if (mConn == null)
        return null;
      
      Credential mCredentials = mConn.getCredentials();

      if (requestInfo == mCredentials
          || requestInfo != null && requestInfo.equals(mCredentials)) {
        return mConn;
      }
    }
    
    Iterator iter = connSet.iterator();

    while (iter.hasNext()) {
      ManagedConnectionImpl mConn = (ManagedConnectionImpl) iter.next();
      Credential mCredentials = mConn.getCredentials();

      if (requestInfo == mCredentials
          || requestInfo != null && requestInfo.equals(mCredentials)) {
        return mConn;
      }
    }

    return null;
  }

  /**
   * Returns any invalid connections.
   */
  public Set getInvalidConnections(Set connSet)
    throws ResourceException
  {
    Iterator iter = connSet.iterator();
    HashSet invalidSet = null;

    while (iter.hasNext()) {
      ManagedConnectionImpl mConn = (ManagedConnectionImpl) iter.next();

      if (! mConn.isValid()) {
        if (invalidSet == null)
          invalidSet = new HashSet();

        invalidSet.add(mConn);
      }
    }

    return invalidSet;
  }

  public void setLogWriter(PrintWriter out)
    throws ResourceException
  {
  }

  public PrintWriter getLogWriter()
    throws ResourceException
  {
    return null;
  }

  public ResourceAdapter getResourceAdapter()
  {
    return null;
  }

  public void setResourceAdapter(ResourceAdapter adapter)
  {
  }
}

