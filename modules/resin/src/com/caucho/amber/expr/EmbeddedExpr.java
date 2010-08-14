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

import com.caucho.amber.field.EntityEmbeddedField;
import com.caucho.amber.field.EmbeddedSubField;
import com.caucho.amber.query.FromItem;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.type.EmbeddableType;
import com.caucho.amber.type.AmberType;
import com.caucho.util.CharBuffer;

import java.util.ArrayList;
import java.util.Map;

/**
 * Embedded path expression
 */
public class EmbeddedExpr extends AbstractPathExpr {
  private PathExpr _parent;

  private EmbeddableType _embeddableType;

  private ArrayList<EmbeddedSubField> _subFields;

  private FromItem _fromItem;
  private FromItem _childFromItem;

  /**
   * Creates a new expression.
   */
  public EmbeddedExpr(PathExpr parent,
                      EmbeddableType embeddableType,
                      ArrayList<EmbeddedSubField> subFields)
  {
    _parent = parent;
    _embeddableType = embeddableType;
    _subFields = subFields;
  }

  /**
   * Returns the target type.
   */
  public EmbeddableType getTargetType()
  {
    return _embeddableType;
  }

  /**
   * Returns the target type.
   */
  public AmberType getType()
  {
    return _embeddableType;
  }

  /**
   * Returns column by name.
   */
  public AmberColumn getColumnByFieldName(String fieldName)
  {
    for (int i = 0; i < _subFields.size(); i++) {
      EmbeddedSubField subField = _subFields.get(i);

      if (subField.getName().equals(fieldName))
        return subField.getColumn();
    }

    return null;
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
    // if (_childFromItem != null)
    //   return _childFromItem;

    FromItem parentFromItem = _parent.bindSubPath(parser);

    _fromItem = parentFromItem;

    return _fromItem;

    /*
      EmbeddedExpr pathExpr = (EmbeddedExpr) parser.addPath(this);

      if (pathExpr != this) {
      _fromItem = pathExpr._fromItem;
      _childFromItem = pathExpr._childFromItem;

      return _childFromItem;
      }

      // XXX: handled at constructor?
      _parent = _parent.bindSelect(parser, null);

      bindSelect(parser, parser.createTableName());

      return _childFromItem;
    */
  }

  /**
   * Binds the expression as a select item.
   */
  public PathExpr bindSelect(QueryParser parser, String id)
  {
    _fromItem = bindSubPath(parser);

    return this;

    /*
      if (_childFromItem != null)
      return this;

      if (_fromItem == null)
      _fromItem = _parent.bindSubPath(parser);

      return this;
    */
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
   * Generates the where expression.
   */
  public void generateMatchArgWhere(CharBuffer cb)
  {
    throw new UnsupportedOperationException();

    /*
      if (_fromItem != null) {
      cb.append(_linkColumns.generateMatchArgSQL(_fromItem.getName()));
      }
      else {
      cb.append(_linkColumns.generateMatchArgSQL(_parent.getChildFromItem().getName()));
      }
    */
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
  public void generateSelect(CharBuffer cb)
  {
    for (int i = 0; i < _subFields.size(); i++) {
      if (i > 0)
        cb.append(", ");
      
      cb.append(_subFields.get(i).generateSelect(null));
    }
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
    return 65521 * _parent.hashCode() + _subFields.hashCode();
  }

  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    EmbeddedExpr embedded = (EmbeddedExpr) o;

    return (_parent.equals(embedded._parent)
            && _subFields.equals(embedded._subFields));
  }

  public String toString()
  {
    return "EmbeddedExpr[" + _childFromItem + "," + _fromItem + "," + _parent + "]";
  }

  //
  // private

  private void generateInternalWhere(CharBuffer cb,
                                     boolean select)
  {
    throw new UnsupportedOperationException();
  }
}
