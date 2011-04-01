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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Fixed length cache with a LRU replacement policy.  If cache items
 * implement CacheListener, they will be informed when they're removed
 * from the cache.
 *
 * <p>Null keys are not allowed.  LruCache is synchronized.
 */
public class WeakLruCache<K,V> {
  // hash table containing the entries.  Its size is twice the capacity
  // so it will always remain at least half empty
  private CacheItem []_entries;
  // maximum allowed entries
  private int _capacity;
  // number of items in the cache
  private int _size;
  private int _mask;

  // head of the LRU list
  private CacheItem<K,V> _head;
  // tail of the LRU list
  private CacheItem<K,V> _tail;
  
  private static Integer NULL = new Integer(0);
  
  /**
   * Create the LRU cache with a specific capacity.
   *
   * @param initialCapacity minimum capacity of the cache
   */
  public WeakLruCache(int initialCapacity)
  {
    int capacity;

    for (capacity = 16; capacity < 2 * initialCapacity; capacity *= 2) {
    }

    _entries = new CacheItem[capacity];
    _mask = capacity - 1;

    _capacity = initialCapacity;
  }

  /**
   * Returns the current number of entries in the cache.
   */
  public int size()
  {
    return _size;
  }

  /**
   * Clears the cache
   */
  public void clear()
  {
    ArrayList<CacheListener> listeners = null;

    synchronized (this) {
      for (int i = 0; i < _entries.length; i++) {
        CacheItem<K,V> item = _entries[i];

        if (item != null) {
          V value = item.getValue();
          
          if (value instanceof CacheListener) {
            if (listeners == null)
              listeners = new ArrayList<CacheListener>();
            listeners.add((CacheListener) value);
          }
        }
        
        _entries[i] = null;
      }

      _size = 0;
      _head = null;
      _tail = null;
    }

    for (int i = listeners == null ? -1 : listeners.size() - 1; i >= 0; i--) {
      CacheListener listener = listeners.get(i);
      listener.removeEvent();
    }
  }

