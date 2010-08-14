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

package com.caucho.db.index;

import com.caucho.db.Database;
import com.caucho.db.block.Block;
import com.caucho.db.block.BlockManager;
import com.caucho.db.block.BlockStore;
// import com.caucho.db.lock.Lock;
import com.caucho.db.xa.Transaction;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Structure of the table:
 *
 * <pre>
 * b4 - flags
 * b4 - length
 * b8 - parent
 * b8 - next
 * tuples*
 * </pre>
 * 
 * Structure of a tuple:
 *
 * <pre>
 * b8  - ptr to the actual data
 * key - the tuple's key
 * </pre>
 *
 * For a non-leaf node, the key is the last matching entry in the subtree.
 */
public final class BTree {
  private final static L10N L = new L10N(BTree.class);
  private final static Logger log
    = Logger.getLogger(BTree.class.getName());
  
  public final static long FAIL = 0;
  private final static int BLOCK_SIZE = BlockStore.BLOCK_SIZE;
  private final static int PTR_SIZE = 8;

  private final static int FLAGS_OFFSET = 0;
  private final static int LENGTH_OFFSET = FLAGS_OFFSET + 4;
  private final static int PARENT_OFFSET = LENGTH_OFFSET + 4;
  private final static int NEXT_OFFSET = PARENT_OFFSET + PTR_SIZE;
  private final static int HEADER_SIZE = NEXT_OFFSET + PTR_SIZE;

  private final static int LEAF_MASK = 0x03;
  private final static int IS_LEAF = 0x01;
  private final static int IS_NODE = 0x02;

  private BlockStore _store;
  
  private long _rootBlockId;
  private Block _rootBlock;
  
  private int _keySize;
  private int _tupleSize;
  private int _n;
  private int _minN;
  
  private KeyCompare _keyCompare;

  private long _timeout = 120000L;

  private volatile boolean _isStarted;

  /**
   * Creates a new BTree with the given backing.
   *
   * @param store the underlying store containing the btree.
   */
  public BTree(BlockStore store,
               long rootBlockId,
               int keySize,
               KeyCompare keyCompare)
    throws IOException
  {
    if (keyCompare == null)
      throw new NullPointerException();
    
    _store = store;
    _store.getBlockManager();
    
    _rootBlockId = rootBlockId;
    _rootBlock = store.readBlock(rootBlockId);
      
    // new Lock("index:" + store.getName());
    
    if (BLOCK_SIZE < keySize + HEADER_SIZE)
      throw new IOException(L.l("BTree key size '{0}' is too large.",
                                keySize));

    _keySize = keySize;

    _tupleSize = keySize + PTR_SIZE;

    _n = (BLOCK_SIZE - HEADER_SIZE) / _tupleSize;
    _minN = (_n + 1) / 2;
    if (_minN < 0)
      _minN = 1;

    _keyCompare = keyCompare;

    byte []rootBuffer = _rootBlock.getBuffer();
    if (getInt(rootBuffer, FLAGS_OFFSET) == 0)
      setLeaf(rootBuffer, true);
  }

  /**
   * Returns the index root.
   */
  public long getIndexRoot()
  {
    return _rootBlockId;
  }

  /**
   * Creates and initializes the btree.
   */
  public void create()
    throws IOException
  {
  }
  
