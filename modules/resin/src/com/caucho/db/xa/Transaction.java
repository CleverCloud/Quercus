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

package com.caucho.db.xa;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.db.blob.Inode;
import com.caucho.db.block.Block;
import com.caucho.db.block.BlockStore;
import com.caucho.db.jdbc.ConnectionImpl;
import com.caucho.db.lock.Lock;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.L10N;

/**
 * Represents a single transaction.
 */
public class Transaction extends StoreTransaction {
  private static final Logger log
    = Logger.getLogger(Transaction.class.getName());
  private static final L10N L = new L10N(Transaction.class);

  private static long AUTO_COMMIT_TIMEOUT = 30000L;

  private boolean _isAutoCommit = true;
  private ConnectionImpl _conn;
  
  private ArrayList<Lock> _readLocks;
  private ArrayList<Lock> _writeLocks;
  
  private ArrayList<Block> _updateBlocks;

  // inodes that need to be deleted on a commit
  private ArrayList<Inode> _deleteInodes;
  
  // inodes that need to be deleted on a rollback
  private ArrayList<Inode> _addInodes;
  
  // blocks that need deallocating on a commit
  private ArrayList<Block> _deallocateBlocks;

  private boolean _isRollbackOnly;
  private SQLException _rollbackExn;

  private long _timeout = AUTO_COMMIT_TIMEOUT;

  private Transaction()
  {
  }

  public static Transaction create(ConnectionImpl conn)
  {
    Transaction xa = new Transaction();
    
    xa.init(conn);

    return xa;
  }

  public static Transaction create()
  {
    Transaction xa = new Transaction();

    return xa;
  }

  private void init(ConnectionImpl conn)
  {
    _conn = conn;
    _timeout = AUTO_COMMIT_TIMEOUT;
    _isRollbackOnly = false;
    _rollbackExn = null;
  }

  /**
   * Sets the transaction timeout.
   */
  public void setTimeout(long timeout)
  {
    _timeout = timeout;
  }

  public long getTimeout()
  {
    return _timeout;
  }
  
  /**
   * Acquires a new read lock.
   */
  /*
  public void addReadLock(Lock lock)
  {
    _readLocks.add(lock);
  }
  */
  
  /**
   * Acquires a new read lock.
   */
  public boolean hasReadLock(Lock lock)
  {
    return _readLocks.contains(lock);
  }

  /**
   * Returns true for an auto-commit transaction.
   */
  public boolean isAutoCommit()
  {
    return _isAutoCommit;
  }

  /**
   * Returns true for an auto-commit transaction.
   */
  public void setAutoCommit(boolean autoCommit)
  {
    _isAutoCommit = autoCommit;
  }
  
  /**
   * Acquires a new write lock.
   */
  public void lockRead(Lock lock)
    throws SQLException
  {
    if (_isRollbackOnly) {
      if (_rollbackExn != null)
        throw _rollbackExn;
      else
        throw new SQLException(L.l("can't get lock with rollback transaction"));
    }

    try {
      if (_readLocks == null)
        _readLocks = new ArrayList<Lock>();
      
      if (_readLocks.contains(lock))
        throw new SQLException(L.l("lockRead must not already have a read lock"));
      
      lock.lockRead(_timeout);
      _readLocks.add(lock);
    } catch (SQLException e) {
      setRollbackOnly(e);
      
      throw e;
    }
  }

  /**
   * Acquires a new write lock.
   */
  public void lockReadAndWrite(Lock lock)
    throws SQLException
  {
    if (_isRollbackOnly) {
      if (_rollbackExn != null)
        throw _rollbackExn;
      else
        throw new SQLException(L.l("can't get lock with rollback transaction"));
    }

    try {
      if (_readLocks == null)
        _readLocks = new ArrayList<Lock>();
      if (_writeLocks == null)
        _writeLocks = new ArrayList<Lock>();

      if (_readLocks.contains(lock))
        throw new SQLException(L.l("lockReadAndWrite cannot already have a read lock"));

      if (_writeLocks.contains(lock))
        throw new SQLException(L.l("lockReadAndWrite cannot already have a write lock"));
      
      lock.lockReadAndWrite(_timeout);
      _readLocks.add(lock);
      _writeLocks.add(lock);
    } catch (SQLException e) {
      setRollbackOnly(e);
      
      throw e;
    }
  }

  /**
   * Conditionally a new write lock, if no contention exists.
   */
  public boolean lockReadAndWriteNoWait(Lock lock)
    throws SQLException
  {
    if (_isRollbackOnly) {
      if (_rollbackExn != null)
        throw _rollbackExn;
      else
        throw new SQLException(L.l("can't get lock with rollback transaction"));
    }

    try {
      if (_readLocks == null)
        _readLocks = new ArrayList<Lock>();
      if (_writeLocks == null)
        _writeLocks = new ArrayList<Lock>();

      if (_readLocks.contains(lock))
        throw new SQLException(L.l("lockReadAndWrite cannot already have a read lock"));

      if (_writeLocks.contains(lock))
        throw new SQLException(L.l("lockReadAndWrite cannot already have a write lock"));
      
      if (lock.lockReadAndWriteNoWait()) {
        _readLocks.add(lock);
        _writeLocks.add(lock);

        return true;
      }
    } catch (SQLException e) {
      setRollbackOnly(e);
      
      throw e;
    }

    return false;
  }

