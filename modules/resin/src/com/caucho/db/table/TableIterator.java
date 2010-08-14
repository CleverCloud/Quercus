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

import com.caucho.db.block.Block;
import com.caucho.db.block.BlockStore;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.sql.SelectResult;
import com.caucho.db.xa.Transaction;
import com.caucho.util.L10N;

import java.io.IOException;
import java.sql.SQLException;


/**
 * Iterates over a table's rows.
 */
public class TableIterator {
  private static final L10N L = new L10N(TableIterator.class);
  
  private final static byte []_nullBuffer = new byte[256];
  
  private Table _table;
  private Transaction _xa;
  private QueryContext _queryContext;

  private long _blockId;
  private int _rowLength;
  private int _rowEnd;
  private int _rowOffset;

  private Block _block;
  private byte []_buffer;

  public TableIterator()
  {
  }

  TableIterator(Table table)
  {
    init(table);
  }

  public void init(Table table)
  {
    _table = table;
    
    if (table.getId() == 0) {
      throw new IllegalStateException(L.l("iterating with closed table."));
    }
      
    table.getColumns();

    _rowLength = table.getRowLength();
    _rowEnd = table.getRowEnd();
    _rowOffset = _rowEnd;
    _blockId = 0;
  }

  /**
   * Returns the table of the iterator.
   */
  public Table getTable()
  {
    return _table;
  }

  /**
   * Returns the current block id of the iterator.
   */
  public final long getBlockId()
  {
    return _blockId;
  }

  /**
   * Sets the current block id of the iterator.
   */
  public final void setBlockId(long blockId)
  {
    _blockId = blockId;
  }

  /**
   * Returns the current address.
   */
  public final long getRowAddress()
  {
    return BlockStore.blockIdToAddress(_blockId) + _rowOffset;
  }

  /**
   * Returns the current row offset of the iterator.
   */
  public final int getRowOffset()
  {
    return _rowOffset;
  }

  /**
   * Sets the current row offset of the iterator.
   */
  public final void setRowOffset(int rowOffset)
  {
    _rowOffset = rowOffset;
  }

  /**
   * Gets the current block.
   */
  public final byte []getBuffer()
  {
    return _buffer;
  }

  /**
   * Returns the transaction for the iterator.
   */
  public Transaction getTransaction()
  {
    return _xa;
  }

  /**
   * Returns the query context for the iterator.
   */
  public QueryContext getQueryContext()
  {
    return _queryContext;
  }

  public void init(QueryContext queryContext)
    throws SQLException
  {
    init(queryContext.getTransaction());

    _queryContext = queryContext;
  }

  public void init(Transaction xa)
    throws SQLException
  {
    Block block = _block;
    _block = null;
    _buffer = null;

    if (block != null) {
      block.free();
    }
    
    _blockId = 0;
    _rowOffset = Integer.MAX_VALUE / 2;
    _queryContext = null;
    _xa = xa;
  }

  public void initRow()
    throws IOException
  {
    _rowOffset = -_rowLength;
  }

  public void prevRow()
  {
    _rowOffset -= _rowLength;
  }

  /**
   * Sets the row.
   */
  void setRow(Block block, int rowOffset)
  {
    _block = block;
    _buffer = block.getBuffer();
    _blockId = block.getBlockId();
    _rowOffset = rowOffset;
  }

  public Block getBlock()
  {
    return _block;
  }

  /**
   * Returns the next tuple in the current row.
   *
   * @return true if a tuple is found,
   * or false if the block has no more tuples
   */
  public boolean nextRow()
    throws IOException
  {
    int rowOffset = _rowOffset;
    int rowLength = _rowLength;
    int rowEnd = _rowEnd;
    byte []buffer = _buffer;

    rowOffset += rowLength;
    for (; rowOffset < rowEnd; rowOffset += rowLength) {
      if ((buffer[rowOffset] & Table.ROW_VALID) != 0) {
        _rowOffset = rowOffset;
        return true;
      }
    }

    _rowOffset = rowOffset;

    return false;
  }

  /**
   * Returns the next row.
   */
  public boolean next()
    throws IOException
  {
    do {
      if (nextRow())
        return true;
    } while (nextBlock());

    return false;
  }

