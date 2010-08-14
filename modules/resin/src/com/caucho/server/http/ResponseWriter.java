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
import com.caucho.vfs.AbstractPrintWriter;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResponseWriter extends AbstractPrintWriter {
  private static final Logger log
    = Logger.getLogger(ResponseWriter.class.getName());
  static final L10N L = new L10N(ResponseWriter.class);

  private AbstractResponseStream _out;
  private boolean _hasError;

  public ResponseWriter()
  {
  }

  ResponseWriter(AbstractResponseStream out)
  {
    _out = out;
  }

  public void init(AbstractResponseStream out)
  {
    _out = out;
    _hasError = false;
  }

  public int getBufferSize()
  {
    return _out.getBufferSize();
  }

  /**
   * Sets the buffer size.
   */
  public void setBufferSize(int size)
  {
    _out.setBufferSize(size);
  }

  public int getRemaining()
  {
    return _out.getRemaining();
  }

  /**
   * Checks for an error.
   */
  public boolean checkError()
  {
    return _hasError;
  }

  /**
   * Clears the response buffer.
   */
  public void clearBuffer()
  {
    _out.clearBuffer();
  }

  /**
   * Writes a character to the output.
   *
   * @param buf the buffer to write.
   */
  final public void write(int ch)
  {
    try {
      _out.print(ch);
    } catch (IOException e) {
      _hasError = true;
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Writes a character array to the writer.
   *
   * @param buf the buffer to write.
   * @param off the offset into the buffer
   * @param len the number of characters to write
   */
  final public void write(char []buf, int offset, int length)
  {
    try {
      _out.print(buf, offset, length);
    } catch (IOException e) {
      _hasError = true;
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Writes a subsection of a string to the output.
   */
  final public void write(String s, int off, int len)
  {
    try {
      char []writeBuffer = _out.getCharBuffer();
      int size = writeBuffer.length;
      int writeLength = _out.getCharOffset();
      int end = off + len;

      while (off < end) {
        int sublen = end - off;

        if (size - writeLength < sublen) {
          if (size == writeLength) {
            writeBuffer = _out.nextCharBuffer(writeLength);
            writeLength = 0;

            if (size < sublen)
              sublen = size;
          }
          else
            sublen = size - writeLength;
        }

        int tail = off + sublen;
        s.getChars(off, tail, writeBuffer, writeLength);

        off = tail;
        writeLength += sublen;
      }

      _out.setCharOffset(writeLength);
    } catch (IOException e) {
      _hasError = true;
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Flushes the buffered response to the output stream.
   */
  public void flush()
  {
    try {
      _out.flushChar();
    } catch (IOException e) {
      _hasError = true;
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Flush the contents of the buffer to the underlying stream.
   *
   * @param isEnd true if the request is done.
   */
  public void flushBuffer()
  {
    try {
      _out.flushBuffer();
    } catch (IOException e) {
      _hasError = true;
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void close()
  {
    _hasError = false;

    // server/0200
    // server/1720
    /*
    try {
      _out.close();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    */

    /*
    try {
      _out.flushBuffer();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    */
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _out + "]";
  }
}
