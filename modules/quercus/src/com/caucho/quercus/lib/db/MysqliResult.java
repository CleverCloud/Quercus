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

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ResourceType;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.*;
import com.caucho.util.L10N;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * mysqli object oriented API facade
 */
@ResourceType("mysql result")
public class MysqliResult extends JdbcResultResource {
  private static final Logger log
    = Logger.getLogger(MysqliResult.class.getName());
  private static final L10N L
    = new L10N(MysqliResult.class);

  private int _resultSetSize;

  /**
   * Constructor for MysqliResult
   *
   * @param stmt the corresponding statement
   * @param rs the corresponding result set
   * @param conn the corresponding connection
   */
  public MysqliResult(Env env,
                      Statement stmt,
                      ResultSet rs,
                      Mysqli conn)
  {
    super(env, stmt, rs, conn);

    // getNumRows() is efficient for MySQL
    _resultSetSize = getNumRows();
  }

  /**
   * Constructor for MysqliResult
   *
   * @param metaData the corresponding result set meta data
   * @param conn the corresponding connection
   */
  public MysqliResult(Env env,
                      ResultSetMetaData metaData,
                      Mysqli conn)
  {
    super(env, metaData, conn);
  }

  public String getResourceType()
  {
    return "mysql result";
  }


  public boolean isLastSqlDescribe()
  {
    return ((Mysqli) getConnection()).isLastSqlDescribe();
  }

