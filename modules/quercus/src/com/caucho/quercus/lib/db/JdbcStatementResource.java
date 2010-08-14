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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Var;
import com.caucho.quercus.env.StringValue;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Represents a JDBC Statement value.
 */
public class JdbcStatementResource {
  private static final Logger log = Log.open(JdbcStatementResource.class);
  private static final L10N L = new L10N(JdbcStatementResource.class);

  private JdbcConnectionResource _conn;
  private ResultSet _rs;
  private String _query;
  private PreparedStatement _stmt;
  private ResultSetMetaData _metaData;
  private JdbcResultResource _resultResource = null;

  private char[] _types;
  private Value[] _params;
  private Value[] _results;

  private String _errorMessage = "";
  private int _errorCode;

  // Statement type
  // (SELECT, UPDATE, DELETE, INSERT, CREATE,
  // DROP, ALTER, BEGIN, DECLARE, UNKNOWN)
  private String _stmtType;

  /**
   * Constructor for JdbcStatementResource
   *
   * @param connV a JdbcConnectionResource connection
   */
  public JdbcStatementResource(JdbcConnectionResource connV)
  {
    _conn = connV;
  }

  /**
   * Creates _types and _params array for this prepared statement.
   *
   * @param types  = string of i,d,s,b (ie: "idds")
   * @param params = array of values (probably Vars)
   * @return true on success ir false on failure
   */
  protected boolean bindParams(Env env,
                               String types,
                               Value[] params)
  {
    // This will create the _types and _params arrays
    // for this prepared statement.

    final int size = types.length();

    // Check to see that types and params have the same length
    if (params.length == 0 || size != params.length) {
      env.warning(L.l("number of types does not match number of parameters"));
      return false;
    }

    // Check to see that types only contains i,d,s,b
    for (int i = 0; i < size; i++) {
      if ("idsb".indexOf(types.charAt(i)) < 0) {
        env.warning(L.l("invalid type string {0}", types));
        return false;
      }
    }

    _types = new char[size];
    _params = new Value[size];

    for (int i = 0; i < size; i++) {
      _types[i] = types.charAt(i);
      _params[i] = params[i];
    }

    return true;
  }

  /**
   * Associate (bind) columns in the result set to variables.
   * <p/>
   * NB: we assume that the statement has been executed and
   * compare the # of outParams w/ the # of columns in the
   * resultset because we cannot know in advance how many
   * columns "SELECT * FROM TableName" can return.
   * <p/>
   * PHP 5.0 seems to provide some rudimentary checking on # of
   * outParams even before the statement has been executed
   * and only issues a warning in the case of "SELECT * FROM TableName".
   * <p/>
   * Our implementation REQUIRES the execute happen first.
   *
   * @param env the PHP executing environment
   * @param outParams the output variables
   * @return true on success or false on failure
   */
  public boolean bindResults(Env env,
                             Value[] outParams)
  {
    final int size = outParams.length;
    int numColumns;

    try {
      ResultSetMetaData md = getMetaData();

      numColumns = md.getColumnCount();
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }

    for (int i = 0; i < size; i++) {
      Value val = outParams[i];

      if (! (val instanceof Var)) {
        env.error(L.l("Only variables can be passed by reference"));
        return false;
      }
    }

    if ((size == 0) || (size != numColumns)) {
      env.warning(
          L.l("number of bound variables does not equal number of columns"));
      return false;
    }

    _results = new Value[size];

    System.arraycopy(outParams, 0, _results, 0, size);

    return true;
  }

