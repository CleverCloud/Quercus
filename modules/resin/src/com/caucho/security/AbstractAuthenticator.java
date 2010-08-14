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

package com.caucho.security;

import java.security.MessageDigest;
import java.security.Principal;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;

import com.caucho.config.inject.HandleAware;
import com.caucho.server.security.PasswordDigest;
import com.caucho.util.Base64;
import com.caucho.util.L10N;

/**
 * All applications should extend AbstractAuthenticator to implement
 * their custom authenticators.  While this isn't absolutely required,
 * it protects implementations from API changes.
 *
 * <p>The AbstractAuthenticator provides a single-signon cache.  Users
 * logged into one web-app will share the same principal.
 */
@SuppressWarnings("serial")
public class AbstractAuthenticator
  implements Authenticator, HandleAware, java.io.Serializable
{
  private static final Logger log
    = Logger.getLogger(AbstractAuthenticator.class.getName());
  static final L10N L = new L10N(AbstractAuthenticator.class);
  
  protected String _passwordDigestAlgorithm = "MD5-base64";
  protected String _passwordDigestRealm = "resin";
  protected PasswordDigest _passwordDigest;

  private boolean _logoutOnTimeout = true;

  private Object _serializationHandle;

  private SingleSignon _singleSignon;

  /**
   * Returns the password digest
   */
  public PasswordDigest getPasswordDigest()
  {
    return _passwordDigest;
  }

  /**
   * Sets the password digest.  The password digest of the form:
   * "algorithm-format", e.g. "MD5-base64".
   */
  public void setPasswordDigest(PasswordDigest digest)
  {
    _passwordDigest = digest;
  }

  /**
   * Returns the password digest algorithm
   */
  public String getPasswordDigestAlgorithm()
  {
    return _passwordDigestAlgorithm;
  }

  /**
   * Sets the password digest algorithm.  The password digest of the form:
   * "algorithm-format", e.g. "MD5-base64".
   */
  public void setPasswordDigestAlgorithm(String digest)
  {
    _passwordDigestAlgorithm = digest;
  }

  /**
   * Returns the password digest realm
   */
  public String getPasswordDigestRealm()
  {
    return _passwordDigestRealm;
  }

  /**
   * Sets the password digest realm.
   */
  public void setPasswordDigestRealm(String realm)
  {
    _passwordDigestRealm = realm;
  }

  /**
   * Returns true if the user should be logged out on a session timeout.
   */
  public boolean getLogoutOnSessionTimeout()
  {
    return _logoutOnTimeout;
  }

  /**
   * Sets true if the principal should logout when the session times out.
   */
  public void setLogoutOnSessionTimeout(boolean logout)
  {
    _logoutOnTimeout = logout;
  }

  /**
   * Adds a role mapping.
   */
  public void addRoleMapping(Principal principal, String role)
  {
  }

  /**
   * Initialize the authenticator with the application.
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    if (_passwordDigest != null) {
      if (_passwordDigest.getAlgorithm() == null
          || _passwordDigest.getAlgorithm().equals("none")) {
        _passwordDigest = null;
        _passwordDigestAlgorithm = "none";
      }
    }
    else if (_passwordDigestAlgorithm == null
             || _passwordDigestAlgorithm.equals("none")) {
    }
    else {
      int p = _passwordDigestAlgorithm.indexOf('-');

      if (p > 0) {
        String algorithm = _passwordDigestAlgorithm.substring(0, p);
        String format = _passwordDigestAlgorithm.substring(p + 1);

        _passwordDigest = new PasswordDigest();
        _passwordDigest.setAlgorithm(algorithm);
        _passwordDigest.setFormat(format);
        _passwordDigest.setRealm(_passwordDigestRealm);

        _passwordDigest.init();
      }
    }

    /*
    if (Server.getCurrent() != null) {
      _singleSignon = _localSingleSignon.get();
      
      // server/1al4 vs server/1ak1
      if (_singleSignon == null) {
        MemorySingleSignon memorySignon = new MemorySingleSignon();
        memorySignon.init();
        _singleSignon = memorySignon;
        _localSingleSignon.set(_singleSignon);
      }
    }
    */
  }

  //
  // Authenticator API
  //

  /**
   * Authenticator main call to login a user.
   *
   * @param user the Login's user, generally a BasicPrincipal just containing
   * the name, but may contain an X.509 certificate
   * @param credentials the login credentials
   * @param details extra information, e.g. HttpServletRequest
   */
  @Override
  public Principal authenticate(Principal user,
                                Credentials credentials,
                                Object details)
  {
    if (credentials instanceof PasswordCredentials) {
      return authenticate(user, (PasswordCredentials) credentials, details);
    }
    else if (credentials instanceof HttpDigestCredentials) {
      return authenticate(user, (HttpDigestCredentials) credentials, details);
    }
    else if (credentials instanceof DigestCredentials) {
      return authenticate(user, (DigestCredentials) credentials, details);
    }
    else
      return null;
  }

  /**
   * Returns true if the user plays the named role.
   *
   * @param user the user to test
   * @param role the role to test
   */
  @Override
  public boolean isUserInRole(Principal user, String role)
  {
    PasswordUser passwordUser = getPasswordUser(user);

    if (passwordUser != null)
      return passwordUser.isUserInRole(role);
    else
      return false;
  }

  /**
   * Logs the user out from the session.
   *
   * @param user the logged in user
   */
  @Override
  public void logout(Principal user)
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " logout " + user);
  }

  //
  // basic password authentication
  //

  /**
   * Main authenticator API.
   */
  protected Principal authenticate(Principal principal,
                                   PasswordCredentials cred,
                                   Object details)
  {
    PasswordUser user = getPasswordUser(principal);

    if (user == null || user.isDisabled())
      return null;
    
    char []password = cred.getPassword();
    char []digest = getPasswordDigest(principal.getName(), password);
    
    if (digest == null)
      return null;

    if (! isMatch(digest, user.getPassword()) && ! user.isAnonymous()) {
      user = null;
    }
    
    Arrays.fill(digest, 'a');

    if (user != null)
      return user.getPrincipal();
    else
      return null;
  }

  //
  // http digest authentication
  //
  
  /**
   * Validates the user when HTTP Digest authentication.
   * The HTTP Digest authentication uses the following algorithm
   * to calculate the digest.  The digest is then compared to
   * the client digest.
   *
   * <code><pre>
   * A1 = MD5(username + ':' + realm + ':' + password)
   * A2 = MD5(method + ':' + uri)
   * digest = MD5(A1 + ':' + nonce + A2)
   * </pre></code>
   *
   * @param principal the user trying to authenticate.
   * @param cred the digest credentials
   *
   * @return the logged in principal if successful
   */
  protected Principal authenticate(Principal principal,
                                   HttpDigestCredentials cred,
                                   Object details)
  {
    String cnonce = cred.getCnonce();
    String method = cred.getMethod();
    String nc = cred.getNc();
    String nonce = cred.getNonce();
    String qop = cred.getQop();
    String realm = cred.getRealm();
    byte []clientDigest = cred.getResponse();
    String uri = cred.getUri();

    try {
      if (clientDigest == null)
        return null;
      
      MessageDigest digest = MessageDigest.getInstance("MD5");
      
      byte []a1 = getDigestSecret(principal, realm);

      if (a1 == null)
        return null;

      digestUpdateHex(digest, a1);
      
      digest.update((byte) ':');
      for (int i = 0; i < nonce.length(); i++)
        digest.update((byte) nonce.charAt(i));

      if (qop != null) {
        digest.update((byte) ':');
        for (int i = 0; i < nc.length(); i++)
          digest.update((byte) nc.charAt(i));

        digest.update((byte) ':');

        for (int i = 0; cnonce != null && i < cnonce.length(); i++)
          digest.update((byte) cnonce.charAt(i));
        
        digest.update((byte) ':');
        for (int i = 0; qop != null && i < qop.length(); i++)
          digest.update((byte) qop.charAt(i));
      }
      digest.update((byte) ':');

      byte []a2 = digest(method + ":" + uri);

      digestUpdateHex(digest, a2);

      byte []serverDigest = digest.digest();

      if (isMatch(clientDigest, serverDigest))
        return principal;
      else
        return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  //
  // http digest authentication
  //
  
  /**
   * Validates the user when Resin's Digest authentication.
   * The digest authentication uses the following algorithm
   * to calculate the digest.  The digest is then compared to
   * the client digest.
   *
   * <code><pre>
   * A1 = MD5(username + ':' + realm + ':' + password)
   * digest = MD5(A1 + ':' + nonce)
   * </pre></code>
   *
   * @param principal the user trying to authenticate.
   * @param cred the digest credentials
   *
   * @return the logged in principal if successful
   */
  protected Principal authenticate(Principal principal,
                                   DigestCredentials cred,
                                   Object details)
  {
    String nonce = cred.getNonce();
    String realm = cred.getRealm();
    byte []clientDigest = cred.getDigest();

    try {
      if (clientDigest == null)
        return null;
      
      byte []a1 = getDigestSecret(principal, realm);

      if (a1 == null)
        return null;

      MessageDigest md = MessageDigest.getInstance("MD5");
      
      digestUpdateHex(md, a1);
      
      md.update((byte) ':');
      for (int i = 0; i < nonce.length(); i++) {
        md.update((byte) nonce.charAt(i));
      }

      byte []serverDigest = md.digest();

      if (isMatch(clientDigest, serverDigest))
        return principal;
      else
        return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the digest view of the password.  The default
   * uses the PasswordDigest class if available, and returns the
   * plaintext password if not.
   */
  protected char []getPasswordDigest(String user, char []password)
  {
    if (_passwordDigest != null) {
      char []digest = _passwordDigest.getPasswordDigest(user, password);

      if (digest != null)
        return digest;
    }

    char []digest = new char[password.length];
    System.arraycopy(password, 0, digest, 0, password.length);
      
    return digest;
  }

  /**
   * Returns the digest secret for Digest authentication.
   */
  protected byte []getDigestSecret(Principal principal, String realm)
  {
    PasswordUser user = getPasswordUser(principal);

    if (user == null || user.isDisabled())
      return null;

    if (_passwordDigest != null)
      return _passwordDigest.stringToDigest(user.getPassword());

    String username = principal.getName();

    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");

      String string = username + ":" + realm + ":";
      byte []data = string.getBytes("UTF8");
      digest.update(data);
      char []password = user.getPassword();

      for (int i = 0; i < password.length; i++)
        digest.update((byte) password[i]);
      
      return digest.digest();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  //
  // abstract methods
  //

  /**
   * Abstract method to return a user based on the name
   *
   * @param userName the string user name
   *
   * @return the populated PasswordUser value
   */
  protected PasswordUser getPasswordUser(String userName)
  {
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " getPasswordUser() is not implemented for "
               + userName);
    }
    
    return null;
  }

  /**
   * Returns the user based on a principal
   */
  protected PasswordUser getPasswordUser(Principal principal)
  {
    return getPasswordUser(principal.getName());
  }

  //
  // Compatibility
  //

  /**
   * Returns the scoped single-signon
   */
  public SingleSignon getSingleSignon()
  {
    if (_singleSignon == null) {
      _singleSignon = AbstractSingleSignon.getCurrent();
    
      if (_singleSignon == null)
        _singleSignon = new NullSingleSignon();
    }
    
    return _singleSignon;
  }

  //
  // utilities
  //

  private void digestUpdateHex(MessageDigest digest, byte []bytes)
  {
    for (int i = 0; i < bytes.length; i++) {
      int b = bytes[i];
      int d1 = (b >> 4) & 0xf;
      int d2 = b & 0xf;

      if (d1 < 10)
        digest.update((byte) (d1 + '0'));
      else
        digest.update((byte) (d1 + 'a' - 10));

      if (d2 < 10)
        digest.update((byte) (d2 + '0'));
      else
        digest.update((byte) (d2 + 'a' - 10));
    }
  }

  protected byte []stringToDigest(String digest)
  {
    if (digest == null)
      return null;
    
    int len = (digest.length() + 1) / 2;
    byte []clientDigest = new byte[len];

    for (int i = 0; i + 1 < digest.length(); i += 2) {
      int ch1 = digest.charAt(i);
      int ch2 = digest.charAt(i + 1);

      int b = 0;
      if (ch1 >= '0' && ch1 <= '9')
        b += ch1 - '0';
      else if (ch1 >= 'a' && ch1 <= 'f')
        b += ch1 - 'a' + 10;

      b *= 16;
      
      if (ch2 >= '0' && ch2 <= '9')
        b += ch2 - '0';
      else if (ch2 >= 'a' && ch2 <= 'f')
        b += ch2 - 'a' + 10;

      clientDigest[i / 2] = (byte) b;
    }

    return clientDigest;
  }

  protected byte []digest(String value)
    throws ServletException
  {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");

      byte []data = value.getBytes("UTF8");
      return digest.digest(data);
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  /**
   * Tests passwords
   */
  private boolean isMatch(char []password, char []userPassword)
  {
    int len = password.length;

    if (len != userPassword.length)
      return false;

    for (int i = 0; i < len; i++) {
      if (password[i] != userPassword[i])
        return false;
    }

    return true;
  }

  /**
   * Tests passwords
   */
  private boolean isMatch(byte []password, byte []userPassword)
  {
    int len = password.length;

    if (len != userPassword.length)
      return false;

    for (int i = 0; i < len; i++) {
      if (password[i] != userPassword[i])
        return false;
    }

    return true;
  }
  
  /**
   * Sets the serialization handle
   */
  public void setSerializationHandle(Object handle)
  {
    _serializationHandle = handle;
  }

  /**
   * Serialize to the handle
   */
  public Object writeReplace()
  {
    return _serializationHandle;
  }

  @Override
  public String toString()
  {
    if (_passwordDigest != null) {
      return (getClass().getSimpleName()
              + "[" + _passwordDigest.getAlgorithm()
              + "," + _passwordDigest.getRealm() + "]");
    }
    else {
      return (getClass().getSimpleName()
              + "[" + _passwordDigestAlgorithm
              + "," + _passwordDigestRealm + "]");
    }
  }
}
