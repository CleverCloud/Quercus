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

package com.caucho.quercus.lib;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.QDate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * PHP HTTP functions
 */
public class HttpModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(HttpModule.class);

  private static ArrayList<String> getHeaders(Env env)
  {
    ArrayList<String> headers
      = (ArrayList) env.getSpecialValue("caucho.headers");

    if (headers == null) {
      headers = new ArrayList<String>();
      env.setSpecialValue("caucho.headers", headers);
    }

    return headers;
  }

  /**
   * Adds a header.
   */
  public static Value header(Env env,
                             StringValue headerStr,
                             @Optional("true") boolean replace,
                             @Optional long httpResponseCode)
  {
    HttpServletResponse res = env.getResponse();

    if (res == null) {
      env.error(L.l("header requires an http context"));
      return NullValue.NULL;
    }

    String header = headerStr.toString();
    int len = header.length();

    if (header.startsWith("HTTP/")) {
      int p = header.indexOf(' ');
      int status = 0;
      int ch;

      for (; p < len && header.charAt(p) == ' '; p++) {
      }

      for (; p < len && '0' <= (ch = header.charAt(p)) && ch <= '9'; p++) {
        status = 10 * status + ch - '0';
      }

      for (; p < len && header.charAt(p) == ' '; p++) {
      }

      if (status > 0) {
        res.setStatus(status, header.substring(p));

        return NullValue.NULL;
      }
    }

    int colonIndex = header.indexOf(':');

    if (colonIndex > 0) {
      String key = header.substring(0, colonIndex).trim();
      String value = header.substring(colonIndex + 1).trim();

      if (key.equalsIgnoreCase("Location")) {
        // do not use sendRedirect because sendRedirect commits the response,
        // preventing Wordpress from sending a second Location header that
        // replaces the previous one
        //res.sendRedirect(value);
        //return NullValue.NULL;
        
        res.setStatus(302, "Found");
      }

      if (replace) {
        res.setHeader(key, value);

        ArrayList<String> headers = getHeaders(env);

        int regionEnd = colonIndex + 1;

        for (int i = 0; i < headers.size(); i++) {

          String compare = headers.get(i);

          if (compare.regionMatches(true, 0, header, 0, regionEnd)) {
            headers.remove(i);
            break;
          }
        }

        headers.add(header);
      }
      else {
        res.addHeader(key, value);
        getHeaders(env).add(header);
      }

      if (key.equalsIgnoreCase("Content-Type")) {
        String encoding = env.getOutputEncoding();

        if (encoding != null) {
          if (value.indexOf("charset") < 0) {
            if (value.indexOf("text/") < 0)
              res.setCharacterEncoding(encoding);
          }
          else if ("".equals(res.getCharacterEncoding())) {
            // php/1b0d
            res.setCharacterEncoding(encoding);
          }
        }
      }
    } else {
      // Check for special headers that are not
      // colon separated "key: value" pairs.

      if (header.equals("Not Modified")
          || header.equals("No Content")) {
        // php/1b0(j|k|l|m)

        if (httpResponseCode != 0)
          res.setStatus((int) httpResponseCode, header);
      }
    }

    return NullValue.NULL;
  }

  /**
   * Return a list of the headers that have been sent or are ready to send.
   */

  public static ArrayValue headers_list(Env env)
  {
    ArrayList<String> headersList = getHeaders(env);
    int size = headersList.size();

    ArrayValueImpl headersArray = new ArrayValueImpl(size);

    for (int i = 0; i < size; i++)
      headersArray.put(headersList.get(i));

    return headersArray;
  }

  /**
   * Return true if the headers have been sent.
   */
  public static boolean headers_sent(Env env,
                                     @Optional @Reference Value file,
                                     @Optional @Reference Value line)
  {
    HttpServletResponse res = env.getResponse();

    return res.isCommitted();
  }

  /**
   * Sets a cookie
   */
  public static boolean setcookie(Env env,
                                  String name,
                                  @Optional String value,
                                  @Optional long expire,
                                  @Optional String path,
                                  @Optional String domain,
                                  @Optional boolean secure,
                                  @Optional boolean httpOnly)
  {
    long now = env.getCurrentTime();

    if (value == null || value.equals(""))
      value = "";

    StringBuilder sb = new StringBuilder();
    int len = value.length();

    for (int i = 0; i < len; i++) {
      char ch = value.charAt(i);

      if ('0' <= ch && ch <= '9'
          || 'a' <= ch && ch <= 'z'
          || 'A' <= ch && ch <= 'Z'
          || ch == '-'
          || ch == '.'
          || ch == '_') {
        sb.append(ch);
      }
      else if (ch == ' ') {
        sb.append('+');
      }
      else {
        sb.append('%');

        int d = (ch / 16) & 0xf;
        if (d < 10)
          sb.append((char) ('0' + d));
        else
          sb.append((char) ('A' + d - 10));

        d = ch & 0xf;
        if (d < 10)
          sb.append((char) ('0' + d));
        else
          sb.append((char) ('A' + d - 10));
      }
    }

    Cookie cookie = new Cookie(name, sb.toString());

    int maxAge = 0;

    if (expire > 0) {
      maxAge = (int) (expire - now / 1000);
      
      if (maxAge > 0)
        cookie.setMaxAge(maxAge);
      else
        cookie.setMaxAge(0); //php/1b0i
    }

    if (path != null && ! path.equals(""))
      cookie.setPath(path);

    if (domain != null && ! domain.equals(""))
      cookie.setDomain(domain);

    if (secure)
      cookie.setSecure(true);

    env.getResponse().addCookie(cookie);

    // add to headers list

    StringBuilder cookieHeader = new StringBuilder();
    cookieHeader.append("Set-Cookie: ");

    cookieHeader.append(cookie.getName());

    cookieHeader.append("=");
    cookieHeader.append(cookie.getValue());

    if (maxAge == 0) {
      cookieHeader.append("; expires=Thu, 01-Dec-1994 16:00:00 GMT");
    }
    else {
      QDate date = env.getGmtDate();
      
      date.setGMTTime(now + 1000L * (long) maxAge);
      cookieHeader.append("; expires=");
      cookieHeader.append(date.format("%a, %d-%b-%Y %H:%M:%S GMT"));
    }

    if (path != null && ! path.equals("")) {
      cookieHeader.append("; path=");
      cookieHeader.append(path);
    }

    if (domain != null && ! domain.equals("")) {
      cookieHeader.append("; domain=");
      cookieHeader.append(domain);
    }

    if (secure)
      cookieHeader.append("; secure");

    getHeaders(env).add(cookieHeader.toString());

    return true;
  }
}

