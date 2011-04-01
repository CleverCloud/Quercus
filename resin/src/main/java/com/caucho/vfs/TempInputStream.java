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

import java.io.InputStream;
import java.io.IOException;

public class TempInputStream extends InputStream {
  private TempBuffer _head;

  private int _offset;

  public TempInputStream(TempBuffer head)
  {
    _head = head;
  }

  @Override
  public int read()
    throws IOException
  {
    TempBuffer head = _head;
    
    if (head == null)
      return -1;

    int value = head._buf[_offset++] & 0xff;

    if (head._length <= _offset) {
      _head = head._next;

      head._next = null;
      TempBuffer.free(head);
      _offset = 0;
    }

    return value;
  }

  @Override
  public int read(byte []buf, int offset, int length) throws IOException
  {
    TempBuffer head = _head;
    
    if (head == null)
      return -1;

    int sublen = head._length - _offset;

    if (length < sublen)
      sublen = length;

    System.arraycopy(head._buf, _offset, buf, offset, sublen);

    if (head._length <= _offset + sublen) {
      _head = head._next;

      head._next = null;
      TempBuffer.free(head);
      _offset = 0;
    }
    else
      _offset += sublen;
    
    return sublen;
  }

  @Override
  public int available() throws IOException
  {
    if (_head != null)
      return _head._length - _offset;
    else
      return 0;
  }

  @Override
  public void close()
    throws IOException
  {
    TempBuffer head = _head;
    _head = null;
    
    if (head != null)
      TempBuffer.freeAll(head);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() +  "[]";
  }
}
