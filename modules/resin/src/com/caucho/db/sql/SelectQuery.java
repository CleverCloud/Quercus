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
import com.caucho.db.table.TableIterator;
import com.caucho.db.xa.Transaction;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.CharBuffer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.logging.*;

public class SelectQuery extends Query {
  private static final Logger log
    = Logger.getLogger(SelectQuery.class.getName());

  private Expr []_results;
  private String []_resultNames;

  private boolean []_groupFields;

  private Order _order;
  private int _limit = Integer.MAX_VALUE / 2;

  SelectQuery(Database db, String sql)
    throws SQLException
  {
    super(db, sql);
  }

  SelectQuery(Database db, String sql, FromItem []fromItems)
    throws SQLException
  {
    super(db, sql, fromItems);
  }

  void setResults(Expr []resultExprs)
    throws SQLException
  {
    _results = new Expr[resultExprs.length];

    for (int i = 0; i < resultExprs.length; i++) {
      _results[i] = resultExprs[i];
    }

    setDataFields(resultExprs.length);
  }

  Expr []getResults()
  {
    return _results;
  }

  /**
   * Sets the result item as group.
   */
  public void setGroupResult(int index)
  {
    if (_groupFields == null)
      _groupFields = new boolean[_results.length];

    _groupFields[index] = true;
  }

  protected void bind()
    throws SQLException
  {
    super.bind();

    for (int i = 0; i < _results.length; i++) {
      _results[i] = _results[i].bind(this);
    }

    if (isGroup()) {
      for (int i = 0; i < _results.length; i++) {
        if (isGroup() && ! (_results[i] instanceof GroupExpr))
          _results[i] = new GroupResultExpr(i, _results[i]);
      }
    }

    for (int i = 0; i < _results.length; i++) {
      _results[i] = _results[i].bind(this);
    }

    _resultNames = new String[_results.length];

    for (int i = 0; i < _resultNames.length; i++)
      _resultNames[i] = _results[i].getName();
  }

  void setOrder(Order order)
  {
    _order = order;
  }

  @Override
  public void setLimit(int limit)
  {
    _limit = limit;
  }

  /**
   * Returns true for select queries.
   */
  public boolean isSelect()
  {
    return true;
  }

  /**
   * Returns the type of the child.
   */
  Class getType()
  {
    if (_results.length == 1)
      return _results[0].getType();
    else
      return Object.class;
  }

  /**
   * Executes the query.
   */
  public void execute(QueryContext context, Transaction xa)
    throws SQLException
  {
    SelectResult result = SelectResult.create(_results, _order);
    FromItem []fromItems = getFromItems();
    TableIterator []rows = null;

    try {
      rows = result.initRows(fromItems);
      context.init(xa, rows, isReadOnly());

      if (isGroup())
        executeGroup(result, rows, context, xa);
      else
        execute(result, rows, context, xa);

      result.initRead();

      context.setResult(result);
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    } finally {
      // autoCommitRead must be before freeRows in case freeRows
      // throws an exception
      try {
        context.close();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      if (rows != null)
        freeRows(rows, rows.length);
    }
  }

  /**
   * Executes the query.
   */
  private void execute(SelectResult result,
                       TableIterator []rows,
                       QueryContext context,
                       Transaction xa)
    throws SQLException, IOException
  {
    FromItem []fromItems = getFromItems();
    int rowLength = fromItems.length;

    int limit = _limit;
    int contextLimit = context.getLimit();
    if (contextLimit > 0)
      limit = contextLimit;

    if (start(rows, rowLength, context, xa)) {
      do {
        result.startRow();

        for (int i = 0; i < _results.length; i++) {
          _results[i].evalToResult(context, result);
        }
      } while (nextTuple(rows, rowLength, context, xa) && --limit > 0);
    }
  }

  private void executeGroup(SelectResult result,
                            TableIterator []rows,
                            QueryContext context,
                            Transaction transaction)
    throws SQLException, IOException
  {
    FromItem []fromItems = getFromItems();
    int rowLength = fromItems.length;

    Expr []results = _results;
    int resultsLength = results.length;

    /*
    for (int i = 0; i < results.length; i++)
      results[i].initGroup(context);
    */

    if (_groupFields == null)
      _groupFields = new boolean[0];

    boolean []groupByFields = _groupFields;
    int groupByLength = _groupFields.length;

    if (start(rows, rowLength, context, transaction)) {
      do {
        context.initGroup(getDataFields(), _groupFields);

        for (int i = 0; i < groupByLength; i++) {
          if (groupByFields[i])
            results[i].evalGroup(context);
        }

        context.selectGroup();

        for (int i = 0; i < resultsLength; i++) {
          if (! (i < groupByLength && groupByFields[i]))
            results[i].evalGroup(context);
        }
      } while (nextTuple(rows, rowLength, context, transaction));
    }

    Iterator<GroupItem> groupIter = context.groupResults();

    while (groupIter.hasNext()) {
      GroupItem item = groupIter.next();

      context.setGroupItem(item);

      result.startRow();
      for (int i = 0; i < results.length; i++) {
        results[i].evalToResult(context, result);
      }
    }
  }

  public String toString()
  {
    CharBuffer cb = CharBuffer.allocate();
    cb.append("SelectQuery[");
    cb.append("SELECT ");
    for (int i = 0; i < _results.length; i++) {
      if (i != 0)
        cb.append(",");
      cb.append(_results[i]);
    }
    cb.append(" FROM ");

    FromItem []fromItems = getFromItems();
    for (int i = 0; i < fromItems.length; i++) {
      if (i != 0)
        cb.append(",");
      cb.append(fromItems[i]);
    }

    if (_whereExpr != null) {
      cb.append(" WHERE " + _whereExpr);
    }
    cb.append("]");

    return cb.close();
  }
}
