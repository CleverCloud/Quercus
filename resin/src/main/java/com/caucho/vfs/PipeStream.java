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

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * Stream allowing two threads to read and write to each other.
 */
public class PipeStream extends StreamImpl {
  private PipeStream sibling;
  private byte[] readBuffer;
  private int readOffset;
  private int readLength;

  private PipeStream()
  {
    setPath(new NullPath("pipe"));
    readBuffer = new byte[2 * TempBuffer.SIZE];
    readOffset = 0;
    readLength = 0;
  }

  /**
   * Creates a pipe pair.  The first object is a ReadStream, the second
   * is a WriteStream.
   */
  public static Object []create()
  {
    PipeStream a = new PipeStream();
    PipeStream b = new PipeStream();

    a.sibling = b;
    b.sibling = a;

    return new Object[] { new ReadStream(a, null), new WriteStream(b) };
  }

  /**
   * PipeStreams can read
   */
  public boolean canRead()
  {
    return true;
  }

  /**
   * Reads the available bytes if any, otherwise block.
   */
  public int read(byte []buf, int offset, int length) throws IOException
  {
    if (readBuffer == null)
      return 0;

    synchronized (this) {
      try {
        if (readOffset >= readLength) {
          // Sibling has closed
          if (sibling.readBuffer == null)
            return 0;

          notifyAll();
          wait();
        }

        int sublen = readLength - readOffset;
        if (sublen <= 0)
          return 0;

        if (length < sublen)
          sublen = length;

        System.arraycopy(readBuffer, readOffset, buf, offset, sublen);
        readOffset += sublen;

        return sublen;
      } catch (InterruptedException e) {
        throw new InterruptedIOException(e.getMessage());
      }
    }
  }

  /**
   * Return the available bytes.
   */
  public int getAvailable() throws IOException
  {
    synchronized (this) {
      return readLength - readOffset;
    }
  }

  /**
   * The pipe stream can write.
   */
  public boolean canWrite()
  {
    return true;
  }

  /**
   * Implementation of the pipe write.
   *
   * @param buf byte buffer containing the bytes
   * @param offset offset where to start writing
   * @param length number of bytes to write
   * @param isEnd true when the write is flushing a close.
   */
  public void write(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    while (length > 0) {
      synchronized (sibling) {
        if (sibling.readBuffer == null)
          return;

        if (sibling.readLength == sibling.readBuffer.length) {
          if (sibling.readOffset < sibling.readLength) {
            try {
              sibling.wait();
            } catch (InterruptedException e) {
              throw new InterruptedIOException(e.getMessage());
            }
          }
          sibling.readOffset = 0;
          sibling.readLength = 0;
        }

        if (sibling.readOffset == sibling.readLength) {
          sibling.readOffset = 0;
          sibling.readLength = 0;
        }

        if (sibling.readBuffer == null)
          return;

        int sublen = sibling.readBuffer.length - sibling.readLength;
        if (length < sublen)
          sublen = length;

        System.arraycopy(buf, offset,
                         sibling.readBuffer, sibling.readLength, sublen);

        sibling.readLength += sublen;

        length -= sublen;
        offset += sublen;

        sibling.notifyAll();
      }
    }
  }

  public void close() throws IOException
  {
    if (readBuffer == null)
      return;

    synchronized (this) {
      readBuffer = null;
      readLength = 0;
      readOffset = 0;
    
      notifyAll();
    }

    synchronized (sibling) {
      sibling.notifyAll();
    }
  }
}
