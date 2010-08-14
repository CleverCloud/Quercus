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

package com.caucho.server.http;

import com.caucho.util.L10N;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.TempCharBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract public class ToCharResponseStream extends AbstractResponseStream {
  private static final Logger log
    = Logger.getLogger(ToCharResponseStream.class.getName());
  
  static final L10N L = new L10N(ToCharResponseStream.class);

  private static final int SIZE = TempCharBuffer.SIZE;
  
  private final byte []_byteBuffer = new byte[SIZE];
  
  // head of the expandable buffer
  private TempCharBuffer _head = TempCharBuffer.allocate();
  private TempCharBuffer _tail;
  
  private char []_charBuffer;
  private int _charLength;

  private int _bufferCapacity;
  private int _bufferSize;
  
  private BufferInputStream _in;
  private Reader _encodingReader;

  public ToCharResponseStream()
  {
  }
  
  /**
   * Initializes the Buffered Response stream at the beginning of a request.
   */
  @Override
  public void start()
  {
    super.start();
    
    _head.clear();
    _head.setNext(null);
    _tail = _head;

    _charBuffer = _tail.getBuffer();
    _charLength = 0;
    
    _bufferCapacity = SIZE;
    _bufferSize = 0;

    _encodingReader = null;

    // _toChar = Encoding.getReadEncoding("ISO-8859-1");
  }

  /**
   * Returns true for a caucho response stream.
   */
  public boolean isCauchoResponseStream()
  {
    // server/1b13
    return false;
  }

  /**
   * Sets the buffer size.
   */
  public void setBufferSize(int size)
  {
    size = (size + SIZE - 1);
    size -= size % SIZE;

    if (_bufferCapacity < size)
      _bufferCapacity = size;
  }

  /**
   * Gets the buffer size.
   */
  public int getBufferSize()
  {
    return _bufferCapacity;
  }

  /**
   * Returns the remaining buffer entries.
   */
  public int getRemaining()
  {
    return _bufferCapacity - _bufferSize - _charLength;
  }

  /**
   * Returns the char buffer.
   */
  public char []getCharBuffer()
  {
    return _charBuffer;
  }

  /**
   * Returns the char offset.
   */
  public int getCharOffset()
  {
    return _charLength;
  }

  /**
   * Sets the char offset.
   */
  public void setCharOffset(int offset)
    throws IOException
  {
    _charLength = offset;

    if (SIZE <= _charLength)
      expandCharBuffer();
  }

  /**
   * Converts the char buffer.
   */
  public char []nextCharBuffer(int offset)
    throws IOException
  {
    _charLength = offset;

    if (SIZE <= _charLength)
      expandCharBuffer();

    return _charBuffer;
  }

  /**
   * Writes a character to the output.
   */
  public void print(int ch)
    throws IOException
  {
    /*
    if (_isClosed)
      return;
    */
    _charBuffer[_charLength++] = (char) ch;

    if (SIZE <= _charLength)
      expandCharBuffer();
  }

  /**
   * Writes a char array to the output.
   */
  public void print(char []buffer, int offset, int length)
    throws IOException
  {
    /*
    if (_isHead || _isClosed)
      return;

    if (_byteLength > 0)
      flushByteBuffer();
    */

    int charLength = _charLength;
    while (length > 0) {
      int sublen = length;
      if (SIZE - charLength < sublen)
        sublen = SIZE - charLength;

      System.arraycopy(buffer, offset, _charBuffer, charLength, sublen);

      offset += sublen;
      length -= sublen;
      charLength += sublen;

      if (SIZE <= charLength) {
        _charLength = charLength;
        expandCharBuffer();
        charLength = _charLength;
      }
    }

    _charLength = charLength;
  }

  /**
   * Returns the buffer offset.
   */
  public int getBufferOffset()
  {
    return 0;
  }

  /**
   * Sets the byte buffer offset.
   */
  public void setBufferOffset(int offset)
  {
    if (offset > 0) {
      try {
        write(_byteBuffer, 0, offset);
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  /**
   * Gets the byte buffer
   */
  public byte []getBuffer()
  {
    return _byteBuffer;
  }

  /**
   * Returns the next buffer.
   */
  public byte []nextBuffer(int offset)
    throws IOException
  {
    if (offset > 0)
      write(_byteBuffer, 0, offset);

    return _byteBuffer;
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  public void write(byte []buf, int offset, int length)
    throws IOException
  {
    if (length == 0)
      return;

    if (_encodingReader == null) {
      // server/1b13
      if (_in == null)
        _in = new BufferInputStream();
      _encodingReader = Encoding.getReadEncoding(_in, getEncoding());
    }

    // XXX: performance issues
    if (_encodingReader == null) {
      for (; length > 0; length--) {
        print((char) buf[offset++]);
      }
      return;
    }
    
    _in.init(buf, offset, length);

    // XXX: performance issues
    int ch;
    while ((ch = _encodingReader.read()) >= 0) {
      print(ch);
    }
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  public void write(int ch)
    throws IOException
  {
    if (_encodingReader == null) {
      if (_in == null)
        _in = new BufferInputStream();
      _byteBuffer[0] = (byte) ch;
      _encodingReader = Encoding.getReadEncoding(_in, getEncoding());

      if (_encodingReader == null) {
        print((char) ch);
        return;
      }
    }
    
    _byteBuffer[0] = (byte) ch;
    _in.init(_byteBuffer, 0, 1);

    while ((ch = _encodingReader.read()) >= 0) {
      print(ch);
    }
  }

  /**
   * Flushes the buffer.
   */
  public void flushBuffer()
    throws IOException
  {
    flushCharBuffer();
  }

  /**
   * Flushes the buffer.
   */
  public void flushChar()
    throws IOException
  {
    flushCharBuffer();
  }

  /**
   * Flushes or writes to the buffer.
   */
  private void expandCharBuffer()
    throws IOException
  {
    if (_bufferCapacity <= _bufferSize + _charLength) {
      flushCharBuffer();
    }
    else if (_charLength == SIZE) {
      _tail.setLength(_charLength);
      _bufferSize += _charLength;

      TempCharBuffer tempBuf = TempCharBuffer.allocate();
      _tail.setNext(tempBuf);
      _tail = tempBuf;

      _charBuffer = _tail.getBuffer();
      _charLength = 0;
    }
  }

  /**
   * Flushes the buffered response to the output stream.
   */
  private void flushCharBuffer()
    throws IOException
  {
    _tail.setLength(_charLength);
    _bufferSize += _charLength;
    _charLength = 0;

    TempCharBuffer ptr = _head;
    do {
      _head = ptr;
      
      TempCharBuffer next = ptr.getNext();
      ptr.setNext(null);

      writeNext(ptr.getBuffer(), 0, ptr.getLength());

      if (next != null)
        TempCharBuffer.free(ptr);

      ptr = next;
    } while (ptr != null);

    _tail = _head;
    _tail.setLength(0);
    _charBuffer = _tail.getBuffer();
    _bufferSize = 0;
  }

  /**
   * Flushes the buffer.
   */
  public void clearBuffer()
  {
    _charLength = 0;

    TempCharBuffer ptr = _head;
    do {
      _head = ptr;
      
      TempCharBuffer next = ptr.getNext();
      ptr.setNext(null);

      if (next != null)
        TempCharBuffer.free(ptr);

      ptr = next;
    } while (ptr != null);

    _tail = _head;
    _tail.setLength(0);
    _charBuffer = _head.getBuffer();
    _bufferSize = 0;
  }

  /**
   * Returns the encoding.
   */
  abstract public String getEncoding();
  
  /**
   * Writes to the next.
   */
  abstract protected void writeNext(char []buffer, int offset, int length)
    throws IOException;

  static class BufferInputStream extends InputStream {
    private byte []_buffer;
    private int _offset;
    private int _length;

    void init(byte []buffer, int offset, int length)
    {
      _buffer = buffer;
      _offset = offset;
      _length = length;
    }

    public int read()
    {
      if (_offset < _length)
        return _buffer[_offset++] & 0xff;
      else
        return -1;
    }
  }
}
