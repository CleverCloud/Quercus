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

class StringEqExpr extends Expr {
  private ColumnExpr _column;
  private Expr _right;

  StringEqExpr(ColumnExpr left, Expr right)
  {
    _column = left;
    _right = right;
  }

  public Expr bind(Query query)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the cost based on the given FromList.
   */
  public long subCost(ArrayList<FromItem> fromList)
  {
    return _column.subCost(fromList) + _right.subCost(fromList);
  }

  /**
   * Returns the type of the expression.
   */
  public Class getType()
  {
    return boolean.class;
  }

  /**
   * Returns true for a null value.
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    return _column.isNull(context);
  }

  /**
   * Evaluates the expression as a boolean.
   */
  public int evalBoolean(QueryContext context)
    throws SQLException
  {
    if (_right.isNull(context))
      return UNKNOWN;
    else if (_column.isNull(context))
      return UNKNOWN;
    
    if (_column.evalEqual(context, _right.evalString(context)))
      return TRUE;
    else
      return FALSE;
  }

  public String evalString(QueryContext context)
    throws SQLException
  {
    throw new SQLException("can't convert string to boolean");
  }

  public String toString()
  {
    return "(" + _column + " = " + _right + ")";
  }
}
