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

package com.caucho.db.blob;

import com.caucho.db.block.BlockStore;
import com.caucho.db.xa.RawTransaction;
import com.caucho.db.xa.StoreTransaction;
import com.caucho.db.xa.Transaction;
import com.caucho.vfs.TempBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

public class BlobOutputStream extends OutputStream {
  private StoreTransaction _xa;
  private BlockStore _store;
  
  private TempBuffer _tempBuffer;
  private byte []_buffer;
  private int _offset;
  private int _bufferEnd;

  private Inode _inode;

  private byte []_inodeBuffer;
  private int _inodeOffset;
  
  /**
   * Creates a blob output stream.
   *
   * @param store the output store
   */
  public BlobOutputStream(Transaction xa, BlockStore store,
                          byte []inode, int inodeOffset)
  {
    init(store, inode, inodeOffset);

    _xa = xa;
  }
  
  /**
   * Creates a blob output stream.
   *
   * @param store the output store
   */
  public BlobOutputStream(BlockStore store, byte []inode, int inodeOffset)
  {
    init(store, inode, inodeOffset);
  }
  
  /**
   * Creates a blob output stream.
   *
   * @param store the output store
   */
  BlobOutputStream(Inode inode)
  {
    init(inode.getStore(), inode.getBuffer(), 0);

    _inode = inode;
  }

  /**
   * Initialize the output stream.
   */
  public void init(BlockStore store, byte []inode, int inodeOffset)
  {
    _store = store;
    _xa = RawTransaction.create();

    _inodeBuffer = inode;
    _inodeOffset = inodeOffset;

    Inode.clear(_inodeBuffer, _inodeOffset);

    _offset = 0;

    if (_tempBuffer == null) {
      _tempBuffer = TempBuffer.allocateLarge();
      _buffer = _tempBuffer.getBuffer();
      _bufferEnd = _buffer.length;
    }
  }

  /**
   * Writes a byte.
   */
  public void write(int v)
    throws IOException
  {
    if (_bufferEnd <= _offset) {
      flushBlock();
    }

    _buffer[_offset++] = (byte) v;
  }

  /**
   * Writes a buffer.
   */
  @Override
  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    while (length > 0) {
      if (_bufferEnd <= _offset) {
        flushBlock();
      }

      int sublen = _bufferEnd - _offset;
      if (length < sublen)
        sublen = length;

      System.arraycopy(buffer, offset, _buffer, _offset, sublen);

      offset += sublen;
      _offset += sublen;

      length -= sublen;
    }
  }

  public void writeFromStream(InputStream is)
    throws IOException
  {
    while (true) {
      if (_bufferEnd <= _offset) {
        flushBlock();
      }

      int sublen = _bufferEnd - _offset;

      sublen = is.read(_buffer, _offset, sublen);

      if (sublen < 0)
        return;

      _offset += sublen;
    }
  }

  /**
   * Completes the stream.
   */
  @Override
  public void close()
    throws IOException
  {
    try {
      if (_tempBuffer == null)
        return;
      
      flushBlock();
    } finally {
      Inode inode = _inode;
      _inode = null;
      
      if (inode != null)
        inode.closeOutputStream();

      _inodeBuffer = null;

      TempBuffer tempBuffer = _tempBuffer;
      _tempBuffer = null;

      if (tempBuffer != null) {
        TempBuffer.freeLarge(tempBuffer);
      }
    }
  }

  /**
   * Updates the buffer.
   */
  private void flushBlock()
    throws IOException
  {
    int length = _offset;
    _offset = 0;
    
    Inode.append(_inodeBuffer, _inodeOffset,
                 _store, _xa,
                 _buffer, 0, length);
  }
}
