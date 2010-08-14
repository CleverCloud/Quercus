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

package com.caucho.quercus.mysql;

import com.caucho.util.*;
import com.caucho.quercus.lib.db.QuercusResultSetMetaData;
import com.caucho.vfs.*;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import javax.sql.*;

/**
 * Special Quercus Mysql connection.
 */
public class MysqlResultSetMetaData implements QuercusResultSetMetaData {
  private static final Logger log
    = Logger.getLogger(MysqlResultSetMetaData.class.getName());
  private static final L10N L = new L10N(MysqlResultSetMetaData.class);

  private int _columnCount;
  private ArrayList<MysqlColumn> _columns = new ArrayList<MysqlColumn>();

  MysqlResultSetMetaData()
  {
  }

  public int getColumnCount()
  {
    return _columnCount;
  }

  public void setColumnCount(int columnCount)
  {
    _columnCount = columnCount;

    while (_columns.size() < columnCount) {
      _columns.add(new MysqlColumn());
    }
  }

  public ArrayList<MysqlColumn> getColumns()
  {
    return _columns;
  }

  public MysqlColumn getColumn(int column)
  {
    if (column < 1 || _columnCount < column)
      throw new IllegalArgumentException(L.l("{0} is an invalid column [1-{1}]",
                                             column, _columnCount));

    return _columns.get(column - 1);
  }

  /**
   * Returns true if the column is auto-numbered.
   */
  public boolean isAutoIncrement(int column)
  {
    return getColumn(column).isAutoIncrement();
  }

  /**
   * Returns true if the column is case sensitive
   */
  public boolean isCaseSensitive(int column)
  {
    return true;
  }

  /**
   * Returns true if the column can be in a where clause
   */
  public boolean isSearchable(int column)
  {
    return false;
  }

  /**
   * Returns true if the column is a currency;
   */
  public boolean isCurrency(int column)
  {
    return false;
  }

  /**
   * Returns true if the column is nullable
   */
  public int isNullable(int column)
  {
    if (getColumn(column).isNotNull())
      return columnNoNulls;
    else
      return columnNullable;
  }

  /**
   * Returns the normal width
   */
  public int getColumnDisplaySize(int column)
  {
    return getColumn(column).getPrecision();
  }

  /**
   * Returns the column label
   */
  public String getColumnLabel(int column)
  {
    return getColumnName(column);
  }

  /**
   * Returns the column name
   */
  public String getColumnName(int column)
  {
    return getColumn(column).getName();
  }

  /**
   * Returns the column original name
   */
  public String getColumnOrigName(int column)
  {
    return getColumn(column).getOrigName();
  }

  /**
   * Returns the column schema
   */
  public String getColumnSchema(int column)
  {
    return getColumn(column).getSchema();
  }

  /**
   * Returns true for signed results.
   */
  public boolean isSigned(int column)
  {
    return ! getColumn(column).isUnsigned();
  }

  /**
   * Returns the column precision
   */
  public int getPrecision(int column)
  {
    return getColumn(column).getPrecision();
  }

  /**
   * Returns the column scale
   */
  public int getScale(int column)
  {
    return getColumn(column).getScale();
  }

  /**
   * Returns the column table name
   */
  public String getSchemaName(int column)
  {
    return getColumn(column).getSchema();
  }

  /**
   * Returns the column table name
   */
  public String getTableName(int column)
  {
    return getColumn(column).getTable();
  }

  /**
   * Returns the column table orig name
   */
  public String getOrigTableName(int column)
  {
    return getColumn(column).getOrigTable();
  }

  /**
   * Returns the column catalog name
   */
  public String getCatalogName(int column)
  {
    return getColumn(column).getCatalog();
  }

  /**
   * Returns the column type
   */
  public int getColumnType(int column)
  {
    return getColumn(column).getSQLType();
  }

  /**
   * Returns the column type name
   */
  public String getColumnTypeName(int column)
  {
    return getColumn(column).getTypeName();
  }

  /**
   * Returns the column writability
   */
  public boolean isReadOnly(int column)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the column writability
   */
  public boolean isWritable(int column)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the column writability
   */
  public boolean isDefinitelyWritable(int column)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the column class namewritability
   */
  public String getColumnClassName(int column)
  {
    throw new UnsupportedOperationException();
  }

  public <T> T unwrap(Class<T> iface) throws SQLException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public boolean isWrapperFor(Class<?> iface) throws SQLException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  //
  // Quercus Result set
  //

  public boolean isBlob(int column)
  {
    return getColumn(column).isBlob();
  }

  public boolean isMultipleKey(int column)
  {
    return getColumn(column).isMultipleKey();
  }

  public boolean isZeroFill(int column)
  {
    return getColumn(column).isZeroFill();
  }

  public boolean isUnsigned(int column)
  {
    return getColumn(column).isUnsigned();
  }

  public boolean isUniqueKey(int column)
  {
    return getColumn(column).isUniqueKey();
  }

  public boolean isPrimaryKey(int column)
  {
    return getColumn(column).isPrimaryKey();
  }

  public boolean isNotNull(int column)
  {
    return getColumn(column).isNotNull();
  }

  public int getLength(int column)
  {
    return getColumn(column).getLength();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
