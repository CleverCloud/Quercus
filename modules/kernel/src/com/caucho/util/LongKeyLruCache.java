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

package com.caucho.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fixed length cache with a LRU replacement policy.  If cache items
 * implement CacheListener, they will be informed when they're removed
 * from the cache.
 *
 * <p>LongKeyLruCache is synchronized.
 */
public class LongKeyLruCache<V> {
  private static final Logger log
    = Logger.getLogger(LongKeyLruCache.class.getName());
  
  private static final int LRU_MASK = 0x3fffffff;
  
  // maximum allowed entries
  private final int _capacity;
  // size 1 capacity is half the actual capacity
  private final int _capacity1;
  
  // hash table containing the entries.  Its size is twice the capacity
  // so it will always remain at least half empty
  private final CacheItem<V> []_entries;
  private final Object []_locks;
  
  // mask for the hash
  private final int _prime;
  
  //
  // LRU
  //

  private final Object _lruLock = new Object();
  
  // number of items in the cache seen once
  private int _size1;

  // head of the LRU list
  private CacheItem<V> _head1;
  // tail of the LRU list
  private CacheItem<V> _tail1;
  
  // number of items in the cache seen more than once
  private int _size2;

  // head of the LRU list
  private CacheItem<V> _head2;
  // tail of the LRU list
  private CacheItem<V> _tail2;

  // lru timeout reduces lru updates for the most used items
  private final int _lruTimeout;

  private final AtomicBoolean _isLruTailRemove = new AtomicBoolean();

  // counts group 2 updates, rolling over at 0x3fffffff
  private volatile int _lruCounter;

  // hit count statistics
  private volatile long _hitCount;
  // miss count statistics
  private volatile long _missCount;
  
  /**
   * Create the LRU cache with a specific capacity.
   *
   * @param initialCapacity minimum capacity of the cache
   */
  public LongKeyLruCache(int initialCapacity)
  {
    int capacity = calculateCapacity(initialCapacity);

    _entries = new CacheItem[capacity];
    _prime = Primes.getBiggestPrime(_entries.length);

    _locks = new Object[(_entries.length >> 3) + 1];
    for (int i = 0; i < _locks.length; i++) {
      _locks[i] = new Object();
    }

    _capacity = initialCapacity;
    _capacity1 = _capacity / 2;

    if (capacity > 32)
      _lruTimeout = capacity / 32;
    else
      _lruTimeout = 1;
  }

  /**
   * Returns the current number of entries in the cache.
   */
  public int size()
  {
    return _size1 + _size2;
  }

  /**
   * Returns the capacity.
   */
  public int getCapacity()
  {
    return _capacity;
  }

  /**
   * Ensure the cache can contain the given value.
   */
  public LongKeyLruCache<V> ensureCapacity(int newCapacity)
  {
    int capacity = calculateCapacity(newCapacity);

    if (capacity <= _entries.length)
      return this;
    else
      return setCapacity(newCapacity);
  }

  public LongKeyLruCache<V> setCapacity(int newCapacity)
  {
    int capacity = calculateCapacity(newCapacity);

    if (capacity == _entries.length)
      return this;
    
    LongKeyLruCache<V> newCache = new LongKeyLruCache<V>(newCapacity);

    for (int i = 0; i < _entries.length; i++) {
      Object lock = getLock(i);
      
      synchronized (lock) {
        for (CacheItem<V> item = _entries[i];
             item != null;
             item = item._nextHash) {
          newCache.put(item._key, (V) item._value);
        }

        _entries[i] = null;
      }
    }

    return newCache;
  }

  private int calculateCapacity(int initialCapacity)
  {
    int capacity;
    
    for (capacity = 16; capacity < 8 * initialCapacity; capacity *= 2) {
    }

    return capacity;
  }

