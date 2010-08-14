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

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.caucho.config.ConfigException;
import com.caucho.util.Base64;
import com.caucho.util.L10N;

/**
 * Calculates a digest for the user and password.
 *
 * <p>If the realm is missing, the digest will calculate:
 * <code><pre>
 * MD5(user + ':' + password)
 * </pre></code>
 *
 * <p>If the realm is specified, the digest will calculate:
 * <code><pre>
 * MD5(user + ':' + realm + ':' + password)
 * </pre></code>
 *
 * <p>The second version matches the way HTTP digest authentication
 * is handled, so it is the preferred method for storing passwords.
 *
 * <p>The returned result is the base64 encoding of the digest.
 */
public class PasswordDigest {
  private static final L10N L = new L10N(PasswordDigest.class);
  
  private String _algorithm = "MD5";
  private String _format = "base64";
  private String _realm = null;
  private MessageDigest _digest;
  private byte[] _digestBytes = new byte[256];
  
  /**
   * Returns the message digest algorithm.
   */
  public void setAlgorithm(String algorithm)
  {
    _algorithm = algorithm;
  }
  
  /**
   * Returns the message digest algorithm.
   */
  public String getAlgorithm()
  {
    return _algorithm;
  }
  
  /**
   * Set the message digest format (base64 or hex).
   */
  public void setFormat(String format)
  {
    _format = format;
  }
  
  /**
   * Returns the message digest format (base64 or hex).
   */
  public String getFormat()
  {
    return _format;
  }
  
  /**
   * Set the message digest default realm
   */
  public void setRealm(String realm)
  {
    _realm = realm;
  }
  
  /**
   * Returns the message digest default realm.
   */
  public String getRealm()
  {
    return _realm;
  }

  /**
   * Sets the algorithm for bean-style init.
   */
  public void addText(String value)
    throws ConfigException
  {
    int p = value.indexOf('-');

    if (p > 0) {
      String algorithm = value.substring(0, p);
      String format = value.substring(p + 1);

      setAlgorithm(algorithm);
      setFormat(format);
    }
    else if (value.equals("none")) {
      setAlgorithm(null);
      setFormat(null);
    }
    else
      throw new ConfigException(L.l("{0} is an illegal algorithm.  Expected 'none' or 'algorithm-format', for example 'MD5-base64'.", value));
  }

