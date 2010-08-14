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

package com.caucho.distcache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.cache.CacheEntry;
import javax.cache.CacheListener;
import javax.cache.CacheLoader;
import javax.cache.CacheStatistics;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.types.Period;
import com.caucho.loader.Environment;
import com.caucho.server.cluster.Server;
import com.caucho.server.distcache.CacheConfig;
import com.caucho.server.distcache.DistCacheEntry;
import com.caucho.server.distcache.DistributedCacheManager;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

/**
 * Implements the distributed cache
 */

abstract public class AbstractCache extends AbstractMap
  implements ObjectCache, ByteStreamCache, CacheStatistics
{
  private static final L10N L = new L10N(AbstractCache.class);

  private static final Logger log
    = Logger.getLogger(AbstractCache.class.getName());

  private CacheManager _localManager;

  private String _name = null;

  private String _guid;

  private Collection<CacheListener<?>> _listeners
    = new ConcurrentLinkedQueue<CacheListener<?>>();

  private CacheConfig _config = new CacheConfig();

  private LruCache<Object,DistCacheEntry> _entryCache;

  private boolean _isInit;
  private boolean _isClosed;

  private DistributedCacheManager _manager;

  private long _priorMisses = 0;
  private long _priorHits = 0;

  private String _scopeName = Scope.CLUSTER.toString();
  private String _persistenceOption = Persistence.TRIPLE.toString();

  public AbstractCache()
  {
    _localManager = CacheManager.createManager();
  }

  /**
   * Returns the name of the cache.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Assigns the name of the cache.
   * A name is mandatory and must be unique among open caches.
   */
  @Configurable
  public void setName(String name)
  {
    _name = name;
  }

  public void setGuid(String guid)
  {
    _guid = guid;
  }

  /**
   * Sets the CacheLoader that the Cache can then use to populate
   * cache misses from a reference store (database).
   */
  @Configurable
  public void setCacheLoader(CacheLoader loader)
  {
    _config.setCacheLoader(loader);
  }

  /**
   * Assign the serializer used on values.
   *
   * @Note: This setting should not be changed after
   * a cache is created.
   */
  @Configurable
  public void setSerializer(CacheSerializer serializer)
  {
    _config.setValueSerializer(serializer);
  }

  /**
   * Sets the backup mode.  If backups are enabled, copies of the
   * cache item will be sent to the owning triad server.
   * <p/>
   * Defaults to true.
   */
  @Configurable
  public void setBackup(boolean isBackup)
  {
    _config.setBackup(isBackup);
  }

  /**
   * Sets the global mode.  If global is enabled, copies of the
   * cache item will be sent to all clusters
   * <p/>
   * Defaults to false.
   */
  @Configurable
  public void setGlobal(boolean isGlobal)
  {
    _config.setGlobal(isGlobal);
  }

  /**
   * Sets the triplicate backup mode.  If triplicate backups is set,
   * all triad servers have a copy of the cache item.
   * <p/>
   * Defaults to true.
   */
  @Configurable
  public void setTriplicate(boolean isTriplicate)
  {
    _config.setTriplicate(isTriplicate);
  }

  /**
   * The maximum valid time for an item.  Items stored in the cache
   * for longer than the expire time are no longer valid and z null value
   * will be returned for a get.
   * <p/>
   * Default is infinite.
   */
  public long getExpireTimeout()
  {
    return _config.getExpireTimeout();
  }

  /**
   * The maximum valid time for a cached item before it expires.
   * Items stored in the cache for longer than the expire time are
   * no longer valid and will return null from a get.
   * <p/>
   * Default is infinite.
   */
  @Configurable
  public void setExpireTimeout(Period expireTimeout)
  {
    setExpireTimeoutMillis(expireTimeout.getPeriod());
  }

  /**
   * The maximum valid time for an item.  Items stored in the cache
   * for longer than the expire time are no longer valid and will
   * return null from a get.
   * <p/>
   * Default is infinite.
   */
  @Configurable
  public void setExpireTimeoutMillis(long expireTimeout)
  {
    _config.setExpireTimeout(expireTimeout);
  }

  /**
   * The maximum idle time for an item, which is typically used for
   * temporary data like sessions.  For example, session
   * data might be removed if idle over 30 minutes.
   * <p/>
   * Cached data would have infinite idle time because
   * it doesn't depend on how often it's accessed.
   * <p/>
   * Default is infinite.
   */
  @Configurable
  public void setIdleTimeout(Period period)
  {
    setIdleTimeoutMillis(period.getPeriod());
  }

  /**
   * The maximum idle time for an item, which is typically used for
   * temporary data like sessions.  For example, session
   * data might be removed if idle over 30 minutes.
   * <p/>
   * Cached data would have infinite idle time because
   * it doesn't depend on how often it's accessed.
   * <p/>
   * Default is infinite.
   */
  public long getIdleTimeout()
  {
    return _config.getIdleTimeout();
  }

  /**
   * Sets the idle timeout in milliseconds
   */
  @Configurable
  public void setIdleTimeoutMillis(long timeout)
  {
    _config.setIdleTimeout(timeout);
  }

  /**
   * Returns the idle check window, used to minimize traffic when
   * updating access times.
   */
  public long getIdleTimeoutWindow()
  {
    return _config.getIdleTimeoutWindow();
  }

  /**
   * Sets the idle timeout windows
   */
  public void setIdleTimeoutWindow(Period period)
  {
    _config.setIdleTimeoutWindow(period.getPeriod());
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  public long getLeaseTimeout()
  {
    return _config.getLeaseTimeout();
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  @Configurable
  public void setLeaseTimeout(Period period)
  {
    setLeaseTimeoutMillis(period.getPeriod());
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  @Configurable
  public void setLeaseTimeoutMillis(long timeout)
  {
    _config.setLeaseTimeout(timeout);
  }

  /**
   * The local read timeout is how long a local copy of
   * a cache item can be reused before checking with the master copy.
   * <p/>
   * A read-only item could be infinite (-1).  A slow changing item
   * like a list of bulletin-board comments could be 10s.  Even a relatively
   * quickly changing item can be 10ms or 100ms.
   * <p/>
   * The default is 10ms
   */
  public long getLocalReadTimeout()
  {
    return _config.getLocalReadTimeout();
  }

  /**
   * The local read timeout sets how long a local copy of
   * a cache item can be reused before checking with the master copy.
   * <p/>
   * A read-only item could be infinite (-1).  A slow changing item
   * like a list of bulletin-board comments could be 10s.  Even a relatively
   * quicky changing item can be 10ms or 100ms.
   * <p/>
   * The default is 10ms
   */
  @Configurable
  public void setLocalReadTimeout(Period period)
  {
    setLocalReadTimeoutMillis(period.getPeriod());
  }

  /**
   * The local read timeout sets how long a local copy of
   * a cache item can be reused before checking with the master copy.
   * <p/>
   * A read-only item could be infinite (-1).  A slow changing item
   * like a list of bulletin-board comments could be 10s.  Even a relatively
   * quicky changing item can be 10ms or 100ms.
   * <p/>
   * The default is 10ms
   */
  @Configurable
  public void setLocalReadTimeoutMillis(long period)
  {
    _config.setLocalReadTimeout(period);
  }

  public void setScopeMode(Scope scope)
  {
    _config.setScopeMode(scope);
  }

  /**
   * Sets the {@link Scope} of the cache.
   */
  @Configurable
  public void setScope(String scopeName)
  {
      _scopeName = scopeName;
  }

  /**
   * Returns the name of the Scope of the cache.
   * @return
   */
  public String getScope()
  {
    return _config.getScopeMode().toString().toLowerCase();
  }

  public void setPersistence(String persistenceOption)
  {
    _persistenceOption = persistenceOption;
  }

  /**
   * Initialize the cache.
   */
  @PostConstruct
  public void init()
  {
    synchronized (this) {
      if (_isInit)
        return;

      _isInit = true;

      _config.init();

      initServer();

      initName(_name);

      initScope(_scopeName);

      initPersistence(_persistenceOption);

      _config.setCacheKey(_manager.createSelfHashKey(_config.getGuid(),
                                                     _config.getKeySerializer()));

      if (_localManager.putIfAbsent(_guid, this) != null) {
        throw new ConfigException(L.l("'{0}' with full name '{1}' is an invalid Cache name because it's already used by another cache.",
                                      this, _guid));
      }

      _entryCache = new LruCache<Object,DistCacheEntry>(512);
    }
  }

  /**
   * Returns the object with the given key without checking the backing store.
   */
  public Object peek(Object key)
  {
    DistCacheEntry cacheEntry = _entryCache.get(key);

    return (cacheEntry != null) ? cacheEntry.peek() : null;
  }

  /**
   * Returns the object with the given key, checking the backing
   * store if necessary.
   */
  public Object get(Object key)
  {
    return getDistCacheEntry(key).get(_config);
  }

  /**
   * Returns the object with the given key, updating the backing
   * store if necessary.
   */
  public Object getLazy(Object key)
  {
    return getDistCacheEntry(key).getLazy(_config);
  }

  /**
   * Fills an output stream with the value for a key.
   */
  public boolean get(Object key, OutputStream os)
    throws IOException
  {
    return getDistCacheEntry(key).getStream(os, _config);
  }

  /**
   * Returns the cache entry for the object with the given key.
   */
  public ExtCacheEntry getExtCacheEntry(Object key)
  {
    return getDistCacheEntry(key).getMnodeValue(_config);
  }

  /**
   * Returns the cache entry for the object with the given key.
   */
  public ExtCacheEntry peekExtCacheEntry(Object key)
  {
    return getDistCacheEntry(key).getMnodeValue();
  }

  /**
   * Returns the cache entry for the object with the given key.
   */
  public CacheEntry getCacheEntry(Object key)
  {
    return getExtCacheEntry(key);
  }

  /**
   * Puts a new item in the cache.
   *
   * @param key   the key of the item to put
   * @param value the value of the item to put
   */
  public Object put(Object key, Object value)
  {
    Object object = getDistCacheEntry(key).put(value, _config);
    notifyPut(key);

    return object;
  }

  /**
   * Puts a new item in the cache with a custom idle
   * timeout (used for sessions).
   *
   * @param key         the key of the item to put
   * @param is          the value of the item to put
   * @param idleTimeout the idle timeout for the item
   */
  public ExtCacheEntry put(Object key,
                           InputStream is,
                           long idleTimeout)
    throws IOException
  {
    return getDistCacheEntry(key).put(is, _config, idleTimeout);
  }

  /**
   * Updates the cache if the old version matches the current version.
   * A zero value for the old value hash only adds the entry if it's new.
   *
   * @param key     the key to compare
   * @param version the version of the old value, returned by getEntry
   * @param value   the new value
   * @return true if the update succeeds, false if it fails
   */
  public boolean compareAndPut(Object key,
                               long version,
                               Object value)
  {
    put(key, value);

    return true;
  }

  /**
   * Updates the cache if the old version matches the current value.
   * A zero value for the old version only adds the entry if it's new.
   *
   * @param key         the key to compare
   * @param version     the hash of the old version, returned by getEntry
   * @param inputStream the new value
   * @return true if the update succeeds, false if it fails
   */
  public boolean compareAndPut(Object key,
                               long version,
                               InputStream inputStream)
    throws IOException
  {
    put(key, inputStream);

    return true;
  }

  /**
   * Removes the entry from the cache.
   *
   * @return true if the object existed
   */
  public Object remove(Object key)
  {
    notifyRemove(key);
    return getDistCacheEntry(key).remove(_config);
  }

  /**
   * Removes the entry from the cache if the current entry matches the version.
   */
  public boolean compareAndRemove(Object key, long version)
  {
    DistCacheEntry cacheEntry = getDistCacheEntry(key);

    if (cacheEntry.getVersion() == version) {
      remove(key);

      return true;
    }

    return false;
  }

  /**
   * Returns the CacheKeyEntry for the given key.
   */
  protected DistCacheEntry getDistCacheEntry(Object key)
  {
    DistCacheEntry cacheEntry = _entryCache.get(key);

    if (cacheEntry == null) {
      cacheEntry = _manager.getCacheEntry(key, _config);

      _entryCache.put(key, cacheEntry);
    }

    return cacheEntry;
  }

  /**
   * Returns a set containing an entry for each key->value pair in the local cache.
   */
  public Set<Map.Entry> entrySet()
  {
    return new CacheEntrySet<Entry>(_entryCache);
  }

  /**
   * Returns a new map of the items found in the central cache.
   *
   * @note If a cacheLoader is configured if an item is not found in the cache.
   */
  public Map getAll(Collection keys)
  {
    Map result = new HashMap();

    for (Object key : keys) {
      Object value = get(key);

      if (value != null) {
        result.put(key, value);
      }
    }

    return result;
  }

  /**
   * Loads an item into the cache if not already there and was returned from  in the optional cache loader.
   *
   * @param key
   */
  public void load(Object key)
  {
    if (containsKey(key) || get(key) != null)
      return;

    Object loaderValue = cacheLoader(key);

    if (loaderValue != null)
      put(key, loaderValue);

    notifyLoad(key);
  }

  /**
   * Implements the loadAll method for a collection of keys.
   */
  public void loadAll(Collection keys)
  {
    Map<Object, Object> entries = null;
    CacheLoader loader = _config.getCacheLoader();

    if (loader == null || keys == null || keys.size() == 0)
      return;

    entries = loader.loadAll(keys);

    if (entries.isEmpty())
      return;

    for (Entry loaderEntry : entries.entrySet()) {
      Object loaderKey = loaderEntry.getKey();
      if (!containsKey(loaderKey) && (get(loaderKey) != null)) {
        put(loaderKey, loaderEntry.getValue());
        notifyLoad(loaderKey);
      }
    }
  }

  /**
   * Adds a listener to the cache.
   */
  public void addListener(CacheListener listener)
  {
    _listeners.add(listener);
  }

  /**
   * Removes a listener from the cache.
   */
  public void removeListener(CacheListener listener)
  {
    _listeners.remove(listener);
  }

  /**
   * Returns the CacheStatistics for this cache.
   */
  public CacheStatistics getCacheStatistics()
  {
    return this;
  }

  /**
   * Ignored, since evictions are handled by the container.
   */
  public void evict()
  {
    notifyEvict(null);
  }

  /**
   * Returns a collection of the values in the local cache.
   */
  public Collection values()
  {
    return new CacheValues(_entryCache);
  }

  /**
   * Returns a set of the keys in the local set.
   */
  public Set keySet()
  {
    return new CacheKeys(_entryCache);
  }

  /**
   * Returns true if the value is contained in the local cache.
   */
  @Override
  public boolean containsValue(Object value)
  {
    if (value == null)
      return false;

    Collection values = values();

    for (Object item : values) {
      if (value.equals(item))
        return true;
    }

    return false;
  }

  /**
   * Removes all items from the local cache.
   */
  @Override
  public void clear()
  {
    _entryCache.clear();
    notifyClear(null);
  }

  /**
   * Returns the number current size of the cache.
   */
  @Override
  public int size()
  {
    return _entryCache.size();
  }

  /**
   * Puts each item in the map into the cache.
   */
  @Override
  public void putAll(Map map)
  {
    if (map == null || map.size() == 0)
      return;
    Set entries = map.entrySet();

    for (Object item : entries) {
      Map.Entry entry = (Map.Entry) item;
      put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Returns true if an entry for the item is found in  the cache.
   *
   * @param key
   * @return
   */
  @Override
  public boolean containsKey(Object key)
  {
    return _entryCache.get(key) != null;
  }

  /**
   * Returns true is the local cache is empty
   *
   * @return
   */
  @Override
  public boolean isEmpty()
  {
    return _entryCache.size() == 0;
  }

  /**
   * Returns the number of cache hits that have occured since the cache started or
   * since the last call to clearStatistics.
   */
  public int getCacheHits()
  {
    return (int) (_entryCache.getHitCount() - _priorHits);
  }

  /**
   * Returns the number of cache misses that have occured since the cache started or
   * since the last call to clearStatistics.
   */
  public int getCacheMisses()
  {
    return (int) (_entryCache.getMissCount() - _priorMisses);
  }

  /**
   * Returns the number of entries currently in the local cache.
   */
  public int getObjectCount()
  {
    return _entryCache.size();
  }

  /**
   * Simulates a reset of the counters for cache hits and misses.
   */
  public void clearStatistics()
  {
    _priorHits = _entryCache.getHitCount();
    _priorMisses = _entryCache.getMissCount();
  }

  /**
   * Defines the accuracy of this implementation.
   */
  public int getStatisticsAccuracy()
  {
    return CacheStatistics.STATISTICS_ACCURACY_BEST_EFFORT;
  }

  public boolean isBackup()
  {
    return _config.isBackup();
  }

  public boolean isTriplicate()
  {
    return _config.isTriplicate();
  }

  /**
   * Places an item in the cache from the loader unless the item is in cache already.
   */
  protected Object cacheLoader(Object key)
  {
    Object value = get(key);

    if (value != null)
      return value;

    CacheLoader loader = _config.getCacheLoader();

    value = (loader != null) ? loader.load(key) : null;

    if (value != null)
      put(key, value);
    notifyLoad(key);

    return value;
  }

  protected  void setPersistenceMode(Persistence persistence)
  {
      switch (persistence) {
      case NONE:
        setTriplicate(false);
        setBackup(false);
        break;

      case SINGLE:
        setTriplicate(false);
        setBackup(true);
        break;

      case TRIPLE:
      default:
        setTriplicate(true);
    }
  }

  protected void notifyLoad(Object key)
  {
    for (CacheListener listener : _listeners) {
      listener.onLoad(key);
    }
  }

  protected void notifyEvict(Object key)
  {
    for (CacheListener listener : _listeners) {
      listener.onEvict(key);
    }
  }

  protected void notifyClear(Object key)
  {
    for (CacheListener listener : _listeners) {
      listener.onClear(key);
    }
  }

  protected void notifyPut(Object key)
  {
    for (CacheListener listener : _listeners) {
      listener.onPut(key);
    }
  }

  protected void notifyRemove(Object key)
  {
    for (CacheListener listener : _listeners) {
      listener.onRemove(key);
    }
  }

  public boolean isClosed()
  {
    return _isClosed;
  }

  public void close()
  {
    _isClosed = true;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _guid + "]";
  }

  private void initName(String name)
    throws ConfigException
  {
    if (_name == null || _name.length() == 0)
      throw new ConfigException(L.l("Each Cache must have a name."));

    String contextId = Environment.getEnvironmentName();

    if (_guid == null)
      _guid = contextId + ":" + _name;

    _config.setGuid(_guid);
  }

  private void initPersistence(String persistence)
    throws ConfigException
  {
    Persistence result  = Persistence.TRIPLE;

    if (persistence != null) {
      try {
        result = Persistence.valueOf(persistence.toUpperCase());
      }
      catch (Exception e) {
        throw new ConfigException(L.l("'{0}' is not a valid Persistence option", persistence));
      }
    }

    setPersistenceMode(result);
  }

  private void initScope(String scopeName)
    throws ConfigException
  {
    Scope scope = null;

    if (_scopeName != null) {
      try {
        scope = Scope.valueOf(_scopeName.toUpperCase());
      }
      catch (Exception e) {
        throw new ConfigException(L.l("'{0}' is not a valid Scope option", scopeName));
      }
    }

    setScopeMode(scope);
  }

  private void initServer()
    throws ConfigException
  {
    Server server = Server.getCurrent();

    if (server == null)
      throw new ConfigException(L.l("'{0}' cannot be initialized because it is not in a clustered environment",
                                    getClass().getSimpleName()));

    _manager = server.getDistributedCacheManager();

    if (_manager == null)
      throw new IllegalStateException("distributed cache manager not available");
  }


  /**
   * Defines the scope options for a cache.
   */
  public enum Scope {

    /** Not distributed, no persistence.*/
    LOCAL,

    /** Not distributed, single or no persistence */
    SERVER,

    /** Distributed across a pod, persistence required.*/
    POD,

    /** Accessible across a multi-pod cluster*/
    CLUSTER,

    /** Support CRUD operation with basic access control*/
    GLOBAL
  }

  /**
   * Defines the persistence options for a cache.
   */
  public enum Persistence {

    /**
     * No persistence.
     */
    NONE,

    /**
     * A single copy of the cache is persisted on one server in the cluster.
     */
    SINGLE,

    /**
     * Three copies of the cache and its entrys are saved on three servers.
     */
    TRIPLE
  }

  /**
   * provides the implementation of the set of entries over the
   * cache,
   */
  protected static class CacheEntrySet<E>
    extends AbstractSet
  {
    private LruCache<Object, CacheEntry> _lruCache;

    protected CacheEntrySet(LruCache cache)
    {
      super();
      _lruCache = cache;
    }

    public Iterator iterator()
    {
      return new CacheEntrySetIterator<Object,CacheEntry>(_lruCache);
    }

    public int size()
    {
      return _lruCache.size();
    }
  }

  /**
   * Provides an iterator over the entries in the the local cache.
   */
  protected static class CacheEntrySetIterator<K, V>
    implements Iterator
  {
    private Iterator<LruCache.Entry<K, V>> _iterator;

    protected CacheEntrySetIterator(LruCache<K, V> lruCache)
    {
      _iterator = lruCache.iterator();
    }

    public Object next()
    {
      if (!hasNext())
        throw new NoSuchElementException();

      LruCache.Entry<K, V> entry = _iterator.next();
      CacheEntry cacheEntry = (CacheEntry) entry.getValue();

      return new AbstractMap.SimpleEntry<Object, Object>(
        cacheEntry.getKey(),
        cacheEntry.getValue());
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    /**
     *
     */
    public void remove()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }
  }

  /**
   * Provides the values contined in the local cache.
   */
  protected static class CacheValues<K,V>
    extends CacheEntrySet
  {
    private LruCache<K, V> _lruCache;

    public CacheValues(LruCache<K,V> lruCache)
    {
      super(lruCache);
      _lruCache = lruCache;
    }

    /**
     * Override the entry set iterator to return the value for the entry.
     *
     * @return
     */
    @Override
    public Iterator iterator()
    {
      return new CacheEntrySetIterator<K,V>(_lruCache) {
        @Override
        public Object next()
        {
          return ((Entry) super.next()).getValue();
        }
      };
    }
  }

  /**
   * Provides access to the keys of the map as a set.
   */
  protected static class CacheKeys
    extends CacheEntrySet
  {
    private LruCache _lruCache;

    public CacheKeys(LruCache cache)
    {
      super(cache);

      _lruCache = cache;
    }

    /**
     * Override the entry set iterator to return the key for the entry.
     */
    @Override
    public Iterator iterator()
    {
      return new CacheEntrySetIterator<Object, Object>(_lruCache) {
        @Override
        public Object next()
        {
          return ((Entry) super.next()).getKey();
        }
      };
    }
  }
}
