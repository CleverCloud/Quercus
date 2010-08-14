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
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.caucho.server.dispatch.BadRequestException;
import com.caucho.server.session.CookieImpl;
import com.caucho.server.session.SessionImpl;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.CaseInsensitiveIntMap;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.TempBuffer;

/**
 * Encapsulates the servlet response, controlling response headers and the
 * response stream.
 */
abstract public class AbstractHttpResponse {
  static final protected Logger log
    = Logger.getLogger(AbstractHttpResponse.class.getName());
  static final L10N L = new L10N(AbstractHttpResponse.class);

  protected static final CaseInsensitiveIntMap _headerCodes;
  protected static final int HEADER_CACHE_CONTROL = 1;
  protected static final int HEADER_CONTENT_TYPE = HEADER_CACHE_CONTROL + 1;
  protected static final int HEADER_CONTENT_LENGTH = HEADER_CONTENT_TYPE + 1;
  protected static final int HEADER_DATE = HEADER_CONTENT_LENGTH + 1;
  protected static final int HEADER_SERVER = HEADER_DATE + 1;
  protected static final int HEADER_CONNECTION = HEADER_SERVER + 1;

  private static final ConcurrentHashMap<String,ContentType> _contentTypeMap
    = new ConcurrentHashMap<String,ContentType>();

  protected final AbstractHttpRequest _request;

  protected final ArrayList<String> _headerKeys = new ArrayList<String>();
  protected final ArrayList<String> _headerValues = new ArrayList<String>();

  protected final ArrayList<String> _footerKeys = new ArrayList<String>();
  protected final ArrayList<String> _footerValues = new ArrayList<String>();

  private final AbstractResponseStream _responseStream;

  private final ServletOutputStreamImpl _responseOutputStream
    = new ServletOutputStreamImpl();
  private final ResponseWriter _responsePrintWriter
    = new ResponseWriter();

  protected final QDate _calendar = new QDate(false);

  protected final CharBuffer _cb = new CharBuffer();
  protected final char [] _headerBuffer = new char[256];

  private HttpBufferStore _bufferStore;

  private boolean _isHeaderWritten;
  private boolean _isClientDisconnect;

  protected long _contentLength;
  private boolean _isClosed;

  protected AbstractHttpResponse(AbstractHttpRequest request)
  {
    _request = request;

    _responseStream = createResponseStream();
  }

  TempBuffer getBuffer()
  {
    return _bufferStore.getTempBuffer();
  }

  protected final QDate getCalendar()
  {
    return _calendar;
  }

  /**
   * If set true, client disconnect exceptions are no propagated to the
   * server code.
   */
  public boolean isIgnoreClientDisconnect()
  {
    return _request.isIgnoreClientDisconnect();
  }

  /**
   * Return true if the client has disconnected
   */
  public boolean isClientDisconnect()
  {
    return _isClientDisconnect;
  }

  /**
   * Called when the client has disconnected
   */
  public void clientDisconnect()
  {
    _request.clientDisconnect();

    _isClientDisconnect = true;
  }

  /**
   * Return true for the top request.
   */
  /*
  public boolean isTop()
  {
    if (_request instanceof AbstractHttpRequest)
      return ((AbstractHttpRequest) _request).isTop();
    else
      return false;
  }
  */

  /**
   * Returns the next response.
   */
  public ServletResponse getResponse()
  {
    return null;
  }

  /**
   * Returns the corresponding request.
   */
  public AbstractHttpRequest getRequest()
  {
    return _request;
  }

  /**
   * Returns true for closed requests.
   */
  public boolean isClosed()
  {
    return _isClosed;
  }

  /**
   * Initializes the Response at the beginning of the request.
   */
  public void startRequest(HttpBufferStore bufferStore)
    throws IOException
  {
    _bufferStore = bufferStore;

    _headerKeys.clear();
    _headerValues.clear();

    _footerKeys.clear();
    _footerValues.clear();

    _responseStream.start();

    _isHeaderWritten = false;
    _isClientDisconnect = false;

    _contentLength = -1;
    _isClosed = false;
  }

  public void startInvocation()
  {
  }

  abstract protected AbstractResponseStream createResponseStream();

  /**
   * For a HEAD request, the response stream should write no data.
   */
  protected void setHead()
  {
    _responseStream.setHead();
  }

  /**
   * For a HEAD request, the response stream should write no data.
   */
  protected final boolean isHead()
  {
    return _responseStream.isHead();
  }

  //
  // headers
  //

