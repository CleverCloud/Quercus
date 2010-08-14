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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.vfs;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract socket to handle both normal sockets and bin/resin sockets.
 */
public class QServerSocketWrapper extends QServerSocket {
  private static final Logger log
    = Logger.getLogger(QServerSocketWrapper.class.getName());
  
  private ServerSocket _ss;
  private boolean _tcpNoDelay = true;
  private int _connectionSocketTimeout = 65000;
  
  public QServerSocketWrapper()
  {
  }

  public QServerSocketWrapper(ServerSocket ss)
  {
    init(ss);
  }

  public void init(ServerSocket ss)
  {
    _ss = ss;
  }

  public void setTcpNoDelay(boolean delay)
  {
    _tcpNoDelay = delay;
  }

  public boolean getTcpNoDelay()
  {
    return _tcpNoDelay;
  }

  public void setConnectionSocketTimeout(int socketTimeout)
  {
    _connectionSocketTimeout = socketTimeout;
  }
  
  /**
   * Accepts a new socket.
   */
  public boolean accept(QSocket qSocket)
    throws IOException
  {
    QSocketWrapper s = (QSocketWrapper) qSocket;

    Socket socket = _ss.accept();

    if (socket == null)
      return false;

    // XXX:
    if (_tcpNoDelay)
      socket.setTcpNoDelay(true);

    if (_connectionSocketTimeout > 0)
      socket.setSoTimeout(_connectionSocketTimeout);

    s.init(socket);

    return true;
  }
  
  /**
   * Creates a new socket object.
   */
  public QSocket createSocket()
    throws IOException
  {
    return new QSocketWrapper();
  }

  public InetAddress getLocalAddress()
  {
    return _ss.getInetAddress();
  }

  public int getLocalPort()
  {
    return _ss.getLocalPort();
  }

  public Selector getSelector()
  {
    try {
      ServerSocketChannel channel = _ss.getChannel();

      if (channel == null)
        return null;
      
      SelectorProvider provider = channel.provider();

      if (provider != null)
        return provider.openSelector();
      else
        return null;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      return null;
    }
  }

  /**
   * Closes the underlying socket.
   */
  public void close()
    throws IOException
  {
    ServerSocket ss = _ss;
    _ss = ss;

    if (ss != null) {
      try {
        ss.close();
      } catch (Exception e) {
      }
    }
  }
  
  public String toString()
  {
    return "ServerSocketWrapper[" + getLocalAddress() + ":" + getLocalPort() + "]";
  }
}

