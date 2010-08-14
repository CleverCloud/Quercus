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
 * WebSocketReader reads a single WebSocket packet.
 *
 * <code><pre>
 * 0x00 utf-8 data 0xff
 * </pre></code>
 */
public class WebSocketReader extends Reader {
  private static final L10N L = new L10N(WebSocketReader.class);
  
  private InputStream _is;

  public WebSocketReader()
  {
  }

  public WebSocketReader(InputStream is)
    throws IOException
  {
    init(is);
  }

  public void init(InputStream is)
    throws IOException
  {
    _is = is;

    int ch = is.read();

    if (ch != 0x00)
      throw new IOException(L.l("{0}: expected 0x00 at '0x{1}' because WebSocket protocol expects 0x00 at beginning",
                                this, Integer.toHexString(ch & 0xffff)));
  }

  public int read()
    throws IOException
  {
    InputStream is = _is;

    if (is == null)
      return -1;

    int ch = is.read();

    if (ch == 0xff || ch < 0) {
      _is = null;
      return -1;
    }

    if (ch < 0x80)
      return ch;
    else if ((ch & 0xe0) == 0xc0) {
      int ch2 = is.read();

      if (ch2 < 0) {
        _is = null;
        return -1;
      }

      return ((ch & 0x1f) << 6) + (ch2 & 0x7f);
    }
    else if ((ch & 0xf0) == 0xe0) {
      int ch2 = is.read();
      int ch3 = is.read();

      if (ch2 < 0) {
        _is = null;
        return -1;
      }

      return ((ch & 0xf) << 12) + ((ch2 & 0x3f) << 6) + (ch3 & 0x3f);
    }
    else
      return 0xfeff;
  }

  public int read(char []buffer, int offset, int length)
    throws IOException
  {
    int start = offset;
    int end = offset + length;

    for (; offset < end; offset++) {
      int ch = read();

      if (ch >= 0) {
        buffer[offset] = (char) ch;
      }
      else {
        break;
      }
    }

    if (start == offset)
      return -1;
    else
      return offset - start;
  }

  public void close()
    throws IOException
  {
    InputStream is = _is;
    _is = null;
  }
}
