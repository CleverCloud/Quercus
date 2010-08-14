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
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.AmberTable;
import com.caucho.util.CharBuffer;

import java.util.ArrayList;

/**
 * Represents a member query expression
 */
public class MemberExpr extends AbstractAmberExpr {
  private boolean _isNot;

  // PathExpr or ArgExpr (jpa/10c8)
  private AmberExpr _itemExpr;

  private AmberExpr _collectionExpr;

  private MemberExpr(AmberExpr itemExpr,
                     AmberExpr collectionExpr, boolean isNot)
  {
    _itemExpr = itemExpr;
    _collectionExpr = collectionExpr;
    _isNot = isNot;
  }

  public static AmberExpr create(QueryParser parser,
                                 AmberExpr itemExpr,
                                 AmberExpr collectionExpr,
                                 boolean isNot)
  {
    if (collectionExpr instanceof IdExpr)
      collectionExpr = ((CollectionIdExpr) collectionExpr).getPath();

    if (itemExpr instanceof ArgExpr) {
      // jpa/10c8, jpa/10c9
    }
    else if (itemExpr instanceof ManyToOneExpr) {
      // jpa/10ca
    }
    else if (collectionExpr instanceof OneToManyExpr) {
      // ejb/06u0, jpa/10c4, jpa/10c5
      OneToManyExpr oneToMany = (OneToManyExpr) collectionExpr;
      PathExpr parent = oneToMany.getParent();

      FromItem childFromItem = ((PathExpr) itemExpr).getChildFromItem();

      AmberExpr expr;

      expr = new ManyToOneJoinExpr(oneToMany.getLinkColumns(),
                                   childFromItem,
                                   parent.getChildFromItem());

      if (isNot)
        return new UnaryExpr(QueryParser.NOT, expr);
      else
        return expr;
    }

    return new MemberExpr(itemExpr, collectionExpr, isNot);
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    return this;
  }

  /**
   * Returns true for a boolean expression.
   */
  public boolean isBoolean()
  {
    return true;
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    if (! (_itemExpr instanceof PathExpr))
      return false;

    return (_collectionExpr.usesFrom(from, type) ||
            ((PathExpr) _itemExpr).usesFrom(from, type));
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public AmberExpr replaceJoin(JoinExpr join)
  {
    if (_itemExpr instanceof PathExpr) {
      _collectionExpr = _collectionExpr.replaceJoin(join);
      _itemExpr = (PathExpr) _itemExpr.replaceJoin(join);
    }

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

    if (_isNot)
      cb.append("NOT ");

    // jpa/10ca
    // XXX: needs to handle compound PK.
    ForeignColumn fk = (ForeignColumn) join.getColumns().get(0);
    cb.append(oneToMany.getParent().getChildFromItem().getName());
    cb.append('.');
    cb.append(fk.getTargetColumn().getName());

    // changed to IN for jpa/10ca cb.append("EXISTS (SELECT *");
    cb.append(" IN (SELECT "); // SELECT *");
    cb.append(fk.getName());
    AmberTable table = join.getSourceTable();
    cb.append(" FROM " + table.getName() + " caucho");
    cb.append(" WHERE ");

    String targetTable = oneToMany.getParent().getChildFromItem().getName();

    cb.append(join.generateJoin("caucho", targetTable));

    if (_itemExpr instanceof ArgExpr) {

      cb.append(" AND caucho.");

      if (_collectionExpr instanceof ManyToOneExpr) {
        join = ((ManyToOneExpr) _collectionExpr).getLinkColumns();

        String name = join.getColumns().get(0).getName();

        cb.append(name);
      }
      else {
        // XXX: needs to handle compound PK.
        ArrayList<AmberColumn> idColumns =
          join.getSourceTable().getIdColumns();

        cb.append(idColumns.get(0).getName());
      }

      cb.append(" = ?");
    }
    else if (_collectionExpr instanceof ManyToOneExpr) {
      join = ((ManyToOneExpr) _collectionExpr).getLinkColumns();

      String itemWhere;
      boolean isArg = false;

      String where;

      if (_itemExpr instanceof ManyToOneExpr) {
        LinkColumns manyToOneJoin = ((ManyToOneExpr) _itemExpr).getLinkColumns();

        itemWhere = ((ManyToOneExpr) _itemExpr).getParent().getChildFromItem().getName();

        where = join.generateJoin(manyToOneJoin, "caucho", itemWhere);
      }
      else {
        if (_itemExpr instanceof PathExpr) {
          itemWhere = ((PathExpr) _itemExpr).getChildFromItem().getName();
        }
        else {
          isArg = true;
          itemWhere = "?";
        }

        where = join.generateJoin("caucho", itemWhere, isArg);
      }

      cb.append(" AND " + where);
    }
    else if (_collectionExpr instanceof OneToManyExpr) {
      if (_itemExpr instanceof ManyToOneExpr) {

        join = ((ManyToOneExpr) _itemExpr).getLinkColumns();

        String itemWhere = ((ManyToOneExpr) _itemExpr).getParent().getChildFromItem().getName();

        String where = join.generateJoin(itemWhere, "caucho");

        cb.append(" AND " + where);
      }
      else {
        // XXX: needs to handle compound PK.
        ArrayList<AmberColumn> idColumns =
          join.getSourceTable().getIdColumns();

        String id = idColumns.get(0).getName();

        cb.append(" AND (caucho." + id + " = ");

        FromItem childFromItem = ((PathExpr) _itemExpr).getChildFromItem();

        if (childFromItem != null) {
          cb.append(childFromItem.getName() + ".");

          // XXX: needs to handle compound PK.
          idColumns = childFromItem.getTable().getIdColumns();

          cb.append(idColumns.get(0).getName() + ")");
        }
      }
    }

    cb.append(')');
  }
}
