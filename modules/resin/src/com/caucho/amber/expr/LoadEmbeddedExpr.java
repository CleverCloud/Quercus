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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.expr;

import com.caucho.amber.entity.Embeddable;
import com.caucho.amber.field.AmberField;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.query.FromItem;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.EmbeddableType;
import com.caucho.amber.type.AmberType;
import com.caucho.util.CharBuffer;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * An embedded expression which should be loaded.
 */
public class LoadEmbeddedExpr extends LoadExpr {
  LoadEmbeddedExpr(PathExpr expr)
  {
    super(expr);
  }

  /**
   * Returns the embeddable type.
   */
  public EmbeddableType getEmbeddableType()
  {
    return (EmbeddableType) getType();
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    _fromItem = _expr.bindSubPath(parser);

    if (_fromItem == null)
      throw new NullPointerException(_expr.getClass().getName() + " " + _expr);

    return this;
  }

  /**
   * Returns the object for the expr.
   */
  public Object getObject(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    Embeddable embeddable = getEmbeddableType().createObject();

    embeddable.__caucho_load(aConn, rs, index);

    return embeddable;
  }

  /**
   * Returns the object for the expr.
   */
  public Object getCacheObject(AmberConnection aConn,
                               ResultSet rs,
                               int index)
    throws SQLException
  {
    // XXX: needs to handle embeddable type caching.

    return getObject(aConn, rs, index);
  }
}
