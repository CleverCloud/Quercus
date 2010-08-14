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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.sql;

/**
 * Represtents a prepared statement cache key.
 */
class PreparedStatementKey {
  private String _sql;
  private int _resultType;

  PreparedStatementKey()
  {
  }

  PreparedStatementKey(String sql)
  {
    init(sql);
  }

  PreparedStatementKey(String sql, int resultType)
  {
    _sql = sql;
    _resultType = resultType;
  }

  void init(String sql, int resultType)
  {
    _sql = sql;
    _resultType = resultType;
  }

  void init(String sql)
  {
    _sql = sql;
    _resultType = -1;
  }

  PreparedStatementKey copy()
  {
    return new PreparedStatementKey(_sql, _resultType);
  }

  public int hashCode()
  {
    int hash = _sql.hashCode();

    hash = 65521 * hash + _resultType;

    return hash;
  }

  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    PreparedStatementKey key = (PreparedStatementKey) o;

    if (! _sql.equals(key._sql))
      return false;
    else if (_resultType != key._resultType)
      return false;
    else
      return true;
  }
}
