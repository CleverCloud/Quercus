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
import java.util.HashSet;
import java.util.logging.Logger;

class InExpr extends Expr {
  private Expr _expr;
  private final HashSet<String> _values;
  private final boolean _isNot;

  InExpr(Expr expr, HashSet<String> values, boolean isNot)
  {
    _expr = expr;
    _values = values;
    _isNot = isNot;
  }

  /**
   * Binds the expression to the actual tables.
   */
  public Expr bind(Query query)
    throws SQLException
  {
    _expr = _expr.bind(query);

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
    return _expr.subCost(fromList);
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
    if (_values.contains(_expr.evalString(context)))
      return _isNot ? FALSE : TRUE;
    else
      return _isNot ? TRUE : FALSE;
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
    return (evalBoolean(context) == TRUE) ? "1" : "0";
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
  }

  public String toString()
  {
    return _expr + " IN " + _values;
  }
}
