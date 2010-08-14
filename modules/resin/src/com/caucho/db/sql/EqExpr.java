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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.db.sql;

import java.sql.SQLException;
import java.util.ArrayList;

import com.caucho.db.table.Column.ColumnType;
import com.caucho.inject.Module;

@Module
final class EqExpr extends Expr {
  private final Expr _left;
  private final Expr _right;

  EqExpr(Expr left, Expr right)
  {
    if (left == null || right == null)
      throw new NullPointerException();

    if (right instanceof UnboundIdentifierExpr &&
        ! (left instanceof UnboundIdentifierExpr)) {
      Expr temp = right;
      right = left;
      left = temp;
    }

    _left = left;
    _right = right;
  }

  @Override
  public Expr bind(Query query)
    throws SQLException
  {
    Expr newLeft = _left.bind(query);
    Expr newRight = _right.bind(query);

    if (newLeft instanceof ColumnExpr
        && newLeft.getType().equals(String.class)) {
      return new StringEqExpr((ColumnExpr) newLeft, newRight);
    }
    else if (newRight instanceof ColumnExpr
             && newRight.getType().equals(String.class)) {
      return new StringEqExpr((ColumnExpr) newRight, newLeft);
    }

    if (newLeft.isLong() && (newRight.isLong() || newRight.isParam()))
      return new LongEqExpr(newLeft, newRight);
    else if (newRight.isLong() && (newLeft.isLong() || newLeft.isParam()))
      return new LongEqExpr(newLeft, newRight);

    if (newLeft.isDouble() && (newRight.isDouble() || newRight.isParam()))
      return new DoubleEqExpr(newLeft, newRight);
    if (newRight.isDouble() && (newLeft.isDouble() || newLeft.isParam()))
      return new DoubleEqExpr(newLeft, newRight);

    if (_left == newLeft && _right == newRight)
      return this;
    else
      return new EqExpr(newLeft, newRight);
  }

  /**
   * Returns an index expression if available.
   */
  @Override
  public RowIterateExpr getIndexExpr(FromItem item)
  {
    if (_left instanceof IdExpr) {
      IdExpr expr = (IdExpr) _left;

      if (item != expr.getFromItem()) {
      }
      else if (expr.getColumn().getTypeCode() == ColumnType.IDENTITY) {
        return new IdentityIndexExpr(expr, _right);
      }
      else if (expr.getColumn().getIndex() != null) {
        return new IndexExpr(expr, _right);
      }
    }

    if (_right instanceof IdExpr) {
      IdExpr expr = (IdExpr) _right;

      if (item != expr.getFromItem()) {
      }
      else if (expr.getColumn().getTypeCode() == ColumnType.IDENTITY) {
        return new IdentityIndexExpr(expr, _left);
      }
      else if (expr.getColumn().getIndex() != null) {
        return new IndexExpr(expr, _left);
      }
    }

    return null;
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
  @Override
  public long cost(ArrayList<FromItem> fromList)
  {
    if (_left instanceof UnboundIdentifierExpr
        && _right.cost(fromList) == 0) {
      UnboundIdentifierExpr id = (UnboundIdentifierExpr) _left;

      return id.lookupCost(fromList);
    }
    else if (_right instanceof UnboundIdentifierExpr
             && _left.cost(fromList) == 0) {
      UnboundIdentifierExpr id = (UnboundIdentifierExpr) _right;

      return id.lookupCost(fromList);
    }
    else {
      return subCost(fromList);
    }
  }

  /**
   * Returns the cost based on a subitem.
   */
  @Override
  public long subCost(ArrayList<FromItem> fromList)
  {
    return _left.subCost(fromList) + _right.subCost(fromList);
  }

  /**
   * Evaluates the expression for nulls
   */
  @Override
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    return (_left.isNull(context) || _right.isNull(context));
  }

  /**
   * Evaluates the expression as a boolean.
   */
  @Override
  public int evalBoolean(QueryContext context)
    throws SQLException
  {
    if (_left.isNull(context) || _right.isNull(context))
      return UNKNOWN;

    if (_left.isLong() && _right.isLong()) {
      if (_left.evalLong(context) == _right.evalLong(context))
        return TRUE;
      else
        return FALSE;
    }
    else if (_left.isDouble() && _right.isDouble()) {
      if (_left.evalDouble(context) == _right.evalDouble(context))
        return TRUE;
      else
        return FALSE;
    }
    else {
      String leftValue = _left.evalString(context);
      String rightValue = _right.evalString(context);
      
      if (leftValue == rightValue || leftValue.equals(rightValue))
        return TRUE;
      else
        return FALSE;
    }
  }

  @Override
  public String evalString(QueryContext context)
    throws SQLException
  {
    throw new SQLException("can't convert string to boolean");
  }

  /**
   * Evaluates aggregate functions during the group phase.
   *
   * @param state the current database tuple
   */
  @Override
  public void evalGroup(QueryContext context)
    throws SQLException
  {
    _left.evalGroup(context);
    _right.evalGroup(context);
  }

  public String toString()
  {
    return "(" + _left + " = " + _right + ")";
  }
}
