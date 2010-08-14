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

package com.caucho.amber;

import com.caucho.amber.entity.AmberCompletion;
import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.entity.EntityKey;
import com.caucho.amber.gen.AmberEnhancer;
import com.caucho.amber.gen.AmberGenerator;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.query.QueryCacheKey;
import com.caucho.amber.query.ResultSetCacheChunk;
import com.caucho.amber.type.EntityType;
import com.caucho.bytecode.JClassLoader;
import com.caucho.config.ConfigException;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.enhancer.EnhancerManager;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main interface between Resin and the connector.  It's the
 * top-level SPI class for creating the SPI ManagedConnections.
 *
 * The resource configuration in Resin's web.xml will use bean-style
 * configuration to configure the ManagecConnectionFactory.
 */
public class EnvAmberManager
{
  private static final Logger log
    = Logger.getLogger(AmberPersistenceUnit.class.getName());
  private static final L10N L = new L10N(AmberPersistenceUnit.class);

  private static EnvironmentLocal<EnvAmberManager> _localManager
    = new EnvironmentLocal<EnvAmberManager>();

  private ClassLoader _parentLoader;

  private ArrayList<AmberPersistenceUnit> _managerList
    = new ArrayList<AmberPersistenceUnit>();

  private AmberEnhancer _enhancer;

  private long _tableCacheTimeout = 250;

  private JClassLoader _jClassLoader;

  private HashMap<String,AmberEntityHome> _entityHomeMap
    = new HashMap<String,AmberEntityHome>();

  private LruCache<QueryCacheKey,SoftReference<ResultSetCacheChunk>> _queryCache
    = new LruCache<QueryCacheKey,SoftReference<ResultSetCacheChunk>>(1024);

  private LruCache<EntityKey,SoftReference<EntityItem>> _entityCache
    = new LruCache<EntityKey,SoftReference<EntityItem>>(32 * 1024);

  private EntityKey _entityKey = new EntityKey();

  private long _xid;

  private AmberGenerator _generator;

  private volatile boolean _isInit;

