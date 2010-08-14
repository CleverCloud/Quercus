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

import java.sql.SQLException;

import com.caucho.db.block.BlockStore;
import com.caucho.db.sql.Expr;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.sql.SelectResult;
import com.caucho.db.xa.Transaction;
import com.caucho.util.L10N;

/**
 * Represents the 64-bit long identity value
 */
class IdentityColumn extends Column {
  private static final L10N L = new L10N(IdentityColumn.class);
  
  /**
   * Creates an identity column.
   *
   * @param row the row the column is being added to
   * @param name the column's name
   */
  IdentityColumn(Row row, String name)
  {
    super(row, name);
    
    setPrimaryKey(true);
  }

  /**
   * Returns the column's type code.
   */
  @Override
  public ColumnType getTypeCode()
  {
    return ColumnType.IDENTITY;
  }

  /**
   * Returns the column's Java type.
   */
  @Override
  public Class<?> getJavaType()
  {
    return long.class;
  }
  
  /**
   * Returns the column's declaration size.
   */
  @Override
  public int getDeclarationSize()
  {
    return 0;
  }
  
  /**
   * Returns the column's length
   */
  @Override
  public int getLength()
  {
    return 0;
  }

  /**
   * Returns true if the column is null.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  @Override
  public boolean isNull(byte []block, int rowOffset)
  {
    return false;
  }
  
  /**
   * Sets a string value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  @Override
  void setString(Transaction xa, byte []block, int rowOffset, String str)
  {
    throw new IllegalStateException(L.l("an IDENTITY column cannot be set"));
  }
  
  /**
   * Gets a string value from the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  @Override
  public String getString(long blockId, byte []block, int rowOffset)
  {
    return "0x" + Long.toHexString(getLong(blockId, block, rowOffset));
  }
  
  /**
   * Sets an integer value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  @Override
  void setInteger(Transaction xa, byte []block, int rowOffset, int value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Gets an integer value from the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  @Override
  public int getInteger(long blockId, byte []block, int rowOffset)
  {
    return (int) getLong(blockId, block, rowOffset);
  }
  
  /**
   * Sets a long value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  @Override
  void setLong(Transaction xa, byte []block, int rowOffset, long value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Gets a long value from the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  @Override
  public long getLong(long blockId, byte []block, int rowOffset)
  {
    return (blockId & BlockStore.BLOCK_MASK) + rowOffset;
  }
  
  /**
   * Gets an integer value from the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  @Override
  public double getDouble(long blockId, byte []block, int rowOffset)
  {
    return (double) getLong(blockId, block, rowOffset);
  }
  
  /**
   * Sets the column based on an expression.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param expr the expression to store
   */
  @Override
  void setExpr(Transaction xa,
               byte []block, int rowOffset,
               Expr expr, QueryContext context)
    throws SQLException
  {
    long value = expr.evalLong(context);
  }

  /**
   * Sets based on an expression
   */
  @Override
  public void set(Transaction xa,
                  TableIterator iter, Expr expr, QueryContext context)
    throws SQLException
  {
      throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Evaluates the column to a stream.
   */
  @Override
  public void evalToResult(long blockId, byte []block, int rowOffset,
                           SelectResult result)
  {
    result.writeLong((blockId & BlockStore.BLOCK_MASK) + rowOffset);
  }
  
  /**
   * Evaluate to a buffer.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param buffer the result buffer
   * @param buffer the result buffer offset
   *
   * @return the length of the value
   */
  @Override
  int evalToBuffer(byte []block, int rowOffset,
                   byte []buffer, int bufferOffset)
    throws SQLException
  {
      throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if the items in the given rows match.
   */
  @Override
  public boolean isEqual(byte []block1, int rowOffset1,
                         byte []block2, int rowOffset2)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
