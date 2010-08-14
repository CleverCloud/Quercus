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
import com.caucho.util.Base64;
import com.caucho.util.CharBuffer;
import com.caucho.util.RandomUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Authentication
{
  /**
   * Returns an authorization response string.
   * Supports digest and basic only.
   */
  public static String getAuthorization(String user,
                              String pass,
                              String requestMethod,
                              String uri,
                              String header)       
  {
    if (header.startsWith("Digest"))
      return digest(user, pass, requestMethod, uri, header);
    else
      return basic(user, pass);
  }

  /**
   * Returns a basic encoded response string.
   */
  public static String basic(String user, String pass)
  {
    StringBuilder sb = new StringBuilder();

    sb.append(user);
    sb.append(':');
    sb.append(pass);

    return basic(sb.toString());
  }

  public static String basic(String usernamePassword)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("Basic ");
    sb.append(Base64.encode(usernamePassword));

    return sb.toString();
  }

  /**
   * Returns a digest encoded response string.
   */
  public static String digest(String user,
                              String pass,
                              String requestMethod,
                              String uri,
                              String header)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("Digest ");
    sb.append("username=\"");
    sb.append(user);

    Scanner scanner = new Scanner(header);

    String realm = "";
    String nonce = "";
    String qop = "";
    String opaque = null;
    String algorithm = null;

    String key;
    while ((key = scanner.readKey()) != null) {
      String value = scanner.readValue();

      if (key.equals("realm"))
        realm = value;
      else if (key.equals("nonce"))
        nonce = value;
      else if (key.equals("qop"))
        qop = value;
      else if (key.equals("opaque"))
        opaque = value;
      else if (key.equals("algorithm"))
        algorithm = value;
    }

    scanner.close();

    sb.append("\", realm=\"");
    sb.append(realm);

    sb.append("\", nonce=\"");
    sb.append(nonce);

    sb.append("\", uri=\"");
    sb.append(uri);

    sb.append("\", qop=\"");
    sb.append("auth");

    String cnonce = Base64.encode(String.valueOf(RandomUtil.getRandomLong()));
    sb.append("\", cnonce=\"");
    sb.append(cnonce);

    String nc = "00000001";
    sb.append("\", nc=\"");
    sb.append(nc);

    if (opaque != null) {
      sb.append("\", opaque=\"");
      sb.append(opaque);
    }

    if (algorithm != null) {
      sb.append("\", algorithm=\"");
      sb.append(algorithm);
    }
    else
      algorithm = "MD5";

    sb.append("\", response=\"");
    appendResponse(sb,
                   user,
                   realm,
                   pass,
                   requestMethod,
                   uri,
                   nonce,
                   nc,
                   cnonce,
                   qop,
                   algorithm);

    sb.append('"');
    return sb.toString();
  }

  /**
   * Appends the authorization string to the StringBuilder.
   */
  private static void appendResponse(StringBuilder sb,
                              String user,
                              String realm,
                              String pass,
                              String requestMethod,
                              String uri,
                              String nonce,
                              String nc,
                              String cnonce,
                              String qop,
                              String algorithm)
  {
    MessageDigest resultDigest = null;
    MessageDigest scratchDigest = null;

    try {
      resultDigest = MessageDigest.getInstance(algorithm);
      scratchDigest = MessageDigest.getInstance(algorithm);
    }
    catch (NoSuchAlgorithmException e) {
      throw new QuercusModuleException(e);
    }

    {
      md5(scratchDigest, user);
      scratchDigest.update((byte)':');

      md5(scratchDigest, realm);
      scratchDigest.update((byte)':');

      md5(scratchDigest, pass);

      update(resultDigest, scratchDigest.digest());
      resultDigest.update((byte)':');
    }

    md5(resultDigest, nonce);
    resultDigest.update((byte)':');

    md5(resultDigest, nc);
    resultDigest.update((byte)':');

    md5(resultDigest, cnonce);
    resultDigest.update((byte)':');  

    md5(resultDigest, qop);
    resultDigest.update((byte)':');

    {
      scratchDigest.reset();

      md5(scratchDigest, requestMethod);
      scratchDigest.update((byte)':');

      md5(scratchDigest, uri);

      update(resultDigest, scratchDigest.digest());
    }

    appendHex(sb, resultDigest.digest());
  }

  /**
   * Updates MD5 hash.
   */
  private static void md5(MessageDigest md,
                              String string)
  {
    int length = string.length();
    for (int i = 0; i < length; i++) {
      md.update((byte)string.charAt(i));
    }
  }

  /**
   * Updates MD5 hash result.
   */
  private static void update(MessageDigest resultDigest,
                              byte[] digest)
  {
    for (int i = 0; i < digest.length; i++) {
      int d1 = (digest[i] >> 4) & 0xf;
      int d2 = (digest[i] & 0xf);
        
      resultDigest.update((byte)toHexChar(d1));
      resultDigest.update((byte)toHexChar(d2));
    }
  }

  /**
   * Appends hex characters to StringBuilder.
   */
  private static void appendHex(StringBuilder sb,
                              byte[] digest)
  {
    for (int i = 0; i < digest.length; i++) {
      int d1 = (digest[i] >> 4) & 0xf;
      int d2 = (digest[i] & 0xf);
        
      sb.append(toHexChar(d1));
      sb.append(toHexChar(d2));
    }
  }

  private static char toHexChar(int d)
  {
    d &= 0xf;
    
    if (d < 10)
      return (char) (d + '0');
    else
      return (char) (d - 10 + 'a');
  }

}

  /**
   * Represents a HTTP header field values scanner.
   */
  class Scanner {
    String _header;
    int _position;
    int _length;

    CharBuffer _cb;

    Scanner(String header)
    {
      _header = header;
      _position = header.indexOf("Digest") + "Digest".length();
      _length = header.length();

      _cb = CharBuffer.allocate();
    }

    String readKey()
    {
      int ch = skipWhitespace();

      if (ch < 0)
        return null;

      if (ch == ',')
        ch = skipWhitespace();

      do {
        _cb.append((char)ch);
      } while ((ch = read()) != '=');

      // discard quote
      read();

      String key = _cb.toString();
      _cb.clear();

      return key;
    }

    String readValue()
    {
      int ch;
      while ((ch = read()) != '"') {
        _cb.append((char)ch);
      }

      String value = _cb.toString();
      _cb.clear();

      return value;
    }

    int skipWhitespace()
    {
      int ch;

      while ((ch = read()) >= 0) {
        if (ch != ' '
            && ch != '\t'
            && ch != '\r'
            && ch != '\n'
            && ch != '\f')
          break;
      }

      return ch;
    }

    int read()
    {
      if (_position >= _length)
        return -1;
      else
        return _header.charAt(_position++);
    }

    void close()
    {
      _cb.free();
    }
  }
