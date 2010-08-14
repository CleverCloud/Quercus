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

package com.caucho.amber.query;

import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.AmberType;
import com.caucho.util.Alarm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the application's view of the query.
 */
public class CachedQuery {
  private SelectQuery _query;
  private CachedQueryKey _key;
  private ResultSetImpl _rs;

  private AmberType []_argTypes;
  private Object []_argValues;
  private int _argLength = 0;

  private long _loadTime;

  private ArrayList<Object> _values = new ArrayList<Object>();

  private volatile boolean _isLoading;
  private volatile boolean _isValidLoad;

  CachedQuery(UserQuery query)
  {
    _query = (SelectQuery) query.getQuery();

    AmberType []argTypes = query.getArgTypes();
    Object []argValues = query.getArgValues();
    
    _argLength = query.getArgLength();

    if (_argLength > 0) {
      _argTypes = new AmberType[_argLength];
      _argValues = new Object[_argLength];

      for (int i = _argLength - 1; i >= 0; i--) {
        _argTypes[i] = argTypes[i];
        _argValues[i] = argValues[i];
      }
    }

    _key = new CachedQueryKey();
    _key.init(_query.getQueryString(), _argValues, _argLength);

    _query.registerUpdates(this);
  }

  /**
   * returns the key.
   */
  public CachedQueryKey getKey()
  {
    return _key;
  }

  /**
   * Updates the query.
   */
  public void update()
  {
    synchronized (this) {
      _loadTime = 0;
      _isValidLoad = false;
    }
  }

  /**
   * Executes the query, filling the list.
   */
  public void list(List<Object> list, AmberConnection aConn, long maxAge)
    throws SQLException
  {
    AmberType type = _query.getResultType(0);
    EntityType entityType = (EntityType) type;
    Class cl = entityType.getBeanClass();
    
    synchronized (this) {
      long now = Alarm.getCurrentTime();

      if (now < _loadTime + maxAge || _isLoading && _loadTime > 0) {
        int length = _values.size();

        for (int i = 0; i < length; i++) {
          Object key = _values.get(i);

          list.add(aConn.loadLazy(cl.getName(), entityType.getName(), (java.io.Serializable) key));
        }
        return;
      }

      _isLoading = true;
      _isValidLoad = true;
    }

    try {
      ArrayList<Object> values = new ArrayList<Object>();

      ResultSetImpl rs = executeQuery(aConn);

      while (rs.next()) {
        values.add(rs.getKey(1));

        list.add(rs.getObject(1));
      }

      rs.close();
      
      synchronized (this) {
        if (_isValidLoad) {
          _values = values;

          _loadTime = Alarm.getCurrentTime();
        }
      }
    } finally {
      _isLoading = false;
    }
  }

  /**
   * Executes the query returning a result set.
   */
  private ResultSetImpl executeQuery(AmberConnection aConn)
    throws SQLException
  {
    if (_rs == null)
      _rs = new ResultSetImpl();

    PreparedStatement pstmt;
    pstmt = aConn.prepareStatement(_query.getSQL());

    pstmt.clearParameters();
    
    for (int i = 0; i < _argLength; i++) {
      if (_argValues[i] != null)
        _argTypes[i].setParameter(pstmt, i + 1, _argValues[i]);
    }

    ResultSet rs = pstmt.executeQuery();

    _rs.setResultSet(rs);
    _rs.setQuery((SelectQuery) _query);
    _rs.setSession(aConn);
    _rs.init();
    
    return _rs;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _query.getQueryString() + "]";
  }
}
