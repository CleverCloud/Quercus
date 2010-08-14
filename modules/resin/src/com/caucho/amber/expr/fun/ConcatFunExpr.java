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
import com.caucho.amber.query.QueryParser;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.util.ArrayList;


/**
 * CONCAT function expression
 */
public class ConcatFunExpr extends FunExpr {
  private static final L10N L = new L10N(ConcatFunExpr.class);

  /**
   * Creates a new expression
   */
  protected ConcatFunExpr(QueryParser parser,
                          ArrayList<AmberExpr> args)
  {
    super(parser, "concat", args, false);
  }

  public static FunExpr create(QueryParser parser,
                               ArrayList<AmberExpr> args)
  {
    return new ConcatFunExpr(parser, args);
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

  //
  // private/protected

  void generateInternalWhere(CharBuffer cb,
                             boolean select)
  {
    ArrayList<AmberExpr> args = getArgs();

    int n = args.size();

    // XXX: this validation should be moved to QueryParser
    // if (n != 2)
    //   throw new QueryParseException(L.l("expected 2 string arguments for CONCAT"));

    if (_parser.isDerbyDBMS()) {

      // Derby does not accept two params in (? || ?).
      // Translates to ((? || '') || ('' || ?)).

      // XXX: there seems to be an issue with VARCHAR(32672)
      // and a Derby driver patch should be released soon.

      cb.append("VARCHAR(CAST(");

      if (select)
        args.get(0).generateWhere(cb);
      else
        args.get(0).generateUpdateWhere(cb);

      cb.append("AS VARCHAR(2000)) || CAST(");

      if (select)
        args.get(1).generateWhere(cb);
      else
        args.get(1).generateUpdateWhere(cb);

      cb.append(" AS VARCHAR(2000)))");

      return;
    }
    else if (_parser.isPostgresDBMS()) {
      // jpa/1230
      generateInternalConcat(cb, true, true, null, select);
      return;
    }

    cb.append("concat");

    generateInternalConcat(cb, true, true, null, select);
  }

  private void generateInternalConcat(CharBuffer cb,
                                      boolean arg0,
                                      boolean arg1,
                                      String str,
                                      boolean select)
  {
    ArrayList<AmberExpr> args = getArgs();

    boolean usesConcatOperator
      = _parser.isDerbyDBMS() || _parser.isPostgresDBMS();

    cb.append('(');

    if (arg0) {
      if (select)
        args.get(0).generateWhere(cb);
      else
        args.get(0).generateUpdateWhere(cb);

      if (usesConcatOperator)
        cb.append(" || ");
      else
        cb.append(',');
    }

    if (arg1) {
      if (select)
        args.get(1).generateWhere(cb);
      else
        args.get(1).generateUpdateWhere(cb);

      if (arg0) {
        cb.append(')');
        return;
      }

      if (usesConcatOperator)
        cb.append(" || ");
      else
        cb.append(',');
    }

    cb.append('\'');

    cb.append(str);

    cb.append('\'');

    cb.append(')');
  }
}
