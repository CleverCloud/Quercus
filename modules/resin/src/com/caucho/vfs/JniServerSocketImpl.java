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
import java.net.InetAddress;

import com.caucho.util.JniTroubleshoot;
import com.caucho.util.L10N;

/**
 * Abstract socket to handle both normal sockets and bin/resin sockets.
 */
public class JniServerSocketImpl extends QServerSocket {
  private static final L10N L = new L10N(JniServerSocketImpl.class);
  private static final JniTroubleshoot _jniTroubleshoot;

  private long _fd;
  private String _id;
  
  private String _host;

  /**
   * Creates the new server socket.
   */
  private JniServerSocketImpl(String host, int port)
    throws IOException
  {
    _fd = bindPort(host, port);

    _id = host + ":" + port;
    
    _host = host;

    if (_fd == 0)
      throw new IOException(L.l("Socket bind failed for {0}:{1} while running as {2}.  Check for other processes listening to the port and check for permissions (root on unix).",
                                host, port,
                                System.getProperty("user.name")));
  }

  /**
   * Creates the new server socket.
   */
  private JniServerSocketImpl(int fd, int port, boolean isOpen)
    throws IOException
  {
    _fd = nativeOpenPort(fd, port);

    _id = "fd=" + fd + ",port=" + port;

    if (_fd == 0)
      throw new java.net.BindException(L.l("Socket bind failed for port {0} fd={1} opened by watchdog.  Check that the watchdog and Resin permissions are properly configured.", port, fd));
  }

  public static boolean isEnabled()
  {
    return _jniTroubleshoot.isEnabled();
  }

  public static String getInitMessage()
  {
    if (! _jniTroubleshoot.isEnabled())
      return _jniTroubleshoot.getMessage();
    else
      return null;
  }

  /**
   * Returns the file descriptor to OpenSSLFactory.  No other classes
   * may use the file descriptor.
   */
  public long getFd()
  {
    return _fd;
  }

  /**
   * Returns true if this is a JNI socket, to distinguish between
   * file-descriptors we have extra control over.
   */
  @Override
  public boolean isJni()
  {
    return true;
  }
  
  public boolean isJniValid()
  {
    return isEnabled();
  }

  @Override
  public boolean setSaveOnExec()
  {
    return nativeSetSaveOnExec(_fd);
  }

  @Override
  public int getSystemFD()
  {
    return nativeGetSystemFD(_fd);
  }

  /**
   * Sets the socket's listen backlog.
   */
  @Override
  public void listen(int backlog)
  {
    nativeListen(_fd, backlog);
  }

  public static QServerSocket create(String host, int port)
    throws IOException
  {
    _jniTroubleshoot.checkIsValid();

    return new JniServerSocketImpl(host, port);
  }

  public static QServerSocket open(int fd, int port)
    throws IOException
  {
    _jniTroubleshoot.checkIsValid();

    return new JniServerSocketImpl(fd, port, true);
  }

  /**
   * Sets the connection read timeout.
   */
  @Override
  public void setConnectionSocketTimeout(int ms)
  {
    nativeSetConnectionSocketTimeout(_fd, ms);
  }

  /**
   * Accepts a new connection from the socket.
   *
   * @param socket the socket connection structure
   *
   * @return true if the accept returns a new socket.
   */
  @Override
  public boolean accept(QSocket socket)
    throws IOException
  {
    JniSocketImpl jniSocket = (JniSocketImpl) socket;

    if (_fd == 0)
      throw new IOException("accept from closed socket");

    return jniSocket.accept(_fd);
  }

  /**
   * Factory method creating an instance socket.
   */
  @Override
  public QSocket createSocket()
    throws IOException
  {
    return new JniSocketImpl();
  }

  @Override
  public InetAddress getLocalAddress()
  {
    try {
      if (_host != null)
        return InetAddress.getByName(_host);
      else
        return InetAddress.getLocalHost();
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public int getLocalPort()
  {
    return getLocalPort(_fd);
  }

  public boolean isClosed()
  {
    return _fd == 0;
  }

  /**
   * Closes the socket.
   */
  @Override
  public void close()
    throws IOException
  {
    long fd;

    synchronized (this) {
      fd = _fd;
      _fd = 0;
    }

    if (fd != 0)
      closeNative(fd);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }

  @Override
  public void finalize()
    throws Throwable
  {
    try {
      close();
    } catch (Throwable e) {
    }
    
    super.finalize();
  }

  /**
   * Binds the port.
   */
  static native long bindPort(String ip, int port);

  /**
   * Open the port.
   */
  static native long nativeOpenPort(int fd, int port);

  /**
   * Sets the connection read timeout.
   */
  native void nativeSetConnectionSocketTimeout(long fd, int timeout);

  /**
   * Sets the listen backlog
   */
  native void nativeListen(long fd, int listen);

  /**
   * Returns the server's local port.
   */
  private native int getLocalPort(long fd);

  /**
   * Returns the OS file descriptor.
   */
  private native int nativeGetSystemFD(long fd);

  /**
   * Save across an exec.
   */
  private native boolean nativeSetSaveOnExec(long fd);

  /**
   * Closes the server socket.
   */
  native int closeNative(long fd)
    throws IOException;

  static {
    JniTroubleshoot jniTroubleshoot = null;

    try {
      System.loadLibrary("resin_os");
      jniTroubleshoot
        = new JniTroubleshoot(JniServerSocketImpl.class, "resin_os");
    } catch (Throwable e) {
      jniTroubleshoot
        = new JniTroubleshoot(JniServerSocketImpl.class, "resin_os", e);
    }

    _jniTroubleshoot = jniTroubleshoot;
  }
}

