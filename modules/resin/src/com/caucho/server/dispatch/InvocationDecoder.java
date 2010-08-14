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

package com.caucho.server.dispatch;

import com.caucho.config.ConfigException;
import com.caucho.i18n.CharacterEncoding;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.ByteToChar;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decodes invocation URI.
 */
public class InvocationDecoder {
  private static final Logger log
    = Logger.getLogger(InvocationDecoder.class.getName());
  static final L10N L = new L10N(InvocationDecoder.class);

  // The character encoding
  private String _encoding = "UTF-8";

  private String _sessionCookie = "JSESSIONID";
  private String _sslSessionCookie;
  
  // The URL-encoded session suffix
  private String _sessionSuffix = ";jsessionid=";
  private char _sessionSuffixChar = ';';

  // The URL-encoded session prefix
  private String _sessionPrefix;
  private int _maxURILength = 1024;

  /**
   * Creates the invocation decoder.
   */
  public InvocationDecoder()
  {
    _encoding = CharacterEncoding.getLocalEncoding();
    if (_encoding == null)
      _encoding = "UTF-8";
  }

  /**
   * Returns the character encoding.
   */
  public String getEncoding()
  {
    return _encoding;
  }

  /**
   * Sets the character encoding.
   */
  public void setEncoding(String encoding)
  {
    _encoding = encoding;
  }
  
  /**
   * Sets the session cookie
   */
  public void setSessionCookie(String cookie)
  {
    _sessionCookie = cookie;
  }

  /**
   * Gets the session cookie
   */
  public String getSessionCookie()
  {
    return _sessionCookie;
  }
  
  /**
   * Sets the SSL session cookie
   */
  public void setSSLSessionCookie(String cookie)
  {
    _sslSessionCookie = cookie;
  }

  /**
   * Gets the SSL session cookie
   */
  public String getSSLSessionCookie()
  {
    if (_sslSessionCookie != null)
      return _sslSessionCookie;
    else
      return _sessionCookie;
  }

  /**
   * Sets the session url prefix.
   */
  public void setSessionURLPrefix(String prefix)
  {
    _sessionSuffix = prefix;
    if (_sessionSuffix != null)
      _sessionSuffixChar = _sessionSuffix.charAt(0);
  }

  /**
   * Gets the session url prefix.
   */
  public String getSessionURLPrefix()
  {
    return _sessionSuffix;
  }

  /**
   * Sets the alternate session url prefix.
   */
  public void setAlternateSessionURLPrefix(String prefix)
    throws ConfigException
  {
    if (! prefix.startsWith("/"))
      prefix = '/' + prefix;

    if (prefix.lastIndexOf('/') > 0)
      throw new ConfigException(L.l("`{0}' is an invalidate alternate-session-url-prefix.  The url-prefix must not have any embedded '/'.", prefix));
    
    _sessionPrefix = prefix;
    _sessionSuffix = null;
  }

  /**
   * Gets the session url prefix.
   */
  public String getAlternateSessionURLPrefix()
  {
    return _sessionPrefix;
  }

  public int getMaxURILength()
  {
    return _maxURILength;
  }

  public void setMaxURILength(int maxURILength)
  {
    _maxURILength = maxURILength;
  }

