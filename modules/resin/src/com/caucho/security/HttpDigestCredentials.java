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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.security;

/**
 * HTTP Digest-based credentials
 *
 * The HTTP Digest authentication uses the following algorithm
 * to calculate the digest.  The digest is then compared to
 * the client digest.
 *
 * <code><pre>
 * A1 = MD5(username + ':' + realm + ':' + password)
 * A2 = MD5(method + ':' + uri)
 * digest = MD5(A1 + ':' + nonce + ':' + A2)
 * </pre></code>
 */
public class HttpDigestCredentials implements Credentials
{
  private String _cnonce;
  private String _method;
  private String _nc;
  private String _nonce;
  private String _qop;
  private String _realm;
  private byte []_response;
  private String _uri;
  
  public HttpDigestCredentials()
  {
  }

  public String getCnonce()
  {
    return _cnonce;
  }

  public void setCnonce(String cnonce)
  {
    _cnonce = cnonce;
  }

  public String getMethod()
  {
    return _method;
  }

  public void setMethod(String method)
  {
    _method = method;
  }

  public String getNc()
  {
    return _nc;
  }

  public void setNc(String nc)
  {
    _nc = nc;
  }

  public String getNonce()
  {
    return _nonce;
  }

  public void setNonce(String nonce)
  {
    _nonce = nonce;
  }

  public String getQop()
  {
    return _qop;
  }

  public void setQop(String qop)
  {
    _qop = qop;
  }

  public String getRealm()
  {
    return _realm;
  }

  public void setRealm(String realm)
  {
    _realm = realm;
  }

  public byte []getResponse()
  {
    return _response;
  }

  public void setResponse(byte []response)
  {
    _response = response;
  }

  public String getUri()
  {
    return _uri;
  }

  public void setUri(String uri)
  {
    _uri = uri;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}

