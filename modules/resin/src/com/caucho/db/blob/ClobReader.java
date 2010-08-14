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

import java.io.IOException;
import java.io.Reader;

import com.caucho.db.block.BlockStore;

public class ClobReader extends Reader {
  private BlockStore _store;

  private long _offset;

  private byte []_inode;
  private int _inodeOffset;

  private char []_buffer;

  /**
   * Creates a clob reader.
   *
   * @param store the backing store
   */
  public ClobReader(BlockStore store, byte []inode, int inodeOffset)
  {
    init(store, inode, inodeOffset);
  }
  
  /**
   * Creates a clob reader.
   *
   * @param store the backing store
   */
  public ClobReader(Inode inode)
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

    readLong(inode, inodeOffset);
    _offset = 0;
  }

  /**
   * Reads a char.
   */
  public int read()
    throws IOException
  {
    if (_buffer == null)
      _buffer = new char[1];

    int len = read(_buffer, 0, 1);

    if (len < 0)
      return -1;
    else
      return _buffer[0];
  }

  /**
   * Reads a buffer.
   */
  public int read(char []buf, int offset, int length)
    throws IOException
  {
    int sublen = Inode.read(_inode, _inodeOffset,
                            _store, _offset,
                            buf, offset, length);

    if (sublen > 0)
      _offset += 2 * sublen;

    return sublen;
  }

  /**
   * Closes the buffer.
   */
  public void close()
  {
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
}
