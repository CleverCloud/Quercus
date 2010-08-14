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

import com.caucho.quercus.env.*;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Time;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a JDBC Result value.
 */
public class JdbcResultResource {
  private static final Logger log
    = Logger.getLogger(JdbcResultResource.class.getName());
  private static final L10N L = new L10N(JdbcResultResource.class);

  public static final int FETCH_ASSOC = 0x1;
  public static final int FETCH_NUM = 0x2;
  public static final int FETCH_BOTH = FETCH_ASSOC | FETCH_NUM;

  public static final String INTEGER = "int";
  public static final String BLOB = "blob";
  public static final String STRING = "string";
  public static final String DATE = "date";
  public static final String DATETIME = "datetime";
  public static final String REAL = "real";
  public static final String TIME = "time";
  public static final String TIMESTAMP = "timestamp";
  public static final String UNKNOWN = "unknown";
  public static final String YEAR = "year";

  private Statement _stmt;
  protected ResultSet _rs;
  private boolean _isValid;
  private int _fieldOffset;
  private JdbcConnectionResource _conn;
  private Env _env;

  protected ResultSetMetaData _metaData;
  private Value[] _columnNames;

  private int _affectedRows;

  /**
   * Constructor for JdbcResultResource
   *
   * @param stmt the corresponding statement
   * @param rs the corresponding result set
   * @param conn the corresponding connection
   */
  public JdbcResultResource(Env env,
                            Statement stmt,
                            ResultSet rs,
                            JdbcConnectionResource conn)
  {
    _env = env;
    _stmt = stmt;
    _rs = rs;
    _conn = conn;
  }

  /**
   * Constructor for JdbcResultResource
   *
   * @param metaData the corresponding result set meta data
   * @param conn the corresponding connection
   */
  public JdbcResultResource(Env env,
                            ResultSetMetaData metaData,
                            JdbcConnectionResource conn)
  {
    _env = env;

    _metaData = metaData;
    _conn = conn;
    _env = conn.getEnv();
  }

