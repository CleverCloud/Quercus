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

import com.caucho.amber.entity.Entity;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.field.AmberField;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.query.FromItem;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.BeanType;
import com.caucho.amber.type.EmbeddableType;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.AmberType;
import com.caucho.util.CharBuffer;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * An embedded or entity expression which should be loaded.
 */
abstract public class LoadExpr extends AbstractAmberExpr {
  PathExpr _expr;
  FromItem _fromItem;
  FromItem _rootItem;
  int _index;

  ArrayList<FromItem> _subItems = new ArrayList<FromItem>();

  public static LoadExpr create(PathExpr expr)
  {
    if (expr instanceof EmbeddedExpr)
      return new LoadEmbeddedExpr(expr);

    return new LoadEntityExpr(expr);
  }

  public static LoadExpr create(PathExpr expr,
                                FromItem rootItem)
  {
    LoadExpr loadExpr = expr.createLoad();

    loadExpr._rootItem = rootItem;

    return loadExpr;
  }

  LoadExpr(PathExpr expr)
  {
    _expr = expr;
  }

  /**
   * Returns the type.
   */
  public AmberType getType()
  {
    return _expr.getTargetType();
  }

  /**
   * Returns the underlying expression
   */
  public PathExpr getExpr()
  {
    return _expr;
  }

  /**
   * Returns the number of columns consumed from
   * a result set after loading the entity.
   */
  public int getIndex()
  {
    return _index;
  }

  /**
   * Returns the table.
   */
  public String getTable()
  {
    return _fromItem.getName();
  }

  /**
   * Binds the expression as a select item.
   */
  public FromItem bindSubPath(QueryParser parser)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    if (_fromItem == from)
      return true;

    for (int i = 0; i < _subItems.size(); i++) {
      FromItem subItem = _subItems.get(i);

      if (from == subItem)
        return true;
    }

    return _expr.usesFrom(from, type, isNot);
  }

  /**
   * Returns the from item
   */
  public FromItem getChildFromItem()
  {
    return _expr.getChildFromItem();
  }

  /**
   * Generates the where expression.
   */
  public void generateSelect(CharBuffer cb)
  {
    generateSelect(cb, true);
  }

  /**
   * Generates the where expression.
   */
  public void generateSelect(CharBuffer cb,
                             boolean fullSelect)
  {
    BeanType type = (BeanType) getType();

    if (type instanceof EmbeddableType) {
      _expr.generateSelect(cb);
      return;
    }

    if (type instanceof EntityType) {
      EntityType relatedType = (EntityType) type;
      cb.append(relatedType.getId().generateSelect(getTable()));
    }

    if (! fullSelect)
      return;

    FromItem item = _fromItem;

    // jpa/0l4b
    if (_rootItem != null) {
      EntityType parentType = (EntityType) type;

      while (parentType.getParentType() != null
             && parentType.getParentType() instanceof EntityType) {
        parentType = parentType.getParentType();
      }

      item = _rootItem;
    }

    String valueSelect = "";

    // jpa/0l12, jpa/0l47
    valueSelect = type.generateLoadSelect(item.getTable(),
                                          item.getName());

    if (valueSelect != null && ! "".equals(valueSelect)) {
      cb.append(", ");
      cb.append(valueSelect);
    }

    for (int i = 0; i < _subItems.size(); i++) {
      item = _subItems.get(i);

      valueSelect = type.generateLoadSelect(item.getTable(), item.getName());

      if (! valueSelect.equals("")) {
        cb.append(", ");
        cb.append(valueSelect);
      }
    }
  }

  /**
   * Generates the where expression.
   */
  public void generateWhere(CharBuffer cb, String fieldName)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the (update) where expression.
   */
  public void generateUpdateWhere(CharBuffer cb, String fieldName)
  {
    generateWhere(cb, fieldName);
  }

  /**
   * Generates the having expression.
   */
  public void generateHaving(CharBuffer cb, String fieldName)
  {
    generateWhere(cb, fieldName);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _expr + "," + getType() + "]";
  }
}
