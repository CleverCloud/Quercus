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

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.crypto.*;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.logging.*;
import java.security.*;
import java.security.cert.Certificate;

import java.net.*;

/**
 * Abstract socket to handle both normal sockets and bin/resin sockets.
 */
public class JsseSSLFactory implements SSLFactory {
  private static final Logger log
    = Logger.getLogger(JsseSSLFactory.class.getName());
  
  private static final L10N L = new L10N(JsseSSLFactory.class);
  
  private Path _keyStoreFile;
  private String _alias;
  private String _password;
  private String _verifyClient;
  private String _keyStoreType = "jks";
  private String _keyManagerFactory = "SunX509";
  private String _sslContext = "TLS";
  private String []_cipherSuites;
  private String []_protocols;

  private String _selfSignedName;

  private KeyStore _keyStore;
  
  /**
   * Creates a ServerSocket factory without initializing it.
   */
  public JsseSSLFactory()
  {
  }

  /**
   * Sets the enabled cipher suites
   */
  public void setCipherSuites(String []ciphers)
  {
    _cipherSuites = ciphers;
  }

  /**
   * Sets the key store
   */
  public void setKeyStoreFile(Path keyStoreFile)
  {
    _keyStoreFile = keyStoreFile;
  }

  /**
   * Returns the certificate file.
   */
  public Path getKeyStoreFile()
  {
    return _keyStoreFile;
  }

  /**
   * Sets the password.
   */
  public void setPassword(String password)
  {
    _password = password;
  }

  /**
   * Returns the key file.
   */
  public String getPassword()
  {
    return _password;
  }

  /**
   * Sets the certificate alias
   */
  public void setAlias(String alias)
  {
    _alias = alias;
  }

  /**
   * Returns the alias.
   */
  public String getAlias()
  {
    return _alias;
  }

  /**
   * Sets the verifyClient.
   */
  public void setVerifyClient(String verifyClient)
  {
    _verifyClient = verifyClient;
  }

  /**
   * Returns the key file.
   */
  public String getVerifyClient()
  {
    return _verifyClient;
  }

  /**
   * Sets the key-manager-factory
   */
  public void setKeyManagerFactory(String keyManagerFactory)
  {
    _keyManagerFactory = keyManagerFactory;
  }

  /**
   * Sets the self-signed certificate name
   */
  public void setSelfSignedCertificateName(String name)
  {
    _selfSignedName = name;
  }

  /**
   * Sets the ssl-context
   */
  public void setSSLContext(String sslContext)
  {
    _sslContext = sslContext;
  }

  /**
   * Sets the key-store
   */
  public void setKeyStoreType(String keyStore)
  {
    _keyStoreType = keyStore;
  }

  /**
   * Sets the protocol
   */
  public void setProtocol(String protocol)
  {
    _protocols = protocol.split("[\\s,]+");
  }

  /**
   * Initialize
   */
  @PostConstruct
  public void init()
    throws ConfigException, IOException, GeneralSecurityException
  {
    if (_keyStoreFile != null && _password == null)
      throw new ConfigException(L.l("'password' is required for JSSE."));
    if (_password != null && _keyStoreFile == null)
      throw new ConfigException(L.l("'key-store-file' is required for JSSE."));

    if (_alias != null && _keyStoreFile == null)
      throw new ConfigException(L.l("'alias' requires a key store for JSSE."));

    if (_keyStoreFile == null && _selfSignedName == null)
      throw new ConfigException(L.l("JSSE requires a key-store-file or a self-signed-certificate-name."));

    if (_keyStoreFile == null)
      return;
    
    _keyStore = KeyStore.getInstance(_keyStoreType);
    
    InputStream is = _keyStoreFile.openRead();
    try {
      _keyStore.load(is, _password.toCharArray());
    } finally {
      is.close();
    }

    if (_alias != null) {
      Key key = _keyStore.getKey(_alias, _password.toCharArray());

      if (key == null)
        throw new ConfigException(L.l("JSSE alias '{0}' does not have a corresponding key.",
                                  _alias));

      Certificate []certChain = _keyStore.getCertificateChain(_alias);
      
      if (certChain == null)
        throw new ConfigException(L.l("JSSE alias '{0}' does not have a corresponding certificate chain.",
                                  _alias));

      _keyStore = KeyStore.getInstance(_keyStoreType);
      _keyStore.load(null, _password.toCharArray());

      _keyStore.setKeyEntry(_alias, key, _password.toCharArray(), certChain);
    }
  }

  /**
   * Creates the SSL ServerSocket.
   */
  public QServerSocket create(InetAddress host, int port)
    throws IOException, GeneralSecurityException
  {
    SSLServerSocketFactory factory = null;
    
    if (_keyStore != null) {
      SSLContext sslContext = SSLContext.getInstance(_sslContext);

      KeyManagerFactory kmf
        = KeyManagerFactory.getInstance(_keyManagerFactory);
    
      kmf.init(_keyStore, _password.toCharArray());
      
      sslContext.init(kmf.getKeyManagers(), null, null);

      /*
      if (_cipherSuites != null)
        sslContext.createSSLEngine().setEnabledCipherSuites(_cipherSuites);

      if (_protocols != null)
        sslContext.createSSLEngine().setEnabledProtocols(_protocols);
      */

      factory = sslContext.getServerSocketFactory();
    }
    else {
      factory = createAnonymousFactory(host, port);
    }

    ServerSocket serverSocket;

    int listen = 100;

    if (host == null)
      serverSocket = factory.createServerSocket(port, listen);
    else
      serverSocket = factory.createServerSocket(port, listen, host);

    SSLServerSocket sslServerSocket = (SSLServerSocket) serverSocket;

    if (_cipherSuites != null) {
      sslServerSocket.setEnabledCipherSuites(_cipherSuites);
    }

    if (_protocols != null) {
      sslServerSocket.setEnabledProtocols(_protocols);
    }
    
    if ("required".equals(_verifyClient))
      sslServerSocket.setNeedClientAuth(true);

    return new QServerSocketWrapper(serverSocket);
  }

  private SSLServerSocketFactory createAnonymousFactory(InetAddress hostAddr,
                                                        int port)
    throws IOException, GeneralSecurityException
  {
    SSLContext sslContext = SSLContext.getInstance(_sslContext);

    String []cipherSuites = _cipherSuites;

    /*
    if (cipherSuites == null) {
      cipherSuites = sslContext.createSSLEngine().getSupportedCipherSuites();
    }
    */

    String selfSignedName = _selfSignedName;

    if (selfSignedName == null
        || "".equals(selfSignedName)
        || "*".equals(selfSignedName)) {
      if (hostAddr != null)
        selfSignedName = hostAddr.getHostName();
      else {
        InetAddress addr = InetAddress.getLocalHost();

        selfSignedName = addr.getHostAddress();
      }
    }
    
    SelfSignedCert cert = SelfSignedCert.create(selfSignedName,
                                                cipherSuites);

    if (cert == null)
      throw new ConfigException(L.l("Cannot generate anonymous certificate"));
      
    sslContext.init(cert.getKeyManagers(), null, null);

    // SSLEngine engine = sslContext.createSSLEngine();

    SSLServerSocketFactory factory = sslContext.getServerSocketFactory();

    return factory;
  }
  
  /**
   * Creates the SSL ServerSocket.
   */
  public QServerSocket bind(QServerSocket ss)
    throws ConfigException, IOException, GeneralSecurityException
  {
    throw new ConfigException(L.l("jsse is not allowed here"));
  }
}

