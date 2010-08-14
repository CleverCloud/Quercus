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

package com.caucho.security.openid;

import com.caucho.config.Config;
import com.caucho.util.L10N;

import java.io.*;
import java.net.*;
import java.util.logging.*;

public class OpenIdRelay
{
  private static final L10N L = new L10N(OpenIdRelay.class);
  private static final Logger log
    = Logger.getLogger(OpenIdRelay.class.getName());

  private static final String OPEN_ID_2_0_SERVER
    = "http://specs.openid.net/auth/2.0/server";

  public String discoverOpenId(String urlString)
  {
    String loc = lookupLocation(urlString);

    if (loc == null)
      return null;

    YadisXrd xrd = readXrd(loc);

    if (xrd == null)
      return null;

    // OP Identifier
    String openIdUrl = xrd.findService(OPEN_ID_2_0_SERVER);

    return openIdUrl;
  }

  public String lookupLocation(String urlString)
  {
    HttpURLConnection conn = null;
    try {
      if (! urlString.startsWith("http"))
        urlString = "http://" + urlString;

      URL url = new URL(urlString);

      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("HEAD");
      conn.setFollowRedirects(true);

      String host = urlString;
      int p = host.indexOf("://");
      host = host.substring(p + 3);
      p = host.indexOf('/');
      if (p > 0)
        host = host.substring(0, p);

      conn.addRequestProperty("Host", host);

      int code = conn.getResponseCode();

      String location = conn.getHeaderField("X-XRDS-Location");

      if (location != null)
        return location;
      else if (code != 200)
        return null;
      
      if ("application/xrds+xml".equals(conn.getHeaderField("Content-Type")))
        return urlString;

      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (conn != null)
        conn.disconnect();
    }
  }

  public YadisXrd readXrd(String urlString)
  {
    HttpURLConnection conn = null;
    try {
      URL url = new URL(urlString);

      conn = (HttpURLConnection) url.openConnection();
      conn.setFollowRedirects(true);

      conn.addRequestProperty("Accept", "application/xrds+xml");

      int code = conn.getResponseCode();

      String contentType = conn.getHeaderField("Content-Type");

      if (code != 200 || ! "application/xrds+xml".equals(contentType))
        throw new RuntimeException(L.l("bad code '{0}' or content-type '{1}' for URL='{2}'",
                                       code, contentType, urlString));
      
      InputStream is = conn.getInputStream();
      int ch;

      YadisXrd xrd = new YadisXrd();
      Config config = new Config();

      config.configure(xrd, is);

      return xrd;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (conn != null)
        conn.disconnect();
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
