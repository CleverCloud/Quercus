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
import com.caucho.vfs.OutputStreamWithBuffer;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * API for handling the PrintWriter/ServletOutputStream
 */
public class StubResponseStream extends AbstractResponseStream {
  private final byte []_byteBuffer = new byte[16];
  private final char []_charBuffer = new char[16];
  
  /**
   * Returns true for a Caucho response stream.
   */
  @Override
  public boolean isCauchoResponseStream()
  {
    return true;
  }

  /**
   * Sets the buffer size.
   */
  @Override
  public void setBufferSize(int size)
  {
  }

  /**
   * Gets the buffer size.
   */
  @Override
  public int getBufferSize()
  {
    return 0;
  }

  /**
   * Returns the remaining buffer entries.
   */
  @Override
  public int getRemaining()
  {
    return 0;
  }
  /**
   * Returns the stream's buffer.
   */
  public byte []getBuffer()
    throws IOException
  {
    return _byteBuffer;
  }
  
  /**
   * Returns the stream's buffer offset.
   */
  public int getBufferOffset()
    throws IOException
  {
    return 0;
  }
  
  /**
   * Sets the stream's buffer length.
   */
  public void setBufferOffset(int offset)
    throws IOException
  {
  }
  
  /**
   * Returns the next buffer.
   *
   * @param length the length of the completed buffer
   *
   * @return the next buffer
   */
  public byte []nextBuffer(int offset)
    throws IOException
  {
    return _byteBuffer;
  }

  /**
   * Returns the char buffer.
   */
  @Override
  public char []getCharBuffer()
    throws IOException
  {
    return _charBuffer;
  }

  /**
   * Returns the char buffer offset.
   */
  @Override
  public int getCharOffset()
    throws IOException
  {
    return 0;
  }

  /**
   * Sets the char buffer offset.
   */
  @Override
  public void setCharOffset(int offset)
    throws IOException
  {
  }

  /**
   * Returns the next char buffer.
   */
  @Override
  public char []nextCharBuffer(int offset)
    throws IOException
  {
    return _charBuffer;
  }
  
  /**
   * Writes a byte to the output.
   */
  @Override
  public void write(int v)
    throws IOException
  {
  }

  /**
   * Writes a byte array to the output.
   */
  @Override
  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
  }

  /**
   * Writes a character to the output.
   */
  @Override
  public void print(int ch)
    throws IOException
  {
  }

  /**
   * Writes a char array to the output.
   */
  @Override
  public void print(char []buffer, int offset, int length)
    throws IOException
  {
  }

  /**
   * Clears the output buffer.
   */
  @Override
  public void clearBuffer()
  {
  }

  /**
   * Flushes the output buffer.
   */
  @Override
  public void flushBuffer()
    throws IOException
  {
  }
}
