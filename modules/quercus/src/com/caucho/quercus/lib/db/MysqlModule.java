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

import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * PHP mysql routines.
 */
public class MysqlModule extends AbstractQuercusModule {

  private static final Logger log = Log.open(MysqlModule.class);
  private static final L10N L = new L10N(MysqlModule.class);

  public static final int MYSQL_ASSOC = JdbcResultResource.FETCH_ASSOC;
  public static final int MYSQL_NUM = JdbcResultResource.FETCH_NUM;
  public static final int MYSQL_BOTH = JdbcResultResource.FETCH_BOTH;

  public static final int MYSQL_USE_RESULT = 0x0;
  public static final int MYSQL_STORE_RESULT = 0x1;

  private static final StringValue SV_NAME
    = new ConstStringValue("name");
  private static final StringValue SV_TABLE
    = new ConstStringValue("table");
  private static final StringValue SV_DEF
    = new ConstStringValue("def");
  private static final StringValue SV_MAX_LENGTH
    = new ConstStringValue("max_length");
  private static final StringValue SV_NOT_NULL
    = new ConstStringValue("not_null");
  private static final StringValue SV_PRIMARY_KEY
    = new ConstStringValue("primary_key");
  private static final StringValue SV_MULTIPLE_KEY
    = new ConstStringValue("multiple_key");
  private static final StringValue SV_UNIQUE_KEY
    = new ConstStringValue("unique_key");
  private static final StringValue SV_NUMERIC
    = new ConstStringValue("numeric");
  private static final StringValue SV_BLOB
    = new ConstStringValue("blob");
  private static final StringValue SV_TYPE
    = new ConstStringValue("type");
  private static final StringValue SV_UNSIGNED
    = new ConstStringValue("unsigned");
  private static final StringValue SV_ZEROFILL
    = new ConstStringValue("zerofill");

  public MysqlModule()
  {
  }

  /**
   * Returns true for the mysql extension.
   */
  public String []getLoadedExtensions()
  {
    return new String[] { "mysql" };
  }

  /**
   * Returns the number of affected rows.
   */
  public static int mysql_affected_rows(Env env, @Optional Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    return conn.affected_rows();
  }

  /**
   * Get information about the most recent query.
   */
  public static Value mysql_info(Env env, @Optional Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    return conn.info(env);
  }

  /**
   * Change the logged in user of the current active connection.
   * This function is deprecated and was removed from PHP in PHP 3.0.14.
   */

  public static boolean mysql_change_user(Env env,
                               StringValue user,
                               StringValue pass,
                               @Optional StringValue database,
                               @Optional Mysqli conn)
  {
    return false;
  }

  /**
   * Returns the client encoding
   */
  public static StringValue mysql_client_encoding(
      Env env, @Optional Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    return conn.client_encoding(env);
  }

  /**
   * Closes a mysql connection.
   */
  public static boolean mysql_close(Env env, @Optional Mysqli conn)
  {
    boolean isEnvConn = false;

    if (conn == null) {
      conn = (Mysqli) env.getSpecialValue("caucho.mysql");

      isEnvConn = true;
    }

    if (conn == null) {
      // php/1435

      env.warning(L.l("no MySQL-Link resource supplied"));
      return false;
    }

    if (isEnvConn || env.getSpecialValue("caucho.mysql") != null)
      env.removeSpecialValue("caucho.mysql");

    if (conn.isConnected()) {
      conn.close(env);

      return true;
    }
    else {
      env.warning(
          L.l("connection is either not connected or is already closed"));

      return false;
    }
  }

