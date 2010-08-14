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

import com.caucho.db.index.BTree;
import com.caucho.db.sql.Expr;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.sql.SelectResult;
import com.caucho.db.xa.Transaction;
import com.caucho.util.CharBuffer;

import java.sql.SQLException;

/**
 * Represents a numeric column.
 */
class NumericColumn extends Column {
  private int _precision;
  private int _scale;
  private long _offset;
  
  /**
   * Creates a date column.
   *
   * @param row the row the column is being added to
   * @param name the column's name
   */
  NumericColumn(Row row, String name, int precision, int scale)
  {
    super(row, name);

    _precision = precision;
    _scale = scale;

    _offset = 1;
    
    for (int i = 0; i < scale; i++)
      _offset *= 10;
  }

  /**
   * Returns the column's type code.
   */
  @Override
  public ColumnType getTypeCode()
  {
    return ColumnType.NUMERIC;
  }

  /**
   * Returns the precision.
   */
  public int getPrecision()
  {
    return _precision;
  }

  /**
   * Returns the scale.
   */
  public int getScale()
  {
    return _scale;
  }

  /**
   * Returns the column's Java type.
   */
  @Override
  public Class<?> getJavaType()
  {
    return double.class;
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
    if (str == null || str.length() == 0)
      setNull(block, rowOffset);
    else
      setDouble(xa, block, rowOffset, Double.parseDouble(str));
  }
  
  /**
   * Gets a string value from the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  @Override
  public String getString(long blockId, byte []block, int rowOffset)
    throws SQLException
  {
    if (isNull(block, rowOffset))
      return null;
    else {
      long value = getNumeric(block, rowOffset);

      CharBuffer cb = new CharBuffer();

      long head = value / _offset;
      long tail = value % _offset;

      cb.append(head);
      cb.append('.');
      cb.append(tail);

      return cb.toString();
    }
  }
  
  /**
   * Sets a double value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  @Override
  public void setDouble(Transaction xa, byte []block, int rowOffset, double v)
    throws SQLException
  {
    setNumeric(xa, block, rowOffset, (long) (v * _offset + 0.5));
  }
  
  /**
   * Sets a double value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  @Override
  public double getDouble(long blockId, byte []block, int rowOffset)
    throws SQLException
  {
    return (double) getNumeric(block, rowOffset) / _offset;
  }

  /**
   * Evaluates the column to a stream.
   */
  @Override
  public void evalToResult(long blockId, byte []block, int rowOffset,
                           SelectResult result)
    throws SQLException
  {
    if (isNull(block, rowOffset)) {
      result.writeNull();
      return;
    }

    result.writeString(getString(blockId, block, rowOffset));
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
    if (expr.isNull(null))
      setNull(block, rowOffset);
    else
      setDouble(xa, block, rowOffset, expr.evalDouble(context));
  }

  /**
   * Returns true if the items in the given rows match.
   */
  @Override
  public boolean isEqual(byte []block1, int rowOffset1,
                         byte []block2, int rowOffset2)
  {
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
  public void set(TableIterator iter, Expr expr, QueryContext context)
    throws SQLException
  {
    iter.setDirty();
    setDouble(iter.getTransaction(), iter.getBuffer(), iter.getRowOffset(),
            expr.evalDouble(context));
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
  
  /**
   * Sets a numeric value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  void setNumeric(Transaction xa, byte []block, int rowOffset, long value)
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
   * Gets a long value from the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  long getNumeric(byte []block, int rowOffset)
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
}