  private EnvAmberManager()
  {
    _parentLoader = Thread.currentThread().getContextClassLoader();
    _jClassLoader = EnhancerManager.create(_parentLoader).getJavaClassLoader();

    try {
      if (_parentLoader instanceof DynamicClassLoader)
        ((DynamicClassLoader) _parentLoader).make();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // _enhancer = new AmberEnhancer(this);
    EnhancerManager.create().addClassEnhancer(_enhancer);
  }

  public static EnvAmberManager createLocal()
  {
    EnvAmberManager manager = _localManager.get();

    if (manager == null) {
      manager = new EnvAmberManager();
      _localManager.set(manager);
    }

    return manager;
  }

  /**
   * Adds an amber manager.
   */
  public void addAmberManager(AmberPersistenceUnit manager)
  {
    _managerList.add(manager);
  }

  /**
   * Set the default table cache time.
   */
  public void setTableCacheTimeout(long timeout)
  {
    _tableCacheTimeout = timeout;
  }

  /**
   * Get the default table cache time.
   */
  public long getTableCacheTimeout()
  {
    return _tableCacheTimeout;
  }

  /**
   * Returns a new xid.
   */
  public long getXid()
  {
    synchronized (this) {
      return _xid++;
    }
  }

  /**
   * Returns the enhanced loader.
   */
  public ClassLoader getEnhancedLoader()
  {
    return _parentLoader;
  }

  /**
   * Returns the enhanced loader.
   */
  public JClassLoader getJClassLoader()
  {
    return _jClassLoader;
  }

  /**
   * Adds the entity home.
   */
  public void addEntityHome(String name, AmberEntityHome home)
  {
    _entityHomeMap.put(name, home);
  }

  /**
   * Returns the entity home.
   */
  public AmberEntityHome getEntityHome(String name)
  {
    if (! _isInit) {
      /* XXX:
         try {
         initEntityHomes();
         } catch (RuntimeException e) {
         throw e;
         } catch (Exception e) {
         throw new AmberRuntimeException(e);
         }
      */
    }

    return _entityHomeMap.get(name);
  }

  /**
   * Returns a matching entity.
   */
  public EntityType getEntity(String className)
  {
    AmberEntityHome home = _entityHomeMap.get(className);

    if (home != null)
      return home.getEntityType();
    else
      return null;
  }

  /**
   * Returns a matching entity.
   */
  public EntityType getEntityByInstanceClass(String className)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the generator.
   */
  public void setGenerator(AmberGenerator generator)
  {
    _generator = generator;
  }

  /**
   * Sets the generator.
   */
  public AmberGenerator getGenerator()
  {
    if (_generator != null)
      return _generator;
    else if (_enhancer != null)
      return _enhancer;
    else {
      // _generator = new AmberGeneratorImpl(this);

      return _generator;
    }
  }

  /**
   * Initialize the resource.
   */
  public void initLoaders()
    throws ConfigException, IOException
  {
  }

  /**
   * Returns the cache connection.
   */
  public AmberConnection createAmberConnection(boolean isExtended)
  {
    // XXX: needs to be an EnvAmberConnection
    return _managerList.get(0).createAmberConnection(isExtended);
  }

  /**
   * Initialize the home interfaces.
   */
  public void initEntityHomes()
    throws Exception
  {
    for (AmberPersistenceUnit manager : _managerList)
      manager.initEntityHomes();
  }

  /**
   * Initialize the resource.
   */
  public void init()
    throws ConfigException, IOException
  {
    initLoaders();
  }

  /**
   * Returns an EntityHome.
   */
  public AmberEntityHome getHome(Class cl)
  {
    return getEntityHome(cl.getName());
  }

  /**
   * Returns the query result.
   */
  public ResultSetCacheChunk getQueryChunk(QueryCacheKey key)
  {
    SoftReference<ResultSetCacheChunk> ref = _queryCache.get(key);

    if (ref == null)
      return null;
    else {
      ResultSetCacheChunk chunk = ref.get();

      if (chunk != null && chunk.isValid())
        return chunk;
      else
        return null;
    }
  }

  /**
   * Sets the query result.
   */
  public void putQueryChunk(QueryCacheKey key, ResultSetCacheChunk chunk)
  {
    _queryCache.put(key, new SoftReference<ResultSetCacheChunk>(chunk));
  }

  /**
   * Returns the entity item.
   */
  public EntityItem getEntityItem(String homeName, Object key)
    throws AmberException
  {
    AmberEntityHome home = getEntityHome(homeName);

    // return home.findEntityItem(getCacheConnection(), key, false);

    return null; // XXX:
  }

  /**
   * Returns the query result.
   */
  public EntityItem getEntity(EntityType rootType, Object key)
  {
    SoftReference<EntityItem> ref;

    synchronized (_entityKey) {
      _entityKey.init(rootType.getInstanceClass(), key);
      ref = _entityCache.get(_entityKey);
    }

    if (ref != null)
      return ref.get();
    else
      return null;
  }

  /**
   * Sets the entity result.
   */
  public EntityItem putEntity(EntityType rootType,
                              Object key,
                              EntityItem entity)
  {
    SoftReference<EntityItem> ref = new SoftReference<EntityItem>(entity);
    EntityKey entityKey = new EntityKey(rootType.getInstanceClass(), key);

    ref = _entityCache.putIfNew(entityKey, ref);

    return ref.get();
  }

  /**
   * Remove the entity result.
   */
  public EntityItem removeEntity(EntityType rootType, Object key)
  {
    SoftReference<EntityItem> ref;

    synchronized (_entityKey) {
      _entityKey.init(rootType.getInstanceClass(), key);
      ref = _entityCache.remove(_entityKey);
    }

    if (ref != null)
      return ref.get();
    else
      return null;
  }

  /**
   * Completions affecting the cache.
   */
  public void complete(ArrayList<AmberCompletion> completions)
  {
    int size = completions.size();
    if (size == 0)
      return;

    synchronized (_entityCache) {
      Iterator<LruCache.Entry<EntityKey,SoftReference<EntityItem>>> iter;

      iter = _entityCache.iterator();
      while (iter.hasNext()) {
        LruCache.Entry<EntityKey,SoftReference<EntityItem>> entry;
        entry = iter.next();

        EntityKey key = entry.getKey();
        SoftReference<EntityItem> valueRef = entry.getValue();
        EntityItem value = valueRef.get();

        if (value == null)
          continue;

        EntityType entityRoot = value.getEntityHome().getEntityType();
        Object entityKey = key.getKey();

        for (int i = 0; i < size; i++) {
          if (completions.get(i).complete(entityRoot, entityKey, value)) {
            // XXX: delete
          }
        }
      }
    }

    synchronized (_queryCache) {
      Iterator<SoftReference<ResultSetCacheChunk>> iter;

      iter = _queryCache.values();
      while (iter.hasNext()) {
        SoftReference<ResultSetCacheChunk> ref = iter.next();

        ResultSetCacheChunk chunk = ref.get();

        if (chunk != null) {
          for (int i = 0; i < size; i++) {
            if (completions.get(i).complete(chunk)) {
              // XXX: delete
            }
          }
        }
      }
    }
  }

  /**
   * destroys the manager.
   */
  public void destroy()
  {
    _queryCache = null;
    _entityCache = null;
    _parentLoader = null;
  }

  public String toString()
  {
    return "EnvAmberManager[]";
  }
}
