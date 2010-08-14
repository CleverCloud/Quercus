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

package com.caucho.quercus.lib.db;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.EnvCleanup;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PDO object oriented API facade.
 */
public class PDO implements EnvCleanup {
  private static final Logger log = Logger.getLogger(PDO.class.getName());
  private static final L10N L = new L10N(PDO.class);

  public static final int ATTR_AUTOCOMMIT = 0;
  public static final int ATTR_PREFETCH = 1;
  public static final int ATTR_TIMEOUT = 2;
  public static final int ATTR_ERRMODE = 3;
  public static final int ATTR_SERVER_VERSION = 4;
  public static final int ATTR_CLIENT_VERSION = 5;
  public static final int ATTR_SERVER_INFO = 6;
  public static final int ATTR_CONNECTION_STATUS = 7;
  public static final int ATTR_CASE = 8;
  public static final int ATTR_CURSOR_NAME = 9;
  public static final int ATTR_CURSOR = 10;
  public static final int ATTR_ORACLE_NULLS = 11;
  public static final int ATTR_PERSISTENT = 12;
  public static final int ATTR_STATEMENT_CLASS = 13;
  public static final int ATTR_FETCH_TABLE_NAMES = 14;
  public static final int ATTR_FETCH_CATALOG_NAMES = 15;
  public static final int ATTR_DRIVER_NAME = 16;
  public static final int ATTR_STRINGIFY_FETCHES = 17;
  public static final int ATTR_MAX_COLUMN_LEN = 18;

  public static final int CASE_NATURAL = 0;
  public static final int CASE_UPPER = 1;
  public static final int CASE_LOWER = 2;

  public static final int CURSOR_FWDONLY = 0;
  public static final int CURSOR_SCROLL = 1;

  public static final String ERR_NONE = "00000";

  public static final int ERRMODE_SILENT = 0;
  public static final int ERRMODE_WARNING = 1;
  public static final int ERRMODE_EXCEPTION = 2;

  public static final int FETCH_LAZY = 1;
  public static final int FETCH_ASSOC = 2;
  public static final int FETCH_NUM = 3;
  public static final int FETCH_BOTH = 4;
  public static final int FETCH_OBJ = 5;
  public static final int FETCH_BOUND = 6;
  public static final int FETCH_COLUMN = 7;
  public static final int FETCH_CLASS = 8;
  public static final int FETCH_INTO = 9;
  public static final int FETCH_FUNC = 10;
  public static final int FETCH_NAMED = 11;

  public static final int FETCH_GROUP = 0x00010000;
  public static final int FETCH_UNIQUE = 0x00030000;
  public static final int FETCH_CLASSTYPE = 0x00040000;
  public static final int FETCH_SERIALIZE = 0x00080000;

  public static final int FETCH_ORI_NEXT = 0;
  public static final int FETCH_ORI_PRIOR = 1;
  public static final int FETCH_ORI_FIRST = 2;
  public static final int FETCH_ORI_LAST = 3;
  public static final int FETCH_ORI_ABS = 4;
  public static final int FETCH_ORI_REL = 5;

  public static final int NULL_NATURAL = 0;
  public static final int NULL_EMPTY_STRING = 1;
  public static final int NULL_TO_STRING = 2;

  public static final int PARAM_NULL = 0;
  public static final int PARAM_INT = 1;
  public static final int PARAM_STR = 2;
  public static final int PARAM_LOB = 3;
  public static final int PARAM_STMT = 4;
  public static final int PARAM_BOOL = 5;

  public static final int PARAM_INPUT_OUTPUT = 0x80000000;

  private final Env _env;
  private final String _dsn;
  private String _user;
  private String _password;

  private final PDOError _error;

  private Connection _conn;

  private Statement _lastStatement;
  private PDOStatement _lastPDOStatement;
  private String _lastInsertId;

  private boolean _inTransaction;
  
  private static String ENCODING = "ISO8859_1";

