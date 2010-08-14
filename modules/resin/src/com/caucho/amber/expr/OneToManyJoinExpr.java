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
 * Joins two tables as "a.children".
 */
public class OneToManyJoinExpr extends JoinExpr {
  private LinkColumns _linkColumns;

  private FromItem _sourceFromItem;
  private FromItem _targetFromItem;

  /**
   * Creates the expr.
   */
  public OneToManyJoinExpr(LinkColumns linkColumns,
                           FromItem source,
                           FromItem target)
  {
    _linkColumns = linkColumns;

    _sourceFromItem = source;
    _targetFromItem = target;

    if (source == null || target == null)
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
    if (_sourceFromItem.getJoinExpr() == null ||
        _sourceFromItem.getJoinExpr().equals(this)) {
      _sourceFromItem.setJoinExpr(this);
      return true;
    }

    ManyToOneJoinExpr manyToOne = new ManyToOneJoinExpr(_linkColumns,
                                                        _sourceFromItem,
                                                        _targetFromItem);

    if (_targetFromItem.getJoinExpr() == null ||
        _targetFromItem.getJoinExpr().equals(manyToOne)) {
      _targetFromItem.setJoinExpr(manyToOne);

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
   * Returns the parent join clause, i.e. the first in the FROM order.
   * <pre>
   * FROM o, o.children
   * <pre>
   */
  public FromItem getJoinParent()
  {
    return _targetFromItem;
  }

  /**
   * Returns the id expr with the joined expression.
   */
  public AmberExpr replace(KeyColumnExpr id)
  {
    PathExpr parent = id.getParent();

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
   * Returns the where clause once the parent is removed
   */
  public AmberExpr getWhere()
  {
    /*
     * The code is partially correct, but the is null test isn't
     * needed in many cases, so should probably test the actual
     * expression
     AndExpr and = new AndExpr();
     IdExpr id = new IdExpr(_sourceFromItem);

     for (ForeignColumn column : _linkColumns.getColumns()) {
     and.add(new UnaryExpr(QueryParser.NOT_NULL,
     new ColumnExpr(id, column)));
     }

     return and.getSingle();
    */

    return null;
  }

  /**
   * Generates the where expression.
   */
  public void generateWhere(CharBuffer cb)
  {
    cb.append(_linkColumns.generateWhere(_sourceFromItem.getName(),
                                         _targetFromItem.getName()));
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
    if (! (o instanceof OneToManyJoinExpr))
      return false;

    OneToManyJoinExpr joinExpr = (OneToManyJoinExpr) o;

    return (_linkColumns.equals(joinExpr._linkColumns) &&
            _targetFromItem.equals(joinExpr._targetFromItem) &&
            _sourceFromItem.equals(joinExpr._sourceFromItem));
  }


  public String toString()
  {
    return ("OneToManyJoinExpr[" + _linkColumns + "," +
            _targetFromItem + "," + _sourceFromItem + "]");
  }
}
