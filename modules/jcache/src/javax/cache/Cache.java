/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This interface is defined in JSR 107.
 *
 * It may be used to access both local and cluster caches.
 *
 * Some bulk operations will act only upon the local cache, and will not affect a cluster cache, as noted in the
 * JavaDoc entry for each method.
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

package javax.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * The persistent or distributed cache is usable like a normal map, but loads
 * and stores data across the cluster.
 */
public interface Cache extends Map {
    /**
     * Returns the object specified by the given key.
     * <p/>
     * If the item does not exist and a CacheLoader has been specified,
     * the CacheLoader will be used to create a cache value.
     */
    public Object get(Object key);

    /**
     * Puts a new item in the cache.
     *
     * @param key   the key of the item to put
     * @param value the value of the item to put
     */
    public Object put(Object key, Object value);

    /**
     * Removes the entry from the cache
     */
    public Object remove(Object key);

    /**
     * Returns the cache entry for the object with the given key.
     */
    public CacheEntry getCacheEntry(Object key);

    /**
     * Returns the object with the given key, but does not check
     * distributed caches or trigger a CacheLoader.
     */
    public Object peek(Object key);

    /**
     * Loads and returns a map of all values specified by the key collection.
     */
    public Map getAll(Collection keys)
      throws CacheException;

    /**
     * Asynchronous call to preload the cache item.
     */
    public void load(Object key) throws CacheException;

    /**
     * Asynchronous call to preload all the cache items mentioned by the keys.
     * This method can  be used to "warm-up" a cache.
     */
    public void loadAll(Collection keys) throws CacheException;

    /**
     * Returns the CacheStatistics instance for this cache.
     */
    public CacheStatistics getCacheStatistics();

    /**
     * Removes expired items from the cache
     */
    public void evict();

    /**
     * Adds a listener for cache events
     */
    public void addListener(CacheListener listener);

    /**
     * Removes a registered listener for cache events
     */
    public void removeListener(CacheListener listener);

    /**
     * Returns true if the key is present in the local cache.
     */
    public boolean containsKey(Object key);

    /**
     * Returns true if the value is present in at least one entry in the local cache.
     * Note that this is an O(size()) method.
     */
    public boolean containsValue(Object value);

    /**
     * Returns the set of entries from the current local cache.
     */
    public Set entrySet();

    /**
     * Returns true is the local cache is empty.
     */
    public boolean isEmpty();

    /**
     * Returns the set of keys currently present in the local cache.
     */
    public Set keySet();

    /**
     * Adds the content of the map to the Cache.
     */
    public void putAll(Map map);

    /**
     * Returns the number of entries in the local cache.
     */
    public int size();

    /**
     * Retunrs a collection of the values in the local cache.
     */
    public Collection values();

    /**
     *  Clears the local cache of all entries.
     */
    public void clear();
}
