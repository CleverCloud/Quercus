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

class UnboundStarExpr extends Expr {
  private static final L10N L = new L10N(UnboundStarExpr.class);

  private final String _table;

  /**
   * Creates an unbound identifier with just a column name.
   */
  UnboundStarExpr()
  {
    _table =  null;
  }

  /**
   * Creates an unbound identifier with just a column name.
   */
  UnboundStarExpr(String tableName)
  {
    _table =  tableName;
  }

  public Expr bind(Query query)
    throws SQLException
  {
    return bind(query.getFromItems());
  }

  public Expr bind(FromItem []fromItems)
    throws SQLException
  {
    return this;
  }

  protected ArrayList<Expr> expand(FromItem []fromItems)
    throws SQLException
  {
    ArrayList<Expr> exprs = new ArrayList<Expr>();

    for (int i = 0; i < fromItems.length; i++) {
      Table table = fromItems[i].getTable();
      Column []columns = table.getColumns();

      if (_table != null && ! fromItems[i].getName().equals(_table))
        continue;

      for (int j = 0; j < columns.length; j++) {
        exprs.add(new UnboundIdentifierExpr(fromItems[i].getName(),
                                            columns[j].getName()));
      }
    }

    return exprs;
  }

  public String toString()
  {
    return "UnboundStarExpr[]";
  }
}
