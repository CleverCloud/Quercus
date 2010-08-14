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
import java.util.logging.Logger;

class DefaultExpr extends Expr {
  private Expr _expr;
  private Expr _default;

  DefaultExpr(Expr expr, Expr defaultExpr)
  {
    _expr = expr;
    _default = defaultExpr;
  }

  public Expr bind(Query query)
    throws SQLException
  {
    Expr newExpr = _expr.bind(query);
    
    if (_expr == newExpr)
      return this;
    else
      return new DefaultExpr(newExpr, _default);
  }

  /**
   * Returns the type of the expression.
   */
  public Class getType()
  {
    return _expr.getType();
  }

  /**
   * Returns true if the expression is null.
   *
   * @param rows the current database tuple
   *
   * @return true if null
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    return _expr.isNull(context) && _default.isNull(context);
  }

  /**
   * Evaluates the expression as a boolean.
   *
   * @param rows the current database tuple
   *
   * @return the boolean value
   */
  public int evalBoolean(QueryContext context)
    throws SQLException
  {
    if (! _expr.isNull(context))
      return _expr.evalBoolean(context);
    else
      return _default.evalBoolean(context);
  }

  /**
   * Evaluates the expression as a string.
   *
   * @param rows the current database tuple
   *
   * @return the string value
   */
  public String evalString(QueryContext context)
    throws SQLException
  {
    if (! _expr.isNull(context))
      return _expr.evalString(context);
    else
      return _default.evalString(context);
  }

  /**
   * Evaluates the expression as a long.
   *
   * @param rows the current database tuple
   *
   * @return the long value
   */
  public long evalLong(QueryContext context)
    throws SQLException
  {
    if (! _expr.isNull(context))
      return _expr.evalLong(context);
    else
      return _default.evalLong(context);
  }

  /**
   * Evaluates the expression as a double.
   *
   * @param rows the current database tuple
   *
   * @return the double value
   */
  public double evalDouble(QueryContext context)
    throws SQLException
  {
    if (! _expr.isNull(context))
      return _expr.evalDouble(context);
    else
      return _default.evalDouble(context);
  }

  /**
   * Evaluates the expression as a date.
   *
   * @param rows the current database tuple
   *
   * @return the double value
   */
  public long evalDate(QueryContext context)
    throws SQLException
  {
    if (! _expr.isNull(context))
      return _expr.evalDate(context);
    else
      return _default.evalDate(context);
  }

  /**
   * Evaluates the expression, writing to the output stream.
   *
   * @param ws the output write stream

   */
  public void evalToResult(QueryContext context, SelectResult result)
    throws SQLException
  {
    if (! _expr.isNull(context))
      _expr.evalToResult(context, result);
    else
      _default.evalToResult(context, result);
  }

  public String toString()
  {
    return "(" + _expr + " DEFAULT " + _default + ")";
  }
}
