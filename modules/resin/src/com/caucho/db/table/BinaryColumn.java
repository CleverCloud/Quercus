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

import java.sql.SQLException;

import com.caucho.db.index.BTree;
import com.caucho.db.index.BinaryKeyCompare;
import com.caucho.db.index.KeyCompare;
import com.caucho.db.index.SqlIndexAlreadyExistsException;
import com.caucho.db.sql.Expr;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.sql.SelectResult;
import com.caucho.db.xa.Transaction;
import com.caucho.util.L10N;

class BinaryColumn extends Column {
  private static final L10N L = new L10N(BinaryColumn.class);
  
  private final int _length;

  /**
   * Creates a binary column.
   *
   * @param columnOffset the offset within the row
   * @param length the length of the binary
   */
  BinaryColumn(Row row, String name, int length)
  {
    super(row, name);

    if (length < 0)
      throw new IllegalArgumentException("length must be non-negative");
    else if (255 < length)
      throw new IllegalArgumentException("length too big");
    
    _length = length;
  }

  /**
   * Returns the type code for the column.
   */
  @Override
  public ColumnType getTypeCode()
  {
    return ColumnType.BINARY;
  }

  /**
   * Returns the java type.
   */
  @Override
  public Class<?> getJavaType()
  {
    return String.class;
  }

  /**
   * Returns the declaration size
   */
  @Override
  public int getDeclarationSize()
  {
    return _length;
  }

  /**
   * Returns the column's size.
   */
  @Override
  public int getLength()
  {
    return _length;
  }

  /**
   * Returns the key compare for the column.
   */
  @Override
  public KeyCompare getIndexKeyCompare()
  {
    return new BinaryKeyCompare(_length);
  }

  /**
   * Sets the string value.
   *
   * @param block the buffer to store the row
   * @param rowOffset the offset into the row
   * @param str the string value
   */
  @Override
  void setString(Transaction xa, byte []block, int rowOffset, String str)
  {
    int offset = rowOffset + _columnOffset;
    
    if (str == null) {
      setNull(block, rowOffset);
      return;
    }

    int len = str.length();
    int maxOffset = offset + _length;

    for (int i = 0; i < len && offset < maxOffset; i++) {
      int ch = str.charAt(i);

      if (ch < 0x80)
        block[offset++] = (byte) ch;
      else if (ch < 0x800) {
        block[offset++] = (byte) (0xc0 + ((ch >> 6) & 0x1f));
        block[offset++] = (byte) (0x80 + (ch & 0x3f));
      }
      else {
        block[offset++] = (byte) (0xe0 + ((ch >> 12) & 0x0f));
        block[offset++] = (byte) (0x80 + ((ch >> 6) & 0x3f));
        block[offset++] = (byte) (0x80 + (ch & 0x3f));
      }
    }

    setNonNull(block, rowOffset);
  }
  
  @Override
  public String getString(long blockId, byte []block, int rowOffset)
  {
    if (isNull(block, rowOffset))
      return null;
    
    int startOffset = rowOffset + _columnOffset;
    int len = _length;

    char []buffer = new char[len];

    int offset = startOffset;
    int endOffset = offset + len;
    int i = 0;
    while (offset < endOffset) {
      buffer[i++] = (char) (block[offset++] & 0xff);
    }

    return new String(buffer, 0, len);
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
    if (expr.isNull(context)) {
      setNull(block, rowOffset);
    }
    else {
      int i = expr.evalToBuffer(context, block, rowOffset + _columnOffset,
                                getTypeCode());
      int len = getLength();
      
      for (; i < len; i++) {
        block[rowOffset + _columnOffset + i] = 0;
      }
      
      setNonNull(block, rowOffset);
    }
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

    for (int i = _length - 1; i >= 0; i--) {
      if (block1[startOffset1 + i] != block2[startOffset2 + i])
        return false;
    }

    return true;
  }

  /**
   * Returns true if the bytes match.
   */
  @Override
  public boolean isEqual(byte []block, int rowOffset,
                         byte []buffer, int offset, int length)
  {
    if (isNull(block, rowOffset))
      return false;

    int startOffset = rowOffset + _columnOffset;

    if (_length != length)
      return false;

    int blockOffset = startOffset;
    int endOffset = blockOffset + _length;
    while (blockOffset < endOffset) {
      if (block[blockOffset++] != buffer[offset++])
        return false;
    }

    return true;
  }
  
  @Override
  public boolean isEqual(byte []block, int rowOffset, String value)
  {
    if (value == null)
      return isNull(block, rowOffset);
    else if (isNull(block, rowOffset))
      return false;
    
    int startOffset = rowOffset + _columnOffset;

    int strLength = value.length();
    int strOffset = 0;

    int offset = startOffset;
    int endOffset = offset + _length;
    while (offset < endOffset && strOffset < strLength) {
      char ch = value.charAt(strOffset++);
      
      int ch1 = block[offset++] & 0xff;

      // XXX: missing utf-8
      if (ch1 != ch)
        return false;
    }

    return offset == endOffset && strOffset == strLength;
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

    // XXX: add writeVarBinary to SelectResult
    result.writeBinary(block, rowOffset + _columnOffset, getLength());
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

    System.arraycopy(block, startOffset, buffer, bufferOffset, _length);

    return _length;
  }

  /**
   * Sets based on an iterator.
   */
  @Override
  public void set(Transaction xa,
                  TableIterator iter, Expr expr, QueryContext context)
    throws SQLException
  {
    byte []buffer = iter.getBuffer();
    int rowOffset = iter.getRowOffset();
    int colOffset = _columnOffset;
    
    if (expr.isNull(context)) {
      setNull(buffer, rowOffset);
      iter.setDirty();
      return;
    }
    
    int i = expr.evalToBuffer(context, buffer,
                              rowOffset + _columnOffset,
                              getTypeCode());
    
    if (i >= 0) {
      int len = getLength();
      
      for (; i < len; i++) {
        buffer[i + rowOffset + colOffset] = 0;
      }
      
      setNonNull(buffer, rowOffset);
    }
    else
      setNull(buffer, rowOffset);
    
    
    iter.setDirty();
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

    if (index != null) {
      try {
        index.insert(block,
                     rowOffset + _columnOffset, getLength(),
                     rowAddr,
                     false);
      } catch (SqlIndexAlreadyExistsException e) {
        throw new SqlIndexAlreadyExistsException(L.l("StringColumn '{0}.{1}' unique index set failed for {2}\n{3}",
                                          getTable().getName(),
                                          getName(),
                                          getDebugString(block, rowOffset),
                                          e.toString()),
                                      e);
      }
    }
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
      index.remove(block, rowOffset + _columnOffset, getLength());
  }

  private String getDebugString(byte []block, int rowOffset)
  {
    return getIndexKeyCompare().toString(block,
                                         rowOffset + _columnOffset,
                                         _length);
  }

  public String toString()
  {
    if (getIndex() != null)
      return "BinaryColumn[" + getName() + ",index]";
    else
      return "BinaryColumn[" + getName() + "]";
  }
}
