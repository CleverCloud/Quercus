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

package com.caucho.vfs;

import java.io.*;

public class TempOutputStream extends OutputStream
{
  private TempBuffer _head;
  private TempBuffer _tail;

  public byte []getTail()
  {
    return _tail.getBuffer();
  }

  @Override
  public void write(int ch)
  {
    if (_tail == null)
      addBuffer(TempBuffer.allocate());
    else if (_tail._buf.length <= _tail._length)
      addBuffer(TempBuffer.allocate());

    _tail.getBuffer()[_tail._length++] = (byte) ch;
  }

  @Override
  public void write(byte []buf, int offset, int length)
  {
    int index = 0;

    TempBuffer tail = _tail;
    int bufferSize = TempBuffer.SIZE;
    int tailLength;

    if (tail != null)
      tailLength = tail._length;
    else
      tailLength = 0;

    while (index < length) {
      if (tail == null) {
        addBuffer(TempBuffer.allocate());
        tail = _tail;
        tailLength = tail._length;
      }
      else if (bufferSize <= tailLength) {
        addBuffer(TempBuffer.allocate());
        tail = _tail;
        tailLength = tail._length;
      }

      int sublen = bufferSize - tailLength;
      if (length - index < sublen)
        sublen = length - index;

      System.arraycopy(buf, index + offset, tail._buf, tailLength, sublen);

      index += sublen;
      tailLength += sublen;
      tail._length = tailLength;
    }
  }

  public void write(byte []buffer)
  {
    write(buffer, 0, buffer.length);
  }

  private void addBuffer(TempBuffer buf)
  {
    buf._next = null;
    if (_tail != null) {
      _tail._next = buf;
      _tail = buf;
    } else {
      _tail = buf;
      _head = buf;
    }

    _head._bufferCount++;
  }

  @Override
  public void flush()
  {
  }

  @Override
  public void close()
  {
  }

  /**
   * Opens a read stream to the buffer.
   */
  public ReadStream openRead()
    throws IOException
  {
    close();

    TempReadStream read = new TempReadStream(_head);
    read.setFreeWhenDone(true);
    _head = null;
    _tail = null;

    return new ReadStream(read);
  }

  /**
   * Opens a read stream to the buffer.
   */
  public InputStream getInputStream()
    throws IOException
  {
    close();

    TempBuffer head = _head;
    _head = null;
    _tail = null;

    return new TempInputStream(head);
  }

  /**
   * Opens a read stream to the buffer.
   */
  public InputStream openInputStream()
    throws IOException
  {
    close();

    TempBuffer head = _head;
    _head = null;
    _tail = null;

    return new TempInputStream(head);
  }

  /**
   * Opens a read stream to the buffer.
   */
  public InputStream openInputStreamNoFree()
    throws IOException
  {
    close();

    TempBuffer head = _head;

    return new TempInputStreamNoFree(head);
  }

  /**
   * Returns the head buffer.
   */
  public TempBuffer getHead()
  {
    return _head;
  }

  /**
   * clear without removing
   */
  public void clear()
  {
    _head = null;
    _tail = null;
  }

  public void writeToStream(OutputStream os)
    throws IOException
  {
    for (TempBuffer ptr = _head; ptr != null; ptr = ptr._next) {
      os.write(ptr.getBuffer(), 0, ptr.getLength());
    }
  }

  /**
   * Returns the total length of the buffer's bytes
   */
  public int getLength()
  {
    int length = 0;

    for (TempBuffer ptr = _head; ptr != null; ptr = ptr._next) {
      length += ptr.getLength();
    }

    return length;
  }

  public byte []toByteArray()
  {
    int len = getLength();
    byte []data = new byte[len];

    int offset = 0;
    for (TempBuffer ptr = _head; ptr != null; ptr = ptr._next) {
      System.arraycopy(ptr.getBuffer(), 0, data, offset, ptr.getLength());
      offset += ptr.getLength();
    }

    return data;
  }

  public void readAll(int position, char []buffer, int offset, int length)
  {
    int len = getLength();

    TempBuffer ptr = _head;

    for (; ptr != null && ptr.getLength() <= position; ptr = ptr._next) {
      position -= ptr.getLength();
    }

    if (ptr == null)
      return;

    for (; ptr != null && length >= 0; ptr = ptr._next) {
      int sublen = ptr.getLength() - position;

      if (length < sublen)
        sublen = length;

      byte []dataBuffer = ptr.getBuffer();
      int tail = position + sublen;

      for (; position < tail; position++) {
        buffer[offset++] = (char) (dataBuffer[position] & 0xff);
      }

      length -= sublen;
    }
  }

  public void readAll(int position, byte []buffer, int offset, int length)
  {
    int len = getLength();

    TempBuffer ptr = _head;

    for (; ptr != null && ptr.getLength() <= position; ptr = ptr._next) {
      position -= ptr.getLength();
    }

    if (ptr == null)
      return;

    for (; ptr != null && length >= 0; ptr = ptr._next) {
      int sublen = ptr.getLength() - position;

      if (length < sublen)
        sublen = length;

      byte []dataBuffer = ptr.getBuffer();

      System.arraycopy(dataBuffer, position, buffer, offset, sublen);

      offset += sublen;
      position = 0;
      length -= sublen;
    }
  }

  /**
   * Clean up the temp stream.
   */
  public void destroy()
  {
    TempBuffer ptr = _head;

    _head = null;
    _tail = null;

    TempBuffer.freeAll(ptr);
  }
}