  /**
   * Closes the result set.
   */
  public void close()
  {
    try {
      ResultSet rs = _rs;
      _rs = null;

      Statement stmt = _stmt;
      _stmt = null;

      JdbcConnectionResource conn = _conn;
      _conn = null;

      if (rs != null)
        rs.close();

      // XXX: statement no longer reused?
      if (stmt != null && conn != null)
        conn.closeStatement(stmt);

      _env = null;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Fetch the next line as an array.
   *
   * @param type one of FETCH_ASSOC, FETCH_NUM, or FETCH_BOTH (default)
   * By using the FETCH_ASSOC constant this function will behave
   * identically to the mysqli_fetch_assoc(), while FETCH_NUM will
   * behave identically to the mysqli_fetch_row() function. The final
   * option FETCH_BOTH will create a single array with the attributes
   * of both.
   *
   * @return the next result row as an associative,
   * a numeric array, or both.
   */
  public ArrayValue fetchArray(Env env, int type)
  {
    try {
      if (_rs == null)
        return null;

      if (_rs.next()) {
        _isValid = true;

        ArrayValue array = new ArrayValueImpl();

        ResultSetMetaData md = getMetaData();

        int count = md.getColumnCount();

        if ((type & FETCH_ASSOC) != 0) {
          _columnNames = new Value[count];

          for (int i = 0; i < count; i++) {
            String columnName = md.getColumnLabel(i + 1);

            _columnNames[i] = env.createString(columnName);
          }
        }

        for (int i = 0; i < count; i++) {
          Value value = getColumnValue(env, _rs, md, i + 1);

          if ((type & FETCH_NUM) != 0)
            array.put(LongValue.create(i), value);

          if ((type & FETCH_ASSOC) != 0)
            array.put(_columnNames[i], value);
        }

        return array;
      } else {
        return null;
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return null;
    }
  }

  /**
   * Returns an associative array representing the row.
   *
   * @return an associative array representing the row
   * or null if there are no more rows in the result set
   */
  public ArrayValue fetchAssoc(Env env)
  {
    return fetchArray(env, JdbcResultResource.FETCH_ASSOC);
  }

  /**
   * Returns an object with the following fields: name, table, max_length,
   * not_null, primary_key, multiple_key, numeric,
   * blob, type, unsigned, zerofill.
   * <p/>
   * NOTE: does not have a field for unique_key.
   *
   * @param env the PHP executing environment
   * @param maxLength the field maximum length
   * @param tableName the field table name
   * @param type the field type
   * @return the next field in the result set or
   * false if no information is available
   */
  public Value fetchField(Env env,
                          int maxLength,
                          String tableName,
                          String type)
  {
    if (_rs == null)
      return null;

    ObjectValue result = env.createObject();

    try {
      if (! _isValid) {
        _isValid = true;
        _rs.next();
      }

      result.putField(env, "name", env.createString(_rs.getString(1)));
      result.putField(env, "table", env.createString(tableName));
      result.putField(env, "max_length", LongValue.create(maxLength));

      if (! isInResultString(4, "YES"))
        result.putField(env, "not_null", LongValue.ONE);
      else
        result.putField(env, "not_null", LongValue.ZERO);

      if (isInResultString(5, "PRI"))
        result.putField(env, "primary_key", LongValue.ONE);
      else
        result.putField(env, "primary_key", LongValue.ZERO);

      if (isInResultString(5, "MUL"))
        result.putField(env, "multiple_key", LongValue.ONE);
      else
        result.putField(env, "multiple_key", LongValue.ZERO);

      if (isInResultString(2, "int") || isInResultString(2, "real"))
        result.putField(env, "numeric", LongValue.ONE);
      else
        result.putField(env, "numeric", LongValue.ZERO);

      if (isInResultString(2, "blob"))
        result.putField(env, "blob", LongValue.ONE);
      else
        result.putField(env, "blob", LongValue.ZERO);

      result.putField(env, "type", env.createString(type));

      if (isInResultString(2, "unsigned"))
        result.putField(env, "unsigned", LongValue.ONE);
      else
        result.putField(env, "unsigned", LongValue.ZERO);

      if (isInResultString(2, "zerofill"))
        result.putField(env, "zerofill", LongValue.ONE);
      else
        result.putField(env, "zerofill", LongValue.ZERO);

      return result;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns an object with properties that correspond to the fetched row and
   * moves the internal data pointer ahead.
   *
   * @param env the PHP executing environment
   * @return an object representing the current fetched row
   */
  public Value fetchObject(Env env)
  {
    if (_rs == null)
      return NullValue.NULL;

    try {
      if (_rs.next()) {
        _isValid = true;

        Value result = env.createObject();

        ResultSetMetaData md = getMetaData();

        int count = md.getColumnCount();

        for (int i = 0; i < count; i++) {
          String name = md.getColumnLabel(i + 1);
          Value value = getColumnValue(env, _rs, md, i + 1);

          result.putField(env, name, value);
        }

        return result;

      } else {
        return NullValue.NULL;
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return NullValue.NULL;
    }
  }

  /**
   * Returns an array representing the row.
   *
   * @return an array containing the fecthed row
   */
  public ArrayValue fetchRow(Env env)
  {
    return fetchArray(env, JdbcResultResource.FETCH_NUM);
  }

  /**
   * Get the number of affected rows.
   *
   * @return the number of affected rows
   */
  public int getAffectedRows()
  {
    return _affectedRows;
  }

  /**
   * Gets the column number based on a generic Value.
   *
   * @param fieldNameOrNumber the field index or it's name
   * @param base the numbering base: 0 or 1 (usually zero).
   * @return the column number (always 0-based) or -1 on error
   */
  protected int getColumnNumber(Value fieldNameOrNumber,
                                int base)
    throws SQLException
  {
    int fieldNumber = -1;

    if ((fieldNameOrNumber != null) && fieldNameOrNumber.isLongConvertible()) {
      // fieldNameOrNumber is the field number.
      // Convert it to 0-based.
      fieldNumber = fieldNameOrNumber.toInt() - base;
    }

    if (fieldNumber < 0) {
      // fieldNameOrNumber is the field name
      // Get column number (0-based).
      fieldNumber = getColumnNumber(fieldNameOrNumber.toString());
    }

    return fieldNumber;
  }

  /**
   * Gets the column number.
   *
   * @return the column number (0-based) or -1 on error
   */
  protected int getColumnNumber(String colName)
    throws SQLException
  {
    return getColumnNumber(colName, getMetaData());
  }

  /**
   * Helper function for getResultField returns a 0-based column number
   *
   * @param colName the column name
   * @param rsmd the result set meta data
   * @return the column number (0-based) or -1 on error
   */
  private int getColumnNumber(String colName,
                              ResultSetMetaData rsmd)
    throws SQLException
  {
    int numColumns = rsmd.getColumnCount();

    if (colName.indexOf('.') == -1) {
      for (int i = 1; i <= numColumns; i++) {
        if (colName.equals(rsmd.getColumnLabel(i)))
          return (i - 1);
      }

      return -1;
    }
    else {
      for (int i = 1; i <= numColumns; i++) {
        if (colName.equals(rsmd.getTableName(i) + '.' + rsmd.getColumnLabel(i)))
          return (i - 1);
      }

      return -1;
    }

  }

  /**
   * Get the column value in the specified result set.
   *
   * @param env the PHP executing environment
   * @param rs the result set
   * @param metaData the result set meta data
   * @param column the column number
   * @return the column value
   */
  public Value getColumnValue(Env env,
                              ResultSet rs,
                              ResultSetMetaData metaData,
                              int column)
    throws SQLException
  {
    // Note: typically, the PHP column value is returned as
    // a String, except for binary values.

    try {
      switch (metaData.getColumnType(column)) {
      case Types.NULL:
        return NullValue.NULL;

      case Types.BIT:
        {
          String typeName = metaData.getColumnTypeName(column);
          // Postgres matches BIT for BOOL columns
          if (! typeName.equals("bool")) {
            String value = rs.getString(column);

            if (rs.wasNull())
              return NullValue.NULL;
            else
              return _env.createString(value);
          }
          // else fall to boolean
        }

      case Types.BOOLEAN:
        {
          boolean b = rs.getBoolean(column);
          if (rs.wasNull())
            return NullValue.NULL;
          else
            return env.createString(b ? "t" : "f");
        }

      case Types.TINYINT:
      case Types.SMALLINT:
      case Types.INTEGER:
      case Types.BIGINT:
        {
          long value = rs.getLong(column);

          if (rs.wasNull())
            return NullValue.NULL;
          else
            return _env.createString(String.valueOf(value));
        }
      case Types.REAL:
      case Types.DOUBLE:
        {
          double value = rs.getDouble(column);

          if (rs.wasNull())
            return NullValue.NULL;
          else if (metaData.isCurrency(column)) {
            StringValue sb = _env.createUnicodeBuilder();

            sb.append("$");

            return sb.append(value);
          }
          else if (value == 0.0) {
            StringValue sb = _env.createUnicodeBuilder();

            return sb.append("0");
          }
          else {
            StringValue sb = _env.createUnicodeBuilder();

            return sb.append(value);
          }
        }

      case Types.BLOB:
        {
          Object object = rs.getBlob(column);
          if (object.getClass().getName().equals("oracle.sql.BLOB")) {
            OracleOciLob ociLob = new OracleOciLob((Oracle) _conn,
                                                   OracleModule.OCI_D_LOB);
            ociLob.setLob(object);
            object = ociLob;
          }
          return env.wrapJava(object);
        }

      case Types.CLOB:
        {
          Object object = rs.getClob(column);
          if (object.getClass().getName().equals("oracle.sql.CLOB")) {
            OracleOciLob ociLob = new OracleOciLob((Oracle) _conn,
                                                   OracleModule.OCI_D_LOB);
            ociLob.setLob(object);
            object = ociLob;
          }
          return env.wrapJava(object);
        }

      case Types.LONGVARBINARY:
      case Types.VARBINARY:
      case Types.BINARY:
        {
          StringValue bb = env.createBinaryBuilder();

          InputStream is = rs.getBinaryStream(column);

          if (is == null) // || rs.wasNull())
            return NullValue.NULL;

          try {
            bb.appendReadAll(is, Long.MAX_VALUE / 2);
          } catch (RuntimeException e) {
            log.log(Level.WARNING, e.toString(), e);

            return NullValue.NULL;
          }

          return bb;
        }

      case Types.VARCHAR:
      case Types.LONGVARCHAR:
        if (env.isUnicodeSemantics())
          return getUnicodeColumnString(env, rs, metaData, column);
        else
          return getColumnString(env, rs, metaData, column);

      case Types.TIME:
        return getColumnTime(env, rs, column);

      case Types.TIMESTAMP:
        return getColumnTimestamp(env, rs, column);

      case Types.DATE:
        return getColumnDate(env, rs, column);

      default:
        {
          String strValue = rs.getString(column);

          if (strValue == null) // || rs.wasNull())
            return NullValue.NULL;
          else
            return env.createString(strValue);
        }
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return NullValue.NULL;
    } catch (SQLException e) {
      // php/141e
      log.log(Level.FINE, e.toString(), e);

      return NullValue.NULL;
    }
  }

  protected Value getUnicodeColumnString(Env env,
                                         ResultSet rs,
                                         ResultSetMetaData md,
                                         int column)
    throws IOException, SQLException
  {
    Reader reader = rs.getCharacterStream(column);

    if (reader == null) // || rs.wasNull())
      return NullValue.NULL;

    StringValue bb = env.createUnicodeBuilder();

    bb.append(reader);

    return bb;
  }

  protected Value getColumnString(Env env,
                                  ResultSet rs,
                                  ResultSetMetaData md,
                                  int column)
    throws SQLException
  {
    // php/1464, php/144f, php/144g
    // php/144b

    // calling getString() will decode using the database encoding, so
    // get bytes directly.  Also, getBytes is faster for MySQL since
    // getString converts from bytes to string.
    byte []bytes = rs.getBytes(column);

    if (bytes == null)
      return NullValue.NULL;

    StringValue bb = env.createUnicodeBuilder();

    bb.append(bytes);

    return bb;
  }

  protected Value getColumnTime(Env env, ResultSet rs, int column)
    throws SQLException
  {
    Time time = rs.getTime(column);

    if (time == null)
      return NullValue.NULL;
    else
      return env.createString(String.valueOf(time));
  }

  protected Value getColumnDate(Env env, ResultSet rs, int column)
    throws SQLException
  {
    Date date = rs.getDate(column);

    if (date == null)
      return NullValue.NULL;
    else
      return env.createString(String.valueOf(date));
  }

  protected Value getColumnTimestamp(Env env, ResultSet rs, int column)
    throws SQLException
  {
    try {
      Timestamp timestamp = rs.getTimestamp(column);

      if (timestamp == null)
        return NullValue.NULL;
      else {
        String time = String.valueOf(timestamp);

        // the .0 nanoseconds at the end may not matter, but strip it out
        // anyways to match php (postgresql)
        if (time.endsWith(".0"))
          time = time.substring(0, time.length() - 2);

        return env.createString(time);
      }
    } catch (SQLException e) {
      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, e.toString(), e);

      // php/1f0a - mysql jdbc driver issue with zero timestamp
      return env.createString("0000-00-00 00:00:00");
    }
  }

  /**
   * Get the connection corresponding to this result resource.
   *
   * @return a JDBC connection resource
   */
  public JdbcConnectionResource getConnection()
  {
    return _conn;
  }

  /**
   * Get the field catalog name.
   *
   * @param fieldOffset the field number
   * @return the field catalog name
   */
  public Value getFieldCatalog(int fieldOffset)
  {
    try {
      ResultSetMetaData md = getMetaData();

      if (md.getColumnCount() <= fieldOffset || fieldOffset < 0)
        return BooleanValue.FALSE;
      else
        return _env.createString(md.getCatalogName(fieldOffset + 1));
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns column count.
   *
   * @return the number of columns in the result set
   */
  public int getFieldCount()
  {
    try {
      if (getMetaData() != null)
        return getMetaData().getColumnCount();
      else
        return -1;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return true is the field offset is valid, meaning it
   * is larger than 0 and is less that the max number
   * of fields in this result resource.
   */
  protected boolean isValidFieldOffset(int fieldOffset)
  {
    try {
      ResultSetMetaData md = getMetaData();

      if (fieldOffset < 0 || md.getColumnCount() <= fieldOffset)
        return false;
      else
        return true;
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }
  }

  /**
   * Return true if the String result at the
   * given index of the ResultSet contains
   * the substring.
   */

  protected boolean isInResultString(int columnIndex, String substring)
    throws SQLException
  {
    String resultString = _rs.getString(columnIndex);

    if (resultString == null)
      return false;

    int index = resultString.indexOf(substring);

    if (index == -1)
      return false;
    else
      return true;
  }

  /**
   * Get field length. This is the length of the field
   * as defined in the table declaration.
   *
   * @param env the PHP executing environment
   * @param fieldOffset the field number (0-based)
   * @return length of field for specified column
   */
  public Value getFieldLength(Env env, int fieldOffset)
  {
    try {
      ResultSetMetaData md = getMetaData();

      if (md.getColumnCount() <= fieldOffset || fieldOffset < 0) {
        env.invalidArgument("field", fieldOffset);
        return BooleanValue.FALSE;
      }
      else
        return LongValue.create((long) md.getPrecision(fieldOffset + 1));

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the column name.
   *
   * @param env the PHP executing environment
   * @param fieldOffset 0-based field offset
   *
   * @return a StringValue containing the column name
   */
  public Value getFieldName(Env env, int fieldOffset)
  {

    try {
      ResultSetMetaData md = getMetaData();

      if (md.getColumnCount() <= fieldOffset || fieldOffset < 0) {
        env.invalidArgument("field", fieldOffset);
        return BooleanValue.FALSE;
      }
      else
        return env.createString(md.getColumnLabel(fieldOffset + 1));
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns a StringValue containing the column Alias.
   *
   * @param fieldOffset 0-based field offset
   *
   * @return the column alias
   */
  public Value getFieldNameAlias(int fieldOffset)
  {

    try {
      ResultSetMetaData md = getMetaData();

      if (md.getColumnCount() <= fieldOffset || fieldOffset < 0)
        return BooleanValue.FALSE;
      else
        return _env.createString(md.getColumnLabel(fieldOffset + 1));
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the column name.
   *
   * @param env the PHP executing environment
   * @param fieldOffset 0-based field offset
   *
   * @return int(1) if the column is nullable, int(1) if it is not
   */
  public Value getFieldNotNull(Env env, int fieldOffset)
  {
    try {
      ResultSetMetaData md = getMetaData();

      if (md.getColumnCount() <= fieldOffset || fieldOffset < 0) {
        env.invalidArgument("field", fieldOffset);
        return BooleanValue.FALSE;
      }
      else
        if  (md.isNullable(fieldOffset + 1) == ResultSetMetaData.columnNoNulls)
          return LongValue.ONE;
        else
          return LongValue.ZERO;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Get field offset.
   *
   * @return the current field offset
   */
  public int getFieldOffset()
  {
    return _fieldOffset;
  }

  /**
   * Get field scale.
   *
   * @param fieldOffset the field offset
   * @return number of digits to the right of the decimal point
   */
  public Value getFieldScale(int fieldOffset)
  {
    try {
      ResultSetMetaData md = getMetaData();

      if (md.getColumnCount() <= fieldOffset || fieldOffset < 0)
        return BooleanValue.FALSE;
      else
        return LongValue.create((long) md.getScale(fieldOffset + 1));

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the table corresponding to the field.
   *
   * @param env the PHP executing environment
   * @param fieldOffset the field number
   * @return the field table name
   */
  public Value getFieldTable(Env env, int fieldOffset)
  {
    try {
      ResultSetMetaData md = getMetaData();

      if (md.getColumnCount() <= fieldOffset || fieldOffset < 0) {
        env.invalidArgument("field", fieldOffset);
        return BooleanValue.FALSE;
      }
      else {
        String tableName = md.getTableName(fieldOffset + 1);

        if (tableName == null || tableName.equals(""))
          return BooleanValue.FALSE;
        else
          return env.createString(tableName);
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the table corresponding to the field.
   *
   * @param env the PHP executing environment
   * @param fieldOffset the field number
   * @return the field table name
   */
  public Value getFieldSchema(Env env, int fieldOffset)
  {
    try {
      ResultSetMetaData md = getMetaData();

      if (md.getColumnCount() <= fieldOffset || fieldOffset < 0) {
        env.invalidArgument("schema", fieldOffset);
        return BooleanValue.FALSE;
      }
      else {
        String tableName = md.getSchemaName(fieldOffset + 1);

        if (tableName == null || tableName.equals(""))
          return BooleanValue.FALSE;
        else
          return env.createString(tableName);
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Get a StringValue with the column type.
   *
   * @param env the PHP executing environment
   * @param fieldOffset need to add 1 because java
   * is 1 based index and quercus is 0 based
   *
   * @return a StringValue containing the column type
   */
  public Value getFieldType(Env env, int fieldOffset)
  {
    try {
      ResultSetMetaData md = getMetaData();

      if (md.getColumnCount() <= fieldOffset || fieldOffset < 0) {
        env.invalidArgument("field", fieldOffset);
        return BooleanValue.FALSE;
      }
      else {
        int jdbcType = md.getColumnType(fieldOffset + 1);
        return env.createString(getFieldType(fieldOffset, jdbcType));
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Given the JDBC type of the field at the given offset,
   * return a PHP type string.
   */

  protected String getFieldType(int fieldOffset, int jdbcType)
  {
    switch (jdbcType) {
    case Types.BIGINT:
    case Types.BIT:
    case Types.INTEGER:
    case Types.SMALLINT:
    case Types.TINYINT:
        return INTEGER;

    case Types.LONGVARBINARY:
    case Types.LONGVARCHAR:
        return BLOB;

    case Types.CHAR:
    case Types.VARCHAR:
    case Types.BINARY:
    case Types.VARBINARY:
        return STRING;

    case Types.TIME:
        return TIME;

    case Types.DATE:
        return DATE;

    case Types.TIMESTAMP:
        return DATETIME;

    case Types.DECIMAL:
    case Types.DOUBLE:
    case Types.REAL:
        return REAL;

    default:
        return UNKNOWN;
    }
  }

  /**
   * Returns the underlying SQL statement
   * associated to this result resource.
   */
  protected Statement getJavaStatement()
  {
    return _conn.getEnv().getQuercus().getStatement(getStatement());
  }

  /**
   * Get type from Types enumeration
   *
   * @param fieldOffset the field number (0-based)
   * @return the JDBC type
   */
  protected Value getJdbcType(int fieldOffset)
  {
    try {
      ResultSetMetaData md = getMetaData();

      return LongValue.create(md.getColumnType(fieldOffset + 1));
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns an ArrayValue column lengths in the most
   * recently accessed row. If a fetch function has not
   * yet been called this will return BooleanValue.FALSE
   *
   * @return an ArrayValue of column lengths in the most
   * recently accessed row
   */
  public Value getLengths()
  {
    Value result;
    ArrayValue array = new ArrayValueImpl();

    try {
      ResultSetMetaData md = getMetaData();

      int numColumns = md.getColumnCount();

      for (int i = 1; i <= numColumns; i++) {
        array.put(LongValue.create(_rs.getObject(i).toString().length()));
      }
      result = array;

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }

    return result;
  }

  /**
   * Get the result set meta data.
   *
   * @return the meta data for this result set
   */
  public ResultSetMetaData getMetaData()
    throws SQLException
  {
    if (_metaData != null)
      return _metaData;

    /*
    if (_rs != null && ! _isValid) {
      if (! _rs.next())
        return null;
      _isValid = true;
    }
    */

    if (_metaData == null && _rs != null)
      _metaData = _rs.getMetaData();

    return _metaData;
  }

  /**
   * Returns the number of columns returned in query.
   *
   * @return the number of columns for this result set
   */
  public Value getNumFields()
  {

    try {
      Value result = NullValue.NULL;

      ResultSetMetaData md = getMetaData();

      int count = md.getColumnCount();

      if (count != 0) {
        result = LongValue.create((long) count);
      }

      return result;

    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      return NullValue.NULL;
    }
  }

  /**
   * Get the number of rows in this result set.
   *
   * @return the number of rows in this result set
   */
  public int getNumRows()
  {
    return getNumRows(_rs);
  }

  /**
   * Returns number of rows returned in query.
   * last() call is efficient for Mysql because the driver just adjusts
   * the result index.  It is very inefficient for Postgres because that
   * driver iterates over the result set.
   *
   * @param rs a result set
   * @return the number of rows in the specified result set
   */
  public static int getNumRows(ResultSet rs)
  {
    if (rs == null)
      return -1;

    try {
      int currentRow = rs.getRow();

      try {
        rs.last();
        return rs.getRow();
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
        return -1;
      } finally {
        if (currentRow == 0)
          rs.beforeFirst();
        else
          rs.absolute(currentRow);
      }

    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return -1;
    }
  }

  /**
   * Returns the value at a particular row and column.
   *
   * @param env the PHP executing environment
   * @param row a particular row to get the field value from
   * @param field the field name or number
   * @return the value of the specified field
   */
  public Value getResultField(Env env, int row, Value field)
  {
    try {
      ResultSetMetaData md = getMetaData();

      int colNumber;

      if (field.isNumberConvertible())
        colNumber = field.toInt();
      else
        colNumber = getColumnNumber(field.toString(), md);

      if (colNumber < 0 || colNumber >= md.getColumnCount()) {
        env.invalidArgument("field", field);
        return BooleanValue.FALSE;
      }

      int currentRow = _rs.getRow();

      if ((row < 0) || (!_rs.absolute(row + 1)) || _rs.isAfterLast()) {
        if (currentRow > 0)
          _rs.absolute(currentRow);
        else
          _rs.beforeFirst();

        env.invalidArgument("row", row);
        return BooleanValue.FALSE;
      }

      return getColumnValue(env, _rs, md, colNumber + 1);
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Get the underlying result set.
   *
   * @return the underlying ResultSet object
   */
  public ResultSet getResultSet()
  {
    return _rs;
  }

  /**
   * Get the underlying statement.
   *
   * @return the underlying Statement object
   */
  public Statement getStatement()
  {
    return _stmt;
  }

  /**
   * Seeks to an arbitrary result pointer specified
   * by the offset in the result set represented by result.
   * Returns TRUE on success or FALSE on failure
   *
   * @param env the PHP executing environment
   * @param rowNumber the row offset
   * @return true on success or false on failure
   */
  public boolean seek(Env env, int rowNumber)
  {
    if (setRowNumber(rowNumber))
      return true;

    return false;
  }

  /**
   * Set the number of affected rows to the specified value.
   *
   * @param affectedRows the new number of affected rows
   */
  public void setAffectedRows(int affectedRows) {
    _affectedRows = affectedRows;
  }

  /**
   * Set a value for field offset. This method will
   * return true when the field offset is valid,
   * otherwise it will set the field offset
   * to the invalid value and return false.
   *
   * @param fieldOffset PHP is 0-based
   */
  public boolean setFieldOffset(int fieldOffset)
  {
    _fieldOffset = fieldOffset;

    if (fieldOffset < 0 || fieldOffset >= getNumFields().toInt())
      return false;
    else
      return true;
  }

  /**
   * Points to the row right before "rowNumber".
   * Next fetchArray will increment to proper row.
   *
   * @param rowNumber the row offset
   * @return true on success or false on failure
   */
  public boolean setRowNumber(int rowNumber)
  {
    return setRowNumber(_rs, rowNumber);
  }

  /**
   * Points to the row right before "rowNumber".
   * Next fetchArray will increment to proper row.
   *
   * @param rs the result set to move the row pointer
   * @param rowNumber the row offset
   * @return true on success or false on failure
   */
  public static boolean setRowNumber(ResultSet rs,
                                     int rowNumber)
  {
    // throw error if rowNumber is after last row
    int numRows = getNumRows(rs);

    if (numRows <= rowNumber || rowNumber < 0) {
      return false;
    }

    try {
      if (rowNumber == 0)
        rs.beforeFirst();
      else
        rs.absolute(rowNumber);
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return false;
    }

    return true;
  }

  /**
   * Convert this JDBC result resource to a hash code.
   *
   * @return a hash code of this JDBC result resource
   */
  public Value toKey()
  {
    // XXX: phpbb seems to want this?
    return _env
      .createString("JdbcResultResource$" + System.identityHashCode(this));
  }

  /**
   * Returns a string representation for this object.
   *
   * @return a string representation for this object
   */
  public String toString()
  {
    if (_rs != null)
      return getClass()
        .getSimpleName() +  "[" + _rs.getClass().getSimpleName() + "]";
    else
      return getClass().getSimpleName() +  "[]";
  }

  /**
   * Validate this result set and return it.
   *
   * @return the validated result set
   */
  public JdbcResultResource validateResult()
  {
    return this;
  }
}

