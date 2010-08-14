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

import com.caucho.inject.Module;

@Module
public class SubSelectParamExpr extends Expr {
  private SelectQuery _subselect;
  private Expr _expr;
  private int _index;

  SubSelectParamExpr(Query subselect, Expr expr, int index)
  {
    _subselect = (SelectQuery) subselect;
    _expr = expr;
    _index = index;
    
    if (index < 0)
      throw new IllegalStateException("index: " + index + " must be >= 0");
  }

  /**
   * Returns the type of the expression.
   */
  public Class getType()
  {
    return _expr.getType();
  }

  /**
   * Returns the expr.
   */
  public Expr getExpr()
  {
    return _expr;
  }

  /**
   * Returns the cost based on the given FromList.
   */
  public long subCost(ArrayList<FromItem> fromList)
  {
    return _subselect.getSubSelect().cost(fromList) + 1;
  }

  /**
   * Binds the expression.
   */
  public Expr bind(Query parent)
    throws SQLException
  {
    _expr = _expr.bind(parent);

    return this;
  }

  /**
   * Sets the value.
   */
  public void eval(QueryContext parent, QueryContext context)
    throws SQLException
  {
    Class type = getType();

    if (_expr.isNull(parent))
      context.setNull(_index + 1);
    else if (long.class.equals(type))
      context.setLong(_index + 1, _expr.evalLong(parent));
    else if (int.class.equals(type))
      context.setLong(_index + 1, _expr.evalLong(parent));
    else {
      context.setString(_index + 1, _expr.evalString(parent));
    }
  }

  /**
   * Evaluates the expression as a string.
   *
   * @param rows the current database tuple
   *
   * @return the string value
   */
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    return context.isNull(_index + 1);
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
    return context.getString(_index + 1);
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
    return context.getBoolean(_index + 1);
  }

  /**
   * Evaluates the expression as a long.
   *
   * @param rows the current database tuple
   *
   * @return the long value
   */
  @Override
  public long evalLong(QueryContext context)
    throws SQLException
  {
    return context.getLong(_index + 1);
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
    return context.getDouble(_index + 1);
  }

  /**
   * Evaluates the expression as a date
   *
   * @param rows the current database tuple
   *
   * @return the date value
   */
  public long evalDate(QueryContext context)
    throws SQLException
  {
    return context.getDate(_index + 1);
  }

  public String toString()
  {
    return "SubSelectParamExpr[" + _expr + "]";
  }
}
