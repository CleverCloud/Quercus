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
public class CacheableEntityItem extends EntityItem {
  private AmberEntityHome _home;
  private Entity _cacheEntity;
  private long _expireTime;

  public CacheableEntityItem(AmberEntityHome home, Entity cacheEntity)
  {
    _home = home;
    _cacheEntity = cacheEntity;

    // jpa/0w00
    cacheEntity.__caucho_setConnection(null);

    if (cacheEntity.__caucho_isLoaded())
      _expireTime = Alarm.getCurrentTime() + _home.getCacheTimeout();
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
    long now = Alarm.getCurrentTime();

    if (_expireTime < now) {
      long timeout = _home.getCacheTimeout();
      
      boolean isExpired = _expireTime > 0 && timeout > 0;
      
      _expireTime = now + timeout;

      if (isExpired)
        _cacheEntity.__caucho_expire();
    }

    return _cacheEntity;
  }

  /**
   * Returns the cached entity.
   *
   * @return true if the cached value is valid.
   */
  @Override
  public Entity loadEntity(int loadGroup)
  {
    long now = Alarm.getCurrentTime();

    if (_expireTime < now) {
      _expireTime = now + _home.getCacheTimeout();
      _cacheEntity.__caucho_expire();
    }

    AmberConnection aConn = _home.getManager().getCacheConnection();

    try {
      // _cacheEntity.__caucho_setConnection(aConn);
      _cacheEntity.__caucho_retrieve_self(aConn);
    } finally {
      aConn.freeConnection();
    }

    return _cacheEntity;
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
    long now = Alarm.getCurrentTime();

    if (_expireTime < now) {
      _expireTime = now + _home.getCacheTimeout();
      _cacheEntity.__caucho_expire();
    }

    try {
      // jpa/0v33
      // Prepared statements are cached per context so
      // at this point, the cache item needs to use the
      // context connection.
      
      // _cacheEntity.__caucho_setConnection(aConn);
      _cacheEntity.__caucho_retrieve_self(aConn);
    } finally {
      // After loading the entity, all prepared statements
      // were properly cached into the context connection.
      // Now make the cached entity item independent
      // from any particular context.
      _cacheEntity.__caucho_setConnection(null);
    }

    return _cacheEntity;
  }

  /**
   * Creates a new entity instance.
   */
  @Override
  public Entity createEntity(AmberConnection aConn, Object key)
    throws SQLException
  {
    Entity cacheEntity = getEntity();
    AmberEntityHome home = getEntityHome();

    return cacheEntity.__caucho_home_new(home, key, aConn, this);
  }

  /**
   * Saves the item values into the cache.
   */
  public void save(Entity item)
  {
    /*
      long now = Alarm.getCurrentTime();

      synchronized (_cacheEntity) {
      _expireTime = now + _home.getCacheTimeout();

      _cacheEntity.__caucho_loadFromObject(item);
      }
    */
  }

  /**
   * Saves the item values into the cache.
   */
  public void savePart(Entity item)
  {
    /*
      synchronized (_cacheEntity) {
      _cacheEntity.__caucho_loadFromObject(item);
      }
    */
  }

  /**
   * Expire the value from the cache.
   */
  @Override
  public void expire()
  {
    _cacheEntity.__caucho_expire();
  }

  Class getInstanceClass()
  {
    return _cacheEntity.getClass();
  }

  public String toString()
  {
    return "CacheableEntityItem[" + _cacheEntity + "]";
  }
}
