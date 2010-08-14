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

import com.caucho.util.CharBuffer;

final class AndExpr extends Expr {
  private ArrayList<Expr> _exprList = new ArrayList<Expr>();
  private Expr []_exprs;

  AndExpr()
  {
  }

  void add(Expr expr)
  {
    _exprList.add(expr);
  }

  Expr getSingleExpr()
  {
    if (_exprList.size() == 0)
      return null;
    else if (_exprList.size() == 1)
      return _exprList.get(0);
    else
      return this;
  }

  public Expr bind(Query query)
    throws SQLException
  {
    _exprs = new Expr[_exprList.size()];

    for (int i = 0; i < _exprs.length; i++) {
      Expr expr = _exprList.get(i);

      expr = expr.bind(query);

      if (! expr.getType().equals(boolean.class))
        throw new SQLException(L.l("AND requires boolean operands at {0}",
                                   expr));

      _exprList.set(i, expr);
      _exprs[i] = expr;
    }

    Expr tailExpr = _exprs[_exprs.length - 1];

    for (int i = _exprs.length - 2; i >= 0; i--) {
      tailExpr = new BinaryAndExpr(_exprs[i], tailExpr);
    }

    return tailExpr;
  }

  /**
   * Returns the type of the expression.
   */
  @Override
  public Class<?> getType()
  {
    return boolean.class;
  }

  /**
   * Returns the cost based on the given FromList.
   */
  public long subCost(ArrayList<FromItem> fromList)
  {
    long cost = 0;

    for (int i = 0; i < _exprList.size(); i++) {
      cost += _exprList.get(i).subCost(fromList);
    }

    return cost;
  }

  /**
   * Splits the expr into and blocks.
   */
  public void splitAnd(ArrayList<Expr> andProduct)
  {
    for (int i = 0; i < _exprList.size(); i++) {
      _exprList.get(i).splitAnd(andProduct);
    }
  }

  /**
   * Returns true for a null expression
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    Expr []exprs = _exprs;
    int length = exprs.length;

    boolean isNull = false;
    for (int i = 0; i < length; i++) {
      int value = exprs[i].evalBoolean(context);

      if (value == FALSE)
        return false;
      else if (value != TRUE)
        isNull = true;
    }

    return isNull;
  }

  /**
   * Evaluates the expression as a boolean.
   */
  public int evalBoolean(QueryContext context)
    throws SQLException
  {
    Expr []exprs = _exprs;
    int length = exprs.length;

    int value = TRUE;
    for (int i = 0; i < length; i++) {
      int subValue = exprs[i].evalBoolean(context);

      if (subValue == FALSE)
        return FALSE;
      else if (subValue == UNKNOWN)
        value = UNKNOWN;
    }

    return value;
  }

  /**
   * Evaluates the expression as a boolean.
   */
  @Override
  public final boolean isSelect(final QueryContext context)
    throws SQLException
  {
    final Expr []exprs = _exprs;
    final int length = exprs.length;

    for (int i = 0; i < length; i++) {
      if (! exprs[i].isSelect(context))
        return false;
    }

    return true;
  }

  public String evalString(QueryContext context)
    throws SQLException
  {
    switch (evalBoolean(context)) {
    case TRUE:
      return "1";
    case FALSE:
      return "0";
    default:
      return null;
    }
  }

  public String toString()
  {
    CharBuffer cb = CharBuffer.allocate();
    cb.append("(");

    for (int i = 0; i < _exprList.size(); i++) {
      if (i != 0)
        cb.append(" AND ");

      cb.append(_exprList.get(i));
    }

    cb.append(")");

    return cb.close();
  }
}
