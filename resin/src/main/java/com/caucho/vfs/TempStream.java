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

import java.io.*;

public class TempStream extends StreamImpl implements java.io.Serializable
{
  private String _encoding;
  private TempBuffer _head;
  private TempBuffer _tail;

  public TempStream()
  {
  }

  /**
   * Initializes the temp stream for writing.
   */
  public void openWrite()
  {
    TempBuffer ptr = _head;

    _head = null;
    _tail = null;

    _encoding = null;

    TempBuffer.freeAll(ptr);
  }

  public byte []getTail()
  {
    return _tail.getBuffer();
  }

  /**
   * Sets the encoding.
   */
  public void setEncoding(String encoding)
  {
    _encoding = encoding;
  }

  /**
   * Gets the encoding.
   */
  public String getEncoding()
  {
    return _encoding;
  }

  @Override
  public boolean canWrite() { return true; }

  /**
   * Writes a chunk of data to the temp stream.
   */
  @Override
  public void write(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    while (length > 0) {
      if (_tail == null)
        addBuffer(TempBuffer.allocate());
      else if (_tail._buf.length <= _tail._length)
        addBuffer(TempBuffer.allocate());

      int sublen = _tail._buf.length - _tail._length;
      if (length < sublen)
        sublen = length;

      System.arraycopy(buf, offset, _tail._buf, _tail._length, sublen);

      length -= sublen;
      offset += sublen;
      _tail._length += sublen;
    }
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
    throws IOException
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
   *
   * @param free if true, frees the buffer as it's read
   */
  public ReadStream openReadAndSaveBuffer()
    throws IOException
  {
    close();

    TempReadStream read = new TempReadStream(_head);
    read.setFreeWhenDone(false);
    
    return new ReadStream(read);
  }

  /**
   * Opens a read stream to the buffer.
   */
  public void openRead(ReadStream rs)
    throws IOException
  {
    close();

    TempReadStream tempReadStream = new TempReadStream(_head);
    tempReadStream.setPath(getPath());
    tempReadStream.setFreeWhenDone(true);
    
    _head = null;
    _tail = null;
    
    rs.init(tempReadStream, null);
  }

  /**
   * Returns an input stream to the contents, freeing the value
   * automatically.
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
   * Returns the head buffer.
   */
  public TempBuffer getHead()
  {
    return _head;
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

  @Override
  public void clearWrite()
  {
    TempBuffer ptr = _head;

    _head = null;
    _tail = null;

    TempBuffer.freeAll(ptr);
  }

  public void discard()
  {
    _head = null;
    _tail = null;
  }

  /**
   * Copies the temp stream;
   */
  public TempStream copy()
  {
    TempStream newStream = new TempStream();

    TempBuffer ptr = _head;

    for (; ptr != null; ptr = ptr.getNext()) {
      TempBuffer newPtr = TempBuffer.allocate();
      
      if (newStream._tail != null)
        newStream._tail.setNext(newPtr);
      else
        newStream._head = newPtr;
      newStream._tail = newPtr;

      newPtr.write(ptr.getBuffer(), 0, ptr.getLength());
    }

    return newStream;
  }

  /**
   * Clean up the temp stream.
   */
  public void destroy()
  {
    try {
      close();
    } catch (IOException e) {
    } finally {
      TempBuffer ptr = _head;
    
      _head = null;
      _tail = null;

      TempBuffer.freeAll(ptr);
    }
  }
}
