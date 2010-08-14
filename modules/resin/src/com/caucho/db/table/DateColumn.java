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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.db.table;

import com.caucho.db.index.BTree;
import com.caucho.db.sql.Expr;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.sql.SelectResult;
import com.caucho.db.xa.Transaction;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.QDate;

import java.sql.SQLException;

/**
 * Represents a 64-bit long date column.
 */
class DateColumn extends Column {
  private static QDate _gmtDate = new QDate();
  /**
   * Creates a date column.
   *
   * @param row the row the column is being added to
   * @param name the column's name
   */
  DateColumn(Row row, String name)
  {
    super(row, name);
  }

  /**
   * Returns the column's type code.
   */
  @Override
  public ColumnType getTypeCode()
  {
    return ColumnType.DATE;
  }

  /**
   * Returns the column's Java type.
   */
  @Override
  public Class<?> getJavaType()
  {
    return java.sql.Date.class;
  }
  
  /**
   * Returns the column's declaration size.
   */
  @Override
  public int getDeclarationSize()
  {
    return 8;
  }
  
  /**
   * Returns the column's length
   */
  @Override
  public int getLength()
  {
    return 8;
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
    throws SQLException
  {
    if (str == null)
      setNull(block, rowOffset);
    else {
      long date = 0;

      try {
        synchronized (_gmtDate) {
          date = _gmtDate.parseDate(str);
        }
      } catch (Exception e) {
        throw new SQLExceptionWrapper(e);
      }
      
      setDate(xa, block, rowOffset, date);
    }
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
    if (isNull(block, rowOffset))
      return null;
    else
      return QDate.formatISO8601(getDate(block, rowOffset));
  }
  
  /**
   * Sets a date value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  @Override
  void setDate(Transaction xa, byte []block, int rowOffset, long value)
  {
    int offset = rowOffset + _columnOffset;
    
    block[offset++] = (byte) (value >> 56);
    block[offset++] = (byte) (value >> 48);
    block[offset++] = (byte) (value >> 40);
    block[offset++] = (byte) (value >> 32);
    block[offset++] = (byte) (value >> 24);
    block[offset++] = (byte) (value >> 16);
    block[offset++] = (byte) (value >> 8);
    block[offset++] = (byte) (value);

    setNonNull(block, rowOffset);
  }
  
  /**
   * Gets a date value from the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  @Override
  public long getDate(byte []block, int rowOffset)
  {
    if (isNull(block, rowOffset))
      return 0;
    
    int offset = rowOffset + _columnOffset;
    long value = 0;
    
    value = (block[offset++] & 0xffL) << 56;
    value |= (block[offset++] & 0xffL) << 48;
    value |= (block[offset++] & 0xffL) << 40;
    value |= (block[offset++] & 0xffL) << 32;
    value |= (block[offset++] & 0xffL) << 24;
    value |= (block[offset++] & 0xffL) << 16;
    value |= (block[offset++] & 0xffL) << 8;
    value |= (block[offset++] & 0xffL);

    return value;
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
    setDate(xa, block, rowOffset, value);
  }
  
  /**
   * Gets a long value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  @Override
  public long getLong(long blockId, byte []block, int rowOffset)
  {
    return getDate(block, rowOffset);
  }
  
  /**
   * Gets a double value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  @Override
  public double getDouble(long blockId, byte []block, int rowOffset)
  {
    return (double) getDate(block, rowOffset);
  }
  
  /**
   * Sets a long value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  @Override
  void setInteger(Transaction xa, byte []block, int rowOffset, int value)
  {
    setDate(xa, block, rowOffset, value);
  }
  
  /**
   * Sets a long value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  @Override
  public int getInteger(long blockId, byte []block, int rowOffset)
  {
    return (int) getDate(block, rowOffset);
  }

  /**
   * Evaluates the column to a stream.
   */
  @Override
  public void evalToResult(long blockId, byte []block, int rowOffset,
                           SelectResult result)
  {
    if (isNull(block, rowOffset)) {
      result.writeNull();
      return;
    }

    result.writeDate(getDate(block, rowOffset));
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
    if (isNull(block, rowOffset))
      return 0;

    int startOffset = rowOffset + _columnOffset;
    int len = 8;

    System.arraycopy(block, startOffset, buffer, bufferOffset, len);

    return len;
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
    if (expr.isNull(context))
      setNull(block, rowOffset);
    else
      setDate(xa, block, rowOffset, expr.evalDate(context));
  }

  /**
   * Returns true if the items in the given rows match.
   */
  @Override
  public boolean isEqual(byte []block1, int rowOffset1,
                         byte []block2, int rowOffset2)
  {
    //System.out.println("EQ:");
    if (isNull(block1, rowOffset1) != isNull(block2, rowOffset2))
      return false;

    int startOffset1 = rowOffset1 + _columnOffset;
    int startOffset2 = rowOffset2 + _columnOffset;

    return (block1[startOffset1 + 0] == block2[startOffset2 + 0] &&
            block1[startOffset1 + 1] == block2[startOffset2 + 1] &&
            block1[startOffset1 + 2] == block2[startOffset2 + 2] &&
            block1[startOffset1 + 3] == block2[startOffset2 + 3] &&
            block1[startOffset1 + 4] == block2[startOffset2 + 4] &&
            block1[startOffset1 + 5] == block2[startOffset2 + 5] &&
            block1[startOffset1 + 6] == block2[startOffset2 + 6] &&
            block1[startOffset1 + 7] == block2[startOffset2 + 7]);
  }
  
  /**
   * Sets any index for the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param rowAddr the address of the row
   */
  @Override
  void setIndex(Transaction xa,
                byte []block, int rowOffset,
                long rowAddr, QueryContext context)
    throws SQLException
  {
    BTree index = getIndex();

    if (index == null)
      return;

    index.insert(block, rowOffset + _columnOffset, 8, rowAddr, false);
  }

  /**
   * Sets based on an iterator.
   */
  @Override
  public void set(Transaction xa,
                  TableIterator iter, Expr expr, QueryContext context)
    throws SQLException
  {
    iter.setDirty();
    setDate(iter.getTransaction(), iter.getBuffer(), iter.getRowOffset(),
            expr.evalDate(context));
  }
  
  /**
   * Deleting the row, based on the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param expr the expression to store
   */
  @Override
  void deleteIndex(Transaction xa, byte []block, int rowOffset)
    throws SQLException
  {
    BTree index = getIndex();

    if (index != null)
      index.remove(block, rowOffset + _columnOffset, 8);
  }
}
