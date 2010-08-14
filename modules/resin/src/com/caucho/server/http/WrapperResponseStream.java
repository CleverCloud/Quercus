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
import com.caucho.vfs.TempBuffer;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WrapperResponseStream extends AbstractResponseStream {
  private static final Logger log
    = Logger.getLogger(WrapperResponseStream.class.getName());
  
  static final L10N L = new L10N(WrapperResponseStream.class);

  private static final int SIZE = TempBuffer.SIZE;
  
  private final byte []_byteBuffer = new byte[SIZE];
  private final char []_charBuffer = new char[SIZE];
  
  private HttpServletResponse _next;
  
  private ServletOutputStream _os;
  private PrintWriter _writer;

  public WrapperResponseStream()
  {
  }

  public void init(HttpServletResponse next)
  {
    if (next == null)
      throw new NullPointerException();

    _next = next;
    _os = null;
    _writer = null;
  }

  /**
   * Returns true for a caucho response stream.
   */
  public boolean isCauchoResponseStream()
  {
    if (_next instanceof HttpServletResponseImpl)
      return ((HttpServletResponseImpl) _next).isCauchoResponseStream();
    else
      return false;
  }

  /**
   * Sets the buffer size.
   */
  public void setBufferSize(int size)
  {
    _next.setBufferSize(size);
  }

  /**
   * Gets the buffer size.
   */
  public int getBufferSize()
  {
    return _next.getBufferSize();
  }

  /**
   * Returns the remaining buffer entries.
   */
  public int getRemaining()
  {
    //return _next.getRemaining();
    return 0;
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
        print(_charBuffer, 0, offset);
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
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
      print(_charBuffer, 0, offset);

    return _charBuffer;
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  public void print(int ch)
    throws IOException
  {
    if (_writer == null) {
      if (_next == null)
        return;
      
      _writer = _next.getWriter();
    }

    _writer.write(ch);
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  public void print(char []buffer, int offset, int length)
    throws IOException
  {
    if (length == 0)
      return;
    
    if (_writer == null) {
      if (_next == null)
        return;
      
      _writer = _next.getWriter();
    }

    _writer.write(buffer, offset, length);
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
  public void write(int ch)
    throws IOException
  {
    if (_os == null) {
      if (_next == null)
        return;
      
      _os = _next.getOutputStream();
    }

    _os.write(ch);
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
    
    if (_os == null) {
      if (_next == null)
        return;
      
      _os = _next.getOutputStream();
    }

    _os.write(buf, offset, length);
  }

  /**
   * Writes the char buffer to the output stream.
   */
  public void flushCharBuffer()
    throws IOException
  {
  }

  /**
   * Flushes the buffer.
   */
  public void flushBuffer()
    throws IOException
  {
  }

  /**
   * Flushes the buffer.
   */
  public void flushChar()
    throws IOException
  {
    if (_writer == null) {
      if (_next == null)
        return;
      
      _writer = _next.getWriter();
    }

    if (_writer != null)
      _writer.flush();
  }

  /**
   * Flushes the buffer.
   */
  public void flushByte()
    throws IOException
  {
    if (_os == null) {
      if (_next == null)
        return;
      
      _os = _next.getOutputStream();
    }

    if (_os != null)
      _os.flush();
  }

  /**
   * Clears the buffer.
   */
  public void clearBuffer()
  {
    if (_next != null)
      _next.resetBuffer();
  }

  /**
   * Flushes the output.
   */
  public void flush()
    throws IOException
  {
    if (_writer != null)
      _writer.flush();

    if (_os != null)
      _os.flush();
  }

  /**
   * Finish.
   */
  public void finish()
    throws IOException
  {
    /*
    if (_writer != null)
      _writer.flush();
    
    if (_os != null)
      _os.flush();
    */

    _next = null;
    _os = null;
    _writer = null;
  }

  /**
   * Close.
   */
  @Override
  protected void closeImpl()
    throws IOException
  {
    super.closeImpl();
    
    // jsp/17f0
    PrintWriter writer = _writer;
    _writer = null;

    if (writer != null) {
      try {
        writer.close();
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    ServletOutputStream os = _os;
    _os = null;
    
    if (os != null) {
      try {
        os.close();
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    _next = null;
  }
}
