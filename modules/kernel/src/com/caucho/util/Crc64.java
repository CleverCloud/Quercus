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

package com.caucho.util;

/**
 * Crc64 hash
 */
public class Crc64 {
  private static final long POLY64REV = 0xd800000000000000L;
  private static final long []CRC_TABLE;

  /**
   * Calculates CRC from a string.
   */
  public static long generate(String value)
  {
    int len = value.length();
    long crc = 0;

    for (int i = 0; i < len; i++)
      crc = next(crc, value.charAt(i));

    return crc;
  }

  /**
   * Calculates CRC from a string.
   */
  public static long generate(long crc, String value)
  {
    int len = value.length();

    for (int i = 0; i < len; i++) {
      char ch = value.charAt(i);

      if (ch > 0xff)
        crc = next(crc, (ch >> 8));

      crc = next(crc, ch);
    }

    return crc;
  }

  /**
   * Calculates CRC from a char buffer
   */
  public static long generate(long crc, char []buffer, int offset, int len)
  {
    for (int i = 0; i < len; i++) {
      char ch = buffer[offset + i];

      if (ch > 0xff)
        crc = next(crc, (ch >> 8));

      crc = next(crc, ch);
    }

    return crc;
  }

  /**
   * Calculates CRC from a char buffer
   */
  public static long generate(long crc, byte []buffer, int offset, int len)
  {
    for (int i = 0; i < len; i++) {
      crc = next(crc, buffer[offset + i]);
    }

    return crc;
  }

  /**
   * Calculates CRC from a long
   */
  public static long generate(long crc, long value)
  {
    crc = next(crc, (byte) (value >> 56));
    crc = next(crc, (byte) (value >> 48));
    crc = next(crc, (byte) (value >> 40));
    crc = next(crc, (byte) (value >> 32));
    crc = next(crc, (byte) (value >> 24));
    crc = next(crc, (byte) (value >> 16));
    crc = next(crc, (byte) (value >> 8));
    crc = next(crc, (byte) (value >> 0));

    return crc;
  }

  /**
   * Calculates the next crc value.
   */
  public static long generate(long crc, byte ch)
  {
    return (crc >>> 8) ^ CRC_TABLE[((int) crc ^ ch) & 0xff];
  }

  /**
   * Calculates the next crc value.
   */
  public static long next(long crc, int ch)
  {
    return (crc >>> 8) ^ CRC_TABLE[((int) crc ^ ch) & 0xff];
  }

  static {
    CRC_TABLE = new long[256];

    for (int i = 0; i < 256; i++) {
      long v = i;

      for (int j = 0; j < 8; j++) {
        boolean flag = (v & 1) != 0;

        long newV = v >>> 1;

        if ((v & 0x100000000L) != 0)
          newV |= 0x100000000L;

        if ((v & 1) != 0)
          newV ^= POLY64REV;

        v = newV;
      }

      CRC_TABLE[i] = v;
    }
  }
}
