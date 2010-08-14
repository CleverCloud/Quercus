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
import com.caucho.amber.table.AmberColumn;

/**
 * Bound identifier expression.
 */
public class KeyColumnExpr extends ColumnExpr {
  /**
   * Creates a new unbound id expression.
   */
  public KeyColumnExpr(PathExpr parent, AmberColumn column)
  {
    super(parent, column);
  }

  /**
   * Binds the expression as a select item.
   */
  /*
  public AmberExpr bindSelect(QueryParser parser)
  {
    return this;
  }
  */

  /**
   * Returns true if the expression uses the from item.
   */
  public boolean usesFrom(FromItem from, int type, boolean isNot)
  {
    if (_parent instanceof ManyToOneExpr) {
      // jpa/0h1c
      return false;
    }

    FromItem fromItem = _parent.getChildFromItem();

    // ejb/0j00 vs ejb/0h13 vs ejb/0t00
    if (type == IS_INNER_JOIN)
      return (from == fromItem);
    else
      return false;
  }

  /**
   * Replaces linked join to eliminate a table.
   */
  public AmberExpr replaceJoin(JoinExpr join)
  {
    return join.replace(this);
  }
}
