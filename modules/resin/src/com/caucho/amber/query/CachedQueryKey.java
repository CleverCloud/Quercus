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

package com.caucho.amber.query;

/**
 * A key for cached query results.
 */
public class CachedQueryKey {
  private String _sql;
  private Object []_parameters;
  private int _parameterCount;

  public CachedQueryKey()
  {
  }

  public CachedQueryKey(String sql, Object []parameters, int count)
  {
    _sql = sql;
    _parameterCount = count;

    if (count > 0) {
      _parameters = new Object[count];
      
      for (int i = 0; i < count; i++) {
        _parameters[i] = parameters[i];
      }
    }
  }

  void init(String sql, Object []parameters, int count)
  {
    _sql = sql;
    _parameters = parameters;
    _parameterCount = count;
  }

  /**
   * Returns the SQL
   */
  public String getSQL()
  {
    return _sql;
  }

  /**
   * Returns the hash-code for the key.
   */
  public int hashCode()
  {
    int hash = _sql.hashCode();

    for (int i = _parameterCount - 1; i >= 0; i--) {
      Object o = _parameters[i];

      if (o != null)
        hash = 65521 * hash + o.hashCode();
      else
        hash = 65521 * hash;
    }

    return hash;
  }

  /**
   * Returns true if the key matches.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof CachedQueryKey))
      return false;

    CachedQueryKey key = (CachedQueryKey) o;

    if (! _sql.equals(key._sql))
      return false;
    if (_parameterCount != key._parameterCount)
      return false;

    for (int i = _parameterCount - 1; i >= 0; i--) {
      Object paramA = _parameters[i];
      Object paramB = key._parameters[i];
      
      if (paramA != paramB && (paramA == null || ! paramA.equals(paramB)))
        return false;
    }

    return true;
  }
}
