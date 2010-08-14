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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

class UnaryExpr extends Expr {
  private Expr _sub;
  private int _op;

  UnaryExpr(Expr sub, int op)
  {
    _sub = sub;
    _op = op;
  }

  /**
   * Binds the expression to the actual tables.
   */
  public Expr bind(Query query)
    throws SQLException
  {
    Expr newSub = _sub.bind(query);

    switch (_op) {
    case '-':
      if (! newSub.isDouble())
        throw new SQLException(L.l("unary minus requires a numeric expression at '{0}'", newSub));
      break;
    }

    if (newSub == _sub)
      return this;
    else
      return new UnaryExpr(newSub, _op);
  }

  /**
   * Returns the type of the expression.
   */
  public Class getType()
  {
    switch (_op) {
    case '-':
      if (_sub.isLong())
        return long.class;
      else
        return double.class;

    default:
      return Object.class;
    }
  }

  /**
   * Returns the cost based on the given FromList.
   */
  public long subCost(ArrayList<FromItem> fromList)
  {
    return _sub.subCost(fromList);
  }

  /**
   * Evaluates the expression for nulls
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    return _sub.isNull(context);
  }

  /**
   * Evaluates the expression as a boolean
   *
   * @param rows the current tuple being evaluated
   *
   * @return the boolean value
   */
  public int evalBoolean(QueryContext context)
    throws SQLException
  {
    switch (_op) {
    default:
      throw new IllegalStateException();
    }
  }

  /**
   * Evaluates the expression as a long.
   *
   * @param rows the current tuple being evaluated
   *
   * @return the long value
   */
  public long evalLong(QueryContext context)
    throws SQLException
  {
    switch (_op) {
    case '-':
      return - _sub.evalLong(context);

    default:
      throw new IllegalStateException();
    }
  }

  /**
   * Evaluates the expression as a double.
   *
   * @param rows the current tuple being evaluated
   *
   * @return the long value
   */
  public double evalDouble(QueryContext context)
    throws SQLException
  {
    switch (_op) {
    case '-':
      return - _sub.evalDouble(context);

    default:
      throw new IllegalStateException();
    }
  }

  /**
   * Evaluates the expression as a string.
   *
   * @param rows the current tuple being evaluated
   *
   * @return the string value
   */
  public String evalString(QueryContext context)
    throws SQLException
  {
    switch (_op) {
    case '-':
      if (isLong())
        return String.valueOf(evalLong(context));
      else
        return String.valueOf(evalDouble(context));

    default:
      throw new IllegalStateException();
    }
  }

  /**
   * Evaluates aggregate functions during the group phase.
   *
   * @param state the current database tuple
   */
  public void evalGroup(QueryContext context)
    throws SQLException
  {
    _sub.evalGroup(context);
  }

  public String toString()
  {
    switch (_op) {
    case '-':
      return "- " + _sub;

    default:
      throw new IllegalStateException("can't compare:" + _op);
    }
  }
}
