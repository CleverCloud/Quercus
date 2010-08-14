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

import com.caucho.amber.AmberException;
import com.caucho.amber.AmberRuntimeException;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.query.CacheUpdate;
import com.caucho.amber.type.*;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.lang.Comparable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the set of persistent beans.
 */
public class AmberEntityHome implements Comparable {
  private static final L10N L = new L10N(AmberEntityHome.class);
  private static final Logger log
    = Logger.getLogger(AmberEntityHome.class.getName());

  private AmberPersistenceUnit _manager;
  private EntityType _entityType;

  private EntityFactory _entityFactory = new EntityFactory();

  private Entity _homeBean;

  private ArrayList<SoftReference<CacheUpdate>> _cacheUpdates
    = new ArrayList<SoftReference<CacheUpdate>>();

  private volatile boolean _isInit;

  private RuntimeException _configException;

  private Method _cauchoGetBeanMethod;

  public AmberEntityHome(AmberPersistenceUnit manager, EntityType type)
  {
    _manager = manager;
    _entityType = type;
  }

  /**
   * Returns the getBean method to instantiate the Amber object.
   */
  public Method getCauchoGetBeanMethod()
  {
    return _cauchoGetBeanMethod;
  }

  /**
   * Returns the manager.
   */
  public AmberPersistenceUnit getManager()
  {
    return _manager;
  }

  /**
   * Returns the entity type
   */
  public EntityType getEntityType()
  {
    return _entityType;
  }

  /**
   * Returns the entity type
   */
  public EntityType getRootType()
  {
    return (EntityType) _entityType.getRootType();
  }

  /**
   * Returns the java class.
   */
  public Class getJavaClass()
  {
    return _entityType.getInstanceClass();
  }

  /**
   * Returns the entity factory.
   */
  public EntityFactory getEntityFactory()
  {
    return _entityFactory;
  }

  /**
   * Sets the entity factory.
   */
  public void setEntityFactory(EntityFactory factory)
  {
    _entityFactory = factory;
  }

  /**
   * Returns the cache timeout.
   */
  public long getCacheTimeout()
  {
    return _entityType.getCacheTimeout();
  }

  /**
   * Returns the instance class.
   */
  public Class getInstanceClass()
  {
    return _entityType.getInstanceClass();
  }

  /**
   * Link the classes.
   */
  void link()
    throws ConfigException
  {
    // _entityClass.link(_manager);
  }

  /**
   * Initialize the home.
   */
  public void init()
    throws ConfigException
  {
    synchronized (this) {
      if (_isInit)
        return;
      _isInit = true;
    }

    _entityType.init();

    try {
      Class instanceClass = _entityType.getInstanceClass();

      if (! Modifier.isAbstract(instanceClass.getModifiers()))
        _homeBean = (Entity) instanceClass.newInstance();
    } catch (Exception e) {
      _entityType.setConfigException(e);

      _configException = ConfigException.create(e);
      throw _configException;
    }

    _entityType.start();
  }

  /**
   * Returns the entity from the key.
   */
  public Object getKeyFromEntity(Entity entity)
    throws AmberException
  {
    //    return _entityType.getId().getType().getValue(obj);
    return null;
  }

  /**
   * Converts a long key to the key.
   */
  public Object toObjectKey(long key)
  {
    return _entityType.getId().toObjectKey(key);
  }

  /**
   * Finds by the primary key.
   */
  public EntityItem findItem(AmberConnection aConn,
                             ResultSet rs, int index)
    throws SQLException
  {
    EntityItem item = _homeBean.__caucho_home_find(aConn, this, rs, index);

    return item;
  }

