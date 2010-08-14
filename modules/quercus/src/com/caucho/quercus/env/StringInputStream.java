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

package com.caucho.quercus.env;

import java.io.InputStream;

/**
 * Represents an ISO-8859-1 input stream.
 */
public class StringInputStream extends InputStream {
  private final String _string;
  private final int _length;

  private int _index;

  public StringInputStream(String s)
  {
    _string = s;
    _length = s.length();
  }

  public int read()
  {
    if (_index < _length)
      return _string.charAt(_index++);
    else
      return -1;
  }

  public int read(byte []buffer, int offset, int length)
  {
    int sublen = _length - _index;

    if (sublen == 0)
      return -1;
    
    if (length < sublen)
      sublen = length;

    String s = _string;
    int index = _index;

    for (int i = 0; i < sublen; i++) {
      buffer[offset + i] = (byte) s.charAt(index + i);
    }

    _index = index + sublen;

    return sublen;
  }
}