  /**
   * Adds a block for update.
   */
  public void addUpdateBlock(Block block)
  {
    if (block == null)
      return;
    
    if (_updateBlocks == null)
      _updateBlocks = new ArrayList<Block>();

    if (_updateBlocks.size() == 0
        || _updateBlocks.get(_updateBlocks.size() - 1) != block)
      _updateBlocks.add(block);
  }
  
  /**
   * If auto-commit, commit the read
   */
  public void autoCommitRead(Lock lock)
    throws SQLException
  {
    unlockRead(lock);
  }
  
  public void unlockRead(Lock lock)
    throws SQLException
  {
    if (_readLocks.remove(lock))
      lock.unlockRead();
  }
  
  /**
   * If auto-commit, commit the write
   */
  public void autoCommitWrite(Lock lock)
    throws SQLException
  {
    _readLocks.remove(lock);

    if (_writeLocks.remove(lock)) {
      try {
        commit();
      } finally {
        // lock.unlockWrite();
        lock.unlockReadAndWrite();
      }
    }
  }
  
  public void unlockReadAndWrite(Lock lock)
    throws SQLException
  {
    _readLocks.remove(lock);
    
    if (_writeLocks.remove(lock)) {
      lock.unlockReadAndWrite();
    }
  }

  /**
   * Returns a read block.
   */
  public Block readBlock(BlockStore store, long blockAddress)
    throws IOException
  {
    long blockId = store.addressToBlockId(blockAddress);
      
    Block block = null;

    if (block != null)
      block.allocate();
    else
      block = store.readBlock(blockId);

    return block;
  }

  /**
   * Returns a read block.
   */
  public Block loadBlock(BlockStore store, long blockAddress)
    throws IOException
  {
    long blockId = store.addressToBlockId(blockAddress);
      
    Block block = store.loadBlock(blockId);

    return block;
  }

  /**
   * Returns a modified block.
   */
  public Block allocateRow(BlockStore store)
    throws IOException
  {
    return store.allocateRow();
  }

  /**
   * Returns a modified block.
   */
  public void deallocateBlock(Block block)
    throws IOException
  {
    if (isAutoCommit())
      block.getStore().freeBlock(block.getBlockId());
    else {
      if (_deallocateBlocks == null)
        _deallocateBlocks = new ArrayList<Block>();
      
      _deallocateBlocks.add(block);
    }
  }

  /**
   * Adds inode which should be deleted on a commit.
   */
  public void addDeleteInode(Inode inode)
  {
    if (_deleteInodes == null)
      _deleteInodes = new ArrayList<Inode>();
    
    _deleteInodes.add(inode);
  }

  /**
   * Adds inode which should be deleted on a rollback.
   */
  public void addAddInode(Inode inode)
  {
    if (_addInodes == null)
      _addInodes = new ArrayList<Inode>();
    
    _addInodes.add(inode);
  }

  public void autoCommit()
    throws SQLException
  {
    if (_isAutoCommit) {
      ConnectionImpl conn = _conn;
      _conn = null;
      
      if (conn != null) {
        conn.setTransaction(null);
      }
    }
  }

  public void setRollbackOnly(SQLException e)
  {
    if (_rollbackExn == null)
      _rollbackExn = e;
    
    _isRollbackOnly = true;

    releaseLocks();
  }

  public void setRollbackOnly()
  {
    setRollbackOnly(null);
  }

  public void commit()
    throws SQLException
  {
    try {
      writeData();
    } finally {
      releaseLocks();

      close();
    }
  }

  public void writeData()
    throws SQLException
  {
    if (_deleteInodes != null) {
      while (_deleteInodes.size() > 0) {
        Inode inode = _deleteInodes.remove(0);

        // XXX: should be allocating based on auto-commit
        inode.remove();
      }
    }

    ArrayList<Block> updateBlocks = _updateBlocks;
    _updateBlocks = null;
    
    if (updateBlocks != null) {
      while (updateBlocks.size() > 0) {
        Block block = updateBlocks.remove(updateBlocks.size() - 1);

        try {
          block.getStore().saveAllocation();
        } catch (IOException e) {
          log.log(Level.WARNING, e.toString(), e);
        }
        
        try {
          block.commit();
        } catch (IOException e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    }

    if (_deallocateBlocks != null) {
      while (_deallocateBlocks.size() > 0) {
        Block block = _deallocateBlocks.remove(0);

        try {
          block.getStore().freeBlock(block.getBlockId());
        } catch (IOException e) {
          throw new SQLExceptionWrapper(e);
        }
      }
    }
  }

  public void rollback()
    throws SQLException
  {
    releaseLocks();

    close();
  }

  private void releaseLocks()
  {
    // need to unlock write before upgrade to block other threads
    if (_writeLocks != null) {
      for (int i = 0; i < _writeLocks.size(); i++) {
        Lock lock = _writeLocks.get(i);

        if (_readLocks != null)
          _readLocks.remove(lock);

        try {
          lock.unlockReadAndWrite();
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      _writeLocks.clear();
    }
    
    if (_readLocks != null) {
      for (int i = 0; i < _readLocks.size(); i++) {
        Lock lock = _readLocks.get(i);

        try {
          lock.unlockRead();
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      _readLocks.clear();
    }
  }

  void close()
  {
    _isRollbackOnly = false;
    _rollbackExn = null;
  }
}
