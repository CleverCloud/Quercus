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

import com.caucho.vfs.WriteStream;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Implementation of the ServletOutputStream.
 */
public class ServletOutputStreamImpl extends ServletOutputStream {
  private OutputStream _out;
  private byte []_buffer;

  public ServletOutputStreamImpl()
  {
  }

  /**
   * Initialize with the current response stream.
   */
  public void init(OutputStream out)
  {
    _out = out;
  }

  /**
   * Writes a byte to the output stream.
   */
  public final void write(int b) throws IOException
  {
    _out.write(b);
  }

  /**
   * Writes a byte buffer to the output stream.
   */
  public final void write(byte []buf, int offset, int len) throws IOException
  {
    _out.write(buf, offset, len);
  }

  /**
   * Prints a string to the stream.  Note, this method does not properly
   * handle character encoding.
   *
   * @param s the string to write.
   */
  @Override
  public void print(String s) throws IOException
  {
    if (s == null)
      s = "null";

    OutputStream out = _out;
    
    if (out instanceof WriteStream) {
      WriteStream wOut = (WriteStream) out;

      wOut.print(s);
    }
    else {
      int length = s.length();

      if (_buffer == null)
        _buffer = new byte[128];

      byte []buffer = _buffer;

      // server/0810
      int offset = 0;
      
      while (length > 0) {
        int sublen = buffer.length;
        if (length < sublen)
          sublen = length;
        
        for (int i = 0; i < sublen; i++) {
          buffer[i] = (byte) s.charAt(i + offset);
        }

        out.write(buffer, 0, sublen);

        length -= sublen;
        offset += sublen;
      }
    }
  }


  public final void flush() throws IOException
  {
    _out.flush();
  }

  public final void close() throws IOException
  {
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _out + "]";
  }
}
