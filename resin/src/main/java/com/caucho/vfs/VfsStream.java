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
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Stream encapsulating InputStream/OutputStream.
 */
public class VfsStream extends StreamImpl {
  private static byte []unixNewline = new byte[] { (byte) '\n' };

  private InputStream is;
  private OutputStream _os;
  private boolean flushOnNewline;
  private boolean closeChildOnClose = true;
  private byte []newline = unixNewline;

  private long position;

  /**
   * Create an empty VfsStream.
   */
  public VfsStream()
  {
  }

  /**
   * Create a new VfsStream based on the java.io.* stream.
   */
  public VfsStream(InputStream is, OutputStream os)
  {
    init(is, os);
  }

  public VfsStream(InputStream is, OutputStream os, Path path)
  {
    init(is, os);
    setPath(path);
  }

  /**
   * Initializes a VfsStream with an input/output stream pair.  Before a
   * read, the output will be flushed to avoid deadlocks.
   *
   * @param is the underlying InputStream.
   * @param os the underlying OutputStream.
   */
  public void init(InputStream is, OutputStream os)
  {
    this.is = is;
    _os = os;
    setPath(null);
    flushOnNewline = false;
    closeChildOnClose = true;
    position = 0;
  }

  public void setNewline(byte []newline)
  {
    this.newline = newline;
  }

  public byte []getNewline()
  {
    return newline;
  }

  public static ReadWritePair openReadWrite(InputStream is, OutputStream os)
  {
    VfsStream s = new VfsStream(is, os);
    WriteStream writeStream = new WriteStream(s);
    ReadStream readStream = new ReadStream(s, writeStream);
    return new ReadWritePair(readStream, writeStream);
  }

  /**
   * Opens a read stream based on a java.io.InputStream.
   *
   * @param is the underlying InputStream.
   *
   * @return the new ReadStream
   */
  public static ReadStream openRead(InputStream is)
  {
    VfsStream s = new VfsStream(is, null);
    return new ReadStream(s);
  }

  public static ReadStream openRead(InputStream is, WriteStream ws)
  {
    VfsStream s = new VfsStream(is, null);
    return new ReadStream(s, ws);
  }

  public static WriteStream openWrite(OutputStream os)
  {
    VfsStream s = new VfsStream(null, os);
    return new WriteStream(s);
  }

  public boolean canRead()
  {
    return is != null;
  }

  public int read(byte []buf, int offset, int length) throws IOException
  {
    if (is == null)
      return -1;

    int len = is.read(buf, offset, length);

    if (len > 0)
      position += len;

    return len;
  }

  public boolean hasSkip()
  {
    return true;
  }

  public long skip(long n)
    throws IOException
  {
    return is.skip(n);
  }

  public int getAvailable() throws IOException
  {
    if (is == null)
      return -1;
    else
      return is.available();
  }

  public long getReadPosition()
  {
    return position;
  }

  public boolean canWrite()
  {
    return _os != null;
  }

  public boolean getFlushOnNewline()
  {
    return flushOnNewline;
  }

  public void setFlushOnNewline(boolean value)
  {
    flushOnNewline = value;
  }

  /**
   * Writes a buffer to the underlying stream.
   *
   * @param buffer the byte array to write.
   * @param offset the offset into the byte array.
   * @param length the number of bytes to write.
   * @param isEnd true when the write is flushing a close.
   */
  public void write(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    OutputStream os = _os;
    
    if (os != null) {
      os.write(buf, offset, length);
    }
  }

  public void flushToDisk() throws IOException
  {
    flush();
  }

  public void flush() throws IOException
  {
    if (_os != null) {
      _os.flush();
    }
  }

  public void setCloseChildOnClose(boolean close)
  {
    closeChildOnClose = close;
  }

  public void close() throws IOException
  {
    try {
      if (_os != null && closeChildOnClose) {
        _os.close();
        _os = null;
      }
    } finally {
      if (is != null && closeChildOnClose) {
        is.close();
        is = null;
      }
    }
  }
}
