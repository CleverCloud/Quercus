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
import com.caucho.quercus.annotation.ResourceType;
import com.caucho.quercus.env.ConnectionEntry;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * postgres connection class (postgres has NO object oriented API)
 */
@ResourceType("pgsql link")
public class Postgres extends JdbcConnectionResource {
  private static final Logger log = Logger.getLogger(Postgres.class.getName());
  private static final L10N L = new L10N(Postgres.class);

  PostgresResult _asyncResult;
  PostgresStatement _asyncStmt;

  // named prepared statements for postgres
  private HashMap<String,PostgresStatement> _stmtTable
    = new HashMap<String,PostgresStatement>();

  // Postgres specific server error message
  // org.postgresql.util.ServerErrorMessage
  Object _serverErrorMessage;

  public Postgres(Env env,
                  @Optional("localhost") String host,
                  @Optional String user,
                  @Optional String password,
                  @Optional String db,
                  @Optional("5432") int port,
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
        driver = "org.postgresql.Driver";
      }

      if (url == null || url.equals("")) {
        url = "jdbc:postgresql://" + host + ":" + port + "/" + dbname;
      }

      ConnectionEntry jConn;
      
      jConn = env.getConnection(driver, url, userName, password, ! isNewLink);

      return jConn;
    } catch (SQLException e) {
      env.warning(
        "A link to the server could not be established. " + e.toString());
      env.setSpecialValue(
        "postgres.connectErrno", LongValue.create(e.getErrorCode()));
      env.setSpecialValue(
        "postgres.connectError", env.createString(e.getMessage()));

      log.log(Level.FINE, e.toString(), e);

      return null;
    } catch (Exception e) {
      env.warning(
        "A link to the server could not be established. " + e.toString());
      env.setSpecialValue(
        "postgres.connectError", env.createString(e.getMessage()));

      log.log(Level.FINE, e.toString(), e);
      return null;
    }
  }

  /**
   * returns a prepared statement
   */
  public PostgresStatement prepare(Env env, StringValue query)
  {
    PostgresStatement stmt = new PostgresStatement(
      (Postgres)validateConnection());

    stmt.prepare(env, query);

    return stmt;
  }

  /**
   * Executes a query.
   *
   * @param sql the escaped query string
   * (can contain escape sequences like `\n' and `\Z')
   *
   * @return a {@link JdbcResultResource}, or null for failure
   */
  public PostgresResult query(Env env, String sql)
  {
    SqlParseToken tok = parseSqlToken(sql, null);

    if (tok != null
        && tok.matchesFirstChar('S', 's') 
        && tok.matchesToken("SET")) {
      // Check for "SET CLIENT_ENCODING TO ..."

      tok = parseSqlToken(sql, tok);

      if (tok != null && tok.matchesToken("CLIENT_ENCODING")) {
        tok = parseSqlToken(sql, tok);

        if (tok != null && tok.matchesToken("TO")) {
          // Ignore any attempt to change the CLIENT_ENCODING since
          // the JDBC driver for Postgres only supports UNICODE.
          // Execute no-op SQL statement since we need to return
          // a valid SQL result to the caller.

          sql = "SET CLIENT_ENCODING TO 'UNICODE'";
        }
      }
    }

    Object result = realQuery(env, sql).toJavaObject();
    
    if (! (result instanceof PostgresResult))
      return null;
    
    return (PostgresResult) result;
  }

  /**
   * Creates a database-specific result.
   */
  protected JdbcResultResource createResult(Env env,
                                            Statement stmt,
                                            ResultSet rs)
  {
    return new PostgresResult(env, stmt, rs, this);
  }

  public void setAsynchronousResult(PostgresResult asyncResult)
  {
    _asyncResult = asyncResult;
  }

  public PostgresResult getAsynchronousResult()
  {
    return _asyncResult;
  }

  public PostgresStatement getAsynchronousStatement()
  {
    return _asyncStmt;
  }

  public void setAsynchronousStatement(PostgresStatement asyncStmt)
  {
    _asyncStmt = asyncStmt;
  }

  public void putStatement(String name,
                           PostgresStatement stmt)
  {
    _stmtTable.put(name, stmt);
  }

  public PostgresStatement getStatement(String name)
  {
    return _stmtTable.get(name);
  }

  public PostgresStatement removeStatement(String name)
  {
    return _stmtTable.remove(name);
  }

  /**
   * This function is overriden in Postgres to keep
   * result set references for php/430a (see also php/1f33)
   */
  protected void keepResourceValues(Statement stmt)
  {
    setResultResource(createResult(getEnv(), stmt, null));
  }

  /**
   * This function is overriden in Postgres to keep
   * statement references for php/430a
   */
  protected boolean keepStatementOpen()
  {
    return true;
  }

  static public StringValue pgRealEscapeString(StringValue str)
  {
    StringValue buf = str.createStringBuilder(str.length());

    final int strLength = str.length();

    for (int i = 0; i < strLength; i++) {
      char c = str.charAt(i);

      switch (c) {
        case '\u0000':
          buf.append('\\');
          buf.append('\u0000');
          break;
        case '\n':
          buf.append('\\');
          buf.append('n');
          break;
        case '\r':
          buf.append('\\');
          buf.append('r');
          break;
        case '\\':
          buf.append('\\');
          buf.append('\\');
          break;
        case '\'':
          buf.append('\'');
          buf.append('\'');
          break;
        case '"':
          // pg_escape_string does nothing about it.
          // buf.append('\\');
          buf.append('\"');
          break;
        case '\032':
          buf.append('\\');
          buf.append('Z');
          break;
        default:
          buf.append(c);
          break;
      }
    }

    return buf;
  }

  /**
   * Escape the given string for SQL statements.
   *
   * @param str a string
   * @return the string escaped for SQL statements
   */
  protected StringValue realEscapeString(StringValue str)
  {
    return pgRealEscapeString(str);
  }

  /**
   * This function is overriden in Postgres to clear
   * any postgres specific server error message
   */
  protected void clearErrors()
  {
    super.clearErrors();
    _serverErrorMessage = null;
  }

  /**
   * This function is overriden in Postgres to save
   * the postgres specific server error message
   */
  protected void saveErrors(SQLException e)
  {
    try {
      super.saveErrors(e);

      // Get the postgres specific server error message
      Class cl = Class.forName("org.postgresql.util.PSQLException");
      Method method = cl.getDeclaredMethod("getServerErrorMessage", null);
      _serverErrorMessage = method.invoke(e, new Object[] {});
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }
  }

  /**
   * Return the postgres server specific error message
   */
  protected Object getServerErrorMessage()
  {
    return _serverErrorMessage;
  }

  public String toString()
  {
    if (isConnected())
      return "Postgres[" + getHost() + "]";
    else
      return "Postgres[]";
  }

  /**
   * Return the "client_encoding" property. This is the
   * encoding the JDBC driver uses to read character
   * data from the server. The JDBC driver used to let
   * the user change the encoding, but it now fails on
   * any attempt to set the encoding to anything other
   * than UNICODE.
   */

  public String getClientEncoding()
  {
    return "UNICODE";
  }

  /**
   * Set the "client_encoding" property. This is
   * a no-op for the JDBC driver because it only
   * supports UNICODE as the client encoding.
   * Return true to indicate success in all cases.
   */

  public boolean setClientEncoding(String encoding)
  {
    return true;
  }

}