  public long lookup(byte []keyBuffer,
                     int keyOffset,
                     int keyLength)
    throws IOException, SQLException
  {
    try {
      return lookup(keyBuffer, keyOffset, keyLength, _rootBlockId);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }
  
  private long lookup(byte []keyBuffer,
                     int keyOffset,
                     int keyLength,
                     long blockId)
    throws IOException, SQLException, InterruptedException
  {
    Block block;

    if (blockId == _rootBlockId) {
      block = _rootBlock;
      block.allocate();
    }
    else
      block = _store.loadBlock(blockId);

    try {
      Lock blockLock = block.getReadLock();
      
      blockLock.tryLock(_timeout, TimeUnit.MILLISECONDS);

      try {
        validateIndex(block);
        
        block.read();
        
        byte []buffer = block.getBuffer();

        boolean isLeaf = isLeaf(buffer, block);
      
        long value = lookupTuple(blockId, buffer,
                                 keyBuffer, keyOffset, keyLength,
                                 isLeaf);

        if (isLeaf || value == FAIL)
          return value;
        else
          return lookup(keyBuffer, keyOffset, keyLength, value);
      } finally {
        blockLock.unlock();
      }
    } finally {
      block.free();
    }
  }
  
  /**
   * Inserts the new value for the given key.
   *
   * @return false if the block needs to be split
   */
  public void insert(byte []keyBuffer,
                     int keyOffset,
                     int keyLength,
                     long value,
                     boolean isOverride)
    throws SQLException
  {
    try {
      while (! insert(keyBuffer, keyOffset, keyLength,
                      value, isOverride, true,
                      _rootBlockId)) {
        splitRoot(_rootBlockId);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (SQLException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      throw new SQLExceptionWrapper(e.toString(), e);
    }
  }

  /**
   * Inserts the new value for the given key.
   *
   * @return false if the block needs to be split
   * @throws InterruptedException 
   */
  private boolean insert(byte []keyBuffer,
                         int keyOffset,
                         int keyLength,
                         long value,
                         boolean isOverride,
                         boolean isRead,
                         long blockId)
    throws IOException, SQLException, InterruptedException
  {
    Block block;

    if (blockId == _rootBlockId) {
      block = _rootBlock;
      block.allocate();
    }
    else
      block = _store.loadBlock(blockId);
    
    try {
      validateIndex(block);
        
      if (isRead && insertReadChild(keyBuffer, keyOffset, keyLength,
                                    value, isOverride, block))
        return true;
      else
        return insertWriteChild(keyBuffer, keyOffset, keyLength,
                                value, isOverride, block);
    } finally {
      block.free();
    }
  }

  private boolean insertReadChild(byte []keyBuffer,
                                  int keyOffset,
                                  int keyLength,
                                  long value,
                                  boolean isOverride,
                                  Block block)
    throws IOException, SQLException, InterruptedException
  {
    Lock blockLock = block.getReadLock();
    blockLock.tryLock(_timeout, TimeUnit.MILLISECONDS);
      
    try {
      validateIndex(block);
        
      block.read();
      
      long blockId = block.getBlockId();
      byte []buffer = block.getBuffer();

      int length = getLength(buffer);

      if (length == _n) {
        // return false if the block needs to be split
        return false;
      }

      if (isLeaf(buffer, block)) {
        return false;
      }

      long childBlockId = lookupTuple(blockId, buffer,
                                      keyBuffer, keyOffset, keyLength,
                                      false);

      return insert(keyBuffer, keyOffset, keyLength,
                    value, isOverride, true,
                    childBlockId);
    } finally {
      blockLock.unlock();
    }
  }

  private boolean insertWriteChild(byte []keyBuffer,
                                   int keyOffset,
                                   int keyLength,
                                   long value,
                                   boolean isOverride,
                                   Block block)
    throws IOException, SQLException, InterruptedException
  {
    Lock blockLock = block.getWriteLock();
    blockLock.tryLock(_timeout, TimeUnit.MILLISECONDS);
      
    try {
      block.read();
      
      validate(block);
      
      long blockId = block.getBlockId();
      byte []buffer = block.getBuffer();

      int length = getLength(buffer);

      if (length == _n) {
        // return false if the block needs to be split
        return false;
      }

      if (isLeaf(buffer, block)) {
        insertValue(keyBuffer, keyOffset, keyLength,
                    value, isOverride, block);

        validate(block);

        return true;
      }

      long childBlockId = lookupTuple(blockId, buffer,
                                      keyBuffer, keyOffset, keyLength,
                                      false);

      while (! insert(keyBuffer, keyOffset, keyLength,
                      value, isOverride, true,
                      childBlockId)) {
        split(block, childBlockId);

        childBlockId = lookupTuple(blockId, buffer,
                                   keyBuffer, keyOffset, keyLength,
                                   false);
      }
      
      validate(block);

      return true;
    } finally {
      blockLock.unlock();
    }
  }
    
  /**
   * Inserts into the next block given the current block and the given key.
   */
  private void insertValue(byte []keyBuffer,
                           int keyOffset,
                           int keyLength,
                           long value,
                           boolean isOverride,
                           Block block)
    throws IOException, SQLException
  {
    byte []buffer = block.getBuffer();

    insertLeafBlock(block.getBlockId(), buffer,
                    keyBuffer, keyOffset, keyLength,
                    value, isOverride);
    
    block.setFlushDirtyOnCommit(false);
    block.setDirty(0, BlockStore.BLOCK_SIZE);
  }

  /**
   * Inserts into the next block given the current block and the given key.
   */
  private long insertLeafBlock(long blockId,
                               byte []buffer,
                               byte []keyBuffer,
                               int keyOffset,
                               int keyLength,
                               long value,
                               boolean isOverride)
    throws IOException, SQLException
  {
    int tupleSize = _tupleSize;
    int length = getLength(buffer);

    int sublen = length;
    int min = 0;
    int max = length;
    int offset = HEADER_SIZE;
    
    while (min < max) {
      int i = (min + max) / 2;
      
      offset = HEADER_SIZE + i * tupleSize;
    
      int cmp = _keyCompare.compare(keyBuffer, keyOffset,
                                    buffer, offset + PTR_SIZE,
                                    keyLength);

      if (cmp == 0) {
        if (! isOverride) {
          long oldValue = getPointer(buffer, offset);

          if (value != oldValue)
            throw new SqlIndexAlreadyExistsException(L.l("'{0}' insert of key '{1}' fails index uniqueness.",
                                       _store,
                                       _keyCompare.toString(keyBuffer, keyOffset, keyLength)));
        }

        setPointer(buffer, offset, value);
        //writeBlock(blockIndex, block);
        
        return 0;
      }
      else if (0 < cmp) {
        min = i + 1;
      }
      else if (cmp < 0) {
        max = i;
      }
    }

    if (length < _n) {
      offset = HEADER_SIZE + min * tupleSize;
      
      return addKey(blockId, buffer, offset, min, length,
                    keyBuffer, keyOffset, keyLength, value);
    }
    else {
      throw new IllegalStateException("ran out of key space");
    }

    // return split(blockIndex, block);
  }

  private long addKey(long blockId, byte []buffer, int offset,
                      int index, int length,
                      byte []keyBuffer, int keyOffset, int keyLength,
                      long value)
    throws IOException
  {
    int tupleSize = _tupleSize;

    if (index < length) {
      if (offset + tupleSize < HEADER_SIZE)
        throw new IllegalStateException();
      
      System.arraycopy(buffer, offset,
                       buffer, offset + tupleSize,
                       (length - index) * tupleSize);
    }
    
    setPointer(buffer, offset, value);
    setLength(buffer, length + 1);

    if (log.isLoggable(Level.FINEST))
      log.finest("btree insert at " + debugId(blockId) + ":" + offset + " value:" + debugId(value));

    if (offset + PTR_SIZE < HEADER_SIZE)
      throw new IllegalStateException();
      
    System.arraycopy(keyBuffer, keyOffset,
                     buffer, offset + PTR_SIZE,
                     keyLength);
          
    for (int j = PTR_SIZE + keyLength; j < tupleSize; j++)
      buffer[offset + j] = 0;

    return -value;
  }

  /**
   * The length in lBuf is assumed to be the length of the buffer.
   *
   * parent must already be locked
   * @throws InterruptedException 
   */
  private void split(Block parent,
                     long blockId)
    throws IOException, SQLException, InterruptedException
  {
    Block block = _store.readBlock(blockId);

    try {
      validate(block);
        
      Lock blockLock = block.getWriteLock();
      blockLock.tryLock(_timeout, TimeUnit.MILLISECONDS);

      try {
        split(parent, block);

        validate(block);
      } finally {
        blockLock.unlock();
      }
    } finally {
      block.free();
    }
  }

  /**
   * The length in lBuf is assumed to be the length of the buffer.
   */
  private void split(Block parentBlock,
                     Block block)
    throws IOException, SQLException
  {
    long parentId = parentBlock.getBlockId();
    long blockId = block.getBlockId();
    
    log.finest("btree splitting " + debugId(blockId));
    
    block.setFlushDirtyOnCommit(false);

    byte []buffer = block.getBuffer();
    int length = getLength(buffer);

    // Check length to avoid possible timing issue, since we release the
    // read lock for the block between the initial check in insert() and
    // getting it back in split()
    if (length < _n / 2)
      return;

    if (length < 2)
      throw new IllegalStateException(L.l("illegal length '{0}' for block {1}",
                                          length, debugId(blockId)));
      
    Block leftBlock = null;

    try {
      parentBlock.setFlushDirtyOnCommit(false);
    
      byte []parentBuffer = parentBlock.getBuffer();
      int parentLength = getLength(parentBuffer);
    
      validate(parentId, parentBuffer);
      validate(blockId, buffer);
      
      leftBlock = _store.allocateIndexBlock();
      // System.out.println("TREE-alloc1:" + Long.toHexString(leftBlock.getBlockId()));
      leftBlock.setFlushDirtyOnCommit(false);
      // System.out.println("ALLOC: " + leftBlock);
      
      byte []leftBuffer = leftBlock.getBuffer();
      long leftBlockId = leftBlock.getBlockId();

      int pivot = length / 2;

      int pivotSize = pivot * _tupleSize;
      int pivotEnd = HEADER_SIZE + pivotSize;
      int blockEnd = HEADER_SIZE + length * _tupleSize;
      
      System.arraycopy(buffer, HEADER_SIZE,
                       leftBuffer, HEADER_SIZE,
                       pivotSize);

      setInt(leftBuffer, FLAGS_OFFSET, getInt(buffer, FLAGS_OFFSET));
      setLength(leftBuffer, pivot);
      // XXX: NEXT_OFFSET needs to work with getRightIndex
      setPointer(leftBuffer, NEXT_OFFSET, 0);
      
      setPointer(leftBuffer, PARENT_OFFSET, parentId);

      System.arraycopy(buffer, pivotEnd,
                       buffer, HEADER_SIZE,
                       blockEnd - pivotEnd);

      setLength(buffer, length - pivot);

      insertLeafBlock(parentId, parentBuffer,
                      leftBuffer, pivotEnd - _tupleSize + PTR_SIZE, _keySize,
                      leftBlockId,
                      true);
      
      validate(parentId, parentBuffer);
      validate(leftBlockId, leftBuffer);
      validate(blockId, buffer);
      
      validate(block);
      validate(parentBlock);
      validate(leftBlock);
      
      leftBlock.setDirty(0, BlockStore.BLOCK_SIZE);
      parentBlock.setDirty(0, BlockStore.BLOCK_SIZE);
    } finally {
      if (leftBlock != null)
        leftBlock.free();
      
      block.setDirty(0, BlockStore.BLOCK_SIZE);
    }
  }

  /**
   * The length in lBuf is assumed to be the length of the buffer.
   * @throws InterruptedException 
   */
  private void splitRoot(long rootBlockId)
    throws IOException, SQLException, InterruptedException
  {
    Block rootBlock = _rootBlock; // store.readBlock(rootBlockId);
    rootBlock.allocate();

    try {
      Lock rootLock = rootBlock.getWriteLock();
      rootLock.tryLock(_timeout, TimeUnit.MILLISECONDS);
      
      try {
        splitRoot(rootBlock);

        validate(rootBlock);
      } finally {
        rootLock.unlock();
      }
    } finally {
      rootBlock.free();
    }
  }

  /**
   * Splits the current leaf into two.  Half of the entries go to the
   * left leaf and half go to the right leaf.
   */
  private void splitRoot(Block parentBlock)
    throws IOException
  {
    long parentId = parentBlock.getBlockId();
    
    log.finest("btree splitting root " + (parentId / BLOCK_SIZE));

    Block leftBlock = null;
    Block rightBlock = null;

    try {
      byte []parentBuffer = parentBlock.getBuffer();
      int length = getLength(parentBuffer);

      if (length == 1)
        return;
      
      parentBlock.setFlushDirtyOnCommit(false);

      int parentFlags = getInt(parentBuffer, FLAGS_OFFSET);

      leftBlock = _store.allocateIndexBlock();
      // System.out.println("TREE-alloc2:" + Long.toHexString(leftBlock.getBlockId()));
      leftBlock.setFlushDirtyOnCommit(false);
      
      long leftBlockId = leftBlock.getBlockId();
    
      rightBlock = _store.allocateIndexBlock();
      // System.out.println("TREE-alloc3:" + Long.toHexString(rightBlock.getBlockId()));
      rightBlock.setFlushDirtyOnCommit(false);
      
      long rightBlockId = rightBlock.getBlockId();

      int pivot = (length - 1) / 2;
      
      //System.out.println("INDEX SPLIT ROOT: " + (parentId / BLOCK_SIZE)
      //                    + " PIVOT=" + pivot);

      if (length <= 2 || _n < length || pivot < 1 || length <= pivot)
        throw new IllegalStateException(Long.toHexString(parentBlock.getBlockId()) + ": " + length + " is an illegal length, or pivot " + pivot + " is bad, with n=" + _n);

      int pivotOffset = HEADER_SIZE + pivot * _tupleSize;
      long pivotValue = getPointer(parentBuffer, pivotOffset);

      byte []leftBuffer = leftBlock.getBuffer();

      System.arraycopy(parentBuffer, HEADER_SIZE,
                       leftBuffer, HEADER_SIZE,
                       pivotOffset + _tupleSize - HEADER_SIZE);
      setInt(leftBuffer, FLAGS_OFFSET, parentFlags);
      setLength(leftBuffer, pivot + 1);
      setPointer(leftBuffer, PARENT_OFFSET, parentId);
      setPointer(leftBuffer, NEXT_OFFSET, 0); // rightBlockId); 

      byte []rightBuffer = rightBlock.getBuffer();

      if (length - pivot - 1 < 0)
        throw new IllegalStateException("illegal length " + pivot + " " + length);

      System.arraycopy(parentBuffer, pivotOffset + _tupleSize,
                       rightBuffer, HEADER_SIZE,
                       (length - pivot - 1) * _tupleSize);

      setInt(rightBuffer, FLAGS_OFFSET, parentFlags);
      setLength(rightBuffer, length - pivot - 1);
      setPointer(rightBuffer, PARENT_OFFSET, parentId);
      setPointer(rightBuffer, NEXT_OFFSET,
                 getPointer(parentBuffer, NEXT_OFFSET));

      System.arraycopy(parentBuffer, pivotOffset,
                       parentBuffer, HEADER_SIZE,
                       _tupleSize);
      setPointer(parentBuffer, HEADER_SIZE, leftBlockId);

      setLeaf(parentBuffer, false);
      setLength(parentBuffer, 1);
      setPointer(parentBuffer, NEXT_OFFSET, rightBlockId);
      
      parentBlock.setDirty(0, BlockStore.BLOCK_SIZE);
      leftBlock.setDirty(0, BlockStore.BLOCK_SIZE);
      rightBlock.setDirty(0, BlockStore.BLOCK_SIZE);
      
      validate(parentBlock);
      validate(leftBlock);
      validate(rightBlock);
    } finally {
      if (leftBlock != null)
        leftBlock.free();
      
      if (rightBlock != null)
        rightBlock.free();
    }
  }
  
  public void remove(byte []keyBuffer,
                      int keyOffset,
                      int keyLength)
    throws SQLException
  {
    try {
      Block rootBlock = _rootBlock; // _store.readBlock(_rootBlockId);
      rootBlock.allocate();

      try {
        if (! removeRead(rootBlock, keyBuffer, keyOffset, keyLength)) {
          removeWrite(rootBlock, keyBuffer, keyOffset, keyLength);
        }
      } finally {
        rootBlock.free();
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (SQLException e) {
      throw e;
    } catch (Exception e) {
      throw new SQLExceptionWrapper(e.toString(), e);
    }
  }

  /**
   * Recursively remove a key from the index.
   *
   * block is read-locked by the parent.
   * @throws InterruptedException 
   */
  private boolean removeRead(Block block,
                             byte []keyBuffer,
                             int keyOffset,
                             int keyLength)
    throws IOException, SQLException, InterruptedException
  {
    Lock blockLock = block.getReadLock();
    blockLock.tryLock(_timeout, TimeUnit.MILLISECONDS);

    try {
      validateIndex(block);
        
      byte []buffer = block.getBuffer();
      long blockId = block.getBlockId();

      if (isLeaf(buffer, block))
        return false;
      
      long childId;

      childId = lookupTuple(blockId, buffer,
                            keyBuffer, keyOffset, keyLength,
                            false);

      if (childId == FAIL)
        return true;

      Block childBlock = _store.readBlock(childId);
        
      try {
        validateIndex(childBlock);
        
        if (removeRead(childBlock, keyBuffer, keyOffset, keyLength))
          return true;
        else
          return removeWrite(childBlock, keyBuffer, keyOffset, keyLength);
      } finally {
        childBlock.free();
      }
    } finally {
      blockLock.unlock();
    }
  }

  /**
   * Recursively remove a key from the index.
   *
   * block is read-locked by the parent.
   * @throws InterruptedException 
   */
  private boolean removeWrite(Block block,
                              byte []keyBuffer,
                              int keyOffset,
                              int keyLength)
    throws IOException, SQLException, InterruptedException
  {
    byte []buffer = block.getBuffer();
    long blockId = block.getBlockId();
    
    Lock blockLock = block.getWriteLock();
    blockLock.tryLock(_timeout, TimeUnit.MILLISECONDS);

    try {
      boolean isLeaf = isLeaf(buffer, block);

      if (isLeaf) {
        block.setFlushDirtyOnCommit(false);

        removeLeafEntry(blockId, buffer,
                        keyBuffer, keyOffset, keyLength);
        
        block.setDirty(0, BlockStore.BLOCK_SIZE);
      }
      else {
        long childId;

        childId = lookupTuple(blockId, buffer,
                              keyBuffer, keyOffset, keyLength,
                              isLeaf);

        if (childId == FAIL)
          return true;

        Block childBlock = _store.readBlock(childId);
        try {
          validateIndex(childBlock);

          boolean isJoin;

          isJoin = ! removeWrite(childBlock, keyBuffer, keyOffset, keyLength);

          if (isJoin && joinBlocks(block, childBlock)) {
            if (childBlock.getUseCount() > 2) {
              System.out.println("USE: " + childBlock.getUseCount() + " " + block);
            }
            childBlock.deallocate();
          }

          validate(block);
        } finally {
          childBlock.free();
        }
      }
      
      return _minN <= getLength(buffer);
    } finally {
      blockLock.unlock();
    }
  }

  /**
   * Balances the block size so it's always 1/2 full.  joinBlocks is called
   * when the block has one too few items, i.e. less than half full.
   *
   * If the left block has enough items, copy one from the left.
   * If the right block has enough items, copy one from the right.
   *
   * Otherwise, merge the block with either the left or the right block.
   *
   * parent is write-locked by the parent.
   * block is not locked.
   *
   * <pre>
   * ... | leftBlock | block | rightBlock | ...
   * </pre>
   *
   * @return true if the block should be deleted/freed
   * @throws InterruptedException 
   */
  private boolean joinBlocks(Block parent,
                             Block block)
    throws IOException, SQLException, InterruptedException
  {
    long parentBlockId = parent.getBlockId();
    byte []parentBuffer = parent.getBuffer();
    int parentLength = getLength(parentBuffer);

    long blockId = block.getBlockId();
    byte []buffer = block.getBuffer();
    
    long leftBlockId = getLeftBlockId(parent, blockId);
    long rightBlockId = getRightBlockId(parent, blockId);

    // If the left block has extra data, shift the last left item
    // to the block
    if (leftBlockId > 0) {
      Block leftBlock = _store.readBlock(leftBlockId);

      try {
        byte []leftBuffer = leftBlock.getBuffer();

        Lock leftLock = leftBlock.getWriteLock();
        leftLock.tryLock(_timeout, TimeUnit.MILLISECONDS);

        try {
          int leftLength = getLength(leftBuffer);

          Lock blockLock = block.getWriteLock();
          blockLock.tryLock(_timeout, TimeUnit.MILLISECONDS);

          try {
            if (_minN < leftLength) {
              validateEqualLeaf(buffer, leftBuffer, block, leftBlock);
              
              parent.setFlushDirtyOnCommit(false);

              leftBlock.setFlushDirtyOnCommit(false);

              validate(parentBlockId, parentBuffer);
              validate(leftBlockId, leftBuffer);
              validate(blockId, buffer);
              
              // System.out.println("MOVE_FROM_LEFT: " + debugId(blockId) + " from " + debugId(leftBlockId));
              moveFromLeft(parentBuffer, leftBuffer, buffer, blockId);
              validate(parentBlockId, parentBuffer);
              validate(leftBlockId, leftBuffer);
              validate(blockId, buffer);
              
              parent.setDirty(0, BlockStore.BLOCK_SIZE);
              leftBlock.setDirty(0, BlockStore.BLOCK_SIZE);

              return false;
            }
          } finally {
            blockLock.unlock();
          }
        } finally {
          leftLock.unlock();
        }
      } finally {
        leftBlock.free();
      }
    }

    // If the right block has extra data, shift the first right item
    // to the block
    if (rightBlockId > 0) {
      Block rightBlock = _store.readBlock(rightBlockId);

      try {
        byte []rightBuffer = rightBlock.getBuffer();

        Lock blockLock = block.getWriteLock();
        blockLock.tryLock(_timeout, TimeUnit.MILLISECONDS);

        try {
          Lock rightLock = rightBlock.getWriteLock();
          rightLock.tryLock(_timeout, TimeUnit.MILLISECONDS);

          try {
            int rightLength = getLength(rightBuffer);

            if (_minN < rightLength) {
              validateEqualLeaf(buffer, rightBuffer, block, rightBlock);
              
              parent.setFlushDirtyOnCommit(false);

              rightBlock.setFlushDirtyOnCommit(false);

              // System.out.println("MOVE_FROM_RIGHT: " + debugId(blockId) + " from " + debugId(rightBlockId));

              moveFromRight(parentBuffer, buffer, rightBuffer, blockId);
              validate(parentBlockId, parentBuffer);
              validate(blockId, buffer);
              validate(rightBlockId, rightBuffer);
              
              parent.setDirty(0, BlockStore.BLOCK_SIZE);
              rightBlock.setDirty(0, BlockStore.BLOCK_SIZE);

              return false;
            }
          } finally {
            rightLock.unlock();
          }
        } finally {
          blockLock.unlock();
        }
      } finally {
        rightBlock.free();
      }
    }

    if (parentLength < 2)
      return false;
    
    // If the left block has space, merge with it
    if (leftBlockId > 0) {
      Block leftBlock = _store.readBlock(leftBlockId);
      
      try {
        byte []leftBuffer = leftBlock.getBuffer();

        Lock leftLock = leftBlock.getWriteLock();
        leftLock.tryLock(_timeout, TimeUnit.MILLISECONDS);

        try {
          int leftLength = getLength(leftBuffer);

          Lock blockLock = block.getWriteLock();
          blockLock.tryLock(_timeout, TimeUnit.MILLISECONDS);

          try {
            int length = getLength(buffer);

            if (length + leftLength <= _n) {
              validateEqualLeaf(leftBuffer, buffer, leftBlock, block);
              
              parent.setFlushDirtyOnCommit(false);

              leftBlock.setFlushDirtyOnCommit(false);
      
              // System.out.println("MERGE_LEFT: " + debugId(blockId) + " from " + debugId(leftBlockId));

              mergeLeft(parentBuffer,
                        leftBuffer, leftBlockId,
                        buffer, blockId);
              
              validate(parentBlockId, parentBuffer);
              validate(leftBlockId, leftBuffer);
              
              parent.setDirty(0, BlockStore.BLOCK_SIZE);
              leftBlock.setDirty(0, BlockStore.BLOCK_SIZE);

              // System.out.println("FREE-ML: " + block);

              return true;
            }
          } finally {
            blockLock.unlock();
          }
        } finally {
          leftLock.unlock();
        }
      } finally {
        leftBlock.free();
      }
    }
    
    // If the right block has space, merge with it
    if (rightBlockId > 0) {
      Block rightBlock = _store.readBlock(rightBlockId);

      try {
        byte []rightBuffer = rightBlock.getBuffer();

        Lock blockLock = block.getWriteLock();
        blockLock.tryLock(_timeout, TimeUnit.MILLISECONDS);

        try {
          Lock rightLock = rightBlock.getWriteLock();
          rightLock.tryLock(_timeout, TimeUnit.MILLISECONDS);

          try {
            int length = getLength(buffer);
            int rightLength = getLength(rightBuffer);

            if (length + rightLength <= _n) {
              validateEqualLeaf(rightBuffer, buffer, rightBlock, block);

              rightBlock.setFlushDirtyOnCommit(false);

              parent.setFlushDirtyOnCommit(false);

              // System.out.println("MERGE_RIGHT: " + debugId(blockId) + " from " + debugId(rightBlockId));

              validate(blockId, buffer);
              validate(parentBlockId, parentBuffer);
              validate(rightBlockId, rightBuffer);
              
              mergeRight(parentBuffer, buffer, rightBuffer, blockId);
              
              validate(parentBlockId, parentBuffer);
              validate(rightBlockId, rightBuffer);
              
              rightBlock.setDirty(0, BlockStore.BLOCK_SIZE);
              parent.setDirty(0, BlockStore.BLOCK_SIZE);

              // System.out.println("FREE-MR: " + block);

              return true;
            }
          } finally {
            rightLock.unlock();
          }
        } finally {
          blockLock.unlock();
        }
      } finally {
        rightBlock.free();
      }
    }

    // XXX: error

    return false;
  }

  private void validateEqualLeaf(byte []leftBuffer, byte []rightBuffer,
                                 Block left, Block right)
  {
    if (isLeaf(leftBuffer, left) != isLeaf(rightBuffer, right)) {
      throw new IllegalStateException(L.l("leaf mismatch {0} {1} and {2} {3}",
                                          isLeaf(leftBuffer, left),
                                          isLeaf(rightBuffer, right),
                                          left, right));
    }
  }

  /**
   * Returns the block index to the left of blockId
   *
   * <pre>
   *  ... | leftBlockId | blockId | ...
   * </pre>
   */
  private long getLeftBlockId(Block parent, long blockId)
  {
    byte []buffer = parent.getBuffer();
    
    int length = getLength(buffer);

    if (length < 1)
      throw new IllegalStateException("zero length for " + debugId(parent.getBlockId()));

    int offset = HEADER_SIZE;
    int tupleSize = _tupleSize;
    int end = offset + length * tupleSize;

    for (; offset < end; offset += tupleSize) {
      long pointer = getPointer(buffer, offset);

      if (pointer == blockId) {
        if (HEADER_SIZE < offset) {
          return getPointer(buffer, offset - tupleSize);
        }
        else
          return -1;
      }
    }
    
    long pointer = getPointer(buffer, NEXT_OFFSET);
    
    if (pointer == blockId)
      return getPointer(buffer, HEADER_SIZE + (length - 1) * tupleSize);
    else
      throw new IllegalStateException("Can't find " + debugId(blockId) + " in parent " + debugId(parent.getBlockId()));
  }

  /**
   * Takes the last entry from the left block and moves it to the
   * first entry in the current block.
   *
   * @param parentBuffer the parent block buffer
   * @param leftBuffer the left block buffer
   * @param buffer the block's buffer
   * @param index the index of the block
   */
  private void moveFromLeft(byte []parentBuffer,
                            byte []leftBuffer,
                            byte []buffer,
                            long blockId)
  {
    int parentLength = getLength(parentBuffer);

    int tupleSize = _tupleSize;
    int parentEnd = HEADER_SIZE + parentLength * tupleSize;
    int parentOffset = HEADER_SIZE;

    int leftLength = getLength(leftBuffer);

    int length = getLength(buffer);

    // pointer in the parent to the left defaults to the tail - 1
    int parentLeftOffset = -1;

    if (blockId == getPointer(parentBuffer, NEXT_OFFSET)) {
      // db/0040
      // parentLeftOffset = parentOffset - tupleSize;
      parentLeftOffset = parentEnd - tupleSize;
    }
    else {
      for (parentOffset = HEADER_SIZE + tupleSize;
           parentOffset < parentEnd;
           parentOffset += tupleSize) {
        long pointer = getPointer(parentBuffer, parentOffset);

        if (pointer == blockId) {
          parentLeftOffset = parentOffset - tupleSize;
          break;
        }
      }
    }

    if (parentLeftOffset < 0) {
      log.warning("Can't find parent left in deletion borrow left ");
      return;
    }

    // shift the data in the buffer
    System.arraycopy(buffer, HEADER_SIZE,
                     buffer, HEADER_SIZE + tupleSize,
                     length * tupleSize);

    int leftEnd = HEADER_SIZE + leftLength * tupleSize;
    
    // copy the last item in the left to the buffer
    System.arraycopy(leftBuffer, leftEnd - tupleSize,
                     buffer, HEADER_SIZE,
                     tupleSize);

    // add the buffer length
    setLength(buffer, length + 1);

    // subtract from the left length
    leftLength -= 1;
    setLength(leftBuffer, leftLength);

    leftEnd = HEADER_SIZE + leftLength * tupleSize;

    // copy the key from the new left tail to the left item
    System.arraycopy(leftBuffer, leftEnd - tupleSize + PTR_SIZE,
                     parentBuffer, parentLeftOffset + PTR_SIZE,
                     tupleSize - PTR_SIZE);
  }

  /**
   * Merge the buffer together with the leftBuffer
   *
   * <pre>
   * ... | leftBlock | block | rightBlock | ...
   * </pre>
   *
   * <pre>
   * ... | leftBlock + block | rightBlock | ...
   * </pre>
   */
  private void mergeLeft(byte []parentBuffer,
                         byte []leftBuffer,
                         long leftBlockId,
                         byte []buffer,
                         long blockId)
  {
    if (isLeaf(leftBuffer) != isLeaf(buffer)) {
      throw new IllegalStateException("leaf does not match "
                                      + isLeaf(leftBuffer)
                                      + " " + isLeaf(buffer)
                                      + debugId(blockId));
    }
    
    int tupleSize = _tupleSize;

    int parentLength = getLength(parentBuffer);
    int parentEnd = HEADER_SIZE + parentLength * tupleSize;
    int parentOffset = HEADER_SIZE;
    
    int leftLength = getLength(leftBuffer);
    int leftEnd = HEADER_SIZE + leftLength * tupleSize;

    int blockLength = getLength(buffer);
    int blockSize = blockLength * tupleSize;

    for (parentOffset += tupleSize;
         parentOffset < parentEnd;
         parentOffset += tupleSize) {
      long pointer = getPointer(parentBuffer, parentOffset);

      if (pointer == blockId) {
        // shift the parent buffer to replace the left item with the
        // current item (replacing the key)
        System.arraycopy(parentBuffer, parentOffset,
                         parentBuffer, parentOffset - tupleSize,
                         parentEnd - parentOffset);
        
        // set the parent's pointer to the left block id
        setPointer(parentBuffer, parentOffset - tupleSize, leftBlockId);
        setLength(parentBuffer, parentLength - 1);

        // the new left.next value is the buffer's next value
        setPointer(leftBuffer, NEXT_OFFSET,
                   getPointer(buffer, NEXT_OFFSET));

        // append the buffer to the left buffer
        System.arraycopy(buffer, HEADER_SIZE,
                         leftBuffer, leftEnd,
                         blockSize);

        setLength(leftBuffer, leftLength + blockLength);

        return;
      }
    }

    // Here block is the last item in the parent

    long pointer = getPointer(parentBuffer, NEXT_OFFSET);

    if (pointer != blockId) {
      throw new IllegalStateException("BTree remove can't find matching block: " + debugId(blockId));
    }
    
    setPointer(parentBuffer, NEXT_OFFSET, leftBlockId);
    setLength(parentBuffer, parentLength - 1);

    // the new left.next value is the buffer's next value
    setPointer(leftBuffer, NEXT_OFFSET,
               getPointer(buffer, NEXT_OFFSET));

    // append the buffer to the left buffer
    System.arraycopy(buffer, HEADER_SIZE,
                     leftBuffer, leftEnd,
                     blockSize);

    setLength(leftBuffer, leftLength + blockLength);
  }

  /**
   * Returns the index to the right of the current one
   */
  private long getRightBlockId(Block parent, long blockId)
  {
    byte []buffer = parent.getBuffer();
    
    int length = getLength(buffer);

    int offset = HEADER_SIZE;
    int tupleSize = _tupleSize;
    int end = offset + length * tupleSize;

    for (; offset < end; offset += tupleSize) {
      long pointer = getPointer(buffer, offset);

      if (pointer == blockId) {
        if (offset + tupleSize < end) {
          return getPointer(buffer, offset + tupleSize);
        }
        else
          return getPointer(buffer, NEXT_OFFSET);
      }
    }

    return -1;
  }

  /**
   * Takes the first entry from the right block and moves it to the
   * last entry in the current block.
   *
   * @param parentBuffer the parent block buffer
   * @param rightBuffer the right block buffer
   * @param buffer the block's buffer
   * @param index the index of the block
   */
  private void moveFromRight(byte []parentBuffer,
                             byte []buffer,
                             byte []rightBuffer,
                             long blockId)
  {
    int parentLength = getLength(parentBuffer);

    int tupleSize = _tupleSize;
    int parentEnd = HEADER_SIZE + parentLength * tupleSize;
    int parentOffset;

    int rightLength = getLength(rightBuffer);
    int rightSize = rightLength * tupleSize;

    int blockLength = getLength(buffer);
    int blockEnd = HEADER_SIZE + blockLength * tupleSize;

    for (parentOffset = HEADER_SIZE;
         parentOffset < parentEnd;
         parentOffset += tupleSize) {
      long pointer = getPointer(parentBuffer, parentOffset);

      if (pointer == blockId)
        break;
    }

    if (parentEnd <= parentOffset) {
      log.warning("Can't find buffer in deletion borrow right ");
      return;
    }

    // copy the first item in the right to the buffer
    System.arraycopy(rightBuffer, HEADER_SIZE,
                     buffer, blockEnd,
                     tupleSize);

    // add the buffer length
    setLength(buffer, blockLength + 1);

    // shift the data in the right buffer
    System.arraycopy(rightBuffer, HEADER_SIZE + tupleSize,
                     rightBuffer, HEADER_SIZE,
                     rightSize - tupleSize);

    // subtract from the right length
    setLength(rightBuffer, rightLength - 1);

    // copy the entry from the new buffer tail to the buffer's parent entry
    System.arraycopy(buffer, blockEnd + PTR_SIZE,
                     parentBuffer, parentOffset + PTR_SIZE,
                     tupleSize - PTR_SIZE);
  }

  /**
   * Merges the buffer with the right-most one.
   *
   * <pre>
   * ... | leftBlock | block | rightBlock | ...
   * </pre>
   *
   * <pre>
   * ... | leftBlock | block + rightBlock | ...
   * </pre>
   */
  private void mergeRight(byte []parentBuffer,
                          byte []buffer,
                          byte []rightBuffer,
                          long blockId)
  {
    if (isLeaf(buffer) != isLeaf(rightBuffer)) {
      throw new IllegalStateException("leaf does not match "
                                      + isLeaf(buffer)
                                      + " " + isLeaf(rightBuffer)
                                      + debugId(blockId));
    }
    
    int tupleSize = _tupleSize;
    
    int parentLength = getLength(parentBuffer);
    int parentEnd = HEADER_SIZE + parentLength * tupleSize;
    int parentOffset;

    int rightLength = getLength(rightBuffer);
    int rightSize = rightLength * tupleSize;

    int blockLength = getLength(buffer);
    int blockSize = blockLength * tupleSize;

    for (parentOffset = HEADER_SIZE;
         parentOffset < parentEnd;
         parentOffset += tupleSize) {
      long pointer = getPointer(parentBuffer, parentOffset);

      if (pointer == blockId) {
        // remove the buffer's pointer from the parent
        System.arraycopy(parentBuffer, parentOffset + tupleSize,
                         parentBuffer, parentOffset,
                         parentEnd - parentOffset - tupleSize);

        setLength(parentBuffer, parentLength - 1);
        
        // add space in the right buffer
        System.arraycopy(rightBuffer, HEADER_SIZE,
                         rightBuffer, HEADER_SIZE + blockSize,
                         rightSize);

        // add the buffer to the right buffer
        System.arraycopy(buffer, HEADER_SIZE,
                         rightBuffer, HEADER_SIZE,
                         blockSize);

        setLength(rightBuffer, blockLength + rightLength);

        return;
      }
    }

    throw new IllegalStateException("BTree merge right can't find matching index: " + debugId(blockId));
  }

  /**
   * Looks up the next block given the current block and the given key.
   */
  private long lookupTuple(long blockId,
                           byte []buffer,
                           byte []keyBuffer,
                           int keyOffset,
                           int keyLength,
                           boolean isLeaf)
    throws IOException
  {
    int length = getLength(buffer);

    int offset = HEADER_SIZE;
    int tupleSize = _tupleSize;
    int end = HEADER_SIZE + length * tupleSize;

    long value;

    while (length > 0) {
      int tail = offset + tupleSize * length;
      int delta = tupleSize * (length / 2);
      int newOffset = offset + delta;

      if (newOffset < 0) {
        System.out.println("UNDERFLOW: " + debugId(blockId)  + " LENGTH:" + length + " STU:" + getLength(buffer) + " DELTA:" + delta);
        throw new IllegalStateException("lookupTuple underflow newOffset:" + newOffset);

      }
      else if (newOffset > 65536) {
        System.out.println("OVERFLOW: " + debugId(blockId)  + " LENGTH:" + length + " STU:" + getLength(buffer) + " DELTA:" + delta);
        throw new IllegalStateException("lookupTuple overflow newOffset:" + newOffset);

      }

      int cmp = _keyCompare.compare(keyBuffer, keyOffset,
                                    buffer, PTR_SIZE + newOffset, keyLength);
      
      if (cmp == 0) {
        value = getPointer(buffer, newOffset);

        if (value == 0 && ! isLeaf)
          throw new IllegalStateException("illegal 0 value at " + newOffset + " for block " + debugId(blockId));

        return value;
      }
      else if (cmp > 0) {
        offset = newOffset + tupleSize;
        length = (tail - offset) / tupleSize;
      }
      else if (cmp < 0) {
        length = length / 2;
      }

      if (length > 0) {
      }
      else if (isLeaf)
        return 0;
      else if (cmp < 0) {
        value = getPointer(buffer, newOffset);

        if (value == 0 && ! isLeaf)
          throw new IllegalStateException("illegal 0 value at " + newOffset + " for block " + debugId(blockId));

        return value;
      }
      else if (offset == end) {
        value = getPointer(buffer, NEXT_OFFSET);

        if (value != 0 || isLeaf)
          return value;
        else
          return getPointer(buffer, end - tupleSize);
        
        /*
        if (value == 0 && ! isLeaf)
          throw new IllegalStateException("illegal 0 value at end=" + newOffset + " for block " + debugId(blockId) + " tuple=" + _tupleSize);

        return value;
        */
      }
      else {
        value = getPointer(buffer, offset);

        if (value == 0 && ! isLeaf)
          throw new IllegalStateException("illegal 0 value at " + newOffset + " for block " + debugId(blockId));

        return value;
      }
    }

    if (isLeaf)
      return 0;
    else {
      value = getPointer(buffer, NEXT_OFFSET);

      if (value == 0 && ! isLeaf)
        throw new IllegalStateException("illegal 0 value at NEXT_OFFSET for block " + debugId(blockId));

      return value;
    }
  }

  /**
   * Removes from the next block given the current block and the given key.
   */
  private long removeLeafEntry(long blockIndex,
                               byte []buffer,
                               byte []keyBuffer,
                               int keyOffset,
                               int keyLength)
    throws IOException
  {
    int offset = HEADER_SIZE;
    int tupleSize = _tupleSize;
    int length = getLength(buffer);

    for (int i = 0; i < length; i++) {
      int cmp = _keyCompare.compare(keyBuffer, keyOffset,
                                    buffer, offset + PTR_SIZE,
                                    keyLength);
      
      if (0 < cmp) {
        offset += tupleSize;
        continue;
      }
      else if (cmp == 0) {
        int blockEnd = HEADER_SIZE + length * tupleSize;

        if (offset + tupleSize < blockEnd) {
          if (offset < HEADER_SIZE)
            throw new IllegalStateException();

          System.arraycopy(buffer, offset + tupleSize,
                           buffer, offset,
                           blockEnd - offset - tupleSize);
        }

        setLength(buffer, length - 1);
        
        return i;
      }
      else {
        return 0;
      }
    }

    return 0;
  }

  private void validate(long blockId, byte []buffer)
  {
    boolean isLeaf = isLeaf(buffer);

    if (isLeaf)
      return;
    
    int tupleSize = _tupleSize;
    int length = getLength(buffer);

    int end = HEADER_SIZE + tupleSize * length;
    
    if (length < 0 || BlockStore.BLOCK_SIZE < end) {
      throw new IllegalStateException("illegal length " + length + " for " + debugId(blockId));
    }

    int offset;

    if (false && getPointer(buffer, NEXT_OFFSET) == 0)
      throw new IllegalStateException("Null next pointer for " + debugId(blockId));

    for (offset = HEADER_SIZE;
         offset < end;
         offset += tupleSize) {
      if (getPointer(buffer, offset) == 0)
        throw new IllegalStateException("Null pointer at " + offset + " for " + debugId(blockId) + " tupleSize=" + tupleSize);
    }
  }

  private boolean isLeaf(byte []buffer, Block block)
  {
    int flags = getInt(buffer, FLAGS_OFFSET) & LEAF_MASK;

    if (flags == IS_LEAF)
      return true;
    else if (flags == IS_NODE)
      return false;
    else {
      if (! block.isIndex())
        throw new IllegalStateException(L.l("block {0} is not an index block",
                                            block));
      
      if (! block.isValid())
        throw new IllegalStateException(L.l("block {0} is not valid",
                                            block));
      
      throw new IllegalStateException(L.l("leaf value is invalid: {0} for {1}",
                                          flags, block));
    }
  }

  private void validate(Block block)
  {
    isLeaf(block.getBuffer(), block);
  }

  private void validateIndex(Block block)
  {
    if (block == _rootBlock)
      return;
    
    block.validateIsIndex();
  }

  private boolean isLeaf(byte []buffer)
  {
    int flags = getInt(buffer, FLAGS_OFFSET) & LEAF_MASK;

    if (flags == IS_LEAF)
      return true;
    else if (flags == IS_NODE)
      return false;
    else
      throw new IllegalStateException(L.l("leaf value is invalid: {0}",
                                          flags));
  }

  private void setLeaf(byte []buffer, boolean isLeaf)
  {
    int flags = getInt(buffer, FLAGS_OFFSET) & ~LEAF_MASK;
    
    if (isLeaf)
      setInt(buffer, FLAGS_OFFSET, flags + IS_LEAF);
    else
      setInt(buffer, FLAGS_OFFSET, flags + IS_NODE);
  }
  
  /**
   * Reads an int
   */
  private int getInt(byte []buffer, int offset)
  {
    return (((buffer[offset + 0] & 0xff) << 24) +
            ((buffer[offset + 1] & 0xff) << 16) +
            ((buffer[offset + 2] & 0xff) << 8) +
            ((buffer[offset + 3] & 0xff)));
  }

  /**
   * Reads a pointer.
   */
  private long getPointer(byte []buffer, int offset)
  {
    return (((buffer[offset + 0] & 0xffL) << 56) +
            ((buffer[offset + 1] & 0xffL) << 48) +
            ((buffer[offset + 2] & 0xffL) << 40) +
            ((buffer[offset + 3] & 0xffL) << 32) +
            ((buffer[offset + 4] & 0xffL) << 24) +
            ((buffer[offset + 5] & 0xffL) << 16) +
            ((buffer[offset + 6] & 0xffL) << 8) +
            ((buffer[offset + 7] & 0xffL)));
  }

  /**
   * Sets an int
   */
  private void setInt(byte []buffer, int offset, int value)
  {
    buffer[offset + 0] = (byte) (value >> 24);
    buffer[offset + 1] = (byte) (value >> 16);
    buffer[offset + 2] = (byte) (value >> 8);
    buffer[offset + 3] = (byte) (value);
  }

  /**
   * Sets the length
   */
  private void setLength(byte []buffer, int value)
  {
    if (value < 0 || BLOCK_SIZE / _tupleSize < value) {
      System.out.println("BAD-LENGTH: " + value);
      throw new IllegalArgumentException("BTree: bad length " + value);
    }

    setInt(buffer, LENGTH_OFFSET, value);
  }

  /**
   * Sets the length
   */
  private int getLength(byte []buffer)
  {
    int value = getInt(buffer, LENGTH_OFFSET);
    
    if (value < 0 || value > 65536) {
      System.out.println("BAD-LENGTH: " + value);
      throw new IllegalArgumentException("BTree: bad length " + value);
    }

    return value;
  }

  /**
   * Sets a pointer.
   */
  private void setPointer(byte []buffer, int offset, long value)
  {
    if (offset <= LENGTH_OFFSET)
      System.out.println("BAD_POINTER: " + offset);
    
    buffer[offset + 0] = (byte) (value >> 56);
    buffer[offset + 1] = (byte) (value >> 48);
    buffer[offset + 2] = (byte) (value >> 40);
    buffer[offset + 3] = (byte) (value >> 32);
    buffer[offset + 4] = (byte) (value >> 24);
    buffer[offset + 5] = (byte) (value >> 16);
    buffer[offset + 6] = (byte) (value >> 8);
    buffer[offset + 7] = (byte) (value);
  }

  /**
   * Opens the BTree.
   */
  private void start()
    throws IOException
  {
    synchronized (this) {
      if (_isStarted)
        return;

      _isStarted = true;
    }
  }
  
  /**
   * Testing: returns the keys for a block
   */
  public ArrayList<String> getBlockKeys(long blockIndex)
    throws IOException
  {
    long blockId = _store.addressToBlockId(blockIndex * BLOCK_SIZE);

    if (! _store.isIndexBlock(blockId)) {
      return null;
    }
    
    Block block = _store.readBlock(blockId);

    block.read();
    byte []buffer = block.getBuffer();
      
    int length = getInt(buffer, LENGTH_OFFSET);
    int offset = HEADER_SIZE;
    int tupleSize = _tupleSize;

    ArrayList<String> keys = new ArrayList<String>();
    for (int i = 0; i < length; i++) {
      keys.add(_keyCompare.toString(buffer,
                                    offset + i * tupleSize + PTR_SIZE,
                                    tupleSize - PTR_SIZE));
    }

    block.free();
    
    return keys;
  }
  
  /**
   * Testing: returns the keys for a block
   */
  public long getBlockNext(long blockIndex)
    throws IOException
  {
    long blockId = _store.addressToBlockId(blockIndex * BLOCK_SIZE);

    if (! _store.isIndexBlock(blockId)) {
      return -1;
    }
    
    Block block = _store.readBlock(blockId);

    block.read();
    byte []buffer = block.getBuffer();
      
    long next = getPointer(buffer, NEXT_OFFSET);

    block.free();
    
    return next / BlockStore.BLOCK_SIZE;
  }

  public static BTree createTest(Path path, int keySize)
    throws IOException, java.sql.SQLException
  {
    Database db = new Database();
    db.setPath(path);
    db.init();

    BlockStore store = new BlockStore(db, "test", null);
    store.create();

    Block block = store.allocateIndexBlock();
    long blockId = block.getBlockId();
    block.free();

    return new BTree(store, blockId, keySize, new KeyCompare());
  }

  public static BTree createStringTest(Path path, int keySize)
    throws IOException, java.sql.SQLException
  {
    BlockStore store = BlockStore.create(path);

    Block block = store.allocateIndexBlock();
    long blockId = block.getBlockId();
    block.free();

    return new BTree(store, blockId, keySize, new StringKeyCompare());
  }

  private String debugId(long blockId)
  {
    return Long.toHexString(blockId);
  }

  public void close()
  {
    Block rootBlock = _rootBlock;
    _rootBlock = null;
    
    if (rootBlock != null)
      rootBlock.free();
  }

  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _store + "," + (_rootBlockId / BLOCK_SIZE) + "]");
  }
}