  /**
   * Clears the cache
   */
  public void clear()
  {
    ArrayList<CacheListener> listeners = null;
    ArrayList<SyncCacheListener> syncListeners = null;

    for (int i = _entries.length - 1; i >= 0; i--) {
      Object lock = getLock(i);
      
      synchronized (lock) {
        CacheItem<V> item = _entries[i];

        for (; item != null; item = item._nextHash) {
          removeLruItem(item);
          if (item._value instanceof CacheListener) {
            if (listeners == null)
              listeners = new ArrayList<CacheListener>();
            listeners.add((CacheListener) item._value);
          }

          if (item._value instanceof SyncCacheListener) {
            if (syncListeners == null)
              syncListeners = new ArrayList<SyncCacheListener>();
            syncListeners.add((SyncCacheListener) item._value);
          }
        }
        
        _entries[i] = null;
      }
    }

    for (int i = listeners == null ? -1 : listeners.size() - 1; i >= 0; i--) {
      CacheListener listener = listeners.get(i);
      listener.removeEvent();
    }

    for (int i = syncListeners == null ? -1 : syncListeners.size() - 1;
         i >= 0;
         i--) {
      SyncCacheListener listener = syncListeners.get(i);
      listener.syncRemoveEvent();
    }
  }

  /**
   * Get an item from the cache and make it most recently used.
   *
   * @param key key to lookup the item
   * @return the matching object in the cache
   */
  public V get(long key)
  {
    int hash = hash(key) % _prime;

    CacheItem<V> item;

    for (item = _entries[hash];
         item != null;
         item = item._nextHash) {
      if (item._key == key) {
        updateLru(item);

        _hitCount++;

        return item._value;
      }
    }
      
    _missCount++;

    return null;
  }

  /**
   * Puts a new item in the cache.  If the cache is full, remove the
   * LRU item.
   *
   * @param key key to store data
   * @param value value to be stored
   *
   * @return old value stored under the key
   */
  public V put(long key, V value)
  {
    V oldValue = put(key, value, true);

    if (oldValue instanceof CacheListener)
      ((CacheListener) oldValue).removeEvent();

    return oldValue;
  }

  /**
   * Puts a new item in the cache.  If the cache is full, remove the
   * LRU item.
   *
   * @param key key to store data
   * @param value value to be stored
   *
   * @return the value actually stored
   */
  public V putIfNew(long key, V value)
  {
    V oldValue = put(key, value, false);

    if (oldValue != null)
      return oldValue;
    else
      return value;
  }

  /**
   * Puts a new item in the cache.  If the cache is full, remove the
   * LRU item.
   *
   * @param key key to store data
   * @param value value to be stored
   *
   * @return the value actually stored
   */
  public V putIfAbsent(long key, V value)
  {
    return put(key, value, false);
  }

  /**
   * Puts a new item in the cache.  If the cache is full, remove the
   * LRU item.
   *
   * @param key key to store data
   * @param value value to be stored
   *
   * @return old value stored under the key
   */
  private V put(long key, V value, boolean replace)
  {
    removeLru();

    int hash = hash(key) % _prime;

    V oldValue = null;

    Object lock = getLock(hash);

    synchronized (lock) {
      CacheItem<V> item = _entries[hash];
      for (;
           item != null;
           item = item._nextHash) {
        // matching item gets replaced
        if (item._key == key) {
          updateLru(item);

          oldValue = item._value;

          if (replace) {
            if (oldValue instanceof SyncCacheListener) {
              ((SyncCacheListener) oldValue).syncRemoveEvent();
            }

            item._value = value;
          }

          break;
        }
      }

      // No matching item, so create one
      if (item == null) {
        CacheItem<V> next = _entries[hash];
        
        item = new CacheItem<V>(key, value);

        addNewLruItem(item);
        
        item._nextHash = next;
        _entries[hash] = item;

        return null;
      }
    }

    if (replace && oldValue instanceof CacheListener)
      ((CacheListener) oldValue).removeEvent();

    return oldValue;
  }

  private void addNewLruItem(CacheItem<V> item)
  {
    synchronized (_lruLock) {
      _lruCounter = (_lruCounter + 1) & LRU_MASK;
      item._lruCounter = _lruCounter;
          
      _size1++;

      item._nextLru = _head1;
      if (_head1 != null)
        _head1._prevLru = item;
      _head1 = item;
          
      if (_tail1 == null)
        _tail1 = item;
    }
  }

