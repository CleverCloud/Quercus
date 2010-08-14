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
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.AmberType;
import com.caucho.util.CharBuffer;

/**
 * Expression to a collection of rows
 *
 * The relation is maintained by a link from the child objects
 * to the parent object.
 */
public class OneToManyExpr extends AbstractPathExpr {
  private PathExpr _parent;

  // Link from the target to the parent
  private LinkColumns _linkColumns;

  private FromItem _fromItem;
  private FromItem _childFromItem;

  /**
   * Creates a new expression to the child objects.
   */
  public OneToManyExpr(QueryParser parser,
                       PathExpr parent,
                       LinkColumns linkColumns)
  {
    _parent = parent;

    _linkColumns = linkColumns;
  }

  /**
   * Returns the link columns.
   */
  public LinkColumns getLinkColumns()
  {
    return _linkColumns;
  }

  /**
   * Returns the expr type.
   */
  public AmberType getType()
  {
    return _linkColumns.getSourceTable().getType();
  }

  /**
   * Returns the expr type.
   */
  public EntityType getTargetType()
  {
    return (EntityType) getType();
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    return (from == _childFromItem ||
            type == IS_INNER_JOIN && _parent.usesFrom(from, type));
  }

  /**
   * Returns the parent.
   */
  public PathExpr getParent()
  {
    return _parent;
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    return bindSelect(parser, null);
  }

  /**
   * Binds the expression as a select item.
   */
  public PathExpr bindSelect(QueryParser parser, String id)
  {
    if (_fromItem != null)
      return this;

    _fromItem = _parent.bindSubPath(parser);

    AmberTable sourceTable = _linkColumns.getSourceTable();
    _childFromItem = parser.addFromItem(sourceTable, id);

    JoinExpr joinExpr;
    joinExpr = new OneToManyJoinExpr(_linkColumns,
                                     _childFromItem,
                                     _fromItem);

    _childFromItem.setJoinExpr(joinExpr);
    _childFromItem.setCollectionExpr(this);

    return this;
  }

  /**
   * Returns the child from item.
   */
  public FromItem getChildFromItem()
  {
    return _childFromItem;
  }

  /**
   * Binds the expression as a subpath.
   */
  public FromItem bindSubPath(QueryParser parser)
  {
    if (_childFromItem == null)
      bindSelect(parser, null);

    return _childFromItem;
  }

  /**
   * Returns the table.
   */
  public AmberTable getTable()
  {
    // return _field.getTable();
    return _fromItem.getTable();
  }

  /**
   * Generates the where expression.
   */
  public void generateWhere(CharBuffer cb)
  {
    throw new IllegalStateException(getClass().getName());
  }

  /**
   * Generates the (update) where expression.
   */
  public void generateUpdateWhere(CharBuffer cb)
  {
    generateWhere(cb);
  }

  /**
   * Generates the having expression.
   */
  public void generateHaving(CharBuffer cb)
  {
    generateWhere(cb);
  }

  /**
   * Generates the where expression.
   */
  public void generateSelect(CharBuffer cb)
  {
    String id = _childFromItem.getName();

    cb.append(_linkColumns.generateSelectSQL(id));
  }

  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    OneToManyExpr oneToMany = (OneToManyExpr) o;

    return (_parent.equals(oneToMany._parent) &&
            _linkColumns.equals(oneToMany._linkColumns));
  }

  public String toString()
  {
    return "OneToManyExpr[" +  _parent + "," + _linkColumns + "]";
  }
}