  /**
   * Get an item from the cache and make it most recently used.
   *
   * @param key key to lookup the item
   * @return the matching object in the cache
   */
  public V get(K key)
  {
    Object okey = key;
    if (okey == null)
      okey = NULL;
    
    int hash = okey.hashCode() & _mask;
    int count = _size + 1;

    synchronized (this) {
      for (; count > 0; count--) {
        CacheItem<K,V> item = _entries[hash];

        if (item == null)
          return null;

        if (item._key == key || item._key.equals(key)) {
          updateLru(item);

          return item.getValue();
        }

        hash = (hash + 1) & _mask;
      }
    }

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
  public V put(K key, V value)
  {
    Object okey = key;
    
    if (okey == null)
      okey = NULL;

    // remove LRU items until we're below capacity
    while (_capacity <= _size) {
      remove(_tail._key);
    }

    V oldValue = null;

    int hash = key.hashCode() & _mask;
    int count = _size + 1;

    synchronized (this) {
      for (; count > 0; count--) {
        CacheItem<K,V> item = _entries[hash];

        // No matching item, so create one
        if (item == null) {
          item = new CacheItem<K,V>(key, value);
          _entries[hash] = item;
          _size++;
          item._next = _head;
          if (_head != null)
            _head._prev = item;
          else
            _tail = item;
          _head = item;

          return null;
        }

        // matching item gets replaced
        if (item._key == okey || item._key.equals(okey)) {
          updateLru(item);

          oldValue = item.getValue();
          item.setValue(value);
          break;
        }

        hash = (hash + 1) & _mask;
      }
    }
    
    if (oldValue instanceof CacheListener && oldValue != value)
      ((CacheListener) oldValue).removeEvent();

    return oldValue;
  }

  /**
   * Put item at the head of the lru list.  This is always called while
   * synchronized.
   */
  private void updateLru(CacheItem<K,V> item)
  {
    CacheItem<K,V> prev = item._prev;
    CacheItem<K,V> next = item._next;

    if (prev != null) {
      prev._next = next;

      item._prev = null;
      item._next = _head;
      _head._prev = item;
      _head = item;

      if (next != null)
        next._prev = prev;
      else
        _tail = prev;
    }
  }

  /**
   * Remove the last item in the LRU
   */
  public boolean removeTail()
  {
    CacheItem<K,V> last = _tail;

    if (last == null)
      return false;
    else {
      remove(last._key);
      return true;
    }
  }

  /**
   * Removes an item from the cache
   *
   * @param key the key to remove
   *
   * @return the value removed
   */
  public V remove(K key)
  {
    Object okey = key;
    if (okey == null)
      okey = NULL;
    
    int hash = key.hashCode() & _mask;
    int count = _size + 1;

    V value = null;

    synchronized (this) {
      for (; count > 0; count--) {
        CacheItem<K,V> item = _entries[hash];

        if (item == null)
          return null;

        if (item._key == okey || item._key.equals(okey)) {
          _entries[hash] = null;
          _size--;

          CacheItem<K,V> prev = item._prev;
          CacheItem<K,V> next = item._next;

          if (prev != null)
            prev._next = next;
          else
            _head = next;

          if (next != null)
            next._prev = prev;
          else
            _tail = prev;

          // Shift colliding entries down
          for (int i = 1; i <= count; i++) {
            int nextHash = (hash + i) & _mask;
            CacheItem<K,V> nextItem = _entries[nextHash];
            if (nextItem == null)
              break;

            _entries[nextHash] = null;
            refillEntry(nextItem);
          }

          value = item.getValue();
          break;
        }

        hash = (hash + 1) & _mask;
      }
    }

    if (count < 0)
      throw new RuntimeException("internal cache error");

    if (value instanceof CacheListener)
      ((CacheListener) value).removeEvent();

    return value;
  }

  /**
   * Put the item in the best location available in the hash table.
   */
  private void refillEntry(CacheItem<K,V> item)
  {
    int baseHash = item._key.hashCode();

    for (int count = 0; count < _size + 1; count++) {
      int hash = (baseHash + count) & _mask;

      if (_entries[hash] == null) {
        _entries[hash] = item;
        return;
      }
    }
  }

  /**
   * Returns the keys stored in the cache
   */
  public Iterator<K> keys()
  {
    KeyIterator<K,V> iter = new KeyIterator<K,V>();
    iter.init(this);
    return iter;
  }

  /**
   * Returns keys stored in the cache using an old iterator
   */
  public Iterator<K> keys(Iterator<K> oldIter)
  {
    KeyIterator iter = (KeyIterator) oldIter;
    iter.init(this);
    return oldIter;
  }

  /**
   * Returns the values in the cache
   */
  public Iterator<V> values()
  {
    ValueIterator<K,V> iter = new ValueIterator<K,V>();
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
   * A cache item
   */
  static class CacheItem<K,V> {
    CacheItem<K,V> _prev;
    CacheItem<K,V> _next;
    K _key;
    private WeakReference<V> _value;
    int _index;

    CacheItem(K key, V value)
    {
      _key = key;
      
      if (value == null)
        _value = null;
      else
        _value = new WeakReference<V>(value);
    }

    public final V getValue()
    {
      WeakReference<V> ref = _value;

      if (ref == null)
        return null;
      else
        return ref.get();
    }

    public final void setValue(V value)
    {
      if (value == null)
        _value = null;
      else
        _value = new WeakReference<V>(value);
    }
  }

  /**
   * Iterator of cache keys
   */
  static class KeyIterator<K,V> implements Iterator<K> {
    CacheItem<K,V> _item;

    void init(WeakLruCache<K,V> cache)
    {
      _item = cache._head;
    }

    public boolean hasNext()
    {
      return _item != null;
    }

    public K next()
    {
      K key = _item._key;

      _item = _item._next;

      return key;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Iterator of cache values
   */
  static class ValueIterator<K,V> implements Iterator<V> {
    CacheItem<K,V> _item;

    void init(WeakLruCache<K,V> cache)
    {
      _item = cache._head;
    }

    public boolean hasNext()
    {
      return _item != null;
    }

    public V next()
    {
      V value = _item.getValue();

      _item = _item._next;

      return value;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
