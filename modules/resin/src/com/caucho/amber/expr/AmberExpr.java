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

import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.query.FromItem;
import com.caucho.amber.query.QueryParseException;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.type.AmberType;
import com.caucho.util.CharBuffer;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Represents an Amber query expression
 */
public interface AmberExpr {
  public final static int USES_DATA = 0;
  public final static int IS_INNER_JOIN = 1;

  /**
   * Returns true for a boolean expression.
   */
  boolean isBoolean();

  /**
   * Returns the expr type.
   */
  AmberType getType();

  /**
   * Binds the expression as a select item.
   */
  AmberExpr bindSelect(QueryParser parser);

  /**
   * Returns true if the expression uses the from item.
   */
  boolean usesFrom(FromItem from, int type);

  /**
   * Returns true if the expression uses the from item.
   */
  boolean usesFrom(FromItem from, int type, boolean isNot);

  /**
   * Returns true if the item is required to exist by the expression, e.g.
   * foo.id='1' forces foo to exist.
   */
  boolean exists(FromItem from);

  /**
   * Returns true if the expression item is required to exist, e.g.
   * constants and arguments.
   *
   * (Can also apply to arguments required to exist after a particular
   * from item, i.e. from items already proved to exist.)
   */
  boolean exists();

  /**
   * Returns true if the expression uses the from item.
   */
  AmberExpr replaceJoin(JoinExpr join);

  /**
   * Generates the where expression.
   */
  void generateWhere(CharBuffer cb);

  /**
   * Generates the (update) where expression.
   */
  void generateUpdateWhere(CharBuffer cb);

  /**
   * Generates the having expression.
   */
  void generateHaving(CharBuffer cb);

  /**
   * Generates the where expression.
   */
  void generateJoin(CharBuffer cb);

  /**
   * Generates the select expression.
   */
  void generateSelect(CharBuffer cb);

  /**
   * Converts to a boolean expression.
   */
  public AmberExpr createBoolean()
    throws QueryParseException;

  /**
   * Returns the object for the expr.
   */
  public Object getObject(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException;

  /**
   * Returns the cache object for the expr.
   */
  public Object getCacheObject(AmberConnection aConn,
                               ResultSet rs, int index)
    throws SQLException;

  /**
   * Returns true if this expr has any relationship.
   */
  public boolean hasRelationship();

  /**
   * Returns the object for the expr.
   */
  public EntityItem findItem(AmberConnection aConn,
                             ResultSet rs, int index)
    throws SQLException;

  /**
   * Binds the argument type based on another expr.
   */
  public void setInternalArgType(AmberExpr otherExpr);
}
