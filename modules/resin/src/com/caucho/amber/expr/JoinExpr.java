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

/**
 * Links two tables.
 */
abstract public class JoinExpr extends AbstractAmberExpr {
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
   * Binds the expression as a select item.
   */
  public boolean bindToFromItem()
  {
    return false;
  }

  /**
   * Returns the target join clause.
   */
  public FromItem getJoinTarget()
  {
    return null;
  }

  /**
   * Returns the parent join clause.
   */
  public FromItem getJoinParent()
  {
    return null;
  }

  /**
   * Returns true if the given from item is the parent link.
   */
  public boolean isDependent(FromItem parent,
                             LinkColumns link)
  {
    return false;
  }

  /**
   * Returns the where clause once the parent is removed
   */
  public AmberExpr getWhere()
  {
    return null;
  }

  /**
   * Returns the id expr with the joined expression.
   */
  public AmberExpr replace(KeyColumnExpr id)
  {
    return id;
  }

  /**
   * Returns the id expr with the joined expression.
   */
  public AmberExpr replace(IdExpr id)
  {
    return id;
  }
}
