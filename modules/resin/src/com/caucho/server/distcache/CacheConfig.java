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

package com.caucho.server.distcache;

import com.caucho.config.Configurable;
import com.caucho.distcache.AbstractCache;
import com.caucho.distcache.CacheSerializer;
import com.caucho.distcache.HessianSerializer;
import com.caucho.util.Alarm;
import com.caucho.util.HashKey;

import javax.cache.CacheLoader;
import javax.cache.CacheStatistics;

/**
 * Manages the distributed cache
 */
public class CacheConfig
{
  public static final long TIME_INFINITY  = Long.MAX_VALUE / 2;
  public static final long TIME_HOUR  = 3600 * 1000L;
  public static final int FLAG_EPHEMERAL  = 0x01;
  public static final int FLAG_BACKUP = 0x02;
  public static final int FLAG_TRIPLICATE = 0x04;
  
  public static final int FLAG_CLUSTER = 0x08;
  public static final int FLAG_GLOBAL = 0x10;

  private String _guid;
  private HashKey _cacheKey;

  private int _flags = (FLAG_BACKUP | FLAG_TRIPLICATE);

  private long _expireTimeout = 24 * TIME_HOUR;

  private long _expireTimeoutWindow = 0;

  private long _idleTimeout = 24 * TIME_HOUR;
  private long _idleTimeoutWindow = -1;

  private long _localReadTimeout
    = Alarm.isTest() ? -1 : 250L; // 250ms default timeout, except for QA

  private long _leaseTimeout = 5 * 60 * 1000; // 5 min lease timeout

  private CacheLoader _cacheLoader;

  private CacheSerializer _keySerializer;
  private CacheSerializer _valueSerializer;

  private int _accuracy;

  private AbstractCache.Scope _scope;

  /**
   * The Cache will use a CacheLoader to populate cache misses.
   */

  public CacheLoader getCacheLoader()
  {
    return _cacheLoader;
  }

  /**
   * Sets the CacheLoader that the Cache can then use to
   * populate cache misses for a reference store (database)
   */
  public void setCacheLoader(CacheLoader cacheLoader)
  {
    _cacheLoader = cacheLoader;
  }

  /**
   * Returns the globally-unique id for the cache.
   */
  public String getGuid()
  {
    return _guid;
  }

  /**
   * Sets the globally-unique id for the cache
   */
  public void setGuid(String guid)
  {
    _guid = guid;
  }


  /**
   * Returns the globally-unique id for the cache.
   */
  public HashKey getCacheKey()
  {
    return _cacheKey;
  }

  /**
   * Sets the globally-unique id for the cache
   */
  public void setCacheKey(HashKey cacheKey)
  {
    _cacheKey = cacheKey;
  }

  /**
   * Returns internal flags
   */
  public int getFlags()
  {
    return _flags;
  }

  /**
   * Sets inteneral flags
   */
  public void setFlags(int flags)
  {
    _flags = flags;
  }

  /**
   * The maximum valid time for an item.  Items stored in the cache
   * for longer than the expire time are no longer valid and will
   * return null from a get.
   *
   * Default is infinite.
   */
  public long getExpireTimeout()
  {
    return _expireTimeout;
  }

  /**
   * Returns the expire check window, i.e. the precision of the expire
   * check.  Since an expired item can cause a massive cascade of
   * attempted loads from the backup, the actual expiration is randomized.
   */
  public long getExpireTimeoutWindow()
  {
    return _expireTimeoutWindow;
  }

  /**
   * The maximum valid time for an item.  Items stored in the cache
   * for longer than the expire time are no longer valid and will
   * return null from a get.
   *
   * Default is infinite.
   */
  @Configurable
  public void setExpireTimeout(long expireTimeout)
  {
    if (expireTimeout < 0 || TIME_INFINITY <= expireTimeout)
      expireTimeout = TIME_INFINITY;
    else
      _expireTimeout = expireTimeout;
  }

  /**
   * Returns the expire check window, i.e. the precision of the expire
   * check.  Since an expired item can cause a massive cascade of
   * attempted loads from the backup, the actual expiration is randomized.
   */
  public long getExpireCheckWindow()
  {
    return (_expireTimeoutWindow > 0
            ? _expireTimeoutWindow
            : _expireTimeout / 4);
  }

  /**
   * Provides the opportunity to control the expire check window,
   * i.e. the precision of the expirecheck.
   * <p/>
   * Since an expired item can cause a massive cascade of
   * attempted loads from the backup, the actual expiration is randomized.
   */
  @Configurable
  public void setExpireTimeoutWindow(long expireTimeoutWindow)
  {
    _expireTimeoutWindow = expireTimeoutWindow;
  }

  /**
   * The maximum time that an item can remain in cache without being referenced.
   * For example, session data could be configured to be removed if idle for more than 30 minutes.
   * <p/>
   * Cached data would typically have infinite idle time because
   * it doesn't depend on how often it's accessed.
   *
   * Default is infinite.
   */
  public long getIdleTimeout()
  {
    return _idleTimeout;
  }

