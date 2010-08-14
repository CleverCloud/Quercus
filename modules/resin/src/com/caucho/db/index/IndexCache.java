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

import com.caucho.util.*;
import com.caucho.db.xa.Transaction;
import com.caucho.env.thread.TaskWorker;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.*;
import java.sql.SQLException;

/**
 * Manages the block cache
 */
public final class IndexCache
{
  private static final Logger log
    = Logger.getLogger(IndexCache.class.getName());
  private static final L10N L = new L10N(IndexCache.class);

  private static IndexCache _staticCache;

  private final LruCache<IndexKey,IndexKey> _cache;

  private final ArrayList<IndexKey> _writeQueue
    = new ArrayList<IndexKey>();

  private final AtomicReference<IndexKey> _freeKey
    = new AtomicReference<IndexKey>();

  private IndexCacheWriter _indexWriter = new IndexCacheWriter();

  private IndexCache(int capacity)
  {
    _cache = new LruCache<IndexKey,IndexKey>(capacity);
  }

  /**
   * Returns the block manager, ensuring a minimum number of entries.
   */
  public static IndexCache create()
  {
    if (_staticCache == null) {
      int size;
      
      if (Alarm.isTest())
        size = 8 * 1024;
      else
        size = 64 * 1024;
      
      _staticCache = new IndexCache(size);
    }

    return _staticCache;
  }

  public static IndexCache getCurrent()
  {
    return _staticCache;
  }

  /**
   * Gets the index entry.
   */
  public long lookup(BTree btree,
                     byte []buffer, int offset, int length,
                     Transaction xa)
    throws SQLException
  {
    IndexKey value = lookupValue(btree, buffer, offset, length);

    if (value != null && value.isValid()) {
      return value.getValue();
    }

    long btreeValue;
    
    try {
      btreeValue = btree.lookup(buffer, offset, length);
    } catch (IOException e) {
      throw new SQLException(e);
    }

    value = IndexKey.create(btree, buffer, offset, length, btreeValue);
    value.setValid(true);

    _cache.compareAndPut(null, value, value);

    return btreeValue;
  }

  /**
   * Gets the index entry.
   */
  public void insert(BTree btree,
                     byte []buffer, int offset, int length,
                     long value,
                     Transaction xa)
    throws SQLException
  {
    IndexKey key = IndexKey.create(btree, buffer, offset, length, value);

    if (! _cache.compareAndPut(null, key, key)) {
      // XXX:
      throw new SQLException(L.l("duplicate key exception"));
    }

    long btreeValue;
    
    try {
      btreeValue = btree.lookup(buffer, offset, length);
    } catch (IOException e) {
      throw new SQLException(e);
    }

    if (btreeValue != 0) {
      key.setValue(btreeValue);
      key.setValid(true);
      
      throw new SQLException(L.l("duplicate key exception"));
    }
    
    key.setValid(true);
  }

  /**
   * Remove the index entry.
   */
  public void delete(BTree btree,
                     byte []buffer, int offset, int length,
                     Transaction xa)
    throws SQLException
  {
    IndexKey value = lookupValue(btree, buffer, offset, length);

    if (value != null) {
      value.setValue(0); // any updates will get written by the thread
    }
    else {
      btree.remove(buffer, offset, length);
    }
  }

  private IndexKey lookupValue(BTree btree,
                               byte []buffer, int offset, int length)
  {
    IndexKey key = _freeKey.getAndSet(null);

    if (key == null)
      key = new IndexKey();

    key.init(btree, buffer, offset, length);

    IndexKey value = _cache.get(key);

    if (value == null)  {
      synchronized (_writeQueue) {
        int size = _writeQueue.size();

        for (int i = 0; i < size; i++) {
          IndexKey writeKey = _writeQueue.get(i);

          if (key.equals(writeKey)) {
            value = writeKey;
            _cache.compareAndPut(null, value, value);
          }
        }
      }
    }

    _freeKey.set(key);

    return value;
  }

  /**
   * Adds a block that's needs to be flushed.
   */
  void addWrite(IndexKey key)
  {
    key.setStored(true);
    
    while (_writeQueue.size() > 1024) {
      _indexWriter.wake();
    
      synchronized (_writeQueue) {
        if (_writeQueue.size() > 1024) {
          try {
            _writeQueue.wait(1000);
          } catch (Exception e) {
            log.log(Level.FINEST, e.toString(), e);
          }
        }
      }
    }

    synchronized (_writeQueue) {
      _writeQueue.add(key);
    }
    
    _indexWriter.wake();
  }

  class IndexCacheWriter extends TaskWorker {
    public long runTask()
    {
      Transaction xa = Transaction.create();
      
      try {
        IndexKey key = null;

        Thread.interrupted();

        synchronized (_writeQueue) {
          if (_writeQueue.size() == 0)
            return -1;

          key = _writeQueue.get(0);
        }

        if (key != null) {
          BTree btree = key.getBTree();
          long value = key.getValue();

          if (! key.isStored()) {
          }
          else if (value != 0) {
            btree.insert(key.getBuffer(), key.getOffset(), key.getLength(),
                         value, true);
          }
          else {
            btree.remove(key.getBuffer(), key.getOffset(), key.getLength());
          }
        }

        synchronized (_writeQueue) {
          if (key != null)
            _writeQueue.remove(0);

          _writeQueue.notify();
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
      
      return -1;
    }
  }
}
