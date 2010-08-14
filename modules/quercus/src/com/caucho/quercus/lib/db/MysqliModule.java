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

import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Quercus mysql routines.
 */
public class MysqliModule extends AbstractQuercusModule {
  private static final Logger log = Log.open(MysqliModule.class);
  private static final L10N L = new L10N(MysqliModule.class);

  public static final int MYSQLI_ASSOC = JdbcResultResource.FETCH_ASSOC;
  public static final int MYSQLI_NUM = JdbcResultResource.FETCH_NUM;
  public static final int MYSQLI_BOTH = JdbcResultResource.FETCH_BOTH;

  public static final int MYSQLI_USE_RESULT = 0x0;
  public static final int MYSQLI_STORE_RESULT = 0x1;

  // Used by mysqli_fetch_field.
  public static final int NOT_NULL_FLAG = 0x1;
  public static final int PRI_KEY_FLAG = 0x2;
  public static final int UNIQUE_KEY_FLAG = 0x4;
  public static final int MULTIPLE_KEY_FLAG = 0x8;
  public static final int BLOB_FLAG = 0x10;
  public static final int UNSIGNED_FLAG = 0x20;
  public static final int ZEROFILL_FLAG = 0x40;
  public static final int BINARY_FLAG = 0x80;

  // Sent to new clients
  public static final int ENUM_FLAG = 0x100;
  public static final int AUTO_INCREMENT_FLAG = 0x200;
  public static final int TIMESTAMP_FLAG = 0x400;
  public static final int SET_FLAG = 0x800;
  public static final int NUM_FLAG = 0x8000;
  public static final int PART_KEY_FLAG = 0x4000; //Intern: Part of some key???
  public static final int GROUP_FLAG = 0x8000;    //Intern: Group field???
  public static final int UNIQUE_FLAG = 0x10000;   //Intern: Used by sql_yacc???
  public static final int BINCMP_FLAG = 0x20000;  //Intern: Used by sql_yacc???

  // The following are numerical respresentations
  // of types returned by mysqli_fetch_field.
  // These are defined in mysql's mysql_com.h header.

  public static final int MYSQLI_TYPE_DECIMAL = 0x0;
  public static final int MYSQLI_TYPE_TINY = 0x1;
  public static final int MYSQLI_TYPE_SHORT = 0x2;
  public static final int MYSQLI_TYPE_LONG = 0x3;
  public static final int MYSQLI_TYPE_FLOAT = 0x4;
  public static final int MYSQLI_TYPE_DOUBLE = 0x5;
  public static final int MYSQLI_TYPE_NULL = 0x6;
  public static final int MYSQLI_TYPE_TIMESTAMP = 0x7;
  public static final int MYSQLI_TYPE_LONGLONG = 0x8;
  public static final int MYSQLI_TYPE_INT24 = 0x9;
  public static final int MYSQLI_TYPE_DATE = 0xA;
  public static final int MYSQLI_TYPE_TIME = 0xB;
  public static final int MYSQLI_TYPE_DATETIME = 0xC;
  public static final int MYSQLI_TYPE_YEAR = 0xD;
  public static final int MYSQLI_TYPE_NEWDATE = 0xE;

  // Mysql defines the constant MYSQL_TYPE_VARCHAR = 0xF
  // but there is no MYSQLI_TYPE_VARCHAR flag.

  public static final int MYSQLI_TYPE_BIT = 0x10;
  public static final int MYSQLI_TYPE_NEWDECIMAL = 0xF6;
  public static final int MYSQLI_TYPE_ENUM = 0xF7;
  public static final int MYSQLI_TYPE_SET = 0xF8;
  public static final int MYSQLI_TYPE_TINY_BLOB = 0xF9;
  public static final int MYSQLI_TYPE_MEDIUM_BLOB = 0xFA;
  public static final int MYSQLI_TYPE_LONG_BLOB = 0xFB;
  public static final int MYSQLI_TYPE_BLOB = 0xFC;
  public static final int MYSQLI_TYPE_VAR_STRING = 0xFD;
  public static final int MYSQLI_TYPE_STRING = 0xFE;
  public static final int MYSQLI_TYPE_GEOMETRY = 0xFF;

