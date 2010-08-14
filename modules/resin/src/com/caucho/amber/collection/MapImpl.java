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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.collection;

import com.caucho.amber.AmberQuery;
import com.caucho.amber.AmberRuntimeException;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.query.UserQuery;
import com.caucho.util.Alarm;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a lazy collection.
 */
public class MapImpl<K, V> extends AbstractMap<K, V>
  implements AmberCollection {
  private AmberQuery _query;

  private AmberConnection _aConn;

  private HashMap<K, V> _values = new HashMap<K, V>();
  private long _expireTime;

  private Method _methodGetMapKey;

  public MapImpl(AmberConnection aConn,
                 String query,
                 Method methodGetMapKey)
  {
    _aConn = aConn;
    _methodGetMapKey = methodGetMapKey;

    if (query != null) {
      try {
        _query = _aConn.prepareQuery(query);
      } catch (SQLException e) {
        throw new AmberRuntimeException(e);
      }
    }
  }

  public MapImpl(AmberQuery query,
                 Method methodGetMapKey)
  {
    _query = query;
    _methodGetMapKey = methodGetMapKey;

    // jpa/0v00
    if (_query != null)
      setSession(((UserQuery) _query).getConnection());
  }

  /**
   * Sets the session.
   */
  public void setSession(AmberConnection aConn)
  {
    _aConn = aConn;

    // jpa/0v00
    if (aConn != null)
      _aConn.register(this);
  }

  /**
   * Returns the session.
   */
  public AmberConnection getSession()
  {
    return _aConn;
  }

  /**
   * Returns the query.
   */
  public AmberQuery getQuery()
  {
    return _query;
  }

  /**
   * Returns the number of items in the collection.
   */
  public int size()
  {
    fill();

    return _values.size();
  }

  /**
   * Returns a set view of the mappings contained in this map.
   */
  public Set<Map.Entry<K, V>> entrySet()
  {
    fill();

    return _values.entrySet();
  }

  /**
   * Returns an iterator of the items.
   */
  public V get(Object key)
  {
    fill();

    return _values.get(key);
  }

  /**
   * Returns a set view of the keys contained in this map.
   */
  public Set<K> keySet()
  {
    fill();

    return _values.keySet();
  }

  /**
   * Clears the collection.
   */
  public void clear()
  {
    _values.clear();
    _expireTime = Alarm.getCurrentTime();
  }

  /**
   * Updates the collection.
   */
  public void update()
  {
    _expireTime = 0;
  }

  /**
   * Detaches the collection.
   */
  public void detach()
  {
    _aConn = null;
    _query = null;
  }

  /**
   * Adds an item to the collection.
   */
  public void putAll(Map<? extends K, ? extends V> map)
  {
    // jpa/0v04

    fill();

    _values.putAll(map);
  }

  protected boolean isValid()
  {
    return Alarm.getCurrentTime() <= _expireTime;
  }

  private void fill()
  {
    // jpa/0v04
    if (_query == null)
      return;

    // If it is detached should not be updated.
    if (_aConn == null)
      return;

    if (Alarm.getCurrentTime() <= _expireTime)
      return;

    try {
      _expireTime = Alarm.getCurrentTime();

      ((UserQuery) _query).setSession(_aConn);
      _values.clear();
      _query.list((HashMap) _values, _methodGetMapKey);
    } catch (Exception e) {
      throw new AmberRuntimeException(e);
    }
  }
}
