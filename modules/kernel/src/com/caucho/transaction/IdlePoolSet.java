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

package com.caucho.transaction;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

import javax.resource.spi.ManagedConnection;

/**
 * Queue (fifo) ordered set, used by the JCA code so connections can be
 * load balanced using a round-robin.
 */
public class IdlePoolSet extends AbstractSet<ManagedConnection> {
  private final int _capacity;

  private final ManagedConnection []_entries;
  private final int _entriesLength;
    
  private int _head;
  private int _tail;

  IdlePoolSet(int capacity)
  {
    _capacity = capacity;
    _entries = new ManagedConnection[2 * capacity];
    _entriesLength = _entries.length;
  }

  /**
   * Returns the number of elements in the set.
   */
  public int size()
  {
    return (_head - _tail + _entriesLength) % _entriesLength;
  }

  /**
   * Returns true if empty.
   */
  public boolean isEmpty()
  {
    return _head == _tail;
  }

  /**
   * Peeks the first item.
   */
  public ManagedConnection first()
  {
    if (_head != _tail)
      return _entries[_tail];
    else
      return null;
  }

  /**
   * Adds an element.
   */
  public boolean add(ManagedConnection o)
  {
    synchronized (this) {
      if (_capacity <= size())
        return false;

      for (int i = _tail; i != _head; i = (i + 1) % _entriesLength) {
        if (_entries[i] == o)
          return false;
      }

      _entries[_head] = o;
      _head = (_head + 1) % _entriesLength;
    }

    return true;
  }

  /**
   * Clears the set.
   */
  public void clear()
  {
    _head = _tail = 0;

    for (int i = 0; i < _entriesLength; i++)
      _entries[i] = null;
  }

  /**
   * Returns true if the item is in the set.
   */
  public boolean contains(Object o)
  {
    for (int i = _tail; i != _head; i = (i + 1) % _entriesLength) {
      if (_entries[i] == o)
        return true;
    }

    return false;
  }

  /**
   * Returns true if the item is in the set.
   */
  public boolean containsAll(Collection<?> c)
  {
    Iterator iter = c.iterator();

    while (iter.hasNext()) {
      if (! contains(iter.next()))
        return false;
    }

    return true;
  }

  /**
   * Returns an iterator to the set.
   */
  public Iterator<ManagedConnection> iterator()
  {
    return new IdlePoolIterator();
  }

  /**
   * Removes an element of the set.
   */
  public boolean remove(Object o)
  {
    for (int i = _tail; i != _head; i = (i + 1) % _entriesLength) {
      if (_entries[i] == o) {
        return removeEntry(i);
      }
    }
    
    return false;
  }

  /**
   * Removes an element of the set.
   */
  public boolean removeAll(Collection<?> c)
  {
    Iterator iter = c.iterator();
    boolean result = true;

    while (iter.hasNext()) {
      if (! remove(iter.next()))
        result = false;
    }

    return result;
  }

  /**
   * Removes an element of the set.
   */
  public boolean retainAll(Collection<?> c)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns an array of the elements in the set.
   */
  public Object []toArray()
  {
    Object []values = new Object[size()];
    int j = 0;

    for (int i = _tail; i != _head; i = (i + 1) % _entriesLength) {
      values[j++] = _entries[i];
    }
    
    return values;
  }

  /**
   * Returns an array of the elements in the set.
   */
  public ManagedConnection[] toArray(ManagedConnection[] values)
  {
    int j = 0;

    for (int i = _tail; i != _head; i = (i + 1) % _entriesLength) {
      values[j++] = _entries[i];
    }

    return values;
  }

  boolean removeEntry(int i)
  {
    if (i == _head)
      return false;

    if (i == _tail) {
      _entries[_tail] = null;
    }
    else if (_tail < i) {
      // ... _tail xxx i xxx _head ...
      
      System.arraycopy(_entries, _tail, _entries, _tail + 1, i - _tail);
    }
    else {
      // xxx i xxx _head ... _tail xxx
      
      if (i > 0)
        System.arraycopy(_entries, 0, _entries, 1, i);

      _entries[0] = _entries[_entriesLength - 1];
      
      System.arraycopy(_entries, _tail, _entries, _tail + 1,
                       _entriesLength - _tail - 1);
    }
    
    _entries[_tail] = null;
    _tail = (_tail + 1) % _entriesLength;

    return true;
  }

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    return System.identityHashCode(_entries);
  }

  /**
   * Test for equality
   */
  public boolean equals(Object o)
  {
    return this == o;
  }

  class IdlePoolIterator implements Iterator<ManagedConnection> {
    private int _head;
    private int _tail;
    private int _i;

    IdlePoolIterator()
    {
      _head = IdlePoolSet.this._head;
      _tail = IdlePoolSet.this._tail;
      _i = _tail;
    }

    public boolean hasNext()
    {
      return _i != _head;
    }

    public ManagedConnection next()
    {
      if (_i == _head)
        return null;

      ManagedConnection value = _entries[_i];

      _i = (_i + 1) % _entriesLength;

      return value;
    }

    public void remove()
    {
      int i = (_i + _entriesLength - 1) % _entriesLength;
      
      removeEntry(i);

      _tail = IdlePoolSet.this._tail;
    }
  }
}
