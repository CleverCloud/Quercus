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

package com.caucho.network.listen;

import java.net.InetAddress;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Represents a protocol-independent connection.  Protocol servers and
 * their associated Requests use Connection to retrieve the read and
 * write streams and to get information about the connection.
 *
 * <p>TcpConnection is the most common implementation.  The test harness
 * provides a string based Connection.
 */
public abstract class AbstractSocketLink
  implements SocketLink
{
  private final ReadStream _readStream;
  private final WriteStream _writeStream;

  public AbstractSocketLink()
  {
    _readStream = new ReadStream();
    _readStream.setReuseBuffer(true);
    _writeStream = new WriteStream();
    _writeStream.setReuseBuffer(true);
  }

  /**
   * Returns the connection id.  Primarily for debugging.
   */
  @Override
  abstract public int getId();

  /**
   * Returns the connection's buffered read stream.  If the ReadStream
   * needs to block, it will automatically flush the corresponding
   * WriteStream.
   */
  @Override
  public final ReadStream getReadStream()
  {
    return _readStream;
  }

  /**
   * Returns the connection's buffered write stream.  If the ReadStream
   * needs to block, it will automatically flush the corresponding
   * WriteStream.
   */
  @Override
  public final WriteStream getWriteStream()
  {
    return _writeStream;
  }

  /**
   * Returns true if secure (ssl)
   */
  @Override
  public boolean isSecure()
  {
    return false;
  }
  /**
   * Returns the static virtual host
   */
  @Override
  public String getVirtualHost()
  {
    return null;
  }
  /**
   * Returns the local address of the connection
   */
  @Override
  public abstract InetAddress getLocalAddress();

  /**
   * Returns the remote client's inet address.
   */
  @Override
  public String getLocalHost()
  {
    InetAddress localAddress = getLocalAddress();
    
    if (localAddress != null)
      return localAddress.getHostAddress();
    else
      return null;
  }

  /**
   * Returns the local port of the connection
   */
  @Override
  public abstract int getLocalPort();

  /**
   * Returns the remote address of the connection
   */
  @Override
  public abstract InetAddress getRemoteAddress();

  /**
   * Returns the remote client's inet address.
   */
  @Override
  public String getRemoteHost()
  {
    InetAddress remoteAddress = getRemoteAddress();
    
    if (remoteAddress != null)
      return remoteAddress.getHostAddress();
    else
      return null;
  }

  /**
   * Returns the remote address of the connection
   */
  @Override
  public int getRemoteAddress(byte []buffer, int offset, int length)
  {
    InetAddress remote = getRemoteAddress();
    String name = remote.getHostAddress();
    int len = name.length();

    for (int i = 0; i < len; i++)
      buffer[offset + i] = (byte) name.charAt(i);

    return len;
  }

  /**
   * Returns the remove port of the connection
   */
  @Override
  public abstract int getRemotePort();
  
  //
  // SSL api
  //
  
  /**
   * Returns the cipher suite
   */
  @Override
  public String getCipherSuite()
  {
    return null;
  }
  
  /***
   * Returns the key size.
   */
  @Override
  public int getKeySize()
  {
    return 0;
  }
  
  /**
   * Returns any client certificates.
   * @throws CertificateException 
   */
  @Override
  public X509Certificate []getClientCertificates()
    throws CertificateException
  {
    return null;
  }
  
  @Override
  public boolean isKeepaliveAllocated()
  {
    return false;
  }

  @Override
  public void killKeepalive()
  {
  }

  public SocketLinkState getState()
  {
    return SocketLinkState.REQUEST_ACTIVE_KA;
  }

  /**
   * Returns true for a comet connection
   */
  @Override
  public boolean isCometActive()
  {
    return false;
  }

  /**
   * Returns true for a comet connection
   */
  @Override
  public boolean isCometSuspend()
  {
    return false;
  }
  
  @Override
  public boolean isCometComplete()
  {
    return false;
  }

  /**
   * Returns true for a duplex connection
   */
  @Override
  public boolean isDuplex()
  {
    return false;
  }

  @Override
  public boolean wake()
  {
    return false;
  }

  /**
   * Starts a comet request
   */
  @Override
  public AsyncController toComet(CometHandler cometHandler)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