  /**
   * Splits out the query string and unescape the value.
   */
  public void splitQueryAndUnescape(Invocation invocation,
                                    byte []rawURI, int uriLength)
    throws IOException
  {
    for (int i = 0; i < uriLength; i++) {
      if (rawURI[i] == '?') {
        i++;

        // XXX: should be the host encoding?
        String queryString = byteToChar(rawURI, i, uriLength - i,
                                        "ISO-8859-1");
        invocation.setQueryString(queryString);

        uriLength = i - 1;
        break;
      }
    }

    String rawURIString = byteToChar(rawURI, 0, uriLength, "ISO-8859-1");
    invocation.setRawURI(rawURIString);
    
    String decodedURI = normalizeUriEscape(rawURI, 0, uriLength, _encoding);

    if (_sessionSuffix != null) {
      int p = decodedURI.indexOf(_sessionSuffix);

      if (p >= 0) {
        int suffixLength = _sessionSuffix.length();
        int tail = decodedURI.indexOf(';', p + suffixLength);
        String sessionId;

        if (tail > 0)
          sessionId = decodedURI.substring(p + suffixLength, tail);
        else
          sessionId = decodedURI.substring(p + suffixLength);

        decodedURI = decodedURI.substring(0, p);

        invocation.setSessionId(sessionId);

        p = rawURIString.indexOf(_sessionSuffix);
        if (p > 0) {
          rawURIString = rawURIString.substring(0, p);
          invocation.setRawURI(rawURIString);
        }
      }
    }
    else if (_sessionPrefix != null) {
      if (decodedURI.startsWith(_sessionPrefix)) {
        int prefixLength = _sessionPrefix.length();

        int tail = decodedURI.indexOf('/', prefixLength);
        String sessionId;

        if (tail > 0) {
          sessionId = decodedURI.substring(prefixLength, tail);
          decodedURI = decodedURI.substring(tail);
          invocation.setRawURI(rawURIString.substring(tail));
        }
        else {
          sessionId = decodedURI.substring(prefixLength);
          decodedURI = "/";
          invocation.setRawURI("/");
        }

        invocation.setSessionId(sessionId);
      }
    }

    String uri = normalizeUri(decodedURI);

    invocation.setURI(uri);
    invocation.setContextURI(uri);
  }

  /**
   * Splits out the query string, and normalizes the URI, assuming nothing
   * needs unescaping.
   */
  public void splitQuery(Invocation invocation, String rawURI)
    throws IOException
  {
    int p = rawURI.indexOf('?');
    if (p > 0) {
      invocation.setQueryString(rawURI.substring(p + 1));
      
      rawURI = rawURI.substring(0, p);
    }

    invocation.setRawURI(rawURI);
    
    String uri = normalizeUri(rawURI);

    invocation.setURI(uri);
    invocation.setContextURI(uri);
  }

  /**
   * Just normalize the URI.
   */
  public void normalizeURI(Invocation invocation, String rawURI)
    throws IOException
  {
    invocation.setRawURI(rawURI);
    
    String uri = normalizeUri(rawURI);

    invocation.setURI(uri);
    invocation.setContextURI(uri);
  }

  /**
   * Splits out the session.
   */
  public void splitSession(Invocation invocation)
  {
    if (_sessionSuffix != null) {
      String uri = invocation.getURI();
    }
    else if (_sessionPrefix != null) {
      String uri = invocation.getURI();
    }
  }

