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

import com.caucho.db.table.TableIterator;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

class LeftOuterJoinExpr extends RowIterateExpr {
  private Expr _expr;
  private FromItem _table;
  private int _tableIndex;

  LeftOuterJoinExpr(FromItem table, Expr expr)
  {
    _table = table;
    _expr = expr;
  }

  /**
   * Returns the expected result type of the expression.
   */
  public Class getType()
  {
    return boolean.class;
  }

  /**
   * Returns the cost based on the given FromList.
   */
  public long subCost(ArrayList<FromItem> fromList)
  {
    if (! fromList.contains(_table))
      return Integer.MAX_VALUE;
    else if (fromList.get(fromList.size() - 1) == _table)
      return 0;
    else
      return 100 * 100;
  }

  /**
   * Returns an index expression if available.
   */
  public RowIterateExpr getIndexExpr(FromItem fromItem)
  {
    if (_table == fromItem)
      return this;
    else
      return null;
  }

  /**
   * Binds the expression.
   */
  public Expr bind(Query query)
    throws SQLException
  {
    _expr = _expr.bind(query);

    FromItem []fromItems = query.getFromItems();

    for (int i = 0; i < fromItems.length; i++) {
      if (_table == fromItems[i])
        _tableIndex = i;
    }
    
    return this;
  }

  /**
   * Sets the initial row.
   */
  boolean init(QueryContext context, TableIterator rowIter)
    throws SQLException, IOException
  {
    rowIter.init(context);

    if (! rowIter.next()) {
      return false;
    }

    return true;
  }

  /**
   * Sets the initial row.
   */
  boolean initRow(QueryContext context, TableIterator rowIter)
    throws SQLException, IOException
  {
    rowIter.init(context);

    if (! rowIter.next())
      return false;
    
    rowIter.initRow();

    Expr expr = _expr;
    do {
      if (rowIter.nextRow()) {
      }
      else if (rowIter.next()) {
        rowIter.initRow();
      }
      else {
        rowIter.initNullRow();
        return true;
      }
    } while (expr.evalBoolean(context) != TRUE);
    TableIterator parentIter = context.getTableIterators()[1];
    
    return true;
  }

  /**
   * Returns true if shifing the child rows will make a difference.
   */
  boolean allowChildRowShift(QueryContext context, TableIterator rowIter)
  {
    return false;
  }

  /**
   * Returns the next row.
   */
  boolean nextRow(QueryContext context, TableIterator rowIter)
    throws IOException, SQLException
  {
    if (rowIter.isNullRow())
      return false;
    else {
      Expr expr = _expr;

      while (rowIter.nextRow() || rowIter.next()) {
        if (expr.evalBoolean(context) == TRUE) {
          return true;
        }
      }
      
      return false;
    }
  }

  /**
   * Returns the next row.
   */
  boolean nextBlock(QueryContext context, TableIterator rowIter)
    throws IOException
  {
    return false;
  }

  public String toString()
  {
    return "LeftOuterJoinExpr(" + _expr + ")";
  }
}
