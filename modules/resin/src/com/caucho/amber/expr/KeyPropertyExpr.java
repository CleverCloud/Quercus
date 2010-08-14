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

import com.caucho.amber.field.KeyPropertyField;
import com.caucho.amber.query.FromItem;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.AmberType;

/**
 * Bound identifier expression.
 */
public class KeyPropertyExpr extends AbstractAmberExpr implements IdFieldExpr {
  protected PathExpr _parent;
  private KeyPropertyField _field;
  
  /**
   * Creates a new unbound id expression.
   */
  public KeyPropertyExpr(PathExpr parent, KeyPropertyField field)
  {
    _parent = parent;
    _field = field;

    // XXX: ejb/0a08
    if (_field.getType() instanceof EntityType)
      throw new IllegalStateException();
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    _parent = (PathExpr) _parent.bindSelect(parser);
    
    return this;
  }

  /**
   * Returns the parent expression.
   */
  public PathExpr getParent()
  {
    return _parent;
  }

  /**
   * Returns the parent expression.
   */
  public KeyPropertyField getField()
  {
    return _field;
  }

  /**
   * Returns the expr type
   */
  public AmberType getType()
  {
    return getField().getType();
  }

  /**
   * Returns the parent expression.
   */
  public AmberColumn getColumn()
  {
    return getField().getColumn();
  }
  
  /**
   * Creates the expr from the path.
   */
  /*
  public PathExpr createField(QueryParser parser, String name)
  {
    AmberType type = getType();

    if (! (type instanceof EntityType))
      return null;

    EntityType entityType = (EntityType) type;
    
    AbstractField field = entityType.getField(name);

    if (field == null)
      return null;
    else {
      EntityPathExpr dst = (EntityPathExpr) getField().createExpr(parser,
                                                 new KeyManyToOneExpr(this));
      PathExpr result = field.createExpr(parser, (EntityPathExprdst);
      
      return result;
    }
  }
  */

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    if (_parent instanceof IdExpr) {
      // IdExpr parent = (IdExpr) _parent;
      // return (parent.getFromItem() == from && type == USES_ANY);
      
      return type == IS_INNER_JOIN && _parent.usesFrom(from, type);
    }
    else
      return _parent.usesFrom(from, type);
  }

  /**
   * Replaces linked join to eliminate a table.
   */
  /*
  public AmberExpr replaceJoin(JoinExpr join)
  {
    // _parent = (EntityPathExpr) _parent.replaceJoin(join);

    if (_parent instanceof IdExpr)
      return join.replace(this);
    else
      return super.replaceJoin(join);
  }
  */

  /**
   * Returns the child from item.
   *
   * XXX: untested
   */
  public FromItem getChildFromItem()
  {
    return _parent.getChildFromItem();
  }

  /**
   * Returns the field string.
   */
  public String toString()
  {
    return "KeyPropertyExpr[" + _parent + "," + _field + "]";
  }
}
