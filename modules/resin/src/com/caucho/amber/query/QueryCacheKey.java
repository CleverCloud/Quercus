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

import com.caucho.util.CharBuffer;

/**
 * The key for a query cache entry
 */
public class QueryCacheKey {
  private String _sql;
  private Object []_args;
  private int _startRow;

  public QueryCacheKey()
  {
  }

  public QueryCacheKey(String sql, Object []args, int startRow)
  {
    init(sql, args, startRow);
  }

  /**
   * Initialize the cache key values.
   */
  public void init(String sql, Object []args, int startRow)
  {
    _sql = sql;
    _args = args;
    _startRow = startRow;
  }

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    int hash = _startRow;

    hash = 65537 * hash + _sql.hashCode();

    Object []args = _args;
    for (int i = args.length - 1; i >= 0; i--) {
      Object v = args[i];

      if (v == null)
        hash = 65537 * hash + 17;
      else
        hash = 65537 * hash + 17 * v.hashCode();
    }

    return hash;
  }

  /**
   * Test for equality.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (o == null || o.getClass() != getClass())
      return false;

    QueryCacheKey key = (QueryCacheKey) o;

    if (! _sql.equals(key._sql)) {
      return false;
    }
  
    if (_startRow != key._startRow) {
      return false;
    }

    Object []argsA = _args;
    Object []argsB = key._args;

    if (argsA.length != argsB.length) {
      return false;
    }

    for (int i = argsA.length - 1; i >= 0; i--) {
      Object a = argsA[i];
      Object b = argsB[i];

      if (a != b && (a == null || ! a.equals(b))) {
        return false;
      }
    }

    return true;
  }

  public String toString()
  {
    CharBuffer cb = new CharBuffer();

    cb.append("QueryCacheKey[");
    cb.append(_sql);

    for (int i = 0; i < _args.length; i++) {
      cb.append(",");
      cb.append(_args[i]);
    }

    cb.append("]");

    return cb.toString();
  }
}
