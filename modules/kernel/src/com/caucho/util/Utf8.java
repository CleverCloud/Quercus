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

package com.caucho.util;

import java.io.*;

/**
 * Utf8 decoding.
 */
public final class Utf8 {
  public static void write(OutputStream os, char ch)
    throws IOException
  {
    if (ch < 0x80)
      os.write(ch);
    else if (ch < 0x800) {
      os.write(0xc0 + (ch >> 6));
      os.write(0x80 + (ch & 0x3f));
    }
    else {
      os.write(0xe0 + (ch >> 12));
      os.write(0x80 + ((ch >> 6) & 0x3f));
      os.write(0x80 + (ch & 0x3f));
    }
  }
  
  public static void write(OutputStream os, String s)
    throws IOException
  {
    int len = s.length();

    for (int i = 0; i < len; i++) {
      write(os, s.charAt(i));
    }
  }

  public static int read(InputStream is)
    throws IOException
  {
    int ch1 = is.read();

    if (ch1 < 0x80)
      return ch1;

    if ((ch1 & 0xe0) == 0xc0) {
      int ch2 = is.read();

      if (ch2 < 0)
        return -1;
      else if ((ch2 & 0x80) != 0x80)
        return 0xfdff;

      return (((ch1 & 0x1f) << 6)
              | (ch2 & 0x3f));
    }
    else if ((ch1 & 0xf0) == 0xe0) {
      int ch2 = is.read();
      int ch3 = is.read();

      if (ch2 < 0)
        return -1;
      else if ((ch2 & 0x80) != 0x80)
        return 0xfdff;
      else if ((ch3 & 0x80) != 0x80)
        return 0xfdff;

      return (((ch1 & 0xf) << 12)
              | ((ch2 & 0x3f) << 6)
              | ((ch3 & 0x3f) << 6));
    }
    else if (ch1 == 0xff)
      return 0xffff;
    else
      return 0xfdff;
  }
}
