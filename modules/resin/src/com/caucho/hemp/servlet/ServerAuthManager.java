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

package com.caucho.hemp.servlet;

import java.io.ByteArrayInputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.PublicKey;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import com.caucho.bam.NotAuthorizedException;
import com.caucho.cloud.security.SecurityService;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hmtp.GetPublicKeyQuery;
import com.caucho.hmtp.NonceQuery;
import com.caucho.hmtp.SignedCredentials;
import com.caucho.security.Authenticator;
import com.caucho.security.BasicPrincipal;
import com.caucho.security.DigestCredentials;
import com.caucho.security.PasswordCredentials;
import com.caucho.server.cluster.Server;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

/**
 * Manages links on the server
 */

public class ServerAuthManager {
  private static final Logger log
    = Logger.getLogger(ServerAuthManager.class.getName());
  private static final L10N L = new L10N(ServerAuthManager.class);
  
  private SecurityService _security;

  private KeyPair _authKeyPair; // authentication key pair
  private boolean _isAuthenticationRequired = true;
  
  private LruCache<String,String> _nonceMap
    = new LruCache<String,String>(4096);
  
  public ServerAuthManager()
  {
    _security = SecurityService.create();
  }
  
  public void setAuthenticationRequired(boolean isAuthenticationRequired)
  {
    _isAuthenticationRequired = isAuthenticationRequired;
  }
  
  public Authenticator getAuth()
  {
    return _security.getAuthenticator();
  }

  //
  // authentication
  //

  public Key decryptKey(String keyAlgorithm, byte []encKey)
  {
    try {
      Cipher cipher = Cipher.getInstance("RSA");

      cipher.init(Cipher.UNWRAP_MODE, _authKeyPair.getPrivate());

      Key key = cipher.unwrap(encKey, keyAlgorithm, Cipher.SECRET_KEY);

      return key;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  void authenticate(String to, Object credentials, String ipAddress)
  {
    Authenticator auth = getAuth();
    
    if (credentials instanceof SignedCredentials) {
      SignedCredentials signedCred = (SignedCredentials) credentials;

      String uid= signedCred.getUid();
      String nonce = signedCred.getNonce();
      String signature = signedCred.getSignature();
      
      /*
      String savedNonce = _nonceMap.get(uid);
      
      if (savedNonce == null)
        throw new NotAuthorizedException(L.l("'{0}' has invalid credentials",
                                             uid));
                                             */
      
      String serverSignature;
      
      if (uid != null && ! uid.equals("")) {
        // XXXX:
        serverSignature = _security.signSystem(uid, nonce);
      }
      else if (_security.isSystemAuthKey() || ! _isAuthenticationRequired)
        serverSignature = _security.signSystem(uid, nonce);
      else {
        log.info("Authentication failed because no resin-system-auth-key");
        
        throw new NotAuthorizedException(L.l("'{0}' has invalid credentials",
                                             uid));
      }
      
      if (! serverSignature.equals(signature)) {
        throw new NotAuthorizedException(L.l("'{0}' has invalid credentials",
                                             uid));
      }
    }
    else if (auth == null && ! _isAuthenticationRequired) {
    }
    else if (auth == null) {
      log.finer("Authentication failed because no authenticator configured");
      
      throw new NotAuthorizedException(L.l("'{0}' has invalid credentials",
                                           credentials));
    }
    else if (credentials instanceof DigestCredentials) {
      DigestCredentials digestCred = (DigestCredentials) credentials;

      Principal user = new BasicPrincipal(digestCred.getUserName());
      
      user = auth.authenticate(user, digestCred, null);

      if (user == null) {
        throw new NotAuthorizedException(L.l("'{0}' has invalid credentials",
                                             digestCred.getUserName()));
      }
    }
    else if (credentials instanceof String) {
      String password = (String) credentials;
    
      Principal user = new BasicPrincipal(to);
      PasswordCredentials pwdCred = new PasswordCredentials(password);
    
      if (auth.authenticate(user, pwdCred, null) == null) {
        throw new NotAuthorizedException(L.l("'{0}' has invalid credentials",
                                             to));
      }
    }
    /*
    else if (server.getAdminCookie() == null && credentials == null) {
      if (! "127.0.0.1".equals(ipAddress)) {
        throw new NotAuthorizedException(L.l("'{0}' is an invalid local address for '{1}', because no password credentials are available",
                                             ipAddress, uid));
      }
    }
    */
    else {
      throw new NotAuthorizedException(L.l("'{0}' is an unknown credential",
                                           credentials));
    }
  }
  
  NonceQuery generateNonce(NonceQuery query)
  {
    String uid = query.getUid();
    String clientNonce = query.getNonce();

    String clientSignature = _security.signSystem(uid, clientNonce);
    
    String nonce = String.valueOf(Alarm.getCurrentTime());
    
    return new NonceQuery(uid, nonce, clientSignature);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAuth() + "]";
  }  
}
