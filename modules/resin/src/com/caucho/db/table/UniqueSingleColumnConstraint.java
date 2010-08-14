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

import com.caucho.db.block.BlockStore;
import com.caucho.db.index.BTree;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.xa.Transaction;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.L10N;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Validity constraints.
 */
public class UniqueSingleColumnConstraint extends Constraint {
  private final static L10N L = new L10N(UniqueSingleColumnConstraint.class);
  private final Column _uniqueColumn;

  /**
   * Creates a uniqueness constraint.
   */
  public UniqueSingleColumnConstraint(Column column)
  {
    _uniqueColumn = column;
  }

  /**
   * validate the constraint.
   */
  @Override
  public void validate(TableIterator []sourceRows,
                       QueryContext queryContext, Transaction xa)
    throws SQLException
  {
    Column column = _uniqueColumn;
    BTree index = column.getIndex();

    if (index != null) {
      validateIndex(sourceRows, queryContext, xa);
      return;
    }
      
    TableIterator sourceRow = sourceRows[0];
    
    Table table = sourceRow.getTable();
    TableIterator iter = table.createTableIterator();

    try {
      byte []sourceBuffer = sourceRow.getBuffer();
      int sourceOffset = sourceRow.getRowOffset();

      iter.init(queryContext);

      while (iter.next()) {
        byte []iterBuffer = iter.getBuffer();

        iter.prevRow();

        while (iter.nextRow()) {
          int iterOffset = iter.getRowOffset();

          if (iterBuffer == sourceBuffer && iterOffset == sourceOffset)
            continue;

          if (column.isEqual(iterBuffer, iterOffset,
                             sourceBuffer, sourceOffset)) {
            long blockId = iter.getBlockId();

            throw new SQLException(L.l("`{0}' in {1}.{2} fails uniqueness constraint.",
                                       column.getString(blockId, iterBuffer, iterOffset),
                                       table.getName(),
                                       column.getName()));
          }
        }
      }
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    } finally {
      iter.free();
    }
  }

  /**
   * validate the constraint.
   */
  private void validateIndex(TableIterator []sourceRows,
                             QueryContext context, Transaction xa)
    throws SQLException
  {
    try {
      Column column = _uniqueColumn;
      TableIterator sourceRow = sourceRows[0];

      byte []sourceBuffer = sourceRow.getBuffer();
      int sourceOffset = sourceRow.getRowOffset();
      
      BTree index = column.getIndex();

      /*
      int length = column.evalToBuffer(sourceBuffer, sourceOffset,
                                       buffer, 0);

      if (length <= 0)
        return;

      long value = index.lookup(buffer, 0, length,
                                context.getTransaction());

      */

      // currently this is a static length.  See StringColumn.
      int length = column.getLength();
      int offset = sourceOffset + _uniqueColumn.getColumnOffset();
      long value = index.lookup(sourceBuffer, offset, length);

      if (value != 0) {
        Table table = sourceRow.getTable();
        long blockId = sourceRow.getBlockId();

        throw new SQLException(L.l("'{0}' in {1}.{2} fails uniqueness constraint with block address {3}.",
                                   column.getString(blockId,
                                                    sourceBuffer,
                                                    sourceOffset),
                                   table.getName(),
                                   column.getName(),
                                   ("" + (value / BlockStore.BLOCK_SIZE)
                                    + "." + (value % BlockStore.BLOCK_SIZE))));
      }
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
  }
}
