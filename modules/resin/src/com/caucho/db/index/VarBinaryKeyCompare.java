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
public class VarBinaryKeyCompare extends KeyCompare {
  /**
   * Compares the key to the block data.
   */
  public int compare(byte []keyBuffer, int keyOffset,
                     byte []block, int offset, int length)
  {
    int keyLen = keyBuffer[keyOffset] & 0xff;
    int blockLen = block[offset] & 0xff;

    int end = keyLen;
    if (blockLen < end)
      end = blockLen;
    
    for (int i = 1; i <= end; i++) {
      int ch1 = keyBuffer[keyOffset + i] & 0xff;
      int ch2 = block[offset + i] & 0xff;

      if (ch1 < ch2)
        return -1;
      else if (ch2 < ch1)
        return 1;
    }

    if (keyLen == blockLen)
      return 0;

    if (keyLen < blockLen)
      return -1;
    else if (blockLen < keyLen)
      return 1;
    else
      return 0;
  }

  public String toString(byte []buffer, int offset, int length)
  {
    StringBuilder sb = new StringBuilder();
    
    int keyLen = buffer[offset];

    for (int j = 0; j < keyLen; j++) {
      int ch = buffer[offset + 1 + j] & 0xff;

      if (ch == 0)
        break;

      if (ch < 0x80)
        sb.append((char) ch);
      else if ((ch & 0xe0) == 0xc0) {
        int ch1 = buffer[offset + 1 + j] & 0xff;
        sb.append((char) (((ch & 0x0f) << 6) + (ch & 0x3f)));
        j++;
      }
      else {
        int ch2 = buffer[offset + 1 + j] & 0xff;
        int ch3 = buffer[offset + 2 + j] & 0xff;
        sb.append((char) (((ch & 0x0f) << 12)
                          + ((ch & 0x3f) << 6)
                          + ((ch & 0x3f))));
        j += 2;
      }
    }

    return sb.toString();
  }
}
