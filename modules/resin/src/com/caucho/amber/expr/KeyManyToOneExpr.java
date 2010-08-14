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

import com.caucho.amber.field.AmberField;
import com.caucho.amber.field.IdField;
import com.caucho.amber.field.KeyManyToOneField;
import com.caucho.amber.field.KeyPropertyField;
import com.caucho.amber.query.FromItem;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.type.EntityType;
import com.caucho.util.CharBuffer;

/**
 * Bound identifier expression.
 */
public class KeyManyToOneExpr extends AbstractPathExpr
  implements IdFieldExpr {
  private PathExpr _parent;
  // identifier name value
  private KeyManyToOneField _manyToOne;

  private FromItem _fromItem;
  private FromItem _childFromItem;

  /**
   * Creates a new unbound id expression.
   */
  public KeyManyToOneExpr(PathExpr parent, KeyManyToOneField manyToOne)
  {
    _parent = parent;
    _manyToOne = manyToOne;
  }

  /**
   * Returns the parent expression.
   */
  public PathExpr getParent()
  {
    return _parent;
  }

  /**
   * Returns the name.
   */
  public IdField getField()
  {
    return _manyToOne;
  }

  /**
   * Returns the entity class.
   */
  public EntityType getTargetType()
  {
    return _manyToOne.getEntityType();
  }

  /**
   * Returns the column.
   */
  public AmberColumn getColumn()
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Creates the expr from the path.
   */
  public AmberExpr createField(QueryParser parser, String name)
  {
    AmberField field = getTargetType().getField(name);

    if (field == null)
      return null;
    else if (field instanceof IdField) {
      KeyPropertyField idField = _manyToOne.getIdField((IdField) field);

      return idField.createExpr(parser, _parent);
    }
    else
      return field.createExpr(parser, this);
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

    KeyManyToOneExpr pathExpr = (KeyManyToOneExpr) parser.addPath(this);

    if (pathExpr != this) {
      _fromItem = pathExpr._fromItem;
      _childFromItem = pathExpr._childFromItem;

      return _childFromItem;
    }
    
    _fromItem = _parent.bindSubPath(parser);

    if (_fromItem != null)
      _childFromItem = _fromItem.getQuery().createFromItem(getTargetType().getTable(),
                                                           parser.createTableName());
    else
      _childFromItem = parser.getSelectQuery().createFromItem(getTargetType().getTable(),
                                                        parser.createTableName());

    JoinExpr link = new ManyToOneJoinExpr(_manyToOne.getLinkColumns(),
                                          _fromItem,
                                          _childFromItem);

    _childFromItem.setJoinExpr(link);
    
    return _childFromItem;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    return from == _childFromItem || _parent.usesFrom(from, type);
  }

  /**
   * Replaces linked join to eliminate a table.
   */
  /*
  public AmberExpr replaceJoin(JoinExpr join)
  {
    if (_parent instanceof IdExpr) {
      return join.replace(this);
    }
    else
      return super.replaceJoin(join);
  }
  */

  /**
   * Returns the table.
   */
  /*
  public String getTable()
  {
    if (_childFromItem != null)
      return _childFromItem.getName();
    else if (_fromItem != null)
      return _fromItem.getName();
    else
      return _parent.getChildFromItem().getName();
  }
  */
  
  /**
   * Generates the where expression.
   */
  public void generateMatchArgWhere(CharBuffer cb)
  {
    String table;
    
    if (_fromItem != null)
      table = _fromItem.getName();
    else
      table = _parent.getChildFromItem().getName();
    
    cb.append(_manyToOne.getLinkColumns().generateMatchArgSQL(table));
  }

  public String toString()
  {
    return _parent + "." + getField();
  }

  public int hashCode()
  {
    return 65521 *  _parent.hashCode() + getField().hashCode();
  }

  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    KeyManyToOneExpr manyToOne = (KeyManyToOneExpr) o;

    return (_parent.equals(manyToOne._parent) &&
            getField().equals(manyToOne.getField()));
  }
}
