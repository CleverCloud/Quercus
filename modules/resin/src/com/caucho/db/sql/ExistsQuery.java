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

import com.caucho.db.Database;
import com.caucho.db.table.TableIterator;
import com.caucho.db.xa.Transaction;
import com.caucho.sql.SQLExceptionWrapper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

public class ExistsQuery extends SelectQuery {
  private static Expr []_nullExprs = new Expr[0];

  protected ExistsQuery(Database db, String sql)
    throws SQLException
  {
    super(db, sql);
  }

  /**
   * Executes the query.
   */
  public boolean exists(QueryContext context, Transaction xa)
    throws SQLException
  {
    SelectResult result = SelectResult.create(_nullExprs, null);

    try {
      TableIterator []rows = result.initRows(getFromItems());
      context.init(xa, rows, isReadOnly());

      return execute(result, rows, context, xa);
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    } finally {
      context.unlock();

      result.close();
    }
  }
  
  /**
   * Executes the query.
   */
  private boolean execute(SelectResult result,
                       TableIterator []rows,
                       QueryContext context,
                       Transaction transaction)
    throws SQLException, IOException
  {
    FromItem []fromItems = getFromItems();
    int rowLength = fromItems.length;
    
    return start(rows, rowLength, context, transaction);
  }

  /**
   * Executes the query.
   */
  public void execute(QueryContext queryCtx, Transaction xa)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }
}
