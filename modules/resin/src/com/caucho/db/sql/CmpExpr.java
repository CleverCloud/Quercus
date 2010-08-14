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

class CmpExpr extends AbstractBinaryExpr {
  private Expr _left;
  private Expr _right;
  private int _op;

  CmpExpr(Expr left, Expr right, int op)
  {
    _left = left;
    _right = right;
    _op = op;
  }

  @Override
  public final Expr getLeft()
  {
    return _left;
  }

  @Override
  public final Expr getRight()
  {
    return _right;
  }

  @Override
  public Expr create(Expr left, Expr right)
  {
    return new CmpExpr(left, right, _op);
  }

  public Expr bind(Query query)
    throws SQLException
  {
    _left = _left.bind(query);
    _right = _right.bind(query);

    /*
    if (_left.isLong() && _right.isLong())
      return new LongCmpExpr(_op, _left, _right);
    */

    switch (_op) {
    case Parser.LT:
      if (_left.isNullable() || _right.isNullable())
        return new DoubleLtExpr(_left, _right);
      else
        return new DoubleLtNonNullExpr(_left, _right);

    case Parser.LE:
      if (_left.isNullable() || _right.isNullable()) {
        return new DoubleLeExpr(_left, _right);
      }
      else
        return new DoubleLeNonNullExpr(_left, _right);

    case Parser.GT:
      return new DoubleGtExpr(_left, _right);
    case Parser.GE:
      return new DoubleGeExpr(_left, _right);
    }

    if (_left.isDouble() || _right.isDouble()) {
      switch (_op) {
      case Parser.EQ:
        return new DoubleEqExpr(_left, _right);
      case Parser.NE:
        return new DoubleNeqExpr(_left, _right);
      }
    }

    return this;
  }

  /**
   * Returns the type of the expression.
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
    return _left.subCost(fromList) + _right.subCost(fromList);
  }

  /**
   * Evaluates the expression as a boolean.
   */
  public int evalBoolean(QueryContext context)
    throws SQLException
  {
    if (_left.isNull(context) || _right.isNull(context))
      return UNKNOWN;

    switch (_op) {
    case Parser.NE:
      {
        String leftValue = _left.evalString(context);
        String rightValue = _right.evalString(context);

        if (! (leftValue == rightValue
               || leftValue != null && leftValue.equals(rightValue)))
          return TRUE;
        else
          return FALSE;
      }

    case Parser.EQ:
      {
        String leftValue = _left.evalString(context);
        String rightValue = _right.evalString(context);
        if (leftValue == rightValue
            || leftValue != null && leftValue.equals(rightValue))
          return TRUE;
        else
          return FALSE;
      }

    default:
      throw new SQLException("can't compare");
    }
  }

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
  public void evalGroup(QueryContext context)
    throws SQLException
  {
    _left.evalGroup(context);
    _right.evalGroup(context);
  }

  public String toString()
  {
    switch (_op) {
    case Parser.EQ:
      return "(" + _left + " = " + _right + ")";
    case Parser.NE:
      return "(" + _left + " <> " + _right + ")";
    case Parser.LT:
      return "(" + _left + " < " + _right + ")";
    case Parser.LE:
      return "(" + _left + " <= " + _right + ")";
    case Parser.GT:
      return "(" + _left + " > " + _right + ")";
    case Parser.GE:
      return "(" + _left + " >= " + _right + ")";
    default:
      return "(" + _left + " =? " + _right + ")";
    }
  }
}
