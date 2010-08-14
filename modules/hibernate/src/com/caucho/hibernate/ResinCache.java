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

package com.caucho.hibernate;

import java.util.Map;
import org.hibernate.cache.*;
import com.caucho.distcache.ClusterCache;

public class ResinCache implements Cache {
  private String _name;
  private ClusterCache _cache;

  ResinCache(String name, ClusterCache cache)
  {
    _name = name;
    _cache = cache;
  }

  public String getRegionName()
  {
    return _name;
  }

  /**
   * Returns object from cache
   */
  public Object read(Object key)
    throws CacheException
  {
    return _cache.get(key);
  }

  /**
   * Returns item from cache, non-XA
   */
  public Object get(Object key)
    throws CacheException
  {
    Object value = _cache.get(key);

    return value;
  }

  /**
   * Adds an item to the cache, non-XA with failfast.
   */
  public void put(Object key, Object value)
  {
    _cache.put(key, value);
  }

  /**
   * Adds an item to the cache (XA?)
   */
  public void update(Object key, Object value)
    throws CacheException
  {
    _cache.put(key, value);
  }

  /**
   * Remove an item from the cache (XA?)
   */
  public void remove(Object key)
    throws CacheException
  {
    _cache.remove(key);
  }

  /**
   * Clear the cache
   */
  public void clear()
    throws CacheException
  {
  }

  /**
   * Close the cache
   */
  public void destroy()
    throws CacheException
  {
  }

  /**
   * Lock an item for a clustered cache.
   */
  public void lock(Object key)
    throws CacheException
  {
  }

  /**
   * Unlock an item for a clustered cache.
   */
  public void unlock(Object key)
    throws CacheException
  {
  }

  public long nextTimestamp()
  {
    return Timestamper.next();
  }

  public int getTimeout()
  {
    return 10000;
  }

  public long getSizeInMemory()
  {
    return -1;
  }

  public long getElementCountInMemory()
  {
    return -1;
  }

  public long getElementCountOnDisk()
  {
    return -1;
  }

  public Map toMap()
  {
    return null;
  }
}