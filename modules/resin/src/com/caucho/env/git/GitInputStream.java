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

package com.caucho.env.git;

import java.io.IOException;
import java.io.InputStream;

/**
 * Stream with type/length combined with a data stream
 */
public class GitInputStream extends InputStream {
  private InputStream _is;

  private byte []_buffer;
  private int _bufferOffset;
  private int _bufferLength;
  
  public GitInputStream(String type, long length, InputStream is)
    throws IOException
  {
    _is = is;

    _buffer = new byte[64];
    int offset = 0;

    for (int i = 0; i < type.length(); i++) {
      _buffer[offset++] = (byte) type.charAt(i);
    }

    _buffer[offset++] = (byte) ' ';

    String lengthStr = String.valueOf(length);

    for (int i = 0; i < lengthStr.length(); i++) {
      _buffer[offset++] = (byte) lengthStr.charAt(i);
    }
    _buffer[offset++] = 0;

    _bufferLength = offset;
    _bufferOffset = 0;
  }

  public int read()
    throws IOException
  {
    if (_bufferOffset < _bufferLength)
      return _buffer[_bufferOffset++] & 0xff;
    else
      return _is.read();
  }

  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    int sublen = _bufferLength - _bufferOffset;

    if (sublen > 0) {
      if (length < sublen)
        sublen = length;

      System.arraycopy(_buffer, _bufferOffset, buffer, offset, sublen);

      _bufferOffset += sublen;
      offset += sublen;
      length -= sublen;
    }

    if (length > 0) {
      int readLength = _is.read(buffer, offset, length);

      if (readLength > 0)
        return sublen + readLength;
      else if (sublen > 0)
        return sublen;
      else
        return -1;
    }
    else
      return sublen;
  }

  public void close()
    throws IOException
  {
    _is.close();
  }
}