  private String byteToChar(byte []buffer, int offset, int length,
                            String encoding)
  {
    ByteToChar converter = ByteToChar.create();
    // XXX: make this configurable

    if (encoding == null)
      encoding = "utf-8";

    try {
      converter.setEncoding(encoding);
    } catch (UnsupportedEncodingException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    try {
      for (; length > 0; length--)
        converter.addByte(buffer[offset++]);
      
      return converter.getConvertedString();
    } catch (IOException e) {
      return "unknown";
    }
  }

  /**
   * Normalize a uri to remove '///', '/./', 'foo/..', etc.
   *
   * @param uri the raw uri to be normalized
   * @return a normalized URI
   */
  public String normalizeUri(String uri)
    throws IOException
  {
    return normalizeUri(uri, CauchoSystem.isWindows());
  }

  /**
   * Normalize a uri to remove '///', '/./', 'foo/..', etc.
   *
   * @param uri the raw uri to be normalized
   * @return a normalized URI
   */
  public String normalizeUri(String uri, boolean isWindows)
    throws IOException
  {
    CharBuffer cb = new CharBuffer();

    int len = uri.length();

    if (len > _maxURILength)
      throw new BadRequestException(L.l("The request contains an illegal URL."));

    boolean isBogus;
    char ch;
    char ch1;
    if (len == 0 || (ch = uri.charAt(0)) != '/' && ch != '\\')
      cb.append('/');

    for (int i = 0; i < len; i++) {
      ch = uri.charAt(i);

      if (ch == '/' || ch == '\\') {
      dots:
        while (i + 1 < len) {
          ch = uri.charAt(i + 1);

          if (ch == '/' || ch == '\\')
            i++;
          else if (ch != '.')
            break dots;
          else if (len <= i + 2
                   || (ch = uri.charAt(i + 2)) == '/' || ch == '\\') {
            i += 2;
          }
          else if (ch != '.')
            break dots;
          else if (len <= i + 3
                   || (ch = uri.charAt(i + 3)) == '/' || ch == '\\') {
            int j;

            for (j = cb.length() - 1; j >= 0; j--) {
              if ((ch = cb.charAt(j)) == '/' || ch == '\\')
                break;
            }
            if (j > 0)
              cb.setLength(j);
            else
              cb.setLength(0);
            i += 3;
          } else {
            throw new BadRequestException(L.l("The request contains an illegal URL."));
          }
        }

        while (isWindows && cb.getLength() > 0
               && ((ch = cb.getLastChar()) == '.' || ch == ' ')) {
          cb.setLength(cb.getLength() - 1);

          if (cb.getLength() > 0
              && (ch = cb.getLastChar()) == '/' || ch == '\\') {
            cb.setLength(cb.getLength() - 1);
            // server/003n
            continue;
          }
        }

        cb.append('/');
      }
      else if (ch == 0)
        throw new BadRequestException(L.l("The request contains an illegal URL."));
      else
        cb.append(ch);
    }

    while (isWindows && cb.getLength() > 0
           && ((ch = cb.getLastChar()) == '.' || ch == ' ')) {
      cb.setLength(cb.getLength() - 1);
    }

    return cb.toString();
  }

  /**
   * Converts the escaped URI to a string.
   *
   * @param rawUri the escaped URI
   * @param i index into the URI
   * @param len the length of the uri
   * @param encoding the character encoding to handle %xx
   *
   * @return the converted URI
   */
  private static String normalizeUriEscape(byte []rawUri, int i, int len,
                                           String encoding)
    throws IOException
  {
    ByteToChar converter = ByteToChar.create();
    // XXX: make this configurable

    if (encoding == null)
      encoding = "utf-8";

    try {
      converter.setEncoding(encoding);
    } catch (UnsupportedEncodingException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    try {
      while (i < len) {
        int ch = rawUri[i++] & 0xff;

        if (ch == '%')
          i = scanUriEscape(converter, rawUri, i, len);
        else
          converter.addByte(ch);
      }

      return converter.getConvertedString();
    } catch (Exception e) {
      throw new BadRequestException(L.l("The URL contains escaped bytes unsupported by the {0} encoding.", encoding));
    }
  }

  /**
   * Scans the next character from URI, adding it to the converter.
   *
   * @param converter the byte-to-character converter
   * @param rawUri the raw URI
   * @param i index into the URI
   * @param len the raw URI length
   *
   * @return next index into the URI
   */
  private static int scanUriEscape(ByteToChar converter,
                                   byte []rawUri, int i, int len)
    throws IOException
  {
    int ch1 = i < len ? (rawUri[i++] & 0xff) : -1;

    if (ch1 == 'u') {
      ch1 = i < len ? (rawUri[i++] & 0xff) : -1;
      int ch2 = i < len ? (rawUri[i++] & 0xff) : -1;
      int ch3 = i < len ? (rawUri[i++] & 0xff) : -1;
      int ch4 = i < len ? (rawUri[i++] & 0xff) : -1;

      converter.addChar((char) ((toHex(ch1) << 12) +
                                (toHex(ch2) << 8) + 
                                (toHex(ch3) << 4) + 
                                (toHex(ch4))));
    }
    else {
      int ch2 = i < len ? (rawUri[i++] & 0xff) : -1;

      int b = (toHex(ch1) << 4) + toHex(ch2);;

      converter.addByte(b);
    }

    return i;
  }

  /**
   * Convert a character to hex
   */
  private static int toHex(int ch)
  {
    if (ch >= '0' && ch <= '9')
      return ch - '0';
    else if (ch >= 'a' && ch <= 'f')
      return ch - 'a' + 10;
    else if (ch >= 'A' && ch <= 'F')
      return ch - 'A' + 10;
    else
      return -1;
  }
}
