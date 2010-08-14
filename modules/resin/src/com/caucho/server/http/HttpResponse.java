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

package com.caucho.server.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.caucho.network.listen.TcpSocketLink;
import com.caucho.server.cluster.Server;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.WriteStream;

public class HttpResponse extends AbstractHttpResponse
{
  static final byte []_http10ok = "HTTP/1.0 200 OK".getBytes();
  static final byte []_http11ok = "HTTP/1.1 200 OK".getBytes();
  static final byte []_contentLengthBytes = "\r\nContent-Length: ".getBytes();
  static final byte []_contentTypeBytes = "\r\nContent-Type: ".getBytes();
  static final byte []_textHtmlBytes = "\r\nContent-Type: text/html".getBytes();
  static final byte []_charsetBytes = "; charset=".getBytes();
  static final byte []_textHtmlLatin1Bytes = "\r\nContent-Type: text/html; charset=iso-8859-1".getBytes();

  static final byte []_connectionCloseBytes = "\r\nConnection: close".getBytes();

  final byte []_resinServerBytes;

  static final char []_connectionCb = "Connection".toCharArray();
  static final CharBuffer _closeCb = new CharBuffer("Close");

  private final HttpRequest _request;

  private final byte []_dateBuffer = new byte[256];
  private final CharBuffer _dateCharBuffer = new CharBuffer();

  private int _dateBufferLength;
  private long _lastDate;
  private boolean _isChunked;

  private WriteStream _rawWrite;

  /**
   * Creates a new HTTP-protocol response.
   *
   * @param request the matching request object.
   */
  HttpResponse(HttpRequest request, WriteStream rawWrite)
  {
    super(request);

    _request = request;
    _rawWrite = rawWrite;

    Server server = (Server) request.getDispatchServer();

    _resinServerBytes = ("\r\nServer: " + server.getServerHeader()).getBytes();
  }

  @Override
  protected AbstractResponseStream createResponseStream()
  {
    HttpRequest request = (HttpRequest) getRequest();

    return new HttpResponseStream(this, request.getRawWrite());
  }

  boolean isChunkedEncoding()
  {
    return _isChunked;
  }

  protected WriteStream getRawWrite()
  {
    return _rawWrite;
  }

  /**
   * Upgrade protocol
   */
  /*
  @Override
  public TcpDuplexController upgradeProtocol(TcpDuplexHandler handler)
  {
    TcpConnection conn
      = (TcpConnection) ((TcpServerRequest) getRequest()).getConnection();

    TcpDuplexController controller = conn.toDuplex(handler);

    HttpServletResponseImpl response = _request.getResponseFacade();

    response.setStatus(101);
    setContentLength(0);

    if (log.isLoggable(Level.FINE))
      log.fine(this + " upgrade HTTP to " + handler);

    return controller;
  }
  */

  /**
   * Writes the 100 continue response.
   */
  @Override
    protected void writeContinueInt()
    throws IOException
  {
    // #2938, server/0558
    /*
    os.print("HTTP/1.1 100 Continue");

    if (! containsHeader("Server"))
      os.write(_resinServerBytes, 0, _resinServerBytes.length);

    os.print("\r\nContent-Length: 0");

    long now = Alarm.getCurrentTime();
    if (_lastDate + 1000 < now) {
      fillDate(now);
    }

    os.write(_dateBuffer, 0, _dateBufferLength);
    os.flush();
    */

    // server/269n
    WriteStream os = getRawWrite();
    os.print("HTTP/1.1 100 Continue\r\n\r\n");
    os.flush();
  }

