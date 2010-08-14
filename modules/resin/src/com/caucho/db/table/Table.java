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

import com.caucho.db.Database;
import com.caucho.db.block.Block;
import com.caucho.db.block.BlockStore;
import com.caucho.db.index.BTree;
import com.caucho.db.index.KeyCompare;
// import com.caucho.db.lock.Lock;
import com.caucho.db.sql.CreateQuery;
import com.caucho.db.sql.Expr;
import com.caucho.db.sql.Parser;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.xa.Transaction;
import com.caucho.env.thread.TaskWorker;
import com.caucho.inject.Module;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Table format:
 *
 * <pre>
 * Block 0: allocation table
 * Block 1: fragment table
 * Block 2: table definition
 *   0    - store data
 *   1024 - table data
 *    1024 - index pointers
 *   2048 - CREATE text
 * Block 3: first data
 * </pre>
 */
@Module
public class Table extends BlockStore {
  private final static Logger log
    = Logger.getLogger(Table.class.getName());
  private final static L10N L = new L10N(Table.class);

  private final static int ROOT_DATA_OFFSET = STORE_CREATE_END;
  private final static int INDEX_ROOT_OFFSET = ROOT_DATA_OFFSET + 32;

  private final static int ROOT_DATA_END = ROOT_DATA_OFFSET + 1024;

  public final static int INLINE_BLOB_SIZE = 120;

  public final static long ROW_CLOCK_MIN = 1024;

  public final static byte ROW_VALID = 0x1;
  public final static byte ROW_ALLOC = 0x2;
  public final static byte ROW_MASK = 0x3;

  private final static String DB_VERSION = "Resin-DB 4.0.6";
  private final static String MIN_VERSION = "Resin-DB 4.0.6";

  private final Row _row;

  private final int _rowLength;
  private final int _rowsPerBlock;
  private final int _rowEnd;

  private final Constraint[]_constraints;

  private final Column _autoIncrementColumn;

  private long _entries;

  private static final int FREE_ROW_BLOCK_SIZE = 256;
  private final AtomicLongArray _insertFreeRowBlockArray
    = new AtomicLongArray(FREE_ROW_BLOCK_SIZE);
  private final AtomicInteger _insertFreeRowBlockHead
    = new AtomicInteger();
  private final AtomicInteger _insertFreeRowBlockTail
    = new AtomicInteger();

  private long _rowTailTop = BlockStore.BLOCK_SIZE * FREE_ROW_BLOCK_SIZE;
  private final AtomicLong _rowTailOffset = new AtomicLong();

  private final RowAllocator _rowAllocator = new RowAllocator();

  // clock counters for row insert allocation
  private long _rowClockTop;
  private long _rowClockOffset;

  private long _clockRowFree;
  private long _clockRowUsed;
  
  private long _clockBlockFree;

  private long _autoIncrementValue = -1;

  Table(Database database, String name, Row row, Constraint constraints[])
  {
    super(database, name, null);

    _row = row;
    _constraints = constraints;

    _rowLength = _row.getLength();
    _rowsPerBlock = BLOCK_SIZE / _rowLength;
    _rowEnd = _rowLength * _rowsPerBlock;

    Column []columns = _row.getColumns();
    Column autoIncrementColumn = null;
    for (int i = 0; i < columns.length; i++) {
      columns[i].setTable(this);

      if (columns[i].getAutoIncrement() >= 0)
        autoIncrementColumn = columns[i];
    }
    _autoIncrementColumn = autoIncrementColumn;

    //new Lock("table-insert:" + name);
    //new Lock("table-alloc:" + name);
  }

  Row getRow()
  {
    return _row;
  }

  /**
   * Returns the length of a row.
   */
  public int getRowLength()
  {
    return _rowLength;
  }

  /**
   * Returns the end of the row
   */
  int getRowEnd()
  {
    return _rowEnd;
  }

  public final Column []getColumns()
  {
    return _row.getColumns();
  }

  /**
   * Returns the table's constraints.
   */
  public final Constraint []getConstraints()
  {
    return _constraints;
  }

  /**
   * Returns the auto-increment column.
   */
  public Column getAutoIncrementColumn()
  {
    return _autoIncrementColumn;
  }

  /**
   * Returns the column for the given column name.
   *
   * @param name the column name
   *
   * @return the column
   */
  public Column getColumn(String name)
  {
    Column []columns = getColumns();

    for (int i = 0; i < columns.length; i++) {
      if (columns[i].getName().equals(name))
        return columns[i];
    }

    return null;
  }

