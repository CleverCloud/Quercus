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

package com.caucho.db.table;

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
public class UniqueConstraint extends Constraint {
  private final static L10N L = new L10N(UniqueConstraint.class);
  private final Column []_uniqueSet;

  /**
   * Creates a uniqueness constraint.
   */
  public UniqueConstraint(Column []uniqueSet)
  {
    _uniqueSet = uniqueSet;
  }

  /**
   * validate the constraint.
   */
  @Override
  public void validate(TableIterator []sourceRows,
                       QueryContext queryContext, Transaction xa)
    throws SQLException
  {
    TableIterator sourceRow = sourceRows[0];
    
    Table table = sourceRow.getTable();
    TableIterator iter = table.createTableIterator();

    byte []sourceBuffer = sourceRow.getBuffer();
    int sourceOffset = sourceRow.getRowOffset();

    iter.init(queryContext);

    try {
      while (iter.next()) {
        byte []iterBuffer = iter.getBuffer();

        iter.prevRow();

        while (iter.nextRow()) {
          int iterOffset = iter.getRowOffset();

          if (iterBuffer == sourceBuffer && iterOffset == sourceOffset)
            continue;

          boolean isMatch = true;

          for (int i = 0; i < _uniqueSet.length; i++) {
            Column column = _uniqueSet[i];

            if (! column.isEqual(iterBuffer, iterOffset,
                                 sourceBuffer, sourceOffset)) {
              isMatch = false;
              break;
            }
          }

          if (isMatch) {
            long blockId = iter.getBlockId();

            throw new SQLException(L.l("'{0}' in {1}.{2} fails uniqueness constraint.",
                                       _uniqueSet[0].getString(blockId, iterBuffer, iterOffset),
                                       table.getName(),
                                       _uniqueSet[0].getName()));
          }
        }
      }
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
  }
}
