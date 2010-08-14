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
 * WebSocketOutputStream writes a single WebSocket packet.
 *
 * <code><pre>
 * 0x80 0x8X 0x8X 0x0X binarydata
 * </pre></code>
 */
public class WebSocketOutputStream extends OutputStream {
  private static final L10N L = new L10N(WebSocketOutputStream.class);

  private OutputStream _os;
  private TempBuffer _tBuf;
  private byte []_buffer;
  private int _offset;

  public WebSocketOutputStream()
  {
  }

  public WebSocketOutputStream(OutputStream os)
    throws IOException
  {
    init(os);
  }

  public void init(OutputStream os)
    throws IOException
  {
    _os = os;

    _tBuf = TempBuffer.allocate();
    _buffer = _tBuf.getBuffer();
    _offset = 3;
  }

  public void write(int ch)
    throws IOException
  {
    byte []buffer = _buffer;

    if (buffer == null)
      return;

    if (_offset == buffer.length)
      flush();

    buffer[_offset++] = (byte) ch;
  }

  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    byte []wsBuffer = _buffer;

    if (wsBuffer == null)
      return;

    while (length > 0) {
      if (_offset == wsBuffer.length)
        flush();

      int sublen = wsBuffer.length - _offset;
      if (length < sublen)
        sublen = length;

      System.arraycopy(buffer, offset, wsBuffer, _offset, sublen);

      offset += sublen;
      length -= sublen;
      _offset += sublen;
    }
  }

  public void complete()
    throws IOException
  {
    byte []buffer = _buffer;

    if (buffer == null)
      return;

    int offset = _offset;
    _offset = 3;

    // don't flush empty chunk
    if (offset == 3)
      return;

    int length = offset - 3;

    buffer[0] = (byte) 0x80;
    buffer[1] = (byte) (0x80 + ((length >> 7) & 0x3f));
    buffer[2] = (byte) (0x00 + (length & 0x3f));

    _os.write(buffer, 0, offset);
  }

  public void flush()
    throws IOException
  {
    complete();

    _os.flush();
  }

  public void close()
    throws IOException
  {
    flush();

    OutputStream os = _os;
    _os = null;

    byte []buffer = _buffer;
    _buffer = null;

    TempBuffer tBuf = _tBuf;
    _tBuf = null;

    if (buffer == null)
      return;

    buffer[0] = (byte) 0x80;
    buffer[1] = (byte) 0x00;

    os.write(buffer, 0, 2);

    TempBuffer.free(tBuf);
  }
}