  /**
   * Returns true if the response already contains the named header.
   *
   * @param name name of the header to test.
   */
  public boolean containsHeader(String name)
  {
    for (int i = 0; i < _headerKeys.size(); i++) {
      String oldKey = _headerKeys.get(i);

      if (oldKey.equalsIgnoreCase(name))
        return true;
    }

    if (name.equalsIgnoreCase("content-type"))
      return _request.getResponseFacade().getContentType() != null;

    if (name.equalsIgnoreCase("content-length"))
      return _contentLength >= 0;

    return false;
  }

  /**
   * Returns the value of an already set output header.
   *
   * @param name name of the header to get.
   */
  public String getHeader(String name)
  {
    ArrayList<String> keys = _headerKeys;

    int headerSize = keys.size();
    for (int i = 0; i < headerSize; i++) {
      String oldKey = keys.get(i);

      if (oldKey.equalsIgnoreCase(name))
        return (String) _headerValues.get(i);
    }

    if (name.equalsIgnoreCase("content-type"))
      return _request.getResponseFacade().getContentType();

    if (name.equalsIgnoreCase("content-length"))
      return _contentLength >= 0 ? String.valueOf(_contentLength) : null;

    return null;
  }

  /**
   * Sets a header, replacing an already-existing header.
   *
   * @param key the header key to set.
   * @param value the header value to set.
   */
  public void setHeader(String key, String value)
  {
    if (value == null)
      throw new NullPointerException();

    if (setSpecial(key, value)) {
      return;
    }

    // server/05e8 (tck)
    // XXX: server/13w0 for _isHeaderWritten because the Expires in caching
    // occurs after the output fills (committed), which contradicts the tck
    // requirements
    if (isCommitted() && ! _isHeaderWritten) {
      return;
    }

    setHeaderImpl(key, value);
  }


  /**
   * Sets a header, replacing an already-existing header.
   *
   * @param key the header key to set.
   * @param value the header value to set.
   */
  protected void setHeaderImpl(String key, String value)
  {
    int i = 0;
    boolean hasHeader = false;

    ArrayList<String> keys = _headerKeys;
    ArrayList<String> values = _headerValues;

    for (i = keys.size() - 1; i >= 0; i--) {
      String oldKey = keys.get(i);

      if (oldKey.equalsIgnoreCase(key)) {
        if (hasHeader) {
          keys.remove(i);
          values.remove(i);
        }
        else {
          hasHeader = true;

          values.set(i, value);
        }
      }
    }

    if (! hasHeader) {
      keys.add(key);
      values.add(value);
    }
  }



  /**
   * Adds a new header.  If an old header with that name exists,
   * both headers are output.
   *
   * @param key the header key.
   * @param value the header value.
   */
  public void addHeader(String key, String value)
  {
    // server/05e8 (tck)
    if (isCommitted()) {
      return;
    }

    addHeaderImpl(key, value);
  }

  /**
   * Adds a new header.  If an old header with that name exists,
   * both headers are output.
   *
   * @param key the header key.
   * @param value the header value.
   */
  public void addHeaderImpl(String key, String value)
  {
    if (setSpecial(key, value))
      return;

    _headerKeys.add(key);
    _headerValues.add(value);
  }

  protected static ContentType parseContentType(String contentType)
  {
    ContentType item = _contentTypeMap.get(contentType);

    if (item == null) {
      item = new ContentType(contentType);

      _contentTypeMap.put(contentType, item);
    }

    return item;
  }

  /**
   * Special processing for a special value.
   */
  protected boolean setSpecial(String key, String value)
  {
    int length = key.length();
    if (256 <= length)
      return false;

    key.getChars(0, length, _headerBuffer, 0);

    switch (_headerCodes.get(_headerBuffer, length)) {
    case HEADER_CACHE_CONTROL:
      // server/13d9, server/13dg
      if (value.startsWith("max-age")) {
      }
      else if (value.startsWith("s-maxage")) {
      }
      else if (value.equals("x-anonymous")) {
      }
      else {
        _request.getResponseFacade().setCacheControl(true);
      }

      return false;

    case HEADER_CONNECTION:
      if ("close".equalsIgnoreCase(value))
        _request.killKeepalive();
      return true;

    case HEADER_CONTENT_TYPE:
      _request.getResponseFacade().setContentType(value);
      return true;

    case HEADER_CONTENT_LENGTH:
      // server/05a8
      // php/164v
      _contentLength = Long.parseLong(value.trim());
      return true;

    case HEADER_DATE:
      return true;

    case HEADER_SERVER:
      return false;

    default:
      return false;
    }
  }

