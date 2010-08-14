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

import com.caucho.db.blob.BlobInputStream;
import com.caucho.db.blob.BlobOutputStream;
import com.caucho.db.blob.Inode;
import com.caucho.db.sql.Expr;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.sql.SelectResult;
import com.caucho.db.xa.Transaction;
import com.caucho.util.IoUtil;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

class BlobColumn extends Column {
  private static final Logger log
    = Logger.getLogger(BlobColumn.class.getName());
  
  /**
   * Creates an inode column.
   *
   * @param columnOffset the offset within the row
   * @param maxLength the maximum length of the string
   */
  BlobColumn(Row row, String name)
  {
    super(row, name);
  }

  /**
   * Returns the type code for the column.
   */
  @Override
  public ColumnType getTypeCode()
  {
    return ColumnType.BLOB;
  }

  /**
   * Returns the java type.
   */
  @Override
  public Class<?> getJavaType()
  {
    return java.sql.Blob.class;
  }

  /**
   * Returns the declaration size
   */
  @Override
  public int getDeclarationSize()
  {
    return 128;
  }

  /**
   * Returns the column's size.
   */
  @Override
  public int getLength()
  {
    return 128;
  }

  /**
   * Sets the string value.
   *
   * @param block the buffer to store the row
   * @param rowOffset the offset into the row
   * @param str the string value
   */
  @Override
  void setString(Transaction xa,
                 byte []block, int rowOffset, String str)
  {
    if (! isNull(block, rowOffset)) {
      long length = Inode.readLong(block, rowOffset + _columnOffset);

      if (Table.INLINE_BLOB_SIZE <= length) {
        Inode inode = new Inode();
        inode.init(getTable(), xa, block, rowOffset + _columnOffset);
        xa.addDeleteInode(inode);
      }
    }
    
    if (str == null) {
      setNull(block, rowOffset);
      return;
    }

    setNonNull(block, rowOffset);

    BlobOutputStream os = null;
    
    try {
      os = new BlobOutputStream(xa, getTable(),
                                block, rowOffset + _columnOffset);

      int length = str.length();
      for (int i = 0; i < length; i++) {
        int ch = str.charAt(i);

        if (ch < 0x80)
          os.write(ch);
        else if (ch < 0x800) {
          os.write(0xc0 + (ch >> 6));
          os.write(0x80 + (ch & 0x3f));
        }
        else {
          os.write(0xe0 + (ch >> 12));
          os.write(0x80 + ((ch >> 6) & 0x3f));
          os.write(0x80 + (ch & 0x3f));
        }
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      IoUtil.close(os);
    }
  }

  /**
   * Sets the string value.
   *
   * @param block the buffer to store the row
   * @param rowOffset the offset into the row
   * @param str the string value
   */
  private void setStream(Transaction xa,
                         byte []block, int rowOffset,
                         InputStream value)
  {
    if (! isNull(block, rowOffset)) {
      long length = Inode.readLong(block, rowOffset + _columnOffset);

      if (Table.INLINE_BLOB_SIZE <= length) {
        Inode inode = new Inode();
        inode.init(getTable(), xa, block, rowOffset + _columnOffset);
        xa.addDeleteInode(inode);
      }
    }

    if (value == null) {
      setNull(block, rowOffset);
      return;
    }

    setNonNull(block, rowOffset);

    try {
      BlobOutputStream os;
      os = new BlobOutputStream(xa, getTable(),
                                block, rowOffset + _columnOffset);

      os.writeFromStream(value);

      os.close();
      value.close();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
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
  void deleteData(Transaction xa, byte []block, int rowOffset)
    throws SQLException
  {
    if (! isNull(block, rowOffset)) {
      long length = Inode.readLong(block, rowOffset + _columnOffset);

      if (length < Table.INLINE_BLOB_SIZE)
        return;
      
      Inode inode = new Inode();
      inode.init(getTable(), xa, block, rowOffset + _columnOffset);
      xa.addDeleteInode(inode);
    }
  }

  @Override
  public String getString(long blockId, byte []block, int rowOffset)
  {
    if (isNull(block, rowOffset))
      return null;

    try {
      BlobInputStream is;
      is = new BlobInputStream(getTable(), block, rowOffset + _columnOffset);

      int ch;
      StringBuilder cb = new StringBuilder();

      while ((ch = is.read()) >= 0) {
        if (ch < 0x80)
          cb.append((char) ch);
        else if ((ch & 0xe0) == 0xc0) {
          int ch1 = is.read();

          cb.append((char) (((ch & 0x3f) << 6) +
                            (ch1 & 0x3f)));
        }
        else {
          int ch1 = is.read();
          int ch2 = is.read();

          cb.append((char) (((ch & 0xf) << 12) +
                            ((ch1 & 0x3f) << 6) +
                            ((ch2 & 0x3f))));
        }
      }

      is.close();

      return cb.toString();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    
    return null;
  }

  /**
   * Sets based on an iterator.
   */
  @Override
  public void set(Transaction xa,
                  TableIterator iter, Expr expr, QueryContext context)
    throws SQLException
  {
    byte []block = iter.getBuffer();
    int rowOffset = iter.getRowOffset();

    if (expr.isNull(context))
      setNull(block, rowOffset);
    else if (expr.isBinaryStream(context))
      setStream(xa, block, rowOffset, expr.evalStream(context));
    else
      setString(xa, block, rowOffset, expr.evalString(context));
    
    iter.setDirty();
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
    else if (expr.isBinaryStream(context)) {
      setStream(xa, block, rowOffset, expr.evalStream(context));
    }
    else {
      setString(xa, block, rowOffset, expr.evalString(context));
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

    for (int i = 128 - 1; i >= 0; i--) {
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

    return false;
  }
  
  @Override
  public boolean isEqual(byte []block, int rowOffset, String value)
  {
    if (value == null)
      return isNull(block, rowOffset);
    else if (isNull(block, rowOffset))
      return false;
    
    return false;
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

    result.writeBlob(block, rowOffset + _columnOffset);
  }

  public String toString()
  {
    return "BlobColumn[" + getName() + "]";
  }
}
