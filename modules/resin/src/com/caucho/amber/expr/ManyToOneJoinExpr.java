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
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.util.CharBuffer;

/**
 * Links two tables.
 *
 * The parent table is "b" in "b.next".
 * The child table is "b.next"
 *
 * The source is "b", i.e. the parent.
 * The target is "b.next", i.e. the child.
 */
public class ManyToOneJoinExpr extends JoinExpr {
  private LinkColumns _linkColumns;

  private FromItem _sourceFromItem;
  private FromItem _targetFromItem;

  /**
   * Creates the expr.
   */
  public ManyToOneJoinExpr(LinkColumns link,
                           FromItem source,
                           FromItem target)
  {
    _linkColumns = link;

    _sourceFromItem = source;
    _targetFromItem = target;

    // commented out: jpa/10c9
    // if (source == null || target == null)

    if (target == null)
      throw new NullPointerException();
  }

  /**
   * Returns true for a boolean expression.
   */
  public boolean isBoolean()
  {
    return true;
  }

  /**
   * Returns true for a many-to-many expression.
   */
  public boolean isManyToMany()
  {
    if (_sourceFromItem == null)
      return false;

    return _sourceFromItem.getJoinExpr() instanceof OneToManyJoinExpr;
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    return this;
  }

  /**
   * Binds the link to the from item.
   */
  public boolean bindToFromItem()
  {
    if (_targetFromItem.getJoinExpr() == null ||
        _targetFromItem.getJoinExpr().equals(this)) {
      _targetFromItem.setJoinExpr(this);
      return true;
    }
    else if (_sourceFromItem.getJoinExpr() == null) {
      _sourceFromItem.setJoinExpr(new OneToManyJoinExpr(_linkColumns,
                                                        _sourceFromItem,
                                                        _targetFromItem));

      return true;
    }
    else
      return false;
  }

  /**
   * Returns the target join clause.
   */
  public FromItem getJoinTarget()
  {
    return _targetFromItem;
  }

  /**
   * Returns the target join clause.
   */
  public FromItem getJoinParent()
  {
    return _sourceFromItem;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    return from == _targetFromItem || from == _sourceFromItem;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  @Override
  public boolean exists(FromItem from)
  {
    return false;
  }

  /**
   * Returns the id expr with the joined expression.
   */
  public AmberExpr replace(KeyColumnExpr id)
  {
    PathExpr parent = (PathExpr) id.getParent();

    if (parent.getChildFromItem() != _targetFromItem)
      return id;

    ForeignColumn sourceColumn = _linkColumns.getSourceColumn(id.getColumn());

    if (sourceColumn == null)
      throw new IllegalStateException(id.getColumn().getName());

    return new ColumnExpr(_sourceFromItem.getIdExpr(), sourceColumn);
  }

  /**
   * Returns the id expr with the joined expression.
   */
  public AmberExpr replace(IdExpr id)
  {
    return id;
  }

  /**
   * Generates the where expression.
   */
  public void generateWhere(CharBuffer cb)
  {
    String sourceName = null;

    // jpa/10c9
    if (_sourceFromItem != null)
      sourceName = _sourceFromItem.getName();

    String targetName = _targetFromItem.getName();

    cb.append(_linkColumns.generateWhere(sourceName,
                                         targetName));
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
  public void generateJoin(CharBuffer cb)
  {
    cb.append(_linkColumns.generateJoin(_sourceFromItem.getName(),
                                        _targetFromItem.getName()));
  }

  /**
   * Test for equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof ManyToOneJoinExpr))
      return false;

    ManyToOneJoinExpr joinExpr = (ManyToOneJoinExpr) o;

    return (_linkColumns.equals(joinExpr._linkColumns) &&
            _sourceFromItem.equals(joinExpr._sourceFromItem) &&
            _targetFromItem.equals(joinExpr._targetFromItem));
  }


  public String toString()
  {
    return ("ManyToOneJoinExpr[" + _linkColumns + "," +
            _sourceFromItem + "," + _targetFromItem + "]");
  }
}