  public static final int MYSQLI_TYPE_CHAR = MYSQLI_TYPE_TINY;
  public static final int MYSQLI_TYPE_INTERVAL = MYSQLI_TYPE_ENUM;

  public static final int MYSQL_CLIENT_COMPRESS = (1 << 5);

  // The next constant is NOT exported by this module,
  // but the PHP documentation states that 128 can be passed
  // in the client_flags parameter to mysql_connect().
  // This flag will be ignored by Mysqli.connectImpl().

  private static final int MYSQL_CLIENT_LOCAL_FILES = (1 << 7);

  public static final int MYSQL_CLIENT_IGNORE_SPACE = (1 << 8);
  public static final int MYSQL_CLIENT_INTERACTIVE = (1 << 10);
  public static final int MYSQL_CLIENT_SSL = (1 << 11);

  // mysqli_options option flags

  public static final int MYSQLI_READ_DEFAULT_GROUP = 0x0;
  public static final int MYSQLI_READ_DEFAULT_FILE = 0x1;
  public static final int MYSQLI_OPT_CONNECT_TIMEOUT = 0x2;
  public static final int MYSQLI_OPT_LOCAL_INFILE = 0x3;
  public static final int MYSQLI_INIT_COMMAND = 0x4;

  public MysqliModule()
  {
  }

  /**
   * Returns true for the mysql extension.
   */
  public String []getLoadedExtensions()
  {
    return new String[] { "mysqli" };
  }

  /**
   * Returns the number of affected rows.
   */
  public static int mysqli_affected_rows(@NotNull Mysqli conn)
  {
    if (conn == null)
      return -1;

    return conn.affected_rows();
  }

  /**
   * Turns auto-commit on or off.
   */
  public static boolean mysqli_autocommit(@NotNull Mysqli conn, boolean mode)
  {
    if (conn == null)
      return false;

    return conn.autocommit(mode);
  }

  /**
   * Deprecated alias for {@link #mysqli_stmt_bind_param}.
   */
  public static boolean mysqli_bind_param(Env env,
                                          @NotNull MysqliStatement stmt,
                                          StringValue types,
                                          @Reference Value[] params)
  {
    return mysqli_stmt_bind_param(env, stmt, types, params);
  }

  /**
   * Commits the current transaction for the supplied connection.
   *
   * returns true on success or false on failure
   */
  public static boolean mysqli_commit(@NotNull Mysqli conn)
  {
    if (conn == null)
      return false;

    return conn.commit();
  }

  /**
   * Returns the client encoding.
   */
  public static Value mysqli_character_set_name(Env env, @NotNull Mysqli conn)
  {
    if (conn == null)
      return NullValue.NULL;

    return conn.character_set_name(env);
  }

  /**
   * Alias for {@link #mysqli_character_set_name}.
   */
  public static Value mysqli_client_encoding(Env env, @NotNull Mysqli conn)
  {
    return mysqli_character_set_name(env, conn);
  }

  /**
   * Closes a connection.
   */
  public static boolean mysqli_close(Env env, @NotNull Mysqli conn)
  {
    if (conn == null)
      return false;
    else if (! conn.isConnected()) {
      env.warning(L.l("no MySQLi-Link resource supplied"));
      return false;
    }

    return conn.close(env);
  }

  /**
   * Returns a new connection.
   */
  @ReturnNullAsFalse
  public static Mysqli mysqli_connect(Env env,
              @Optional("localhost") StringValue host,
              @Optional StringValue userName,
              @Optional StringValue password,
              @Optional String dbname,
              @Optional("3306") int port,
              @Optional StringValue socket)
    throws IllegalStateException
  {
    Mysqli mysqli = new Mysqli(env,
                               host,
                               userName,
                               password,
                               dbname,
                               port,
                               socket);

    if (! mysqli.isConnected())
      return null;

    return mysqli;
  }

  /**
   * Returns an error code value for the last call to mysqli_connect(),
   * 0 for no previous error.
   */
  public static int mysqli_connect_errno(Env env)
  {
    Value value = (Value) env.getSpecialValue("mysqli.connectErrno");

    if (value != null)
      return value.toInt();
    else
      return 0;
  }