  public void removeHeader(String key)
  {
    ArrayList<String> keys = _headerKeys;
    ArrayList<String> values = _headerValues;

    for (int i = keys.size() - 1; i >= 0; i--) {
      String oldKey = keys.get(i);

      if (oldKey.equalsIgnoreCase(key)) {
        keys.remove(i);
        values.remove(i);
        return;
      }
    }
  }

  /**
   * Convenience for setting an integer header.  An old header with the
   * same name will be replaced.
   *
   * @param name the header name.
   * @param value an integer to be converted to a string for the header.
   */
  public void setIntHeader(String name, int value)
  {
    _cb.clear();
    _cb.append(value);
    setHeader(name, _cb.toString());
  }

  /**
   * Convenience for adding an integer header.  If an old header already
   * exists, both will be sent to the browser.
   *
   * @param key the header name.
   * @param value an integer to be converted to a string for the header.
   */
  public void addIntHeader(String key, int value)
  {
    _cb.clear();
    _cb.append(value);
    addHeader(key, _cb.toString());
  }

  /**
   * Convenience for setting a date header.  An old header with the
   * same name will be replaced.
   *
   * @param name the header name.
   * @param value an time in milliseconds to be converted to a date string.
   */
  public void setDateHeader(String name, long value)
  {
    _calendar.setGMTTime(value);

    setHeader(name, _calendar.printDate());
  }


  /**
   * Convenience for adding a date header.  If an old header with the
   * same name exists, both will be displayed.
   *
   * @param key the header name.
   * @param value an time in milliseconds to be converted to a date string.
   */
  public void addDateHeader(String key, long value)
  {
    _calendar.setGMTTime(value);

    addHeader(key, _calendar.printDate());
  }

  public ArrayList<String> getHeaderKeys()
  {
    return _headerKeys;
  }

  public ArrayList<String> getHeaderValues()
  {
    return _headerValues;
  }

  public Collection<String> getHeaders(String name)
  {
    ArrayList<String> headers = new ArrayList<String>();

    for (int i = 0; i < _headerKeys.size(); i++) {
      String key = _headerKeys.get(i);

      if (key.equals(name))
        headers.add(_headerValues.get(i));
    }

    return headers;
  }

  public Collection<String> getHeaderNames()
  {
    return new HashSet<String>(_headerKeys);
  }

  public ArrayList<String> getFooterKeys()
  {
    return _footerKeys;
  }

  public ArrayList<String> getFooterValues()
  {
    return _footerValues;
  }

  /**
   * Sets the content length of the result.  In general, Resin will handle
   * the content length, but for things like long downloads adding the
   * length will give a valuable hint to the browser.
   *
   * @param length the length of the content.
   */
  public void setContentLength(int length)
  {
    _contentLength = length;
  }

  /**
   * Returns the value of the content-length header.
   */
  public long getContentLengthHeader()
  {
    return _contentLength;
  }

  /**
   * Sets a footer, replacing an already-existing footer
   *
   * @param key the header key to set.
   * @param value the header value to set.
   */
  public void setFooter(String key, String value)
  {
    if (value == null)
      throw new NullPointerException();

    int i = 0;
    boolean hasFooter = false;

    for (i = _footerKeys.size() - 1; i >= 0; i--) {
      String oldKey = _footerKeys.get(i);

      if (oldKey.equalsIgnoreCase(key)) {
        if (hasFooter) {
          _footerKeys.remove(i);
          _footerValues.remove(i);
        }
        else {
          hasFooter = true;

          _footerValues.set(i, value);
        }
      }
    }

    if (! hasFooter) {
      _footerKeys.add(key);
      _footerValues.add(value);
    }
  }

  /**
   * Adds a new footer.  If an old footer with that name exists,
   * both footers are output.
   *
   * @param key the footer key.
   * @param value the footer value.
   */
  public void addFooter(String key, String value)
  {
    if (setSpecial(key, value))
      return;

    _footerKeys.add(key);
    _footerValues.add(value);
  }

  protected boolean hasFooter()
  {
    return _footerKeys.size() > 0;
  }

  /**
   * Gets the response stream.
   */
  protected AbstractResponseStream getResponseStream()
  {
    return _responseStream;
  }

  protected ServletOutputStreamImpl getResponseOutputStream()
  {
    return _responseOutputStream;
  }

  protected ResponseWriter getResponsePrintWriter()
  {
    return _responsePrintWriter;
  }

