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
import com.caucho.util.CharBuffer;

/**
 * Represents a member query expression
 */
public class EmptyExpr extends AbstractAmberExpr {
  private AmberExpr _collectionExpr;

  private String _tableName;

  public EmptyExpr(AmberExpr collectionExpr)
  {
    _collectionExpr = collectionExpr;
  }

  /**
   * Returns true as a boolean expression.
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
    _tableName = parser.createTableName();

    return this;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    return (_collectionExpr.usesFrom(from, type));
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public AmberExpr replaceJoin(JoinExpr join)
  {
    _collectionExpr = _collectionExpr.replaceJoin(join);

    return this;
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

  //
  // private

  private void generateInternalWhere(CharBuffer cb,
                                     boolean select)
  {
    OneToManyExpr oneToMany = null;

    // ManyToMany is implemented as a
    // ManyToOne[embeddeding OneToMany]
    if (_collectionExpr instanceof ManyToOneExpr) {
      PathExpr expr = ((ManyToOneExpr) _collectionExpr).getParent();
      if (expr instanceof OneToManyExpr)
        oneToMany = (OneToManyExpr) expr;

    } else if (_collectionExpr instanceof OneToManyExpr) {
      oneToMany = (OneToManyExpr) _collectionExpr;
    }
    else
      throw new UnsupportedOperationException();

    LinkColumns join = oneToMany.getLinkColumns();

    AmberTable table = join.getSourceTable();
    cb.append("EXISTS(SELECT ");

    if (table.getIdColumns().size() > 0)
      cb.append(table.getIdColumns().get(0).getName());
    else
      cb.append('*');

    cb.append(" FROM " + table.getName() + " " + _tableName);
    cb.append(" WHERE ");

    String targetTable = oneToMany.getParent().getChildFromItem().getName();

    cb.append(join.generateJoin(_tableName, targetTable));

    cb.append(')');
  }
}
