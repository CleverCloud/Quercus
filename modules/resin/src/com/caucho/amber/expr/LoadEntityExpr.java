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

import com.caucho.amber.entity.Entity;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.field.AmberField;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.query.FromItem;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.AmberType;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An entity expression which should be loaded.
 */
public class LoadEntityExpr extends LoadExpr {
  private static final L10N L = new L10N(LoadEntityExpr.class);
  private static final Logger log
    = Logger.getLogger(LoadEntityExpr.class.getName());

  LoadEntityExpr(PathExpr expr)
  {
    super(expr);
  }

  /**
   * Returns the entity type.
   */
  public EntityType getEntityType()
  {
    return (EntityType) getType();
  }

  /**
   * Binds the expression as a select item.
   */
  public AmberExpr bindSelect(QueryParser parser)
  {
    _fromItem = _expr.bindSubPath(parser);

    if (_fromItem == null)
      throw new NullPointerException(_expr.getClass().getName() + " " + _expr);

    EntityType type = getEntityType();

    if (type.getSecondaryTables().size() > 0) {
      for (AmberField field : type.getFields()) {
        AmberTable subTable = field.getTable();

        if (subTable != null && subTable != type.getTable()) {
          LinkColumns link = subTable.getDependentIdLink();

          FromItem item = parser.createDependentFromItem(_fromItem, link);

          _subItems.add(item);
        }
      }
    }

    return this;
  }

  /**
   * Returns the object for the expr.
   */
  public Object getObject(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    return getEntityType().getLoadObject(aConn, rs, index);
  }

  /**
   * Returns the object for the expr.
   */
  public Object getCacheObject(AmberConnection aConn,
                               ResultSet rs,
                               int index)
    throws SQLException
  {
    return getCacheObject(aConn, rs, index, null);
  }

  /**
   * Returns the object for the expr.
   */
  public Object getCacheObject(AmberConnection aConn,
                               ResultSet rs,
                               int index,
                               Map<AmberExpr, String> joinFetchMap)
    throws SQLException
  {
    return findItem(aConn, rs, index, joinFetchMap);
  }

  /**
   * Returns the object for the expr.
   */
  public EntityItem findItem(AmberConnection aConn,
                             ResultSet rs,
                             int index)
    throws SQLException
  {
    return findItem(aConn, rs, index, null);
  }

  /**
   * Returns the object for the expr.
   */
  public EntityItem findItem(AmberConnection aConn,
                             ResultSet rs,
                             int index,
                             Map<AmberExpr, String> joinFetchMap)
    throws SQLException
  {
    // jpa/0h13, jpa/1160

    EntityType entityType = getEntityType();

    EntityItem item = entityType.getHome().findItem(aConn, rs, index);

    if (item == null)
      return null;

    int offset = entityType.getId().getKeyCount();

    Entity entity = item.getEntity();

    /* XXX: jpa/0s2c
    if (entityType.getDiscriminator() != null) {
      // jpa/0l47
      offset++;
    }
    */
    _index = entity.__caucho_load(aConn, rs, index + offset);

    item.setNumberOfLoadingColumns(_index);

    return item;
  }
}
