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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of a TCP stream.  Mostly this just forwards the
 * request to the underlying socket streams.
 */
class TcpStream extends StreamImpl {
  private static final Logger log
    = Logger.getLogger(TcpStream.class.getName());

  private Socket _s;
  private InputStream _is;
  private OutputStream _os;

  private TcpStream(TcpPath path,
                    long connectTimeout,
                    long socketTimeout,
                    boolean isNoDelay)
    throws IOException
  {
    setPath(path);

    //_s = new Socket(path.getHost(), path.getPort());
    _s = new Socket();

    if (connectTimeout > 0)
      _s.connect(path.getSocketAddress(), (int) connectTimeout);
    else
      _s.connect(path.getSocketAddress());

    if (! _s.isConnected())
      throw new IOException("connection timeout");

    if (socketTimeout < 0)
      socketTimeout = 120000;

    _s.setSoTimeout((int) socketTimeout);
    
    if (isNoDelay)
      _s.setTcpNoDelay(true);

    try {
      if (path instanceof TcpsPath) {
        SSLContext context = SSLContext.getInstance("TLS");

        javax.net.ssl.TrustManager tm =
          new javax.net.ssl.X509TrustManager() {
            public java.security.cert.X509Certificate[]
              getAcceptedIssuers() {
              return null;
            }
            public void checkClientTrusted(
                                           java.security.cert.X509Certificate[] cert, String foo) {
            }
            public void checkServerTrusted(
                                           java.security.cert.X509Certificate[] cert, String foo) {
            }
          };


        context.init(null, new javax.net.ssl.TrustManager[] { tm }, null);
        SSLSocketFactory factory = context.getSocketFactory();

        _s = factory.createSocket(_s, path.getHost(), path.getPort(), true);
      }
    } catch (IOException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IOExceptionWrapper(e);
    }

    _is = _s.getInputStream();
    _os = _s.getOutputStream();
  }

  public void setAttribute(String name, Object value)
  {
    if (name.equals("timeout")) {
      try {
        if (value instanceof Number)
          _s.setSoTimeout(((Number) value).intValue());
        else
          _s.setSoTimeout(Integer.parseInt(String.valueOf(value)));
      } catch (SocketException e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
    else if (name.equals("no-delay")) {
      try {
        if (Boolean.TRUE.equals(value)) {
          _s.setTcpNoDelay(true);
        }
      } catch (SocketException e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  static TcpStream openRead(TcpPath path,
                            long connectTimeout,
                            long socketTimeout,
                            boolean isNoDelay)
    throws IOException
  {
    return new TcpStream(path, connectTimeout, socketTimeout, isNoDelay);
  }

  static TcpStream openReadWrite(TcpPath path,
                                 long connectTimeout,
                                 long socketTimeout,
                                 boolean isNoDelay)
    throws IOException
  {
    return new TcpStream(path, connectTimeout, socketTimeout, isNoDelay);
  }

  public boolean canWrite()
  {
    return _os != null;
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
    if (_os != null)
      _os.write(buf, offset, length);
  }

  public boolean canRead()
  {
    return _is != null;
  }

  public int getAvailable() throws IOException
  {
    if (_is != null)
      return _is.available();
    else
      return -1;
  }

  public int read(byte []buf, int offset, int length) throws IOException
  {
    InputStream is = _is;
    
    if (is != null) {
      int len = is.read(buf, offset, length);
      
      if (len < 0)
        close();
      
      return len;
    }
    else
      return -1;
  }

  public void closeWrite() throws IOException
  {
    OutputStream os = _os;
    _os = null;

    try {
      if (os != null)
        _s.shutdownOutput();
    } finally {
      if (_is == null) {
        Socket s = _s;
        _s = null;

        if (s != null)
          s.close();
      }
    }
  }

  public void close() throws IOException
  {
    InputStream is = _is;
    _is = null;

    OutputStream os = _os;
    _os = null;

    Socket s = _s;
    _s = null;

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
