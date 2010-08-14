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

package com.caucho.db.index;

/**
 * Compares two keys.
 */
public class BinaryKeyCompare extends KeyCompare {
  private final int _length;

  public BinaryKeyCompare(int length)
  {
    _length = length;
  }
  
  /**
   * Compares the key to the block data.
   */
  public int compare(byte []keyBuffer, int keyOffset,
                     byte []block, int offset, int length)
  {
    int end = _length;
    
    for (int i = 0; i < end; i++) {
      int ch1 = keyBuffer[keyOffset + i] & 0xff;
      int ch2 = block[offset + i] & 0xff;

      if (ch1 < ch2)
        return -1;
      else if (ch2 < ch1)
        return 1;
    }

    return 0;
  }

  public String toString(byte []buffer, int offset, int length)
  {
    StringBuilder sb = new StringBuilder();
    
    int keyLen = _length;

    for (int j = 0; j < keyLen; j++) {
      int ch = buffer[offset + j] & 0xff;

      int d1 = (ch >> 4) & 0xf;
      int d2 = (ch) & 0xf;

      if (d1 < 10)
        sb.append((char) ('0' + d1));
      else
        sb.append((char) ('a' + d1 - 10));

      if (d2 < 10)
        sb.append((char) ('0' + d2));
      else
        sb.append((char) ('a' + d2 - 10));
    }

    return sb.toString();
  }
}
