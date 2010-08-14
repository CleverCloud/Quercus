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

import com.caucho.amber.expr.*;
import com.caucho.amber.query.*;
import com.caucho.amber.table.*;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.util.ArrayList;


/**
 * SIZE function expression
 */
public class SizeFunExpr extends FunExpr {
  private static final L10N L = new L10N(SizeFunExpr.class);

  /**
   * Creates a new expression
   */
  protected SizeFunExpr(QueryParser parser,
                        ArrayList<AmberExpr> args)
  {
    super(parser, "size", args, false);
  }

  public static FunExpr create(QueryParser parser,
                               ArrayList<AmberExpr> args)
  {
    return new SizeFunExpr(parser, args);
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
   * Generates the where clause.
   */
  void generateInternalWhere(CharBuffer cb,
                             boolean select)
  {
    cb.append("count(");

    AmberExpr arg = _args.get(0);

    if (arg instanceof OneToManyExpr) {

      // jpa/119m

      OneToManyExpr oneToMany = (OneToManyExpr) arg;

      FromItem fromItem = oneToMany.getChildFromItem();

      cb.append(fromItem.getName());
      cb.append('.');

      LinkColumns linkColumns = oneToMany.getLinkColumns();
      ForeignColumn fkColumn = linkColumns.getColumns().get(0);

      cb.append(fkColumn.getName());
    }
    else {
      if (select)
        arg.generateWhere(cb);
      else
        arg.generateUpdateWhere(cb);
    }

    cb.append(')');
  }
}