  public PDO(Env env,
             String dsn,
             @Optional("null") String user,
             @Optional("null") String password,
             @Optional ArrayValue options)
  {
    _env = env;
    _dsn = dsn;
    _user = user;
    _password = password;
    _error = new PDOError(_env);

    // XXX: following would be better as annotation on destroy() method
    _env.addCleanup(this);

    try {
      DataSource ds = getDataSource(env, dsn);

      if (ds == null) {
        env.warning(L.l("'{0}' is an unknown PDO data source.", dsn));
      }
      else if (_user != null && ! "".equals(_user))
        _conn = ds.getConnection(_user, _password);
      else
        _conn = ds.getConnection();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {

      env.warning(e.getMessage(), e);
      _error.error(e);
    }
  }

  /**
   * Starts a transaction.
   */
  public boolean beginTransaction()
  {
    if (_conn == null)
      return false;

    if (_inTransaction)
      return false;

    _inTransaction = true;

    try {
      _conn.setAutoCommit(false);
      return true;
    }
    catch (SQLException e) {
      _error.error(e);
      return false;
    }
  }

  private void closeStatements()
  {
    Statement lastStatement = _lastStatement;

    _lastInsertId = null;
    _lastStatement = null;
    _lastPDOStatement = null;

    try {
      if (lastStatement != null)
        lastStatement.close();
    }
    catch (Throwable t) {
      log.log(Level.WARNING, t.toString(), t);
    }

  }

  /**
   * Commits a transaction.
   */
  public boolean commit()
  {
    if (_conn == null)
      return false;

    if (! _inTransaction)
      return false;

    _inTransaction = false;

    boolean result = false;
    try {
      _conn.commit();
      _conn.setAutoCommit(true);

      return true;
    }
    catch (SQLException e) {
      _error.error(e);
    }

    return result;
  }

  public void close()
  {
    cleanup();
  }

  /**
   * Implements the EnvCleanup interface.
   */
  public void cleanup()
  {
    Connection conn = _conn;

    _conn = null;

    closeStatements();

    if (conn != null) {
      try {
        conn.close();
      }
      catch (SQLException e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  public String errorCode()
  {
    return _error.errorCode();
  }

  public ArrayValue errorInfo()
  {
    return _error.errorInfo();
  }

  /**
   * Executes a statement, returning the number of rows.
   */
  public int exec(String query)
    throws SQLException
  {
    if (_conn == null)
      return -1;

    closeStatements();

    Statement stmt = null;

    int rowCount;

    try {
      stmt = _conn.createStatement();
      stmt.setEscapeProcessing(false);

      if (stmt.execute(query)) {
        ResultSet resultSet = null;

        try {
          resultSet = stmt.getResultSet();

          resultSet.last();

          rowCount = resultSet.getRow();

          _lastStatement = stmt;

          stmt = null;
        }
        finally {
          try {
            if (resultSet != null)
              resultSet.close();
          }
          catch (SQLException e) {
            log.log(Level.FINER, e.toString(), e);
          }
        }
      }
      else {
        rowCount = stmt.getUpdateCount();

        _lastStatement = stmt;

        stmt = null;
      }
    } catch (SQLException e) {
      _error.error(e);

      return -1;
    } finally {
      try {
        if (stmt != null)
          stmt.close();
      } catch (SQLException e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    return rowCount;
  }

  public Value getAttribute(int attribute)
  {
    switch (attribute) {
      case ATTR_AUTOCOMMIT:
        return BooleanValue.create(getAutocommit());
      case ATTR_CASE:
        return LongValue.create(getCase());
      case ATTR_CLIENT_VERSION:
        throw new UnimplementedException();
      case ATTR_CONNECTION_STATUS:
        throw new UnimplementedException();
      case ATTR_DRIVER_NAME:
        throw new UnimplementedException();
      case ATTR_ERRMODE:
        return LongValue.create(_error.getErrmode());
      case ATTR_ORACLE_NULLS:
        return LongValue.create(getOracleNulls());
      case ATTR_PERSISTENT:
        return BooleanValue.create(getPersistent());
      case ATTR_PREFETCH:
        return LongValue.create(getPrefetch());
      case ATTR_SERVER_INFO:
        return StringValue.create(getServerInfo());
      case ATTR_SERVER_VERSION:
        return StringValue.create(getServerVersion());
      case ATTR_TIMEOUT:
        return LongValue.create(getTimeout());

      default:
        _error.unsupportedAttribute(attribute);
        // XXX: check what php does
        return BooleanValue.FALSE;

    }
  }

  public static ArrayValue getAvailableDrivers()
  {
    ArrayValue array = new ArrayValueImpl();
    
    array.put("mysql");
    array.put("pgsql");
    array.put("java");
    array.put("jdbc");
    
    return array;
  }
  
  /**
   * Returns the auto commit value for the connection.
   */
  private boolean getAutocommit()
  {
    if (_conn == null)
      return true;

    try {
      return _conn.getAutoCommit();
    }
    catch (SQLException e) {
      _error.error(e);
      return true;
    }
  }

  public int getCase()
  {
    throw new UnimplementedException();
  }

  public int getOracleNulls()
  {
    throw new UnimplementedException();
  }

  private boolean getPersistent()
  {
    return true;
  }

  private int getPrefetch()
  {
    throw new UnimplementedException();
  }

  private String getServerInfo()
  {
    throw new UnimplementedException();
  }

  // XXX: might be int return
  private String getServerVersion()
  {
    throw new UnimplementedException();
  }

  private int getTimeout()
  {
    throw new UnimplementedException();
  }

  public String lastInsertId(@Optional String name)
  {
    if (!(name == null || name.length() == 0))
      throw new UnimplementedException("lastInsertId with name");

    if (_lastInsertId != null)
      return _lastInsertId;

    String lastInsertId = null;

    if (_lastPDOStatement != null)
      lastInsertId =  _lastPDOStatement.lastInsertId(name);
    else if (_lastStatement != null) {
      ResultSet resultSet = null;

      try {
        resultSet = _lastStatement.getGeneratedKeys();

        if (resultSet.next())
          lastInsertId = resultSet.getString(1);
      }
      catch (SQLException ex) {
        _error.error(ex);
      }
      finally {
        try {
          if (resultSet != null)
            resultSet.close();
        }
        catch (SQLException ex) {
          log.log(Level.WARNING, ex.toString(), ex);
        }
      }
    }

    _lastInsertId = lastInsertId == null ? "0" : lastInsertId;

    return _lastInsertId;
  }

  /**
   * Prepares a statement for execution.
   */
  @ReturnNullAsFalse
  public PDOStatement prepare(String statement,
                              @Optional ArrayValue driverOptions)
  {
    if (_conn == null)
      return null;

    try {
      closeStatements();

      PDOStatement pdoStatement
        = new PDOStatement(_env, _conn, statement, true, driverOptions);
      
      _lastPDOStatement = pdoStatement;

      return pdoStatement;
    } catch (SQLException e) {
      _error.error(e);

      return null;
    }
  }

  /**
   * Queries the database
   */
  public Value query(String query)
  {
    if (_conn == null)
      return BooleanValue.FALSE;

    try {
      closeStatements();

      PDOStatement pdoStatement = new PDOStatement(
          _env, _conn, query, false, null);
      _lastPDOStatement = pdoStatement;
      return _env.wrapJava(pdoStatement);
    } catch (SQLException e) {
      _error.error(e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Quotes the string
   */
  public String quote(String query, @Optional int parameterType)
  {
    return "'" + real_escape_string(query) + "'";
  }

  /**
   * Escapes the string.
   */
  public String real_escape_string(String str)
  {
    StringBuilder buf = new StringBuilder();

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
        buf.append('\\');
        buf.append('\'');
        break;
      case '"':
        buf.append('\\');
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

    return buf.toString();
  }

  /**
   * Rolls a transaction back.
   */
  public boolean rollBack()
  {
    if (_conn == null)
      return false;

    if (! _inTransaction)
      return false;

    _inTransaction = false;

    try {
      _conn.rollback();
      _conn.setAutoCommit(true);
      return true;
    }
    catch (SQLException e) {
      _error.error(e);
      return false;
    }
  }

  public boolean setAttribute(int attribute, Value value)
  {
    return setAttribute(attribute, value, false);
  }

  private boolean setAttribute(int attribute, Value value, boolean isInit)
  {
    switch (attribute) {
      case ATTR_AUTOCOMMIT:
        return setAutocommit(value.toBoolean());

      case ATTR_ERRMODE:
        return _error.setErrmode(value.toInt());

      case ATTR_CASE:
        return setCase(value.toInt());

      case ATTR_ORACLE_NULLS:
        return setOracleNulls(value.toInt());

      case ATTR_STRINGIFY_FETCHES:
        return setStringifyFetches(value.toBoolean());

      case ATTR_STATEMENT_CLASS:
        return setStatementClass(value);
    }

    if (isInit) {
      switch (attribute) {
        // XXX: there may be more of these
        case ATTR_TIMEOUT:
          return setTimeout(value.toInt());

        case ATTR_PERSISTENT:
          return setPersistent(value.toBoolean());
      }
    }

    // XXX: check what PHP does
    _error.unsupportedAttribute(attribute);
    return false;
  }

  /**
   * Sets the auto commit, if true commit every statement.
   * @return true on success, false on error.
   */
  private boolean setAutocommit(boolean autoCommit)
  {
    if (_conn == null)
      return false;

    try {
      _conn.setAutoCommit(autoCommit);
    }
    catch (SQLException e) {
      _error.error(e);
      return false;
    }

    return true;
  }

  /**
   * Force column names to a specific case.
   *
   * <dl>
   * <dt>{@link CASE_LOWER}
   * <dt>{@link CASE_NATURAL}
   * <dt>{@link CASE_UPPER}
   * </dl>
   */
  private boolean setCase(int value)
  {
    switch (value) {
      case CASE_LOWER:
      case CASE_NATURAL:
      case CASE_UPPER:
        throw new UnimplementedException();

      default:
        _error.unsupportedAttributeValue(value);
        return false;
    }
  }

  /**
   * Sets whether or not the convert nulls and empty strings, works for
   * all drivers.
   *
   * <dl>
   * <dt> {@link NULL_NATURAL}
   * <dd> no conversion
   * <dt> {@link NULL_EMPTY_STRING}
   * <dd> empty string is converted to NULL
   * <dt> {@link NULL_TO_STRING} NULL
   * <dd> is converted to an empty string.
   * </dl>
   *
   * @return true on success, false on error.
   */
  private boolean setOracleNulls(int value)
  {
    switch (value) {
      case NULL_NATURAL:
      case NULL_EMPTY_STRING:
      case NULL_TO_STRING:
        throw new UnimplementedException();
      default:
        _error.warning(L.l("unknown value `{0}'", value));
        _error.unsupportedAttributeValue(value);
        return false;
    }
  }

  private boolean setPersistent(boolean isPersistent)
  {
    return true;
  }

  private boolean setPrefetch(int prefetch)
  {
    throw new UnimplementedException();
  }

  /**
   * Sets a custom statement  class derived from PDOStatement.
   *
   * @param value an array(classname, array(constructor args)).
   *
   * @return true on success, false on error.
   */
  private boolean setStatementClass(Value value)
  {
    throw new UnimplementedException("ATTR_STATEMENT_CLASS");
  }

  /**
   * Convert numeric values to strings when fetching.
   *
   * @return true on success, false on error.
   */
  private boolean setStringifyFetches(boolean stringifyFetches)
  {
    throw new UnimplementedException();
  }

  private boolean setTimeout(int timeoutSeconds)
  {
    throw new UnimplementedException();
  }

  /**
   * Opens a connection based on the dsn.
   */
  private DataSource getDataSource(Env env, String dsn)
    throws Exception
  {
    if (dsn.startsWith("mysql:"))
      return getMysqlDataSource(env, dsn);
    if (dsn.startsWith("pgsql:"))
      return getPgsqlDataSource(env, dsn);
    else if (dsn.startsWith("java"))
      return getJndiDataSource(env, dsn);
    else if (dsn.startsWith("resin:"))
      return getResinDataSource(env, dsn);
    else {
      env.error(L.l("'{0}' is an unknown PDO data source.",
                    dsn));

      return null;
    }
  }

  /**
   * Opens a mysql connection based on the dsn.
   */
  private DataSource getMysqlDataSource(Env env, String dsn)
    throws Exception
  {
    HashMap<String,String> attr = parseAttr(dsn, dsn.indexOf(':'));

    // XXX: more robust to get attribute values as is done in getPgsqlDataSource

    String host = attr.get("host");
    String portStr = attr.get("port");
    String dbname = attr.get("dbname");
    String unixSocket = attr.get("unix_socket");

    if (unixSocket != null) {
      env.error(L.l("PDO mysql: does not support unix_socket"));
      return null;
    }

    if (host == null)
      host = "localhost";

    int port = 3306;
    
    if (portStr != null) {
      try {
        port = Integer.parseInt(portStr);
      } catch (NumberFormatException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    if (dbname == null)
      dbname = "test";

    // PHP doc does not sure user and password as attributes for mysql,
    // but in the pgsql doc  the dsn specified user and
    // password override arguments
    // passed to constructor
    String user = attr.get("user");
    if (user != null)
      _user = user;

    String password = attr.get("password");
    if (password != null)
      _password = password;

    String driver = Mysqli.DRIVER;
    
    // XXX: mysql options?
    String url = Mysqli.getUrl(host, port, dbname, ENCODING,
                               false, false, false);
    
    return env.getDataSource(driver, url);
  }

  /**
   * Opens a postgres connection based on the dsn.
   */
  private DataSource getPgsqlDataSource(Env env, String dsn)
    throws Exception
  {
    HashMap<String,String> attr = parseAttr(dsn, dsn.indexOf(':'));

    String host = "localhost";
    String port = null;
    String dbname = "test";
    String user = null;
    String password = null;

    for (Map.Entry<String,String> entry : attr.entrySet()) {
      String key = entry.getKey();

      if ("host".equals(key))
        host = entry.getValue();
      else if ("port".equals(key))
        port = entry.getValue();
      else if ("dbname".equals(key))
        dbname = entry.getValue();
      else if ("user".equals(key))
        user = entry.getValue();
      else if ("password".equals(key))
        password = entry.getValue();
      else
        env.warning(L.l("unknown pgsql attribute '{0}'", key));
    }

    // confirmed with PHP doc,  the dsn specified user and password override
    // arguments passed to constructor
    if (user != null)
      _user = user;

    if (password != null)
      _password = password;

    String driver = "org.postgresql.Driver";

    StringBuilder url = new StringBuilder();
    url.append("jdbc:postgresql://");
    url.append(host);

    if (port != null)
      url.append(port);

    url.append('/');
    url.append(dbname);

    return env.getDataSource(driver, url.toString());
  }

  /**
   * Opens a resin connection based on the dsn.
   */
  private DataSource getResinDataSource(Env env, String dsn)
    throws Exception
  {
    String driver = "com.caucho.db.jdbc.ConnectionPoolDataSourceImpl";

    String url = "jdbc:" + dsn;

    return env.getDataSource(driver, url);
  }

  /**
   * Opens a connection based on the dsn.
   */
  private DataSource getJndiDataSource(Env env, String dsn)
  {
    DataSource ds = null;

    try {
      Context ic = new InitialContext();

      ds = (DataSource) ic.lookup(dsn);
    } catch (NamingException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    if (ds == null)
      env.error(L.l("'{0}' is an unknown PDO JNDI data source.", dsn));

    return ds;
  }

  private HashMap<String,String> parseAttr(String dsn, int i)
  {
    HashMap<String,String> attr = new LinkedHashMap<String,String>();

    int length = dsn.length();

    for (; i < length; i++) {
      char ch = dsn.charAt(i);

      if (! Character.isJavaIdentifierStart(ch))
        continue;

      StringBuilder name = new StringBuilder();
      for (;
           i < length && Character.isJavaIdentifierPart((ch = dsn.charAt(i)));
           i++) {
        name.append(ch);
      }

      for (; i < length && ((ch = dsn.charAt(i)) == ' ' || ch == '='); i++) {
      }

      StringBuilder value = new StringBuilder();
      for (; i < length && (ch = dsn.charAt(i)) != ' ' && ch != ';'; i++) {
        value.append(ch);
      }

      attr.put(name.toString(), value.toString());
    }

    return attr;
  }

  public String toString()
  {
    // do not show password!
    if (_dsn == null)
      return "PDO[]";

    if (_dsn.indexOf("pass") < 0)
      return "PDO[" + _dsn + "]";

    int i = _dsn.lastIndexOf(':');

    if (i < 0)
      return "PDO[]";

    if (_dsn.startsWith("java"))
      return "PDO[" + _dsn + "]";

    StringBuilder str = new StringBuilder();
    str.append("PDO[");

    str.append(_dsn, 0, i + 1);

    HashMap<String,String> attr = parseAttr(_dsn, i);

    if (_user != null && ! "".equals(_user))
      attr.put("user", _user);

    boolean first = true;

    for (Map.Entry<String,String> entry : attr.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if ("password".equalsIgnoreCase(key))
        value = "XXXXX";
      else if ("passwd".equalsIgnoreCase(key))
        value = "XXXXX";
      else if ("pass".equalsIgnoreCase(key))
        value = "XXXXX";

      if (!first)
        str.append(' ');

      first = false;

      str.append(key);
      str.append("=");
      str.append(value);
    }

    str.append("]");

    return str.toString();
  }
}