  /**
   * Implementation to write the HTTP headers.  If the length is positive,
   * it's a small request where the buffer contains the entire request,
   * so the length is already known.
   *
   * @param os the output stream to write the headers to.
   * @param length if non-negative, the length of the entire request.
   *
   * @return true if the data in the request should use chunked encoding.
   */
  @Override
  protected boolean writeHeadersInt(int length,
                                    boolean isHead)
    throws IOException
  {
    HttpServletRequestImpl request = _request.getRequestFacade();
    HttpServletResponseImpl response = _request.getResponseFacade();

    if (request == null)
      return false;

    _isChunked = false;

    int version = _request.getVersion();
    boolean debug = log.isLoggable(Level.FINE);

    if (version < HttpRequest.HTTP_1_0) {
      _request.killKeepalive();
      return false;
    }

    TcpSocketLink tcpConn = null;

    if (_request.getConnection() instanceof TcpSocketLink)
      tcpConn = (TcpSocketLink) _request.getConnection();

    WebApp webApp = request.getWebApp();

    String contentType = response.getContentTypeImpl();
    String charEncoding = response.getCharacterEncodingImpl();

    WriteStream os = getRawWrite();

    int statusCode = response.getStatus();
    if (statusCode == 200) {
      if (version < HttpRequest.HTTP_1_1)
        os.write(_http10ok, 0, _http10ok.length);
      else
        os.write(_http11ok, 0, _http11ok.length);
    } else {
      if (version < HttpRequest.HTTP_1_1)
        os.printLatin1("HTTP/1.0 ");
      else
        os.printLatin1("HTTP/1.1 ");

      os.write((statusCode / 100) % 10 + '0');
      os.write((statusCode / 10) % 10 + '0');
      os.write(statusCode % 10 + '0');
      os.write(' ');
      os.printLatin1(response.getStatusMessage());
    }

    if (debug) {
      log.fine(_request.dbgId() + "HTTP/1.1 " +
               statusCode + " " + response.getStatusMessage());
    }

    boolean isUpgrade = false;

    if (tcpConn != null && tcpConn.isDuplex()) {
      isUpgrade = true;

      String upgrade = getHeader("Upgrade");

      if (upgrade != null) {
        os.printLatin1("\r\nUpgrade: ");
        os.printLatin1(upgrade);
      }

      os.printLatin1("\r\nConnection: Upgrade");
      _request.killKeepalive();

      if (debug)
        log.fine(_request.dbgId() + "Connection: Upgrade");
    }

    if (! containsHeader("Server")) {
      os.write(_resinServerBytes, 0, _resinServerBytes.length);
    }

    if (statusCode >= 400) {
      removeHeader("ETag");
      removeHeader("Last-Modified");
    }
    else if (statusCode == HttpServletResponse.SC_NOT_MODIFIED
             || statusCode == HttpServletResponse.SC_NO_CONTENT) {
      // php/1b0k

      contentType = null;
    }
    else if (response.isCacheControl()) {
      // application manages cache control
    }
    else if (response.isNoCache()) {
      // server/1b15
      removeHeader("ETag");
      removeHeader("Last-Modified");

      // even in case of 302, this may be needed for filters which
      // automatically set cache headers
      setHeaderImpl("Expires", "Thu, 01 Dec 1994 16:00:00 GMT");

      os.printLatin1("\r\nCache-Control: no-cache");

      if (debug) {
        log.fine(_request.dbgId() + "" +
                 "Cache-Control: no-cache");
      }
    }
    else if (response.isNoCacheUnlessVary()
             && ! containsHeader("Vary")) {
      os.printLatin1("\r\nCache-Control: private");

      if (debug) {
        log.fine(_request.dbgId() + "Cache-Control: private");
      }
    }
    else if (response.isPrivateCache()) {
      if (HttpRequest.HTTP_1_1 <= version) {
        // technically, this could be private="Set-Cookie,Set-Cookie2"
        // but caches don't recognize it, so there's no real extra value
        os.printLatin1("\r\nCache-Control: private");

        if (debug)
          log.fine(_request.dbgId() + "Cache-Control: private");
      }
      else {
        setHeaderImpl("Expires", "Thu, 01 Dec 1994 16:00:00 GMT");
        os.printLatin1("\r\nCache-Control: no-cache");

        if (debug) {
          log.fine(_request.dbgId() + "CacheControl: no-cache");
        }
      }
    }

    int size = _headerKeys.size();
    for (int i = 0; i < size; i++) {
      String key = (String) _headerKeys.get(i);

      if (isUpgrade && "Upgrade".equalsIgnoreCase(key))
        continue;

      os.write('\r');
      os.write('\n');
      os.printLatin1(key);
      os.write(':');
      os.write(' ');
      os.printLatin1((String) _headerValues.get(i));

      if (debug) {
        log.fine(_request.dbgId() + "" +
                 key + ": " + _headerValues.get(i));
      }
    }

    long now = Alarm.getCurrentTime();
    ArrayList<Cookie> cookiesOut = response.getCookies();

    if (cookiesOut != null) {
      for (int i = 0; i < cookiesOut.size(); i++) {
        Cookie cookie = cookiesOut.get(i);
        int cookieVersion = cookie.getVersion();

        CharBuffer cb = _cb;
        // XXX:
        fillCookie(cb, cookie, now, cookieVersion, false);
        os.printLatin1("\r\nSet-Cookie: ");
        os.printLatin1(cb.getBuffer(), 0, cb.getLength());
        if (cookieVersion > 0) {
          fillCookie(cb, cookie, now, cookieVersion, true);
          os.printLatin1("\r\nSet-Cookie2: ");
          os.printLatin1(cb.getBuffer(), 0, cb.getLength());
        }

        if (debug)
          log.fine(_request.dbgId() + "Set-Cookie: " + cb);
      }
    }

    if (contentType != null) {
      if (charEncoding == null) {
        if (webApp != null)
          charEncoding = webApp.getCharacterEncoding();

        // always use a character encoding to avoid XSS attacks (?)
        if (charEncoding == null)
          charEncoding = "utf-8";
      }

      os.write(_contentTypeBytes, 0, _contentTypeBytes.length);
      os.printLatin1(contentType);
      os.write(_charsetBytes, 0, _charsetBytes.length);
      os.printLatin1(charEncoding);

      if (debug) {
        log.fine(_request.dbgId() + "Content-Type: " + contentType
                 + "; charset=" + charEncoding);
      }
    }

    if (hasFooter()) {
      _contentLength = -1;
      length = -1;
    }

    boolean hasContentLength = false;
    /*
    if (isHead()) {
      // server/269t, server/0560
      hasContentLength = true;
      os.write(_contentLengthBytes, 0, _contentLengthBytes.length);
      os.print(0);
    }
    else
    */
    if (_contentLength >= 0) {
      os.write(_contentLengthBytes, 0, _contentLengthBytes.length);
      os.print(_contentLength);
      hasContentLength = true;

      if (debug)
        log.fine(_request.dbgId() + "Content-Length: " + _contentLength);
    }
    else if (statusCode == HttpServletResponse.SC_NOT_MODIFIED) {
      // #3089
      // In the HTTP spec, a 304 has no message body so the content-length
      // is not needed.  The content-length is not explicitly forbidden,
      // but does cause problems with certain clients.
      hasContentLength = true;
      setHead();
    }
    else if (statusCode == HttpServletResponse.SC_NO_CONTENT) {
      hasContentLength = true;
      os.write(_contentLengthBytes, 0, _contentLengthBytes.length);
      os.print(0);
      setHead();

      if (debug)
        log.fine(_request.dbgId() + "Content-Length: 0");
    }
    else if (length >= 0) {
      os.write(_contentLengthBytes, 0, _contentLengthBytes.length);
      os.print(length);
      hasContentLength = true;

      if (debug)
        log.fine(_request.dbgId() + "Content-Length: " + length);
    }

    if (version < HttpRequest.HTTP_1_1) {
      _request.killKeepalive();
    }
    else {
      /* XXX: the request processing already processed this header
      CharSegment conn = _request.getHeaderBuffer(_connectionCb,
                                                  _connectionCb.length);
      if (conn != null && conn.equalsIgnoreCase(_closeCb)) {
        _request.killKeepalive();
      }
      else
      */

      if (_request.isKeepaliveAllowed()) {
      }
      else if (isUpgrade) {
      }
      else {
        os.write(_connectionCloseBytes, 0, _connectionCloseBytes.length);
        _request.killKeepalive();

        if (debug)
          log.fine(_request.dbgId() + "Connection: close");
      }
    }

    if (HttpRequest.HTTP_1_1 <= version
        && ! hasContentLength
        && ! isHead) {
      os.printLatin1("\r\nTransfer-Encoding: chunked");
      _isChunked = true;

      if (debug)
        log.fine(_request.dbgId() + "Transfer-Encoding: chunked");
    }

    if (_lastDate / 1000 != now / 1000) {
      fillDate(now);
    }

    if (_isChunked)
      os.write(_dateBuffer, 0, _dateBufferLength - 2);
    else
      os.write(_dateBuffer, 0, _dateBufferLength);

    return _isChunked;
  }

