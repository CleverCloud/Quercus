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
import com.caucho.amber.type.BeanType;

/**
 * Represents an amber mapping query expression
 */
public interface PathExpr extends AmberExpr {
  /**
   * Returns the target type.
   */
  public BeanType getTargetType();

  /**
   * Creates the expr from the path.
   */
  public AmberExpr createField(QueryParser parser, String field);

  /**
   * Creates an array reference.
   */
  public AmberExpr createArray(AmberExpr field);

  /**
   * Creates an id expression.
   */
  public IdExpr createId(FromItem from);

  /**
   * Creates a load expression.
   */
  public LoadExpr createLoad();

  /**
   * Binds the expression as a select item.
   */
  public PathExpr bindSelect(QueryParser parser, String tableName);

  /**
   * Binds the expression as a select item.
   */
  public FromItem bindSubPath(QueryParser parser);

  /**
   * Returns the from item
   */
  public FromItem getChildFromItem();
}
