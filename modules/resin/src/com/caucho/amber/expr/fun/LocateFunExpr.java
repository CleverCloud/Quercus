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

import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.query.QueryParser;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


/**
 * LOCATE function expression
 */
public class LocateFunExpr extends FunExpr {
  private static final L10N L = new L10N(LocateFunExpr.class);

  /**
   * Creates a new expression
   */
  protected LocateFunExpr(QueryParser parser,
                          ArrayList<AmberExpr> args)
  {
    super(parser, "locate", args, false);
  }

  public static FunExpr create(QueryParser parser,
                               ArrayList<AmberExpr> args)
  {
    return new LocateFunExpr(parser, args);
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
   * Returns the object for the expr.
   */
  public Object getObject(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    return rs.getInt(index);
  }

  //
  // private/protected

  void generateInternalWhere(CharBuffer cb,
                             boolean select)
  {
    // Translate to => POSITION('word' in SUBSTRING(data,i,LENGTH(data)))+(i-1)

    ArrayList<AmberExpr> args = getArgs();

    int n = args.size();

    // XXX:
    // if (n < 2)
    //   throw _parser.error(L.l("expected at least 2 string arguments for LOCATE"));
    //
    // if (n > 3)
    //   throw _parser.error(L.l("expected at most 3 arguments for LOCATE"));

    if (! _parser.isPostgresDBMS()) {

      // Derby (jpa/119k), MySql (ejb/0a40, jpa/119p)

      cb.append("locate(");

      if (select)
        args.get(0).generateWhere(cb);
      else
        args.get(0).generateUpdateWhere(cb);

      cb.append(',');

      if (select)
        args.get(1).generateWhere(cb);
      else
        args.get(1).generateUpdateWhere(cb);

      if (n > 2) {
        cb.append(',');

        AmberExpr expr = args.get(2);

         if (select)
          expr.generateWhere(cb);
        else
          expr.generateUpdateWhere(cb);
      }

      cb.append(')');

      return;
    }

    // Postgres: jpa/1190, jpa/119b, jpa/119c

    cb.append("position(");

    if (select)
      args.get(0).generateWhere(cb);
    else
      args.get(0).generateUpdateWhere(cb);

    cb.append(" in substring(");

    if (select)
      args.get(1).generateWhere(cb);
    else
      args.get(1).generateUpdateWhere(cb);

    cb.append(" from ");

    int fromIndex = 1;

    AmberExpr expr = null;

    if (n == 2) {
      cb.append('1');
    }
    else {
      expr = args.get(2);

      try {
        fromIndex = Integer.parseInt(expr.toString());
      } catch (Exception ex) {
        // XXX: this validation should be moved to QueryParser
        // throw new QueryParseException(L.l("expected an integer for LOCATE 3rd argument"));
      }

      if (select)
        expr.generateWhere(cb);
      else
        expr.generateUpdateWhere(cb);
    }

    cb.append("))");
  }
}