  /**
   * Put item at the head of the used-twice lru list.
   * This is always called while synchronized.
   */
  private void updateLru(CacheItem<V> item)
  {
    long lruCounter = _lruCounter;
    long itemCounter = item._lruCounter;

    long delta = (lruCounter - itemCounter) & LRU_MASK;

    if (_lruTimeout < delta || delta < 0) {
      // update LRU only if not used recently
      updateLruImpl(item);
    }
  }

  /**
   * Put item at the head of the used-twice lru list.
   * This is always called while synchronized.
   */
  private void updateLruImpl(CacheItem<V> item)
  {
    synchronized (_lruLock) {
      _lruCounter = (_lruCounter + 1) & LRU_MASK;

      item._lruCounter = _lruCounter;
      
      CacheItem<V> prevLru = item._prevLru;
      CacheItem<V> nextLru = item._nextLru;

      if (item._hitCount <= 0) {
        // item deleted before update
        return;
      }
      else if (item._hitCount == 1) {
        item._hitCount = 2;

        item._prevLru = null;
        item._nextLru = _head2;
        
        if (prevLru != null)
          prevLru._nextLru = nextLru;
        else {
          assert(_head1 == item);
          
          _head1 = nextLru;
        }

        if (nextLru != null)
          nextLru._prevLru = prevLru;
        else {
          assert(_tail1 == item);
          _tail1 = prevLru;
        }

        if (_head2 != null)
          _head2._prevLru = item;
        else {
          assert(_tail2 == null);
          
          _tail2 = item;
        }
      
        _head2 = item;

        _size1--;
        _size2++;
      }
      else {
        if (item == _head2)
          return;

        item._prevLru = null;
        item._nextLru = _head2;
      
        prevLru._nextLru = nextLru;
      
        _head2._prevLru = item;
        _head2 = item;
      
        if (nextLru != null)
          nextLru._prevLru = prevLru;
        else {
          assert(_tail2 == item);
          
          _tail2 = prevLru;
        }
      }
    }
  }

  private void removeLru()
  {
    if (_capacity <= _size1 + _size2) {
      if (_isLruTailRemove.compareAndSet(false, true)) {
        try {
          // remove LRU items until we're below capacity
          while (_capacity <= _size1 + _size2 && removeTail()) {
          }
        } finally {
          _isLruTailRemove.set(false);
        }
      }
    }
  }

  /**
   * Remove the last item in the LRU
   */
  public boolean removeTail()
  {
    CacheItem<V> tail = null;

    if (_capacity1 <= _size1)
      tail = _tail1;

    if (tail == null) {
      tail = _tail2;
      
      if (tail == null) {
        tail = _tail1;

        if (tail == null)
          return false;
      }
    }

    if (tail == null)
      return false;

    Object value = remove(tail._key, true);
    
    return value != null;
  }

  /**
   * Removes an item from the cache
   *
   * @param key the key to remove
   *
   * @return the value removed
   */
  public V remove(long key)
  {
    return remove(key, false);
  }

  /**
   * Removes an item from the cache
   *
   * @param key the key to remove
   *
   * @return the value removed
   */
  private V remove(long key, boolean isTail)
  {
    int hash = hash(key) % _prime;

    V value = null;

    Object lock = getLock(hash);
    
    synchronized (lock) {
      CacheItem<V> prevItem = null;
      
      for (CacheItem<V> item = _entries[hash];
           item != null;
           item = item._nextHash) {
        if (item._key == key) {
          value = item._value;
          
          SyncCacheListener syncListener = null;

          // sync must occur before remove because get() is non-locking
          if (value instanceof SyncCacheListener) {
            syncListener = (SyncCacheListener) value;
            
            if (isTail && ! syncListener.startLruRemove()) {
              item._lruCounter = _lruCounter - _lruTimeout - 2;
              updateLruImpl(item);
              return null;
            }
          }
          
          removeLruItem(item);

          // sync must occur before remove because get() is non-locking
          if (syncListener != null) {
            if (isTail)
              syncListener.syncLruRemoveEvent();
            else
              syncListener.syncRemoveEvent();
          }
          
          CacheItem<V> nextHash = item._nextHash;

          if (prevItem != null)
            prevItem._nextHash = nextHash;
          else {
            assert(_entries[hash] == item);
                   
            _entries[hash] = nextHash;
          }

          break;
        }

        prevItem = item;
      }
    }

    if (value instanceof CacheListener)
      ((CacheListener) value).removeEvent();

    return value;
  }

