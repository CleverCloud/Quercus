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

package javax.servlet;

import java.io.IOException;
import java.io.InputStream;

/**
 * Servlets generally read POSTed data with ServletInputStream.
 * ServletInputStream adds a readLine method to the standard InputStream
 * API.
 */
public abstract class ServletInputStream extends InputStream {

  protected ServletInputStream()
  {
  }

  /**
   * Reads a line from the POST data.
   *
   * @param buffer buffer to hold the line data
   * @param offset offset into the buffer to start
   * @param length maximum number of bytes to read.
   *
   * @return number of bytes read or -1 on the end of line.
   */
  public int readLine(byte []buffer, int offset, int length)
    throws IOException
  {
    int i = 0;
    for (i = 0; i < length; i++) {
      int ch = read();
      if (ch < 0)
        return i == 0 ? -1 : i;

      buffer[offset + i] = (byte) ch;

      if (ch == '\n')
        return i + 1;
    }

    return i;
  }
}
