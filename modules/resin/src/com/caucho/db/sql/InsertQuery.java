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

package com.caucho.db.sql;

import java.sql.SQLException;
import java.util.ArrayList;

import com.caucho.db.Database;
import com.caucho.db.table.Column;
import com.caucho.db.table.Table;
import com.caucho.db.table.TableIterator;
import com.caucho.db.table.Column.ColumnType;
import com.caucho.db.xa.Transaction;
import com.caucho.sql.SQLExceptionWrapper;

class InsertQuery extends Query {
  private Table _table;

  private ArrayList<Column> _columns;
  private ArrayList<Expr> _values;

  InsertQuery(Database db, String sql, Table table, ArrayList<Column> columns)
    throws SQLException
  {
    super(db, sql, null);

    _table = table;

    _columns = columns;
  }

  public boolean isReadOnly()
  {
    return false;
  }

  public void setValues(ArrayList<Expr> values)
  {
    _values = values;
  }

  void init()
    throws SQLException
  {
    Column []tableColumns = _table.getColumns();

    for (int i = 0; i < tableColumns.length; i++) {
      Column column = tableColumns[i];

      Expr defaultExpr = column.getDefault();

      if (column.getTypeCode() == ColumnType.IDENTITY) {
        defaultExpr = new IdentityExpr(column.getTable(), column);
      }
      else if (column.getAutoIncrement() > 0) {
        defaultExpr = new AutoIncrementExpr(column.getTable());
      }

      if (defaultExpr == null)
        continue;

      int j = 0;
      for (; j < _columns.size(); j++) {
        if (_columns.get(j) == column)
          break;
      }

      if (j == _columns.size()) {
        _columns.add(column);
        _values.add(new NullExpr());
      }

      _values.set(j, new DefaultExpr(_values.get(j), defaultExpr));
    }
  }

  /**
   * Executes the query.
   */
  public void execute(QueryContext queryContext, Transaction xa)
    throws SQLException
  {
    TableIterator []rows = new TableIterator[1];

    try {
      rows[0] = _table.createTableIterator();
      queryContext.init(xa, rows, isReadOnly());

      _table.insert(queryContext, xa, _columns, _values);

      queryContext.setRowUpdateCount(1);
    } catch (java.io.IOException e) {
      throw new SQLExceptionWrapper(e);
    } finally {
      queryContext.close();
    }
  }

  public String toString()
  {
    return "InsertQuery[]";
  }
}
