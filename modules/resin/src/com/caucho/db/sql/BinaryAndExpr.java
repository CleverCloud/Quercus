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

import com.caucho.util.CharBuffer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

final class BinaryAndExpr extends AbstractBinaryBooleanExpr {
  private final Expr _left;
  private final Expr _right;

  BinaryAndExpr(Expr left, Expr right)
  {
    _left = left;
    _right = right;
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
    return new BinaryAndExpr(left, right);
  }

  @Override
  public Expr bind(Query query)
    throws SQLException
  {
    Expr newLeft = _left.bind(query);

    if (! newLeft.getType().equals(boolean.class))
      throw new SQLException(L.l("AND requires boolean operands at {0}",
                                 newLeft));

    Expr newRight = _right.bind(query);

    if (! newRight.getType().equals(boolean.class))
      throw new SQLException(L.l("AND requires boolean operands at {0}",
                                 newRight));

    return create(newLeft, newRight);
  }

  /**
   * Returns true for a null expression
   */
  @Override
  public boolean isNull(final QueryContext context)
    throws SQLException
  {
    final int leftValue = _left.evalBoolean(context);

    if (leftValue == FALSE)
      return false;

    final int rightValue = _right.evalBoolean(context);

    if (rightValue == FALSE)
      return false;

    return leftValue == UNKNOWN || rightValue == UNKNOWN;
  }

  /**
   * Evaluates the expression as a boolean.
   */
  @Override
  public int evalBoolean(final QueryContext context)
    throws SQLException
  {
    final int leftValue = _left.evalBoolean(context);

    if (leftValue == FALSE)
      return FALSE;

    final int rightValue = _right.evalBoolean(context);

    if (rightValue == FALSE)
      return FALSE;

    if (leftValue == UNKNOWN || rightValue == UNKNOWN)
      return UNKNOWN;
    else
      return TRUE;
  }

  /**
   * Evaluates the expression as a boolean.
   */
  @Override
  public final boolean isSelect(final QueryContext context)
    throws SQLException
  {
    return _left.isSelect(context) && _right.isSelect(context);
  }

  @Override
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

  @Override
  public String toString()
  {
    return "(" + _left + " AND " + _right + ")";
  }
}
