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
import com.caucho.util.L10N;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

class UnboundIdentifierExpr extends Expr {
  private static final L10N L = new L10N(UnboundIdentifierExpr.class);
  
  private String _table;
  private String _column;

  /**
   * Creates an unbound identifier with just a column name.
   */
  UnboundIdentifierExpr(String column)
  {
    _column = column;
  }

  /**
   * Creates an unbound identifier with a table and a column name.
   */
  UnboundIdentifierExpr(String table, String column)
  {
    _table = table;
    _column = column;
  }

  /**
   * The cost of a match of the expr.
   */
  protected long lookupCost(ArrayList<FromItem> fromList)
  {
    Column column = findColumn(fromList);
    if (column == null)
      return Integer.MAX_VALUE;
    
    FromItem fromItem = fromList.get(fromList.size() - 1);

    Table table = fromItem.getTable();

    column = table.getColumn(_column);

    if (column == null)
      return 100 * 100 * 100;
    else if (column.isPrimaryKey())
      return 100;
    else if (column.isUnique())
      return 100 * 100;
    else
      return 100 * 100 * 100;
  }

  /**
   * The cost of a match of the expr.
   */
  public long subCost(ArrayList<FromItem> fromList)
  {
    Column column = findColumn(fromList);

    if (column == null)
      return Integer.MAX_VALUE;
    else
      return 10 * 100 * 100 * 100;
  }

  /**
   * Finds the column in the from list.
   */
  private Column findColumn(ArrayList<FromItem> fromItems)
  {
    if (_table == null) {
      for (int i = 0; i < fromItems.size(); i++) {
        FromItem fromItem = fromItems.get(i);

        Table table = fromItem.getTable();

        Column column = table.getColumn(_column);

        if (column != null)
          return column;
      }

      return null;
    }
    else {
      for (int i = 0; i < fromItems.size(); i++) {
        FromItem fromItem = fromItems.get(i);

        if (_table.equals(fromItem.getName())) {
          Table table = fromItem.getTable();

          return table.getColumn(_column);
        }
      }
      
      return null;
    }
  }

  public Expr bind(Query query)
    throws SQLException
  {
    return query.bind(_table, _column);
  }

  public String toString()
  {
    if (_table != null)
      return "UnboundIdentifier[" + _table + "," + _column + "]";
    else
      return "UnboundIdentifier[" + _column + "]";
  }
}
