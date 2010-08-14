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

package com.caucho.quercus.env;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a PHP chained map
 */
public class ChainedMap<K,V> implements VarMap<K,V> {
  private final VarMap<K,V> _next;

  private final Map<K,V> _map;

  public ChainedMap(VarMap<K,V> next, Map<K,V> map)
  {
    _next = next;
    _map = map;
  }

  public ChainedMap(VarMap<K,V> next)
  {
    _next = next;
    _map = new HashMap<K,V>();
  }

  /**
   * Sets the value
   */
  public void put(K key, V value)
  {
    _map.put(key, value);
  }

  /**
   * Remove the value
   */
  public V remove(K key)
  {
    return _map.remove(key);
  }

  /**
   * Gets a value.
   */
  public V get(K key)
  {
    V value = _map.get(key);

    if (value != null)
      return value;
    else if (_next != null)
      return _next.get(key);
    else
      return null;
  }
}
