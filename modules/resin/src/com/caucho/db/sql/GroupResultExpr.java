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

import com.caucho.util.L10N;

import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Represents a GROUP BY expression.
 */
public class GroupResultExpr extends Expr implements GroupExpr {
  protected static final L10N L = new L10N(GroupResultExpr.class);

  private static final int LONG_VALUE = 1;
  private static final int DOUBLE_VALUE = 2;
  private static final int STRING_VALUE = 3;

  private Expr _expr;
  private int _index;
  private int _type;

  GroupResultExpr(int index, Expr expr)
  {
    _index = index;
    _expr = expr;

    if (expr instanceof GroupResultExpr)
      Thread.dumpStack();

    if (_expr.isLong())
      _type = LONG_VALUE;
    else if (_expr.isDouble())
      _type = DOUBLE_VALUE;
    else
      _type = STRING_VALUE;
  }

  /**
   * Returns any name.
   */
  public String getName()
  {
    return _expr.getName();
  }

  public Expr bind(Query query)
    throws SQLException
  {
    _expr = _expr.bind(query);

    return this;
  }

  /**
   * Evaluates aggregate functions during the group phase.
   *
   * @param context the current database tuple
   */
  public void evalGroup(QueryContext context)
    throws SQLException
  {
    if (_expr.isNull(context))
      return;

    switch (_type) {
    default:
      {
        String value = _expr.evalString(context);

        context.setGroupString(_index, value);
      }
      break;
    }
  }

  public String evalString(QueryContext context)
  {
    return context.getGroupString(_index);
  }

  public String toString()
  {
    return "GroupResult[" + _expr + "]";
  }
}
