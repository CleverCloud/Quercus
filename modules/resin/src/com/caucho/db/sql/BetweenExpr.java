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

class BetweenExpr extends Expr {
  private Expr _expr;
  private Expr _min;
  private Expr _max;
  private boolean _isNot;

  private boolean _isLong;

  BetweenExpr(Expr expr, Expr min, Expr max, boolean isNot)
  {
    _expr = expr;
    _min = min;
    _max = max;
    _isNot = isNot;
  }

  public Expr bind(Query query)
    throws SQLException
  {
    _expr = _expr.bind(query);
    _min = _min.bind(query);
    _max = _max.bind(query);

    _isLong = _expr.isLong();

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
    return (_expr.subCost(fromList) +
            _min.subCost(fromList) +
            _max.subCost(fromList));
  }

  /**
   * Evaluates the expression as a boolean.
   */
  public int evalBoolean(QueryContext context)
    throws SQLException
  {
    if (_expr.isNull(context))
      return UNKNOWN;
    
    if (_isLong) {
      long min = _min.evalLong(context);
      long max = _max.evalLong(context);

      long value = _expr.evalLong(context);

      if (_isNot)
        return ! (min <= value && value <= max) ? TRUE : FALSE;
      else
        return min <= value && value <= max ? TRUE : FALSE;
    }
    else {
      double min = _min.evalDouble(context);
      double max = _max.evalDouble(context);

      double value = _expr.evalDouble(context);

      if (_isNot)
        return ! (min <= value && value <= max) ? TRUE : FALSE;
      else
        return min <= value && value <= max ? TRUE : FALSE;
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
    _expr.evalGroup(context);
    _min.evalGroup(context);
    _max.evalGroup(context);
  }

  public String toString()
  {
    return "(" + _expr + " BETWEEN " + _min + " AND " + _max + ")";
  }
}
