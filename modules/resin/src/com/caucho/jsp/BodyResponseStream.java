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

package com.caucho.jsp;

import com.caucho.server.http.AbstractResponseStream;
import com.caucho.util.L10N;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.TempBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BodyResponseStream extends AbstractResponseStream {
  private static final Logger log
    = Logger.getLogger(BodyResponseStream.class.getName());
  
  static final L10N L = new L10N(BodyResponseStream.class);

  private static final int SIZE = TempBuffer.SIZE;
  
  private final byte []_byteBuffer = new byte[SIZE];
  private final char []_charBuffer = new char[SIZE];
  
  private Writer _out;
  private String _encoding;

  private BufferInputStream _in;
  private Reader _encodingReader;
  
  public BodyResponseStream()
  {
  }

  /**
   * Returns true for a caucho response stream.
   */
  public boolean isCauchoResponseStream()
  {
    return true;
  }

  public void setWriter(Writer out)
  {
    _out = out;
    _encoding = null;
    _encodingReader = null;
  }

  @Override
  public String getEncoding()
  {
    return _encoding;
  }

  public void setEncoding(String encoding)
  {
    _encoding = encoding;
  }

  /**
   * Sets the buffer size.
   */
  public void setBufferSize(int size)
  {
  }

  /**
   * Gets the buffer size.
   */
  public int getBufferSize()
  {
    return SIZE;
  }

  /**
   * Returns the remaining buffer entries.
   */
  public int getRemaining()
  {
    return SIZE;
  }

  /**
   * Returns the char buffer.
   */
  public char []getCharBuffer()
  {
    return _charBuffer;
  }

  /**
   * Returns the char buffer offset.
   */
  public int getCharOffset()
  {
    return 0;
  }

  /**
   * Sets the char buffer offset.
   */
  public void setCharOffset(int offset)
  {
    if (offset > 0) {
      try {
        _out.write(_charBuffer, 0, offset);
      } catch (IOException e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  /**
   * Returns the next char buffer.
   */
  public char []nextCharBuffer(int offset)
    throws IOException
  {
    if (offset > 0)
      _out.write(_charBuffer, 0, offset);

    return _charBuffer;
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  public void print(char []buf, int offset, int length)
    throws IOException
  {
    if (length == 0)
      return;

    _out.write(buf, offset, length);
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  @Override
  public void print(int ch)
    throws IOException
  {
    _out.write(ch);
  }

  /**
   * Returns the buffer offset.
   */
  @Override
  public int getBufferOffset()
  {
    return 0;
  }

  /**
   * Sets the byte buffer offset.
   */
  @Override
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
  @Override
  public byte []getBuffer()
  {
    return _byteBuffer;
  }

  /**
   * Returns the next buffer.
   */
  @Override
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
  @Override
  public void write(byte []buf, int offset, int length)
    throws IOException
  {
    if (length == 0)
      return;

    if (_encodingReader == null) {
      if (_in == null)
        _in = new BufferInputStream();
      _encodingReader = Encoding.getReadEncoding(_in, _encoding);
    }

    if (_encodingReader == null) {
      for (; length > 0; length--) {
        print((char) (buf[offset++] & 0xff));
      }
      return;
    }
    
    _in.init(buf, offset, length);

    int ch;
    while ((ch = _encodingReader.read()) >= 0) {
      print(ch);
    }
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param ch - character to write
   */
  @Override
  public void write(int ch)
    throws IOException
  {
    if (_encodingReader == null) {
      if (_in == null)
        _in = new BufferInputStream();
      _byteBuffer[0] = (byte) ch;
      _encodingReader = Encoding.getReadEncoding(_in, _encoding);
    }

    if (_encodingReader == null) {
      print((char) ch);
      return;
    }
    
    _in.init(_byteBuffer, 0, 1);

    while ((ch = _encodingReader.read()) >= 0) {
      print(ch);
    }
  }

  /**
   * Flushes the buffer.
   */
  @Override
  public void flushBuffer()
    throws IOException
  {
  }

  /**
   * Clears the buffer.
   */
  public void clearBuffer()
  {
  }

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
