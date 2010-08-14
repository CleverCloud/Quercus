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

import java.io.IOException;
import java.security.Principal;
import java.text.CharacterIterator;
import java.util.logging.Level;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.util.Base64;
import com.caucho.util.CharBuffer;
import com.caucho.util.CharCursor;
import com.caucho.util.RandomUtil;
import com.caucho.util.StringCharCursor;
import com.caucho.xml.XmlChar;

/**
 * Implements the "digest" auth-method.  Basic uses the
 * HTTP authentication with WWW-Authenticate and SC_UNAUTHORIZE.
 *
 * The HTTP Digest authentication uses the following algorithm
 * to calculate the digest.  The digest is then compared to
 * the client digest.
 *
 * <code><pre>
 * A1 = MD5(username + ':' + realm + ':' + password)
 * A2 = MD5(method + ':' + uri)
 * digest = MD5(A1 + ':' + nonce + A2)
 * </pre></code>
 */

@ApplicationScoped
public class DigestLogin extends AbstractLogin {
  protected String _realm;

  public DigestLogin()
  {
  }
  
  /**
   * Sets the login realm.
   */
  public void setRealmName(String realm)
  {
    _realm = realm;
  }

  /**
   * Gets the realm.
   */
  public String getRealmName()
  {
    return _realm;
  }

  /**
   * Returns the authentication type.
   */
  public String getAuthType()
  {
    return "Digest";
  }

  /**
   * Returns the principal from a digest authentication
   *
   * @param auth the authenticator for this application.
   */
  @Override
  protected Principal getUserPrincipalImpl(HttpServletRequest request)
  {
    String value = request.getHeader("authorization");

    if (value == null)
      return null;

    String username = null;
    String realm = null;
    String uri = null;
    String nonce = null;
    String cnonce = null;
    String nc = null;
    String qop = null;
    String digest = null;

    CharCursor cursor = new StringCharCursor(value);

    String key = scanKey(cursor);

    if (! "Digest".equalsIgnoreCase(key))
      return null;
      
    while ((key = scanKey(cursor)) != null) {
      value = scanValue(cursor);

      if (key.equals("username"))
        username = value;
      else if (key.equals("realm"))
        realm = value;
      else if (key.equals("uri"))
        uri = value;
      else if (key.equals("nonce"))
        nonce = value;
      else if (key.equals("response"))
        digest = value;
      else if (key.equals("cnonce"))
        cnonce = value;
      else if (key.equals("nc"))
        nc = value;
      else if (key.equals("qop"))
        qop = value;
    }

    byte []clientDigest = decodeDigest(digest);

    if (clientDigest == null || username == null
        || uri == null || nonce == null)
      return null;

    Authenticator auth = getAuthenticator();
    Principal principal = new BasicPrincipal(username);

    HttpDigestCredentials cred = new HttpDigestCredentials();

    cred.setCnonce(cnonce);
    cred.setMethod(request.getMethod());
    cred.setNc(nc);
    cred.setNonce(nonce);
    cred.setQop(qop);
    cred.setRealm(realm);
    cred.setResponse(clientDigest);
    cred.setUri(uri);

    Principal user;

    user = auth.authenticate(principal, cred, request);

    if (log.isLoggable(Level.FINE))
      log.fine("digest: " + username + " -> " + user);

    return user;
  }

  /**
   * Sends a challenge for basic authentication.
   */
  protected void loginChallenge(HttpServletRequest req,
                                HttpServletResponse res)
    throws ServletException, IOException
  {
    String realm = getRealmName();
    if (realm == null)
      realm = "resin";

    StringBuilder cb = new StringBuilder();
    Base64.encode(cb, RandomUtil.getRandomLong());
    String nonce = cb.toString();
    cb.setLength(0);
    cb.append("Digest ");
    cb.append("realm=\"");
    cb.append(realm);
    cb.append("\", qop=\"auth\", ");
    cb.append("nonce=\"");
    cb.append(nonce);
    cb.append("\"");

    res.setHeader("WWW-Authenticate", cb.toString());
    
    res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
  }

  protected long getRandomLong(ServletContext application)
  {
    return RandomUtil.getRandomLong();
  }

  protected byte []decodeDigest(String digest)
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

  protected String scanKey(CharCursor cursor)
  {
    int ch;
    while (XmlChar.isWhitespace((ch = cursor.current())) || ch == ',') {
      cursor.next();
    }

    ch = cursor.current();
    if (ch == CharacterIterator.DONE)
      return null;
    
    if (! XmlChar.isNameStart(ch))
      throw new RuntimeException("bad key: " + (char) ch + " " + cursor);

    CharBuffer cb = CharBuffer.allocate();
    while (XmlChar.isNameChar(ch = cursor.read())) {
      cb.append((char) ch);
    }
    if (ch != CharacterIterator.DONE)
      cursor.previous();

    return cb.close();
  }

  protected String scanValue(CharCursor cursor)
  {
    int ch;
    skipWhitespace(cursor);

    ch = cursor.read();
    if (ch != '=')
      throw new RuntimeException("expected '='");

    skipWhitespace(cursor);
    
    CharBuffer cb = CharBuffer.allocate();
    
    ch = cursor.read();
    if (ch == '"')
      while ((ch = cursor.read()) != CharacterIterator.DONE && ch != '"')
        cb.append((char) ch);
    else {
      for (;
           ch != CharacterIterator.DONE && ch != ','
             && ! XmlChar.isWhitespace(ch);
           ch = cursor.read())
        cb.append((char) ch);

      if (ch != CharacterIterator.DONE)
        cursor.previous();
    }

    return cb.close();
  }

  protected void skipWhitespace(CharCursor cursor)
  {
    while (XmlChar.isWhitespace(cursor.current())) {
      cursor.next();
    }
  }
}
