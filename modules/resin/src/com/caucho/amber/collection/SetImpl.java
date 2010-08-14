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

package com.caucho.amber.collection;

import com.caucho.amber.AmberQuery;
import com.caucho.amber.AmberRuntimeException;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.query.UserQuery;
import com.caucho.util.Alarm;

import java.sql.SQLException;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Represents a lazy collection.
 */
public class SetImpl<E> extends AbstractSet<E>
  implements AmberCollection {
  private AmberQuery _query;

  private AmberConnection _aConn;

  private ArrayList<E> _values = new ArrayList<E>();
  private long _expireTime;

  public SetImpl(AmberConnection aConn, String query)
  {
    _aConn = aConn;

    if (_query != null) {
      try {
        _query = _aConn.prepareQuery(query);
      } catch (SQLException e) {
        throw new AmberRuntimeException(e);
      }
    }
  }

  public SetImpl(AmberQuery query)
  {
    _query = query;

    if (query != null)
      setSession(((UserQuery) _query).getConnection());
  }

  /**
   * Sets the session.
   */
  public void setSession(com.caucho.amber.AmberConnection aConn)
  {
    throw new UnsupportedOperationException();
    // setSession(((UserAmberConnection) aConn).getManagedConnection());
  }

  /**
   * Sets the session.
   */
  public void setSession(AmberConnection aConn)
  {
    _aConn = aConn;

    // jpa/0j62
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
   * Returns an iterator of the items.
   */
  public Iterator iterator()
  {
    fill();

    return new ValuesIterator(_values);
  }

  /**
   * Returns an iterator of the items.
   */
  public E get(int index)
  {
    fill();

    return _values.get(index);
  }

  /**
   * Adds all items to this collection.
   */
  public boolean addAll(Collection<? extends E> collection)
  {
    fill();

    return _values.addAll(collection);
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

  protected boolean isValid()
  {
    return Alarm.getCurrentTime() <= _expireTime;
  }

  private void fill()
  {
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
      _query.list((ArrayList) _values);
    } catch (SQLException e) {
      throw new AmberRuntimeException(e);
    }
  }

  class ValuesIterator implements Iterator {
    private ArrayList<E> _values;
    private int i = 0;

    ValuesIterator(ArrayList<E> values)
    {
      _values = new ArrayList<E>(values);
    }

    public boolean hasNext()
    {
      return i < _values.size();
    }

    public Object next()
    {
      if (i < _values.size())
        return _values.get(i++);
      else
        return null;
    }

    public void remove()
    {
      SetImpl.this.remove(_values.get(i - 1));
    }
  }
}
