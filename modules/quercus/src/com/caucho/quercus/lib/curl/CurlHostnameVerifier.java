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

import java.security.Principal;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

public final class CurlHostnameVerifier
  implements HostnameVerifier
{
  private boolean _isVerifySSLPeer;
  private boolean _isVerifySSLCommonName;
  private boolean _isVerifySSLHostname;
  
  private CurlHostnameVerifier(boolean verifyPeer,
                               boolean commonName,
                               boolean hostname)
  {
    _isVerifySSLPeer = verifyPeer;
    _isVerifySSLCommonName = commonName;
    _isVerifySSLHostname = hostname;
  }
  
  public static CurlHostnameVerifier create()
  {
    return new CurlHostnameVerifier(true, true, true);
  }
  
  public static CurlHostnameVerifier create(boolean verifyPeer,
                                            boolean commonName,
                                            boolean hostname)
  {
    return new CurlHostnameVerifier(verifyPeer, commonName, hostname);
  }
  
  public boolean verify(String hostname, SSLSession session)
  {
    System.out.println("VERIFY: " + hostname);
    if (_isVerifySSLPeer == false
        && _isVerifySSLCommonName == false
        && _isVerifySSLHostname == false) {
      return true;
    }
    
    Principal principal = null;
    
    try {
      principal = session.getPeerPrincipal();
    }
    catch (SSLPeerUnverifiedException e) { 
      if (_isVerifySSLPeer)
        return false;
    }
      
    if (_isVerifySSLPeer) {
      try {
        session.getPeerPrincipal();
      }
      catch (SSLPeerUnverifiedException e) {
        //XXX: log
        return false;
      }
    }
    
    if (_isVerifySSLCommonName) {
      if (principal == null || ! principal.getName().equals(hostname))
        return false;
    }
    
    if (_isVerifySSLHostname) {
      if (session.getPeerHost() == null
          || ! session.getPeerHost().equals(hostname))
        return false;
    }
    
    return true;
  }
}