  /**
   * Returns the column index for the given column name.
   *
   * @param name the column name
   *
   * @return the column index.
   */
  public int getColumnIndex(String name)
    throws SQLException
  {
    Column []columns = getColumns();

    for (int i = 0; i < columns.length; i++) {
      if (columns[i].getName().equals(name))
        return i;
    }

    return -1;
  }

  //
  // initialization
  //

  /**
   * Loads the table from the file.
   */
  public static Table loadFromFile(Database db, String name)
    throws IOException, SQLException
  {
    Path path = db.getPath().lookup(name + ".db");

    if (! path.exists()) {
      if (log.isLoggable(Level.FINE))
        log.fine(db + " '" + path.getNativePath() + "' is an unknown table");

      return null; //throw new SQLException(L.l("table {0} does not exist", name));
    }

    String version = null;

    ReadStream is = path.openRead();
    try {
      // skip allocation table and fragment table
      is.skip(DATA_START + ROOT_DATA_OFFSET);

      StringBuilder sb = new StringBuilder();
      int ch;

      while ((ch = is.read()) > 0) {
        sb.append((char) ch);
      }

      version = sb.toString();

      if (! version.startsWith("Resin-DB")) {
        throw new SQLException(L.l("table {0} is not a Resin DB.  Version '{1}'",
                                   name, version));
      }
      else if (version.compareTo(MIN_VERSION) < 0 ||
               DB_VERSION.compareTo(version) < 0) {
        throw new SQLException(L.l("table {0} is out of date.  Old version {1}.",
                                   name, version));
      }
    } finally {
      is.close();
    }

    is = path.openRead();
    try {
      // skip allocation table and fragment table
      is.skip(DATA_START + ROOT_DATA_END);

      StringBuilder cb = new StringBuilder();

      int ch;
      while ((ch = is.read()) > 0) {
        cb.append((char) ch);
      }

      String sql = cb.toString();

      if (log.isLoggable(Level.FINER))
        log.finer("Table[" + name + "] " + version + " loading\n" + sql);

      try {
        CreateQuery query = (CreateQuery) Parser.parse(db, sql);

        TableFactory factory = query.getFactory();

        if (! factory.getName().equalsIgnoreCase(name))
          throw new IOException(L.l("factory {0} does not match", name));

        Table table = new Table(db, factory.getName(), factory.getRow(),
                                factory.getConstraints());

        table.init();

        table.clearIndexes();
        table.initIndexes();
        table.rebuildIndexes();

        return table;
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);

        throw new SQLException(L.l("can't load table {0} in {1}.\n{2}",
                                   name, path.getNativePath(), e.toString()));
      }
    } finally {
      is.close();
    }
  }

  /**
   * Creates the table.
   */
  @Override
  public void create()
    throws IOException, SQLException
  {
    super.create();

    initIndexes();

    byte []tempBuffer = new byte[BLOCK_SIZE];

    getReadWrite().readBlock(BLOCK_SIZE, tempBuffer, 0, BLOCK_SIZE);

    TempStream ts = new TempStream();

    WriteStream os = new WriteStream(ts);

    try {
      for (int i = 0; i < ROOT_DATA_OFFSET; i++)
        os.write(tempBuffer[i]);

      writeTableHeader(os);
    } finally {
      os.close();
    }

    TempBuffer head = ts.getHead();
    int offset = 0;
    for (; head != null; head = head.getNext()) {
      byte []buffer = head.getBuffer();

      int length = head.getLength();

      System.arraycopy(buffer, 0, tempBuffer, offset, length);

      for (; length < buffer.length; length++) {
        tempBuffer[offset + length] = 0;
      }

      offset += buffer.length;
    }

    for (; offset < BLOCK_SIZE; offset++)
      tempBuffer[offset] = 0;

    boolean isPriority = false;
    getReadWrite().writeBlock(BLOCK_SIZE, tempBuffer, 0, BLOCK_SIZE, isPriority);

    _database.addTable(this);
  }

  /**
   * Initialize the indexes
   */
  private void initIndexes()
    throws IOException, SQLException
  {
    Column []columns = _row.getColumns();
    for (int i = 0; i < columns.length; i++) {
      Column column = columns[i];

      if (! column.isUnique())
        continue;

      KeyCompare keyCompare = column.getIndexKeyCompare();

      if (keyCompare == null)
        continue;

      Block rootBlock = allocateIndexBlock();
      long rootBlockId = rootBlock.getBlockId();
      rootBlock.free();

      BTree btree = new BTree(this, rootBlockId, column.getLength(),
                              keyCompare);

      column.setIndex(btree);
    }
  }

  /**
   * Clears the indexes
   */
  private void clearIndexes()
    throws IOException
  {
    Column []columns = _row.getColumns();

    for (int i = 0; i < columns.length; i++) {
      BTree index = columns[i].getIndex();

      if (index == null)
        continue;

      long rootAddr = index.getIndexRoot();

      Block block = readBlock(addressToBlockId(rootAddr));

      try {
        byte []blockBuffer = block.getBuffer();

        synchronized (blockBuffer) {
          for (int j = 0; j < blockBuffer.length; j++) {
            blockBuffer[j] = 0;
          }

          block.setDirty(0, BLOCK_SIZE);
        }
      } finally {
        block.free();
      }
    }

    long blockAddr = 0;

    while ((blockAddr = firstBlock(blockAddr + BLOCK_SIZE, ALLOC_INDEX)) > 0) {
      freeBlock(blockAddr);
    }
  }

  /**
   * Rebuilds the indexes
   */
  private void rebuildIndexes()
    throws IOException, SQLException
  {
    Transaction xa = Transaction.create();
    xa.setAutoCommit(true);

    try {
      TableIterator iter = createTableIterator();

      iter.init(xa);

      Column []columns = _row.getColumns();

      while (iter.nextBlock()) {
        iter.initRow();

        byte []blockBuffer = iter.getBuffer();

        while (iter.nextRow()) {
          try {
            long rowAddress = iter.getRowAddress();
            int rowOffset = iter.getRowOffset();

            for (int i = 0; i < columns.length; i++) {
              Column column = columns[i];

              /*
              if (column.getIndex() != null)
                System.out.println(Long.toHexString(iter.getBlock().getBlockId()) + ":" + Long.toHexString(rowAddress) + ":" + Long.toHexString(rowOffset) + ": " + column.getIndexKeyCompare().toString(blockBuffer, rowOffset + column.getColumnOffset(), column.getLength()));
              */

              column.setIndex(xa, blockBuffer, rowOffset, rowAddress, null);
            }
          } catch (Exception e) {
            log.log(Level.WARNING, e.toString(), e);
          }
        }
      }
    } finally {
      xa.commit();
    }
  }

  /**
   * Rebuilds the indexes
   */
  public void validate()
    throws SQLException
  {
    try {
      validateIndexes();
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
  }

  /**
   * Rebuilds the indexes
   */
  public void validateIndexes()
    throws IOException, SQLException
  {
    Transaction xa = Transaction.create();
    xa.setAutoCommit(true);

    try {
      TableIterator iter = createTableIterator();

      iter.init(xa);

      Column []columns = _row.getColumns();

      while (iter.nextBlock()) {
        iter.initRow();

        byte []blockBuffer = iter.getBuffer();

        while (iter.nextRow()) {
          try {
            long rowAddress = iter.getRowAddress();
            int rowOffset = iter.getRowOffset();

            for (int i = 0; i < columns.length; i++) {
              Column column = columns[i];

              column.validateIndex(xa, blockBuffer, rowOffset, rowAddress);
            }
          } catch (Exception e) {
            log.log(Level.WARNING, e.toString(), e);
          }
        }
      }
    } finally {
      xa.commit();
    }
  }

  private void writeTableHeader(WriteStream os)
    throws IOException
  {
    os.print(DB_VERSION);
    os.write(0);

    while (os.getPosition() < INDEX_ROOT_OFFSET) {
      os.write(0);
    }

    Column []columns = _row.getColumns();
    for (int i = 0; i < columns.length; i++) {
      if (! columns[i].isUnique())
        continue;

      BTree index = columns[i].getIndex();

      if (index != null) {
        writeLong(os, index.getIndexRoot());
      }
      else {
        writeLong(os, 0);
      }
    }

    while (os.getPosition() < ROOT_DATA_END) {
      os.write(0);
    }

    os.print("CREATE TABLE " + getName() + "(");
    for (int i = 0; i < _row.getColumns().length; i++) {
      Column column = _row.getColumns()[i];

      if (i != 0)
        os.print(",");

      os.print(column.getName());
      os.print(" ");

      switch (column.getTypeCode()) {
      case IDENTITY:
        os.print("IDENTITY");
        break;
      case VARCHAR:
        os.print("VARCHAR(" + column.getDeclarationSize() + ")");
        break;
      case VARBINARY:
        os.print("VARBINARY(" + column.getDeclarationSize() + ")");
        break;
      case BINARY:
        os.print("BINARY(" + column.getDeclarationSize() + ")");
        break;
      case SHORT:
        os.print("SMALLINT");
        break;
      case INT:
        os.print("INTEGER");
        break;
      case LONG:
        os.print("BIGINT");
        break;
      case DOUBLE:
        os.print("DOUBLE");
        break;
      case DATE:
        os.print("TIMESTAMP");
        break;
      case BLOB:
        os.print("BLOB");
        break;
      case NUMERIC:
        {
          NumericColumn numeric = (NumericColumn) column;

          os.print("NUMERIC(" + numeric.getPrecision() + "," + numeric.getScale() + ")");
          break;
        }
      default:
        throw new UnsupportedOperationException(String.valueOf(column));
      }

      if (column.isPrimaryKey())
        os.print(" PRIMARY KEY");
      else if (column.isUnique())
        os.print(" UNIQUE");

      if (column.isNotNull())
        os.print(" NOT NULL");

      Expr defaultExpr = column.getDefault();

      if (defaultExpr != null) {
        os.print(" DEFAULT (");
        os.print(defaultExpr);
        os.print(")");
      }

      if (column.getAutoIncrement() >= 0)
        os.print(" auto_increment");
    }
    os.print(")");

    /*
    writeLong(os, _blockMax);
    writeLong(os, _entries);
    writeLong(os, _clockAddr);
    */
  }

  public TableIterator createTableIterator()
  {
    assertStoreActive();

    return new TableIterator(this);
  }

  /**
   * Returns the next auto-increment value.
   */
  public long nextAutoIncrement(QueryContext context)
    throws SQLException
  {
    synchronized (this) {
      if (_autoIncrementValue >= 0)
        return ++_autoIncrementValue;
    }

    long max = 0;

    try {
      TableIterator iter = createTableIterator();
      iter.init(context);
      while (iter.next()) {
        byte []buffer = iter.getBuffer();
        long blockId = iter.getBlockId();

        long value = _autoIncrementColumn.getLong(blockId, buffer, iter.getRowOffset());

        if (max < value)
          max = value;
      }
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }

    synchronized (this) {
      if (_autoIncrementValue < max)
        _autoIncrementValue = max;

      return ++_autoIncrementValue;
    }
  }

  //
  // insert code
  //

  /**
   * Inserts a new row, returning the row address.
   */
  public long insert(QueryContext queryContext, Transaction xa,
                     ArrayList<Column> columns,
                     ArrayList<Expr> values)
    throws IOException, SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("db table " + getName() + " insert row xa:" + xa);

    Block block = null;

    try {
      while (true) {
        long blockId = allocateInsertRowBlock();

        block = xa.loadBlock(this, blockId);

        int rowOffset = allocateRow(block, xa);

        if (rowOffset >= 0) {
          insertRow(queryContext, xa, columns, values,
                    block, rowOffset);

          block.saveAllocation();
          
          freeRowBlockId(blockId);

          return blockIdToAddress(blockId, rowOffset);
        }

        Block freeBlock = block;
        block = null;
        freeBlock.free();
      }
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    } finally {
      if (block != null)
        block.free();
    }
  }

  private int allocateRow(Block block, Transaction xa)
    throws IOException, SQLException, InterruptedException
  {
    Lock blockLock = block.getWriteLock();

    blockLock.tryLock(xa.getTimeout(), TimeUnit.MILLISECONDS);
    try {
      block.read();

      byte []buffer = block.getBuffer();

      int rowOffset = 0;

      for (; rowOffset < _rowEnd; rowOffset += _rowLength) {
        if (buffer[rowOffset] == 0) {
          buffer[rowOffset] = ROW_ALLOC;

          block.setDirty(rowOffset, rowOffset + 1);

          return rowOffset;
        }
      }
    } finally {
      blockLock.unlock();
    }

    return -1;
  }

  public void insertRow(QueryContext queryContext, Transaction xa,
                        ArrayList<Column> columns,
                        ArrayList<Expr> values,
                        Block block, int rowOffset)
    throws SQLException
  {
    byte []buffer = block.getBuffer();

    long rowAddr = blockIdToAddress(block.getBlockId(), rowOffset);
    //System.out.println("ADDR:" + rowAddr + " " + rowOffset + " " + block);

    TableIterator iter = createTableIterator();
    TableIterator []iterSet = new TableIterator[] { iter };
    // QueryContext context = QueryContext.allocate();
    boolean isReadOnly = false;
    queryContext.init(xa, iterSet, isReadOnly);
    iter.init(queryContext);

    boolean isOkay = false;
    queryContext.lock();
    
    try {
      iter.setRow(block, rowOffset);

      if (buffer[rowOffset] != ROW_ALLOC)
        throw new IllegalStateException(L.l("Expected ROW_ALLOC at '{0}'",
                                            buffer[rowOffset]));

      for (int i = rowOffset + _rowLength - 1; rowOffset < i; i--)
        buffer[i] = 0;

      for (int i = 0; i < columns.size(); i++) {
        Column column = columns.get(i);
        Expr value = values.get(i);

        column.setExpr(xa, buffer, rowOffset, value, queryContext);
      }

      // lock for insert, i.e. entries, indices, and validation
      // XXX: the set index needs to handle the validation
      //xa.lockWrite(_insertLock);
      try {
        validate(block, rowOffset, queryContext, xa);

        for (int i = 0; i < columns.size(); i++) {
          Column column = columns.get(i);
          
          column.setIndex(xa, buffer, rowOffset, rowAddr, queryContext);
        }

        buffer[rowOffset] = (byte) ((buffer[rowOffset] & ~ROW_MASK) | ROW_VALID);

        xa.addUpdateBlock(block);

        if (_autoIncrementColumn != null) {
          long blockId = iter.getBlockId();
          
          long value = _autoIncrementColumn.getLong(blockId, buffer, rowOffset);

          synchronized (this) {
            if (_autoIncrementValue < value)
              _autoIncrementValue = value;
          }
        }

        block.setDirty(rowOffset, rowOffset + _rowLength);
        _entries++;

        isOkay = true;
      } catch (SQLException e) {
        // e.printStackTrace();
        throw e;
      } finally {
        // xa.unlockWrite(_insertLock);

        if (! isOkay) {
          delete(xa, block, buffer, rowOffset, false);
          block.setDirty(rowOffset, rowOffset + _rowLength);
        }
      }
    } finally {
      queryContext.unlock();
    }
  }

  //
  // row allocation
  //

  private long allocateInsertRowBlock()
    throws IOException
  {
    long blockId = allocateRowBlockId();

    if (blockId != 0) {
      return blockId;
    }

    long rowTailOffset = _rowTailOffset.get();

    blockId = firstRowBlock(rowTailOffset);

    if (blockId <= 0) {
      Block block = allocateRow();

      blockId = block.getBlockId();
      // System.out.println("ALLOC: " + blockId + " " + _rowTailOffset.get() + " " + _rowTailTop);

      block.free();
    }

    _rowTailOffset.compareAndSet(rowTailOffset, blockId + BLOCK_SIZE);
    
    return blockId;
  }

  //
  // allocator
  //

  private void fillFreeRows()
  {
    if (_rowTailOffset.get() < _rowTailTop)
      return;
    
    while (scanClock()) {
      if (! resetClock()) {
        return;
      }
    }
  }

  private boolean resetClock()
  {
    // force 50% free rows before clock starts again
    long newRowCount = (_clockRowUsed - _clockRowFree) / _rowsPerBlock;

    // minimum 256 blocks of free rows
    if (_clockRowFree < ROW_CLOCK_MIN && _rowClockOffset > 0) {
      newRowCount = ROW_CLOCK_MIN;
    }
    
    if (newRowCount > 0) {
      _rowTailTop = _rowTailOffset.get() + newRowCount * _rowLength;
    }
    
    // System.out.println("RESET: used=" + _clockRowUsed + " free=" + _clockRowFree + " top=" + _rowTailTop);

    _rowClockOffset = 0;
    _rowClockTop = _rowTailOffset.get();
    _clockRowUsed = 0;
    _clockRowFree = 0;

    if (newRowCount > 0) {
      return false;
    }
    
    return true;
  }
  
  private boolean scanClock()
  {
    while (isFreeRowBlockIdAvailable()) {
      long clockBlockId = _rowClockOffset;

      try {
        clockBlockId = firstRowBlock(clockBlockId);

        if (clockBlockId < 0) {
          _rowClockOffset = _rowClockTop;
          
          return true;
        }

        if (isRowBlockFree(clockBlockId)) {
          _clockBlockFree++;
          freeRowBlockId(clockBlockId);
        }
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);

        clockBlockId = _rowClockTop;
      } finally {
        _rowClockOffset = clockBlockId + BlockStore.BLOCK_SIZE;
      }
    }
    
    return false;
  }
  
  /**
   * Test if any row in the block is free
   */
  private boolean isRowBlockFree(long blockId)
    throws IOException
  {
    Block block = readBlock(blockId);

    try {
      int rowOffset = 0;

      byte []buffer = block.getBuffer();
      boolean isFree = false;

      for (; rowOffset < _rowEnd; rowOffset += _rowLength) {
        if (buffer[rowOffset] == 0) {
          isFree = true;
          _clockRowFree++;
        }
        else
          _clockRowUsed++;
      }

      return isFree;
    } finally {
      block.free();
    }
  }

  private boolean isFreeRowBlockIdAvailable()
  {
    int head = _insertFreeRowBlockHead.get();
    int tail = _insertFreeRowBlockTail.get();
    
    return (head + 1) % FREE_ROW_BLOCK_SIZE != tail;
  }

  private long allocateRowBlockId()
  {
    while (true) {
      int tail = _insertFreeRowBlockTail.get();
      int head = _insertFreeRowBlockHead.get();
      
      if (head == tail) {
        _rowAllocator.wake();
        return 0;
      }
    
      long blockId = _insertFreeRowBlockArray.getAndSet(tail, 0);
      
      int nextTail = (tail + 1) % FREE_ROW_BLOCK_SIZE;
      
      _insertFreeRowBlockTail.compareAndSet(tail, nextTail);
      
      if (blockId > 0) {
        int size = (head - tail + FREE_ROW_BLOCK_SIZE) % FREE_ROW_BLOCK_SIZE;
        
        if (2 * size < FREE_ROW_BLOCK_SIZE) {
          _rowAllocator.wake();
        }
        
        return blockId;
      }
    }
  }

  private void freeRowBlockId(long blockId)
  {
    while (true) {
      int head = _insertFreeRowBlockHead.get();
      int tail = _insertFreeRowBlockTail.get();
      
      int nextHead = (head + 1) % FREE_ROW_BLOCK_SIZE;
      
      if (nextHead == tail)
        return;
      
      _insertFreeRowBlockHead.compareAndSet(head, nextHead);
      
      if (_insertFreeRowBlockArray.compareAndSet(head, 0, blockId))
        return;
    }
  }

  //
  // insert
  //

  /**
   * Validates the given row.
   */
  private void validate(Block block, int rowOffset,
                        QueryContext queryContext, Transaction xa)
    throws SQLException
  {
    TableIterator row = createTableIterator();
    TableIterator []rows = new TableIterator[] { row };

    row.setRow(block, rowOffset);

    for (int i = 0; i < _constraints.length; i++) {
      _constraints[i].validate(rows, queryContext, xa);
    }
  }

  void delete(Transaction xa, Block block,
              byte []buffer, int rowOffset,
              boolean isDeleteIndex)
    throws SQLException
  {
    byte rowState = buffer[rowOffset];

    /*
    if ((rowState & ROW_MASK) == 0)
      return;
    */

    buffer[rowOffset] = (byte) ((rowState & ~ROW_MASK) | ROW_ALLOC);

    Column []columns = _row.getColumns();

    for (int i = 0; i < columns.length; i++) {
      columns[i].deleteData(xa, buffer, rowOffset);
    }

    if (isDeleteIndex) {
      for (int i = 0; i < columns.length; i++) {
        try {
          columns[i].deleteIndex(xa, buffer, rowOffset);
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    }

    buffer[rowOffset] = 0;
  }

  @Override
  public void close()
  {

    _row.close();

    super.close();

    _rowAllocator.destroy();
  }

  private void writeLong(WriteStream os, long value)
    throws IOException
  {
    os.write((int) (value >> 56));
    os.write((int) (value >> 48));
    os.write((int) (value >> 40));
    os.write((int) (value >> 32));
    os.write((int) (value >> 24));
    os.write((int) (value >> 16));
    os.write((int) (value >> 8));
    os.write((int) value);
  }

  @Override
  public String toString()
  {
    int id = Alarm.isTest() ? 1 : getId();
    
    return getClass().getSimpleName() + "[" + getName() + ":" + id + "]";
  }

  class RowAllocator extends TaskWorker {
    @Override
    public long runTask()
    {
      fillFreeRows();
      
      return -1;
    }
  }
}
