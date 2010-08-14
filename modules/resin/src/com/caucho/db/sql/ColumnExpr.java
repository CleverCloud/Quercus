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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.db.sql;

import com.caucho.db.table.Column;
import com.caucho.db.table.Table;
import com.caucho.db.table.TableIterator;

import java.sql.SQLException;
import java.util.logging.Logger;

final class ColumnExpr extends Expr {
  private final Table _table;

  private final int _tableIndex;
  private final Column _column;
  private final int _columnIndex;

  private final String _name;
  private final Class<?> _type;

  ColumnExpr(String name, Table table, int tableIndex,
             int columnIndex, Class<?> type)
  {
    _name = name;
    _table = table;
    _tableIndex = tableIndex;
    _columnIndex = columnIndex;
    _column = table.getColumns()[_columnIndex];
    _type = type;
  }

  public Class getType()
  {
    return _type;
  }

  /**
   * Returns any column name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the column.
   */
  public Column getColumn()
  {
    return _column;
  }

  /**
   * Returns the column's table.
   */
  public Table getTable()
  {
    return _table;
  }

  /**
   * Returns true if the column can contain a null value.
   */
  public boolean isNullable()
  {
    return ! _column.isNotNull();
  }

  /**
   * Returns true if the expression is null.
   */
  final public boolean isNull(final QueryContext context)
    throws SQLException
  {
    final TableIterator []rows = context.getTableIterators();
    final TableIterator row = rows[_tableIndex];

    return row.isNull(_column);
  }

  /**
   * Evaluates the expression as a string.
   */
  public String evalString(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.getString(_column);
  }

  public int evalInt(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.getInteger(_column);
  }

  public long evalLong(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.getLong(_column);
  }

  public final double evalDouble(final QueryContext context)
    throws SQLException
  {
    final TableIterator []rows = context.getTableIterators();
    final TableIterator row = rows[_tableIndex];

    return row.getDouble(_column);
  }

  @Override
  public int evalToBuffer(QueryContext context, byte []buffer, int offset)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.getBuffer(_column, buffer, offset);
  }

  /**
   * Evaluates the expression, writing to the result stream.
   *
   * @param context the query context
   * @param result the output result
   */
  @Override
  public void evalToResult(QueryContext context, SelectResult result)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    row.evalToResult(_column, result);
  }

  public boolean evalEqual(QueryContext context, byte []matchBuffer)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.isEqual(_column, matchBuffer);
  }

  public boolean evalEqual(QueryContext context, String string)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.isEqual(_column, string);
  }

  public boolean equals(Object o)
  {
    if (o == null || ! ColumnExpr.class.equals(o.getClass()))
      return false;

    ColumnExpr expr = (ColumnExpr) o;

    return (_tableIndex == expr._tableIndex
            && _column == expr._column);
  }

  public String toString()
  {
    return "ColumnExpr[" + _tableIndex + "," + _columnIndex + "]";
  }
}
