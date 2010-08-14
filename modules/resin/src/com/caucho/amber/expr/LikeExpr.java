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
import com.caucho.amber.type.BooleanType;
import com.caucho.amber.type.AmberType;
import com.caucho.util.CharBuffer;

/**
 * Bound identifier expression.
 */
public class LikeExpr extends AbstractAmberExpr {
  private AmberExpr _expr;

  private AmberExpr _value;
  private String _escape;
  private boolean _isNot;

  /**
   * Creates a new cmp expression
   */
  public LikeExpr(AmberExpr expr,
                  AmberExpr value,
                  String escape,
                  boolean isNot)
  {
    _expr = expr;
    _value = value;
    _isNot = isNot;
    _escape = escape;
  }

  /**
   * Returns true for a boolean expression.
   */
  public boolean isBoolean()
  {
    return true;
  }

  /**
   * Returns the expr type.
   */
  public AmberType getType()
  {
    return BooleanType.create();
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    _expr = _expr.bindSelect(parser);
    _value = _value.bindSelect(parser);

    return this;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    return (_expr.usesFrom(from, type));
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public AmberExpr replaceJoin(JoinExpr join)
  {
    _expr = _expr.replaceJoin(join);
    _value = _value.replaceJoin(join);

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

  public String toString()
  {
    CharBuffer cb = CharBuffer.allocate();

    cb.append('(');
    cb.append(_expr);

    if (_isNot)
      cb.append(" NOT");

    cb.append(" LIKE ");

    cb.append(_value);

    cb.append(')');

    return cb.close();
  }

  //
  // private

  private void generateInternalWhere(CharBuffer cb,
                                     boolean select)
  {
    cb.append('(');

    if (select)
      _expr.generateWhere(cb);
    else
      _expr.generateUpdateWhere(cb);

    if (_isNot)
      cb.append(" NOT");

    cb.append(" LIKE ");

    if (select)
      _value.generateWhere(cb);
    else
      _value.generateUpdateWhere(cb);

    if (_escape != null) {
      cb.append(" ESCAPE ");
      cb.append(_escape);
    }

    cb.append(')');
  }
}
