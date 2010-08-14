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

import java.io.IOException;
import java.sql.SQLException;
import com.caucho.db.index.BTree;
import com.caucho.db.index.KeyCompare;
import com.caucho.db.sql.Expr;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.sql.SelectResult;
import com.caucho.db.xa.Transaction;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

@Module
abstract public class Column {
  private static final L10N L = new L10N(Column.class);
  
  @Module
  public enum ColumnType {
    NONE,
    VARCHAR,
    BOOLEAN,
    BYTE,
    SHORT,
    INT,
    LONG,
    DOUBLE,
    DATE,
    BLOB,
    NUMERIC,
    BINARY,
    VARBINARY,
    IDENTITY;
  }
  
  private final Row _row;
  private final String _name;

  protected final int _columnOffset;
  protected final int _nullOffset;
  protected final byte _nullMask;

  private Table _table;
  
  private boolean _isPrimaryKey;
  private boolean _isUnique;
  private boolean _isNotNull;
  private int _autoIncrementMin = -1;
  private Expr _defaultExpr;

  private BTree _index;

  Column(Row row, String name)
  {
    _row = row;
    _name = name;

    _columnOffset = _row.getLength();
    _nullOffset = _row.getNullOffset();
    _nullMask = _row.getNullMask();
  }
  
  /**
   * Returns the column's name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the table.
   */
  void setTable(Table table)
  {
    _table = table;
  }

  /**
   * Gets the table.
   */
  public Table getTable()
  {
    return _table;
  }

  /**
   * Returns the column offset.
   */
  int getColumnOffset()
  {
    return _columnOffset;
  }

  /**
   * Returns the column's code.
   */
  abstract public ColumnType getTypeCode();

  /**
   * Returns the java type.
   */
  public Class<?> getJavaType()
  {
    return Object.class;
  }

  /**
   * Returns true if the column is a primary key
   */
  public boolean isPrimaryKey()
  {
    return _isPrimaryKey;
  }

  /**
   * Returns true if the column is a primary key
   */
  public void setPrimaryKey(boolean primaryKey)
  {
    _isPrimaryKey = primaryKey;
  }

  /**
   * Returns true if the column is unique.
   */
  public boolean isUnique()
  {
    return _isUnique;
  }

  /**
   * Set if the column is unique.
   */
  public void setUnique()
  {
    _isUnique = true;
  }

  /**
   * Returns the index.
   */
  public BTree getIndex()
  {
    return _index;
  }

  /**
   * Sets the index.
   */
  public void setIndex(BTree index)
  {
    _index = index;
  }

  /**
   * Returns the key compare for the column.
   */
  public KeyCompare getIndexKeyCompare()
  {
    return null;
  }

  /**
   * Set true if the column is NOT NULL.
   */
  public void setNotNull()
  {
    _isNotNull = true;
  }

  /**
   * Returns true if the column is NOT NULL.
   */
  public boolean isNotNull()
  {
    return _isNotNull;
  }

  /**
   * Sets the default expression
   */
  public void setDefault(Expr expr)
  {
    _defaultExpr = expr;
  }

  /**
   * Gets the default expression
   */
  public Expr getDefault()
  {
    return _defaultExpr;
  }

  /**
   * Returns true if the column is auto increment
   */
  public void setAutoIncrement(int min)
  {
    _autoIncrementMin = min;
  }

  /**
   * Set if the column is unique.
   */
  public int getAutoIncrement()
  {
    return _autoIncrementMin;
  }

  /**
   * Returns the column's size (from the decl).
   */
  abstract public int getDeclarationSize();
  
  abstract int getLength();

