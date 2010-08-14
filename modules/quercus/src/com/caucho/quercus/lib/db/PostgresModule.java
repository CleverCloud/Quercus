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

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// Do not add new compile dependencies (use reflection instead)
// import org.postgresql.largeobject.*;


/**
 * Quercus postgres routines.
 */
public class PostgresModule extends AbstractQuercusModule {

  private static final Logger log = Log.open(PostgresModule.class);
  private static final L10N L = new L10N(PostgresModule.class);

  public static final int PGSQL_ASSOC = 0x01;
  public static final int PGSQL_NUM = 0x02;
  public static final int PGSQL_BOTH = 0x03;
  public static final int PGSQL_CONNECT_FORCE_NEW = 0x04;
  public static final int PGSQL_CONNECTION_BAD = 0x05;
  public static final int PGSQL_CONNECTION_OK = 0x06;
  public static final int PGSQL_SEEK_SET = 0x07;
  public static final int PGSQL_SEEK_CUR = 0x08;
  public static final int PGSQL_SEEK_END = 0x09;
  public static final int PGSQL_EMPTY_QUERY = 0x0A;
  public static final int PGSQL_COMMAND_OK = 0x0B;
  public static final int PGSQL_TUPLES_OK = 0x0C;
  public static final int PGSQL_COPY_OUT = 0x0D;
  public static final int PGSQL_COPY_IN = 0x0E;
  public static final int PGSQL_BAD_RESPONSE = 0x0F;
  public static final int PGSQL_NONFATAL_ERROR = 0x10;
  public static final int PGSQL_FATAL_ERROR = 0x11;
  public static final int PGSQL_TRANSACTION_IDLE = 0x12;
  public static final int PGSQL_TRANSACTION_ACTIVE = 0x13;
  public static final int PGSQL_TRANSACTION_INTRANS = 0x14;
  public static final int PGSQL_TRANSACTION_INERROR = 0x15;
  public static final int PGSQL_TRANSACTION_UNKNOWN = 0x16;
  public static final int PGSQL_DIAG_SEVERITY = 0x17;
  public static final int PGSQL_DIAG_SQLSTATE = 0x18;
  public static final int PGSQL_DIAG_MESSAGE_PRIMARY = 0x19;
  public static final int PGSQL_DIAG_MESSAGE_DETAIL = 0x20;
  public static final int PGSQL_DIAG_MESSAGE_HINT = 0x21;
  public static final int PGSQL_DIAG_STATEMENT_POSITION = 0x22;
  public static final int PGSQL_DIAG_INTERNAL_POSITION = 0x23;
  public static final int PGSQL_DIAG_INTERNAL_QUERY = 0x24;
  public static final int PGSQL_DIAG_CONTEXT = 0x25;
  public static final int PGSQL_DIAG_SOURCE_FILE = 0x26;
  public static final int PGSQL_DIAG_SOURCE_LINE = 0x27;
  public static final int PGSQL_DIAG_SOURCE_FUNCTION = 0x28;
  public static final int PGSQL_ERRORS_TERSE = 0x29;
  public static final int PGSQL_ERRORS_DEFAULT = 0x2A;
  public static final int PGSQL_ERRORS_VERBOSE = 0x2B;
  public static final int PGSQL_STATUS_LONG = 0x2C;
  public static final int PGSQL_STATUS_STRING = 0x2D;
  public static final int PGSQL_CONV_IGNORE_DEFAULT = 0x2E;
  public static final int PGSQL_CONV_FORCE_NULL = 0x2F;

  /**
   * Constructor
   */
  public PostgresModule()
  {
  }

  /**
   * Returns true for the postgres extension.
   */
  public String []getLoadedExtensions()
  {
    return new String[] { "postgres", "pgsql" };
  }

  /**
   * Returns number of affected records (tuples)
   */
  public static int pg_affected_rows(Env env,
                                     @NotNull PostgresResult result)
  {
    try {
      if (result == null)
        return -1;

      return result.getAffectedRows();
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return 0;
    }
  }

  /**
   * pg_affected_rows() alias.
   */
  public static int pg_cmdtuples(Env env,
                                 @NotNull PostgresResult result)
  {
    if (result == null)
      return -1;
    
    return pg_affected_rows(env, result);
  }
  