  /**
   * Finds by the primary key.
   */
  public Object loadFull(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    // jpa/0l43
    Entity entity;

    if (_homeBean == null)
      throw new NullPointerException("HOME:" + this);

    entity = aConn.getSubEntity(_homeBean.getClass(),
                                rs.getObject(index));

    if (entity != null) {
      if (entity.__caucho_getEntityState().isManaged())
        return entity;
    }

    EntityItem item = findItem(aConn, rs, index);

    if (item == null)
      return null;

    entity = null;

    Object value = aConn.getEntityLazy(item);

    if (aConn.isActiveTransaction()) {
      if (value instanceof Entity)
        entity = (Entity) value;
      else if (_cauchoGetBeanMethod != null) {
        try {
          entity = (Entity) _cauchoGetBeanMethod.invoke(value, new Object[0]);
          entity.__caucho_makePersistent(aConn, item);
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }

      if (entity == null)
        entity = aConn.getEntity(item);
    }
    else
      entity = item.getEntity();

    int keyLength = _entityType.getId().getKeyCount();

    entity.__caucho_load(aConn, rs, index + keyLength);

    return entity;
  }

  /**
   * Finds by the primary key.
   */
  public Object loadLazy(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    EntityItem item = findItem(aConn, rs, index);

    if (item == null)
      return null;

    return aConn.getEntity(item);
  }
  
  public EntityItem findEntityItem(AmberConnection aConn, Object key)
    throws AmberException
  {
    if (_homeBean == null && _configException != null)
      throw _configException;

    Entity entity;

    entity = (Entity) _homeBean.__caucho_home_find(aConn, this, key);

    // jpa/0l43 - with inheritance, an XA entity creates XAEntityItem
    // to avoid double loading
    if (entity == null)
      return null;
    else if (aConn.isActiveTransaction())
      return new XAEntityItem(this, entity);
    else
      return new CacheableEntityItem(this, entity);
  }

  /**
   * Loads an entity based on the primary key.
   *
   * @param key the primary key
   * @param aConn the Amber connection to associate with the loaded item
   * @param isLoad if true, try to load the bean
   */
  public EntityItem setEntityItem(Object key, EntityItem item)
    throws AmberException
  {
    if (key == null)
      throw new NullPointerException("primaryKey");

    try {
      item.getEntity().__caucho_setConnection(_manager.getCacheConnection());

      return _manager.putEntity(getRootType(), key, item);
    } catch (Exception e) {
      throw AmberException.create(e);
    }
  }

  /**
   * Loads an entity where the type is determined by a discriminator
   *
   * @param aConn the connection to associate with the entity
   * @param key the primary key
   * @param discriminator the object's discriminator
   */
  public EntityItem findDiscriminatorEntityItem(AmberConnection aConn,
                                                Object key,
                                                String discriminator)
    throws SQLException
  {
    EntityItem item = null;

    // jpa/0l20
    // XXX: ejb/0d01
    // if (aConn.shouldRetrieveFromCache())
    item = _manager.getEntity(getRootType(), key);

    if (item == null) {
      EntityType subEntity
        = (EntityType) _entityType.getSubClass(discriminator);

      Entity cacheEntity = subEntity.createBean();

      cacheEntity.__caucho_setPrimaryKey(key);
      cacheEntity.__caucho_makePersistent(_manager.getCacheConnection(),
                                          subEntity);

      item = new CacheableEntityItem(this, cacheEntity);

      // The cache entity is added after commit.
      if (! aConn.isActiveTransaction()) {
        item = _manager.putEntity(getRootType(), key, item);
      }
    }

    return item;
  }

  /**
   * Instantiates a new entity for this home.
   */
  public Entity newEntity(Object key)
  {
    return (Entity) _homeBean.__caucho_home_new(this, key, null, null);
  }

  /**
   * Loads an entity where the type is determined by a discriminator
   *
   * @param aConn the connection to associate with the entity
   * @param key the primary key
   * @param discriminator the object's discriminator
   */
  public Entity newDiscriminatorEntity(Object key,
                                       String discriminator)
  {
    if (discriminator == null || key == null)
      throw new AmberRuntimeException(L.l("{0} is not a valid inheritance key.",
                                          key));

    EntityType subType = (EntityType) _entityType.getSubClass(discriminator);

    return subType.getHome().newEntity(key);
  }

  /**
   * Finds by the primary key.
   */
  public Entity makePersistent(Entity entity,
                               AmberConnection aConn,
                               boolean isLazy)
    throws SQLException
  {
    entity.__caucho_makePersistent(aConn, _entityType);

    return entity;
  }

  /**
   * Saves based on the object.
   */
  public void save(AmberConnection aConn, Entity entity)
    throws SQLException
  {
    // Common to JPA and CMP.
    entity.__caucho_lazy_create(aConn, _entityType);

    if (! _manager.isJPA())
      entity.__caucho_create(aConn, _entityType);
  }

  /**
   * Deletes by the primary key.
   */
  public void delete(AmberConnection aConn, Object key)
    throws SQLException
  {
    _manager.removeEntity(getRootType(), key);
  }

  /**
   * Deletes by the primary key.
   */
  public void delete(AmberConnection aConn, long primaryKey)
    throws SQLException
  {
  }

  /**
   * Update for a modification.
   */
  public void update(Entity entity)
    throws SQLException
  {
  }

  /**
   * Adds a cache update.
   */
  public void addUpdate(CacheUpdate update)
  {
    _cacheUpdates.add(new SoftReference<CacheUpdate>(update));
  }

  public int compareTo(Object b)
  {
    AmberEntityHome home = (AmberEntityHome) b;

    return _entityType.getClassName().compareTo(home.getEntityType().getClassName());
  }

  public String toString()
  {
    return "AmberEntityHome[" + _entityType + "]";
  }
}
