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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jdbc;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract way of grabbing data from the JDBC connection.
 */
public class ConnectionContext {
  private static final L10N L = new L10N(ConnectionContext.class);
  private static final Logger log = Log.open(ConnectionContext.class);

  private static InitialContext _initialContext;

  private static ThreadLocal<HashMap<String,ConnectionContext>> _localConn
    = new ThreadLocal<HashMap<String,ConnectionContext>>();

  private int _depth;
  private Connection _conn;

  public static void begin(String jndiName)
  {
    HashMap<String,ConnectionContext> map = _localConn.get();

    if (map == null) {
      map = new HashMap<String,ConnectionContext>(8);

      _localConn.set(map);
    }

    ConnectionContext cxt = map.get(jndiName);

    if (cxt == null) {
      cxt = new ConnectionContext();
      
      map.put(jndiName, cxt);
    }

    cxt._depth++;
  }

  public static Connection getConnection(String jndiName)
    throws SQLException
  {
    HashMap<String,ConnectionContext> map = _localConn.get();

    if (map == null)
      throw new IllegalStateException(L.l("'{0}' is not an available connection.",
                                          jndiName));

    ConnectionContext cxt = map.get(jndiName);

    if (cxt == null || cxt._depth == 0)
      throw new IllegalStateException(L.l("'{0}' is not an available connection.",
                                          jndiName));

    if (cxt._conn == null) {
      try {
        DataSource ds = (DataSource) _initialContext.lookup(jndiName);

        cxt._conn = ds.getConnection();
      } catch (NamingException e) {
        throw new IllegalStateException(e);
      }
    }

    return null;
  }

  public static void end(String jdbcName)
  {
    HashMap<String,ConnectionContext> map = _localConn.get();

    if (map == null)
      return;

    ConnectionContext cxt = map.get(jdbcName);

    if (cxt == null)
      return;

    if (--cxt._depth == 0) {
      Connection conn = cxt._conn;

      try {
        cxt._conn = null;
        conn.close();
      } catch (SQLException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  private static Context getInitialContext()
  {
    if (_initialContext == null) {
      try {
        _initialContext = new InitialContext();
      } catch (NamingException e) {
      }
    }

    return _initialContext;
  }
}
