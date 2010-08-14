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
import com.caucho.util.L10N;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

final class IdExpr extends Expr {
  private static final L10N L = new L10N(IdExpr.class);

  private final FromItem _fromItem;
  private final Column _column;

  private int _tableIndex = -1;

  /**
   * Creates an unbound identifier with just a column name.
   */
  IdExpr(FromItem fromItem, Column column)
  {
    _fromItem = fromItem;
    _column = column;
  }

  public Class getType()
  {
    return _column.getJavaType();
  }

  /**
   * Returns the column.
   */
  public Column getColumn()
  {
    return _column;
  }

  /**
   * Returns any column name.
   */
  public String getName()
  {
    return _column.getName();
  }

  /**
   * Returns the from item
   */
  public FromItem getFromItem()
  {
    return _fromItem;
  }

  /**
   * Returns the column's table.
   */
  public Table getTable()
  {
    return _fromItem.getTable();
  }

  /**
   * Returns true if the column can contain a null value.
   */
  @Override
  public boolean isNullable()
  {
    return ! _column.isNotNull();
  }

  /**
   * The cost of a match of the expr.
   */
  protected long lookupCost(ArrayList<FromItem> fromList)
  {
    if (! fromList.contains(_fromItem))
      return COST_NO_TABLE;

    if (fromList.indexOf(_fromItem) < fromList.size() - 1)
      return 0;

    if (_column.isPrimaryKey())
      return COST_INDEX;
    else if (_column.isUnique())
      return COST_UNIQUE;
    else
      return COST_SCAN;
  }

  /**
   * The cost of a match of the expr.
   */
  public long subCost(ArrayList<FromItem> fromList)
  {
    if (! fromList.contains(_fromItem))
      return Integer.MAX_VALUE;


    return 10 * 100 * 100 * 100;
  }

  public Expr bind(Query query)
    throws SQLException
  {
    FromItem []fromItems = query.getFromItems();

    for (int i = 0; i < fromItems.length; i++) {
      if (fromItems[i] == _fromItem)
        _tableIndex = i;
    }

    return this;
  }

  /**
   * Returns true if the expression is null.
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

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

  /**
   * Evaluates the expression as a boolean.
   */
  public int evalBoolean(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    String value = row.getString(_column);

    if (value == null)
      return UNKNOWN;
    else if (value.equals("1"))
      return TRUE;
    else if (value.equalsIgnoreCase("t"))
      return TRUE;
    else if (value.equalsIgnoreCase("y"))
      return TRUE;
    else
      return FALSE;
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

  public double evalDouble(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.getDouble(_column);
  }

  /**
   * Evaluates the expression, writing to the result stream.
   *
   * @param context the query context
   * @param result the output result
   */
  public void evalToResult(QueryContext context, SelectResult result)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    row.evalToResult(_column, result);
  }

  /**
   * Evaluates the expression to a buffer
   *
   * @param result the result buffer
   *
   * @return the length of the result
   */
  @Override
  public int evalToBuffer(QueryContext context,
                          byte []buffer,
                          int offset)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.getBuffer(_column, buffer, offset);
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
    if (o == null || ! IdExpr.class.equals(o.getClass()))
      return false;

    IdExpr expr = (IdExpr) o;

    return (_fromItem == expr._fromItem &&
            _column == expr._column);
  }

  public String toString()
  {
    return _fromItem.getName() + "." + _column.getName();
  }
}