  /**
   * Returns true if the column is null.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  public boolean isNull(byte []block, int rowOffset)
  {
    return (block[rowOffset + _nullOffset] & _nullMask) == 0;
  }

  /**
   * Sets the column null.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  public final void setNull(byte []block, int rowOffset)
  {
    block[rowOffset + _nullOffset] &= ~_nullMask;
  }

  /**
   * Sets the column non-null.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  protected final void setNonNull(byte []block, int rowOffset)
  {
    block[rowOffset + _nullOffset] |= _nullMask;
  }

  /**
   * Gets a string value from the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  public abstract String getString(long blockId, byte []block, int rowOffset)
    throws SQLException;

  /**
   * Sets a string value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  abstract void setString(Transaction xa,
                          byte []block, int rowOffset, String value)
    throws SQLException;
  
  /**
   * Sets an integer value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  public int getInteger(long blockId, byte []block, int rowOffset)
    throws SQLException
  {
    String str = getString(blockId, block, rowOffset);

    if (str == null)
      return 0;

    return Integer.parseInt(str);
  }
  
  /**
   * Sets an integer value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  void setInteger(Transaction xa,
                  byte []block, int rowOffset, int value)
    throws SQLException
  {
    setString(xa, block, rowOffset, String.valueOf(value));
  }
  
  /**
   * Sets a long value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  public long getLong(long blockId, byte []block, int rowOffset)
    throws SQLException
  {
    String str = getString(blockId, block, rowOffset);

    if (str == null)
      return 0;

    return Long.parseLong(str);
  }
  
  /**
   * Sets a long value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  void setLong(Transaction xa,
               byte []block, int rowOffset, long value)
    throws SQLException
  {
    setString(xa, block, rowOffset, String.valueOf(value));
  }
  
  /**
   * Sets a double value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  public double getDouble(long blockId, byte []block, int rowOffset)
    throws SQLException
  {
    String str = getString(blockId, block, rowOffset);

    if (str == null)
      return 0;

    return Double.parseDouble(str);
  }
  
  /**
   * Sets a double value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  void setDouble(Transaction xa,
                 byte []block, int rowOffset, double value)
    throws SQLException
  {
    setString(xa, block, rowOffset, String.valueOf(value));
  }
  
  /**
   * Sets the column based on an expression.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param expr the expression to store
   */
  void setExpr(Transaction xa,
               byte []block, int rowOffset,
               Expr expr, QueryContext context)
    throws SQLException
  {
    if (expr.isNull(context))
      setNull(block, rowOffset);
    else
      setString(xa, block, rowOffset, expr.evalString(context));
  }
  
  /**
   * Gets a double value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   */
  public long getDate(byte []block, int rowOffset)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets a date value in the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param value the value to store
   */
  void setDate(Transaction xa, byte []block, int rowOffset, long value)
    throws SQLException
  {
    throw new UnsupportedOperationException();
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
  int evalToBuffer(byte []block, int rowOffset,
                   byte []buffer, int bufferOffset)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if the bytes are equal.
   */
  public boolean isEqual(byte []block, int rowOffset,
                         byte []buffer, int offset, int length)
  {
    return false;
  }

  /**
   * Returns true if the bytes are equal.
   */
  public boolean isEqual(byte []buffer1, int rowOffset1,
                         byte []buffer2, int rowOffset2)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if the string is equal.
   */
  public boolean isEqual(byte []block, int rowOffset, String string)
  {
    return false;
  }

  /**
   * Evaluates the column to the result.
   */
  public void evalToResult(long blockId, byte []block, int rowOffset, SelectResult result)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets based on an iterator.
   */
  public void set(Transaction xa,
                  TableIterator iter, Expr expr, QueryContext context)
    throws SQLException
  {
    setString(xa, iter.getBuffer(), iter.getRowOffset(),
              expr.evalString(context));
    
    iter.setDirty();
  }
  
  /**
   * Sets any index for the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param rowAddr the address of the row
   */
  void setIndex(Transaction xa,
                byte []block, int rowOffset,
                long rowAddr, QueryContext context)
    throws SQLException
  {
    BTree index = getIndex();

    if (index == null)
      return;

    /*
    IndexCache manager = IndexCache.create();

    manager.insert(index,
                   block, rowOffset + getColumnOffset(), getLength(),
                   rowAddr,
                   xa);
    */
    index.insert(block, rowOffset + getColumnOffset(), getLength(),
                 rowAddr, false);
  }
  
  /**
   * Deleting the row, based on the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param expr the expression to store
   */
  void deleteIndex(Transaction xa, byte []block, int rowOffset)
    throws SQLException
  {
    BTree index = getIndex();

    if (index == null)
      return;

    /*
    IndexCache manager = IndexCache.create();

    manager.delete(index,
                   block, rowOffset + getColumnOffset(), getLength(),
                   xa);
    */

    index.remove(block, rowOffset + getColumnOffset(), getLength());
  }
  
  /**
   * Sets any index for the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param rowAddr the address of the row
   */
  void validateIndex(Transaction xa,
                     byte []block, int rowOffset,
                     long rowAddr)
    throws SQLException, IOException
  {
    BTree index = getIndex();

    if (index == null)
      return;

    long value
      = index.lookup(block, rowOffset + getColumnOffset(), getLength());

    if (value != rowAddr)
      throw new IllegalStateException(L.l("invalid index '{0}' at {1}",
                                          value, Long.toHexString(rowAddr)));
  }
  
  /**
   * Deleting the row, based on the column.
   *
   * @param block the block's buffer
   * @param rowOffset the offset of the row in the block
   * @param expr the expression to store
   */
  void deleteData(Transaction xa, byte []block, int rowOffset)
    throws SQLException
  {
  }

  /**
   * Cleanup of the column on table shutdown.
   */
  public void close()
  {
    BTree index = _index;
    
    if (index != null)
      index.close();
  }

  public String toString()
  {
    if (getIndex() != null)
      return getClass().getName() + "[" + _name + ",index]";
    else
      return getClass().getName() + "[" + _name + "]";
  }
}
