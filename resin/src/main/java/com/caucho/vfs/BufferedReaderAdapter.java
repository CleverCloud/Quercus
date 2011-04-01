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

package com.caucho.vfs;

import java.io.BufferedReader;
import java.io.IOException;

  /**
   * Trivial adapter so readers and input streams can be used in the
   * same servlet.  This adapter also saves GC because it only needs
   * allocation once.
   */
public class BufferedReaderAdapter extends BufferedReader {
  private ReadStream rs;

  public BufferedReaderAdapter(ReadStream rs)
  {
    // this is only a dummy because the rs.getReader is never actually called
    super(rs.getReader(), 1);
  }

  public void init(ReadStream rs)
  {
    this.rs = rs;
  }

  public int read() throws IOException
  {
    return rs.readChar();
  }

  public int read(char []cbuf, int offset, int length) throws IOException
  {
    return rs.read(cbuf, offset, length);
  }

  public String readLine() throws IOException
  {
    return rs.readln();
  }

  public long skip(long n) throws IOException
  {
    long count = 0;

    for (; count < n && rs.readChar() >= 0; count++) {
    }

    return count;
  }

  public boolean ready() throws IOException
  {
    return rs.available() > 0;
  }

  public void close() throws IOException
  {
  }
}
