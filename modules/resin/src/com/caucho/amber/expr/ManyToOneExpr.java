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
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.AmberType;
import com.caucho.util.CharBuffer;

/**
 * Link expression to a new table
 */
public class ManyToOneExpr extends AbstractPathExpr {
  private PathExpr _parent;

  private LinkColumns _linkColumns;

  private FromItem _fromItem;
  private FromItem _childFromItem;

  /**
   * Creates a new unbound id expression.
   */
  public ManyToOneExpr(PathExpr parent, LinkColumns linkColumns)
  {
    _parent = parent;
    _linkColumns = linkColumns;
  }

  /**
   * Returns the entity class.
   */
  public EntityType getTargetType()
  {
    return _linkColumns.getTargetTable().getType();
  }

  /**
   * Returns the entity class.
   */
  public AmberType getType()
  {
    return getTargetType();
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    _fromItem = _parent.bindSubPath(parser);

    return this;
  }

  /**
   * Return the parent from item.
   */
  public FromItem getFromItem()
  {
    return _fromItem;
  }

  /**
   * Returns the link columns.
   */
  public LinkColumns getLinkColumns()
  {
    return _linkColumns;
  }

  /**
   * Return the child from item.
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
    if (_childFromItem != null)
      return _childFromItem;

    ManyToOneExpr pathExpr = (ManyToOneExpr) parser.addPath(this);

    if (pathExpr != this) {
      _fromItem = pathExpr._fromItem;
      _childFromItem = pathExpr._childFromItem;

      return _childFromItem;
    }

    // XXX: handled at constructor?
    _parent = _parent.bindSelect(parser, null);

    bindSelect(parser, parser.createTableName());

    return _childFromItem;
  }

  /**
   * Binds the expression as a select item.
   */
  public PathExpr bindSelect(QueryParser parser, String id)
  {
    if (_childFromItem != null)
      return this;

    if (_fromItem == null)
      _fromItem = _parent.bindSubPath(parser);

    AmberTable targetTable = _linkColumns.getTargetTable();
    _childFromItem = parser.addFromItem(targetTable, id);

    JoinExpr joinExpr;
    joinExpr = new ManyToOneJoinExpr(_linkColumns,
                                     _fromItem,
                                     _childFromItem);

    _childFromItem.setJoinExpr(joinExpr);

    return this;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    return (_childFromItem == from && type == IS_INNER_JOIN
            || _fromItem == from
            || _parent.usesFrom(from, type));
  }

  /**
   * Returns true if the item is forced to exist by the parent
   */
  @Override
  public boolean exists(FromItem from)
  {
    return (_fromItem == from
            && _parent.exists());
  }

  /**
   * Returns the table.
   */
  /*
    public AmberTable getTable()
    {
    if (_childFromItem != null)
    return _childFromItem.getTable();
    else if (_fromItem != null)
    return _fromItem.getTable();
    else
    return _parent.getTable();
    }
  */

  /**
   * Generates the where expression.
   */
  public void generateMatchArgWhere(CharBuffer cb)
  {
    if (_fromItem != null) {
      cb.append(_linkColumns.generateMatchArgSQL(_fromItem.getName()));
    }
    else {
      cb.append(_linkColumns.generateMatchArgSQL(_parent.getChildFromItem().getName()));
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
   * Generates the select expression.
   */
  // ejb/06q4
  public void generateSelect(CharBuffer cb)
  {
    String tableName;

    if (_fromItem != null)
      tableName = _fromItem.getName();
    else
      tableName = _parent.getChildFromItem().getName();

    cb.append(_linkColumns.generateSelectSQL(tableName));
  }

  /**
   * Creates a load expression.
   */
  @Override
  public LoadExpr createLoad()
  {
    return new LoadEntityExpr(this);
  }

  /**
   * Returns the parent.
   */
  public PathExpr getParent()
  {
    return _parent;
  }

  public int hashCode()
  {
    return 65521 * _parent.hashCode() + _linkColumns.hashCode();
  }

  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    ManyToOneExpr manyToOne = (ManyToOneExpr) o;

    return (_parent.equals(manyToOne._parent) &&
            _linkColumns.equals(manyToOne._linkColumns));
  }

  public String toString()
  {
    return "ManyToOneExpr[" + _childFromItem + "," + _fromItem + "," + _parent + "]";
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

      cb.append(_linkColumns.getColumns().get(0).getName());
    }
    else {

      if (select)
        super.generateWhere(cb);
      else
        super.generateUpdateWhere(cb);
    }
  }
}
