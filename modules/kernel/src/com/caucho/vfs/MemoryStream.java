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

package com.caucho.vfs;

import java.io.IOException;
import java.io.OutputStream;

public class MemoryStream extends StreamImpl {
  private TempBuffer _head;
  private TempBuffer _tail;

  @Override
  public Path getPath() { return new NullPath("temp:"); }

  /**
   * A memory stream is writable.
   */
  @Override
  public boolean canWrite()
  {
    return true;
  }
  
  /**
   * Writes a buffer to the underlying stream.
   *
   * @param buffer the byte array to write.
   * @param offset the offset into the byte array.
   * @param length the number of bytes to write.
   * @param isEnd true when the write is flushing a close.
   */
  @Override
  public void write(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    while (offset < length) {
      if (_tail == null || _tail._length >= _tail._buf.length)
        addBuffer(TempBuffer.allocate());

      int sublen = _tail._buf.length - _tail._length;
      if (length - offset < sublen)
        sublen = length - offset;

      System.arraycopy(buf, offset, _tail._buf, _tail._length, sublen);

      offset += sublen;
      _tail._length += sublen;
    }
  }

  private void addBuffer(TempBuffer buf)
  {
    buf._next = null;
    buf._length = 0;
    if (_tail != null) {
      _tail._next = buf;
      _tail = buf;
    } else {
      _tail = buf;
      _head = buf;
    }
    _head._bufferCount++;
  }

  public void writeToStream(OutputStream os) throws IOException
  {
    for (TempBuffer node = _head; node != null; node = node._next) {
      os.write(node._buf, 0, node._length);
    } 
  }

  public int getLength()
  {
    if (_tail == null)
      return 0;
    else
      return (_head._bufferCount - 1) * _head._length + _tail._length;
  }

  public ReadStream openReadAndSaveBuffer()
    throws IOException
  {
    close();

    TempReadStream read = new TempReadStream(_head);
    read.setFreeWhenDone(false);

    return new ReadStream(read);
  }

  public void destroy()
  {
    TempBuffer ptr;
    TempBuffer next;

    ptr = _head;
    _head = null;
    _tail = null;
    
    for (; ptr != null; ptr = next) {
      next = ptr._next;
      TempBuffer.free(ptr);
      ptr = null;
    }
  }
}
