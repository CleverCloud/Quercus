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

import com.caucho.server.cluster.Server;
import com.caucho.util.CharSegment;
import com.caucho.util.FreeList;
import com.caucho.vfs.TempBuffer;

/**
 * Holds the HTTP buffers for keepalive reuse.  Because a request needs a
 * large number of buffers, but a keepalive doesn't need those buffers,
 * Resin can recycle the buffers during keepalives to keep the memory
 * consumption low.
 */
public final class HttpBufferStore
{
  private static final FreeList<HttpBufferStore> _freeList
    = new FreeList<HttpBufferStore>(256);

  private final byte []_logBuffer = null;//new byte[1024];
  
  private final byte []_uri;              // "/path/test.jsp/Junk?query=7"
  private final char []_headerBuffer;

  private final int _headerCapacity;
  private final CharSegment []_headerKeys;
  private final CharSegment []_headerValues;
  
  private final TempBuffer _tempBuffer = TempBuffer.allocate();

  /**
   * Create a new Request.  Because the actual initialization occurs with
   * the start() method, this just allocates statics.
   *
   * @param server the parent server
   */
  private HttpBufferStore(Server server)
  {
    int urlLengthMax = server.getUrlLengthMax();
    
    _uri = new byte[urlLengthMax];

    if (TempBuffer.isSmallmem()) {
      _headerBuffer = new char[4 * 1024];
      _headerCapacity = 64;
    }
    else {
      _headerBuffer = new char[16 * 1024];
      _headerCapacity = 256;
    }

    _headerKeys = new CharSegment[_headerCapacity];
    _headerValues = new CharSegment[_headerCapacity];
    
    for (int i = 0; i < _headerCapacity; i++) {
      _headerKeys[i] = new CharSegment();
      _headerValues[i] = new CharSegment();
    }
  }

  public static HttpBufferStore allocate(Server server)
  {
    HttpBufferStore buffer = _freeList.allocate();

    if (buffer == null)
      buffer = new HttpBufferStore(server);

    return buffer;
  }

  public static void free(HttpBufferStore buffer)
  {
    _freeList.free(buffer);
  }

  public final byte []getUriBuffer()
  {
    return _uri;
  }

  public final char []getHeaderBuffer()
  {
    return _headerBuffer;
  }

  public final int getHeaderCapacity()
  {
    return _headerCapacity;
  }

  public final CharSegment []getHeaderKeys()
  {
    return _headerKeys;
  }

  public final CharSegment []getHeaderValues()
  {
    return _headerValues;
  }

  public final TempBuffer getTempBuffer()
  {
    return _tempBuffer;
  }

  public final byte []getLogBuffer()
  {
    return _logBuffer;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
