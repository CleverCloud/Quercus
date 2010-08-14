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

class ConcatExpr extends Expr {
  private Expr _left;
  private Expr _right;

  ConcatExpr(Expr left, Expr right)
  {
    _left = left;
    _right = right;
  }

  public Expr bind(Query query)
    throws SQLException
  {
    _left = _left.bind(query);
    _right = _right.bind(query);

    return this;
  }

  /**
   * Returns the type of the expression.
   */
  public Class getType()
  {
    return String.class;
  }

  /**
   * Returns the cost based on the given FromList.
   */
  public long subCost(ArrayList<FromItem> fromList)
  {
    return _left.subCost(fromList) + _right.subCost(fromList);
  }

  /**
   * Evaluates the expression as a string.
   */
  public String evalString(QueryContext context)
    throws SQLException
  {
    String leftValue = _left.evalString(context);
    String rightValue = _right.evalString(context);

    if (leftValue == null)
      return rightValue;

    if (rightValue == null)
      return leftValue;

    return leftValue + rightValue;
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
    return "(" + _left + " || " + _right + ")";
  }
}
