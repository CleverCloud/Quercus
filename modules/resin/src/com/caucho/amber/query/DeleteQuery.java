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

package com.caucho.amber.query;

import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.TableInvalidateCompletion;
import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.jdbc.JdbcMetaData;
import com.caucho.util.CharBuffer;

import java.sql.SQLException;


/**
 * Represents an Amber delete query
 */
public class DeleteQuery extends AbstractQuery {
  private String _sql;

  DeleteQuery(String query, JdbcMetaData metaData)
  {
    super(query, metaData);
  }

  /**
   * Sets the where expression
   */
  void setWhere(AmberExpr expr)
  {
    _where = expr;
  }

  /**
   * Returns the id load sql
   */
  public String getSQL()
  {
    return _sql;
  }

  /**
   * Initialize
   */
  void init()
    throws QueryParseException
  {
    super.init();

    CharBuffer cb = CharBuffer.allocate();

    cb.append("DELETE FROM ");

    FromItem item = _fromList.get(0);

    // jpa/1332
    cb.append(item.getTable().getName());

    // jpa/1300, jpa/1331
    if ((getMetaData().supportsUpdateTableAlias() && (_fromList.size() > 1))
        || hasSubQuery()) {
      cb.append(" ");
      cb.append(item.getName());
    }

    if (_where != null) {
      cb.append(" WHERE ");
      _where.generateUpdateWhere(cb);
    }

    _sql = cb.close();
  }

  /**
   * Adds any completion info.
   */
  public void prepare(UserQuery userQuery, AmberConnection aConn)
    throws SQLException
  {
    aConn.flushNoChecks();
  }

  /**
   * Adds any completion info.
   */
  public void complete(UserQuery userQuery, AmberConnection aConn)
    throws SQLException
  {
    aConn.expire();

    FromItem item = _fromList.get(0);

    aConn.addCompletion(new TableInvalidateCompletion(item.getEntityType().getTable().getName()));
  }

  /**
   * Debug view.
   */
  public String toString()
  {
    return "DeleteQuery[" + getQueryString() + "]";
  }
}
