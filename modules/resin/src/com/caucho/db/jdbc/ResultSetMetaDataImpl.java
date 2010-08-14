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
package com.caucho.db.jdbc;

import com.caucho.db.sql.Expr;
import com.caucho.db.sql.SelectResult;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Metadata for the result
 */
public class ResultSetMetaDataImpl implements ResultSetMetaData {
  private final SelectResult _rs;
  
  ResultSetMetaDataImpl(SelectResult rs)
  {
    _rs = rs;
  }

  /**
   * Returns the number of columns.
   */
  public int getColumnCount()
  {
    return _rs.getExprs().length;
  }

  /**
   * Returns true if the column is auto-numbered.
   */
  public boolean isAutoIncrement(int column)
  {
    return false;
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
    return columnNullableUnknown;
  }

  /**
   * Returns the normal width
   */
  public int getColumnDisplaySize(int column)
  {
    return 16;
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
   * Returns the column schema
   */
  public String getColumnSchema(int column)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true for signed results.
   */
  public boolean isSigned(int column)
  {
    return true;
  }

  /**
   * Returns the column precision
   */
  public int getPrecision(int column)
  {
    return 0;
  }

  /**
   * Returns the column scale
   */
  public int getScale(int column)
  {
    return 0;
  }

  /**
   * Returns the column table name
   */
  public String getSchemaName(int column)
  {
    return getTableName(column);
  }

  /**
   * Returns the column table name
   */
  public String getTableName(int column)
  {
    return getColumn(column).getTable().getName();
  }

  /**
   * Returns the column catalog name
   */
  public String getCatalogName(int column)
  {
    return null;
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
    throw new UnsupportedOperationException();
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

  /**
   * Returns the column.
   */
  public Expr getColumn(int column)
  {
    return _rs.getExprs()[column - 1];
  }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
