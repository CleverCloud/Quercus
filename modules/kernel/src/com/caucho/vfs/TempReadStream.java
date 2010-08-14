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

import java.io.IOException;

public class TempReadStream extends StreamImpl {
  private TempBuffer _cursor;

  private int _offset;
  private boolean _freeWhenDone = true;

  public TempReadStream(TempBuffer cursor)
  {
    init(cursor);
  }

  public TempReadStream()
  {
  }

  public void init(TempBuffer cursor)
  {
    _cursor = cursor;
    _offset = 0;
    _freeWhenDone = true;
  }

  public void setFreeWhenDone(boolean free)
  {
    _freeWhenDone = free;
  }

  @Override
  public boolean canRead() { return true; }

  // XXX: any way to make this automatically free?
  @Override
  public int read(byte []buf, int offset, int length) throws IOException
  {
    TempBuffer cursor = _cursor;
    
    if (cursor == null)
      return -1;

    int sublen = cursor._length - _offset;

    if (length < sublen)
      sublen = length;

    System.arraycopy(cursor._buf, _offset, buf, offset, sublen);

    if (cursor._length <= _offset + sublen) {
      _cursor = cursor._next;

      if (_freeWhenDone) {
        cursor._next = null;
        TempBuffer.free(cursor);
        cursor = null;
      }
      _offset = 0;
    }
    else
      _offset += sublen;

    return sublen;
  }

  @Override
  public int getAvailable() throws IOException
  {
    if (_cursor != null)
      return _cursor._length - _offset;
    else
      return 0;
  }

  @Override
  public void close()
    throws IOException
  {
    if (_freeWhenDone && _cursor != null)
      TempBuffer.freeAll(_cursor);
    
    _cursor = null;
  }

  @Override
  public String toString()
  {
    return "TempReadStream[]";
  }
}
