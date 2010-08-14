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
public interface SocketLink {
  /**
   * Returns the connection id.  Primarily for debugging.
   */
  public int getId();

  /**
   * Returns the connection's buffered read stream.
   */
  public ReadStream getReadStream();

  /**
   * Returns the connection's buffered write stream. 
   */
  public WriteStream getWriteStream();

  /**
   * Returns true if secure (ssl)
   */
  public boolean isSecure();
  
  /**
   * Returns the static configured virtual host
   */
  public String getVirtualHost();
  
  /**
   * Returns the local address of the connection
   */
  public InetAddress getLocalAddress();
  
  /**
   * Returns the local host of the connection
   */
  public String getLocalHost();

  /**
   * Returns the local port of the connection
   */
  public int getLocalPort();

  /**
   * Returns the remote address of the connection
   */
  public InetAddress getRemoteAddress();

  /**
   * Returns the remote client's inet address.
   */
  public String getRemoteHost();

  /**
   * Returns the remote address of the connection
   */
  public int getRemoteAddress(byte []buffer, int offset, int length);

  /**
   * Returns the remove port of the connection
   */
  public int getRemotePort();
  
  //
  // SSL-related information
  //
  
  /**
   * Returns the cipher suite
   */
  public String getCipherSuite();
  
  /***
   * Returns the key size.
   */
  public int getKeySize();
  
  /**
   * Returns any client certificates.
   * @throws CertificateException 
   */
  public X509Certificate []getClientCertificates()
    throws CertificateException;
  
  //
  // checks for Port enable/disable
  //
  
  public boolean isPortActive();
  
  //
  // keepalive support
  //

  public boolean isKeepaliveAllocated();

  public void killKeepalive();

  //
  // comet support
  //

  /**
   * Starts a comet request
   */
  public AsyncController toComet(CometHandler cometHandler);
  
  /**
   * Returns true for a comet connection
   */
  public boolean isCometActive();

  /**
   * Returns true for a comet connection
   */
  public boolean isCometSuspend();
  
  public boolean isCometComplete();

  public boolean wake();
  
  //
  // duplex support
  //
  
  /**
   * Returns true for a duplex connection
   */
  public boolean isDuplex();
  

  /**
   * Starts a full duplex (tcp style) request for hmtp/xmpp
   */
  public SocketLinkDuplexController startDuplex(SocketLinkDuplexListener handler);
}
