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

package com.caucho.amber.expr;

import com.caucho.amber.query.FromItem;
import com.caucho.amber.query.QueryParseException;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.type.AmberType;
import com.caucho.util.CharBuffer;


/**
 * Bound identifier expression.
 */
public class ColumnExpr extends AbstractAmberExpr {
  protected PathExpr _parent;
  // identifier name value
  private AmberColumn _column;

  protected FromItem _fromItem;

  /**
   * Creates a new unbound id expression.
   */
  public ColumnExpr(PathExpr parent, AmberColumn column)
  {
    _parent = parent;
    _column = column;
  }

  /**
   * Returns the parent.
   */
  public PathExpr getParent()
  {
    return _parent;
  }

  /**
   * Returns the name.
   */
  public AmberColumn getColumn()
  {
    return _column;
  }

  /**
   * Returns the expr type.
   */
  public AmberType getType()
  {
    return getColumn().getType();
  }

  /**
   * Returns true if this expr has any relationship.
   */
  public boolean hasRelationship()
  {
    // jpa/1232
    return ! (_parent instanceof IdExpr);
  }

  /**
   * Returns a boolean expression.
   */
  public AmberExpr createBoolean()
    throws QueryParseException
  {
    if (getColumn().getType().isBoolean())
      return new BooleanColumnExpr(_parent, _column);
    else
      return super.createBoolean();
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    FromItem parentFromItem = _parent.bindSubPath(parser);

    if (parentFromItem.getTable() == getColumn().getTable()) {
      _fromItem = parentFromItem;

      return this;
    }

    LinkColumns link = getColumn().getTable().getDependentIdLink();

    if (link == null)
      return this;

    _fromItem = parser.createDependentFromItem(parentFromItem, link);

    return this;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    if (from.isOuterJoin() && type == AmberExpr.IS_INNER_JOIN) {
      // jpa/115a
      return false;
    }

    if (_fromItem == from)
      return true;
    else if (_fromItem == null && _parent.getChildFromItem() == from)
      return true;
    else
      return false;
  }

  /**
   * Replaces linked join to eliminate a table.
   */
  public AmberExpr replaceJoin(JoinExpr join)
  {
    _parent = (PathExpr) _parent.replaceJoin(join);

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
    return _parent + "." + _column.getName();
  }

  //
  // private

  private void generateInternalWhere(CharBuffer cb,
                                     boolean select)
  {
    if (_fromItem != null) {

      if (select) {
        cb.append(_fromItem.getName());
        cb.append('.');
      }

      cb.append(_column.getName());
    }
    else {

      if (select) {
        cb.append(_parent.getChildFromItem().getName());
        cb.append('.');
      }

      cb.append(_column.getName());
    }
  }
}