  /**
   * Initialize the digest.
   */
  @PostConstruct
  public void init()
  {
    if (_algorithm == null)
      return;
    
    try {
      _digest = MessageDigest.getInstance(_algorithm);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the digest of the password
   */
  public String getPasswordDigest(String password)
    throws ServletException
  {
    return getPasswordDigest(null, password, _realm, null);
  }

  /**
   * Returns the digest of the user/password
   */
  public String getPasswordDigest(String user, String password)
  {
    return getPasswordDigest(user, password, _realm, null);
  }

  /**
   * Returns the digest of the user/password
   */
  public String getPasswordDigest(String user, String password, String realm)
  {
    return getPasswordDigest(user, password, realm, null);
  }
  
  /**
   * Returns the digest of the user/password
   *
   * <p>The default implementation returns the digest of
   * <b>user:password</b> or <b>user:realm:password</b> if a
   * default realm has been configured.
   *
   * @param request the http request
   * @param response the http response
   * @param app the servlet context
   * @param user the user name
   * @param password the cleartext password
   */
  public String getPasswordDigest(String user,
                                  String password,
                                  HttpServletRequest request)
  {
    return getPasswordDigest(user, password, _realm, request);
  }
  
  /**
   * Returns the digest of the user/password
   *
   * <p>The default implementation returns the digest of
   * <b>user:realm:password</b>.  If the realm is null, it will use
   * <b>user:password</b>.
   *
   * @param request the http request
   * @param user the user name
   * @param password the cleartext password
   * @param realm the security realm
   */
  public String getPasswordDigest(String user,
                                  String password,
                                  String realm,
                                  HttpServletRequest request)
  {
    if (_digest == null) {
      init();

      if (_digest == null)
        return null;
    }
    
    try {
      synchronized (_digest) {
        _digest.reset();

        updateDigest(_digest, user, password.toCharArray(), realm);

        int len = _digest.digest(_digestBytes, 0, _digestBytes.length);

        return digestToString(_digestBytes, len);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Returns the digest of the user/password
   *
   * <p>The default implementation returns the digest of
   * <b>user:realm:password</b>.  If the realm is null, it will use
   * <b>user:password</b>.
   *
   * @param user the user name
   * @param password the cleartext password
   * @param realm the security realm
   */
  public char []getPasswordDigest(String user,
                                  char []password)
  {
    return getPasswordDigest(user, password, _realm);
  }
  
  /**
   * Returns the digest of the user/password
   *
   * <p>The default implementation returns the digest of
   * <b>user:realm:password</b>.  If the realm is null, it will use
   * <b>user:password</b>.
   *
   * @param user the user name
   * @param password the cleartext password
   * @param realm the security realm
   */
  public char []getPasswordDigest(String user,
                                  char []password,
                                  String realm)
  {
    if (_digest == null) {
      init();

      if (_digest == null)
        return null;
    }

    try {
      synchronized (_digest) {
        _digest.reset();

        updateDigest(_digest, user, password, realm);

        int len = _digest.digest(_digestBytes, 0, _digestBytes.length);

        return digestToCharArray(_digestBytes, len);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Updates the digest based on the user:realm:password
   */
  protected void updateDigest(MessageDigest digest,
                              String user, char []password, String realm)
  {
    if (user != null) {
      addDigestUTF8(digest, user);
      digest.update((byte) ':');
    }

    if (realm != null && ! realm.equals("none")) {
      addDigestUTF8(digest, realm);
      digest.update((byte) ':');
    }

    addDigestUTF8(digest, password);
  }

  /**
   * Adds the string to the digest using a UTF8 encoding.
   */
  protected static void addDigestUTF8(MessageDigest digest, String string)
  {
    if (string == null)
      return;
    
    int len = string.length();
    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);
      
      addDigestUTF8(digest, ch);
    }
  }

  /**
   * Adds the string to the digest using a UTF8 encoding.
   */
  protected static void addDigestUTF8(MessageDigest digest, char []string)
  {
    if (string == null)
      return;
    
    int len = string.length;
    for (int i = 0; i < len; i++) {
      char ch = string[i];

      addDigestUTF8(digest, ch);
    }
  }

  /**
   * Adds the string to the digest using a UTF8 encoding.
   */
  protected static void addDigestUTF8(MessageDigest digest, char ch)
  {
    if (ch < 0x80)
      digest.update((byte) ch);
    else if (ch < 0x800) {
      digest.update((byte) (0xc0 + (ch >> 6)));
      digest.update((byte) (0x80 + (ch & 0x3f)));
    }
    else {
      digest.update((byte) (0xe0 + (ch >> 12)));
      digest.update((byte) (0x80 + ((ch >> 6) & 0x3f)));
      digest.update((byte) (0x80 + (ch & 0x3f)));
    }
  }

  /**
   * Convert the string to a digest byte array.
   */
  public byte[]stringToDigest(String s)
  {
    return Base64.decodeToByteArray(s);
  }

  /**
   * Convert the string to a digest byte array.
   */
  public byte[]stringToDigest(char []s)
  {
    return Base64.decodeToByteArray(new String(s));
  }
  
  /**
   * Convert the digest byte array to a string.
   */
  protected String digestToString(byte []digest, int len)
  {
    return new String(digestToCharArray(digest, len));
  }
  
  /**
   * Convert the digest byte array to a string.
   */
  protected char []digestToCharArray(byte []digest, int len)
  {
    if (! _format.equals("base64"))
      return digestToHex(digest, len);
    else
      return digestToBase64(digest, len);
  }
  
  protected static char []digestToBase64(byte []digest, int len)
  {
    StringBuilder sb = new StringBuilder();

    Base64.encode(sb, digest, 0, len);

    char []string = new char[sb.length()];
    sb.getChars(0, string.length, string, 0);
    sb.setLength(0);
    
    return string;
  }
  
  protected static char []digestToHex(byte []digest, int len)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++) {
      int d1 = (digest[i] >> 4) & 0xf;
      int d2 = digest[i] & 0xf;

      if (d1 >= 10)
        sb.append((char) (d1 + 'a' - 10));
      else
        sb.append((char) (d1 + '0'));
      
      if (d2 >= 10)
        sb.append((char) (d2 + 'a' - 10));
      else
        sb.append((char) (d2 + '0'));
    }

    char []string = new char[sb.length()];
    sb.getChars(0, string.length, string, 0);
    sb.setLength(0);

    return string;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _algorithm + "," + _format + "]";
  }
}