  /**
   * Seeks to an arbitrary result pointer specified
   * by the offset in the result set represented by result.
   *
   * @param env the PHP executing environment
   * @param rowNumber the row offset
   * @return true on success or false on failure
   */
  public boolean data_seek(Env env, int rowNumber)
  {
    if (seek(env, rowNumber)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Fetch a result row as an associative, a numeric array, or both.
   *
   * @param type one of MYSQLI_ASSOC, MYSQLI_NUM, or MYSQLI_BOTH (default)
   * By using the MYSQLI_ASSOC constant this function will behave
   * identically to the mysqli_fetch_assoc(), while MYSQLI_NUM will
   * behave identically to the mysqli_fetch_row() function. The final
   * option MYSQLI_BOTH will create a single array with the attributes
   * of both.
   *
   * @return a result row as an associative, a numeric array, or both
   * or null if there are no more rows in the result set
   */
  @ReturnNullAsFalse
  public ArrayValue fetch_array(Env env,
                                @Optional("MYSQLI_BOTH") int type)
  {
    if (type != MysqliModule.MYSQLI_ASSOC
            && type != MysqliModule.MYSQLI_BOTH
            && type != MysqliModule.MYSQLI_NUM) {
      env.warning(L.l("invalid result_type"));
      return null;
    }

    return fetchArray(env, type);
  }

  /**
   * Returns an associative array representing the row.
   *
   * @return an associative array representing the row
   * or null if there are no more rows in the result set
   */
  public ArrayValue fetch_assoc(Env env)
  {
    return fetchArray(env, JdbcResultResource.FETCH_ASSOC);
  }

  /**
   * Returns field metadata for a single field.
   *
   * @param env the PHP executing environment
   * @param offset the field number
   * @return the field metadata or false if no field
   * information for specified offset is available
   */
  public Value fetch_field_direct(Env env, int offset)
  {
    return fetchFieldDirect(env, offset);
  }

  /**
   * Returns the next field in the result set.
   *
   * @param env the PHP executing environment
   * @return the next field in the result set or
   * false if no information is available
   */
  public Value fetch_field(Env env)
  {
    return fetchNextField(env);
  }

  /**
   * Returns metadata for all fields in the result set.
   *
   * @param env the PHP executing environment
   * @return an array of objects which contains field
   * definition information or FALSE if no field
   * information is available
   */
  public Value fetch_fields(Env env)
  {
    return getFieldDirectArray(env);
  }

  /**
   * Returns the lengths of the columns of the
   * current row in the result set.
   *
   * @return an array with the lengths of the
   * columns of the current row in the result set
   * or false if you call it before calling
   * mysqli_fetch_row/array/object or after
   * retrieving all rows in the result set
   */
  public Value fetch_lengths()
  {
    return getLengths();
  }

  /**
   * Returns an object representing the current row.
   *
   * @param env the PHP executing environment
   * @return an object that corresponds to the
   * fetched row or NULL if there are no more
   * rows in resultset
   */
  public Value fetch_object(Env env)
  {
    return fetchObject(env);
  }

  /**
   * Returns a numerical array representing the current row.
   *
   * @return an array that corresponds to the
   * fetched row or NULL if there are no more
   * rows in result set
   */
  public ArrayValue fetch_row(Env env)
  {
    return fetchArray(env, JdbcResultResource.FETCH_NUM);
  }

  /**
   * Returns the number of fields in the result set.
   *
   * @param env the PHP executing environment
   * @return the number of fields in the result set
   */
  public int field_count(Env env)
  {
    return getFieldCount();
  }

  /**
   * returns an object containing the following field information:
   *
   * name: The name of the column
   * orgname: The original name if an alias was specified
   * table: The name of the table
   * orgtable: The original name if an alias was specified
   * def: default value for this field, represented as a string
   * max_length: The maximum width of the field for the result set
   * flags: An integer representing the bit-flags for the field (see _constMap).
   * type: The data type used for this field (an integer... also see _constMap)
   * decimals: The number of decimals used (for integer fields)
   *
   * @param env the PHP executing environment
   * @param fieldOffset 0 <= fieldOffset < number of fields
   * @return an object or BooleanValue.FALSE
   */
  protected Value fetchFieldDirect(Env env,
                                   int fieldOffset)
  {
    if (! isValidFieldOffset(fieldOffset)) {
      // php/1f77 : No warning printed for invalid index

      return BooleanValue.FALSE;
    }

    try {
      ResultSetMetaData md = getMetaData();

      if (md == null)
        return BooleanValue.FALSE;

      int offset = fieldOffset + 1;

      if (offset < 1 || md.getColumnCount() < offset)
        return BooleanValue.FALSE;

      int jdbcType = md.getColumnType(offset);
      String catalogName = md.getCatalogName(offset);
      String fieldTable = md.getTableName(offset);
      String fieldSchema = md.getSchemaName(offset);
      String fieldName = md.getColumnName(offset);
      String fieldAlias = md.getColumnLabel(offset);
      String fieldMysqlType = md.getColumnTypeName(offset);
      int fieldLength = md.getPrecision(offset);
      int fieldScale = md.getScale(offset);

      if (fieldTable == null || "".equals(fieldTable)) {
        return fetchFieldImproved(env, md, offset);
      }

      String sql = "SHOW FULL COLUMNS FROM "
          + fieldTable + " LIKE \'" + fieldName + "\'";

      MysqliResult metaResult;

      metaResult = ((Mysqli) getConnection()).metaQuery(env,
                                                        sql,
                                                        catalogName);

      if (metaResult == null) {
        return fetchFieldImproved(env, md, offset);
      }

      return metaResult.fetchFieldImproved(env,
                                           fieldLength,
                                           fieldAlias,
                                           fieldName,
                                           fieldTable,
                                           jdbcType,
                                           fieldMysqlType,
                                           fieldScale);
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  protected Value fetchFieldImproved(Env env,
                                     ResultSetMetaData md,
                                     int offset)
  {
    Value result = env.createObject();

    try {
      int jdbcType = md.getColumnType(offset);
      String catalogName = md.getCatalogName(offset);
      String fieldTable = md.getTableName(offset);
      String fieldSchema = md.getSchemaName(offset);
      String fieldName = md.getColumnName(offset);
      String fieldAlias = md.getColumnLabel(offset);
      String mysqlType = md.getColumnTypeName(offset);
      int fieldLength = md.getPrecision(offset);
      int scale = md.getScale(offset);

      if ((fieldTable == null || "".equals(fieldTable))
          && ((Mysqli) getConnection()).isLastSqlDescribe())
        fieldTable = "COLUMNS";

      result.putField(env, "name", env.createString(fieldAlias));
      result.putField(env, "orgname", env.createString(fieldName));
      result.putField(env, "table", env.createString(fieldTable));
      //XXX: orgtable same as table
      result.putField(env, "orgtable", env.createString(fieldTable));

      result.putField(env, "def", env.createString(""));

      // "max_length" is the maximum width of this field in this
      // result set.

      result.putField(env, "max_length", LongValue.ZERO);

      // "length" is the width of the field defined in the table
      // declaration.

      result.putField(env, "length", LongValue.create(fieldLength));

      //generate flags
      long flags = 0;

      result.putField(env, "flags", LongValue.create(flags));

      //generate PHP type
      int quercusType = 0;
      switch (jdbcType) {
      case Types.DECIMAL:
        quercusType = MysqliModule.MYSQLI_TYPE_DECIMAL;
        break;
      case Types.BIT:
        // Connector-J enables the tinyInt1isBit property
        // by default and converts TINYINT to BIT. Use
        // the mysql type name to tell the two apart.

        if (mysqlType.equals("BIT")) {
          quercusType = MysqliModule.MYSQLI_TYPE_BIT;
        } else {
          quercusType = MysqliModule.MYSQLI_TYPE_TINY;
        }
        break;
      case Types.SMALLINT:
        quercusType = MysqliModule.MYSQLI_TYPE_SHORT;
        break;
      case Types.INTEGER: {
        if (! isInResultString(2, "medium"))
          quercusType = MysqliModule.MYSQLI_TYPE_LONG;
        else
          quercusType = MysqliModule.MYSQLI_TYPE_INT24;
        break;
      }
      case Types.REAL:
        quercusType = MysqliModule.MYSQLI_TYPE_FLOAT;
        break;
      case Types.DOUBLE:
        quercusType = MysqliModule.MYSQLI_TYPE_DOUBLE;
        break;
      case Types.BIGINT:
        quercusType = MysqliModule.MYSQLI_TYPE_LONGLONG;
        break;
      case Types.DATE:
        if (mysqlType.equals("YEAR")) {
          quercusType = MysqliModule.MYSQLI_TYPE_YEAR;
        } else {
          quercusType = MysqliModule.MYSQLI_TYPE_DATE;
        }
        break;
      case Types.TINYINT:
        quercusType = MysqliModule.MYSQLI_TYPE_TINY;
        break;
      case Types.TIME:
        quercusType = MysqliModule.MYSQLI_TYPE_TIME;
        break;
      case Types.TIMESTAMP:
        if (mysqlType.equals("TIMESTAMP")) {
          quercusType = MysqliModule.MYSQLI_TYPE_TIMESTAMP;
        } else {
          quercusType = MysqliModule.MYSQLI_TYPE_DATETIME;
        }
        break;
      case Types.LONGVARBINARY:
      case Types.LONGVARCHAR:
        quercusType = MysqliModule.MYSQLI_TYPE_BLOB;
        break;
      case Types.BINARY:
      case Types.CHAR:
        quercusType = MysqliModule.MYSQLI_TYPE_STRING;
        break;
      case Types.VARBINARY:
      case Types.VARCHAR:
        quercusType = MysqliModule.MYSQLI_TYPE_VAR_STRING;
        break;
        // XXX: may need to revisit default
      default:
        quercusType = MysqliModule.MYSQLI_TYPE_NULL;
        break;
      }

      result.putField(env, "type", LongValue.create(quercusType));
      result.putField(env, "decimals", LongValue.create(scale));

      // The "charsetnr" field is an integer identifier
      // for the character set used to encode the field.
      // This integer is sent by the server to the JDBC client
      // and is stored as com.mysql.jdbc.Field.charsetIndex,
      // but this field is private and the class does not provide
      // any means to access the field. There is also no way
      // to lookup the mysql index given a Java or Mysql encoding
      // name.

      result.putField(env, "charsetnr", LongValue.ZERO);
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }

    return result;
  }

  /**
   * Returns an object with the following fields:
   *
   * name: The name of the column
   * orgname: The original name if an alias was specified
   * table: The name of the table
   * orgtable: The original name if an alias was specified
   * def: default value for this field, represented as a string
   * max_length: The maximum width of the field for the result set
   * flags: An integer representing the bit-flags for the field
   * type: An integer respresenting the data type used for this field
   * decimals: The number of decimals used (for integer fields)
   *
   * @param env the PHP executing environment
   * @param fieldLength the field length as defined in the table declaration.
   * @param name the field name
   * @param originalName the field original name
   * @param table the field table name
   * @param type the field type
   * @param scale the field scale
   * @return an object containing field metadata
   */
  protected Value fetchFieldImproved(Env env,
                                     int fieldLength,
                                     String name,
                                     String originalName,
                                     String table,
                                     int jdbcType,
                                     String mysqlType,
                                     int scale)
  {
    Value result = env.createObject();

    try {
      ResultSetMetaData md = getMetaData();

      if (! _rs.next())
        return BooleanValue.FALSE;

      result.putField(env, "name", env.createString(name));
      result.putField(env, "orgname", env.createString(originalName));
      result.putField(env, "table", env.createString(table));
      //XXX: orgtable same as table
      result.putField(env, "orgtable", env.createString(table));

      result.putField(env, "def", env.createString(_rs.getString(6)));

      // "max_length" is the maximum width of this field in this
      // result set.

      result.putField(env, "max_length", LongValue.ZERO);

      // "length" is the width of the field defined in the table
      // declaration.

      result.putField(env, "length", LongValue.create(fieldLength));

      //generate flags
      long flags = 0;

      if (! isInResultString(4, "YES"))
        flags += MysqliModule.NOT_NULL_FLAG;

      if (isInResultString(5, "PRI")) {
        flags += MysqliModule.PRI_KEY_FLAG;
        flags += MysqliModule.PART_KEY_FLAG;
      }

      if (isInResultString(5, "MUL")) {
        flags += MysqliModule.MULTIPLE_KEY_FLAG;
        flags += MysqliModule.PART_KEY_FLAG;
      }

      if (isInResultString(2, "blob")
          || (jdbcType == Types.LONGVARCHAR)
          || (jdbcType == Types.LONGVARBINARY))
        flags += MysqliModule.BLOB_FLAG;

      if (isInResultString(2, "unsigned"))
        flags += MysqliModule.UNSIGNED_FLAG;

      if (isInResultString(2, "zerofill"))
        flags += MysqliModule.ZEROFILL_FLAG;

      // php/1f73 - null check
      if (isInResultString(3, "bin")
          || (jdbcType == Types.LONGVARBINARY)
          || (jdbcType == Types.DATE)
          || (jdbcType == Types.TIMESTAMP))
        flags += MysqliModule.BINARY_FLAG;

      if (isInResultString(2, "enum"))
        flags += MysqliModule.ENUM_FLAG;

      if (isInResultString(7, "auto"))
        flags += MysqliModule.AUTO_INCREMENT_FLAG;

      if (isInResultString(2, "set"))
        flags += MysqliModule.SET_FLAG;

      if ((jdbcType == Types.BIGINT)
          || (jdbcType == Types.BIT)
          || (jdbcType == Types.BOOLEAN)
          || (jdbcType == Types.DECIMAL)
          || (jdbcType == Types.DOUBLE)
          || (jdbcType == Types.REAL)
          || (jdbcType == Types.INTEGER)
          || (jdbcType == Types.SMALLINT))
        flags += MysqliModule.NUM_FLAG;

      result.putField(env, "flags", LongValue.create(flags));

      //generate PHP type
      int quercusType = 0;
      switch (jdbcType) {
      case Types.DECIMAL:
        quercusType = MysqliModule.MYSQLI_TYPE_DECIMAL;
        break;
      case Types.BIT:
        // Connector-J enables the tinyInt1isBit property
        // by default and converts TINYINT to BIT. Use
        // the mysql type name to tell the two apart.

        if (mysqlType.equals("BIT")) {
          quercusType = MysqliModule.MYSQLI_TYPE_BIT;
        } else {
          quercusType = MysqliModule.MYSQLI_TYPE_TINY;
        }
        break;
      case Types.SMALLINT:
        quercusType = MysqliModule.MYSQLI_TYPE_SHORT;
        break;
      case Types.INTEGER: {
        if (! isInResultString(2, "medium"))
          quercusType = MysqliModule.MYSQLI_TYPE_LONG;
        else
          quercusType = MysqliModule.MYSQLI_TYPE_INT24;
        break;
      }
      case Types.REAL:
        quercusType = MysqliModule.MYSQLI_TYPE_FLOAT;
        break;
      case Types.DOUBLE:
        quercusType = MysqliModule.MYSQLI_TYPE_DOUBLE;
        break;
      case Types.BIGINT:
        quercusType = MysqliModule.MYSQLI_TYPE_LONGLONG;
        break;
      case Types.DATE:
        if (mysqlType.equals("YEAR")) {
          quercusType = MysqliModule.MYSQLI_TYPE_YEAR;
        } else {
          quercusType = MysqliModule.MYSQLI_TYPE_DATE;
        }
        break;
      case Types.TINYINT:
        quercusType = MysqliModule.MYSQLI_TYPE_TINY;
        break;
      case Types.TIME:
        quercusType = MysqliModule.MYSQLI_TYPE_TIME;
        break;
      case Types.TIMESTAMP:
        if (mysqlType.equals("TIMESTAMP")) {
          quercusType = MysqliModule.MYSQLI_TYPE_TIMESTAMP;
        } else {
          quercusType = MysqliModule.MYSQLI_TYPE_DATETIME;
        }
        break;
      case Types.LONGVARBINARY:
      case Types.LONGVARCHAR:
        quercusType = MysqliModule.MYSQLI_TYPE_BLOB;
        break;
      case Types.BINARY:
      case Types.CHAR:
        quercusType = MysqliModule.MYSQLI_TYPE_STRING;
        break;
      case Types.VARBINARY:
      case Types.VARCHAR:
        quercusType = MysqliModule.MYSQLI_TYPE_VAR_STRING;
        break;
        // XXX: may need to revisit default
      default:
        quercusType = MysqliModule.MYSQLI_TYPE_NULL;
        break;
      }
      result.putField(env, "type", LongValue.create(quercusType));
      result.putField(env, "decimals", LongValue.create(scale));

      // The "charsetnr" field is an integer identifier
      // for the character set used to encode the field.
      // This integer is sent by the server to the JDBC client
      // and is stored as com.mysql.jdbc.Field.charsetIndex,
      // but this field is private and the class does not provide
      // any means to access the field. There is also no way
      // to lookup the mysql index given a Java or Mysql encoding
      // name.

      result.putField(env, "charsetnr", LongValue.ZERO);
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }

    return result;
  }

  /**
   * Returns the following field flags: not_null,
   * primary_key, multiple_key, blob,
   * unsigned zerofill, binary, enum, auto_increment and timestamp
   * <p/>
   * it does not return the MySQL / PHP flag unique_key
   * <p/>
   * MysqlModule generates a special result set with the appropriate values
   *
   * @return the field flags
   */
  public Value getFieldFlagsImproved(Env env, int jdbcType, String mysqlType)
  {
    try {
      StringBuilder flags = new StringBuilder();

      // php/142r

      if (! _rs.next())
        return BooleanValue.FALSE;

      if (! isInResultString(4, "YES"))
        flags.append("not_null");

      if (isInResultString(5, "PRI")) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("primary_key");
      } else {
        if (isInResultString(5, "MUL")) {
          if (flags.length() > 0)
            flags.append(' ');
          flags.append("multiple_key");
        }
      }

      final boolean isTimestamp = (jdbcType == Types.TIMESTAMP)
          && mysqlType.equals("TIMESTAMP");

      if (isInResultString(2, "blob")
          || (jdbcType == Types.LONGVARCHAR)) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("blob");
      }

      if (isInResultString(2, "unsigned")
          || (jdbcType == Types.BIT && mysqlType.equals("BIT"))
          || isTimestamp) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("unsigned");
      }

      if (isInResultString(2, "zerofill")
          || isTimestamp) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("zerofill");
      }

      if (isInResultString(3, "bin")
          || (jdbcType == Types.BINARY)
          || (jdbcType == Types.LONGVARBINARY)
          || (jdbcType == Types.VARBINARY)
          || (jdbcType == Types.TIME)
          || isTimestamp
          || isInResultString(2, "date")) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("binary");
      }

      if (isInResultString(2, "enum")) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("enum");
      }

