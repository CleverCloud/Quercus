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
import java.util.logging.Logger;

class BooleanLiteralExpr extends Expr {
  private static BooleanLiteralExpr TRUE_EXPR = new BooleanLiteralExpr(true);
  private static BooleanLiteralExpr FALSE_EXPR = new BooleanLiteralExpr(false);

  private final boolean _value;

  private BooleanLiteralExpr(boolean value)
  {
    _value = value;
  }

  static BooleanLiteralExpr create(boolean value)
  {
    return value ? TRUE_EXPR : FALSE_EXPR;
  }

  /**
   * Returns false because the literal is never null;
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    return false;
  }

  /**
   * Returns the type of the expression.
   */
  public Class getType()
  {
    return boolean.class;
  }

  /**
   * Returns true if the expression returns a boolean.
   */
  public boolean isBoolean()
  {
    return true;
  }

  /**
   * Returns the cost based on the given FromList.
   */
  public long subCost(ArrayList<FromItem> fromList)
  {
    return 0;
  }

  /**
   * Evaluates the literal as a string.
   */
  public String evalString(QueryContext context)
    throws SQLException
  {
    return _value ? "1" : "0";
  }

  /**
   * Evaluates the literal as a string.
   */
  public int evalBoolean(QueryContext context)
    throws SQLException
  {
    return _value ? TRUE : FALSE;
  }

  public String toString()
  {
    return _value ? "TRUE" : "FALSE";
  }
}
