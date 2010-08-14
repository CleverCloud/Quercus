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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.expr;

import com.caucho.amber.query.FromItem;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberColumn;
import com.caucho.util.CharBuffer;

import java.util.ArrayList;

/**
 * Links two tables by their key fields.
 */
public class EqualJoinExpr extends JoinExpr {
  private ArrayList<AmberColumn> _keyColumns;

  private FromItem _fromItemA;
  private FromItem _fromItemB;

  /**
   * Creates the expr.
   */
  EqualJoinExpr(ArrayList<AmberColumn> keyColumns,
                FromItem fromItemA,
                FromItem fromItemB)
  {
    _keyColumns = keyColumns;

    _fromItemA = fromItemA;
    _fromItemB = fromItemB;

    if (fromItemA == null || fromItemB == null)
      throw new NullPointerException();
  }

  /**
   * Returns true for a boolean expression.
   */
  public boolean isBoolean()
  {
    return true;
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    return this;
  }

  /**
   * Binds the link to the from item.
   */
  public boolean bindToFromItem()
  {
    if (_fromItemA.getJoinExpr() == null ||
        _fromItemA.getJoinExpr().equals(this)) {
      _fromItemA.setJoinExpr(this);
      return true;
    }
    else if (_fromItemB.getJoinExpr() == null) {
      _fromItemB.setJoinExpr(this);

      return true;
    }
    else
      return false;
  }

  /**
   * Returns the parent join clause.
   */
  public FromItem getJoinTarget()
  {
    return _fromItemA;
  }

  /**
   * Returns the id expr with the joined expression.
   */
  public AmberExpr replace(IdFieldExpr id)
  {
    IdExpr parent = (IdExpr) id.getParent();

    if (parent.getFromItem() == _fromItemA)
      return new ColumnExpr(new IdExpr(_fromItemA), id.getColumn());
    else if (parent.getFromItem() == _fromItemB)
      return new ColumnExpr(new IdExpr(_fromItemB), id.getColumn());
    else
      return id;
  }

  /**
   * Returns the id expr with the joined expression.
   */
  public AmberExpr replace(IdExpr id)
  {
    return id;
  }

  /**
   * Generates the where expression.
   */
  public void generateWhere(CharBuffer cb)
  {
    generateInternalWhere(cb, true);
  }

  /**
   * Generates the (update) where expression.
   */
  public void generateUpdateWhere(CharBuffer cb)
  {
    generateInternalWhere(cb, false);
  }

  /**
   * Generates the having expression.
   */
  public void generateHaving(CharBuffer cb)
  {
    generateWhere(cb);
  }

  /**
   * Test for equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof EqualJoinExpr))
      return false;

    EqualJoinExpr equalExpr = (EqualJoinExpr) o;

    return (_keyColumns.equals(equalExpr._keyColumns) &&
            _fromItemA.equals(equalExpr._fromItemA) &&
            _fromItemB.equals(equalExpr._fromItemB));
  }


  public String toString()
  {
    return ("EqualJoinExpr[" + _keyColumns + "," +
            _fromItemA + "," + _fromItemB + "]");
  }

  //
  // private

  private void generateInternalWhere(CharBuffer cb,
                                     boolean select)
  {
    cb.append('(');

    for (int i = 0; i < _keyColumns.size(); i++) {
      AmberColumn column = _keyColumns.get(i);

      if (i != 0)
        cb.append(" AND ");

      cb.append(_fromItemA.getName());
      cb.append('.');
      cb.append(column.getName());

      cb.append('=');

      cb.append(_fromItemB.getName());
      cb.append('.');
      cb.append(column.getName());
    }

    cb.append('(');
  }
}
