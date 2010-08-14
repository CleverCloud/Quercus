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

package com.caucho.db.table;

import com.caucho.db.index.BTree;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.xa.Transaction;
import com.caucho.inject.Module;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.L10N;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Validity constraints.
 */
@Module
public class UniqueIndexColumnConstraint extends Constraint {
  private final static L10N L = new L10N(UniqueIndexColumnConstraint.class);
  
  private final Column _column;

  /**
   * Creates a uniqueness constraint.
   */
  public UniqueIndexColumnConstraint(Column column)
  {
    _column = column;
  }

  /**
   * validate the constraint.
   */
  @Override
  public void validate(TableIterator []sourceRows,
                       QueryContext context, Transaction xa)
    throws SQLException
  {
    try {
      TableIterator sourceRow = sourceRows[0];

      byte []sourceBuffer = sourceRow.getBuffer();
      int sourceOffset = sourceRow.getRowOffset();
      
      byte []buffer = context.getBuffer();

      int length = _column.getLength();

      if (length <= 0)
        return;

      BTree index = _column.getIndex();

      long value = index.lookup(buffer, 0, length);

      if (value != 0) {
        Table table = sourceRow.getTable();
        long blockId = sourceRow.getBlockId();

        throw new SQLException(L.l("`{0}' in {1}.{2} fails uniqueness constraint.",
                                   _column.getString(blockId, sourceBuffer,
                                                     sourceOffset),
                                   table.getName(),
                                   _column.getName()));
      }
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
  }
}
