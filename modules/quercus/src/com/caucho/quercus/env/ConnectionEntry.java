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

package com.caucho.quercus.env;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.*;

import javax.sql.DataSource;

public class ConnectionEntry implements EnvCleanup
{
  private static final Logger log
    = Logger.getLogger(ConnectionEntry.class.getName());
  
  private Env _env;
  
  private DataSource _ds;
  private String _user;
  private String _password;
  private Connection _conn;
  private boolean _isReuse;

  public ConnectionEntry(Env env)
  {
    _env = env;
  }

  public void init(DataSource ds, String user, String password)
  {
    _ds = ds;
    _user = user;
    _password = password;
  }

  public void connect(boolean isReuse)
    throws SQLException
  {
    if (_conn != null)
      throw new IllegalStateException();

    _isReuse = isReuse;
      
    if (_user != null)
      _conn = _ds.getConnection(_user, _password);
    else
      _conn = _ds.getConnection();

    _env.addCleanup(this);
  }

  public boolean isReusable()
  {
    return _isReuse && _conn != null;
  }

  public Connection getConnection()
  {
    return _conn;
  }

  public void setCatalog(String catalog)
    throws SQLException
  {
    _isReuse = false;

    _conn.setCatalog(catalog);
  }

  public int hashCode()
  {
    int hash = _ds.hashCode();
      
    if (_user == null)
      return hash;
    else
      return 65521 * hash + _user.hashCode();
  }

  public boolean equals(Object o)
  {
    if (! (o instanceof ConnectionEntry))
      return false;

    ConnectionEntry entry = (ConnectionEntry) o;

    if (_ds != entry._ds)
      return false;
    else if (_user == null)
      return entry._user == null;
    else
      return _user.equals(entry._user);
  }

  /**
   * Notify that the connection should not be reused, e.g. with stateful
   * mysql commands like the temp tables
   */
  public void markForPoolRemoval()
  {
    _isReuse = false;

    if (_conn != null)
      _env.getQuercus().markForPoolRemoval(_conn);
  }

  /**
   * Called from php code to close the connection.  Reusable entries
   * are not actually closed until the cleanup phase.
   */
  public void phpClose()
  {
    try {
      if (! _isReuse) {
        cleanup();
      }
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public void cleanup()
    throws SQLException
  {
    Connection conn = _conn;
    _conn = null;

    if (conn != null)
      conn.close();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[ds=" + _ds + ", user=" + _user + "]";
  }
}
