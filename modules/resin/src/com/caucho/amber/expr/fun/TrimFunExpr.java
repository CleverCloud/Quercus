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
 * TRIM function expression
 */
public class TrimFunExpr extends FunExpr {
  private static final L10N L = new L10N(TrimFunExpr.class);

  public enum TrimSemantics { LEADING, TRAILING, BOTH }

  private TrimSemantics _trimSemantics = TrimSemantics.BOTH;

  private AmberExpr _trimChar;

  /**
   * Creates a new expression
   */
  protected TrimFunExpr(QueryParser parser,
                        ArrayList<AmberExpr> args)
  {
    super(parser, "trim", args, false);
  }

  public static TrimFunExpr create(QueryParser parser,
                                   ArrayList<AmberExpr> args)
  {
    return new TrimFunExpr(parser, args);
  }

  /**
   * Sets the trim character.
   */
  public void setTrimChar(AmberExpr trimChar)
  {
    _trimChar = trimChar;
  }

  /**
   * Sets the trim semantics.
   */
  public void setTrimSemantics(TrimSemantics trimSemantics)
  {
    _trimSemantics = trimSemantics;
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
    // if (n != 1)
    //   throw new QueryParseException(L.l("expected 1 string argument for TRIM"));

    if (_parser.isDerbyDBMS()) {

      // Derby.

      switch (_trimSemantics) {

      case LEADING:
        cb.append("ltrim(");
        break;

      case TRAILING:
        cb.append("rtrim(");
        break;

      default:
        cb.append("ltrim(rtrim(");
      }

      if (_trimChar != null) {
        if (select)
          _trimChar.generateWhere(cb);
        else
          _trimChar.generateUpdateWhere(cb);

        cb.append(" from ");
      }

      if (select)
        args.get(0).generateWhere(cb);
      else
        args.get(0).generateUpdateWhere(cb);

      cb.append(')');

      if (_trimSemantics == TrimSemantics.BOTH)
        cb.append(')');

      return;
    }

    cb.append("trim(");

    switch (_trimSemantics) {

    case LEADING:
      cb.append("leading ");
      break;

    case TRAILING:
      cb.append("trailing ");
      break;

    default:
      cb.append("both ");
    }

    if (_trimChar != null) {
      if (select)
        _trimChar.generateWhere(cb);
      else
        _trimChar.generateUpdateWhere(cb);
    }

    cb.append(" from ");

    if (select)
      args.get(0).generateWhere(cb);
    else
      args.get(0).generateUpdateWhere(cb);

    cb.append(")");
  }
}
