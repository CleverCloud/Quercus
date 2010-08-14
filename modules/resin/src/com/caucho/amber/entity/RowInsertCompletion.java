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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.entity;

import com.caucho.amber.query.ResultSetCacheChunk;
import com.caucho.amber.type.EntityType;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.util.logging.Logger;

/**
 * Code to update the cache value on the completion of a transaction
 * after an INSERT statement.
 */
public class RowInsertCompletion implements AmberCompletion {
  private static final L10N L = new L10N(RowInsertCompletion.class);
  private static final Logger log = Log.open(RowInsertCompletion.class);

  private String _table;

  public RowInsertCompletion(String table)
  {
    if (table == null)
      throw new NullPointerException();

    _table = table;
  }

  /**
   * Code when the transaction completes.
   *
   * @return true if the entry should be deleted.
   */
  public boolean complete(EntityType rootType,
                          Object key,
                          EntityItem entityItem)
  {
    // jpa/0g0i
    return false;
  }

  /**
   * Code to invalidate the query.
   *
   * @return true if the entry should be deleted.
   */
  public boolean complete(ResultSetCacheChunk chunk)
  {
    return chunk.invalidate(_table, null);
  }

  /**
   * Returns true for equality.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (o == null || o.getClass() != getClass())
      return false;

    RowInsertCompletion comp = (RowInsertCompletion) o;

    return _table.equals(comp._table);
  }
}
