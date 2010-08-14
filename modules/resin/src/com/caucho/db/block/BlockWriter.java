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

package com.caucho.db.block;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.thread.TaskWorker;
import com.caucho.util.Alarm;

/**
 * Writer thread serializing dirty blocks.
 */
public class BlockWriter extends TaskWorker {
  private final static Logger log
    = Logger.getLogger(BlockWriter.class.getName());
  
  private final BlockStore _store;
  
  private int _writeQueueMax = 256;
  private final ArrayList<Block> _writeQueue = new ArrayList<Block>();
  
  BlockWriter(BlockStore store)
  {
    _store = store;
    
    store.getReadWrite();
  }

  /**
   * Adds a block that's needs to be flushed.
   */
  void addDirtyBlock(Block block)
  {
    synchronized (_writeQueue) {
      if (_writeQueueMax < _writeQueue.size()) {
        wake();
        
        try {
          _writeQueue.wait(100);
        } catch (InterruptedException e) {
        }
      }

      _writeQueue.add(block);
    }

    wake();
  }

  boolean copyDirtyBlock(long blockId, Block block)
  {
    Block writeBlock = null;
    
    synchronized (_writeQueue) {
      int size = _writeQueue.size();

      // search from newest to oldest in case multiple writes
      for (int i = size - 1; i >= 0; i--) {
        Block testBlock = _writeQueue.get(i);

        if (testBlock.getBlockId() == blockId) {
          writeBlock = testBlock;
          break;
        }
      }
    }
    
    if (writeBlock != null)
      return writeBlock.copyToBlock(block);
    else
      return false;
  }
  
  @Override
  public boolean isClosed()
  {
    return super.isClosed() && _writeQueue.size() == 0;
  }
  
  void waitForComplete(long timeout)
  {
    long expires = Alarm.getCurrentTimeActual() + timeout;
    
    synchronized (_writeQueue) {
      while (_writeQueue.size() > 0) {
        wake();
        
        long now = Alarm.getCurrentTimeActual();
        
        long delta = now - expires;
        
        if (delta <= 0)
          return;
        
        try {
          _writeQueue.wait(delta);
        } catch (Exception e) {
          
        }
      }
    }
  }
  
  @Override
  public long runTask()
  {
    try {
      int retryMax = 10;
      int retry = retryMax;

      while (true) {
        Block block = peekFirstBlock();

        if (block != null) {
          retry = retryMax;

          try {
            block.writeFromBlockWriter();
          } finally {
            removeFirstBlock();
          }
        }
        else if (retry-- <= 0) {
          return -1;
        }
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    
    return -1;
  }
  
  @Override
  protected void onThreadStart()
  {
  }
  
  @Override
  protected void onThreadComplete()
  {
  }

  private Block peekFirstBlock()
  {
    synchronized (_writeQueue) {
      if (_writeQueue.size() > 0) {
        Block block = _writeQueue.get(0);
        
        return block;
      }
    }
    
    return null;
  }

  private void removeFirstBlock()
  {
    synchronized (_writeQueue) {
      if (_writeQueue.size() > 0) {
        _writeQueue.remove(0);
        
        _writeQueue.notifyAll();
      }
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _store + "]";
  }
}