  /**
   * Returns true if some data has been sent to the browser.
   */
  public boolean isCommitted()
  {
    if (_responseStream.isCommitted())
      return true;

    // server/05a7
    if (_contentLength > 0
        && _contentLength <= _responseStream.getContentLength()) {
      return true;
    }

    return false;
  }

  protected void reset()
  {
    _headerKeys.clear();
    _headerValues.clear();

    _contentLength = -1;
  }

  /**
   * Returns the number of bytes sent to the output.
   */
  public int getContentLength()
  {
    return _responseStream.getContentLength();
  }

  /**
   * Returns true if the headers have been written.
   */
  public boolean isHeaderWritten()
  {
    return _isHeaderWritten;
  }

  /**
   * Returns true if the headers have been written.
   */
  public void setHeaderWritten(boolean isWritten)
  {
    _isHeaderWritten = isWritten;
  }

  /**
   * Writes the continue
   */
  final void writeContinue()
    throws IOException
  {
    if (! isHeaderWritten()) {
      // writeContinueInt(_rawWrite);
      // _rawWrite.flush();

      writeContinueInt();
    }
  }

  /**
   * Writes the continue
   */
  protected void writeContinueInt()
    throws IOException
  {
  }

  /**
   * Writes the headers to the stream.  Called prior to the first flush
   * of data.
   *
   * @param os browser stream.
   * @param length length of the response if known, or -1 is unknown.
   * @return true if the content is chunked.
   */
  public final boolean writeHeaders(int length)
    throws IOException
  {
    if (isHeaderWritten())
      return false;

    HttpServletRequestImpl req = _request.getRequestFacade();
    HttpServletResponseImpl res = _request.getResponseFacade();

    if (res == null)
      return false;

    _isHeaderWritten = true;
    boolean isHead = false;

    if (_request.getMethod().equals("HEAD")) {
      isHead = true;
      _responseStream.setHead();
    }

    WebApp webApp = _request.getWebApp();

    int statusCode = res.getStatus();

    int majorCode = statusCode / 100;

    if (webApp != null) {
      if (majorCode == 5)
        webApp.addStatus500();
    }

    if (req != null) {
      HttpSession session = req.getMemorySession();
      if (session instanceof SessionImpl)
        ((SessionImpl) session).saveBeforeHeaders();

      res.addServletCookie(webApp);
    }

    return writeHeadersInt(length, isHead);
  }

  abstract protected boolean writeHeadersInt(int length,
                                             boolean isHead)
    throws IOException;

  /**
   * Fills the response for a cookie
   *
   * @param cb result buffer to contain the generated string
   * @param cookie the cookie
   * @param date the current date
   * @param version the cookies version
   */
  public boolean fillCookie(CharBuffer cb, Cookie cookie,
                            long date, int version,
                            boolean isCookie2)
  {
    // How to deal with quoted values?  Old browsers can't deal with
    // the quotes.

    cb.clear();
    cb.append(cookie.getName());
    if (isCookie2) {
      cb.append("=\"");
      cb.append(cookie.getValue());
      cb.append("\"");
    }
    else {
      cb.append("=");
      cb.append(cookie.getValue());
    }

    String domain = cookie.getDomain();
    if (domain != null && ! domain.equals("")) {
      if (isCookie2) {
        cb.append("; Domain=");

        cb.append('"');
        cb.append(domain);
        cb.append('"');
      }
      else {
        cb.append("; domain=");
        cb.append(domain);
      }
    }

    String path = cookie.getPath();
    if (path != null && ! path.equals("")) {
      if (isCookie2) {
        cb.append("; Path=");

        cb.append('"');
        cb.append(path);
        cb.append('"');
      }
      else {
        // Caps from TCK test
        if (version > 0)
          cb.append("; Path=");
        else
          cb.append("; path=");
        cb.append(path);
      }
    }

    if (cookie.getSecure()) {
      if (version > 0)
        cb.append("; Secure");
      else
        cb.append("; secure");
    }

    int maxAge = cookie.getMaxAge();
    if (version > 0) {
      if (maxAge >= 0) {
        cb.append("; Max-Age=");
        cb.append(maxAge);
      }

      cb.append("; Version=");
      cb.append(version);

      if (cookie.getComment() == null) {
      }
      else if (isCookie2) {
        cb.append("; Comment=\"");
        cb.append(cookie.getComment());
        cb.append("\"");
      }
      else {
        cb.append("; Comment=");
        cb.append(cookie.getComment());
      }

      if (cookie instanceof CookieImpl) {
        CookieImpl extCookie = (CookieImpl) cookie;
        String port = extCookie.getPort();

        if (port != null && isCookie2) {
          cb.append("; Port=\"");
          cb.append(port);
          cb.append("\"");
        }
      }
    }

    if (isCookie2) {
    }
    else if (maxAge == 0) {
      cb.append("; expires=Thu, 01-Dec-1994 16:00:00 GMT");
    }
    else if (maxAge >= 0) {
      _calendar.setGMTTime(date + 1000L * (long) maxAge);
      cb.append("; expires=");
      cb.append(_calendar.format("%a, %d-%b-%Y %H:%M:%S GMT"));
    }

    WebApp app = _request.getWebApp();
    if (app.getCookieHttpOnly() || app.getSessionManager().isCookieHttpOnly()) {
      cb.append("; HttpOnly");
    }

    return true;
  }

