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
import java.util.logging.*;

import com.caucho.server.util.CauchoSystem;
import com.caucho.util.JniTroubleshoot;

/**
 * Stream using with JNI.
 */
public class JniFileStream extends StreamImpl
    implements LockableStream
{
  private static final Logger log
    = Logger.getLogger(JniFileStream.class.getName());

  private static final JniTroubleshoot _jniTroubleshoot;
  
  private int _fd;
  private long _pos;

  private boolean _canRead;
  private boolean _canWrite;

  /**
   * Create a new JniStream based on the java.io.* stream.
   */
  public JniFileStream(int fd, boolean canRead, boolean canWrite)
  {
    _jniTroubleshoot.checkIsValid();

    init(fd, canRead, canWrite);
  }

  void init(int fd, boolean canRead, boolean canWrite)
  {
    _fd = fd;
    _pos = 0;

    _canRead = canRead;
    _canWrite = canWrite;
  }

  public static boolean isEnabled()
  {
    return _jniTroubleshoot.isEnabled();
  }

  public static JniFileStream openRead(byte []name, int length)
  {
    int fd = nativeOpenRead(name, length);

    if (fd >= 0)
      return new JniFileStream(fd, true, false);
    else
      return null;
  }

  public static JniFileStream openWrite(byte []name,
                                        int length,
                                        boolean isAppend)
  {
    int fd = nativeOpenWrite(name, length, isAppend);
    
    if (fd >= 0)
      return new JniFileStream(fd, false, true);
    else
      return null;
  }

  public boolean canRead()
  {
    return _canRead && _fd >= 0;
  }

  public boolean hasSkip()
  {
    return _fd >= 0;
  }

  public long skip(long length)
    throws IOException
  {
    long pos = nativeSkip(_fd, length);

    if (pos < 0)
      return -1;
    else {
      _pos = pos;
      return length;
    }
  }

  /**
   * Reads data from the file.
   */
  public int read(byte []buf, int offset, int length)
    throws IOException
  {
    if (buf == null)
      throw new NullPointerException();
    else if (offset < 0 || buf.length < offset + length)
      throw new ArrayIndexOutOfBoundsException();

    int result = nativeRead(_fd, buf, offset, length);

    if (result > 0)
      _pos += result;

    return result;
  }

  // XXX: needs update
  public int getAvailable() throws IOException
  {
    if (_fd < 0) {
      return -1;
    }
    else if (getPath() instanceof FilesystemPath) {
      long length = getPath().getLength();
      
      return (int) (length - _pos);
    }
    else
      return nativeAvailable(_fd);
  }

  /**
   * Returns true if this is a writeable stream.
   */
  public boolean canWrite()
  {
    return _canWrite && _fd >= 0;
  }

  /**
   * Writes data to the file.
   */
  public void write(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    if (buf == null)
      throw new NullPointerException();
    else if (offset < 0 || buf.length < offset + length)
      throw new ArrayIndexOutOfBoundsException();

    nativeWrite(_fd, buf, offset, length);
  }

  public void seekStart(long offset)
    throws IOException
  {
    nativeSeekStart(_fd, offset);
  }

  public void seekEnd(long offset)
    throws IOException
  {
    nativeSeekEnd(_fd, offset);
  }

  public void flush()
    throws IOException
  {
  }

  public void flushToDisk()
    throws IOException
  {
    nativeFlushToDisk(_fd);
  }

  public void close()
    throws IOException
  {
    int fd;

    synchronized (this) {
      fd = _fd;
      _fd = -1;
    }
    
    nativeClose(fd);
  }

  protected void finalize()
    throws IOException
  {
    close();
  }

  /**
   * Native interface to read bytes from the input.
   */
  native int nativeRead(int fd, byte []buf, int offset, int length)
    throws IOException;

  /**
   * Native interface to read bytes from the input.
   */
  native int nativeAvailable(int fd)
    throws IOException;

  /**
   * Native to open a file for reading
   */
  private static native int nativeOpenRead(byte []name, int length);
  
  /**
   * Native to open a file for writing
   */
  private static native int nativeOpenWrite(byte []name, int length,
                                            boolean isAppend);

  /*
  private static native void nativeChown(byte []path, int length,
                                         String user, String group);
  */

  /**
   * Native interface to write bytes to the file
   */
  native int nativeWrite(int fd, byte []buf, int offset, int length)
    throws IOException;

  /**
   * Native interface to skip bytes from the input.
   */
  native long nativeSkip(int fd, long skip)
    throws IOException;

  /**
   * Native interface to force data to the disk
   */
  native int nativeFlushToDisk(int fd)
    throws IOException;

  /**
   * Native interface to read bytes from the input.
   */
  static native int nativeClose(int fd)
    throws IOException;

  /**
   * Native interface to seek from the beginning
   */
  native int nativeSeekStart(int fd, long offset)
    throws IOException;

  /**
   * Native interface to seek from the beginning
   */
  native int nativeSeekEnd(int fd, long offset)
    throws IOException;

  /**
   * Implement LockableStream as a no-op, but maintain
   * compatibility with FileWriteStream and FileReadStream
   * wrt returning false to indicate error case.
   */

  public boolean lock(boolean shared, boolean block)
  {
    if (shared && !_canRead) {
      // Invalid request for a shared "read" lock on a write only stream.

      return false;
    }

    if (!shared && !_canWrite) {
      // Invalid request for an exclusive "write" lock on a read only stream.

      return false;
    }

    return true;
  }

  public boolean unlock()
  {
    return true;
  }

  /**
   * Returns the debug name for the stream.
   */
  public String toString()
  {
    return "JniFileStream[" + getPath().getNativePath() + "]";
  }

  static {
    JniTroubleshoot jniTroubleshoot = null;

    try {
      System.loadLibrary("resin_os");
      jniTroubleshoot = new JniTroubleshoot(JniFileStream.class, "resin_os");
    } catch (Throwable e) {
      jniTroubleshoot = new JniTroubleshoot(JniFileStream.class, "resin_os", e);
    }

    _jniTroubleshoot = jniTroubleshoot;
  }
}