  /**
   * Returns the following block.
   */
  public boolean nextBlock()
    throws IOException
  {
    byte []buffer = _buffer;

    Block block = _block;
    _block = null;
    _buffer = null;

    if (block != null) {
      block.free();
    }

    _blockId = _table.firstRowBlock(_blockId + Table.BLOCK_SIZE);

    if (_blockId < 0) {
      return false;
    }

    block = _xa.readBlock(_table, _blockId);

    buffer = block.getBuffer();
    _block = block;
    _buffer = buffer;
    _rowOffset = 0;

    return true;
  }

  /**
   * Sets the next row.
   */
  public boolean isValidRow(long rowAddr)
    throws IOException
  {
    long blockId = _table.addressToBlockId(rowAddr);
    
    if (! _table.isRowBlock(blockId))
      return false;
    
    int rowOffset = (int) (rowAddr & BlockStore.BLOCK_OFFSET_MASK);
    
    return (rowOffset % _table.getRowLength() == 0);
  }

  /**
   * Sets the next row.
   */
  public void setRow(long rowAddr)
    throws IOException
  {
    long blockId = _table.addressToBlockId(rowAddr);

    if (blockId != _blockId) {
      _blockId = blockId;
    
      Block block = _block;
      _block = null;
      _buffer = null;

      if (block != null) {
        block.free();
      }

      _block = _xa.readBlock(_table, _blockId);
      _buffer = _block.getBuffer();
    }
    
    _rowOffset = (int) (rowAddr & BlockStore.BLOCK_OFFSET_MASK);
  }

  /**
   * Sets the next row.
   */
  public void initNullRow()
    throws IOException
  {
    Block block = _block;
    _block = null;
    _buffer = null;

    if (block != null)
      block.free();

    _rowOffset = 0;
    _buffer = _nullBuffer;
  }

  /**
   * Returns true for the null for (for OUTER JOINs)
   */
  public boolean isNullRow()
  {
    return _buffer == _nullBuffer;
  }

  /**
   * Returns true if the column is null.
   */
  public boolean isNull(Column column)
    throws SQLException
  {
    return column.isNull(_buffer, _rowOffset);
  }

  /**
   * Returns the string for the column at the given index.
   */
  public String getString(Column column)
    throws SQLException
  {
    return column.getString(getBlockId(), _buffer, _rowOffset);
  }

  /**
   * Returns the column's value as an integer
   *
   * @param index column index in the row
   *
   * @return the integer value
   */
  public int getInteger(Column column)
    throws SQLException
  {
    return column.getInteger(getBlockId(), _buffer, _rowOffset);
  }

  /**
   * Returns the column's long value.
   *
   * @param index column index in the row
   *
   * @return the long value
   */
  public long getLong(Column column)
    throws SQLException
  {
    return column.getLong(getBlockId(), _buffer, _rowOffset);
  }

  /**
   * Returns the column's double value.
   *
   * @param index column index in the row
   *
   * @return the double value
   */
  public double getDouble(Column column)
    throws SQLException
  {
    return column.getDouble(getBlockId(), _buffer, _rowOffset);
  }

  public boolean isEqual(Column column, byte []matchBuffer)
    throws SQLException
  {
    return column.isEqual(_buffer, _rowOffset,
                          matchBuffer, 0, matchBuffer.length);
  }

  public boolean isEqual(Column column, byte []matchBuffer, int matchLength)
    throws SQLException
  {
    return column.isEqual(_buffer, _rowOffset,
                          matchBuffer, 0, matchLength);
  }

  public boolean isEqual(Column column, String string)
    throws SQLException
  {
    return column.isEqual(_buffer, _rowOffset, string);
  }

  /**
   * Evaluates the row to the result.
   */
  public int getBuffer(Column column, byte []buffer, int offset)
    throws SQLException
  {
    return column.evalToBuffer(_buffer, _rowOffset, buffer, offset);
  }

  /**
   * Evaluates the row to the result.
   */
  public void evalToResult(Column column, SelectResult result)
    throws SQLException
  {
    column.evalToResult(_blockId, _buffer, _rowOffset, result);
  }

  public void delete()
    throws SQLException
  {
    setDirty();
    _table.delete(_xa, _block, _buffer, _rowOffset, true);
  }
  
  public void setDirty()
    throws SQLException
  {
    _xa.addUpdateBlock(_block);

    _block.setDirty(_rowOffset, _rowOffset + _rowLength);
  }

  public void free()
  {
    Block block = _block;
    _block = null;

    if (block != null)
      block.free();
  }
}
