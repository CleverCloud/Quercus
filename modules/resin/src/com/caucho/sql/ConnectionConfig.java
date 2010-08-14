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

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.sql.Connection;

/**
 * Configures the connection
 */
public class ConnectionConfig {
  private static final L10N L = new L10N(ConnectionConfig.class);

  private int _isolation = -1;
  private boolean _readOnly;
  private String _catalog;

  /**
   * Sets the isolation of the connection.
   */
  public void setTransactionIsolation(String name)
    throws ConfigException
  {
    if ("none".equals(name))
      _isolation = Connection.TRANSACTION_NONE;
    else if ("read-committed".equals(name))
      _isolation = Connection.TRANSACTION_READ_COMMITTED;
    else if ("read-uncommitted".equals(name))
      _isolation = Connection.TRANSACTION_READ_UNCOMMITTED;
    else if ("repeatable-read".equals(name))
      _isolation = Connection.TRANSACTION_REPEATABLE_READ;
    else if ("serializable".equals(name))
      _isolation = Connection.TRANSACTION_SERIALIZABLE;
    else
      throw new ConfigException(L.l("'{0}' is an unknown transaction isolation.",
                                    name));
  }

  /**
   * Returns the isolation of the connection, with -1 for the default.
   */
  public int getTransactionIsolation()
  {
    return _isolation;
  }

  /**
   * Sets the read-only status.
   */
  public void setReadOnly(boolean isReadOnly)
  {
    _readOnly = isReadOnly;
  }

  /**
   * Gets the read-only status.
   */
  public boolean isReadOnly()
  {
    return _readOnly;
  }

  /**
   * Sets the catalog
   */
  public void setCatalog(String catalog)
  {
    if (! "".equals(catalog))
      _catalog = catalog;
    else
      _catalog = null;
  }

  /**
   * Gets the catalog.
   */
  public String getCatalog()
  {
    return _catalog;
  }
}
