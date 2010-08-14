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

import com.caucho.db.Database;
import com.caucho.db.table.Table;
import com.caucho.db.table.TableIterator;
import com.caucho.db.xa.Transaction;
import com.caucho.util.CharBuffer;

import java.sql.SQLException;
import java.util.logging.Logger;

class DeleteQuery extends Query {
  private final Table _table;

  DeleteQuery(Database db, String sql, Table table)
    throws SQLException
  {
    super(db, sql, null);
    
    _table = table;

    setFromItems(new FromItem[] { new FromItem(table, table.getName()) });
  }

  public boolean isReadOnly()
  {
    return false;
  }

  /**
   * Executes the query.
   */
  public void execute(QueryContext context, Transaction xa)
    throws SQLException
  {
    int count = 0;
    TableIterator []rows = new TableIterator[1];

    try {
      rows[0] = _table.createTableIterator();
      context.init(xa, rows, isReadOnly());
      
      if (! start(rows, rows.length, context, xa)) {
        return;
      }

      do {
        rows[0].delete();

        context.setRowUpdateCount(++count);
      } while (nextTuple(rows, rows.length, context, xa));
    } finally {
      // autoCommitWrite must be before freeRows in case freeRows
      // throws an exception
      context.close();
      
      freeRows(rows, rows.length);
    }
  }

  public String toString()
  {
    CharBuffer cb = new CharBuffer();
    cb.append("DeleteQuery[");
    if (_whereExpr != null) {
      cb.append(",where:" + _whereExpr);
    }
    cb.append("]");
    
    return cb.close();
  }
}
