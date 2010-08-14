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

import com.caucho.amber.field.MapElementField;
import com.caucho.amber.query.FromItem;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.AmberType;
import com.caucho.util.CharBuffer;

/**
 * Bound identifier expression.
 */
public class MapFieldExpr extends AbstractAmberExpr {
  protected PathExpr _parent;

  private MapElementField _field;
  private AmberExpr _index;

  private FromItem _fromItem;
  private FromItem _childFromItem;

  /**
   * Creates a new unbound id expression.
   */
  public MapFieldExpr(PathExpr parent,
                      MapElementField field,
                      AmberExpr index)
  {
    _parent = parent;
    _field = field;
    _index = index;
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    _fromItem = _parent.bindSubPath(parser);

    // _childFromItem = parser.addFromItem(null, null, _field.getTable());

    // _childFromItem.setJoinExpr(new MapLinkExpr());

    _index = _index.bindSelect(parser);

    return this;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    return (_childFromItem == from ||
            _parent.usesFrom(from, type) ||
            _index.usesFrom(from, type));
  }

  /**
   * Returns the expr's type.
   */
  public AmberType getType()
  {
    return _field.getTargetType();
  }

  /**
   * Returns the expr's type.
   */
  public EntityType getTableType()
  {
    return (EntityType) getType();
  }

  /**
   * Generates the select expression.
   */
  public void generateSelect(CharBuffer cb)
  {
    String table = _childFromItem.getName();

    /*
      if (_fromItem != null)
      table = _fromItem.getName();
      else
      table = _parent.getTable();
    */

    cb.append(_field.generateTargetSelect(table));
  }

  /**
   * Generates the where expression.
   */
  public void generateWhere(CharBuffer cb)
  {
    cb.append(_field.generateTargetSelect(_childFromItem.getName()));
    /*
      cb.append(_childFromItem.getName());
      ArrayList<Column> column = _field.getColumns();

      cb.append('.');
      cb.append(column.get(0).getName());
    */
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

  public String toString()
  {
    return _parent + "." + _field;
  }
}
