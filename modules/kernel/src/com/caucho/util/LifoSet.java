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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Stack (lifo) ordered set, used by the JCA code so recent
 * connections are used first.
 */
public class LifoSet<E> extends AbstractSet<E> {
  private final ArrayList<E> _list = new ArrayList<E>();

  /**
   * Returns the number of elements in the set.
   */
  public int size()
  {
    return _list.size();
  }

  /**
   * Returns true if empty.
   */
  public boolean isEmpty()
  {
    return _list.isEmpty();
  }

  /**
   * Adds an element.
   */
  public boolean add(E o)
  {
    if (! _list.contains(o)) {
      _list.add(0, o);
      return true;
    }
    else
      return false;
  }

  /**
   * Clears the set.
   */
  public void clear()
  {
    _list.clear();
  }

  /**
   * Returns true if the item is in the set.
   */
  public boolean contains(Object o)
  {
    return _list.contains(o);
  }

  /**
   * Returns true if the item is in the set.
   */
  public boolean containsAll(Collection<?> c)
  {
    return _list.containsAll(c);
  }

  /**
   * Returns an iterator to the set.
   */
  public Iterator<E> iterator()
  {
    return _list.iterator();
  }

  /**
   * Removes an element of the set.
   */
  public boolean remove(Object o)
  {
    return _list.remove(o);
  }

  /**
   * Removes an element of the set.
   */
  public boolean removeAll(Collection<?> c)
  {
    return _list.removeAll(c);
  }

  /**
   * Removes an element of the set.
   */
  public boolean retainAll(Collection<?> c)
  {
    return _list.retainAll(c);
  }

  /**
   * Returns an array of the elements in the set.
   */
  public Object []toArray()
  {
    return _list.toArray();
  }

  /**
   * Returns an array of the elements in the set.
   */
  public <T> T[] toArray(T[] a)
  {
    return _list.toArray(a);
  }

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    return _list.hashCode();
  }

  /**
   * Test for equality
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof LifoSet))
      return false;

    LifoSet set = (LifoSet) o;

    return _list.equals(set._list);
  }
}