  private void fillDate(long now)
  {
    if (_lastDate / 60000 == now / 60000) {
      int min = (int) (now / 60000 % 60);
      int sec = (int) (now / 1000 % 60);

      int m2 = '0' + (min / 10);
      int m1 = '0' + (min % 10);

      int s2 = '0' + (sec / 10);
      int s1 = '0' + (sec % 10);

      _dateBuffer[28] = (byte) m2;
      _dateBuffer[29] = (byte) m1;

      _dateBuffer[31] = (byte) s2;
      _dateBuffer[32] = (byte) s1;

      _lastDate = now;

      return;
    }

    _lastDate = now;
    _calendar.setGMTTime(now);
    _dateCharBuffer.clear();
    _dateCharBuffer.append("\r\nDate: ");
    _calendar.printDate(_dateCharBuffer);

    char []cb = _dateCharBuffer.getBuffer();
    int len = _dateCharBuffer.getLength();

    for (int i = len - 1; i >= 0; i--)
      _dateBuffer[i] = (byte) cb[i];

    _dateBuffer[len] = (byte) '\r';
    _dateBuffer[len + 1] = (byte) '\n';
    _dateBuffer[len + 2] = (byte) '\r';
    _dateBuffer[len + 3] = (byte) '\n';

    _dateBufferLength = len + 4;
  }

  public String toString()
  {
    return "HttpResponse" + _request.dbgId();
  }
}
