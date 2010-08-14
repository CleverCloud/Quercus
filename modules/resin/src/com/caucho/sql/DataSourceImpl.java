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

package com.caucho.sql;

import com.caucho.util.L10N;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * The wrapped data source.
 */
public class DataSourceImpl implements DataSource {
  protected static final Logger log
    = Logger.getLogger(DataSourceImpl.class.getName());
  private static final L10N L = new L10N(DataSourceImpl.class);

  private final ManagedFactoryImpl _managedFactory;
  private final ConnectionManager _connManager;

  DataSourceImpl(ManagedFactoryImpl factory, ConnectionManager cm)
  {
    _managedFactory = factory;
    _connManager = cm;
  }

  /**
   * Returns the primary URL for the connection
   */
  public String getURL()
  {
    return _managedFactory.getURL();
  }

  /**
   * Returns a connection.
   */
  public Connection getConnection()
    throws SQLException
  {
    try {
      return (Connection) _connManager.allocateConnection(_managedFactory,
                                                          null);
    } catch (ResourceException e) {
      Throwable cause;

      for (cause = e; cause != null; cause = cause.getCause()) {
        if (cause instanceof SQLException)
          throw (SQLException) cause;
      }

      throw new SQLExceptionWrapper(e);
    }
  }

  /**
   * Returns a connection.
   */
  public Connection getConnection(String username, String password)
    throws SQLException
  {
    try {
      Credential credential = null;

      if (username != null || password != null)
        credential = new Credential(username, password);

      return (Connection)  _connManager.allocateConnection(_managedFactory,
                                                           credential);
    } catch (ResourceException e) {
      Throwable cause;

      for (cause = e; cause != null; cause = cause.getCause()) {
        if (cause instanceof SQLException)
          throw (SQLException) cause;
      }

      throw new SQLExceptionWrapper(e);
    }
  }

  /**
   * Returns the login timeout.
   */
  public int getLoginTimeout()
  {
    return 0;
  }

  /**
   * Returns the login timeout.
   */
  public void setLoginTimeout(int seconds)
  {
  }

  /**
   * Returns the log writer.
   */
  public PrintWriter getLogWriter()
  {
    return null;
  }

  /**
   * Sets the log writer.
   */
  public void setLogWriter(PrintWriter out)
  {
  }

  /**
   * Returns true if the impl has closed.
   */
  boolean isClosed()
  {
    return false;
  }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

