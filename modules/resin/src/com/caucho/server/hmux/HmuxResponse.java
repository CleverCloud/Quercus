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

package com.caucho.server.hmux;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.Cookie;

import com.caucho.server.http.AbstractHttpResponse;
import com.caucho.server.http.AbstractResponseStream;
import com.caucho.server.http.HttpServletResponseImpl;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.WriteStream;

/**
 * Handles a response for a srun connection, i.e. a connection to
 * a web server plugin.
 */
public class HmuxResponse extends AbstractHttpResponse {
  private final HmuxRequest _req;

  HmuxResponse(HmuxRequest request, WriteStream rawWrite)
  {
    super(request);

    _req = request;

    if (_req == null)
      throw new NullPointerException();
  }

  @Override
  protected AbstractResponseStream createResponseStream()
  {
    HmuxRequest request = (HmuxRequest) getRequest();

    return new HmuxResponseStream(request, this, request.getRawWrite());
  }

  /**
   * headersWritten cannot be undone for hmux
   */
  @Override
  public void setHeaderWritten(boolean isWritten)
  {
    // server/265a
  }

  @Override
  protected boolean writeHeadersInt(int length,
                                    boolean isHead)
    throws IOException
  {
    if (! _request.hasRequest())
      return false;

    HttpServletResponseImpl response = _request.getResponseFacade();

    int statusCode = response.getStatus();

    CharBuffer cb = _cb;
    cb.clear();
    cb.append((char) ((statusCode / 100) % 10 + '0'));
    cb.append((char) ((statusCode / 10) % 10 + '0'));
    cb.append((char) (statusCode % 10 + '0'));
    cb.append(' ');
    cb.append(response.getStatusMessage());

    _req.writeStatus(cb);

    if (statusCode >= 400) {
      removeHeader("ETag");
      removeHeader("Last-Modified");
    }
    else if (response.isNoCache()) {
      removeHeader("ETag");
      removeHeader("Last-Modified");

      setHeader("Expires", "Thu, 01 Dec 1994 16:00:00 GMT");
      _req.writeHeader("Cache-Control", "no-cache");
    }
    else if (response.isPrivateCache())
      _req.writeHeader("Cache-Control", "private");

    int load = (int) (1000 * _req.getServer().getCpuLoad());
    if (Alarm.isTest())
      load = 0;

    _req.writeString(HmuxRequest.HMUX_META_HEADER, "cpu-load");
    _req.writeString(HmuxRequest.HMUX_STRING, String.valueOf(load));

    int size = _headerKeys.size();
    for (int i = 0; i < size; i++) {
      String key = (String) _headerKeys.get(i);
      String value = (String) _headerValues.get(i);

      _req.writeHeader(key, value);
    }

    if (_contentLength >= 0) {
      cb.clear();
      cb.append(_contentLength);
      _req.writeHeader("Content-Length", cb);
    }
    else if (length >= 0) {
      cb.clear();
      cb.append(length);
      _req.writeHeader("Content-Length", cb);
    }

    HttpServletResponseImpl responseFacade = _request.getResponseFacade();

    long now = Alarm.getCurrentTime();
    ArrayList<Cookie> cookiesOut = responseFacade.getCookies();

    if (cookiesOut != null) {
      size = cookiesOut.size();
      for (int i = 0; i < size; i++) {
        Cookie cookie = cookiesOut.get(i);
        int cookieVersion = cookie.getVersion();

        fillCookie(cb, cookie, now, 0, false);
        _req.writeHeader("Set-Cookie", cb);
        if (cookieVersion > 0) {
          fillCookie(cb, cookie, now, cookieVersion, true);
          _req.writeHeader("Set-Cookie2", cb);
        }
      }
    }

    String contentType = responseFacade.getContentTypeImpl();
    String charEncoding = responseFacade.getCharacterEncodingImpl();

    if (contentType != null) {
      if (charEncoding != null)
        _req.writeHeader("Content-Type", contentType + "; charset=" + charEncoding);
      else
        _req.writeHeader("Content-Type", contentType);

    }

    _req.sendHeader();

    return false;
  }
}
