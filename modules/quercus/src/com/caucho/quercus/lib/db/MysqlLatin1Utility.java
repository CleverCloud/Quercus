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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.db;

import com.caucho.util.IntArray;
import com.caucho.util.IntMap;

public class MysqlLatin1Utility
{
  // Mysql's "latin1" is not strict ISO-8859-1 as it is more like
  // ISO-8859-1 supplemented with some of Windows-1252 0x80-0x9F
  // characters.
  //
  // XXX: dependent on server version
  //
  private static char []C1_MAP
    = { '\u20AC', '\u0081', '\u201A', '\u0192',
        '\u201E', '\u2026', '\u2020', '\u2021',
        '\u02C6', '\u2030', '\u0160', '\u2039',
        '\u0152', '\u008D', '\u017D', '\u008F',
        '\u0090', '\u2018', '\u2019', '\u201C',
        '\u201D', '\u2022', '\u2013', '\u2014',
        '\u02DC', '\u2122', '\u0161', '\u203A',
        '\u0153', '\u009D', '\u017E', '\u0178'};
  
  private static final byte []UNICODE_MAP = new byte[0x2400];
  
  static {
    for (int i = 0; i < UNICODE_MAP.length; i++)
      UNICODE_MAP[i] = (byte) i;
    
    for (int i = 0; i < C1_MAP.length; i++)
      UNICODE_MAP[C1_MAP[i]] = (byte) (i + 0x80);
  }

  public static char decode(int ch)
  {
    if (0x80 <= ch && ch <= 0x9F)
      return C1_MAP[ch - 0x80];
    else
      return (char) ch;
  }
  
  public static String decode(byte []bytes)
  {
    StringBuilder sb = new StringBuilder();
    
    for (int i = 0; i < bytes.length; i++) {
      byte b = bytes[i];
      
      if (0x80 <= b && b <= 0x9F)
        sb.append(C1_MAP[b - 0x80]);
      else
        sb.append((char) b);
    }
    
    return sb.toString();
  }
  
  public static byte[] encode(String s)
  {
    int len = s.length();
    
    byte []bytes = new byte[len];
    
    for (int i = 0; i < len; i++) {
      int ch = s.charAt(i);
      
      // there was a previous error in converting to a Java String
      if (ch == 0xfffd)
        return null;

      if (ch < UNICODE_MAP.length)
        bytes[i] = UNICODE_MAP[ch];
      else
        bytes[i] = (byte) ch;
    }
    
    return bytes;
  }
}
