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
 * @author Rodrigo Westrupp
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.ConnectionEntry;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnicodeValueImpl;
import com.caucho.util.L10N;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * oracle connection class (oracle has NO object oriented API)
 */
public class Oracle extends JdbcConnectionResource {
  private static final Logger log = Logger.getLogger(Oracle.class.getName());
  private static final L10N L = new L10N(Oracle.class);

  public Oracle(Env env,
                @Optional("localhost") String host,
                @Optional String user,
                @Optional String password,
                @Optional String db,
                @Optional("1521") int port,
                @Optional String driver,
                @Optional String url)
  {
    super(env);

    connectInternal(env, host, user, password, db, port, "", 0,
                    driver, url, false);
  }

  /**
   * Connects to the underlying database.
   */
  @Override
    protected ConnectionEntry connectImpl(Env env,
                                          String host,
                                          String userName,
                                          String password,
                                          String dbname,
                                          int port,
                                          String socket,
                                          int flags,
                                          String driver,
                                          String url,
                                          boolean isNewLink)
  {
    if (isConnected()) {
      env.warning(L.l("Connection is already opened to '{0}'", this));
      return null;
    }

    try {

      if (host == null || host.equals("")) {
        host = "localhost";
      }

      if (driver == null || driver.equals("")) {
        driver = "oracle.jdbc.OracleDriver";
      }

      if (url == null || url.equals("")) {
        if (dbname.indexOf("//") == 0) {
          // db is the url itself: "//db_host[:port]/database_name"
          url = "jdbc:oracle:thin:@" + dbname.substring(2);
          url = url.replace('/', ':');
        } else {
          url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + dbname;
        }
      }

      ConnectionEntry jConn;
      
      jConn = env.getConnection(driver, url, userName, password, ! isNewLink);
      
      return jConn;

    } catch (SQLException e) {
      env.warning(
          "A link to the server could not be established. " + e.toString());
      env.setSpecialValue(
          "oracle.connectErrno", LongValue.create(e.getErrorCode()));
      env.setSpecialValue(
          "oracle.connectError", env.createString(e.getMessage()));

      log.log(Level.FINE, e.toString(), e);

      return null;
    } catch (Exception e) {
      env.warning(
          "A link to the server could not be established. " + e.toString());
      env.setSpecialValue(
          "oracle.connectError", env.createString(e.getMessage()));

      log.log(Level.FINE, e.toString(), e);
      return null;
    }
  }

  /**
   * returns a prepared statement
   */
  public OracleStatement prepare(Env env, StringValue query)
  {
    OracleStatement stmt = new OracleStatement((Oracle) validateConnection());

    stmt.prepare(env, query);

    return stmt;
  }

  /**
   * Creates a database-specific result.
   */
  protected JdbcResultResource createResult(Env env,
                                            Statement stmt,
                                            ResultSet rs)
  {
    return new OracleResult(env, stmt, rs, this);
  }


  public String toString()
  {
    if (isConnected())
      return "Oracle[" + getHost() + "]";
    else
      return "Oracle[]";
  }
}