  /**
   * Creates a database.
   */
  public static boolean mysql_create_db(Env env,
                                 @NotNull StringValue name,
                                 @Optional Mysqli conn)
  {
    if (name.length() == 0)
      return false;

    if (conn == null)
      conn = getConnection(env);

    Statement stmt = null;

    // XXX: move implementation
    try {
      try {
        Connection sqlConn = conn.validateConnection().getConnection(env);

        if (sqlConn == null)
          return false;

        stmt = sqlConn.createStatement();
        stmt.setEscapeProcessing(false);
        stmt.executeUpdate("CREATE DATABASE " + name.toString());
      } finally {
        if (stmt != null)
          stmt.close();
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }

    return true;
  }

  /**
   * Moves the intenal row pointer of the MySQL result to the
   * specified row number, 0 based.
   */
  public static boolean mysql_data_seek(Env env,
                                        @NotNull MysqliResult result,
                                        int rowNumber)
  {
    if (result == null)
      return false;

    if (result.seek(env, rowNumber)) {
      return true;
    } else {
      env.warning(
          L.l("Offset {0} is invalid for MySQL "
              + "(or the query data is unbuffered)",
                      rowNumber));
      return false;
    }
  }

  /**
   * Retrieves the database name after a call to mysql_list_dbs()
   */
  public static Value mysql_db_name(Env env,
                             @NotNull MysqliResult result,
                             int row,
                             @Optional("0") Value field)
  {
    if (result == null)
      return BooleanValue.FALSE;

    return mysql_result(env, result, row, field);
  }

  /**
   * Deprecated alias for mysql_db_name
   */
  public static Value mysql_dbname(Env env,
                             @NotNull MysqliResult result,
                             int row)
  {
    return mysql_db_name(env, result, row,
                         env.createString("0"));
  }

  /**
   * Returns the value of one field in the result set. FALSE on failure.
   */
  public static Value mysql_result(Env env,
                            @NotNull MysqliResult result,
                            int row,
                            @Optional("0") Value field)
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.getResultField(env, row, field);
  }

  /**
   * Drops a database.
   */
  public static boolean mysql_drop_db(Env env,
                               @NotNull StringValue databaseName,
                               @Optional Mysqli conn)
  {
    if (databaseName.length() == 0)
      return false;

    Value value = mysql_query(env,
      env.createString("DROP DATABASE " + databaseName),
      conn);

    return (value != null && value.toBoolean());
  }

  /**
   * Deprecated alias for mysql_drop_db.
   */
  public static boolean mysql_dropdb(Env env,
                             @NotNull StringValue databaseName,
                             @Optional Mysqli conn)
  {
    return mysql_drop_db(env, databaseName, conn);
  }

  /**
   * Returns the error number of the most recent error
   */
  public static int mysql_errno(Env env, @Optional Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    StringValue error = conn.error(env);

    int errno = conn.errno();

    if (errno != 0)
      return errno;
    else if (error.length() != 0)
      return 2006; // mysql has gone away
    else
      return 0;
  }

  /**
   * Returns the most recent error.
   */
  public static StringValue mysql_error(Env env, @Optional Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    return conn.error(env);
  }

  /**
   * Deprecated, mysql_real_escape_string() should be used instead.
   *
   * @see StringValue MysqlModule.mysql_real_escape_string(String, Mysqli)
   *
   * @return the escaped string
   */

  public static StringValue mysql_escape_string(Env env, Value val)
  {
    StringValue unescapedString = val.toStringValue();

    StringValue sb = unescapedString.createStringBuilder();

    int len = unescapedString.length();

    for (int i = 0; i < len; i++) {
      char ch = unescapedString.charAt(i);

      switch(ch) {
        case 0:
          sb.append('\\');
          sb.append(0);
          break;
        case '\n':
          sb.append('\\');
          sb.append('n');
          break;
        case '\r':
          sb.append('\\');
          sb.append('r');
          break;
        case '\\':
          sb.append('\\');
          sb.append('\\');
          break;
        case '\'':
          sb.append('\\');
          sb.append('\'');
          break;
        case '"':
          sb.append('\\');
          sb.append('"');
          break;
        case 0x1A:
          sb.append('\\');
          sb.append('Z');
          break;
        default:
          sb.append(ch);
      }
    }

    return sb;
  }

  /**
   * Escapes special characters.
   *
   * @see StringValue MysqliModule.mysqli_real_escape_string(
   * JdbcConnectionResource, String)
   *
   * @return the escaped string
   */

  public static StringValue mysql_real_escape_string(Env env,
                                                     Value val,
                                                     @Optional Mysqli conn)
  {
    StringValue unescapedString = val.toStringValue();

    if (conn == null)
      conn = getConnection(env);

    return conn.real_escape_string(unescapedString);
  }

