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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

import java.util.Iterator;
/**
 * The IntMap provides a simple hashmap from keys to integers.  The API is
 * an abbreviation of the HashMap collection API.
 *
 * <p>The convenience of IntMap is avoiding all the silly wrapping of
 * integers.
 */
public class IdentityIntMap {
  /**
   * Encoding of a null entry.  Since NULL is equal to Integer.MIN_VALUE, 
   * it's impossible to distinguish between the two.
   */
  public final static int NULL = Integer.MIN_VALUE;
  private static int DELETED = 0x1;
  private Object []keys;
  private int nullValue;
  private int []values;
  private byte []flags;
  private int size;
  private int mask;

  /**
   * Create a new IntMap.  Default size is 16.
   */
  public IdentityIntMap()
  {
    keys = new Object[16];
    values = new int[16];
    flags = new byte[16];

    mask = keys.length - 1;
    size = 0;

    nullValue = NULL;
  }

  /**
   * Clear the hashmap.
   */
  public void clear()
  {
    nullValue = NULL;
    for (int i = 0; i < values.length; i++) {
      keys[i] = null;
      flags[i] = 0;
      values[i] = 0;
    }
    size = 0;
  }
  /**
   * Returns the current number of entries in the map.
   */
  public int size() 
  { 
    return size;
  }

  /**
   * Puts a new value in the property table with the appropriate flags
   */
  public int get(Object key)
  {
    if (key == null)
      return nullValue;

    int hash = System.identityHashCode(key) & mask;

    while (true) {
      Object mapKey = keys[hash];

      if (mapKey == key)
        return values[hash];
      else if (mapKey == null) {
        if ((flags[hash] & DELETED) == 0)
          return NULL;
      }
      else if (mapKey.equals(key))
        return values[hash];

      hash = (hash + 1) & mask;
    }
  }

  /**
   * Expands the property table
   */
  private void resize(int newSize)
  {
    Object []newKeys = new Object[newSize];
    int []newValues = new int[newSize];
    byte []newFlags = new byte[newSize];

    mask = newKeys.length - 1;

    for (int i = 0; i < keys.length; i++) {
      if (keys[i] == null || (flags[i] & DELETED) != 0)
        continue;

      int hash = System.identityHashCode(keys[i]) & mask;

      while (true) {
        if (newKeys[hash] == null) {
          newKeys[hash] = keys[i];
          newValues[hash] = values[i];
          newFlags[hash] = flags[i];
          break;
        }
        hash = (hash + 1) & mask;
      }
    }

    keys = newKeys;
    values = newValues;
    flags = newFlags;
  }

  /**
   * Puts a new value in the property table with the appropriate flags
   */
  public int put(Object key, int value)
  {
    if (key == null) {
      int old = nullValue;
      nullValue = value;
      return old;
    }

    int hash = System.identityHashCode(key) & mask;

    while (true) {
      Object testKey = keys[hash];

      if (testKey == null || (flags[hash] & DELETED) != 0) {
        keys[hash] = key;
        values[hash] = value;
        flags[hash] = 0;

        size++;

        if (keys.length <= 2 * size)
          resize(2 * keys.length);

        return NULL;
      }
      else if (key != testKey && ! testKey.equals(key)) {
        hash = (hash + 1) & mask;
        continue;
      }
      else {
        int old = values[hash];

        values[hash] = value;

        return old;
      }
    }
  }

  /**
   * Deletes the entry.  Returns true if successful.
   */
  public int remove(Object key)
  {
    if (key == null) {
      int old = nullValue;
      nullValue = NULL;
      return old;
    }

    int hash = System.identityHashCode(key) & mask;

    while (true) {
      Object mapKey = keys[hash];

      if (mapKey == null)
        return NULL;
      else if (mapKey.equals(key)) {
        flags[hash] |= DELETED;

        size--;

        keys[hash] = null;

        return values[hash];
      }

      hash = (hash + 1) & mask;
    }
  }
  /**
   * Returns an iterator of the keys.
   */
  public Iterator iterator()
  {
    return new IntMapIterator();
  }

  public String toString()
  {
    StringBuffer sbuf = new StringBuffer();

    sbuf.append("IntMap[");
    boolean isFirst = true;
    for (int i = 0; i <= mask; i++) {
      if ((flags[i] & DELETED) == 0 && keys[i] != null) {
        if (! isFirst)
          sbuf.append(", ");
        isFirst = false;
        sbuf.append(keys[i]);
        sbuf.append(":");
        sbuf.append(values[i]);
      }
    }
    sbuf.append("]");
    return sbuf.toString();
  }

  class IntMapIterator implements Iterator {
    int index;

    public boolean hasNext()
    {
      for (; index < keys.length; index++)
        if (keys[index] != null && (flags[index] & DELETED) == 0)
          return true;

      return false;
    }

    public Object next()
    {
      for (; index < keys.length; index++)
        if (keys[index] != null && (flags[index] & DELETED) == 0)
          return keys[index++];

      return null;
    }

    public void remove()
    {
      throw new RuntimeException();
    }
  }
}
