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
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.AmberType;
import com.caucho.amber.type.BeanType;
import com.caucho.amber.type.EntityType;
import com.caucho.util.CharBuffer;

/**
 * Bound identifier expression.
 */
public class IdExpr extends AbstractPathExpr {
  private FromItem _fromItem;

  /**
   * Creates a new unbound id expression.
   */
  public IdExpr(FromItem fromItem)
  {
    _fromItem = fromItem;

    //if (fromItem.getEntityType() == null)
    //  throw new NullPointerException();
  }

  /**
   * Returns the name.
   */
  String getId()
  {
    return _fromItem.getName();
  }

  /**
   * Returns the from item
   */
  public FromItem getFromItem()
  {
    return _fromItem;
  }

  /**
   * Returns the table
   */
  AmberTable getTable()
  {
    return _fromItem.getTable();
  }

  /**
   * Returns the from item
   */
  public FromItem getChildFromItem()
  {
    return getFromItem();
  }

  /**
   * Returns the entity class.
   */
  public BeanType getTargetType()
  {
    return _fromItem.getEntityType();
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    return this;
  }

  /**
   * Binds the expression as a select item.
   */
  public FromItem bindSubPath(QueryParser parser)
  {
    return _fromItem;
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
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    return (type == IS_INNER_JOIN && _fromItem == from);
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public AmberExpr replaceJoin(JoinExpr join)
  {
    return join.replace(this);
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
    return getId();
  }

  public int hashCode()
  {
    return _fromItem.hashCode();
  }

  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    IdExpr id = (IdExpr) o;

    return _fromItem.equals(id._fromItem);
  }

  //
  // private

  private void generateInternalWhere(CharBuffer cb,
                                     boolean select)
  {
    if (select) {
      cb.append(_fromItem.getName());
      cb.append('.');
    }

    EntityType entityType = (EntityType) getTargetType();

    cb.append(entityType.getId().getColumns().get(0).getName());
  }
}
