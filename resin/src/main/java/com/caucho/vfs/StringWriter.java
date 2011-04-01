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

import com.caucho.util.CharBuffer;

import java.io.CharConversionException;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class StringWriter extends StreamImpl {
  private WriteStream ws;
  private CharBuffer cb;

  public StringWriter()
  {
  }
  
  public StringWriter(CharBuffer cb)
  {
    this.cb = cb;
  }

  /**
   * Opens a write stream using this StringWriter as the resulting string
   */
  public WriteStream openWrite()
  {
    if (cb != null)
      cb.clear();
    else
      cb = CharBuffer.allocate();

    if (ws == null)
      ws = new WriteStream(this);
    else
      ws.init(this);

    try {
      ws.setEncoding("utf-8");
    } catch (UnsupportedEncodingException e) {
    }

    return ws;
  }

  public String getString()
  {
    try {
      ws.close();
    } catch (IOException e) {
    }

    String str = cb.close();

    cb = null;

    return str;
  }

  /**
   * Returns true since StringWriter is for writing.
   */
  public boolean canWrite()
  {
    return true;
  }

  /**
   * Writes a utf-8 encoded buffer to the underlying string.
   *
   * @param buf byte buffer containing the bytes
   * @param offset offset where to start writing
   * @param length number of bytes to write
   * @param isEnd true when the write is flushing a close.
   */
  public void write(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    int end = offset + length;
    while (offset < end) {
      int ch1 = buf[offset++] & 0xff;

      if (ch1 < 0x80)
        cb.append((char) ch1);
      else if ((ch1 & 0xe0) == 0xc0) {
        if (offset >= end)
          throw new EOFException("unexpected end of file in utf8 character");
        
        int ch2 = buf[offset++] & 0xff;
        if ((ch2 & 0xc0) != 0x80)
          throw new CharConversionException("illegal utf8 encoding");
      
        cb.append((char) (((ch1 & 0x1f) << 6) + (ch2 & 0x3f)));
      }
      else if ((ch1 & 0xf0) == 0xe0) {
        if (offset + 1 >= end)
          throw new EOFException("unexpected end of file in utf8 character");
        
        int ch2 = buf[offset++] & 0xff;
        int ch3 = buf[offset++] & 0xff;
      
        if ((ch2 & 0xc0) != 0x80)
          throw new CharConversionException("illegal utf8 encoding");
      
        if ((ch3 & 0xc0) != 0x80)
          throw new CharConversionException("illegal utf8 encoding");
      
        cb.append((char) (((ch1 & 0x1f) << 12) + ((ch2 & 0x3f) << 6) + (ch3 & 0x3f)));
      }
      else
        throw new CharConversionException("illegal utf8 encoding at (" +
                                          (int) ch1 + ")");
    }
  }
}