      if (isInResultString(2, "set")) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("set");
      }

      if (isInResultString(7, "auto_increment")) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("auto_increment");
      }

      if (isTimestamp) {
        if (flags.length() > 0)
          flags.append(' ');
        flags.append("timestamp");
      }

      return env.createString(flags.toString());
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Get Mysql type string
   *
   * @param fieldOffset the field number (0-based)
   * @return the Mysql type
   */
  protected String getMysqlType(int fieldOffset)
  {
    try {
      ResultSetMetaData md = getMetaData();

      return md.getColumnTypeName(fieldOffset + 1);
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return null;
    }
  }

  /**
   * Given the JDBC type of the field at the given offset,
   * return a PHP type string.
   */

  protected String getFieldType(int fieldOffset, int jdbcType)
  {
    if (jdbcType == Types.TIMESTAMP) {
      // The Mysql types DATETIME and TIMESTAMP both map to Types.TIMESTAMP

      String mysqlType = getMysqlType(fieldOffset);

      if (mysqlType.equals("TIMESTAMP")) {
        return TIMESTAMP;
      } else {
        return DATETIME;
      }
    } else if (jdbcType == Types.DATE) {
      // The Mysql types DATE and YEAR both map to Types.DATE

      String mysqlType = getMysqlType(fieldOffset);

      if (mysqlType.equals("YEAR")) {
        return YEAR;
      } else {
        return DATE;
      }
    } else {
      return super.getFieldType(fieldOffset, jdbcType);
    }
  }

  /**
   * @see Value fetchFieldDirect
   *
   * increments the fieldOffset counter by one;
   *
   * @param env the PHP executing environment
   * @return
   */
  protected Value fetchNextField(Env env)
  {
    int fieldOffset = getFieldOffset();

    Value result = fetchFieldDirect(env, fieldOffset);

    setFieldOffset(fieldOffset + 1);

    return result;
  }

  /**
   * Sets the field metadata cursor to the
   * given offset. The next call to
   * mysqli_fetch_field() will retrieve the
   * field definition of the column associated
   * with that offset.
   *
   * @param env the PHP executing environment
   * @return previous value of field cursor
   */
  public boolean field_seek(Env env, int offset)
  {
    boolean success = setFieldOffset(offset);

    if (! success)
      env.invalidArgument("field", offset);

    return success;
  }

  /**
   * Get current field offset of a result pointer.
   *
   * @param env the PHP executing environment
   * @return current offset of field cursor
   */
  public int field_tell(Env env)
  {
    return getFieldOffset();
  }

  /**
   * Closes the result.
   */
  public void free()
  {
    close();
  }

  /**
   * Closes the result
   */
  public void free_result()
  {
    close();
  }

  /**
   *
   * @param env the PHP executing environment
   * @return array of fieldDirect objects
   */
  public Value getFieldDirectArray(Env env)
  {
    ArrayValue array = new ArrayValueImpl();

    try {
      int numColumns = getMetaData().getColumnCount();

      for (int i = 0; i < numColumns; i++) {
        array.put(fetchFieldDirect(env, i));
      }

      return array;
    } catch (SQLException e) {
      log.log(Level.FINE,  e.toString(), e);
      return BooleanValue.FALSE;
    }
  }

  /**
   * Get the number of fields in the result set.
   *
   * @return the number of columns in the result set
   */
  public int num_fields()
  {
    return getFieldCount();
  }

  /**
   * Get the number of rows in the result set.
   *
   * @return the number of rows in the result set
   */
  public int num_rows()
  {
    return _resultSetSize;
  }

  @Override
  protected Value getColumnString(Env env,
                                  ResultSet rs,
                                  ResultSetMetaData md,
                                  int column)
    throws SQLException
  {
    // php/1464, php/144f, php/144g, php/144b

    // The "SET NAMES 'latin1'" in Mysqli is important to make the default
    // encoding sane

    Mysqli mysqli = getMysqli();

    if (rs instanceof QuercusResultSet) {
      QuercusResultSet qRs = (QuercusResultSet) rs;

      int length = qRs.getStringLength(column);

      if (length < 0)
        return NullValue.NULL;

      // XXX: i18n
      StringBuilderValue sb = new StringBuilderValue();
      sb.ensureAppendCapacity(length);

      qRs.getString(column, sb.getBuffer(), sb.getOffset());
      sb.setOffset(sb.getOffset() + length);

      return sb;
    }

    Method getColumnCharacterSetMethod
      = mysqli.getColumnCharacterSetMethod(md.getClass());

    String encoding = null;

    try {
      if (getColumnCharacterSetMethod != null)
        encoding = (String) getColumnCharacterSetMethod.invoke(md, column);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    if (encoding == null) {
      String value = rs.getString(column);

      if (value != null)
        return env.createString(value);
      else
        return NullValue.NULL;
    }

    // calling getString() will decode using the database encoding, so
    // get bytes directly.  Also, getBytes is faster for MySQL since
    // getString converts from bytes to string.

    if ("UTF-8".equals(encoding)) {
      byte []bytes = rs.getBytes(column);

      if (bytes == null)
        return NullValue.NULL;

      StringValue bb = env.createUnicodeBuilder();

      int length = bytes.length;
      int offset = 0;

      bb.appendUtf8(bytes);

      /*
      while (offset < length) {
        int ch = bytes[offset++] & 0xff;

        if (ch < 0x80) {
          bb.append((char) ch);
        }
        else if (ch < 0xe0) {
          int ch2 = bytes[offset++] & 0xff;
          int v = ((ch & 0x1f) << 6) + ((ch2 & 0x3f));

          bb.append((char) MysqlLatin1Utility.decode(v));
        }
        else {
          int ch2 = bytes[offset++] & 0xff;
          int ch3 = bytes[offset++] & 0xff;
          int v = ((ch & 0xf) << 12) + ((ch2 & 0x3f) << 6) + ((ch3 & 0x3f));

          bb.append((char) MysqlLatin1Utility.decode(v));
        }
      }
      */

      return bb;
    }
    else if ("Cp1252".equals(encoding) || "LATIN1".equals(encoding)) {
      byte []bytes = rs.getBytes(column);

      if (bytes == null)
        return NullValue.NULL;

      StringValue bb = env.createUnicodeBuilder();

      bb.append(bytes);

      return bb;
    }
    else {
      String value = rs.getString(column);

      if (value != null)
        return env.createString(value);
      else
        return NullValue.NULL;
    }
  }

  protected Mysqli getMysqli()
  {
    return (Mysqli) getConnection();
  }
}