  /**
   * Cancel an asynchronous query
   */
  public static boolean pg_cancel_query(Env env,
                                        @NotNull Postgres conn)
  {
    try {
      if (conn == null)
        return false;
      
      conn.setAsynchronousStatement(null);
      conn.setAsynchronousResult(null);

      return true;
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Gets the client encoding
   */
  @ReturnNullAsFalse
  public static String pg_client_encoding(Env env,
                                          @Optional Postgres conn)
  {
    try {
      if (conn == null)
        conn = getConnection(env);

      return conn.getClientEncoding();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Closes a PostgreSQL connection
   */
  public static boolean pg_close(Env env,
                                 @Optional Postgres conn)
  {
    try {
      if (conn == null)
        return false;

      if (conn == env.getSpecialValue("caucho.postgres"))
        env.removeSpecialValue("caucho.postgres");

      conn.close(env);

      return true;
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return false;
  }

  /**
   * Open a PostgreSQL connection
   */
  @ReturnNullAsFalse
  public static Postgres pg_connect(Env env,
                                    String connectionString,
                                    @Optional int connectionType)
  {
    try {
      String host = "localhost";
      int port = 5432;
      String dbName = "";
      String userName = "";
      String password = "";

      HashMap<String, String> nameValueMap
        = parseConnectionString(connectionString);

      String value = nameValueMap.get("host");
      if (value != null)
        host = nameValueMap.get("host");
      
      value = nameValueMap.get("port");
      if (value != null) {
        port = 0;
        int len = value.length();
        
        for (int i = 0; i < len; i++) {
          char ch = value.charAt(i);
          
          if ('0' <= ch && ch <= '9')
            port = port * 10 + value.charAt(i) - '0';
          else
            break;
        }
      }
      
      value = nameValueMap.get("dbname");
      if (value != null)
        dbName = value;
      
      value = nameValueMap.get("user");
      if (value != null)
        userName = value;
      
      value = nameValueMap.get("password");
      if (value != null)
        password = value;

      String driver = "org.postgresql.Driver";
      String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;

      Postgres postgres
        = new Postgres(
          env, host, userName, password, dbName, port, driver, url);

      if (! postgres.isConnected())
        return null;

      env.setSpecialValue("caucho.postgres", postgres);

      return postgres;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns the name/value pairs from the postgres connection string.
   */
  private static HashMap<String, String> parseConnectionString(String s)
  {
    HashMap<String, String> map = new HashMap<String, String>();
    
    char ch;
    int len = s.length();
    
    int i = 0;
    
    CharBuffer buffer = new CharBuffer();
    
    while (i < len) {
      buffer.clear();
      
      // skip whitespace
      for (; i < len && Character.isWhitespace(ch = s.charAt(i)); i++) {
      }
      
      // get name
      for (;
           i < len && ! Character.isWhitespace(ch = s.charAt(i))
               && ch != '='; i++) {
        buffer.append(ch);
      }
      
      String name = buffer.toString();
      buffer.clear();
      
      // skip until '='
      while (i < len && (ch = s.charAt(i++)) != '=') {
      }
      
      // skip whitespace
      for (; i < len && Character.isWhitespace(ch = s.charAt(i)); i++) {
      }
      
      boolean isQuoted = false;
      
      // value may be quoted
      if (i < len) {
        if ((ch = s.charAt(i++)) == '\'')
          isQuoted = true;
        else
          buffer.append(ch);
      }
      
      boolean isEscaped = false;
      
      // get value
      loop:
      while (i < len) {
        ch = s.charAt(i++);
        
        switch(ch) {
          case '\\':
            if (isEscaped)
              buffer.append(ch);

            isEscaped = !isEscaped;
            break;
            
          case '\'':
            if (isEscaped) {
              buffer.append(ch);
              isEscaped = false;
              break;
            }
            else if (isQuoted)
              break loop;

          case ' ':
          case '\n':
          case '\r':
          case '\f':
          case '\t':
            if (isQuoted) {
              buffer.append(ch);
              break;
            }
            else if (isEscaped) {
              buffer.append('\\');
              break loop;
            }
            else
              break loop;

          default:
            if (isEscaped) {
              buffer.append('\\');
              isEscaped = false;
            }

            buffer.append(ch);
        }
      }

      String value = buffer.toString();
      
      if (name.length() > 0)
        map.put(name, value);
    }

    return map;
  }
  
  /**
   * Get connection is busy or not
   */
  public static boolean pg_connection_busy(Env env,
                                           @NotNull Postgres conn)
  {
    // Always return false, for now (pg_send_xxxx are not asynchronous)
    // so there should be no reason for a connection to become busy in
    // between different pg_xxx calls.

    return false;
  }

  /**
   * Reset connection (reconnect)
   */
  public static boolean pg_connection_reset(Env env,
                                            @NotNull Postgres conn)
  {
    try {
      if (conn == null)
        return false;

      // Query database name before closing connection

      String dbname = conn.getDbName();

      conn.close(env);

      conn = new Postgres(env,
                          conn.getHost(),
                          conn.getUserName(),
                          conn.getPassword(),
                          dbname,
                          conn.getPort(),
                          conn.getDriver(),
                          conn.getUrl());

      env.setSpecialValue("caucho.postgres", conn);

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Get connection status
   */
  public static int pg_connection_status(Env env,
                                         @NotNull Postgres conn)
  {
    try {
      if (conn == null)
        return PGSQL_CONNECTION_BAD;
      
      boolean ping = pg_ping(env, conn);

      return ping ? PGSQL_CONNECTION_OK : PGSQL_CONNECTION_BAD;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return PGSQL_CONNECTION_BAD;
    }
  }

  /**
   * Convert associative array values into suitable for SQL statement
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_convert(Env env,
                                      @NotNull Postgres conn,
                                      String tableName,
                                      ArrayValue assocArray,
                                      @Optional("0") int options)
  {
    try {
      if (conn == null)
        return null;

      // XXX: options has not been implemented yet.

      // XXX: the following PHP note has not been implemented yet.
      // Note:  If there are boolean fields in table_name don't use
      // the constant TRUE in assoc_array. It will be converted to the
      // string 'TRUE' which is no valid entry for boolean fields in
      // PostgreSQL. Use one of t, true, 1, y, yes instead.

      if (options > 0) {
        throw new UnimplementedException("pg_convert with options");
      }

      PostgresResult result;

      Connection jdbcConn = conn.getJavaConnection();
      DatabaseMetaData dbMetaData = jdbcConn.getMetaData();

      ResultSet rs = dbMetaData.getColumns("", "", tableName, "");

      // Check column count
      ResultSetMetaData rsMetaData = rs.getMetaData();
      int n = rsMetaData.getColumnCount();
      if (n < assocArray.getSize())
        return null;

      ArrayValueImpl newArray = new ArrayValueImpl();

      // Keep track of column matches: assocArray vs. table columns
      int matches = 0;

      while (rs.next()) {
        // Retrieve the original value to be converted
        String columnName = rs.getString("COLUMN_NAME");
        Value columnNameV = StringValue.create(columnName);
        Value value = assocArray.get(columnNameV);

        // Check for column not passed in
        if (value == UnsetValue.UNSET)
          continue;

        matches++;

        if (value.isNull()) {
          value = StringValue.create("NULL");
          // Add the converted value
          newArray.put(columnNameV, value);
          continue;
        }

        // Retrieve the database column type
        int dataType = rs.getInt("DATA_TYPE");

        // Convert the original value to the database type
        switch (dataType) {
        case Types.BIT:
        case Types.TINYINT:
        case Types.SMALLINT:
        case Types.INTEGER:
        case Types.BIGINT:
          if (value.isLongConvertible()) {
            value = LongValue.create(value.toLong());
          } else {
            StringValue sb = env.createUnicodeBuilder();
            value = sb.append("'").append(value).append("'");
          }
          break;

        case Types.DECIMAL:
        case Types.DOUBLE:
        case Types.FLOAT:
        case Types.NUMERIC:
        case Types.REAL:
          if (value.isDoubleConvertible()) {
            value = DoubleValue.create(value.toDouble());
          } else {
            StringValue sb = env.createUnicodeBuilder();
            value = sb.append("'").append(value).append("'");
          }
          break;

        default:
          StringValue sb = env.createUnicodeBuilder();
          if (value.isNumberConvertible())  {
            value = sb.append(value);
          } else {
            value = sb.append("'").append(value).append("'");
          }
        }

        // Add the converted value
        newArray.put(columnNameV, value);
      }

      rs.close();

      // Check if all columns were consumed. Otherwise, there are
      // wrong column names passed in.
      if (matches < assocArray.getSize()) {
        return null;
      }

      return newArray;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Insert records into a table from an array
   */
  public static boolean pg_copy_from(Env env,
                                     @NotNull Postgres conn,
                                     String tableName,
                                     ArrayValue rows,
                                     @Optional("") String delimiter,
                                     @Optional("") String nullAs)
  {
    // XXX delimiter not used?

    try {
      if (conn == null)
        return false;

      // XXX: At the time this was implemented, the JDBC driver
      // did not support SQL COPY operations that could simplify
      // the code below.

      String delimiterRegex;
      if (delimiter.equals("")) {
        delimiter = "\t";
        delimiterRegex = "\\t";
      } else {
        // XXX: even the native php version does not seem to do it very well.
        throw new UnimplementedException(
            "pg_copy_from with non-default delimiter");
      }

      if (nullAs.equals("")) {
        nullAs = "\\N";
      } else {
        // XXX: even the native php version does not seem to do it very well.
        throw new UnimplementedException(
            "pg_copy_from with non-default nullAs");
      }

      ArrayValueImpl array = (ArrayValueImpl) rows;
      int size = array.size();

      String baseInsert = "INSERT INTO " + tableName + " VALUES(";

      StringBuilder sb = new StringBuilder(baseInsert);

      int lenBaseInsert = sb.length();

      for (int i = 0; i < size; i++) {
        // Every line has a new-line '\n' character and
        // possibly many NULL values "\\N". Ex:
        // line =
//         "\\N\tNUMBER1col\t\\N\t\\N\tNUM" +
//             "BER2col\tNUMBER3col\tNUMBER4col\t\\N\n";
        String line = array.get(LongValue.create(i)).toString();
        line = line.substring(0, line.length() - 1);

        // "INSERT INTO " + tableName + " VALUES("
        sb.setLength(lenBaseInsert);

        // Split columns
        String []cols = line.split(delimiterRegex);

        int len = cols.length;

        if (len > 0) {

          len--;

          for (int j = 0; j < len; j++) {
            if (cols[j].equals(nullAs)) {
              sb.append("NULL, ");
            } else {
              sb.append("'");
              sb.append(cols[j]);
              sb.append("', ");
            }
          }

          if (cols[len].equals(nullAs)) {
            sb.append("NULL)");
          } else {
            sb.append("'");
            sb.append(cols[len]);
            sb.append("')");
          }

          // Insert record
          pg_query(env, conn, sb.toString());
        }
      }

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Copy a table to an array
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_copy_to(Env env,
                                      @NotNull Postgres conn,
                                      String tableName,
                                      @Optional("") String delimiter,
                                      @Optional("") String nullAs)
  {
    try {
      if (conn == null)
        return null;

      // XXX: At the time this was implemented, the JDBC driver
      // did not support SQL COPY operations that could simplify
      // the code below.

      // XXX: This should be replaced when @Optional("\t") is fixed.
      if (delimiter.equals("")) {
        delimiter = "\t";
      }

      // XXX: This should be replaced when @Optional("\\N") is fixed.
      // Note: according to php.net, it must be \\N, i.e. the
      // two-character sequence: {'\\', 'N'}
      if (nullAs.equals("")) {
        nullAs = "\\N";
      }

      PostgresResult result = pg_query(env, conn, "SELECT * FROM " + tableName);

      ArrayValueImpl newArray = new ArrayValueImpl();

      Object value;

      int curr = 0;

      while ((value = result.fetchArray(env, PGSQL_NUM)) != null) {

        ArrayValueImpl arr = (ArrayValueImpl) value;
        int count = arr.size();

        StringValue sb = env.createUnicodeBuilder();

        LongValue currValue = LongValue.create(curr);

        for (int i = 0; i < count; i++) {

          if (sb.length() > 0)
            sb.append(delimiter);

          Value v = newArray.get(currValue);

          Value fieldValue = arr.get(LongValue.create(i));

          if (fieldValue instanceof NullValue) {
            sb.append(nullAs);
          } else {
            sb.append(fieldValue.toString());
          }
        }

        // Every line has a new-line character.
        sb.append("\n");

        newArray.put(currValue, sb);

        curr++;
      }

      return newArray;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Get the database name
   */
  @ReturnNullAsFalse
  public static String pg_dbname(Env env,
                                 @Optional Postgres conn)
  {
    try {
      if (conn == null)
        conn = getConnection(env);

      return conn.getDbName();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Deletes records
   */
  public static boolean pg_delete(Env env,
                                  @NotNull Postgres conn,
                                  String tableName,
                                  ArrayValue assocArray,
                                  @Optional("-1") int options)
  {
    // From php.net: this function is EXPERIMENTAL.
    // This function is EXPERIMENTAL. The behaviour of this function,
    // the name of this function, and anything else
    // documented about this function
    // may change without notice in a future release of PHP.
    // Use this function at your own risk.

    try {
      if (conn == null)
        return false;

      if (options > 0) {
        throw new UnimplementedException("pg_delete with options");
      }

      StringBuilder condition = new StringBuilder();

      boolean isFirst = true;

      for (Map.Entry<Value,Value> entry : assocArray.entrySet()) {
        Value k = entry.getKey();
        Value v = entry.getValue();
        if (isFirst) {
          isFirst = false;
        } else {
          condition.append(" AND ");
        }
        condition.append(k.toString());
        condition.append("='");
        condition.append(v.toString());
        condition.append("'");
      }

      StringBuilder query = new StringBuilder();
      query.append("DELETE FROM ");
      query.append(tableName);
      query.append(" WHERE ");
      query.append(condition);

      pg_query(env, conn, query.toString());

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Sync with PostgreSQL backend
   */
  public static boolean pg_end_copy(Env env,
                                    @Optional Postgres conn)
  {
    env.stub("pg_end_copy");

    return false;
  }

  /**
   * Escape a string for insertion into a bytea field
   */
  @ReturnNullAsFalse
  public static StringValue pg_escape_bytea(Env env,
                                            StringValue data)
  {
    if (data.length() == 0)
      return data;

    try {
      Class cl = Class.forName("org.postgresql.util.PGbytea");

      Method method = cl.getDeclaredMethod(
          "toPGString", new Class[] {byte[].class});

      String s = (String) method.invoke(cl, new Object[] { data.toBytes()});

      return Postgres.pgRealEscapeString(env.createString(s));

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Escape a string for insertion into a bytea field.
   */
  @ReturnNullAsFalse
  public static StringValue pg_escape_bytea(Env env,
                                            @NotNull Postgres conn,
                                            StringValue data)
  {
    return pg_escape_bytea(env, data);
  }

  /**
   * Escape a string for insertion into a text field
   */
  @ReturnNullAsFalse
  public static StringValue pg_escape_string(Env env,
                                             StringValue data)
  {
    try {
      Postgres conn = getConnection(env);

      if (conn == null)
        return null;

      return conn.realEscapeString(data);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Sends a request to execute a prepared statement with given parameters,
   * and waits for the result
   */
  @ReturnNullAsFalse
  public static PostgresResult pg_execute(Env env,
                                          @NotNull Postgres conn,
                                          String stmtName,
                                          ArrayValue params)
  {
    try {
      if (conn == null)
        return null;

      PostgresStatement pstmt = conn.getStatement(stmtName);

      return executeInternal(env, conn, pstmt, params);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      conn.setResultResource(null);
      return null;
    }
  }

  /**
   * Fetches all rows in a particular result column as an array
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_fetch_all_columns(Env env,
                                                @NotNull PostgresResult result,
                                                @Optional("0") int column)
  {
    try {
      if (result == null)
        return null;

      ArrayValueImpl newArray = new ArrayValueImpl();

      int curr = 0;

      for (ArrayValue row = result.fetchRow(env);
           row != null;
           row = result.fetchRow(env)) {

        newArray.put(LongValue.create(curr++),
                     row.get(LongValue.create(column)));

      }

      if (newArray.getSize() > 0) {
        return newArray;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Fetches all rows from a result as an array
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_fetch_all(Env env,
                                        @NotNull PostgresResult result)
  {
    try {
      if (result == null)
        return null;

      ArrayValueImpl newArray = new ArrayValueImpl();

      int curr = 0;

      for (ArrayValue row = result.fetchAssoc(env);
           row != null;
           row = result.fetchAssoc(env)) {

        newArray.put(LongValue.create(curr++), row);

      }

      if (newArray.getSize() > 0) {
        return newArray;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Fetch a row as an array
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_fetch_array(
      Env env,
      @NotNull PostgresResult result,
      @Optional("-1") Value row,
      @Optional("PGSQL_BOTH") int resultType) {
    try {
      if (result == null)
        return null;

      // NOTE: pg_fetch_array has an interesting memory feature.
      // Calls to pg_fetch_array usually return the next row for
      // successive calls. There is an exception though.
      // The first time a NULL row is passed in, the previously
      // returned row is returned again. After that, successive
      // calls return the next row as usual.
      // We set a flag for this. See PostgresResult and php/4342

      if (row.isNull()) {
        if (result.getPassedNullRow()) {
          result.setPassedNullRow();
        } else {
          // Step the cursor back to the previous position
          ResultSet rs = result.getResultSet();
          rs.previous();
        }
      }

      // NOTE: row is of type Value because row is optional and there is
      // only one way to specify that 'row' will not be used:
      //
      // pg_fetch_array(result, NULL, resultType)
      //
      // The resultType will be used above though.
      //
      // For such a case, the marshalling code passes row in as NullValue.NULL
      // If we used 'int row' there would be no way to distinguish row 'zero'
      // from row 'null'.

      if (result == null)
        return  null;

      if (row.isLongConvertible() && row.toInt() >= 0) {
        if (!result.seek(env, row.toInt())) {
          env.warning(L.l("Unable to jump to row {0} on PostgreSQL result",
                          row.toInt()));
          return null;
        }
      }

      return result.fetchArray(env, resultType);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Fetch a row as an associative array
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_fetch_assoc(Env env,
                                          @NotNull PostgresResult result,
                                          @Optional("-1") Value row)
  {
    try {
      if (result == null)
        return null;
      
      if (! row.isNull() && row.toInt() >= 0) {
        result.seek(env, row.toInt());
      }

      return result.fetchAssoc(env);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Fetch a row as an object
   */
  public static Value pg_fetch_object(Env env,
                                      @NotNull PostgresResult result,
                                      @Optional("-1") Value row,
                                      @Optional int resultType)
  {
    try {
      if (result == null)
        return null;

      //@todo use optional resultType
      if ((row != null) && (!row.equals(NullValue.NULL))
          && (row.toInt() >= 0)) {
        result.seek(env, row.toInt());
      }

      Value resultValue =  result.fetchObject(env);

      // php/430l
      if (resultValue.isNull())
        return BooleanValue.FALSE;
      else
        return resultValue;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns values from a result resource
   */
  public static Value pg_fetch_result(Env env,
                                      @NotNull PostgresResult result,
                                      Value row,
                                      @Optional("-1") Value fieldNameOrNumber)
  {
    try {
      if (result == null)
        return null;

      // NOTE: row is of type Value because there is a case where
      // row is optional. In such a case, the row value passed in
      // is actually the field number or field name.

      int rowNumber = -1;

      // Handle the case: optional row with mandatory fieldNameOrNumber.
      if (fieldNameOrNumber.isLongConvertible()
          && (fieldNameOrNumber.toInt() < 0)) {
        fieldNameOrNumber = row;
        rowNumber = -1;
      } else {
        rowNumber = row.toInt();
      }

      if (rowNumber >= 0) {
        result.seek(env, rowNumber);
      }

      Value fetchRow = result.fetchRow(env);

      int fieldNumber = result.getColumnNumber(fieldNameOrNumber, 0);

      return fetchRow.get(LongValue.create(fieldNumber));

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }
  
  /**
   * Returns values from a result resource
   */
  public static Value pg_result(Env env,
                                @NotNull PostgresResult result,
                                Value row,
                                @Optional("-1") Value fieldNameOrNumber)
  {
    return pg_fetch_result(env, result, row, fieldNameOrNumber);
  }

  /**
   * Get a row as an enumerated array
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_fetch_row(Env env,
                                        @NotNull PostgresResult result,
                                        @Optional("-1") Value row)
  {
    try {
      if (result == null)
        return null;

      if (row != null && ! row.equals(NullValue.NULL) && row.toInt() >= 0) {
        result.seek(env, row.toInt());
      }

      return result.fetchRow(env);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Test if a field is SQL NULL
   */
  @ReturnNullAsFalse
  public static LongValue pg_field_is_null(
      Env env,
      @NotNull PostgresResult result,
      Value row,
      @Optional("-1") Value fieldNameOrNumber) {
    try {
      if (result == null)
        return null;

      // NOTE: row is of type Value because there is a case where
      // row is optional. In such a case, the row value passed in
      // is actually the field number or field name.

      int rowNumber = -1;

      // Handle the case: optional row with mandatory fieldNameOrNumber.
      if (fieldNameOrNumber.isLongConvertible()
          && (fieldNameOrNumber.toInt() == -1)) {
        fieldNameOrNumber = row;
        rowNumber = -1;
      } else {
        rowNumber = row.toInt();
      }

      if (rowNumber >= 0) {
        if (!result.seek(env, rowNumber)) {
          env.warning(L.l("Unable to jump to row {0} on PostgreSQL result",
                          rowNumber));
          return null;
        }
      }

      int fieldNumber = result.getColumnNumber(fieldNameOrNumber, 0);

      Value field = pg_fetch_result(env,
                                    result,
                                    LongValue.MINUS_ONE,
                                    LongValue.create(fieldNumber));

      if (field == null || field.isNull())
        return LongValue.ONE;
      else
        return LongValue.ZERO;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * pg_field_is_null() alias.
   */
  @ReturnNullAsFalse
  public static LongValue pg_fieldisnull(
      Env env,
      @NotNull PostgresResult result,
      Value row,
      @Optional("-1") Value fieldNameOrNumber) {
    return pg_field_is_null(env, result, row, fieldNameOrNumber);
  }
  
  /**
   * Returns the name of a field
   */
  public static Value pg_field_name(Env env,
                                    @NotNull PostgresResult result,
                                    int fieldNumber)
  {
    try {
      if (result == null)
        return BooleanValue.FALSE;

      return result.getFieldName(env, fieldNumber);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * pg_field_name() alias.
   */
  public static Value pg_fieldname(Env env,
                                   @NotNull PostgresResult result,
                                   int fieldNumber)
  {
    return pg_field_name(env, result, fieldNumber);
  }
  
  /**
   * Returns the field number of the named field
   *
   * @return the field number (0-based) or -1 on error
   */
  public static int pg_field_num(Env env,
                                 @NotNull PostgresResult result,
                                 String fieldName)
  {
    try {
      if (result == null)
        return -1;

      return result.getColumnNumber(fieldName);
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * pg_field_num() alias.
   */
  public static int pg_fieldnum(Env env,
                                @NotNull PostgresResult result,
                                String fieldName)
  {
    return pg_field_num(env, result, fieldName);
  }
  
  /**
   * Returns the printed length
   */
  public static int pg_field_prtlen(Env env,
                                    @NotNull PostgresResult result,
                                    Value rowNumber,
                                    @Optional("-1") Value fieldNameOrNumber)
  {
    try {
      if (result == null)
        return -1;
      
      int row = rowNumber.toInt();

      if (fieldNameOrNumber.toString().equals("-1")) {
        fieldNameOrNumber = rowNumber;
        row = -1;
      }

      int fieldNumber = result.getColumnNumber(fieldNameOrNumber, 0);

      ResultSetMetaData metaData = result.getMetaData();
      String typeName = metaData.getColumnTypeName(fieldNumber + 1);
      if (typeName.equals("bool")) {
        return 1;
      }

      Value value = pg_fetch_result(env,
                                    result,
                                    LongValue.create(row),
                                    LongValue.create(fieldNumber));

      // Step the cursor back to the original position
      // See php/430p
      result.getResultSet().previous();

      int len = value.toString().length();

      // XXX: check this...
      // if (typeName.equals("money")) {
      //  len++;
      // }

      return len;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * pg_field_ptrlen() alias.
   */
  public static int pg_fieldprtlen(Env env,
                                   @NotNull PostgresResult result,
                                   Value rowNumber,
                                   @Optional("-1") Value fieldNameOrNumber)
  {
    return pg_field_prtlen(env, result, rowNumber, fieldNameOrNumber);
  }
  
  /**
   * Returns the internal storage size of the named field
   */
  @ReturnNullAsFalse
  public static LongValue pg_field_size(Env env,
                                        @NotNull PostgresResult result,
                                        int fieldNumber)
  {
    try {
      if (result == null)
        return LongValue.create(-1);

      ResultSetMetaData metaData = result.getMetaData();

      fieldNumber++;

      int dataType = metaData.getColumnType(fieldNumber);

      int size = -1;

      switch (dataType) {
      case Types.BIT:
        {
          String typeName = metaData.getColumnTypeName(fieldNumber);
          if (typeName.equals("bool")) {
            size = 1;
          }
          break;
        }

      case Types.TINYINT:
        size = 1;
        break;

      case Types.SMALLINT:
        size = 2;
        break;

      case Types.DATE:
      case Types.FLOAT:
      case Types.INTEGER:
      case Types.REAL:
        size = 4;
        break;

      case Types.BIGINT:
      case Types.DOUBLE:
        {
          size = 8;
          String typeName = metaData.getColumnTypeName(fieldNumber);
          if (typeName.equals("money")) {
            size = 4;
          }
        }
        break;

      case Types.TIME:
      case Types.TIMESTAMP:
        size = 8;
        // fall to specific cases

      default:
        {
          String typeName = metaData.getColumnTypeName(fieldNumber);
          if (typeName.equals("timetz")
              || typeName.equals("interval")) {
            size = 12;
          } else if (typeName.equals("macaddr")) {
            size = 6;
          } else if (typeName.equals("point")) {
            size = 16;
          } else if (typeName.equals("circle")) {
            size = 24;
          } else if (typeName.equals("box")
              || typeName.equals("lseg")) {
            size = 32;
          }
        }
      }

      return LongValue.create(size);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * pg_field_size() alias.
   */
  @ReturnNullAsFalse
  public static LongValue pg_fieldsize(Env env,
                                       @NotNull PostgresResult result,
                                       int fieldNumber)
  {
    return pg_field_size(env, result, fieldNumber);
  }
  
  /**
   * Returns the name or oid of the tables field
   *
   * @return By default the tables name that field belongs to
   * is returned but if oid_only is set to TRUE,
   * then the oid will instead be returned.
   */
  @ReturnNullAsFalse
  public static String pg_field_table(Env env,
                                      @NotNull PostgresResult result,
                                      int fieldNumber,
                                      @Optional("false") boolean oidOnly)
  {
    // The Postgres JDBC driver doesn't have a concept of exposing
    // to the client what table maps to a particular select item
    // in a result set, therefore the driver cannot report anything
    // useful to the caller. Thus the driver always returns "" to
    // ResultSetMetaData.getTableName(fieldNumber+1)

    env.stub("pg_field_table");

    return "";
  }

  /**
   * Returns the type ID (OID) for the corresponding field number
   */
  @ReturnNullAsFalse
  public static LongValue pg_field_type_oid(Env env,
                                            @NotNull PostgresResult result,
                                            int fieldNumber)
  {
    try {
      if (result == null)
        return null;

      ResultSetMetaData metaData = result.getMetaData();

      String columnTypeName = metaData.getColumnTypeName(fieldNumber + 1);

      String metaQuery =
          ("SELECT oid FROM pg_type WHERE typname='" + columnTypeName + "'");

      result = pg_query(env, (Postgres) result.getConnection(), metaQuery);

      Value value = pg_fetch_result(env,
                                    result,
                                    LongValue.MINUS_ONE,
                                    LongValue.ZERO);

      if (value.isLongConvertible()) {
        return LongValue.create(value.toLong());
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Returns the type name for the corresponding field number
   */
  @ReturnNullAsFalse
  public static StringValue pg_field_type(Env env,
                                          @NotNull PostgresResult result,
                                          int fieldNumber)
  {
    try {
      if (result == null)
        return null;

      ResultSetMetaData metaData = result.getMetaData();

      fieldNumber++;

      String typeName = metaData.getColumnTypeName(fieldNumber);

      return (StringValue) StringValue.create(typeName);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * pg_field_type() alias.
   */
  @ReturnNullAsFalse
  public static StringValue pg_fieldtype(Env env,
                                          @NotNull PostgresResult result,
                                          int fieldNumber)
  {
    return pg_field_type(env, result, fieldNumber);
  }
  
  /**
   * Free result memory
   */
  public static boolean pg_free_result(Env env,
                                       PostgresResult result)
  {
    try {
      if (result == null)
        return true;
      
      result.close();
      
      return true;
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      
      return true;
    }
  }

  /**
   * pg_free_result() alias.
   */
  public static boolean pg_freeresult(Env env,
                                      PostgresResult result)
  {
    if (result == null)
      return true;
    
    return pg_free_result(env, result);
  }
  
  /**
   * Gets SQL NOTIFY message
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_get_notify(Env env,
                                         @NotNull Postgres conn,
                                         @Optional("-1") int resultType)
  {
    try {
      if (conn == null)
        return null;
      
      if (resultType > 0) {
        throw new UnimplementedException("pg_get_notify with result type");
      }

      // org.postgresql.PGConnection
      Class cl = Class.forName("org.postgresql.PGConnection");

      // public PGNotification[] getNotifications() throws SQLException;
      Method method = cl.getDeclaredMethod("getNotifications", null);

      Connection pgconn = conn.getJavaConnection();

      // getNotifications()
      Object []notifications = (Object[]) method
          .invoke(pgconn, new Object[] {});

      // org.postgresql.PGNotification
      cl = Class.forName("org.postgresql.PGNotification");

      // public String getName();
      Method methodGetName = cl.getDeclaredMethod("getName", null);

      // public int getPID();
      Method methodGetPID = cl.getDeclaredMethod("getPID", null);

      ArrayValueImpl arrayValue = new ArrayValueImpl();

      int n = notifications.length;

      StringValue k;
      LongValue v;

      for (int i = 0; i < n; i++) {
        // getName()
        k = (StringValue) StringValue.create(
            methodGetName.invoke(notifications[i],
                new Object[]{}));
        // getPID()
        v = (LongValue) LongValue.create(
            (Integer) methodGetPID.invoke(notifications[i],
                new Object[]{}));

        arrayValue.put(k, v);
      }

      return arrayValue;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Gets the backend's process ID
   */
  public static int pg_get_pid(Env env,
                               @NotNull Postgres conn)
  {
    try {
      if (conn == null)
        return -1;
      
      // @todo create a random string
      String randomLabel = "caucho_pg_get_pid_random_label";

      pg_query(env, conn, "LISTEN " + randomLabel);
      pg_query(env, conn, "NOTIFY " + randomLabel);

      ArrayValue arrayValue = pg_get_notify(env, conn, -1);

      LongValue pid = (LongValue) arrayValue
          .get(StringValue.create(randomLabel));

      return pid.toInt();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * Get asynchronous query result
   */
  @ReturnNullAsFalse
  public static PostgresResult pg_get_result(Env env,
                                             @Optional Postgres conn)
  {
    // Three different scenarios for pg_get_result:
    //
    // 1. pg_send_prepare/pg_send_execute - php/431m
    //
    //    pg_send_prepare(
    // $conn, "my_query", 'SELECT * FROM test WHERE data = $1');
    //    $res1 = pg_get_result($conn);
    //
    //    pg_send_execute($conn, "my_query", array("Joe's Widgets"));
    //    $res2 = pg_get_result($conn);
    //
    //    pg_send_execute($conn, "my_query", array("Clothes Clothes Clothes"));
    //    $res3 = pg_get_result($conn);
    //
    // 2. Multiquery with pg_send_query - php/430y
    //
    //    pg_send_query(
    // $conn, "select * from test; select count(*) from test;");
    //
    //    // select * from test
    //    $res = pg_get_result($conn);
    //    $rows = pg_num_rows($res);
    //
    //    // select count(*) from test
    //    $res = pg_get_result($conn);
    //    $rows = pg_num_rows($res);
    //
    // 3. Individual pg_send_query - php/431g
    //
    //    $res = pg_send_query($conn, "select * from test;");
    //    var_dump($res);
    //    $res = pg_get_result($conn);
    //    var_dump($res);
    //
    //    $res = pg_send_query($conn, "select * from doesnotexist;");
    //    var_dump($res);
    //    $res = pg_get_result($conn);
    //    var_dump($res);

    try {
      if (conn == null)
        conn = getConnection(env);

      PostgresResult result = (PostgresResult) conn.getResultResource();

      // 1. pg_send_prepare/pg_send_execute
      if (conn.getAsynchronousStatement() != null) {
        if (conn.getAsynchronousResult() != null) {
          conn.setAsynchronousResult(null);
          return result;
        }
        return null;
      }

      // 2. pg_send_query
      if (conn.getAsynchronousResult() != null) {

        // Check for next result
        // Ex: pg_send_query(
        // $conn, "select * from test; select count(*) from test;");

        Statement stmt = result.getJavaStatement();

        if (stmt.getMoreResults()) {
          result = (PostgresResult) conn.createResult(env, stmt,
                                                      stmt.getResultSet());
        } else {
          // 3. Individual pg_send_query (clean up; no futher results)
          conn.setResultResource(null);
        }
      }

      conn.setAsynchronousResult(result);

      return result;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns the host name associated with the connection
   */
  @ReturnNullAsFalse
  public static String pg_host(Env env,
                               @Optional Postgres conn)
  {
    try {
      if (conn == null)
        conn = getConnection(env);

      return conn.getHost();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Insert array into table
   */
  public static boolean pg_insert(Env env,
                                  @NotNull Postgres conn,
                                  String tableName,
                                  ArrayValue assocArray,
                                  @Optional("-1") int options)
  {
    try {
      if (conn == null)
        return false;

      if (options > 0) {
        throw new UnimplementedException("pg_insert with options");
      }

      StringBuilder names = new StringBuilder();
      StringBuilder values = new StringBuilder();

      boolean isFirst = true;

      for (Map.Entry<Value,Value> entry : assocArray.entrySet()) {
        Value k = entry.getKey();
        Value v = entry.getValue();
        if (isFirst) {
          isFirst = false;
        } else {
          values.append("','");
          names.append(",");
        }
        values.append(v.toString());
        names.append(k.toString());
      }

      StringBuilder query = new StringBuilder();
      query.append("INSERT INTO ");
      query.append(tableName);
      query.append("(");
      query.append(names);
      query.append(") VALUES('");
      query.append(values);
      query.append("')");

      pg_query(env, conn, query.toString());

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Get the last error message string of a connection
   */
  @ReturnNullAsFalse
  public static StringValue pg_last_error(Env env,
                                          @Optional Postgres conn)
  {
    try {

      if (conn == null)
        conn = getConnection(env);

      return conn.error(env);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }
  
  /**
   * pg_last_error() alias.
   */
  @ReturnNullAsFalse
  public static StringValue pg_errormessage(Env env,
                                       @Optional Postgres conn)
  {
    return pg_last_error(env, conn);
  }

  /**
   * Returns the last notice message from PostgreSQL server
   */
  @ReturnNullAsFalse
  public static String pg_last_notice(Env env,
                                      @NotNull Postgres conn)
  {
    try {
      if (conn == null)
        return null;
      
      SQLWarning warning = conn.getWarnings();
      
      if (warning != null)
        return warning.toString();
      else
        return null;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Returns the last row's OID
   *
   * Note that:
   * - OID is a unique id. It will not work if the table was
   *   created with "No oid".
   * - MySql's "mysql_insert_id" receives the conection handler as argument but
   * PostgreSQL's "pg_last_oid" uses the result handler.
   */
  @ReturnNullAsFalse
  public static String pg_last_oid(Env env,
                                   PostgresResult result)
  {
    try {

      Statement stmt = result.getJavaStatement();

      Class cl = Class.forName("org.postgresql.jdbc2.AbstractJdbc2Statement");

      Method method = cl.getDeclaredMethod("getLastOID", null);

      int oid = Integer.parseInt(
          method.invoke(stmt, new Object[] {}).toString());

      if (oid > 0)
        return "" + oid;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  @ReturnNullAsFalse
  public static String pg_getlastoid(Env env,
                                   PostgresResult result)
  {
    return pg_last_oid(env, result);
  }
  
  /**
   * Close a large object
   */
  public static boolean pg_lo_close(Env env,
                                    Object largeObject)
  {
    try {

      Class cl = Class.forName("org.postgresql.largeobject.LargeObject");

      Method method = cl.getDeclaredMethod("close", null);

      method.invoke(largeObject, new Object[] {});
      // largeObject.close();

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }
  
  /**
   * pg_lo_close() alias.
   */
  public static boolean pg_loclose(Env env,
                                   Object largeObject)
  {
    return pg_lo_close(env, largeObject);
  }

  /**
   * Create a large object
   */
  @ReturnNullAsFalse
  public static LongValue pg_lo_create(Env env,
                                       @Optional Postgres conn)
  {
    try {

      int oid = -1;

      if (conn == null)
        conn = getConnection(env);

      // LargeObjectManager lobManager;
      Object lobManager;

      // org.postgresql.PGConnection
      Class cl = Class.forName("org.postgresql.PGConnection");

      Method method = cl.getDeclaredMethod("getLargeObjectAPI", null);

      Connection pgconn = conn.getJavaConnection();

      // Large Objects may not be used in auto-commit mode.
      pgconn.setAutoCommit(false);

      lobManager = method.invoke(pgconn, new Object[] {});
      // lobManager = ((org.postgresql.PGConnection)conn).getLargeObjectAPI();

      // org.postgresql.largeobject.LargeObjectManager
      cl = Class.forName("org.postgresql.largeobject.LargeObjectManager");

      method = cl.getDeclaredMethod("create", null);

      Object oidObj = method.invoke(lobManager, new Object[] {});

      oid = Integer.parseInt(oidObj.toString());

      // oid = lobManager.create();

      return LongValue.create(oid);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * pg_lo_create() alias
   */
  @ReturnNullAsFalse
  public static LongValue pg_locreate(Env env,
                                      @Optional Postgres conn)
  {
    return pg_lo_create(env, conn);
  }

  /**
   * Export a large object to a file
   */
  public static boolean pg_lo_export(Env env,
                                     @NotNull Postgres conn,
                                     int oid,
                                     Path path)
  {
    try {
      if (conn == null)
        return false;
      
      //@todo conn should be optional

      // LargeObjectManager lobManager;
      Object lobManager;

      //org.postgresql.largeobject.LargeObjectManager

      Class cl = Class.forName("org.postgresql.PGConnection");

      Method method = cl.getDeclaredMethod("getLargeObjectAPI", null);

      Connection pgconn = conn.getJavaConnection();

      lobManager = method.invoke(pgconn, new Object[] {});
      // lobManager = ((org.postgresql.PGConnection)conn).getLargeObjectAPI();

      cl = Class.forName("org.postgresql.largeobject.LargeObjectManager");

      method = cl.getDeclaredMethod("open", new Class[] {Integer.TYPE});

      Object lobj = method.invoke(lobManager, new Object[] {oid});

      cl = Class.forName("org.postgresql.largeobject.LargeObject");

      method = cl.getDeclaredMethod("getInputStream", null);

      Object isObj = method.invoke(lobj, new Object[] {});

      InputStream is = (InputStream)isObj;

      // Open the file
      WriteStream os = path.openWrite();

      // copy the data from the large object to the file
      os.writeStream(is);

      os.close();
      is.close();

      // Close the large object
      method = cl.getDeclaredMethod("close", null);

      method.invoke(lobj, new Object[] {});

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * pg_lo_export() alias.
   */
  public static boolean pg_loexport(Env env,
                                     @NotNull Postgres conn,
                                     int oid,
                                     Path path)
  {
    return pg_lo_export(env, conn, oid, path);
  }
  
  /**
   * Import a large object from file
   */
  @ReturnNullAsFalse
  public static LongValue pg_lo_import(Env env,
                                       @NotNull Postgres conn,
                                       Path path)
  {
    try {
      if (conn == null)
        return null;

      //@todo conn should be optional

      LongValue value = pg_lo_create(env, conn);

      if (value != null) {

        int oid = value.toInt();
        Object largeObject = pg_lo_open(env, conn, oid, "w");

        String data = "";

        // Open the file
        ReadStream is = path.openRead();

        writeLobInternal(largeObject, is, Integer.MAX_VALUE);

        pg_lo_close(env, largeObject);

        is.close();

        return LongValue.create(oid);
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * pg_lo_import() alias.
   */
  @ReturnNullAsFalse
  public static LongValue pg_loimport(Env env,
                                      @NotNull Postgres conn,
                                      Path path)
  {
    return pg_lo_import(env, conn, path);
  }
  
  /**
   * Open a large object
   */
  @ReturnNullAsFalse
  public static Object pg_lo_open(Env env,
                                  @NotNull Postgres conn,
                                  int oid,
                                  String mode)
  {
    try {
      if (conn == null)
        return null;

      Object largeObject = null;

      // LargeObjectManager lobManager;
      Object lobManager;

      //org.postgresql.largeobject.LargeObjectManager

      Class cl = Class.forName("org.postgresql.PGConnection");

      Method method = cl.getDeclaredMethod("getLargeObjectAPI", null);

      Connection pgconn = conn.getJavaConnection();

      lobManager = method.invoke(pgconn, new Object[] {});

      cl = Class.forName("org.postgresql.largeobject.LargeObjectManager");

      method = cl.getDeclaredMethod("open",
          new Class[] {Integer.TYPE, Integer.TYPE});

      boolean write = mode.indexOf("w") >= 0;
      boolean read = mode.indexOf("r") >= 0;

      int modeREAD = cl.getDeclaredField("READ").getInt(null);
      int modeREADWRITE = cl.getDeclaredField("READWRITE").getInt(null);
      int modeWRITE = cl.getDeclaredField("WRITE").getInt(null);

      int intMode = modeREAD;

      if (read) {
        if (write) {
          intMode = modeREADWRITE;
        }
      } else if (write) {
        intMode = modeWRITE;
      }

      largeObject = method.invoke(lobManager, new Object[] {oid, intMode});

      return largeObject;

    } catch (Exception ex) {
      env.warning(L.l("Unable to open PostgreSQL large object"));
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * pg_lo_open() alias.
   */
  @ReturnNullAsFalse
  public static Object pg_loopen(Env env,
                                 @NotNull Postgres conn,
                                 int oid,
                                 String mode)
  {
    return pg_lo_open(env, conn, oid, mode);
  }
  
  /**
   * Reads an entire large object and send straight to browser
   */
  @ReturnNullAsFalse
  public static LongValue pg_lo_read_all(Env env,
                                         Object largeObject)
  {
    try {
      StringValue contents = pg_lo_read(env, largeObject, -1);
      
      if (contents != null) {
        env.getOut().print(contents);
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * pg_lo_read_all() alias.
   */
  @ReturnNullAsFalse
  public static LongValue pg_loreadall(Env env,
                                       Object largeObject)
  {
    return pg_lo_read_all(env, largeObject);
  }
  
  /**
   * Read a large object
   */
  @ReturnNullAsFalse
  public static StringValue pg_lo_read(Env env,
                                       Object largeObject,
                                       @Optional("-1") int len)
  {
    try {

      if (len < 0) {
        len = Integer.MAX_VALUE;
      }

      Class cl = Class.forName("org.postgresql.largeobject.LargeObject");

      Method method = cl.getDeclaredMethod("getInputStream", null);

      InputStream is = (InputStream) method
          .invoke(largeObject, new Object[] {});

      try {
        StringValue bb = env.createBinaryBuilder();

        bb.appendReadAll(is, len);

        return bb;
      } finally {
        is.close();
      }
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * pg_lo_read() alias.
   */
  @ReturnNullAsFalse
  public static StringValue pg_loread(Env env,
                                      Object largeObject,
                                      @Optional("-1") int len)
  {
    return pg_lo_read(env, largeObject, len);
  }
  
  /**
   * Seeks position within a large object
   */
  public static boolean pg_lo_seek(Env env,
                                   Object largeObject,
                                   int offset,
                                   @Optional int whence)
  {
    try {

      Class cl = Class.forName("org.postgresql.largeobject.LargeObject");

      int seekSET = cl.getDeclaredField("SEEK_SET").getInt(null);
      int seekEND = cl.getDeclaredField("SEEK_END").getInt(null);
      int seekCUR = cl.getDeclaredField("SEEK_CUR").getInt(null);

      switch (whence) {
      case PGSQL_SEEK_SET:
        whence = seekSET;
        break;
      case PGSQL_SEEK_END:
        whence = seekEND;
        break;
      default:
        whence = seekCUR;
      }

      Method method = cl.getDeclaredMethod(
          "seek", new Class[]{Integer.TYPE,Integer.TYPE});

      method.invoke(largeObject, new Object[] {offset, whence});

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Returns current seek position a of large object
   */
  public static int pg_lo_tell(Env env,
                               Object largeObject)
  {
    try {

      Class cl = Class.forName("org.postgresql.largeobject.LargeObject");

      Method method = cl.getDeclaredMethod("tell", null);

      Object obj = method.invoke(largeObject, new Object[] {});

      return Integer.parseInt(obj.toString());

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * Delete a large object
   */
  public static boolean pg_lo_unlink(Env env,
                                     @NotNull Postgres conn,
                                     int oid)
  {
    try {
      if (conn == null)
        return false;

      // LargeObjectManager lobManager;
      Object lobManager;

      //org.postgresql.largeobject.LargeObjectManager

      Class cl = Class.forName("org.postgresql.PGConnection");

      Method method = cl.getDeclaredMethod("getLargeObjectAPI", null);

      Connection pgconn = conn.getJavaConnection();

      lobManager = method.invoke(pgconn, new Object[] {});

      cl = Class.forName("org.postgresql.largeobject.LargeObjectManager");

      method = cl.getDeclaredMethod("unlink", new Class[] {Integer.TYPE});

      method.invoke(lobManager, new Object[] {oid});

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * pg_lo_unlink() alias.
   */
  public static boolean pg_lounlink(Env env,
                                    @NotNull Postgres conn,
                                    int oid)
  {
    return pg_lo_unlink(env, conn, oid);
  }
  
  /**
   * Write to a large object
   */
  @ReturnNullAsFalse
  public static LongValue pg_lo_write(Env env,
                                      @NotNull Object largeObject,
                                      String data,
                                      @Optional int len)
  {
    try {
      if (largeObject == null)
        return null;

      if (len <= 0) {
        len = data.length();
      }

      int written = len;

      Class cl = Class.forName("org.postgresql.largeobject.LargeObject");

      Method method = cl.getDeclaredMethod("write",
                                           new Class[] {byte[].class,
                                                        Integer.TYPE,
                                                        Integer.TYPE});

      method.invoke(largeObject, new Object[] {data.getBytes(), 0, len});

      return LongValue.create(written);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * pg_lo_write() alias.
   */
  @ReturnNullAsFalse
  public static LongValue pg_lowrite(Env env,
                                     @NotNull Object largeObject,
                                     String data,
                                     @Optional int len)
  {
    return pg_lo_write(env, largeObject, data, len);
  }
  
  /**
   * Get meta data for table
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_meta_data(Env env,
                                        @NotNull Postgres conn,
                                        String tableName)
  {
    env.stub("pg_meta_data");

    return null;
  }

  /**
   * Returns the number of fields in a result
   */
  public static int pg_num_fields(Env env,
                                  @NotNull PostgresResult result)
  {
    try {
      return result.getFieldCount();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * pg_num_fields() alias.
   */
  public static int pg_numfields(Env env,
                                 @NotNull PostgresResult result)
  {
    return pg_num_fields(env, result);
  }
  
  /**
   * Returns the number of rows in a result
   */
  public static LongValue pg_num_rows(Env env,
                                      @NotNull PostgresResult result)
  {
    int numRows = -1;

    try {
      if (result == null)
        return LongValue.create(-1);

      if ((result != null) && (result.getResultSet() != null)) {
        numRows = result.getNumRows();
      }

      if (numRows < 0) {
        env.warning(L.l(
            "supplied argument is not a valid PostgreSQL result resource"));
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return LongValue.create(numRows);
  }

  /**
   * pg_num_rows() alias.
   */
  public static LongValue pg_numrows(Env env,
                                     @NotNull PostgresResult result)
  {
    return pg_num_rows(env, result);
  }
  
  /**
   * Get the options associated with the connection
   */
  public static String pg_options(Env env,
                                  @Optional Postgres conn)
  {
    throw new UnimplementedException("pg_options");
  }

  /**
   * Looks up a current parameter setting of the server
   */
  public static Value pg_parameter_status(Env env,
                                          @NotNull Postgres conn,
                                          @NotNull StringValue paramName)
  {
    try {
      if (conn == null || paramName == null)
        return BooleanValue.FALSE;

      PostgresResult result = pg_query(env, conn, "SHOW " + paramName);

      Value value = pg_fetch_result(
          env, result, LongValue.ZERO, LongValue.ZERO);

      if (value == null || value.isNull())
        return BooleanValue.FALSE;

      if (paramName.toString().equals("server_encoding")) {
        if (value.equals(env.createString("UNICODE")))
          value = env.createString("UTF8");
      }

      return value;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Looks up a current parameter setting of the server
   */
  public static Value pg_parameter_status(Env env,
                                          @NotNull StringValue paramName)
  {
    Postgres conn = getConnection(env);
    
    return pg_parameter_status(env, conn, paramName);
  }

  /**
   * Open a persistent PostgreSQL connection
   */
  @ReturnNullAsFalse
  public static Postgres pg_pconnect(Env env,
                                     String connectionString,
                                     @Optional int connectType)
  {
    return pg_connect(env, connectionString, connectType);
  }

  /**
   * Ping database connection
   */
  public static boolean pg_ping(Env env,
                                @Optional Postgres conn)
  {
    try {

      if (conn == null)
        conn = getConnection(env);

      return pg_query(env, conn, "SELECT 1") != null;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Return the port number associated with the connection
   */
  @ReturnNullAsFalse
  public static StringValue pg_port(Env env,
                                    @Optional Postgres conn)
  {
    try {

      if (conn == null)
        conn = getConnection(env);

      // In PHP, a real pg_port test case returns String

      return (StringValue) StringValue.create(conn.getPort());

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Submits a request to create a prepared statement with the given parameters,
   * and waits for completion
   */
  @ReturnNullAsFalse
  public static PostgresStatement pg_prepare(Env env,
                                             @NotNull Postgres conn,
                                             String stmtName,
                                             String query)
  {
    try {
      if (conn == null)
        return null;

      PostgresStatement pstmt = conn.prepare(env, env.createString(query));
      conn.putStatement(stmtName, pstmt);
      
      return pstmt;
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Send a NULL-terminated string to PostgreSQL backend
   */
  public static boolean pg_put_line(Env env,
                                    @NotNull Postgres conn,
                                    String data)
  {
    try {
      if (conn == null)
        return false;

      Class cl = Class.forName("org.postgresql.core.PGStream");

      Constructor constructor = cl.getDeclaredConstructor(new Class[] {
        String.class, Integer.TYPE});

      Object object = constructor.newInstance(
          new Object[] {conn.getHost(), conn.getPort()});

      byte []dataArray = data.getBytes();

      Method method = cl.getDeclaredMethod("Send", new Class[] {byte[].class});

      method.invoke(object, new Object[] {dataArray});

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }

  }

  /**
   * Submits a command to the server and waits for the result,
   * with the ability to pass parameters separately from the SQL command text
   */
  @ReturnNullAsFalse
  public static PostgresResult pg_query_params(Env env,
                                               @NotNull Postgres conn,
                                               String query,
                                               ArrayValue params)
  {
    try {
      if (conn == null)
        return null;
      
      if (pg_send_query_params(env, conn, query, params)) {
        return (PostgresResult) conn.getResultResource();
      }

      return null;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Execute a query
   */
  @ReturnNullAsFalse
  public static PostgresResult pg_query(Env env, @NotNull String query)
  {
    if (query == null)
      return null;
    
    return pg_query_impl(env, getConnection(env), query, true);
  }

  /**
   * Execute a query
   */
  @ReturnNullAsFalse
  public static PostgresResult pg_query(Env env,
                                        @NotNull Postgres conn,
                                        @NotNull String query)
  {
    if (conn == null || query == null)
      return null;

    return pg_query_impl(env, conn, query, true);
  }

 
  /**
   * pg_query() alias
   */
  @ReturnNullAsFalse
  public static PostgresResult pg_exec(Env env,
                                       @NotNull Postgres conn,
                                       String query)
  {
    if (conn == null)
      return null;
    
    return pg_query(env, conn, query);
  }
  
  /**
   * Execute a query
   */
  private static PostgresResult pg_query_impl(Env env,
                                              Postgres conn,
                                              String query,
                                              boolean reportError)
  {
    try {
      // XXX: the PHP api allows conn to be optional but we
      // totally disallow this case.

      if (conn == null)
        conn = getConnection(env);

      PostgresResult result = conn.query(env, query);

      StringValue error = conn.error(env);

      if (error.length() != 0) {
        if (reportError)
          env.warning(L.l("Query failed: {0}", error));

        return null;
      }

      return result;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Returns an individual field of an error report
   */
  public static Value pg_result_error_field(Env env,
                                            @NotNull PostgresResult result,
                                            int fieldCode)
  {
    try {
      if (result == null)
        return BooleanValue.FALSE;

      Object errorField = null;

      // Get the postgres specific server error message
      // org.postgresql.util.ServerErrorMessage
      Object serverError = ((Postgres) result
          .getConnection()).getServerErrorMessage();

      if (serverError != null) {
        Class cl = Class.forName("org.postgresql.util.ServerErrorMessage");

        String methodName;

        switch (fieldCode) {
        case PGSQL_DIAG_SEVERITY:
          methodName = "getSeverity";
          break;
        case PGSQL_DIAG_SQLSTATE:
          methodName = "getSQLState";
          break;
        case PGSQL_DIAG_MESSAGE_PRIMARY:
          methodName = "getMessage";
          break;
        case PGSQL_DIAG_MESSAGE_DETAIL:
          methodName = "getDetail";
          break;
        case PGSQL_DIAG_MESSAGE_HINT:
          methodName = "getHint";
          break;
        case PGSQL_DIAG_STATEMENT_POSITION:
          methodName = "getPosition";
          break;
        case PGSQL_DIAG_INTERNAL_POSITION:
          methodName = "getInternalPosition";
          break;
        case PGSQL_DIAG_INTERNAL_QUERY:
          methodName = "getInternalQuery";
          break;
        case PGSQL_DIAG_CONTEXT:
          methodName = "getWhere";
          break;
        case PGSQL_DIAG_SOURCE_FILE:
          methodName = "getFile";
          break;
        case PGSQL_DIAG_SOURCE_LINE:
          methodName = "getLine";
          break;
        case PGSQL_DIAG_SOURCE_FUNCTION:
          methodName = "getRoutine";
          break;
        default:
          return null;
        }

        Method method = cl.getDeclaredMethod(methodName, null);
        errorField = method.invoke(serverError, new Object[] {});
      }

      if (errorField == null) {
        /* XXX: php/431g
        if (fieldCode == PGSQL_DIAG_INTERNAL_QUERY)
          return BooleanValue.FALSE;
        */

        return NullValue.NULL;
      }

      if (fieldCode == PGSQL_DIAG_STATEMENT_POSITION) {

        Integer position = (Integer) errorField;

        if (position.intValue() == 0)
          return NullValue.NULL;
      }

      if (fieldCode == PGSQL_DIAG_INTERNAL_POSITION) {

        Integer position = (Integer) errorField;

        if (position.intValue() == 0)
          // XXX: php/431g return BooleanValue.FALSE;
          return NullValue.NULL;
      }

      return StringValue.create(errorField.toString());

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return NullValue.NULL;
    }
  }

  /**
   * Get error message associated with result
   */
  @ReturnNullAsFalse
  public static String pg_result_error(Env env,
                                       @Optional PostgresResult result)
  {
    try {
      if (result != null)
        return result.getConnection().getErrorMessage();
      else
        return null;
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Set internal row offset in result resource
   */
  public static boolean pg_result_seek(Env env,
                                       @NotNull PostgresResult result,
                                       int offset)
  {
    try {
      if (result == null)
        return false;

      return result.seek(env, offset);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Get status of query result
   */
  public static int pg_result_status(Env env,
                                     @NotNull PostgresResult result,
                                     @Optional("PGSQL_STATUS_LONG") int type)
  {
    try {
      if (result == null)
        return -1;

      if (type == PGSQL_STATUS_STRING) {
        throw new UnimplementedException(
            "pg_result_status with PGSQL_STATUS_STRING");
      }

      if (result != null) {
        Statement stmt = result.getStatement();
        if (stmt.getUpdateCount() >= 0) {
          return PGSQL_COMMAND_OK;
        }

        ResultSet rs = result.getResultSet();
        if (rs == null) {
          return PGSQL_EMPTY_QUERY;
        }

        return PGSQL_TUPLES_OK;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return -1;
  }

  /**
   * Select records
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_select(Env env,
                                     @NotNull Postgres conn,
                                     String tableName,
                                     ArrayValue assocArray,
                                     @Optional("-1") int options)
  {
    try {
      if (conn == null)
        return null;

      StringValue whereClause = env.createUnicodeBuilder();

      boolean isFirst = true;

      for (Map.Entry<Value,Value> entry : assocArray.entrySet()) {
        Value k = entry.getKey();
        Value v = entry.getValue();
        if (isFirst) {
          isFirst = false;
        } else {
          whereClause.append(" AND ");
        }
        whereClause.append(k.toString()).append("='")
            .append(v.toString()).append("'");
        // String pi = conn.realEscapeString(p).toString();
        // pi = pi.replaceAll("\\\\", "\\\\\\\\");
      }

      StringValue query = env.createUnicodeBuilder();
      query.append("SELECT * FROM ").append(tableName)
          .append(" WHERE ").append(whereClause);

      PostgresResult result = pg_query(env, conn, query.toString());

      return pg_fetch_all(env, result);

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Sends a request to execute a prepared statement with given parameters,
   * without waiting for the result(s)
   */
  public static boolean pg_send_execute(Env env,
                                        @NotNull Postgres conn,
                                        String stmtName,
                                        ArrayValue params)
  {
    try {

      // Note: for now, this is essentially the same as pg_execute.

      PostgresResult result = pg_execute(env, conn, stmtName, params);

      conn.setAsynchronousResult(result);

      if (result != null) {
        return true;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return false;
  }

  /**
   * Sends a request to create a prepared statement with the given parameters,
   * without waiting for completion
   */
  public static boolean pg_send_prepare(Env env,
                                        @NotNull Postgres conn,
                                        String stmtName,
                                        String query)
  {
    try {

      // Note: for now, this is the same as pg_prepare.

      PostgresStatement stmt = pg_prepare(env, conn, stmtName, query);

      conn.setAsynchronousStatement(stmt);

      if (stmt != null) {
        return true;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return false;
  }

  /**
   * Submits a command and separate parameters to the server
   * without waiting for the result(s)
   */
  public static boolean pg_send_query_params(Env env,
                                             @NotNull Postgres conn,
                                             String query,
                                             ArrayValue params)
  {
    try {

      PostgresStatement pstmt = conn.prepare(env, env.createString(query));

      return executeInternal(env, conn, pstmt, params) != null;

      /*
      ArrayValueImpl arrayImpl = (ArrayValueImpl)params;
      int sz = arrayImpl.size();

      for (int i=0; i<sz; i++) {
        StringValue p = arrayImpl.get(LongValue.create(i)).toStringValue();
        String pi = conn.realEscapeString(p).toString();
        pi = pi.replaceAll("\\\\", "\\\\\\\\");
        query = query.replaceAll("\\$"+(i+1), "\\'"+pi+"\\'");
      }

      pg_send_query(env, conn, query);

      return true;
      */

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Sends asynchronous query
   */
  public static boolean pg_send_query(Env env,
                                      @NotNull Postgres conn,
                                      String query)
  {
    if (conn == null)
      return false;

    try {
      PostgresResult result = pg_query_impl(env, conn, query, false);

      // We probably won't need this for now. See pg_get_result().
      // conn.setAsynchronousResult(result);

      // php/431g
      // This is to be compliant with real expected results.
      // Even a SELECT * FROM doesnotexist returns true from pg_send_query.
      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return false;
  }

  /**
   * Set the client encoding
   */
  public static int pg_set_client_encoding(Env env,
                                           @NotNull Postgres conn,
                                           String encoding)
  {
    //@todo conn should be optional
    if (conn == null)
      conn = getConnection(env);

    if (conn.setClientEncoding(encoding))
      return 0;
    else
      return -1;
  }

  /**
   * Determines the verbosity of messages returned
   * by pg_last_error() and pg_result_error()
   */
  public static int pg_set_error_verbosity(Env env,
                                           @NotNull Postgres conn,
                                           int intVerbosity)
  {
    try {

      //@todo conn should be optional

      String verbosity;

      PostgresResult result = pg_query(env, conn, "SHOW log_error_verbosity");

      ArrayValue arr = pg_fetch_row(env, result, LongValue.ZERO);

      String prevVerbosity = arr.get(LongValue.ZERO).toString();

      switch (intVerbosity) {
      case PGSQL_ERRORS_TERSE:
        verbosity = "TERSE";
        break;
      case PGSQL_ERRORS_VERBOSE:
        verbosity = "VERBOSE";
        break;
      default:
        verbosity = "DEFAULT";
      }

      pg_query(env, conn, "SET log_error_verbosity TO '" + verbosity + "'");

      if (prevVerbosity.equals("TERSE")) {
        return PGSQL_ERRORS_TERSE;
      } else if (prevVerbosity.equals("VERBOSE")) {
        return PGSQL_ERRORS_VERBOSE;
      } else {
        return PGSQL_ERRORS_DEFAULT;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }

  /**
   * Enable tracing a PostgreSQL connection
   */
  public static boolean pg_trace(Env env,
                                 Path path,
                                 @Optional String mode,
                                 @Optional Postgres conn)
  {
    env.stub("pg_trace");

    return false;
  }

  /**
   * Returns the current in-transaction status of the server
   */
  public static int pg_transaction_status(Env env,
                                          @Optional Postgres conn)
  {
    return PGSQL_TRANSACTION_IDLE;
  }

  /**
   * Return the TTY name associated with the connection
   */
  public static String pg_tty(Env env,
                              @Optional Postgres conn)
  {
    // Note:  pg_tty() is obsolete, since the server no longer pays attention to
    // the TTY setting, but the function remains for backwards compatibility.

    env.stub("pg_tty");

    return "";
  }

  /**
   * Unescape binary for bytea type
   */
  @ReturnNullAsFalse
  public static String pg_unescape_bytea(Env env,
                                         String data)
  {
    try {

      byte []dataBytes = data.getBytes();

      Class cl = Class.forName("org.postgresql.util.PGbytea");

      Method method = cl.getDeclaredMethod(
          "toBytes", new Class[] {byte[].class});

      return new String((byte[]) method.invoke(cl, new Object[] {dataBytes}));

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Disable tracing of a PostgreSQL connection
   */
  public static boolean pg_untrace(Env env,
                                   @Optional Postgres conn)
  {
    // Always returns TRUE

    env.stub("pg_untrace");

    return true;
  }

  /**
   * Update table
   */
  public static boolean pg_update(Env env,
                                  @NotNull Postgres conn,
                                  String tableName,
                                  ArrayValue data,
                                  ArrayValue condition,
                                  @Optional int options)
  {
    // From php.net: This function is EXPERIMENTAL.

    // The behaviour of this function, the name of this function, and
    // anything else documented about this function may change without
    // notice in a future release of PHP. Use this function at your own risk.

    try {

      if (options > 0) {
        throw new UnimplementedException("pg_update with options");
      }

      StringBuilder values = new StringBuilder();

      boolean isFirst = true;

      for (Map.Entry<Value,Value> entry : data.entrySet()) {
        Value k = entry.getKey();
        Value v = entry.getValue();
        if (isFirst) {
          isFirst = false;
        } else {
          values.append(", ");
        }
        values.append(k.toString());
        values.append("='");
        values.append(v.toString());
        values.append("'");
      }

      StringBuilder whereClause = new StringBuilder();

      isFirst = true;

      for (Map.Entry<Value,Value> entry : condition.entrySet()) {
        Value k = entry.getKey();
        Value v = entry.getValue();
        if (isFirst) {
          isFirst = false;
        } else {
          whereClause.append(" AND ");
        }
        whereClause.append(k.toString());
        whereClause.append("='");
        whereClause.append(v.toString());
        whereClause.append("'");
      }

      StringBuilder query = new StringBuilder();
      query.append("UPDATE ");
      query.append(tableName);
      query.append(" SET ");
      query.append(values);
      query.append(" WHERE ");
      query.append(whereClause);

      pg_query(env, conn, query.toString());

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Returns an array with client, protocol and server version (when available)
   */
  @ReturnNullAsFalse
  public static ArrayValue pg_version(Env env,
                                      @Optional Postgres conn)
  {
    try {

      //@todo return an array

      if (conn == null)
        conn = (Postgres) env.getSpecialValue("caucho.postgres");

      ArrayValue result = new ArrayValueImpl();

      result.append(env.createString("client"),
                    env.createString(conn.getClientInfo()));
      result.append(env.createString("server_version"),
                    env.createString(conn.getServerInfo()));
      
      return result;
    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  private static Postgres getConnection(Env env)
  {
    Postgres conn = (Postgres) env.getSpecialValue("caucho.postgres");

    if (conn != null)
      return conn;

    String driver = "org.postgresql.Driver";
    String url = "jdbc:postgresql://localhost:5432/";

    conn = new Postgres(env, "localhost", "", "", "", 5432, driver, url);

    env.setSpecialValue("caucho.postgres", conn);

    return conn;
  }

  private static PostgresResult executeInternal(Env env,
                                                @NotNull Postgres conn,
                                                PostgresStatement pstmt,
                                                ArrayValue params)
  {
    try {

      StringBuilder stringBuilder = new StringBuilder();

      int size = params.getSize(); // pstmt.getPreparedMappingSize();

      for (int i = 0; i < size; i++)
        stringBuilder.append('s');

      String types = stringBuilder.toString();

      Value []value = params.valuesToArray();
      pstmt.bindParams(env, types, value);

      if (!pstmt.execute(env))
        return null;

      if (pstmt.getStatementType().equals("SELECT")) {
        PostgresResult result = new PostgresResult(
            env, null, pstmt.getResultSet(), null);
        conn.setResultResource(result);
        return result;
      } else {
        // XXX: ??? return type?
        return null;
        // return pstmt;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  private static int writeLobInternal(Object largeObject,
                                      InputStream is,
                                      int len)
  {
    try {

      Class cl = Class.forName("org.postgresql.largeobject.LargeObject");

      Method method = cl.getDeclaredMethod("getOutputStream", null);

      OutputStream os = (OutputStream) method.invoke(
          largeObject, new Object[] {});

      int written = 0;

      int b;

      while (((b = is.read()) >= 0) && (written++ < len)) {
        os.write(b);
      }

      os.close();

      return written;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return -1;
    }
  }
}
