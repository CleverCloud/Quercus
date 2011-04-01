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
 * @author Nam Nguyen
 */

package com.caucho.vfs;

import java.io.IOException;
import java.io.Reader;

import com.caucho.util.L10N;

public class ReaderStream extends StreamImpl {
  private static final L10N L = new L10N(ReaderStream.class);
  
  private Reader _reader;
  private int _peek = -1;

  ReaderStream(Reader reader)
  {
    _reader = reader;
  }

  public static ReadStream open(Reader reader)
  {
    ReaderStream ss = new ReaderStream(reader);
    return new ReadStream(ss);
  }

  public Path getPath()
  {
    throw new UnsupportedOperationException();
  }

  public boolean canRead()
  {
    return true;
  }

  // XXX: encoding issues
  public int read(byte []buf, int offset, int length) throws IOException
  {
    int i = offset;
    int end = i + length;

    while (i < end) {
      int ch;

      if (_peek >= 0) {
        ch = _peek;
        _peek = -1;
      }
      else
        ch = _reader.read();
      
      if (ch < 0)
        break;

      if (ch < 0x80)
            buf[i++] = (byte) ch;
      else if (ch < 0x800) {
        if (i + 1 < end) {
        }
        else if (i == offset)
          throw new IllegalStateException(L.l("buffer is not large enough to accept UTF-8 encoding.  length={0}, 2-character utf-8",
                                              length));
        else {
          _peek = ch;
          return end - offset;
        }

        buf[i++] = (byte) (0xc0 | (ch >> 6));
        buf[i++] = (byte) (0x80 | (ch & 0x3f));
      }
      else if (ch < 0x8000) {
        if (i + 2 < end) {
        }
        else if (i == offset)
          throw new IllegalStateException(L.l("buffer is not large enough to accept UTF-8 encoding.  length={0}, 3-character utf-8",
                                              length));
        else {
          _peek = ch;
          return i - offset;
        }

        buf[i++] = (byte) (0xe0 | (ch >> 12));
        buf[i++] = (byte) (0x80 | ((ch >> 6) & 0x3f));
        buf[i++] = (byte) (0x80 | ((ch >> 6) & 0x3f));
      }
    }

    return i - offset;
  }

  public int getAvailable() throws IOException
  {
    throw new UnsupportedOperationException();
  }
}