  /**
   * Returns an error description for the last call to mysqli_connect(),
   * "" for no previous error.
   */
  public static StringValue mysqli_connect_error(Env env)
  {
    Object error = env.getSpecialValue("mysqli.connectError");

    if (error != null)
      return env.createString(error.toString());
    else
      return env.getEmptyString();
  }

  /**
   * Seeks the specified row.
   *
   * @param env the PHP executing environment
   * @param result the mysqli_result
   * @param rowNumber the row offset
   * @return true on success or false if the row number
   * does not exist. NULL is returned if an error occurred.
   */
  public static Value mysqli_data_seek(Env env,
                                       @NotNull MysqliResult result,
                                       int rowNumber)
  {
    if (result == null)
      return NullValue.NULL;

    if (result.seek(env, rowNumber)) {
      return BooleanValue.TRUE;
    } else {
      env.warning(L.l(
          "Offset {0} is invalid for MySQL (or the query data is unbuffered)",
                      rowNumber));
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the error code for the most recent function call,
   * 0 for no error.
   */
  public static Value mysqli_errno(@NotNull Mysqli conn)
  {
    if (conn == null)
      return NullValue.NULL;

    return LongValue.create(conn.errno());
  }

  /**
   * Alias for {@link #mysqli_real_escape_string}
   */
  public static Value mysqli_escape_string(Env env,
                                           @NotNull Mysqli conn,
                                           StringValue unescapedString)
  {
    return mysqli_real_escape_string(env, conn, unescapedString);
  }

  /**
   * Deprecated alias for {@link #mysqli_stmt_fetch}.
   */

  public static Value mysqli_fetch(Env env,
                                   MysqliStatement stmt)
  {
    return mysqli_stmt_fetch(env, stmt);
  }

  /**
   * Returns the field metadata.
   *
   */
  public static Value mysqli_fetch_field_direct(Env env,
                                                @NotNull MysqliResult result,
                                                int fieldOffset)
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.fetch_field_direct(env, fieldOffset);
  }

  /**
   * Returns the field metadata.
   */
  public static Value mysqli_fetch_field(Env env,
                                         @NotNull MysqliResult result)
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.fetch_field(env);
  }

