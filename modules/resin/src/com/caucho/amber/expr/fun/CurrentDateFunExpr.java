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

import com.caucho.amber.query.QueryParser;
import com.caucho.amber.type.SqlDateType;
import com.caucho.amber.type.AmberType;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;


/**
 * CURRENT_DATE function expression
 */
public class CurrentDateFunExpr extends DateTimeFunExpr {
  private static final L10N L = new L10N(CurrentDateFunExpr.class);

  /**
   * Creates a new expression
   */
  protected CurrentDateFunExpr(QueryParser parser)
  {
    super(parser, "current_date", null, false);
  }

  public static FunExpr create(QueryParser parser)
  {
    return new CurrentDateFunExpr(parser);
  }

  /**
   * Generates the where expression.
   */
  public void generateWhere(CharBuffer cb)
  {
    cb.append("current_date");
  }

  /**
   * Generates the (update) where expression.
   */
  public void generateUpdateWhere(CharBuffer cb)
  {
    generateWhere(cb);
  }

  /**
   * Returns the expr type.
   */
  public AmberType getType()
  {
    return SqlDateType.create();
  }
}
