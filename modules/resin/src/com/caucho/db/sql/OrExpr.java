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

class OrExpr extends Expr {
  private Expr _left;
  private Expr _right;

  OrExpr(Expr left, Expr right)
  {
    _left = left;
    _right = right;
  }

  public Expr bind(Query query)
    throws SQLException
  {
    Expr newLeft = _left.bind(query);
    Expr newRight = _right.bind(query);

    if (! newLeft.isBoolean())
      throw new SQLException(L.l("OR requires boolean operands"));
    if (! newRight.isBoolean())
      throw new SQLException(L.l("OR requires boolean operands"));

    if (_left == newLeft && _right == newRight)
      return this;
    else
      return new OrExpr(newLeft, newRight);
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
    return Integer.MAX_VALUE;
  }

  /**
   * Returns true if the expressoin evaluates to null
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    return evalBoolean(context) == UNKNOWN;
  }

  /**
   * Evaluates the expression as a boolean.
   */
  public int evalBoolean(QueryContext context)
    throws SQLException
  {
    int leftValue = _left.evalBoolean(context);

    if (leftValue == TRUE)
      return TRUE;

    int rightValue = _right.evalBoolean(context);

    if (rightValue == TRUE)
      return TRUE;

    if (leftValue == FALSE && rightValue == FALSE)
      return FALSE;
    else
      return UNKNOWN;
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
    return "(" + _left + " OR " + _right + ")";
  }
}
