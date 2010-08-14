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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.db.blob;

import java.io.IOException;
import java.io.InputStream;

import com.caucho.db.block.Block;
import com.caucho.db.block.BlockStore;

/**
 * Directly reading the blob from the inode.
 */
public class InodeBlobInputStream extends InputStream {
  private static final int INODE_DIRECT_BLOCKS = 14;
    
  private BlockStore _store;

  private long _length;
  private long _offset;

  private byte []_inode;
  private int _inodeOffset;

  private Block _block;
  private byte []_buffer;
  private int _bufferOffset;
  private int _bufferEnd;
  
  /**
   * Creates a blob output stream.
   *
   * @param store the output store
   */
  public InodeBlobInputStream(BlockStore store, byte []inode, int inodeOffset)
  {
    init(store, inode, inodeOffset);
  }
  
  /**
   * Creates a blob output stream.
   *
   * @param store the output store
   */
  public InodeBlobInputStream(Inode inode)
  {
    init(inode.getStore(), inode.getBuffer(), 0);
  }

  /**
   * Initialize the output stream.
   */
  public void init(BlockStore store, byte []inode, int inodeOffset)
  {
    if (store == null)
      throw new NullPointerException();
    
    _store = store;

    _inode = inode;
    _inodeOffset = inodeOffset;

    _length = readLong(inode, inodeOffset);
    _offset = 0;
    
    _block = null;

    if (_length <= Inode.INLINE_BLOB_SIZE) {
      _buffer = inode;
      _bufferOffset = inodeOffset + 8;
      _bufferEnd = (int) (_bufferOffset + _length);
    }
    else {
      _buffer = null;
      _bufferOffset = 0;
      _bufferEnd = 0;
    }
  }

  /**
   * Reads a byte.
   */
  public int read()
    throws IOException
  {
    if (_length <= _offset)
      return -1;

    if (_bufferEnd <= _bufferOffset)
      readBlock();

    _offset++;

    return _buffer[_bufferOffset++] & 0xff;
  }

  /**
   * Reads a buffer.
   */
  public int read(byte []buf, int offset, int length)
    throws IOException
  {
    if (_length <= _offset)
      return -1;

    if (_bufferEnd <= _bufferOffset)
      readBlock();

    int sublen = _bufferEnd - _bufferOffset;
    if (length < sublen)
      sublen = length;

    _offset += sublen;

    System.arraycopy(_buffer, _bufferOffset, buf, offset, sublen);

    _bufferOffset += sublen;

    return sublen;
  }

  /**
   * Closes the buffer.
   */
  public void close()
  {
    if (_block != null) {
      Block block = _block;
      _block = null;
      block.free();
    }
  }

  /**
   * Updates the buffer.
   */
  public void readBlock()
    throws IOException
  {
    if (_block != null) {
      Block block = _block;
      _block = null;
      block.free();
    }

    long addr;

    int blockCount = (int) (_offset / BlockStore.BLOCK_SIZE);
      
    if (blockCount < INODE_DIRECT_BLOCKS) {
      addr = readLong(_inode, _inodeOffset + 8 * (blockCount + 1));
    }
    else {
      long ptrAddr = readLong(_inode,
                              _inodeOffset + 8 * (INODE_DIRECT_BLOCKS + 1));

      Block ptr = _store.readBlock(_store.addressToBlockId(ptrAddr));

      addr = readLong(ptr.getBuffer(), 8 * (blockCount - INODE_DIRECT_BLOCKS));

      ptr.free();
    }

    _block = _store.readBlock(_store.addressToBlockId(addr));
    _buffer = _block.getBuffer();

    int offset = (int) (addr & BlockStore.BLOCK_OFFSET_MASK);

    if (offset > 0) {
      _bufferOffset = readShort(_buffer, offset);
      _bufferEnd = _bufferOffset + readShort(_buffer, offset + 2);
    }
    else {
      _bufferOffset = 0;
      _bufferEnd = _buffer.length;
    }
  }

  /**
   * Writes the long.
   */
  public static long readLong(byte []buffer, int offset)
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
   * Writes the short.
   */
  private static int readShort(byte []buffer, int offset)
  {
    return (((buffer[offset + 0] & 0xff) << 8) +
            ((buffer[offset + 1] & 0xff)));
  }
}