  private void removeLruItem(CacheItem<V> item)
  {
    synchronized (_lruLock) {
      _lruCounter = (_lruCounter + 1) & 0x3fffffff;
      
      CacheItem<V> prevLru = item._prevLru;
      CacheItem<V> nextLru = item._nextLru;

      item._prevLru = null;
      item._nextLru = null;

      int hitCount = item._hitCount;
      item._hitCount = -1;

      if (hitCount <= 0) {
        return; // item already removed
      }
      else if (hitCount == 1) {
        _size1--; 

        if (prevLru != null)
          prevLru._nextLru = nextLru;
        else {
          assert(_head1 == item);
          
          _head1 = nextLru;
        }

        if (nextLru != null)
          nextLru._prevLru = prevLru;
        else {
          assert(_tail1 == item);
          
          _tail1 = prevLru;
        }
      }
      else {
        _size2--; 

        if (prevLru != null)
          prevLru._nextLru = nextLru;
        else {
          assert(_head2 == item);
          
          _head2 = nextLru;
        }

        if (nextLru != null)
          nextLru._prevLru = prevLru;
        else {
          assert(_tail2 == item);
          
          _tail2 = prevLru;
        }
      }
    }
  }

  private static int hash(long key)
  {
    long hash = key;
    
    hash = 65537 * hash + (key >>> 8);
    hash = 65537 * hash + (key >>> 16);
    hash = 65537 * hash + (key >>> 32);
    hash = 65537 * hash + (key >>> 48);

    return (int) (hash & 0x7fffffff);
  }

  private Object getLock(int hash)
  {
    return _locks[hash >> 3];
  }

  /**
   * Returns the values in the cache
   */
  public Iterator<V> values()
  {
    ValueIterator iter = new ValueIterator<V>(this);
    iter.init(this);
    return iter;
  }

  public Iterator<V> values(Iterator<V> oldIter)
  {
    ValueIterator iter = (ValueIterator) oldIter;
    iter.init(this);
    return oldIter;
  }

  /**
   * Returns the hit count.
   */
  public long getHitCount()
  {
    return _hitCount;
  }

  /**
   * Returns the miss count.
   */
  public long getMissCount()
  {
    return _missCount;
  }

  /**
   * A cache item
   */
  static class CacheItem<V> {
    CacheItem<V> _nextHash;
    
    CacheItem<V> _prevLru;
    CacheItem<V> _nextLru;
    
    long _key;
    V _value;
    int _index;
    int _hitCount = 1;
    int _lruCounter;

    CacheItem(long key, V value)
    {
      _key = key;
      _value = value;
    }
  }

  /**
   * Iterator of cache values
   */
  static class ValueIterator<V> implements Iterator<V> {
    private LongKeyLruCache<V> _cache;
    private CacheItem<V> _entry;
    private int _i = -1;

    ValueIterator(LongKeyLruCache<V> cache)
    {
      init(cache);
    }

    void init(LongKeyLruCache<V> cache)
    {
      _cache = cache;
      _entry = null;
      _i = -1;
    }

    /**
     * Returns the next entry in the cache.
     */
    public boolean hasNext()
    {
      if (_entry != null)
        return true;
      
      CacheItem<V> []entries = _cache._entries;
      int length = entries.length;

      int i = _i + 1;
      for (; i < length; i++) {
        if (entries[i] != null) {
          _i = i - 1;

          return true;
        }
      }
      _i = i;
      
      return false;
    }

    /**
     * Returns the next value.
     */
    public V next()
    {
      CacheItem<V> entry = _entry;

      if (entry != null) {
        _entry = entry._nextHash;
        return entry._value;
      }
      
      CacheItem<V> []entries = _cache._entries;
      int length = entries.length;

      int i = _i + 1;
      for (; i < length; i++) {
        entry = entries[i];

        if (entry != null) {
          _entry = entry._nextHash;
          _i = i;
          
          return entry._value;
        }
      }
      _i = i;

      return null;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