  /**
   * Returns an array of field metadata.
   */
  public static Value mysqli_fetch_fields(Env env,
                                          @NotNull MysqliResult result)
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.fetch_fields(env);
  }

  /**
   * Returns an array of integers respresenting the size of each column
   * FALSE if an error occurred.
   *
   * @param env the PHP executing environment
   * @param result the mysqli_result
   * @return true on success or false if an error occurred.
   * NULL is returned if result is null.
   */
  public static Value mysqli_fetch_lengths(Env env,
                                           @NotNull MysqliResult result)
  {
    if (result == null)
      return NullValue.NULL;

    return result.fetch_lengths();
  }

  /**
   * Seeks to the specified field offset.
   * If the next call to mysql_fetch_field() doesn't include
   * a field offset, the field offset specified in
   * mysqli_field_seek() will be returned.
   */
  public static boolean mysqli_field_seek(Env env,
                                          @NotNull MysqliResult result,
                                          int fieldOffset)
  {
    if (result == null)
      return false;

    return result.field_seek(env, fieldOffset);
  }

  /**
   * Returns the position of the field cursor used for the last
   * mysqli_fetch_field() call. This value can be used as an
   * argument to mysqli_field_seek()
   */
  public static int mysqli_field_tell(Env env,
              @NotNull MysqliResult result)
  {
    if (result == null)
      return -1;

    return result.field_tell(env);
  }

  /**
   * Frees a mysqli result
   */
  public static boolean mysqli_free_result(@NotNull MysqliResult result)
  {
    if (result == null)
      return false;

    result.close();

    return true;
  }

  /**
   * Returns ID generated for an AUTO_INCREMENT column by the previous
   * INSERT query on success, 0 if the previous query does not generate
   * an AUTO_INCREMENT value, or FALSE if no MySQL connection was established
   */
  public static Value mysqli_insert_id(Env env,
                                       @NotNull Mysqli conn)
  {
    if (conn == null)
      return BooleanValue.FALSE;

    return conn.insert_id(env);
  }

  /**
   * Returns the number of fields from specified result set.
   */
  public static Value mysqli_num_fields(@NotNull MysqliResult result)
  {
    if (result == null)
      return NullValue.NULL;

    return LongValue.create(result.num_fields());
  }

  /**
   * Executes one or multiple queires which are
   * concatenated by a semicolon.
   */
  public static boolean mysqli_multi_query(Env env,
                                           @NotNull Mysqli conn,
                                           StringValue query)
  {
    if (conn == null)
      return false;

    return conn.multi_query(env, query);
  }

  /**
   * Indicates if one or more result sets are available from
   * a previous call to mysqli_multi_query.
   */
  public static boolean mysqli_more_results(@NotNull Mysqli conn)
  {
    if (conn == null)
      return false;

    return conn.more_results();
  }

  /**
   * Prepares next result set from a previous call to
   * mysqli_multi_query.
   */
  public static boolean mysqli_next_result(@NotNull Mysqli conn)
  {
    if (conn == null)
      return false;

    return conn.next_result();
  }

  /**
   * Returns the error code for the prepared statement.
   */
  public static int mysqli_stmt_errno(Env env,
                                      @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return 0;

    return stmt.errno();
  }

  /**
   * Returns the error message for the prepared statement.
   */
  public static StringValue mysqli_stmt_error(Env env,
                                              @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return env.getEmptyString();

    return stmt.error(env);
  }

  /**
   * Returns the most recent error.
   */
  public static Value mysqli_error(Env env,
                                   @NotNull Mysqli conn)
  {
    if (conn == null)
      return NullValue.NULL;

    return conn.error(env);
  }

  /**
   * Returns the number of columns for the most recent query.
   */
  public static int mysqli_field_count(@NotNull Mysqli conn)
  {
    if (conn == null)
      return 0;

    return conn.field_count();
  }

  /**
   * Returns a row for the result.
   */
  @ReturnNullAsFalse
  public static ArrayValue mysqli_fetch_array(Env env,
                                              @NotNull MysqliResult result,
                                              @Optional("MYSQLI_BOTH") int type)
  {
    if (result == null)
      return null;

    return result.fetch_array(env, type);
  }

  /**
   * Returns an associative array from the result.
   */
  @ReturnNullAsFalse
  public static ArrayValue mysqli_fetch_assoc(Env env,
                                              @NotNull MysqliResult result)
  {
    if (result == null)
      return null;

    return result.fetch_assoc(env);
  }

  /**
   * Returns a row for the result. Return NULL if there are no more rows.
   */

  public static ArrayValue mysqli_fetch_row(Env env,
                                            @NotNull MysqliResult result)
  {
    if (result == null)
      return null;

    return result.fetch_row(env);
  }

  /**
   * Returns an object with properties that correspond
   * to the fetched row and moves the data pointer ahead.
   *
   * @param env the PHP executing environment
   * @param result the mysqli_result
   * @return an object that corresponds to the fetched
   * row or NULL if there are no more rows in resultset
   */
  public static Value mysqli_fetch_object(Env env,
                                          @NotNull MysqliResult result)
  {
    if (result == null)
      return NullValue.NULL;

    return result.fetch_object(env);
  }

  /**
   * Returns the MySQL client version.
   */
  public static StringValue mysqli_get_client_info(Env env)
  {
    return Mysqli.getClientInfo(env);
  }

  /**
   * Returns a number that represents the MySQL client library
   * version in format:
   *
   * main_version*10000 + minor_version*100 + sub_version.
   *
   * For example 4.1.0 is returned as 40100.
   */
   public static int mysqli_get_client_version(Env env)
   {
     return Mysqli.infoToVersion(
       mysqli_get_client_info(env).toString());
   }

  /**
   * Returns a string describing the type of MySQL
   * connection in use.
   */
  public static Value mysqli_get_host_info(Env env, @NotNull Mysqli conn)
  {
    if (conn == null)
      return NullValue.NULL;

    return conn.get_host_info(env);
  }

  /**
   * Return protocol number, for example 10.
   */
  public static Value mysqli_get_proto_info(@NotNull Mysqli conn)
  {
    if (conn == null)
      return NullValue.NULL;

    return LongValue.create(conn.get_proto_info());
  }

  /**
   * Returns the MySQL server version.
   */

  public static Value mysqli_get_server_info(Env env, @NotNull Mysqli conn)
  {
    if (conn == null)
      return NullValue.NULL;

    if (conn.isConnected())
      return conn.get_server_info(env);
    else
      return NullValue.NULL;
  }

  /**
   * Returns a number that represents the MySQL server version.
   */
  public static Value mysqli_get_server_version(@NotNull Mysqli conn)
  {
    if (conn == null)
      return NullValue.NULL;

    return LongValue.create(conn.get_server_version());
  }

  /**
   * Returns the number of rows in the result set.
   *
   * @param env the PHP executing environment
   * @param result the mysqli_result
   * @return the number of rows in the result set
   * or NULL, if an error occurred
   */
  public static Value mysqli_num_rows(Env env,
                                      @NotNull MysqliResult result)
  {
    if (result == null)
      return NullValue.NULL;

    return LongValue.create(result.num_rows());
  }

  /**
   * Sets the options for a connection.
   */
  public static boolean mysqli_options(@NotNull Mysqli mysqli,
                                       int option,
                                       Value value)
  {
    if (mysqli == null)
      return false;

    return mysqli.options(option, value);
  }

  /**
   * Alias of {@link #mysqli_options}.
   */
  public static boolean mysqli_set_opt(@NotNull Mysqli mysqli,
                                       int option,
                                       Value value)
  {
    return mysqli_options(mysqli, option, value);
  }

  /**
   * Alias of {@link #mysqli_stmt_param_count}.
   */
  public static int mysqli_param_count(Env env,
                                       @NotNull MysqliStatement stmt)
  {
    return mysqli_stmt_param_count(env, stmt);
  }

  /**
   * Rolls back the current transaction for the  * connection.
   *
   * @return true on success or false on failure.
   */
  public static boolean mysqli_rollback(@NotNull Mysqli conn)
  {
    if (conn == null)
      return false;

    return conn.rollback();
  }

  /**
   * Sets the character set for a conneciton.
   */
  public static boolean mysqli_set_charset(@NotNull Mysqli mysqli,
             String charset)
  {
    if (mysqli == null)
      return false;

    return mysqli.set_charset(charset);
  }

  /**
   * Returns the number of rows.
   */
  public static Value mysqli_stmt_num_rows(Env env,
                                           @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return BooleanValue.FALSE;

    return stmt.num_rows(env);
  }

  /**
   * Returns an integer representing the number of parameters
   * or -1 if no query has been prepared.
   */
  public static int mysqli_stmt_param_count(Env env,
                                            @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return -1;

    return stmt.param_count(env);
  }

  /**
   * Prepares a statment with a query.
   */
  public static boolean mysqli_stmt_prepare(Env env,
                                            @NotNull MysqliStatement stmt,
                                            StringValue query)
  {
    if (stmt == null)
      return false;

    return stmt.prepare(env, query);
  }

  /**
   * Resets a statment.
   */
  public static boolean mysqli_stmt_reset(Env env,
                                          @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return false;

    return stmt.reset(env);
  }

  /**
   * Returns result information for metadata
   */
  @ReturnNullAsFalse
  public static JdbcResultResource
    mysqli_stmt_result_metadata(Env env,
                                @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return null;

    return stmt.result_metadata(env);
  }

  /**
   * Returns an error string.
   */
  public static Value mysqli_sqlstate(Env env,
                                      @NotNull Mysqli conn)
  {
    if (conn == null)
      return NullValue.NULL;

    return conn.sqlstate(env);
  }

  /**
   * Returns an error string.
   */
  public static Value mysqli_stmt_sqlstate(Env env,
                                           @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return NullValue.NULL;

    return stmt.sqlstate(env);
  }

  /**
   * Saves the result.
   */
  public static boolean mysqli_stmt_store_result(Env env,
                                                 @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return false;

    return stmt.store_result(env);
  }

  /**
   * Transfers the result set from the last query on the
   * database connection represented by conn.
   *
   * Used in conjunction with {@link #mysqli_multi_query}
   */
  @ReturnNullAsFalse
  public static JdbcResultResource mysqli_store_result(Env env,
                                                       @NotNull Mysqli conn)
  {
    if (conn == null)
      return null;

    return conn.store_result(env);
  }

  /**
   * Initiate a result set retrieval. This method is useful when
   * dealing with multiple results from a multi-query. Currently,
   * unbuffered results are not supported so this method always
   * uses buffered results.
   */

  @ReturnNullAsFalse
  public static JdbcResultResource mysqli_use_result(Env env,
                                                     @NotNull Mysqli conn)
  {
    if (conn == null)
      return null;

    return conn.use_result(env);
  }

  /**
   * Returns the number of warnings from the last query
   * in the connection object.
   *
   * @return number of warnings
   */
  public static int mysqli_warning_count(Env env,
                                         @NotNull Mysqli conn)
  {
    if (conn == null)
      return 0;

    return conn.warning_count(env);
  }

  /**
   * Checks if the connection is still valid
   */
  public static boolean mysqli_ping(Env env,
                                    @NotNull Mysqli conn)
  {
    if (conn == null)
      return false;

    return conn.ping(env);
  }

  /**
   * Executes a query and returns the result.
   *
   */
  public static Value mysqli_query(
      Env env,
      @NotNull Mysqli conn,
      StringValue sql,
      @Optional("MYSQLI_STORE_RESULT") int resultMode) {
    // ERRATUM: <i>resultMode</i> is ignored, MYSQLI_USE_RESULT would represent
    //  an unbuffered query, but that is not supported.

    Value value = query(env, conn, sql);

    if (value == null) {
      return BooleanValue.FALSE;
    }

    return value;
  }

  private static Value query(Env env,
                             Mysqli conn,
                             StringValue sql)
  {
    Value value = null;

    try {
      value = conn.query(env, sql, MYSQLI_STORE_RESULT);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    if (value == null) {
      return BooleanValue.FALSE;
    }

    return value;
  }

  /**
   * Connects to the database.
   */
  public static boolean mysqli_real_connect(
      Env env,
      @NotNull Mysqli mysqli,
      @Optional("localhost") StringValue host,
      @Optional StringValue userName,
      @Optional StringValue password,
      @Optional StringValue dbname,
      @Optional("3306") int port,
      @Optional StringValue socket,
      @Optional int flags) {
    if (mysqli == null)
      return false;

    return mysqli.real_connect(env, host, userName, password,
      dbname, port, socket, flags);
  }

  /**
   * Escapes the following special character in unescapedString.
   *
   * @return the escaped string.
   */
  public static Value mysqli_real_escape_string(Env env,
                                                @NotNull Mysqli conn,
                                                StringValue unescapedString)
  {
    if (conn == null)
      return NullValue.NULL;

    if (unescapedString.length() == 0)
      return env.getEmptyString();

    StringBuilder buf = new StringBuilder();

    escapeString(buf, unescapedString.toString());

    return env.createString(buf.toString());
  }

  static void escapeString(StringBuilder buf, String unescapedString)
  {
    char c;

    final int strLength = unescapedString.length();

    for (int i = 0; i < strLength; i++) {
      c = unescapedString.charAt(i);
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
  }

  /**
   * Execute an single query against the database
   * whose result can then be retrieved
   * or stored using the mysqli_store_result()
   * or mysqli_use_result() functions.
   */
  public static boolean mysqli_real_query(Env env,
                                          @NotNull Mysqli conn,
                                          StringValue query)
  {
    if (conn == null)
      return false;

    return conn.real_query(env, query);
  }

  /**
   * Execute a query with arguments and return a result.
   */
  static Value mysqli_query(Env env,
                             Mysqli conn,
                             StringValue query,
                             Object ... args)
  {
    StringBuilder buf = new StringBuilder();

    int size = query.length();

    int argIndex = 0;

    for (int i = 0; i < size; i++) {
      char ch = buf.charAt(i);

      if (ch == '?') {
        Object arg = args[argIndex++];

        if (arg == null)
          throw new IllegalArgumentException(
              L.l("argument `{0}' cannot be null", arg));

        buf.append('\'');
        escapeString(buf, String.valueOf(arg));
        buf.append('\'');
      }
      else
        buf.append(ch);
    }

    return query(env, conn,
                 env.createString(buf.toString()));
  }


  /**
   * Select the database for a connection.
   */
  public static boolean mysqli_select_db(Mysqli conn, String dbName)
  {
    if (conn == null)
      return false;

    return conn.select_db(dbName);
  }

  /**
   * Returns a string with the status of the connection
   * or FALSE if error.
   */
  public static Value mysqli_stat(Env env, @NotNull Mysqli conn)
  {
    if (conn == null)
      return BooleanValue.FALSE;

    return conn.stat(env);
  }

  /**
   * Return the number of rows affected by an INSERT, UPDATE, or DELETE
   * query. Unlike mysqli_stmt_num_rows(), this method does not return
   * the number of rows matched by a SELECT query.
   */
  public static int mysqli_stmt_affected_rows(Env env,
                                              @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return -1;

    if (stmt.errno() != 0)
      return -1;

    return stmt.affected_rows(env);
  }

  /**
   * Binds variables for the parameter markers
   * in SQL statement that was passed to
   * {@link #mysqli_prepare}.
   *
   * Type specification chars:
   * <dl>
   * <dt>i<dd>corresponding variable has type integer;
   * <dt>d<dd>corresponding variable has type double;
   * <dt>b<dd>corresponding variable is a blob and will be sent in packages
   * <dt>s<dd>corresponding variable has type string
   * (which really means all other types);
   * </dl>
   */
  public static boolean mysqli_stmt_bind_param(Env env,
                                               @NotNull MysqliStatement stmt,
                                               StringValue types,
                                               @Reference Value[] params)
  {
    if (stmt == null)
      return false;

    return stmt.bind_param(env, types, params);
  }

  /**
   * Binds outparams to result set.
   */
  public static boolean mysqli_stmt_bind_result(Env env,
                                                @NotNull MysqliStatement stmt,
                                                @Reference Value[] outParams)
  {
    if (stmt == null)
      return false;

    return stmt.bind_result(env, outParams);
  }

  /**
   * Closes the statement.
   */
  public boolean mysql_stmt_close(MysqliStatement stmt)
  {
    if (stmt == null)
      return false;

    stmt.close();

    return true;
  }

  /**
   * Seeks to a given result.
   *
   * @return NULL on sucess or FALSE on failure
   */
  public Value mysqli_stmt_data_seek(Env env,
                                     @NotNull MysqliStatement stmt,
                                     int offset)
  {
    if (stmt == null)
      return BooleanValue.FALSE;

    return stmt.data_seek(env, offset);
  }

  /**
   * Returns the error number.
   */
  public int mysql_stmt_errno(Env env,
                              MysqliStatement stmt)
  {
    if (stmt != null)
      return stmt.errno();
    else
      return 0;
  }

  /**
   * Returns a descrption of the error or an empty strng for no error.
   */
  public StringValue mysql_stmt_error(Env env,
                                 MysqliStatement stmt)
  {
    if (stmt == null)
      return null;

    return stmt.error(env);
  }

  /**
   * Executes a statement that has been prepared using {@link #mysqli_prepare}.
   *
   * @return true on success or false on failure
   */
  public static boolean mysqli_stmt_execute(Env env,
                                            @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return false;

    return stmt.execute(env);
  }

  /**
   * Fetch results from a prepared statement.
   * @return true on success, false on error, null if no more rows.
   */
  public static Value mysqli_stmt_fetch(Env env,
                                        @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return BooleanValue.FALSE;

    return stmt.fetch(env);
  }

  /**
   * Frees the result.
   */
  public static boolean mysqli_stmt_free_result(Env env,
                                                MysqliStatement stmt)
  {
    if (stmt == null)
      return false;

    stmt.free_result(env);

    return true;
  }

  /**
   * Deprecated alias for {@link #mysqli_stmt_bind_result}.
   */

  public static boolean mysqli_bind_result(Env env,
                                           @NotNull MysqliStatement stmt,
                                           @Reference Value[] outParams)
  {
    return mysqli_stmt_bind_result(env, stmt, outParams);
  }

  /**
   * Changes the user and database.
   */
  public static boolean mysqli_change_user(@NotNull Mysqli mysqli,
                                           String user,
                                           String password,
                                           String db)
  {
    if (mysqli == null)
      return false;

    return mysqli.change_user(user, password, db);
  }

  /**
   * Deprecated alias for {@link #mysqli_stmt_execute}.
   */
  public static boolean mysqli_execute(Env env,
                                       @NotNull MysqliStatement stmt)
  {
    return mysqli_stmt_execute(env, stmt);
  }

  /**
   * Deprecated alias for {@link #mysqli_stmt_result_metadata}.
   */
  @ReturnNullAsFalse
  public static JdbcResultResource mysqli_get_metadata(
      Env env,
      @NotNull MysqliStatement stmt) {
    return mysqli_stmt_result_metadata(env, stmt);
  }

  /**
   * Creates a new mysqli object.
   */
  public static Mysqli mysqli_init(Env env)
  {
    return new Mysqli(env);
  }

  /**
   * Prepares a statement.
   */
  @ReturnNullAsFalse
  public static MysqliStatement mysqli_prepare(Env env,
                                               @NotNull Mysqli conn,
                                               StringValue query)
  {
    if (conn == null)
      return null;

    return conn.prepare(env, query);
  }

  /**
   * Closes a statement.
   */
  public static boolean mysqli_stmt_close(Env env,
                                          @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return false;

    return stmt.close(env);
  }

  /**
   * Returns a statement for use with {@link #mysqli_stmt_prepare}
   */
  public static MysqliStatement mysqli_stmt_init(Env env,
                                                 @NotNull Mysqli conn)
  {
    if (conn == null)
      return null;

    return conn.stmt_init(env);
  }

  /**
   * Get information about the most recent query.
   */
  public static Value mysqli_info(Env env, @Optional Mysqli conn)
  {
    if (conn == null)
      return null;

    return conn.info(env);
  }

  /**
   * Undocumented
   */
  public static int mysqli_stmt_field_count(Env env,
                                            @NotNull MysqliStatement stmt)
  {
    if (stmt == null)
      return -1;

    return stmt.field_count(env);
  }

  /**
   * Query an identifier that corresponds to this specific
   * connection. Mysql calls this integer identifier a
   * thread, but it is really a connection identifier.
   */
  public static Value mysqli_thread_id(Env env,
                                       @NotNull Mysqli conn)
  {
    if (conn == null)
      return BooleanValue.FALSE;

    return conn.thread_id(env);
  }

  /**
   * Terminate a Mysql connection with the given thread id.
   * It should be possible to terminate any connection id
   * via this method. In practice, only the mysqli_thread_id
   * API returns a thread id, so only the id of the current
   * thread can be looked up.
   */
  public static boolean mysqli_kill(Env env,
                                    @NotNull Mysqli conn,
                                    int threadId)
  {
    if (conn == null)
      return false;

    return conn.kill(env, threadId);
  }

  // Undocumented
  //
  // mysqli_enable_reads_from_master
  // mysqli_disable_reads_from_master
  // mysqli_enable_rpl_parse
  // mysqli_disable_rpl_parse
  // mysqli_rpl_parse_enabled
  // mysqli_rpl_probe
  // mysqli_rpl_query_type
  // mysqli_embedded_server_start
  // mysqli_embedded_server_end
  // mysqli_get_charset
  // mysqli_master_query
  // mysqli_send_query
  // mysqli_server_end
  // mysqli_server_init
  // mysqli_set_local_infile_default
  // mysqli_set_local_infile_handler
  // mysqli_slave_query
  // mysqli_stmt_attr_get
  // mysqli_stmt_attr_set
  // mysqli_stmt_get_warnings
  // mysqli_stmt_insert_id

  // Unimplemented
  //
  //@todo mysqli_debug
  //@todo mysqli_dump_debug_info
  //@todo mysqli_kill
  //@todo mysqli_report
  //@todo mysqli_send_long_data (alias for mysqli_stmt_send_long_data)
  //@todo mysqli_set_charset
  //@todo mysqli_ssl_set
  //@todo mysqli_stmt_send_long_data
  //@todo mysqli_thread_safe
}
