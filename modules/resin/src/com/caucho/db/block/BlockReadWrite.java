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

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.RandomAccessStream;

/**
 * Filesystem access for the BlockStore.
 */
public class BlockReadWrite {
  private final static Logger log
    = Logger.getLogger(BlockReadWrite.class.getName());
  private final static L10N L = new L10N(BlockReadWrite.class);

  private final BlockStore _store;
  private final BlockManager _blockManager;

  private Path _path;

  private long _fileSize;

  private Object _fileLock = new Object();

  private FreeList<RandomAccessWrapper> _cachedRowFile
    = new FreeList<RandomAccessWrapper>(4);

  private final Semaphore _rowFileSemaphore = new Semaphore(8);

  /**
   * Creates a new store.
   *
   * @param database the owning database.
   * @param name the store name
   * @param lock the table lock
   * @param path the path to the files
   */
  public BlockReadWrite(BlockStore store, Path path)
  {
    _store = store;
    _blockManager = store.getBlockManager();
    _path = path;

    if (path == null)
      throw new NullPointerException();
  }

  /**
   * Returns the file size.
   */
  public long getFileSize()
  {
    return _fileSize;
  }

  /**
   * Creates the store.
   */
  void create()
    throws IOException, SQLException
  {
    _path.getParent().mkdirs();

    if (_path.exists()) {
      throw new SQLException(L.l("CREATE for path '{0}' failed, because the file already exists.  CREATE can not override an existing table.",
                                 _path.getNativePath()));
    }
  }

  boolean isFileExist()
  {
    return _path.exists();
  }

  void init()
    throws IOException
  {
    RandomAccessWrapper wrapper = openRowFile(true);
    boolean isPriority = true;

    try {
      RandomAccessStream file = wrapper.getFile();

      _fileSize = file.getLength();
    } finally {
      closeRowFile(wrapper, isPriority);
    }
  }

  public void remove()
    throws SQLException
  {
    try {
      Path path = _path;
      _path = null;

      close();

      if (path != null)
        path.remove();
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
  }

  /**
   * Reads a block into the buffer.
   */
  public void readBlock(long blockId, byte []buffer, int offset, int length)
    throws IOException
  {
    boolean isPriority = false;
    RandomAccessWrapper wrapper = openRowFile(isPriority);

    try {
      RandomAccessStream is = wrapper.getFile();

      long blockAddress = blockId & BlockStore.BLOCK_MASK;

      if (blockAddress < 0 || _fileSize < blockAddress + length) {
        throw new IllegalStateException(L.l("block at 0x{0} is invalid for file {1} (length 0x{2})",
                                            Long.toHexString(blockAddress),
                                            _path,
                                            Long.toHexString(_fileSize)));
      }

      // System.out.println("READ: " + Long.toHexString(blockAddress));
      int readLen = is.read(blockAddress, buffer, offset, length);

      if (readLen < 0) {
        throw new IllegalStateException("Error reading " + is + " for block " + Long.toHexString(blockAddress) + " result=" + readLen);
      }

      if (readLen < length) {
        System.out.println("BAD-READ: " + Long.toHexString(blockAddress));
        if (readLen < 0)
          readLen = 0;

        for (int i = readLen; i < BlockStore.BLOCK_SIZE; i++)
          buffer[i] = 0;
      }

      _blockManager.addBlockRead();

      freeRowFile(wrapper, isPriority);
      wrapper = null;
    } finally {
      closeRowFile(wrapper, isPriority);
    }
  }

  /**
   * Saves the buffer to the database.
   */
  public void writeBlock(long blockAddress,
                         byte []buffer, int offset, int length,
                         boolean isPriority)
    throws IOException
  {
    RandomAccessWrapper wrapper;

    wrapper = openRowFile(isPriority);

    try {
      RandomAccessStream os = wrapper.getFile();
      /*
      if (blockAddress > 2 * 0x2000000) {
      System.out.println("BLOCK: " + Long.toHexString(blockAddress) + " " + offset);
      Thread.dumpStack();
      }
      */
      if (buffer == null || offset < 0 || length < 0 || buffer.length < offset + length)
        System.out.println("BUFFER: " + buffer + " " + offset + " " + length);

      os.write(blockAddress, buffer, offset, length);

      freeRowFile(wrapper, isPriority);
      wrapper = null;

      synchronized (_fileLock) {
        if (_fileSize < blockAddress + length) {
          _fileSize = blockAddress + length;
        }
      }

      _blockManager.addBlockWrite();
    } finally {
      closeRowFile(wrapper, isPriority);
    }
  }

  /**
   * Opens the underlying file to the database.
   */
  private RandomAccessWrapper openRowFile(boolean isPriority)
    throws IOException
  {
    // limit number of active row files

    if (! isPriority) {
      try {
        Thread.interrupted();
        _rowFileSemaphore.acquire();
      } catch (InterruptedException e) {
        log.log(Level.FINE, e.toString(), e);

        return null;
      }
    }

    RandomAccessWrapper wrapper = null;
    try {
      wrapper = openRowFileImpl();

      return wrapper;
    } catch (RuntimeException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      if (wrapper == null)
        _rowFileSemaphore.release();
    }
  }

  /**
   * Opens the underlying file to the database.
   */
  private RandomAccessWrapper openRowFileImpl()
    throws IOException
  {
    RandomAccessStream file = null;
    RandomAccessWrapper wrapper = null;

    // SoftReference<RandomAccessWrapper> ref = _cachedRowFile.allocate();
    wrapper = _cachedRowFile.allocate();

    /*
    if (ref != null) {
      wrapper = ref.get();
    }
    */

    if (wrapper != null)
      file = wrapper.getFile();

    if (file == null) {
      Path path = _path;

      if (path != null) {
        file = path.openRandomAccess();

        wrapper = new RandomAccessWrapper(file);
      }
    }

    return wrapper;
  }

  private void freeRowFile(RandomAccessWrapper wrapper, boolean isPriority)
    throws IOException
  {
    if (wrapper == null)
      return;

    if (! isPriority)
      _rowFileSemaphore.release();

    /*
    SoftReference<RandomAccessWrapper> fileRef
      = new SoftReference<RandomAccessWrapper>(wrapper);
      */

    if (_cachedRowFile.free(wrapper)) {
      return;
    }

    wrapper.close();
  }

  private void closeRowFile(RandomAccessWrapper wrapper, boolean isPriority)
    throws IOException
  {
    if (wrapper == null)
      return;

    if (! isPriority)
      _rowFileSemaphore.release();

    wrapper.close();
  }

  /**
   * Closes the store.
   */
  void close()
  {
    _path = null;

    RandomAccessWrapper wrapper = null;

    /*
    SoftReference<RandomAccessWrapper> ref = _cachedRowFile.allocate();

    if (ref != null)
      wrapper = ref.get();
      */
    wrapper = _cachedRowFile.allocate();

    if (wrapper != null) {
      try {
        wrapper.close();
      } catch (Throwable e) {
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _store.getId() + "]";
  }

  static class RandomAccessWrapper {
    private RandomAccessStream _file;

    RandomAccessWrapper(RandomAccessStream file)
    {
      _file = file;
    }

    RandomAccessStream getFile()
    {
      return _file;
    }

    void close()
      throws IOException
    {
      RandomAccessStream file = _file;
      _file = null;

      if (file != null)
        file.close();
    }
  }
}
