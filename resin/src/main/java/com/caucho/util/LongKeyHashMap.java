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

import java.util.Iterator;

/**
 * Cache with a clock replacement policy.
 *
 * <p>Null keys are not allowed.  LruCache is synchronized.
 */
public class LongKeyHashMap<E> {
  // array containing the keys
  private long []_keys;
  
  // array containing the values
  private E []_values;
  
  // number of items in the cache
  private int _size;
  private int _mask;
  
  /**
   * Create the clock cache with a specific capacity.
   *
   * @param initialCapacity minimum capacity of the cache
   */
  public LongKeyHashMap()
  {
    this(8);
  }
  
  /**
   * Create the clock cache with a specific capacity.
   *
   * @param initialCapacity minimum capacity of the cache
   */
  public LongKeyHashMap(int initialCapacity)
  {
    int capacity;

    for (capacity = 8; capacity < 2 * initialCapacity; capacity *= 2) {
    }

    _keys = new long[capacity];
    _values = (E []) new Object[capacity];
    _mask = capacity - 1;
  }

  /**
   * Returns the current number of entries in the cache.
   */
  public int size()
  {
    return _size;
  }

  /**
   * Clears the map
   */
  public void clear()
  {
    synchronized (this) {
      for (int i = 0; i < _values.length; i++) {
        _values[i] = null;
      }

      _size = 0;
    }
  }

  /**
   * Get an item from the cache and make it most recently used.
   *
   * @param key key to lookup the item
   * @return the matching object in the cache
   */
  public E get(long key)
  {
    int hash = getHash(key);
    int count = _size + 1;

    synchronized (this) {
      for (; count > 0; count--) {
        E item = _values[hash];

        if (item == null)
          return null;

        if (_keys[hash] == key)
          return item;

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
  public E put(long key, E value)
  {
    E item = putImpl(key, value);

    // forced resizing if 1/2 full
    if (_values.length <= 2 * _size) {
      synchronized (this) {
        long []oldKeys = _keys;
        E []oldValues = _values;

        _keys = new long[2 * oldKeys.length];
        _values = (E []) new Object[2 * oldValues.length];

        _mask = _values.length - 1;

        for (int i = oldValues.length - 1; i >= 0; i--) {
          long oldKey = oldKeys[i];
          E oldValue = oldValues[i];

          if (oldValue != null)
            putImpl(oldKey, oldValue);
        }
      }
    }

    return item;
  }

  /**
   * Implementation of the put.
   */
  private E putImpl(long key, E value)
  {
    E item = null;

    int hash = getHash(key);
    int count = _size + 1;

    synchronized (this) {
      for (; count > 0; count--) {
        item = _values[hash];

        // No matching item, so create one
        if (item == null) {
          _keys[hash] = key;
          _values[hash] = value;
          _size++;

          return null;
        }

        // matching item gets replaced
        if (_keys[hash] == key) {
          _values[hash] = value;

          return item;
        }

        hash = (hash + 1) & _mask;
      }
    }

    throw new IllegalStateException();
  }

  /**
   * Removes an item from the map
   *
   * @param key the key to remove
   *
   * @return the value removed
   */
  public E remove(long key)
  {
    int hash = getHash(key);
    int count = _size + 1;

    E item = null;

    synchronized (this) {
      for (; count > 0; count--) {
        item = _values[hash];

        if (item == null)
          return null;

        if (_keys[hash] == key) {
          _values[hash] = null;
          _size--;

          refillEntries(hash);
          break;
        }

        hash = (hash + 1) & _mask;
      }
    }

    if (count < 0)
      throw new RuntimeException("internal cache error");

    return item;
  }

  /**
   * Put the item in the best location available in the hash table.
   */
  private void refillEntries(int hash)
  {
    for (int count = _size; count >= 0; count--) {
      hash = (hash + 1) & _mask;

      if (_values[hash] == null)
        return;

      _values[hash] = null;
      refillEntry(hash);
    }
  }
  
  /**
   * Put the item in the best location available in the hash table.
   */
  private void refillEntry(int baseHash)
  {
    long key = _keys[baseHash];
    E value = _values[baseHash];
    
    int hash = getHash(key);
    
    for (int count = _size; count >= 0; count--) {
      if (_values[hash] == null) {
        _keys[hash] = key;
        _values[hash] = value;
        return;
      }

      hash = (hash + 1) & _mask;
    }
  }

  /**
   * Returns the key's hash
   */
  private int getHash(long key)
  {
    long hash = key;
    hash = hash * 0x5deece66dl + 0xbl + (hash >>> 32) * 137;

    return (int) (hash) & _mask;
  }

  public Iterator<E> valueIterator()
  {
    return new ValueIterator();
  }

  class ValueIterator implements Iterator<E> {
    private E []_values;
    private int _index;
    private E _value;

    ValueIterator()
    {
      _values = LongKeyHashMap.this._values;
      _index = -1;
      findNext();
    }

    public boolean hasNext()
    {
      return _value != null;
    }

    public E next()
    {
      E value = _value;
      findNext();
      return value;
    }

    public void remove()
    {
    }

    private void findNext()
    {
      _value = null;

      for (_index++; _index < _values.length; _index++) {
        _value = _values[_index];
        if (_value != null)
          return;
      }
    }
  }
}
