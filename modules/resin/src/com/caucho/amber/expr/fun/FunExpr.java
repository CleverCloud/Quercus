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

package com.caucho.amber.expr.fun;

import com.caucho.amber.expr.AbstractAmberExpr;
import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.expr.IdExpr;
import com.caucho.amber.expr.KeyColumnExpr;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.query.FromItem;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.type.AmberType;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


/**
 * Function expression
 */
public class FunExpr extends AbstractAmberExpr {
  private static final L10N L = new L10N(FunExpr.class);

  QueryParser _parser;

  String _id;
  ArrayList<AmberExpr> _args;
  boolean _distinct;

  /**
   * Creates a new function expression
   */
  protected FunExpr(QueryParser parser,
                    String id,
                    ArrayList<AmberExpr> args,
                    boolean distinct)
  {
    _parser = parser;
    _id = id;
    _args = args;
    _distinct = distinct;
  }

  public static FunExpr create(QueryParser parser,
                               String id,
                               ArrayList<AmberExpr> args,
                               boolean distinct)
  {
    return new FunExpr(parser, id, args, distinct);
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    for (int i = 0; i < _args.size(); i++) {
      AmberExpr arg = _args.get(i);

      arg = arg.bindSelect(parser);

      _args.set(i, arg);
    }

    return this;
  }

  /**
   * Returns the expr type.
   */
  public AmberType getType()
  {
    if (getArgs().size() == 0)
      return super.getType();

    // jpa/141j
    return getArgs().get(0).getType();
  }

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    for (int i = 0; i < _args.size(); i++) {
      AmberExpr arg = _args.get(i);

      if (arg instanceof IdExpr) {
        IdExpr id = (IdExpr) arg;

        // jpa/0i18
        if (id.getFromItem() == from)
          return true;
      }

      if (arg instanceof KeyColumnExpr) {
        KeyColumnExpr key = (KeyColumnExpr) arg;

        // jpa/1123
        if (key.usesFrom(from, IS_INNER_JOIN, false))
          return true;
      }

      if (arg.usesFrom(from, type))
        return true;
    }

    return false;
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

  /**
   * Returns the object for the expr.
   */
  public Object getObject(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    if (_id.equalsIgnoreCase("count"))
      return rs.getLong(index);

    if (_id.equalsIgnoreCase("avg"))
      return rs.getDouble(index);

    // jpa/1199
    if (_id.equalsIgnoreCase("size"))
      return rs.getInt(index);

    return super.getObject(aConn, rs, index);
  }

  public String toString()
  {
    String str = _id + "(";

    if (_distinct)
      str += "distinct ";

    for (int i = 0; i < _args.size(); i++) {
      if (i != 0)
        str += ',';

      str += _args.get(i);
    }

    return str + ")";
  }

  // protected

  /**
   * Returns the args.
   */
  public ArrayList<AmberExpr> getArgs()
  {
    return _args;
  }

  /**
   * Generates the where clause.
   */
  private void generateInternalWhere(CharBuffer cb,
                                     boolean select)
  {
    cb.append(_id);
    cb.append('(');

    if (_distinct)
      cb.append("distinct ");

    for (int i = 0; i < _args.size(); i++) {
      if (i != 0)
        cb.append(',');

      if (select)
        _args.get(i).generateWhere(cb);
      else
        _args.get(i).generateUpdateWhere(cb);
    }

    cb.append(')');
  }
}
