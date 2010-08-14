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

import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.*;

import com.caucho.inject.Module;

/**
 * Specialized stream to handle sockets.
 *
 * <p>Unlike VfsStream, when the read() throws and IOException or
 * a SocketException, SocketStream will throw a ClientDisconnectException.
 */
@Module
public class SocketStream extends StreamImpl {
  private static final Logger log
    = Logger.getLogger(SocketStream.class.getName());
  
  private static byte []UNIX_NEWLINE = new byte[] { (byte) '\n' };

  private Socket _s;
  private InputStream _is;
  private OutputStream _os;
  private boolean _needsFlush;
  private byte []_newline = UNIX_NEWLINE;

  private boolean _throwReadInterrupts = false;

  private long _totalReadBytes;
  private long _totalWriteBytes;

  public SocketStream()
  {
  }

  public SocketStream(Socket s)
  {
    init(s);
  }

  /**
   * Initialize the SocketStream with a new Socket.
   *
   * @param s the new socket.
   */
  public void init(Socket s)
  {
    _s = s;
    
    _is = null;
    _os = null;
    _needsFlush = false;
  }

  /**
   * Initialize the SocketStream with a new Socket.
   *
   * @param s the new socket.
   */
  public void init(InputStream is, OutputStream os)
  {
    _is = is;
    _os = os;
    _needsFlush = false;
  }

  /**
   * If true, throws read interrupts instead of returning an end of
   * fail.  Defaults to false.
   */
  public void setThrowReadInterrupts(boolean allowThrow)
  {
    _throwReadInterrupts = allowThrow;
  }

  /**
   * If true, throws read interrupts instead of returning an end of
   * fail.  Defaults to false.
   */
  public boolean getThrowReadInterrupts()
  {
    return _throwReadInterrupts;
  }

  public void setNewline(byte []newline)
  {
    _newline = newline;
  }

  @Override
  public byte []getNewline()
  {
    return _newline;
  }

  /**
   * Returns true if stream is readable and bytes can be skipped.
   */
  @Override
  public boolean hasSkip()
  {
    return canRead();
  }

  /**
   * Skips bytes in the file.
   *
   * @param n the number of bytes to skip
   *
   * @return the actual bytes skipped.
   */
  @Override
  public long skip(long n)
    throws IOException
  {
    if (_is == null) {
      if (_s == null)
        return -1;

      _is = _s.getInputStream();
    }

    return _is.skip(n);
  }

  /**
   * Returns true since the socket stream can be read.
   */
  @Override
  public boolean canRead()
  {
    return _is != null || _s != null;
  }

  /**
   * Reads bytes from the socket.
   *
   * @param buf byte buffer receiving the bytes
   * @param offset offset into the buffer
   * @param length number of bytes to read
   * @return number of bytes read or -1
   * @exception throws ClientDisconnectException if the connection is dropped
   */
  @Override
  public int read(byte []buf, int offset, int length) throws IOException
  {
    try {
      if (_is == null) {
        if (_s == null)
          return -1;
        
        _is = _s.getInputStream();
      }

      int readLength = _is.read(buf, offset, length);

      if (readLength >= 0) {
        _totalReadBytes += readLength;
      }

      return readLength;
    } catch (InterruptedIOException e) {
      if (_throwReadInterrupts)
        throw e;
      
      log.log(Level.FINEST, e.toString(), e);
    } catch (IOException e) {
      if (_throwReadInterrupts)
        throw e;

      log.log(Level.FINER, e.toString(), e);
      // server/0611
      /*
      try {
        close();
      } catch (IOException e1) {
      }
      */
    }

    return -1;
  }

  /**
   * Reads bytes from the socket.
   *
   * @param buf byte buffer receiving the bytes
   * @param offset offset into the buffer
   * @param length number of bytes to read
   * @return number of bytes read or -1
   * @exception throws ClientDisconnectException if the connection is dropped
   */
  @Override
  public int readTimeout(byte []buf, int offset, int length, long timeout)
    throws IOException
  {
    Socket s = _s;
      
    if (s == null)
      return -1;

    int oldTimeout = s.getSoTimeout();

    try {
      s.setSoTimeout((int) timeout);

      int readLength = read(buf, offset, length);

      return readLength;
    } finally {
      s.setSoTimeout(oldTimeout);
    }
  }

  /**
   * Returns the number of bytes available to be read from the input stream.
   */
  @Override
  public int getAvailable() throws IOException
  {
    if (_is == null) {
      if (_s == null)
        return -1;
    
      _is = _s.getInputStream();
    }

    return _is.available();
  }

  @Override
  public boolean canWrite()
  {
    return _os != null || _s != null;
  }

  /**
   * Writes bytes to the socket.
   *
   * @param buf byte buffer containing the bytes
   * @param offset offset into the buffer
   * @param length number of bytes to read
   * @param isEnd if the write is at a close.
   *
   * @exception throws ClientDisconnectException if the connection is dropped
   */
  @Override
  public void write(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    if (_os == null) {
      if (_s == null)
        return;
      
      _os = _s.getOutputStream();
    }
    
    try {
      _needsFlush = true;
      _os.write(buf, offset, length);
      _totalWriteBytes += length;
    } catch (IOException e) {
      try {
        close();
      } catch (IOException e1) {
      }

      throw ClientDisconnectException.create(e);
    }
  }

  /**
   * Flushes the socket.
   */
  @Override
  public void flush() throws IOException
  {
    if (_os == null || ! _needsFlush)
      return;

    _needsFlush = false;
    try {
      _os.flush();
    } catch (IOException e) {
      try {
        close();
      } catch (IOException e1) {
      }
      
      throw ClientDisconnectException.create(e);
    }
  }

  public void resetTotalBytes()
  {
    _totalReadBytes = 0;
    _totalWriteBytes = 0;
  }

  public long getTotalReadBytes()
  {
    return _totalReadBytes;
  }

  public long getTotalWriteBytes()
  {
    return _totalWriteBytes;
  }

  /**
   * Closes the write half of the stream.
   */
  @Override
  public void closeWrite() throws IOException
  {
    OutputStream os = _os;
    _os = null;

    // since the output stream is opened lazily, we might
    // need to open it
    if (_s != null) {
      try {
        _s.shutdownOutput();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    // SSLSocket doesn't support shutdownOutput()
    if (os != null)
      os.close();
  }

  /**
   * Closes the underlying sockets and socket streams.
   */
  @Override
  public void close() throws IOException
  {
    Socket s = _s;
    _s = null;

    OutputStream os = _os;
    _os = null;

    InputStream is = _is;
    _is = null;

    try {
      if (os != null)
        os.close();
      
      if (is != null)
        is.close();
    } finally {
      if (s != null)
        s.close();
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _s + "]";
  }
}