  /**
   * Closes the request, called from web-app for early close.
   */
  public void close()
    throws IOException
  {
    // server/125i
    if (! _request.isSuspend()) {
      finishInvocation(true);

      // finishRequest(true);
    }
  }

  /**
   * Complete the invocation.  Flushes the streams, completes caching
   * and writes the appropriate logs.
   */
  public void finishInvocation() throws IOException
  {
    // server/1960, server/1l11
    // finishInvocation(false);
    boolean isClose = ! _request.isSuspend();

    finishInvocation(isClose);
  }

  /**
   * Complete the invocation.  Flushes the streams, completes caching
   * and writes the appropriate logs.
   */
  public void finishRequest() throws IOException
  {
    finishRequest(false);
  }

  /**
   * Complete the request.  Flushes the streams, completes caching
   * and writes the appropriate logs.
   *
   * @param isClose true if the response should be flushed.
   */
  private void finishInvocation(boolean isClose)
    throws IOException
  {
    if (_isClosed)
      return;

    try {
      /* XXX:
      if (_statusCode == SC_NOT_MODIFIED && _request.isInitial()) {
        handleNotModified(_isTopCache);
      }
      if (_statusCode == SC_NOT_MODIFIED) {
        boolean isTopCache = true;

        handleNotModified(isTopCache);
      }
      */
      // server/137p
      HttpServletResponseImpl response = _request.getResponseFacade();
      if (response != null
          && response.getStatus() == HttpServletResponse.SC_NOT_MODIFIED) {
        response.handleNotModified();
      }

      if (_responseStream == null) {
      }
      else if (isClose) {
        _responseStream.close();
        finishResponseStream(isClose);
      }
      /*
      else if (_responseStream != _originalResponseStream) {
        _responseStream.finish();
      }
      */
      else {
        _responseStream.flush();
        finishResponseStream(isClose);
      }

      /*
      if (_rawWrite != null) {
        _rawWrite.flushBuffer();
      }
      */
    } catch (ClientDisconnectException e) {
      _request.killKeepalive();
      _isClientDisconnect = true;

      if (isIgnoreClientDisconnect())
        log.fine(e.toString());
      else
        throw e;
    } catch (IOException e) {
      _request.killKeepalive();
      _isClientDisconnect = true;

      throw e;
    }
  }

  protected void finishResponseStream(boolean isClose)
    throws IOException
  {
  }

  /**
   * Complete the request.  Flushes the streams, completes caching
   * and writes the appropriate logs.
   *
   * @param isClose true if the response should be flushed.
   */
  private void finishRequest(boolean isClose) throws IOException
  {
    if (_isClosed)
      return;

    try {
      _bufferStore = null;

      AbstractHttpRequest request = _request;

      try {
        request.skip();
      } catch (BadRequestException e) {
        log.warning(e.toString());
        log.log(Level.FINE, e.toString(), e);
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      _isClosed = true;

      // server/0506
      // _responseStream.close();
      /*
    } catch (ClientDisconnectException e) {
      _request.killKeepalive();
      _isClientDisconnect = true;

      if (isIgnoreClientDisconnect())
        log.fine(e.toString());
      else
        throw e;
    } catch (IOException e) {
      _request.killKeepalive();
      _isClientDisconnect = true;

      throw e;
      */
    } finally {
      _isClosed = true;
    }
  }

  protected void free()
  {
  }

  static {
    _headerCodes = new CaseInsensitiveIntMap();
    _headerCodes.put("cache-control", HEADER_CACHE_CONTROL);
    _headerCodes.put("connection", HEADER_CONNECTION);
    _headerCodes.put("content-type", HEADER_CONTENT_TYPE);
    _headerCodes.put("content-length", HEADER_CONTENT_LENGTH);
    _headerCodes.put("date", HEADER_DATE);
    _headerCodes.put("server", HEADER_SERVER);
  }
}
