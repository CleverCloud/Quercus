/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.vfs;

import java.io.IOException;
import java.io.InterruptedIOException;

import com.caucho.inject.Module;

/**
 * Stream using with JNI.
 */
@Module
public class JniStream extends StreamImpl {
  private final static int INTERRUPT_EXN = -2;
  private final static int DISCONNECT_EXN = -3;
  public final static int TIMEOUT_EXN = -4;

  private static NullPath NULL_PATH;

  private final JniSocketImpl _socket;
  
  private IOException _readException;

  private long _totalReadBytes;
  private long _totalWriteBytes;

  /**
   * Create a new JniStream based on the java.io.* stream.
   */
  public JniStream(JniSocketImpl socket)
  {
    _socket = socket;
    if (NULL_PATH == null)
      NULL_PATH = new NullPath("jni-stream");
    setPath(NULL_PATH);
  }

  public void init()
  {
    _readException = null;
  }

  @Override
  public boolean canRead()
  {
    return ! _socket.isClosed();
  }

  public int read(byte []buf, int offset, int length)
    throws IOException
  {
    if (buf == null)
      throw new NullPointerException();
    else if (offset < 0 || buf.length < offset + length)
      throw new ArrayIndexOutOfBoundsException();
    else if (_readException != null)
      throw _readException;

    int result = _socket.read(buf, offset, length, -1);
    
    if (result > 0) {
      _totalReadBytes += result;
      return result;
    }
    else if (result < -1) {
      _readException = exception(result);
      
      throw _readException;
    }
    else
      return -1;
  }

  @Override
  public int readTimeout(byte []buf, int offset, int length, long timeout)
    throws IOException
  {
    if (buf == null)
      throw new NullPointerException();
    else if (offset < 0 || buf.length < offset + length)
      throw new ArrayIndexOutOfBoundsException();

    int result = _socket.read(buf, offset, length, timeout);

    if (result > 0) {
      _totalReadBytes += result;
      return result;
    }
    else if (result == TIMEOUT_EXN) {
      return ReadStream.READ_TIMEOUT;
    }
    else if (result < -1) {
      throw exception(result);
    }
    else
      return -1;
  }

  // XXX: needs update
  public int getAvailable() throws IOException
  {
    return 0;
  }

  @Override
  public boolean canWrite()
  {
    return ! _socket.isClosed();
  }

  @Override
  public void write(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    if (length <= 0)
      return;
    else if (buf == null)
      throw new NullPointerException();
    else if (offset < 0 || buf.length < offset + length)
      throw new ArrayIndexOutOfBoundsException();

    int result = _socket.write(buf, offset, length, isEnd);

    if (result <= -1) {
      // server/1l21: -1 with exception is necessary to catch client disconnect
      throw exception(result);
    }

    _totalWriteBytes += result;
  }

  public void flush()
    throws IOException
  {
  }

  public long getTotalReadBytes()
  {
    return _totalReadBytes;
  }

  public long getTotalWriteBytes()
  {
    return _totalWriteBytes;
  }

  IOException exception(int result)
     throws IOException
  {
    switch (result) {
    case INTERRUPT_EXN:
      return new InterruptedIOException("interrupted i/o");

    case DISCONNECT_EXN:
      return new ClientDisconnectException("connection reset by peer");

    case TIMEOUT_EXN:
      return new SocketTimeoutException("client timeout");

    default:
      return new ClientDisconnectException("unknown exception=" + result);
    }
  }

  /**
   * Closes the stream.
   */
  public void close() throws IOException
  {
    _socket.close();
  }

  /*
  public void finalize()
    throws IOException
  {
    close();
  }
  */
}

