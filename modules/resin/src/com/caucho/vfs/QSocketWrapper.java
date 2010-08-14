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

import com.caucho.inject.Module;
import com.caucho.util.IntMap;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.SelectableChannel;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract socket to handle both normal sockets and bin/resin sockets.
 */
@Module
public class QSocketWrapper extends QSocket {
  private static final Logger log
    = Logger.getLogger(QSocketWrapper.class.getName());
  private static Class<?> sslSocketClass;
  private static IntMap sslKeySizes;
  
  private Socket _s;
  private InputStream _is;
  private OutputStream _os;
  private SocketStream _streamImpl;

  public QSocketWrapper()
  {
  }

  public QSocketWrapper(Socket s)
  {
    init(s);
  }

  public void init(Socket s)
  {
    _s = s;
    _is = null;
    _os = null;
  }
  
  public Socket getSocket()
  {
    return _s;
  }

  /**
   * Sets the socket timeout.
   */
  public void setReadTimeout(int ms)
    throws IOException
  {
    _s.setSoTimeout(ms);
  }

  /**
   * Returns the server inet address that accepted the request.
   */
  @Override
  public InetAddress getLocalAddress()
  {
    return _s.getLocalAddress();
  }
  
  /**
   * Returns the server port that accepted the request.
   */
  @Override
  public int getLocalPort()
  {
    return _s.getLocalPort();
  }

  /**
   * Returns the remote client's inet address.
   */
  @Override
  public InetAddress getRemoteAddress()
  {
    if (_s != null)
      return _s.getInetAddress();
    else
      return null;
  }
  
  /**
   * Returns the remote client's port.
   */
  @Override
  public int getRemotePort()
  {
    if (_s != null)
      return _s.getPort();
    else
      return 0;
  }

  /**
   * Returns true if the connection is secure.
   */
  @Override
  public boolean isSecure()
  {
    if (_s == null || sslSocketClass == null)
      return false;
    else
      return sslSocketClass.isAssignableFrom(_s.getClass());
  }
  /**
   * Returns the secure cipher algorithm.
   */
  @Override
  public String getCipherSuite()
  {
    if (! (_s instanceof SSLSocket))
      return super.getCipherSuite();

    SSLSocket sslSocket = (SSLSocket) _s;
    
    SSLSession sslSession = sslSocket.getSession();
    
    if (sslSession != null)
      return sslSession.getCipherSuite();
    else
      return null;
  }

  /**
   * Returns the bits in the socket.
   */
  @Override
  public int getCipherBits()
  {
    if (! (_s instanceof SSLSocket))
      return super.getCipherBits();
    
    SSLSocket sslSocket = (SSLSocket) _s;
    
    SSLSession sslSession = sslSocket.getSession();
    
    if (sslSession != null)
      return sslKeySizes.get(sslSession.getCipherSuite());
    else
      return 0;
  }
  
  /**
   * Returns the client certificate.
   */
  @Override
  public X509Certificate getClientCertificate()
    throws CertificateException
  {
    X509Certificate []certs = getClientCertificates();

    if (certs == null || certs.length == 0)
      return null;
    else
      return certs[0];
  }

  /**
   * Returns the client certificate.
   */
  @Override
  public X509Certificate []getClientCertificates()
    throws CertificateException
  {
    if (sslSocketClass == null)
      return null;
    else
      return getClientCertificatesImpl();
  }
  
  /**
   * Returns the client certificate.
   */
  private X509Certificate []getClientCertificatesImpl()
    throws CertificateException
  {
    if (! (_s instanceof SSLSocket))
      return null;
    
    SSLSocket sslSocket = (SSLSocket) _s;

    SSLSession sslSession = sslSocket.getSession();
    if (sslSession == null)
      return null;

    try {
      return (X509Certificate []) sslSession.getPeerCertificates();
    } catch (SSLPeerUnverifiedException e) {
      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, e.toString(), e);
      
      return null;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    return null;
  }

  /**
   * Returns the selectable channel.
   */
  @Override
  public SelectableChannel getSelectableChannel()
  {
    if (_s != null)
      return _s.getChannel();
    else
      return null;
  }
  
  /**
   * Returns the socket's input stream.
   */
  @Override
  public StreamImpl getStream()
    throws IOException
  {
    if (_streamImpl == null)
      _streamImpl = new SocketStream();

    _streamImpl.init(_s);

    return _streamImpl;
  }
  
  public void resetTotalBytes()
  {
    if (_streamImpl != null)
      _streamImpl.resetTotalBytes();
  }

  @Override
  public long getTotalReadBytes()
  {
    return (_streamImpl == null) ? 0 : _streamImpl.getTotalReadBytes();
  }

  @Override
  public long getTotalWriteBytes()
  {
    return (_streamImpl == null) ? 0 : _streamImpl.getTotalWriteBytes();
  }

  /**
   * Returns true for closes.
   */
  @Override
  public boolean isClosed()
  {
    return _s == null;
  }

  /**
   * Closes the underlying socket.
   */
  @Override
  public void close()
    throws IOException
  {
    Socket s = _s;
    _s = null;
    
    InputStream is = _is;
    _is = null;
    
    OutputStream os = _os;
    _os = null;

    if (os != null) {
      try {
        os.close();
      } catch (Exception e) {
      }
    }

    if (is != null) {
      try {
        is.close();
      } catch (Exception e) {
      }
    }

    if (s != null) {
      try {
        s.close();
      } catch (Exception e) {
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _s + "]";
  }

  static {
    try {
      sslSocketClass = Class.forName("javax.net.ssl.SSLSocket");
    } catch (Throwable e) {
    }

    sslKeySizes = new IntMap();
    sslKeySizes.put("SSL_DH_anon_WITH_DES_CBC_SHA", 56);
    sslKeySizes.put("SSL_DH_anon_WITH_3DES_EDE_CBC_SHA", 168);
    sslKeySizes.put("SSL_DH_anon_WITH_RC4_128_MD5", 128);
    sslKeySizes.put("SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA", 40);
    sslKeySizes.put("SSL_DH_anon_EXPORT_WITH_RC4_40_MD5", 40);
    sslKeySizes.put("SSL_DHE_DSS_WITH_DES_CBC_SHA", 56);
    sslKeySizes.put("SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", 40);
    sslKeySizes.put("SSL_RSA_WITH_RC4_128_MD5", 128);
    sslKeySizes.put("SSL_RSA_WITH_RC4_128_SHA", 128);
    sslKeySizes.put("SSL_RSA_WITH_DES_CBC_SHA", 56);
    sslKeySizes.put("SSL_RSA_WITH_3DES_EDE_CBC_SHA", 168);
    sslKeySizes.put("SSL_RSA_EXPORT_WITH_RC4_40_MD5", 40);
    sslKeySizes.put("SSL_RSA_WITH_NULL_MD5", 0);
    sslKeySizes.put("SSL_RSA_WITH_NULL_SHA", 0);
    sslKeySizes.put("SSL_DSA_WITH_RC4_128_MD5", 128);
    sslKeySizes.put("SSL_DSA_WITH_RC4_128_SHA", 128);
    sslKeySizes.put("SSL_DSA_WITH_DES_CBC_SHA", 56);
    sslKeySizes.put("SSL_DSA_WITH_3DES_EDE_CBC_SHA", 168);
    sslKeySizes.put("SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA", 168);
    sslKeySizes.put("SSL_DSA_EXPORT_WITH_RC4_40_MD5", 40);
    sslKeySizes.put("SSL_DSA_WITH_NULL_MD5", 0);
    sslKeySizes.put("SSL_DSA_WITH_NULL_SHA", 0);
  } 
}

