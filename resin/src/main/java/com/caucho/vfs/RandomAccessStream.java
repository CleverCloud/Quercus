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

package com.caucho.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Reads from a file in a random-access fashion.
 */
abstract public class RandomAccessStream
  implements LockableStream
{
  /**
   * Returns the length.
   */
  abstract public long getLength()
    throws IOException;
  
  /**
   * Reads a block starting from the current file pointer.
   */
  abstract public int read(byte []buffer, int offset, int length)
    throws IOException;

  /**
   * Reads a block starting from the current file pointer.
   */
  abstract public int read(char []buffer, int offset, int length)
    throws IOException;

  /**
   * Reads a block from a given location.
   */
  abstract public int read(long fileOffset,
                           byte []buffer, int offset, int length)
    throws IOException;

  /**
   * Writes a block starting from the current file pointer.
   */
  abstract public void write(byte []buffer, int offset, int length)
    throws IOException;

  /**
   * Writes a block to a given location.
   */
  abstract public void write(long fileOffset,
                             byte []buffer, int offset, int length)
    throws IOException;

  /**
   * Seeks to the given position in the file.
   */
  abstract public boolean seek(long position);

  /**
   * Returns an OutputStream for this stream.
   */
  abstract public OutputStream getOutputStream()
    throws IOException;

  /**
   * Returns an InputStream for this stream.
   */
  abstract public InputStream getInputStream()
    throws IOException;

  /**
   * Read a byte from the file, advancing the pointer.
   */
  abstract public int read()
    throws IOException;

  /**
   * Write a byte to the file, advancing the pointer.
   */
  abstract public void write(int b)
    throws IOException;

  /**
   * Returns the current position of the file pointer.
   */
  abstract public long getFilePointer()
    throws IOException;

  /**
   * Closes the stream.
   */
  public void close() throws IOException
  {
  }

  // Placeholder for LockableStream implementation

  public boolean lock(boolean shared, boolean block)
  {
    return true;
  }

  public boolean unlock()
  {
    return true;
  }
}
