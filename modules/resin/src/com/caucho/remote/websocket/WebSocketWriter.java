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

import com.caucho.vfs.*;

import java.io.*;

/**
 * WebSocketWriter writes a single WebSocket packet.
 *
 * <code><pre>
 * 0x00 utf-8 data 0xff
 * </pre></code>
 */
public class WebSocketWriter extends Writer {
  private OutputStream _os;

  public WebSocketWriter()
  {
  }

  public WebSocketWriter(OutputStream os)
    throws IOException
  {
    init(os);
  }

  public void init(OutputStream os)
    throws IOException
  {
    _os = os;

    os.write(0x00);
  }

  public void write(int ch)
    throws IOException
  {
    OutputStream os = _os;
    
    if (ch < 0x80)
      os.write(ch);
    else if (ch < 0x800) {
      os.write(0xc0 | (ch >> 6));
      os.write(0x80 | (ch & 0x3f));
    }
    else {
      os.write(0xe0 | ((ch >> 12) & 0x1f));
      os.write(0x80 | ((ch >> 6) & 0x3f));
      os.write(0x80 | (ch & 0x3f));
    }
  }

  public void write(char []buf)
    throws IOException
  {
    write(buf, 0, buf.length);
  }
  
  public void write(char []buf, int offset, int length)
    throws IOException
  {
    OutputStream os = _os;

    int end = offset + length;
    
    for (; offset < end; offset++) {
      int ch = buf[offset];
      
      if (ch < 0x80)
        os.write(ch);
      else if (ch < 0x800) {
        os.write(0xc0 | (ch >> 6));
        os.write(0x80 | (ch & 0x3f));
      }
      else {
        os.write(0xe0 | ((ch >> 12) & 0x1f));
        os.write(0x80 | ((ch >> 6) & 0x3f));
        os.write(0x80 | (ch & 0x3f));
      }
    }
  }

  public void write(String s)
    throws IOException
  {
    write(s, 0, s.length());
  }
  
  public void write(String s, int offset, int length)
    throws IOException
  {
    OutputStream os = _os;

    int end = offset + length;
    
    for (; offset < end; offset++) {
      int ch = s.charAt(offset);
      
      if (ch < 0x80)
        os.write(ch);
      else if (ch < 0x800) {
        os.write(0xc0 | (ch >> 6));
        os.write(0x80 | (ch & 0x3f));
      }
      else {
        os.write(0xe0 | ((ch >> 12) & 0x1f));
        os.write(0x80 | ((ch >> 6) & 0x3f));
        os.write(0x80 | (ch & 0x3f));
      }
    }
  }

  public void flush()
    throws IOException
  {
    if (_os != null)
      _os.flush();
  }

  public void close()
    throws IOException
  {
    OutputStream os = _os;
    _os = null;

    if (os != null) {
      os.write(0xff);
    }
  }
}
