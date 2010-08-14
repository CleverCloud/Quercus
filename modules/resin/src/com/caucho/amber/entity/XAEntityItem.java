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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.entity;

import com.caucho.amber.manager.AmberConnection;
import com.caucho.util.Alarm;

import java.sql.SQLException;
import java.util.Map;

/**
 * An entity item handles the living entities.
 */
public class XAEntityItem extends EntityItem {
  private AmberEntityHome _home;
  private Entity _entity;

  public XAEntityItem(AmberEntityHome home, Entity entity)
  {
    _home = home;
    _entity = entity;
  }

  /**
   * Returns the entity home.
   */
  @Override
  public AmberEntityHome getEntityHome()
  {
    return _home;
  }

  /**
   * Returns the cached entity.
   *
   * @return true if the cached value is valid.
   */
  public Entity getEntity()
  {
    return _entity;
  }

  /**
   * Returns the cached entity.
   *
   * @return true if the cached value is valid.
   */
  @Override
  public Entity loadEntity(int loadGroup)
  {
    AmberConnection aConn = _home.getManager().getCacheConnection();

    try {
      // _cacheEntity.__caucho_setConnection(aConn);
      _entity.__caucho_retrieve_self(aConn);
    } finally {
      aConn.freeConnection();
    }

    return _entity;
  }

  /**
   * Returns the cached entity.
   *
   * @return true if the cached value is valid.
   */
  @Override
  public Entity loadEntity(AmberConnection aConn,
                           int loadGroup)
  {
    _entity.__caucho_retrieve_self(aConn);

    return _entity;
  }

  /**
   * Creates a new entity instance.
   */
  @Override
  public Entity createEntity(AmberConnection aConn, Object key)
    throws SQLException
  {
    return _entity;
  }

  /**
   * Saves the item values into the cache.
   */
  public void save(Entity item)
  {
  }

  /**
   * Saves the item values into the cache.
   */
  public void savePart(Entity item)
  {
  }

  /**
   * Expire the value from the cache.
   */
  @Override
  public void expire()
  {
  }

  Class getInstanceClass()
  {
    return _entity.getClass();
  }

  public String toString()
  {
    return "XAEntityItem[" + _entity + "]";
  }
}