  public long getIdleTimeoutWindow()
  {
    return _idleTimeoutWindow;
  }

  /**
   * The maximum time that an item can remain in cache without being referenced.
   * For example, session data could be configured to be removed if idle for more than 30 minutes.
   *
   * Cached data would typically use an infinite idle time because
   * it doesn't depend on how often it's accessed.
   */
  public void setIdleTimeout(long idleTimeout)
  {
    if (idleTimeout < 0 || TIME_INFINITY <= idleTimeout)
      idleTimeout = TIME_INFINITY;
    else
      _idleTimeout = idleTimeout;
  }

  /**
   * Returns the idle check window, i.e. the precision of the idle
   * check.
   */
  public long getIdleCheckWindow()
  {
    return (_idleTimeoutWindow > 0
            ? _idleTimeoutWindow
            : _idleTimeout / 4);
  }

  /**
   * Provides the option to set the idle check window,  the amount of time
   * in which the idle time limit can be spread out to smooth performance.
   * <p/>
   * If this optional value is not set, the system  uses a fraction of the
   * idle time.
   */
  public void setIdleTimeoutWindow(long idleTimeoutWindow)
  {
    _idleTimeoutWindow = idleTimeoutWindow;
  }

  /**
   * Returns the lease timeout, which is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  public long getLeaseTimeout()
  {
    return _leaseTimeout;
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  public void setLeaseTimeout(long timeout)
  {
    _leaseTimeout = timeout;
  }

  /**
   * The local read timeout is the time a local copy of the
   * cache is considered valid.
   */
  public long getLocalReadTimeout()
  {
    return _localReadTimeout;
  }

  /**
   * The local read timeout is the time a local copy of the
   * cache is considered valid.
   */
  public void setLocalReadTimeout(long timeout)
  {
    _localReadTimeout = timeout;
  }

  /**
   * Returns the key serializer
   */
  public CacheSerializer getKeySerializer()
  {
    return _keySerializer;
  }

  /**
   * Returns the value serializer
   */
  public CacheSerializer getValueSerializer()
  {
    return _valueSerializer;
  }

  /**
   * Sets the value serializer
   */
  public void setValueSerializer(CacheSerializer serializer)
  {
    _valueSerializer = serializer;
  }

  public boolean isBackup()
  {
    return (getFlags() & CacheConfig.FLAG_BACKUP) != 0;
  }

  /**
   * Sets the backup mode.  If backups are enabled, copies of the
   * cache item will be sent to the owning triad server.
   * <p/>
   * Defaults to true.
   */
  public void setBackup(boolean isBackup)
  {
    if (isBackup)
      setFlags(getFlags() | CacheConfig.FLAG_BACKUP);
    else
      setFlags(getFlags() & ~CacheConfig.FLAG_BACKUP);
  }

  /**
   * Sets the global mode.  If global is enabled, copies of the
   * cache item will be sent to all clusters.
   * <p/>
   * Defaults to false.
   */
  public boolean isGlobal()
  {
    return (getFlags() & CacheConfig.FLAG_GLOBAL) != 0;
  }

  /**
   * Sets the global mode.  If global is enabled, copies of the
   * cache item will be sent to all clusters.
   * <p/>
   * Defaults to false.
   */
  public void setGlobal(boolean isGlobal)
  {
    if (isGlobal)
      setFlags(getFlags() | CacheConfig.FLAG_GLOBAL);
    else
      setFlags(getFlags() & ~CacheConfig.FLAG_GLOBAL);
  }

  /**
   * Returns true is the triplicate backup mode enabled so that
   * all triad servers have a copy of the cache item.
   * <p/>
   * Defaults is true.
   */
  public boolean isTriplicate()
  {
    return (getFlags() & CacheConfig.FLAG_TRIPLICATE) != 0;
  }

  /**
   * Sets the triplicate backup mode.  If triplicate backups is set,
   * all triad servers have a copy of each cached item.
   * <p/>
   * Defaults to true.
   */
  public void setTriplicate(boolean isTriplicate)
  {
    if (isTriplicate)
      setFlags(getFlags() | CacheConfig.FLAG_TRIPLICATE);
    else
      setFlags(getFlags() & ~CacheConfig.FLAG_TRIPLICATE);
  }

  /**
   * Returns the level of accuracy supported by the implementation of JCache
   */
  public int getCacheStatisticsAccuraccy()
  {
    return _accuracy;
  }

  /**
   * Sets the {@link AbstractCache.Scope} of this cache.
   */
  public void setScopeMode(AbstractCache.Scope scope)
  {
    _scope = scope;
  }

  /**
   * Returns the {@link AbstractCache.Scope} defined for this cache.
   * @return
   */
  public AbstractCache.Scope getScopeMode()
  {
    return _scope;
  }

  /**
   * Initializes the CacheConfig.
   */
  public void init()
  {
    if (_keySerializer == null)
      _keySerializer = new HessianSerializer();

    if (_valueSerializer == null)
      _valueSerializer = new HessianSerializer();

    _accuracy = CacheStatistics.STATISTICS_ACCURACY_BEST_EFFORT;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
