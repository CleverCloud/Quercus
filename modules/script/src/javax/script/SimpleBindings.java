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

package javax.script;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implements the namespace
 */
public class SimpleBindings implements Bindings {
  private HashMap _map = new HashMap();

  /**
   * Creates the simple namespace.
   */
  public SimpleBindings()
  {
  }

  /**
   * Creates the simple namespace.
   */
  public SimpleBindings(Map map)
  {
    _map.putAll(map);
  }

  /**
   * Clears the map.
   */
  public void clear()
  {
    _map.clear();
  }

  public boolean containsKey(Object key)
  {
    return _map.containsKey(key);
  }

  public boolean containsValue(Object value)
  {
    return _map.containsValue(value);
  }

  public Set entrySet()
  {
    return _map.entrySet();
  }

  public Object get(Object key)
  {
    return _map.get(key);
  }

  public boolean isEmpty()
  {
    return _map.isEmpty();
  }

  public Set keySet()
  {
    return _map.keySet();
  }

  public Object put(String name, Object value)
  {
    return _map.put(name, value);
  }

  public void putAll(Map toMerge)
  {
    _map.putAll(toMerge);
  }

  public Object remove(Object key)
  {
    return _map.remove(key);
  }

  public int size()
  {
    return _map.size();
  }

  public Collection values()
  {
    return _map.values();
  }
}

