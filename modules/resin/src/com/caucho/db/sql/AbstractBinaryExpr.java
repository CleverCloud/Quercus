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

abstract class AbstractBinaryExpr extends Expr {
  /**
   * Returns the left expression.
   */
  abstract protected Expr getLeft();

  /**
   * Returns the right expression.
   */
  abstract protected Expr getRight();

  /**
   * Creates a new instance of the expression for the <code>bind</code> method
   */
  protected Expr create(Expr left, Expr right)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * The binary expression is nullable if either subexpression is nullable.
   */
  @Override
  public boolean isNullable()
  {
    return getLeft().isNullable() || getRight().isNullable();
  }

  /**
   * Returns the cost based on the given FromList.
   */
  @Override
  public long subCost(ArrayList<FromItem> fromList)
  {
    return getLeft().subCost(fromList) + getRight().subCost(fromList);
  }

  /**
   * Binds the expression to the table variables.
   */
  @Override
  public Expr bind(Query query)
    throws SQLException
  {
    Expr left = getLeft();
    Expr right = getRight();

    Expr newLeft = left.bind(query);
    Expr newRight = right.bind(query);

    if (left == newLeft && right == newRight)
      return this;
    else
      return create(newLeft, newRight);
  }

  /**
   * Evaluates aggregate functions during the group phase.
   *
   * @param state the current database tuple
   */
  @Override
  public void evalGroup(QueryContext context)
    throws SQLException
  {
    getLeft().evalGroup(context);
    getRight().evalGroup(context);
  }
}
