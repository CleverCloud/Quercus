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
 * Compares two keys.
 */
public class IntKeyCompare extends KeyCompare {
  /**
   * Compares the key to the block data.
   */
  public int compare(byte []keyBuffer, int keyOffset,
                     byte []block, int offset, int length)
  {
    int key = (((keyBuffer[keyOffset + 0] & 0xff) << 24) +
               ((keyBuffer[keyOffset + 1] & 0xff) << 16) +
               ((keyBuffer[keyOffset + 2] & 0xff) << 8) +
               ((keyBuffer[keyOffset + 3] & 0xff) << 0));
    
    int value = (((block[offset + 0] & 0xff) << 24) +
                 ((block[offset + 1] & 0xff) << 16) +
                 ((block[offset + 2] & 0xff) << 8) +
                 ((block[offset + 3] & 0xff) << 0));

    if (key == value)
      return 0;
    else if (key < value)
      return -1;
    else
      return 1;
  }
  
  /**
   * Returns a printable version of the string.
   */
  @Override
  public String toString(byte []keyBuffer, int keyOffset, int length)
  {
    int key = (((keyBuffer[keyOffset + 0] & 0xff) << 24) +
               ((keyBuffer[keyOffset + 1] & 0xff) << 16) +
               ((keyBuffer[keyOffset + 2] & 0xff) << 8) +
               ((keyBuffer[keyOffset + 3] & 0xff) << 0));

    return String.valueOf(key);
  }
}
