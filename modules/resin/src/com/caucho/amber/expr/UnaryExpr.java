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
import com.caucho.amber.table.ForeignColumn;
import com.caucho.util.CharBuffer;

import java.util.ArrayList;

/**
 * Unary expression.
 */
public class UnaryExpr extends AbstractAmberExpr {
  private AmberExpr _expr;
  private int _token;

  /**
   * Creates a new unary expression
   */
  public UnaryExpr(int token, AmberExpr expr)
  {
    _token = token;
    _expr = expr;
  }

  /**
   * Returns true for a boolean expr.
   */
  public boolean isBoolean()
  {
    return (_token == QueryParser.NOT ||
            _token == QueryParser.NULL ||
            _token == QueryParser.NOT_NULL);
  }

  /**
   * Returns the java type.
   */
  public Class getJavaType()
  {
    switch (_token) {
    case QueryParser.NOT:
    case QueryParser.NULL:
      return boolean.class;
    default:
      return double.class;
    }
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    _expr = _expr.bindSelect(parser);

    return this;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    switch (_token) {
    case '-':
    case '+':
    case QueryParser.NULL:
    case QueryParser.NOT_NULL:
      return _expr.usesFrom(from, type);

    case QueryParser.NOT:
      return _expr.usesFrom(from, type, ! isNot);

    default:
      return false;
    }
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

  public String toString()
  {
    String str = null;

    switch (_token) {
    case '-':
      str = "-";
      break;
    case '+':
      str = "+";
      break;
    case QueryParser.NOT:
      str = "not";
      break;
    case QueryParser.NULL:
      return _expr + " is null";
    case QueryParser.NOT_NULL:
      return _expr + " is not null";
    }

    return str + " " + _expr;
  }

  //
  // private

  private void generateInternalWhere(CharBuffer cb,
                                     boolean select)
  {
    switch (_token) {
    case '-':
      cb.append(" -");
      break;
    case '+':
      cb.append(" +");
      break;
    case QueryParser.NOT:
      cb.append("NOT ");
      break;
    case QueryParser.NULL:
    case QueryParser.NOT_NULL:
      if (_expr instanceof ManyToOneExpr) {
        ManyToOneExpr path = (ManyToOneExpr) _expr;
        cb.append('(');

        ArrayList<ForeignColumn> keys = path.getLinkColumns().getColumns();
        for (int i = 0; i < keys.size(); i++) {
          if (i != 0)
            cb.append(" and ");

          cb.append(path.getFromItem().getName());
          cb.append(".");
          cb.append(keys.get(i).getName());

          if (_token == QueryParser.NULL)
            cb.append(" is null");
          else
            cb.append(" is not null");
        }
        cb.append(')');
      }
      else {

        if (select)
          _expr.generateWhere(cb);
        else
          _expr.generateUpdateWhere(cb);

        if (_token == QueryParser.NULL)
          cb.append(" is null");
        else
          cb.append(" is not null");
      }
      return;
    }

    if (select)
      _expr.generateWhere(cb);
    else
      _expr.generateUpdateWhere(cb);
  }
}