  /**
   * Closes the result set, if any, and closes this statement.
   */
  public void close()
  {
    try {
      ResultSet rs = _rs;
      _rs = null;
      
      if (rs != null)
        rs.close();

      if (_stmt != null)
        _stmt.close();

    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Advance the cursor the number of rows given by offset.
   *
   * @param offset the number of rows to move the cursor
   * @return true on success or false on failure
   */
  protected boolean dataSeek(int offset)
  {
    return JdbcResultResource.setRowNumber(_rs, offset);
  }

  /**
   * Returns the error number for the last error.
   *
   * @return the error number
   */
  public int errorCode()
  {
    return _errorCode;
  }

  /**
   * Returns the error message for the last error.
   *
   * @return the error message
   */
  public String errorMessage()
  {
    return _errorMessage;
  }

  /**
   * Executes a prepared Query.
   *
   * @param env the PHP executing environment
   * @return true on success or false on failure
   */
  public boolean execute(Env env)
  {
    try {
      if (_types != null) {
        int size = _types.length;
        for (int i = 0; i < size; i++) {
          switch (_types[i]) {
          case 'i':
            _stmt.setInt(i + 1, _params[i].toInt());
            break;
          case 'd':
            _stmt.setDouble(i + 1, _params[i].toDouble());
            break;
            // XXX: blob needs to be redone
            // Currently treated as a string
          case 'b':
            _stmt.setString(i + 1, _params[i].toString());
            break;
          case 's':
            _stmt.setString(i + 1, _params[i].toString());
            break;
          default:
            break;
          }
        }
      }

      return executeStatement();

    } catch (SQLException e) {
      env.warning(L.l(e.toString()));
      log.log(Level.FINE, e.toString(), e);
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      return false;
    }
  }

  /**
   * Executes underlying statement
   * Known subclasses: see PostgresStatement.execute
   */
  protected boolean executeStatement()
    throws SQLException
  {
    try {
      if (_stmt.execute()) {
        _conn.setAffectedRows(0);
        _rs = _stmt.getResultSet();
      } else {
        _conn.setAffectedRows(_stmt.getUpdateCount());
      }

      return true;
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      throw e;
    }
  }

  /**
   * Fetch results from a prepared statement into bound variables.
   *
   * @return true on success, false on error, null if no more rows
   */
  public Value fetch(Env env)
  {
    try {
      if (_rs == null)
        return NullValue.NULL;
      
      if (_rs.next()) {
        if (_metaData == null)
          _metaData = _rs.getMetaData();

        JdbcResultResource resultResource = getResultMetadata();
        int size = _results.length;

        for (int i = 0; i < size; i++) {
          _results[i].set(_resultResource.getColumnValue(
              env, _rs, _metaData, i + 1));
        }
        return BooleanValue.TRUE;
      } else {
        return NullValue.NULL;
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Frees the associated result.
   *
   * @return true on success or false on failure
   */
  public boolean freeResult()
  {
    try {
      ResultSet rs = _rs;
      _rs = null;

      if (rs != null)
        rs.close();

      if (_resultResource != null) {
        _resultResource.close();
        _resultResource = null;
      }
      return true;
    } catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      log.log(Level.FINE, e.toString(), e);
      return false;
    }
  }

  /**
   * Returns the meta data for corresponding to the current result set.
   *
   * @return the result set meta data
   */
  protected ResultSetMetaData getMetaData()
    throws SQLException
  {
    if (_metaData == null)
      _metaData = _rs.getMetaData();

    return _metaData;
  }

  /**
   * Returns the number of rows in the result set.
   *
   * @return the number of rows in the result set
   */
  public int getNumRows()
    throws SQLException
  {
    if (_rs != null)
      return JdbcResultResource.getNumRows(_rs);
    else
      return 0;
  }

  /**
   * Returns the internal prepared statement.
   *
   * @return the internal prepared statement
   */
  protected PreparedStatement getPreparedStatement()
  {
    return _stmt;
  }

  /**
   * Resets _fieldOffset in _resultResource
   *
   * @return null if _resultResource == null, otherwise _resultResource
   */
  public JdbcResultResource getResultMetadata()
  {
    if (_resultResource != null) {
      _resultResource.setFieldOffset(0);
      return _resultResource;
    }

    if (_stmt == null || _rs == null)
      return null;

    _resultResource
      = new JdbcResultResource(_conn.getEnv(), _stmt, _rs, _conn);
    return _resultResource;
  }

  /**
   * Returns the internal result set.
   *
   * @return the internal result set
   */
  protected ResultSet getResultSet()
  {
    return _rs;
  }

  /**
   * Returns the underlying SQL connection
   * associated to this statement.
   */
  protected Connection getJavaConnection()
    throws SQLException
  {
    return validateConnection().getJavaConnection();
  }

  /**
   * Returns this statement type.
   *
   * @return this statement type:
   * SELECT, UPDATE, DELETE, INSERT, CREATE, DROP,
   * ALTER, BEGIN, DECLARE, or UNKNOWN.
   */
  public String getStatementType()
  {
    // Oracle Statement type
    // Also used internally in Postgres (see PostgresModule)
    // (SELECT, UPDATE, DELETE, INSERT, CREATE, DROP,
    // ALTER, BEGIN, DECLARE, UNKNOWN)

    _stmtType = _query;
    _stmtType = _stmtType.replaceAll("\\s+.*", "");
    
    if (_stmtType.length() == 0)
      _stmtType = "UNKNOWN";
    else {
      _stmtType = _stmtType.toUpperCase();
      String s = _stmtType.replaceAll(
          "(SELECT|UPDATE|DELETE|INSERT|CREATE|DROP|ALTER|BEGIN|DECLARE)", "");
      if (! s.equals(""))
        _stmtType = "UNKNOWN";
    }

    return _stmtType;
  }

  /**
   * Counts the number of parameter markers in the query string.
   *
   * @return the number of parameter markers in the query string
   */
  public int paramCount()
  {
    if (_query == null)
      return -1;

    int count = 0;
    int length = _query.length();
    boolean inQuotes = false;
    char c;

    for (int i = 0; i < length; i++) {
      c = _query.charAt(i);

      if (c == '\\') {
        if (i < length - 1)
          i++;
        continue;
      }

      if (inQuotes) {
        if (c == '\'')
          inQuotes = false;
        continue;
      }

      if (c == '\'') {
        inQuotes = true;
        continue;
      }

      if (c == '?') {
        count++;
      }
    }

    return count;
  }

  /**
   * Prepares this statement with the given query.
   *
   * @param query SQL query
   * @return true on success or false on failure
   */
  public boolean prepare(Env env, StringValue query)
  {
    try {
      if (_stmt != null)
        _stmt.close();

      _query = query.toString();

      if (_query.length() == 0)
        return false;

      Connection conn = _conn.getConnection(env);
      
      if (conn == null)
        return false;
      
      if (this instanceof OracleStatement) {
        _stmt = conn.prepareCall(_query,
                                 ResultSet.TYPE_SCROLL_INSENSITIVE,
                                 ResultSet.CONCUR_READ_ONLY);
      } else if (_conn.isSeekable()) {
        _stmt = conn.prepareStatement(_query,
                                      ResultSet.TYPE_SCROLL_INSENSITIVE,
                                      ResultSet.CONCUR_READ_ONLY);
      } else {
        _stmt = conn.prepareStatement(_query);
      }

      return true;

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      return false;
    }
  }

  /**
   * Prepares statement with the given query.
   *
   * @param query SQL query
   * @return true on success or false on failure
   */
  public boolean prepareStatement(Env env, String query)
  {
    try {
      if (_stmt != null)
        _stmt.close();

      _query = query;

      Connection conn = _conn.getConnection(env);
      
      if (conn == null)
        return false;
      
      if (this instanceof OracleStatement) {
        _stmt = conn.prepareCall(query,
                                 ResultSet.TYPE_SCROLL_INSENSITIVE,
                                 ResultSet.CONCUR_READ_ONLY);
      } else {
        _stmt = conn.prepareStatement(query,
                                      ResultSet.TYPE_SCROLL_INSENSITIVE,
                                      ResultSet.CONCUR_READ_ONLY);
      }

      return true;

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      return false;
    }
  }

  /**
   * Returns a parameter value
   * Known subclasses: see PostgresStatement.execute
   */
  protected Value getParam(int i)
  {
    if (i >= _params.length)
      return UnsetValue.UNSET;

    return _params[i];
  }

  /**
   * Returns the number of parameters available to binding
   * Known subclasses: see PostgresStatement.execute
   */
  protected int getParamLength()
  {
    return _params.length;
  }

  /**
   * Changes the internal statement.
   */
  protected void setPreparedStatement(PreparedStatement stmt)
  {
    _stmt = stmt;
  }

  /**
   * Changes the internal result set.
   */
  protected void setResultSet(ResultSet rs)
  {
    _rs = rs;
  }

  /**
   * Returns the number of fields in the result set.
   *
   * @param env the PHP executing environment
   * @return the number of fields in the result set
   */
  public int getFieldCount()
  {
    if (_resultResource == null)
      return 0;

    return _resultResource.getFieldCount();
  }

  /**
   * Sets the given parameter
   * Known subclasses: see PostgresStatement.execute
   */
  protected void setObject(int i, Object param)
    throws Exception
  {
    try {
      // See php/4358, php/43b8, php/43d8, and php/43p8.
      java.sql.ParameterMetaData pmd = _stmt.getParameterMetaData();
      int type = pmd.getParameterType(i);

      switch (type) {

      case Types.OTHER:
        {
          // See php/43b8
          String typeName = pmd.getParameterTypeName(i);
          if (typeName.equals("interval")) {
            _stmt.setObject(i, param);
          } else {
            Class cl = Class.forName("org.postgresql.util.PGobject");
            Constructor constructor = cl.getDeclaredConstructor(null);
            Object object = constructor.newInstance();

            Method method = cl.getDeclaredMethod(
                "setType", new Class[] {String.class});
            method.invoke(object, new Object[] {typeName});

            method = cl.getDeclaredMethod(
                "setValue", new Class[] {String.class});
            method.invoke(object, new Object[] {param});

            _stmt.setObject(i, object, type);
          }
          break;
        }

      case Types.DOUBLE:
        {
          // See php/43p8.
          String typeName = pmd.getParameterTypeName(i);
          if (typeName.equals("money")) {
            String s = param.toString();

            if (s.length() == 0) {
              throw new IllegalArgumentException(
                  L.l("argument `{0}' cannot be empty", param));
            } else {

              String money = s;

              if (s.charAt(0) == '$')
                s = s.substring(1);
              else
                money = "$" + money;

              try {
                // This will throw an exception if not double while
                // trying to setObject() would not. The error would
                // come late, otherwise. See php/43p8.
                Double.parseDouble(s);
              } catch (Exception ex) {
                throw new IllegalArgumentException(L.l(
                    "cannot convert argument `{0}' to money", param));
              }

              Class cl = Class.forName("org.postgresql.util.PGmoney");
              Constructor constructor = cl.getDeclaredConstructor(
                  new Class[] {String.class});
              Object object = constructor.newInstance(new Object[] {money});

              _stmt.setObject(i, object, Types.OTHER);

              break;
            }
          }
          // else falls to default case
        }

      default:
        _stmt.setObject(i, param, type);
      }
    }
    catch (SQLException e) {
      _errorMessage = e.getMessage();
      _errorCode = e.getErrorCode();
      throw e;
    }
    catch (Exception e) {
      _stmt.clearParameters();
      throw e;
    }
  }

  /**
   * Returns a string representation for this object.
   *
   * @return the string representation for this object
   */
  public String toString()
  {
    return getClass().getName() + "[" + _conn + "]";
  }

  /**
   * Validates the connection resource.
   *
   * @return the validated connection resource
   */
  public JdbcConnectionResource validateConnection()
  {
    return _conn;
  }
}

