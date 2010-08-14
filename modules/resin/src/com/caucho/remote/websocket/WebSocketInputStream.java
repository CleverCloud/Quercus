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

package com.caucho.remote.websocket;

import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;

/**
 * WebSocketInputStream reads a single WebSocket packet.
 *
 * <code><pre>
 * 0x80 0x8X 0x8X 0x0X binarydata
 * </pre></code>
 */
public class WebSocketInputStream extends InputStream {
  private static final L10N L = new L10N(WebSocketInputStream.class);
  
  private InputStream _is;
  private int _code;
  private int _offset;
  private int _length;

  public WebSocketInputStream()
  {
  }

  public WebSocketInputStream(InputStream is)
    throws IOException
  {
    init(is);
  }

  public void init(InputStream is)
    throws IOException
  {
    _is = is;

    readChunkLength();
  }

  public int getCode()
  {
    return _code;
  }

  public int getLength()
  {
    return _length;
  }

  public int read()
    throws IOException
  {
    InputStream is = _is;

    if (is == null)
      return -1;

    if (_length <= _offset) {
      if (! readChunkLength())
        return -1;
    }

    int ch = is.read();
    _offset++;
    return ch;
  }

  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    InputStream is = _is;

    if (_length <= _offset) {
      if (! readChunkLength())
        return -1;
    }
    
    int sublen = _length - _offset;

    if (sublen <= 0 || is == null)
      return -1;

    if (length < sublen)
      sublen = length;

    sublen = is.read(buffer, offset, sublen);

    if (sublen > 0) {
      _offset += sublen;
      return sublen;
    }
    else {
      close();
      return -1;
    }
  }

  protected boolean readChunkLength()
    throws IOException
  {
    InputStream is = _is;

    if (is == null)
      return false;
    
    int ch = is.read();

    if ((ch & 0x80) != 0x80)
      throw new IOException(L.l("{0}: expected 0x80 at '0x{1}' because WebSocket binary protocol expects 0x80 at beginning",
                                this, Integer.toHexString(ch & 0xffff)));

    _code = ch;

    int length = 0;
    do {
      ch = is.read();
      length = (length << 7) + (ch & 0x7f);
    } while ((ch & 0x80) == 0x80);

    _length = length;
    _offset = 0;

    if (length == 0)
      close();

    return length > 0;
  }

  public void close()
    throws IOException
  {
    InputStream is = _is;
    _is = null;
  }
}
