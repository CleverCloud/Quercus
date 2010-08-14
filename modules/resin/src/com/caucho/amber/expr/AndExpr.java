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
import com.caucho.amber.query.QueryParseException;
import com.caucho.amber.query.QueryParser;
import com.caucho.util.CharBuffer;

import java.util.ArrayList;

/**
 * A conjunction.
 */
public class AndExpr extends AbstractAmberExpr {
  private ArrayList<AmberExpr> _components = new ArrayList<AmberExpr>();

  /**
   * Creates the and.
   */
  public static AmberExpr create(AmberExpr left, AmberExpr right)
    throws QueryParseException
  {
    if (left == null && right == null)
      return null;
    else if (left == null)
      return right.createBoolean();
    else if (right == null)
      return left.createBoolean();
    else if (left instanceof AndExpr) {
      AndExpr and = (AndExpr) left;
      and.add(right.createBoolean());
      return and;
    }
    else if (right instanceof AndExpr) {
      AndExpr and = (AndExpr) right;
      and.add(left.createBoolean());
      return and;
    }
    else {
      AndExpr and = new AndExpr();
      and.add(left.createBoolean());
      and.add(right.createBoolean());

      return and;
    }
  }
  /**
   * Returns true for a boolean expression.
   */
  public boolean isBoolean()
  {
    return true;
  }

  /**
   * Adds a new component.
   */
  public void add(AmberExpr expr)
  {
    _components.add(expr);
  }

  /**
   * Returns the components.
   */
  public ArrayList<AmberExpr> getComponents()
  {
    return _components;
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    for (int i = 0; i < _components.size(); i++) {
      AmberExpr expr = _components.get(i);

      expr = expr.bindSelect(parser);

      _components.set(i, expr);
    }

    return this;
  }

  /**
   * Returns a single expression.
   */
  public AmberExpr getSingle()
  {
    if (_components.size() == 0)
      return null;
    else if (_components.size() == 1)
      return _components.get(0);
    else
      return this;
  }


  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    if (type == IS_INNER_JOIN) {
      // returns true if the from item is used in any term of the conjunction
      for (int i = 0; i < _components.size(); i++) {
        AmberExpr expr = _components.get(i);

        if (! isNot && expr.usesFrom(from, type, isNot))
          return true;
        else if (isNot && ! expr.usesFrom(from, type, isNot))
          return false;
      }

      return false;
    }
    else {
      for (int i = 0; i < _components.size(); i++) {
        AmberExpr expr = _components.get(i);

        if (expr.usesFrom(from, type))
          return true;
      }

      return false;
    }
  }

  /**
   * Returns true if the from item is used in the expression
   */
  @Override
  public boolean exists(FromItem from)
  {
    for (int i = 0; i < _components.size(); i++) {
      if (_components.get(i).exists(from))
        return true;
    }

    return false;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public AmberExpr replaceJoin(JoinExpr join)
  {
    for (int i = 0; i < _components.size(); i++) {
      AmberExpr expr = _components.get(i);

      expr = expr.replaceJoin(join);

      _components.set(i, expr);
    }

    return this;
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
   * Generates the join expression.
   */
  public void generateJoin(CharBuffer cb)
  {
    cb.append('(');

    for (int i = 0; i < _components.size(); i++) {
      if (i != 0)
        cb.append(" and ");

      AmberExpr expr = _components.get(i);

      expr.generateJoin(cb);
    }

    cb.append(')');
  }

  /**
   * Binds the argument type based on another expr.
   */
  public void setInternalArgType(AmberExpr other)
  {
    for (int i = 0; i < _components.size(); i++)
      _components.get(i).setInternalArgType(other);
  }

  //
  // private

  private void generateInternalWhere(CharBuffer cb,
                                     boolean select)
  {
    cb.append('(');

    for (int i = 0; i < _components.size(); i++) {
      if (i != 0)
        cb.append(" and ");

      AmberExpr expr = _components.get(i);

      if (select)
        expr.generateWhere(cb);
      else
        expr.generateUpdateWhere(cb);
    }

    cb.append(')');
  }
}
