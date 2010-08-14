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

import com.caucho.db.table.Table;
import com.caucho.db.table.TableIterator;

import java.sql.SQLException;
import java.util.logging.Logger;

class OidExpr extends Expr {
  private Table _table;

  private int _tableIndex;

  OidExpr(Table table, int tableIndex)
  {
    _table = table;
    _tableIndex = tableIndex;
  }

  public Class getType()
  {
    return long.class;
  }

  @Override
  public boolean isLong()
  {
    return true;
  }

  /**
   * Returns any column name.
   */
  public String getName()
  {
    return "resin_oid";
  }

  /**
   * Returns the column's table.
   */
  public Table getTable()
  {
    return _table;
  }

  /**
   * Returns true if the expression is null.
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    return false;
  }

  /**
   * Evaluates the expression as a string.
   */
  public String evalString(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return String.valueOf(row.getRowAddress());
  }

  public int evalInt(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return (int) row.getRowAddress();
  }

  public long evalLong(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.getRowAddress();
  }

  public double evalDouble(QueryContext context)
    throws SQLException
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    return row.getRowAddress();
  }

  /**
   * Evaluates the expression, writing to the result stream.
   *
   * @param context the query context
   * @param result the output result
   */
  @Override
  public void evalToResult(QueryContext context, SelectResult result)
  {
    TableIterator []rows = context.getTableIterators();
    TableIterator row = rows[_tableIndex];

    result.writeLong(row.getRowAddress());
  }

  public String toString()
  {
    return "OidExpr[" + _tableIndex + "]";
  }
}
