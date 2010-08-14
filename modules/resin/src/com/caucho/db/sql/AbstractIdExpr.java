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

abstract class AbstractIdExpr extends Expr {
  private static final L10N L = new L10N(IdExpr.class);

  abstract protected FromItem getFromItem();
  abstract protected Column getColumn();
  abstract protected int getTableIndex();

  public Class getType()
  {
    return getColumn().getJavaType();
  }

  /**
   * Returns any column name.
   */
  public String getName()
  {
    return getColumn().getName();
  }

  /**
   * Returns the column's table.
   */
  public Table getTable()
  {
    return getFromItem().getTable();
  }

  /**
   * Returns true if the column can contain a null value.
   */
  @Override
  public boolean isNullable()
  {
    return ! getColumn().isNotNull();
  }

  /**
   * The cost of a match of the expr.
   */
  protected long lookupCost(ArrayList<FromItem> fromList)
  {
    final FromItem fromItem = getFromItem();

    if (! fromList.contains(fromItem))
      return COST_NO_TABLE;

    if (fromList.indexOf(fromItem) < fromList.size() - 1)
      return 0;

    final Column column = getColumn();

    if (column.isPrimaryKey())
      return COST_INDEX;
    else if (column.isUnique())
      return COST_UNIQUE;
    else
      return COST_SCAN;
  }

  /**
   * The cost of a match of the expr.
   */
  @Override
  public long subCost(ArrayList<FromItem> fromList)
  {
    if (! fromList.contains(getFromItem()))
      return Integer.MAX_VALUE;


    return 10 * 100 * 100 * 100;
  }

  @Override
  public Expr bind(Query query)
    throws SQLException
  {
    return this;
  }

  /**
   * Returns true if the expression is null.
   */
  @Override
  public boolean isNull(final QueryContext context)
    throws SQLException
  {
    final TableIterator []rows = context.getTableIterators();
    final TableIterator row = rows[getTableIndex()];

    return row.isNull(getColumn());
  }

  /**
   * Evaluates the expression as a string.
   */
  public String evalString(final QueryContext context)
    throws SQLException
  {
    final TableIterator []rows = context.getTableIterators();
    final TableIterator row = rows[getTableIndex()];

    return row.getString(getColumn());
  }

  /**
   * Evaluates the expression as a boolean.
   */
  public int evalBoolean(final QueryContext context)
    throws SQLException
  {
    final TableIterator []rows = context.getTableIterators();
    final TableIterator row = rows[getTableIndex()];

    final String value = row.getString(getColumn());

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

  public int evalInt(final QueryContext context)
    throws SQLException
  {
    final TableIterator []rows = context.getTableIterators();
    final TableIterator row = rows[getTableIndex()];

    return row.getInteger(getColumn());
  }

  @Override
  public long evalLong(final QueryContext context)
    throws SQLException
  {
    final TableIterator []rows = context.getTableIterators();
    final TableIterator row = rows[getTableIndex()];

    return row.getLong(getColumn());
  }

  @Override
  public double evalDouble(final QueryContext context)
    throws SQLException
  {
    final TableIterator []rows = context.getTableIterators();
    final TableIterator row = rows[getTableIndex()];

    return row.getDouble(getColumn());
  }

  /**
   * Evaluates the expression, writing to the result stream.
   *
   * @param context the query context
   * @param result the output result
   */
  @Override
  public void evalToResult(final QueryContext context,
                           final SelectResult result)
    throws SQLException
  {
    final TableIterator []rows = context.getTableIterators();
    final TableIterator row = rows[getTableIndex()];

    row.evalToResult(getColumn(), result);
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
    TableIterator row = rows[getTableIndex()];

    return row.getBuffer(getColumn(), buffer, offset);
  }

  public boolean evalEqual(QueryContext context, byte []matchBuffer)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[getTableIndex()];

    return row.isEqual(getColumn(), matchBuffer);
  }

  public boolean evalEqual(QueryContext context, String string)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[getTableIndex()];

    return row.isEqual(getColumn(), string);
  }

  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    AbstractIdExpr expr = (AbstractIdExpr) o;

    return (getFromItem() == expr.getFromItem()
            && getColumn() == expr.getColumn());
  }

  public String toString()
  {
    return getFromItem().getName() + "." + getColumn().getName();
  }
}
