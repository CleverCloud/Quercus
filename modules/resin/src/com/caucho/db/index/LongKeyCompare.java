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

package com.caucho.db.index;

/**
 * Compares two long keys.
 */
public class LongKeyCompare extends KeyCompare {
  /**
   * Compares the key to the block data.
   */
  @Override
  public int compare(byte []keyBuffer, int keyOffset,
                     byte []block, int offset, int length)
  {
    long key = (((keyBuffer[keyOffset + 0] & 0xffL) << 56)
                + ((keyBuffer[keyOffset + 1] & 0xffL) << 48)
                + ((keyBuffer[keyOffset + 2] & 0xffL) << 40)
                + ((keyBuffer[keyOffset + 3] & 0xffL) << 32)

                + ((keyBuffer[keyOffset + 4] & 0xffL) << 24)
                + ((keyBuffer[keyOffset + 5] & 0xffL) << 16)
                + ((keyBuffer[keyOffset + 6] & 0xffL) << 8)
                + ((keyBuffer[keyOffset + 7] & 0xffL) << 0));
    
    long value = (((block[offset + 0] & 0xffL) << 56)
                  + ((block[offset + 1] & 0xffL) << 48)
                  + ((block[offset + 2] & 0xffL) << 40)
                  + ((block[offset + 3] & 0xffL) << 32)

                  + ((block[offset + 4] & 0xffL) << 24)
                  + ((block[offset + 5] & 0xffL) << 16)
                  + ((block[offset + 6] & 0xffL) << 8)
                  + ((block[offset + 7] & 0xffL) << 0));

    if (key == value)
      return 0;
    else if (key < value)
      return -1;
    else
      return 1;
  }
  /**
   * Compares the key to the block data.
   */
  @Override
  public String toString(byte []keyBuffer, int keyOffset, int length)
  {
    long key = (((keyBuffer[keyOffset + 0] & 0xffL) << 56) +
                ((keyBuffer[keyOffset + 1] & 0xffL) << 48) +
                ((keyBuffer[keyOffset + 2] & 0xffL) << 40) +
                ((keyBuffer[keyOffset + 3] & 0xffL) << 32) +

                ((keyBuffer[keyOffset + 4] & 0xffL) << 24) +
                ((keyBuffer[keyOffset + 5] & 0xffL) << 16) +
                ((keyBuffer[keyOffset + 6] & 0xffL) << 8) +
                ((keyBuffer[keyOffset + 7] & 0xffL) << 0));

    return String.valueOf(key);
  }
}
