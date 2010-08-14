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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.curl;

import com.caucho.quercus.QuercusModuleException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

/**
 * Represents a HttpURLConnection wrapper.
 */
public class HttpsConnection
  extends HttpConnection
{
  protected HttpsConnection(URL url,
                            String username,
                            String password)
    throws IOException
  {
    super(url, username, password);
  }

  public HttpsConnection(URL url,
                         String username,
                         String password,
                         URL proxyURL,
                         String proxyUsername,
                         String proxyPassword,
                         String proxyType)
    throws IOException
  {
    super(url, username, password,
          proxyURL, proxyUsername, proxyPassword, proxyType);
  }
  
  @Override
  protected void init(CurlResource curl)
    throws IOException
  {
    Proxy proxy = getProxy();

    HttpsURLConnection conn;

    if (proxy != null)
      conn = (HttpsURLConnection) getURL().openConnection(proxy);
    else
      conn = (HttpsURLConnection) getURL().openConnection();

    HostnameVerifier hostnameVerifier
      = CurlHostnameVerifier.create(curl.getIsVerifySSLPeer(),
                                    curl.getIsVerifySSLCommonName(),
                                    curl.getIsVerifySSLHostname());
    
    conn.setHostnameVerifier(hostnameVerifier);

    setConnection(conn);
  }
  
  /**
   * Connects to the server.
   */
  /*
  @Override
  public void connect(CurlResource curl)
    throws ConnectException, ProtocolException, SocketTimeoutException,
            IOException
  {
    try {
      super.connect(curl);
      
      ((HttpsURLConnection)getConnection()).getServerCertificates();
    }
    catch (SSLPeerUnverifiedException e) {
      if (curl.getIsVerifySSLPeer())
        throw e;
    }
  }
  */
}
