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

package com.caucho.server.http;

import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StreamImpl;

import java.io.IOException;

/**
 * Filter so POST readers can only read data up to the content length
 */
public class RawInputStream extends StreamImpl {
  // the underlying stream
  private ReadStream _next;

  void init(ReadStream next)
  {
    _next = next;
  }

  public boolean canRead()
  {
    return true;
  }

  /**
   * Reads from the buffer, limiting to the content length.
   *
   * @param buffer the buffer containing the results.
   * @param offset the offset into the result buffer
   * @param length the length of the buffer.
   *
   * @return the number of bytes read or -1 for the end of the file.
   */
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    int len = _next.read(buffer, offset, length);

    return len;
  }

  public int getAvailable()
    throws IOException
  {
    return _next.available();
  }
}