  /**
   * Returns a row from the connection
   */
  public static Value mysql_fetch_array(Env env,
                                 @NotNull MysqliResult result,
                                 @Optional("MYSQL_BOTH") int type)
  {
    if (result == null)
      return BooleanValue.FALSE;

    Value value = result.fetch_array(env, type);

    if (value != null)
      return value;
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns a row from the connection
   */
  @ReturnNullAsFalse
  public static ArrayValue mysql_fetch_assoc(Env env,
                                             @NotNull MysqliResult result)
  {
    if (result == null)
      return null;

    return result.fetch_array(env, MYSQL_ASSOC);
  }

  /**
   * Returns an object containing field information.
   * On success, this method increments the field offset
   * (see {@link #mysql_field_seek}).
   *
   * <h3>ERRATA</h3>
   * <ul>
   *   <li>quercus returns "int" for BIT type, php returns "unknown"
   *   <li>quercus always returns int(0) for unique_key
   *   <li>quercus always returns int(0) for zerofill
   *   <li>quercus always returns int(0) for multiple_key
   * </ul>
   *
   */
  public static Value mysql_fetch_field(Env env,
                                        @NotNull MysqliResult result,
                                        @Optional("-1") int fieldOffset)
  {
    /**
     * ERRATA is also documented in php/142s.qa
     * There is probably a mysql specific query or API that would be better
     * for getting this information
     */

    if (result == null)
      return BooleanValue.FALSE;

    // php/142v.qa - call must succeed even if some info not available

    try {
      if (fieldOffset == -1) {
        fieldOffset = result.field_tell(env);
        result.setFieldOffset(fieldOffset + 1);
      }

      ResultSetMetaData md = result.getMetaData();

      if (md.getColumnCount() <= fieldOffset || fieldOffset < 0) {
        return BooleanValue.FALSE;
      }

      QuercusResultSetMetaData qMd = null;

      if (md instanceof QuercusResultSetMetaData)
        qMd = (QuercusResultSetMetaData) md;

      int jdbcField = fieldOffset + 1;
      int jdbcColumnType = md.getColumnType(jdbcField);

      String catalogName = md.getCatalogName(jdbcField);
      String tableName = md.getTableName(jdbcField);

      String schemaName = md.getSchemaName(jdbcField);

      String columnName = md.getColumnName(jdbcField);
      String columnLabel = md.getColumnLabel(jdbcField);

      if (schemaName == null || "".equals(schemaName))
        schemaName = tableName;

      if ((tableName == null || "".equals(tableName))
          && result.isLastSqlDescribe())
        tableName = "COLUMNS";

      // some information is not available from the ResultSetMetaData
      JdbcColumnMetaData columnMd = null;

      if (qMd == null) {
        JdbcConnectionResource conn = getConnection(env).validateConnection();

        // php/141p
        JdbcTableMetaData tableMd
          = conn.getTableMetaData(env, catalogName, null, tableName);

        if (tableMd != null)
          columnMd = tableMd.getColumn(columnName);
      }

      // XXX: maxlen note from PHP comments:
      // the length of the longest value for that field in the returned dataset,
      // NOT the maximum length of data that column is designed to hold.

      int maxLength = 0;
      int notNull = md
          .isNullable(jdbcField) == ResultSetMetaData.columnNullable ? 0 : 1;
      int numeric = JdbcColumnMetaData.isNumeric(jdbcColumnType) ? 1 : 0;
      int blob = JdbcColumnMetaData.isBlob(jdbcColumnType) ? 1 : 0;
      String type = result.getFieldType(fieldOffset, jdbcColumnType);
      int unsigned = md.isSigned(jdbcField) ? 0 : numeric;

      if (jdbcColumnType == Types.BOOLEAN || jdbcColumnType == Types.BIT)
        unsigned = 0;
      else if (jdbcColumnType == Types.DECIMAL)
        numeric = 1;

      int zerofill = 0;
      int primaryKey = 0;
      int multipleKey = 0;
      int uniqueKey = 0;

      if (qMd != null) {
        zerofill = qMd.isZeroFill(jdbcField) ? 1 : 0;
        primaryKey = qMd.isPrimaryKey(jdbcField) ? 1 : 0;
        multipleKey = qMd.isMultipleKey(jdbcField) ? 1 : 0;
        uniqueKey = qMd.isUniqueKey(jdbcField) ? 1 : 0;
        notNull = qMd.isNotNull(jdbcField) ? 1 : 0;
        // maxLength = qMd.getLength(jdbcField);
      }
      else if (columnMd != null) {
        zerofill = columnMd.isZeroFill() ? 1 : 0;
        primaryKey = columnMd.isPrimaryKey() ? 1 : 0;
        // XXX: not sure what multipleKey is supposed to be
        // multipleKey = columnMd.isIndex() && !columnMd.isPrimaryKey() ? 1 : 0;
        uniqueKey = columnMd.isUnique() ? 1 : 0;
      }
      else
        notNull = 1;

      ObjectValue fieldResult = env.createObject();

      fieldResult.putThisField(env, SV_NAME, env.createString(columnLabel));
      fieldResult.putThisField(env, SV_TABLE, env.createString(tableName));
      fieldResult.putThisField(env, SV_DEF, env.getEmptyString());
      fieldResult.putThisField(env, SV_MAX_LENGTH,
                               LongValue.create(maxLength));
      fieldResult.putThisField(env, SV_NOT_NULL,
                               LongValue.create(notNull));
      fieldResult.putThisField(env, SV_PRIMARY_KEY,
                               LongValue.create(primaryKey));
      fieldResult.putThisField(env, SV_MULTIPLE_KEY,
                               LongValue.create(multipleKey));
      fieldResult.putThisField(env, SV_UNIQUE_KEY,
                               LongValue.create(uniqueKey));
      fieldResult.putThisField(env, SV_NUMERIC,
                               LongValue.create(numeric));
      fieldResult.putThisField(env, SV_BLOB,
                               LongValue.create(blob));
      fieldResult.putThisField(env, SV_TYPE, env.createString(type));
      fieldResult.putThisField(env, SV_UNSIGNED,
                               LongValue.create(unsigned));
      fieldResult.putThisField(env, SV_ZEROFILL, LongValue.create(zerofill));

      return fieldResult;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Executes a query and returns a result set.
   *
   * Returns true on update success, false on failure, and a result set
   * for a successful select
   */
  public static Value mysql_query(Env env,
                                  StringValue sql,
                                  @Optional Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    return conn.query(env, sql, MYSQL_STORE_RESULT);
  }

  /**
   * Returns an array of lengths.
   */
  public static Value mysql_fetch_lengths(Env env, @NotNull MysqliResult result)
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.fetch_lengths();
  }

  /**
   * Returns an object with properties that correspond to the fetched row
   * and moves the data pointer ahead.
   */
  public static Value mysql_fetch_object(Env env, @NotNull MysqliResult result)
  {
    if (result == null)
      return BooleanValue.FALSE;

    Value value = result.fetch_object(env);

    // php/142t
    // must return FALSE for mediawiki
    if (value.isNull())
      value = BooleanValue.FALSE;

    return value;
  }

  /**
   * Returns a numerical row from the result, FALSE if no more rows.
   */
  @ReturnNullAsFalse
  public static ArrayValue mysql_fetch_row(
      Env env, @NotNull MysqliResult result)
  {
    if (result == null)
      return null;

    return result.fetch_row(env);
  }

  /**
   * Returns the field flags of the specified field.  The flags
   * are reported as a space separated list of words, the returned
   * value can be split using explode().
   *
   * The following flages are reported, older version of MySQL
   * may not report all flags:
   * <ul>
   * <li> not_null
   * <li> primary_key
   * <li> multiple_key
   * <li> blob
   * <li> unsigned
   * <li> zerofill
   * <li> binary
   * <li> enum
   * <li> auto_increment
   * <li> timestamp
   * </ul>
   */
  public static Value mysql_field_flags(Env env,
                                 @NotNull MysqliResult result,
                                 int fieldOffset)
  {
    if (result == null)
      return BooleanValue.FALSE;

    Value fieldName = result.getFieldName(env, fieldOffset);

    if (fieldName == BooleanValue.FALSE)
      return BooleanValue.FALSE;

    Value fieldTable = result.getFieldTable(env, fieldOffset);
    Value fieldJdbcType = result.getJdbcType(fieldOffset);
    String fieldMysqlType = result.getMysqlType(fieldOffset);

    if ((fieldTable == BooleanValue.FALSE)
        || (fieldJdbcType == BooleanValue.FALSE)
        || (fieldMysqlType == null))
      return BooleanValue.FALSE;

    String sql = "SHOW FULL COLUMNS FROM "
        + fieldTable.toString() + " LIKE \'" + fieldName.toString() + "\'";

    Mysqli conn = getConnection(env);

    Value resultV = conn.validateConnection().realQuery(env, sql);

    Object metaResult = resultV.toJavaObject();

    if (metaResult instanceof MysqliResult)
      return ((MysqliResult) metaResult).getFieldFlagsImproved(
        env,
        fieldJdbcType.toInt(),
        fieldMysqlType);

    return BooleanValue.FALSE;
  }

  /**
   * Returns field name at given offset.
   */
  public static Value mysql_field_name(Env env,
                                @NotNull MysqliResult result,
                                int fieldOffset)
  {
    if (result == null)
      return BooleanValue.FALSE;

    // XXX : This method can't detect when mysql_field_name()
    // is invoked with just 2 arguments instead of 3. The
    // value 0 is passed for fieldOffsetValue when only
    // two arguments are found. It is not possible to change
    // to a Value argument since a NullValue is passed for
    // both the missing argument and NULL literal argument
    // cases. Also, Vlaue.isset() can't be used since it
    // returns false for the default NULL and the literal.

    return result.getFieldName(env, fieldOffset);
  }

  /**
   * Deprecated alias for mysql_field_name.
   */

  public static Value mysql_fieldname(Env env,
                               @NotNull MysqliResult result,
                               int fieldOffset)
  {
    return mysql_field_name(env, result, fieldOffset);
  }

  /**
   * Seeks to the specified field offset, the field offset is
   * is used as the default for the next call to {@link #mysql_fetch_field}.
   */
  public static boolean mysql_field_seek(Env env,
                                  @NotNull MysqliResult result,
                                  int fieldOffset)
  {
    if (result == null)
      return false;

    return result.field_seek(env, fieldOffset);
  }

  /**
   * Returns the table corresponding to the field.
   */
  public static Value mysql_field_table(Env env,
                                 @NotNull MysqliResult result,
                                 int fieldOffset)
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.getFieldTable(env, fieldOffset);
  }

  /**
   * Deprecated alias for mysql_field_table.
   */
  public static Value mysql_fieldtable(Env env,
                                 @NotNull MysqliResult result,
                                 int fieldOffset)
  {
    return mysql_field_table(env, result, fieldOffset);
  }

  /**
   * Returns the field type.
   */
  public static Value mysql_field_type(Env env,
                                       @NotNull MysqliResult result,
                                       Value fieldOffset)
  {
    if (result == null) {
      return NullValue.NULL;
    }

    if (! fieldOffset.isset())
      return NullValue.NULL;

    return result.getFieldType(env, fieldOffset.toInt());
  }

  /**
   * Deprecated alias for mysql_field_type.
   */
  public static Value mysql_fieldtype(Env env,
                                     @NotNull MysqliResult result,
                                     Value fieldOffset)
  {
    return mysql_field_type(env, result, fieldOffset);
  }

  /**
   * Returns the length of the specified field
   */
  public static Value mysql_field_len(Env env,
                                      @NotNull MysqliResult result,
                                      @Optional("0") int fieldOffset)
  {
    // gallery2 calls this function with 1 arg, so fieldOffset is optional

    if (result == null)
      return BooleanValue.FALSE;

    // ERRATUM: Returns 10 for datatypes DEC and NUMERIC instead of 11

    return result.getFieldLength(env, fieldOffset);
  }

  /**
   * Frees a mysql result.
   */
  public static boolean mysql_free_result(@NotNull MysqliResult result)
  {
    if (result == null)
      return false;

    result.close();

    return true;
  }

  /**
   * Alias for mysql_free_result.
   */
  public static boolean mysql_freeresult(@NotNull MysqliResult result)
  {
    return mysql_free_result(result);
  }

  /**
   * Returns the MySQL client version.
   */
  public static StringValue mysql_get_client_info(Env env)
  {
    return Mysqli.getClientInfo(env);
  }

  /**
   * Returns a string describing the host.
   */
  public static StringValue mysql_get_host_info(Env env, @Optional Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    return conn.get_host_info(env);
  }

  /**
   * Returns an integer respresenting the MySQL protocol
   * version.
   */
  public static int mysql_get_proto_info(Env env, @Optional Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    return conn.get_proto_info();
  }

  /**
   * Returns the MySQL server version.
   */

  public static Value mysql_get_server_info(Env env, @Optional Mysqli conn)
  {
    if (conn == null) {
      conn = getConnection(env);
    }

    if (conn != null && conn.isConnected())
      return conn.get_server_info(env);
    else
      return NullValue.NULL;
  }

  /**
   * returns ID generated for an AUTO_INCREMENT column by the previous
   * INSERT query on success, 0 if the previous query does not generate
   * an AUTO_INCREMENT value, or FALSE if no MySQL connection was established
   */
  public static Value mysql_insert_id(Env env, @Optional Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    return conn.insert_id(env);
  }

  /**
   * Returns a result pointer containing the
   * databases available from the current mysql daemon.
   */
  public static Value mysql_list_dbs(Env env,
                                     @Optional Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    // php/1451
    // need to return results having column name 'Database' for typo3
    //
    // MySQL Connector/J 5.x returns 'SCHEME_NAME' as the column name
    // for "SHOW DATABASES", while 3.x returns 'Database'
    return mysql_query(env,
                       env.createString(
                           "SELECT SCHEMA_NAME AS 'Database' "
                               + "FROM information_schema.SCHEMATA"),
                       conn);
  }

  /**
   * Retrieves information about the given table name.
   * A Result on success, FALSE on failure.
   */
  public static Value mysql_list_fields(Env env,
                                        String database,
                                        StringValue tableName,
                                        @Optional Mysqli conn)
  {
    // php/141c
    // php gives warnings when the table doesn't exist or is an
    // empty string/null, but not when the database doesn't exist

    if (database == null || database.length() == 0)
      return BooleanValue.FALSE;

    if (tableName.length() == 0) {
      env.warning(L.l("Tablename cannot be empty"));

      return BooleanValue.FALSE;
    }

    if (conn == null)
      conn = getConnection(env);

    if (! conn.select_db(database))
      return BooleanValue.FALSE;

    Value result = conn.query(env,
                              env.createString(
                                  "SELECT * FROM " + tableName + " WHERE NULL"),
                              1);

    if (result == BooleanValue.FALSE)
      env.warning(L.l("Table '{0}' does not exist", tableName));

    return result;
  }

  /**
   * Deprecated alias for mysql_list_fields
   */

  public static Value mysql_listfields(Env env,
                                        String databaseName,
                                        StringValue tableName,
                                        @Optional Mysqli conn)
  {
    return mysql_list_fields(env, databaseName, tableName, conn);
  }

  /**
   * Returns result set or false on error
   */
  public static Value mysql_db_query(Env env,
                                     String databaseName,
                                     StringValue query,
                                     @Optional Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    if (! conn.select_db(databaseName))
      return BooleanValue.FALSE;

    return conn.query(env, query, 1);
  }

  /**
   * Selects the database
   */
  public static boolean mysql_select_db(Env env,
                                         String dbName,
                                         @Optional Mysqli conn)
  {
    if (dbName == null || dbName.length() == 0)
      return false;

    if (conn == null)
      conn = getConnection(env, dbName);

    return conn.select_db(dbName);
  }

  /**
   * Retrieves a list of table names from a MySQL database.
   */
  public static Object mysql_list_tables(Env env,
                                  StringValue databaseName,
                                  @Optional Mysqli conn)
  {
    return mysql_query(env,
                       env.createString("SHOW TABLES FROM " + databaseName),
                       conn);
  }

  /**
   * Get number of fields in result
   */

  public static Value mysql_num_fields(Env env, @NotNull MysqliResult result)
  {
    if (result == null)
      return BooleanValue.FALSE;

    return LongValue.create(result.num_fields());
  }

  /**
   * Deprecated alias for mysql_num_fields
   */
  public static Value mysql_numfields(Env env, @NotNull MysqliResult result)
  {
    return mysql_num_fields(env, result);
  }

  /**
   * Retrieves the number of rows in a result set.
   */
  public static Value mysql_num_rows(Env env, @NotNull MysqliResult result)
  {
    if (result == null)
      return BooleanValue.FALSE;

    return LongValue.create(result.num_rows());
  }

  /**
   * Undocumented alias for {#link #mysql_num_rows}.
   */
  public static Value mysql_numrows(Env env, @NotNull MysqliResult result)
  {
    return mysql_num_rows(env, result);
  }

  /**
   * Returns a new persistent mysql connection.  JDBC has its own pooling
   * so don't need to do anything different from regular mysql_connect().
   */
  public static Value mysql_pconnect(Env env,
                                     @Optional StringValue server,
                                     @Optional StringValue user,
                                     @Optional StringValue password,
                                     @Optional boolean newLink,
                                     @Optional int flags)
  {
    Value value = mysql_connect(env, server, user, password, newLink, flags);

    Mysqli conn = (Mysqli) env.getSpecialValue("caucho.mysql");

    if (conn != null && conn.isConnected())
      conn.setPersistent();

    return value;
  }

  /**
   * Returns a new mysql connection.
   */
  public static Value mysql_connect(
      Env env,
      @Optional StringValue host,
      @Optional StringValue userName,
      @Optional StringValue password,
      @Optional boolean isNewLink,
      @Optional int flags) {
    int port = 3306;
    String socketStr = "";
    String hostStr = host.toString();
    int length = host.length();

    if (length == 0) {
      hostStr = env.getIniString("mysql.default_host");
      if (hostStr == null)
        hostStr = "localhost";
    }

    // host string could contain just a host name,
    // or it could contain a host name
    // and a port number, or it could contain
    // a host name and local socket name, or all 3.
    //
    // "localhost"
    // "localhost:3306"
    // ":3306"
    // ":/tmp/mysql.sock"
    // "localhost:/tmp/mysql.sock"
    // "localhost:3306:/tmp/mysql.sock"

    int sepIndex = hostStr.indexOf(':');

    if (sepIndex > -1) {
      String tmp;
      String portStr;

      tmp = hostStr;
      hostStr = tmp.substring(0, sepIndex);
      if (hostStr.length() == 0) {
        hostStr = "localhost";
      }
      sepIndex++;
      tmp = tmp.substring(sepIndex);

      if ((tmp.length() > 0) && (tmp.charAt(0) != '/')) {
        sepIndex = tmp.indexOf(':');

        if (sepIndex > -1) {
          portStr = tmp.substring(0, sepIndex);
          sepIndex++;
          socketStr = tmp.substring(sepIndex);
        } else {
          portStr = tmp;
        }

        try {
          port = Integer.parseInt(portStr);
        } catch (NumberFormatException ex) {
          // Use default port in case of invalid port number
        }
      } else {
        socketStr = tmp;
      }
    }

    /*
    String catalog = (String) env.getQuercus().getSpecial("mysql.catalog");

    if (catalog == null)
      catalog = "";
    */

    Mysqli mysqli = new MysqliResource(env, hostStr, userName.toString(),
                                       password.toString(), "",
                                       port, socketStr, flags,
                                       null, null, isNewLink);

    if (! mysqli.isConnected())
      return BooleanValue.FALSE;

    Value value = env.wrapJava(mysqli);

    env.setSpecialValue("caucho.mysql", mysqli);

    return value;
  }

  /**
   * Checks if the connection is still valid.
   */
  public static boolean mysql_ping(Env env, @Optional Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    return conn.ping(env);
  }

  /**
   * Returns a string with the status of the connection
   * or NULL if error.
   */
  public static Value mysql_stat(Env env, @Optional Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    Value result = conn.stat(env);

    return result == BooleanValue.FALSE ? NullValue.NULL : result;
  }

  /**
   * Retrieves the table name corresponding to a field, using
   * a result return by {@link #mysql_list_tables}.
   */
  public static Value mysql_tablename(Env env,
                               @NotNull MysqliResult result,
                               int i)
  {
    if (result == null)
      return BooleanValue.FALSE;

    return result.getResultField(env, i, LongValue.ZERO);
  }

  /**
   * Queries the database.
   */
  public static Object mysql_unbuffered_query(Env env,
                                       @NotNull StringValue name,
                                       @Optional Mysqli conn)
  {
    // An "unbuffered" query is a performance optimization
    // for large data sets. Mysql will lock the table in
    // question until all rows are read by the client.
    // It is unclear how this would be implemented on top
    // of Connector/J.

    return mysql_query(env, name, conn);
  }

  /**
   * Query an identifier that corresponds to this specific
   * connection. Mysql calls this integer identifier a
   * thread, but it is really a connection identifier.
   */
  public static Value mysql_thread_id(Env env,
                                      @Optional Mysqli conn)
  {
    if (conn == null)
      conn = getConnection(env);

    return conn.thread_id(env);
  }

  //@todo mysql_list_processes()
  //@todo mysql_set_charset()

  private static Mysqli getConnection(Env env)
  {
    return getConnection(env, "");
  }

  private static Mysqli getConnection(Env env, String db)
  {
    Mysqli conn = (Mysqli) env.getSpecialValue("caucho.mysql");

    // php/1436
    if (conn != null)
      return conn;

    conn = new MysqliResource(env,
                              env.getEmptyString(),
                              env.getEmptyString(), env.getEmptyString(),
                              db, 3306,
                              env.getEmptyString());

    env.setSpecialValue("caucho.mysql", conn);

    return conn;
  }
}

