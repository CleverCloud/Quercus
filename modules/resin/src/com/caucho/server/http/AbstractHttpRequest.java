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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.security.Principal;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.network.listen.SocketLinkDuplexController;
import com.caucho.network.listen.SocketLinkDuplexListener;
import com.caucho.network.listen.TcpSocketLink;
import com.caucho.security.SecurityContextProvider;
import com.caucho.server.cluster.Server;
import com.caucho.server.dispatch.DispatchServer;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.webapp.ErrorPageManager;
import com.caucho.server.webapp.RequestDispatcherImpl;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.Alarm;
import com.caucho.util.CaseInsensitiveIntMap;
import com.caucho.util.CharBuffer;
import com.caucho.util.CharSegment;
import com.caucho.util.HashMapImpl;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.util.NullEnumeration;
import com.caucho.util.QDate;
import com.caucho.util.StringCharCursor;
import com.caucho.vfs.BufferedReaderAdapter;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Abstract request implementing methods common to the different
 * request implementations.
 */
public abstract class AbstractHttpRequest
  implements SecurityContextProvider, ProtocolConnection
{
  private static final Logger log
    = Logger.getLogger(AbstractHttpRequest.class.getName());

  private static final L10N L = new L10N(AbstractHttpRequest.class);

  protected static final CaseInsensitiveIntMap _headerCodes;

  public static final String REQUEST_URI = "javax.servlet.include.request_uri";
  public static final String CONTEXT_PATH = "javax.servlet.include.context_path";
  public static final String SERVLET_PATH = "javax.servlet.include.servlet_path";
  public static final String PATH_INFO = "javax.servlet.include.path_info";
  public static final String QUERY_STRING = "javax.servlet.include.query_string";

  public static final String STATUS_CODE = "javax.servlet.error.status_code";
  public static final String EXCEPTION_TYPE = "javax.servlet.error.exception_type";
  public static final String MESSAGE = "javax.servlet.error.message";
  public static final String EXCEPTION = "javax.servlet.error.exception";
  public static final String ERROR_URI = "javax.servlet.error.request_uri";
  public static final String SERVLET_NAME = "javax.servlet.error.servlet_name";

  public static final String JSP_EXCEPTION = "javax.servlet.jsp.jspException";

  public static final String SHUTDOWN = "com.caucho.shutdown";

  private static final char []CONNECTION = "connection".toCharArray();
  private static final char []COOKIE = "cookie".toCharArray();
  private static final char []CONTENT_LENGTH = "content-length".toCharArray();
  private static final char []EXPECT = "expect".toCharArray();
  private static final char []HOST = "host".toCharArray();

  private static final char []CONTINUE_100 = "100-continue".toCharArray();
  private static final char []CLOSE = "close".toCharArray();

  private static final boolean []TOKEN;
  private static final boolean []VALUE;

  private static final Cookie []NULL_COOKIES = new Cookie[0];
  
  private static final LruCache<CharBuffer,String> _nameCache
    = new LruCache<CharBuffer,String>(1024);

  private final Server _server;

  private final SocketLink _conn;
  private final TcpSocketLink _tcpConn;

  private final AbstractHttpResponse _response;

  private final InvocationKey _invocationKey = new InvocationKey();

  // Connection stream
  private final ReadStream _rawRead;
  // Stream for reading post contents
  private final ReadStream _readStream;

  private final ArrayList<Cookie> _cookies = new ArrayList<Cookie>();

  private final ArrayList<Locale> _locales = new ArrayList<Locale>();

  // Servlet input stream for post contents
  private final ServletInputStreamImpl _is = new ServletInputStreamImpl();
  // Reader for post contents
  private final BufferedReaderAdapter _bufferedReader;

  private final Form _formParser = new Form();
  private final HashMapImpl<String,String[]> _form
    = new HashMapImpl<String,String[]>();

  private final ErrorPageManager _errorManager = new ErrorPageManager(null);

  // Efficient date class for printing date headers
  private final QDate _calendar = new QDate();
  private final CharBuffer _cbName = new CharBuffer();
  private final CharBuffer _cbValue = new CharBuffer();
  private final CharBuffer _cb = new CharBuffer();

  private HttpBufferStore _httpBuffer;

  private HttpServletRequestImpl _requestFacade;
  private HttpServletResponseImpl _responseFacade;

  private long _startTime;

  protected CharSegment _hostHeader;
  protected boolean _expect100Continue;

  private long _contentLength;
  // True if the post stream has been initialized
  protected boolean _hasReadStream;
  // character incoding for a Post
  private String _readEncoding;

  /**
   * Create a new Request.  Because the actual initialization occurs with
   * the start() method, this just allocates statics.
   *
   * @param server the parent server
   */
  protected AbstractHttpRequest(Server server, SocketLink conn)
  {
    _server = server;

    _conn = conn;

    if (conn != null)
      _rawRead = conn.getReadStream();
    else
      _rawRead = null;

    if (conn instanceof TcpSocketLink)
      _tcpConn = (TcpSocketLink) conn;
    else
      _tcpConn = null;

    _readStream = new ReadStream();
    _readStream.setReuseBuffer(true);

    _bufferedReader = new BufferedReaderAdapter(_readStream);

    _response = createResponse();
  }

  public Server getServer()
  {
    return _server;
  }

  abstract protected AbstractHttpResponse createResponse();

  protected AbstractHttpResponse getAbstractHttpResponse()
  {
    return _response;
  }

  /**
   * Initialization.
   */
  public void init()
  {
  }

  /**
   * Returns the connection.
   */
  public final SocketLink getConnection()
  {
    return _conn;
  }

  public final int getConnectionId()
  {
    return _conn.getId();
  }

  /**
   * returns the dispatch server.
   */
  public final DispatchServer getDispatchServer()
  {
    return _server;
  }

  protected final CharBuffer getCharBuffer()
  {
    return _cb;
  }

  /**
   * Called when the connection starts
   */
  @Override
  public void onStartConnection()
  {
  }

  /**
   * Prepare the Request object for a new request.
   *
   * @param httpBuffer the raw connection stream
   */
  protected void startRequest(HttpBufferStore httpBuffer)
    throws IOException
  {
    _httpBuffer = httpBuffer;

    _hostHeader = null;
    _expect100Continue = false;

    _cookies.clear();

    _contentLength = -1;

    _hasReadStream = false;

    _locales.clear();

    _readEncoding = null;

    _requestFacade = new HttpServletRequestImpl(this);
    _responseFacade = _requestFacade.getResponse();

    _response.startRequest(httpBuffer);

    _startTime = -1;
  }

  protected void clearRequest()
  {
    _requestFacade = null;
    _responseFacade = null;
  }

  /**
   * Returns true if a request has been set
   */
  public boolean hasRequest()
  {
    return _requestFacade != null;
  }

  /**
   * Returns the http buffer store
   */
  final HttpBufferStore getHttpBufferStore()
  {
    return _httpBuffer;
  }

  public WriteStream getRawWrite()
  {
    return _conn.getWriteStream();
  }

  public abstract byte []getUriBuffer();

  public abstract int getUriLength();

  /**
   * Returns true if client disconnects should be ignored.
   */
  public boolean isIgnoreClientDisconnect()
  {
    // server/183c

    WebApp webApp = getWebApp();

    if (webApp != null)
      return webApp.isIgnoreClientDisconnect();
    else
      return true;
  }

  protected WebApp getWebApp()
  {
    if (_requestFacade != null)
      return _requestFacade.getWebApp();
    else
      return null;
  }

  public StringBuffer getRequestURL()
  {
    HttpServletRequestImpl request = getRequestFacade();

    if (request != null)
      return request.getRequestURL();
    else
      return null;
  }

  public String getRequestURI()
  {
    HttpServletRequestImpl request = getRequestFacade();

    if (request != null)
      return request.getRequestURI();
    else
      return null;
  }

  @Override
  public String getProtocolRequestURL()
  {
    HttpServletRequestImpl request = getRequestFacade();

    if (request != null)
      return request.getRequestURL().toString();
    else
      return null;
  }

  /**
   * Returns true if the client has disconnected
   */
  public boolean isClientDisconnect()
  {
    return _response.isClientDisconnect();
  }

  /**
   * Sets the client disconnect
   */
  public void clientDisconnect()
  {
    if (_tcpConn != null)
      _tcpConn.close();
  }

  public final HttpServletRequestImpl getRequestFacade()
  {
    return _requestFacade;
  }

  public final HttpServletResponseImpl getResponseFacade()
  {
    return _responseFacade;
  }

  /**
   * Returns the response for this request.
   */
  public AbstractHttpResponse getResponse()
  {
    return _response;
  }

  /**
   * Returns the local server name.
   */
  public String getServerName()
  {
    String host = _conn.getVirtualHost();

    /*
    if (host == null && _invocation != null)
      host = _invocation.getHostName();
    */

    CharSequence rawHost;
    if (host == null && (rawHost = getHost()) != null) {
      if (rawHost instanceof CharSegment) {
        CharSegment cb = (CharSegment) rawHost;

        char []buffer = cb.getBuffer();
        int offset = cb.getOffset();
        int length = cb.getLength();

        for (int i = length - 1; i >= 0; i--) {
          char ch = buffer[i + offset];

          if ('A' <= ch && ch <= 'Z')
            buffer[i + offset] = (char) (ch + 'a' - 'A');
        }

        host = new String(buffer, offset, length);
      }
      else
        return rawHost.toString().toLowerCase();
    }

    if (host == null) {
      InetAddress addr = _conn.getLocalAddress();
      return addr.getHostName();
    }

    int p1 = host.lastIndexOf('/');
    if (p1 < 0)
      p1 = 0;

    int p = host.lastIndexOf(':');
    if (p >= 0 && p1 < p)
      return host.substring(p1, p);
    else
      return host;
  }

  protected CharSequence getHost()
  {
    return null;
  }

  /**
   * Returns the server's port.
   */
  public int getServerPort()
  {
    String host = _conn.getVirtualHost();

    CharSequence rawHost;
    if (host == null && (rawHost = getHost()) != null) {
      int length = rawHost.length();
      int i;

      for (i = length - 1; i >= 0; i--) {
        if (rawHost.charAt(i) == ':') {
          int port = 0;

          for (i++; i < length; i++) {
            char ch = rawHost.charAt(i);

            if ('0' <= ch && ch <= '9')
              port = 10 * port + ch - '0';
          }

          return port;
        }
      }

      // server/0521 vs server/052o
      // because of proxies, need to use the host header,
      // not the actual port
      return isSecure() ? 443 : 80;
    }

    if (host == null)
      return _conn.getLocalPort();

    int p1 = host.lastIndexOf(':');

    if (p1 < 0)
      return isSecure() ? 443 : 80;
    else {
      int length = host.length();
      int port = 0;

      for (int i = p1 + 1; i < length; i++) {
        char ch = host.charAt(i);

        if ('0' <= ch && ch <= '9')
          port = 10 * port + ch - '0';
      }

      return port;
    }
  }

  /**
   * Returns the local port.
   */
  public int getLocalPort()
  {
    return _conn.getLocalPort();
  }

  /**
   * Returns the server's address.
   */
  public String getLocalHost()
  {
    return _conn.getLocalHost();
  }

  public String getRemoteAddr()
  {
    return _conn.getRemoteHost();
  }

  public int printRemoteAddr(byte []buffer, int offset)
    throws IOException
  {
    int len = _conn.getRemoteAddress(buffer, offset, buffer.length - offset);

    return offset + len;
  }

  public String getRemoteHost()
  {
    return _conn.getRemoteHost();
  }

  /**
   * Returns the local port.
   */
  public int getRemotePort()
  {
    return _conn.getRemotePort();
  }

  /**
   * Returns the request's scheme.
   */
  public String getScheme()
  {
    return isSecure() ? "https" : "http";
  }

  abstract public String getProtocol();

  abstract public String getMethod();

  /**
   * Returns the named header.
   *
   * @param key the header key
   */
  abstract public String getHeader(String key);

  /**
   * Returns the number of headers.
   */
  public int getHeaderSize()
  {
    return -1;
  }

  /**
   * Returns the header key
   */
  public CharSegment getHeaderKey(int index)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the header value
   */
  public CharSegment getHeaderValue(int index)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Fills the result with the header values as
   * CharSegment values.  Most implementations will
   * implement this directly.
   *
   * @param name the header name
   */
  public CharSegment getHeaderBuffer(String name)
  {
    String value = getHeader(name);

    if (value != null)
      return new CharBuffer(value);
    else
      return null;
  }

  /**
   * Enumerates the header keys
   */
  abstract public Enumeration<String> getHeaderNames();

  /**
   * Sets the header.  setHeader is used for
   * Resin's caching to simulate If-None-Match.
   */
  public void setHeader(String key, String value)
  {
  }

  /**
   * Adds the header, checking for known values.
   */
  protected boolean addHeaderInt(char []keyBuf, int keyOff, int keyLen,
                                 CharSegment value)
  {
    if (keyLen < 4)
      return true;

    int key1 = keyBuf[keyOff];
    switch (key1) {
    case 'c':
    case 'C':
      if (keyLen == CONNECTION.length
          && match(keyBuf, keyOff, keyLen, CONNECTION)) {
        if (match(value.getBuffer(), value.getOffset(), value.getLength(),
                  CLOSE)) {
          handleConnectionClose();
        }
      }
      else if (keyLen == COOKIE.length
               && match(keyBuf, keyOff, keyLen, COOKIE)) {
        fillCookie(_cookies, value);
      }
      else if (keyLen == CONTENT_LENGTH.length
               && match(keyBuf, keyOff, keyLen, CONTENT_LENGTH)) {
        setContentLength(value);
      }

      return true;

    case 'e':
    case 'E':
      if (match(keyBuf, keyOff, keyLen, EXPECT)) {
        if (match(value.getBuffer(), value.getOffset(), value.getLength(),
                  CONTINUE_100)) {
          _expect100Continue = true;
          return false;
        }
      }

      return true;

    case 'h':
    case 'H':
      if (match(keyBuf, keyOff, keyLen, HOST)) {
        _hostHeader = value;
      }
      return true;

    default:
      return true;
    }
  }

  protected void setContentLength(CharSegment value)
  {
    int contentLength = 0;
    int ch;
    int i = 0;

    int length = value.length();
    for (;
         i < length && (ch = value.charAt(i)) >= '0' && ch <= '9';
         i++) {
      contentLength = 10 * contentLength + ch - '0';
    }

    if (i > 0)
      _contentLength = contentLength;
  }

  /**
   * Called for a connection: close
   */
  protected void handleConnectionClose()
  {
    SocketLink conn = _conn;

    if (conn != null)
      conn.killKeepalive();
  }

  /**
   * Matches case insensitively, with the second normalized to lower case.
   */
  private boolean match(char []a, int aOff, int aLength, char []b)
  {
    int bLength = b.length;

    if (aLength != bLength)
      return false;

    for (int i = aLength - 1; i >= 0; i--) {
      char chA = a[aOff + i];
      char chB = b[i];

      if (chA != chB && chA + 'a' - 'A' != chB) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns an enumeration of the headers for the named attribute.
   *
   * @param name the header name
   */
  public Enumeration<String> getHeaders(String name)
  {
    String value = getHeader(name);
    if (value == null)
      return NullEnumeration.create();

    ArrayList<String> list = new ArrayList<String>();
    list.add(value);

    return Collections.enumeration(list);
  }

  /**
   * Fills the result with a list of the header values as
   * CharSegment values.  Most implementations will
   * implement this directly.
   *
   * @param name the header name
   * @param resultList the resulting buffer
   */
  public void getHeaderBuffers(String name, ArrayList<CharSegment> resultList)
  {
    String value = getHeader(name);

    if (value != null)
      resultList.add(new CharBuffer(value));
  }

  /**
   * Returns the named header, converted to an integer.
   *
   * @param key the header key.
   *
   * @return the value of the header as an integer.
   */
  public int getIntHeader(String key)
  {
    CharSegment value = getHeaderBuffer(key);

    if (value == null)
      return -1;

    int len = value.length();
    if (len == 0)
      throw new NumberFormatException(value.toString());

    int iValue = 0;
    int i = 0;
    int ch = value.charAt(i);
    int sign = 1;
    if (ch == '+') {
      if (i + 1 < len)
        ch = value.charAt(++i);
      else
        throw new NumberFormatException(value.toString());
    } else if (ch == '-') {
      sign = -1;
      if (i + 1 < len)
        ch = value.charAt(++i);
      else
        throw new NumberFormatException(value.toString());
    }

    for (; i < len && (ch = value.charAt(i)) >= '0' && ch <= '9'; i++)
      iValue = 10 * iValue + ch - '0';

    if (i < len)
      throw new NumberFormatException(value.toString());

    return sign * iValue;
  }

  /**
   * Returns a header interpreted as a date.
   *
   * @param key the header key.
   *
   * @return the value of the header as an integer.
   */
  public long getDateHeader(String key)
  {
    String value = getHeader(key);
    if (value == null)
      return -1;

    long date = -1;
    try {
      date = _calendar.parseDate(value);

      if (date == Long.MAX_VALUE)
        throw new IllegalArgumentException("getDateHeader(" + value + ")");

      return date;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Returns the content length of a post.
   */
  public int getContentLength()
  {
    return (int) _contentLength;
  }

  /**
   * Returns the content length of a post.
   */
  public long getLongContentLength()
  {
    return _contentLength;
  }

  /**
   * Returns the content-length of a post.
   */
  public String getContentType()
  {
    return getHeader("Content-Type");
  }

  /**
   * Returns the content-length of a post.
   */
  public CharSegment getContentTypeBuffer()
  {
    return getHeaderBuffer("Content-Type");
  }

  /**
   * Returns the character encoding of a post.
   */
  public String getCharacterEncoding()
  {
    if (_readEncoding != null)
      return _readEncoding;

    CharSegment value = getHeaderBuffer("Content-Type");

    if (value == null)
      return null;

    int i = value.indexOf("charset");
    if (i < 0)
      return null;

    int len = value.length();
    for (i += 7; i < len && Character.isWhitespace(value.charAt(i)); i++) {
    }

    if (i >= len || value.charAt(i) != '=')
      return null;

    for (i++; i < len && Character.isWhitespace(value.charAt(i)); i++) {
    }

    if (i >= len)
      return null;

    char end = value.charAt(i);
    if (end == '"') {
      int tail;
      for (tail = ++i; tail < len; tail++) {
        if (value.charAt(tail) == end)
          break;
      }

      _readEncoding = Encoding.getMimeName(value.substring(i, tail));

      return _readEncoding;
    }

    int tail;
    for (tail = i; tail < len; tail++) {
      if (Character.isWhitespace(value.charAt(tail))
          || value.charAt(tail) == ';')
        break;
    }

    _readEncoding = Encoding.getMimeName(value.substring(i, tail));

    return _readEncoding;
  }

  /**
   * Sets the character encoding of a post.
   */
  public void setCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    // server/122k (tck)

    if (_hasReadStream)
      return;

    _readEncoding = encoding;

    try {
      // server/122d (tck)
      //if (_hasReadStream)

      _readStream.setEncoding(_readEncoding);
    } catch (UnsupportedEncodingException e) {
      throw e;
    } catch (java.nio.charset.UnsupportedCharsetException e) {
      throw new UnsupportedEncodingException(e.getMessage());
    }
  }

  /**
   * Returns the cookies from the browser
   */
  public Cookie []getCookies()
  {
    return fillCookies();

    /*
    // The page varies depending on the presense of any cookies
    setVaryCookie(null);

    if (_cookiesIn == null)
      fillCookies();

    // If any cookies actually exist, the page is not anonymous
    if (_cookiesIn != null && _cookiesIn.length > 0)
      setHasCookie();

    if (_cookiesIn == null || _cookiesIn.length == 0)
      return null;
    else
      return _cookiesIn;
    */
  }

  /**
   * Parses cookie information from the cookie headers.
   */
  Cookie []fillCookies()
  {
    int size = _cookies.size();

    if (size > 0) {
      Cookie []cookiesIn = new Cookie[size];

      for (int i = size - 1; i >= 0; i--)
        cookiesIn[i] = _cookies.get(i);

      return cookiesIn;
    }
    else {
      return NULL_COOKIES;
    }
  }

  /**
   * Parses a single cookie
   *
   * @param cookies the array of cookies read
   * @param rawCookie the input for the cookie
   */
  private void fillCookie(ArrayList<Cookie> cookies, CharSegment rawCookie)
  {
    char []buf = rawCookie.getBuffer();
    int j = rawCookie.getOffset();
    int end = j + rawCookie.length();
    int version = 0;
    Cookie cookie = null;

    while (j < end) {
      char ch = 0;

      CharBuffer cbName = _cbName;
      CharBuffer cbValue = _cbValue;

      cbName.clear();
      cbValue.clear();

      for (;
           j < end && ((ch = buf[j]) == ' ' || ch == ';' || ch ==',');
           j++) {
      }

      if (end <= j)
        break;

      boolean isSpecial = false;
      if (buf[j] == '$') {
        isSpecial = true;
        j++;
      }

      for (; j < end; j++) {
        ch = buf[j];
        if (ch < 128 && TOKEN[ch])
          cbName.append(ch);
        else
          break;
      }

      for (; j < end && (ch = buf[j]) == ' '; j++) {
      }

      if (end <= j)
        break;
      else if (ch == ';' || ch == ',') {
        try {
          cookie = new Cookie(cbName.toString(), "");
          cookie.setVersion(version);
          _cookies.add(cookie);
          // some clients can send bogus cookies
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
        continue;
      }
      else if (ch != '=') {
        for (; j < end && (ch = buf[j]) != ';'; j++) {
        }
        continue;
      }

      j++;

      for (; j < end && (ch = buf[j]) == ' '; j++) {
      }

      if (ch == '"') {
        for (j++; j < end; j++) {
          ch = buf[j];
          if (ch == '"')
            break;
          cbValue.append(ch);
        }
        j++;
      }
      else {
        for (; j < end; j++) {
          ch = buf[j];
          if (ch < 128 && VALUE[ch])
            cbValue.append(ch);
          else
            break;
        }
      }

      if (! isSpecial) {
        if (cbName.length() == 0)
          log.warning("bad cookie: " + rawCookie);
        else {
          cookie = new Cookie(toName(cbName), cbValue.toString());
          cookie.setVersion(version);
          _cookies.add(cookie);
        }
      }
      else if (cookie == null) {
        if (cbName.matchesIgnoreCase("Version"))
          version = cbValue.charAt(0) - '0';
      }
      else if (cbName.matchesIgnoreCase("Version"))
        cookie.setVersion(cbValue.charAt(0) - '0');
      else if (cbName.matchesIgnoreCase("Domain"))
        cookie.setDomain(cbValue.toString());
      else if (cbName.matchesIgnoreCase("Path"))
        cookie.setPath(cbValue.toString());
    }
  }
  
  private String toName(CharBuffer cb)
  {
    String value = _nameCache.get(cb);
    
    if (value == null) {
      value = cb.toString();
      
      cb = new CharBuffer(value);
      
      _nameCache.put(cb, value);
    }
    
    return value;
  }

  /**
   * For SSL connections, use the SSL identifier.
   */
  public String findSessionIdFromConnection()
  {
    return null;
  }

  /**
   * Returns true if the transport is secure.
   */
  public boolean isTransportSecure()
  {
    return _conn.isSecure();
  }

  /**
   * Returns the requests underlying read stream, e.g. the post stream.
   */
  public ReadStream getStream()
    throws IOException
  {
    return getStream(true);
  }

  /**
   * Returns the requests underlying read stream, e.g. the post stream.
   */
  public ReadStream getStream(boolean isReader)
    throws IOException
  {
    if (! _hasReadStream) {
      _hasReadStream = true;

      initStream(_readStream, _rawRead);

      if (isReader) {
        // Encoding is based on getCharacterEncoding.
        // getReader needs the encoding.
        String charEncoding = getCharacterEncoding();
        String javaEncoding = Encoding.getJavaName(charEncoding);
        _readStream.setEncoding(javaEncoding);
      }

      if (_expect100Continue) {
        _expect100Continue = false;
        _response.writeContinue();
      }
    }

    return _readStream;
  }

  public final ReadStream getRawRead()
  {
    return _rawRead;
  }

  public final ReadStream getReadStream()
  {
    return _readStream;
  }

  /**
   * Returns the raw read buffer.
   */
  public byte []getRawReadBuffer()
  {
    return _rawRead.getBuffer();
  }

  public int getAvailable()
    throws IOException
  {
    return _readStream.getAvailable();
  }

  protected void skip()
    throws IOException
  {
    try {
      if (! _hasReadStream) {
        if (! initStream(_readStream, _rawRead))
          return;

        _hasReadStream = true;
      }

      while ((_readStream.skip(8192) > 0)) {
      }
    } catch (ClientDisconnectException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Initialize the read stream from the raw stream.
   */
  abstract protected boolean initStream(ReadStream readStream,
                                        ReadStream rawStream)
    throws IOException;

  /**
   * Returns the raw input stream.
   */
  public ReadStream getRawInput()
  {
    throw new UnsupportedOperationException(L.l("raw mode is not supported in this configuration"));
  }

  /**
   * Returns a stream for reading POST data.
   */
  public final ServletInputStream getInputStream()
    throws IOException
  {
    ReadStream stream = getStream(false);

    _is.init(stream);

    return _is;
  }

  /**
   * Returns a Reader for the POST contents
   */
  public final BufferedReader getReader()
    throws IOException
  {
    try {
      // bufferedReader is just an adapter to get the signature right.
      _bufferedReader.init(getStream(true));

      return _bufferedReader;
    } catch (java.nio.charset.UnsupportedCharsetException e) {
      throw new UnsupportedEncodingException(e.getMessage());
    }
  }

  protected void initAttributes(HttpServletRequestImpl facade)
  {

  }

  /*
   * jsdk 2.2
   */

  public Locale getLocale()
  {
    fillLocales();

    return _locales.get(0);
  }

  public Enumeration<Locale> getLocales()
  {
    fillLocales();

    return Collections.enumeration(_locales);
  }

  /**
   * Fill the locale array from the request's headers.
   */
  private void fillLocales()
  {
    if (_locales.size() > 0)
      return;

    Enumeration<String> headers = getHeaders("Accept-Language");
    if (headers == null) {
      _locales.add(Locale.getDefault());
      return;
    }

    CharBuffer cb = _cb;
    while (headers.hasMoreElements()) {
      String header = headers.nextElement();
      StringCharCursor cursor = new StringCharCursor(header);

      while (cursor.current() != CharacterIterator.DONE) {
        char ch;
        for (; Character.isWhitespace(cursor.current()); cursor.next()) {
        }

        cb.clear();
        for (; (ch = cursor.current()) >= 'a' && ch <= 'z' ||
               ch >= 'A' && ch <= 'Z' ||
               ch >= '0' && ch <= '0';
             cursor.next()) {
          cb.append(cursor.current());
        }

        String language = cb.toString();
        String country = "";
        if (cursor.current() == '_' || cursor.current() == '-') {
          cb.clear();
          for (cursor.next();
               (ch = cursor.current()) >= 'a' && ch <= 'z' ||
               ch >= 'A' && ch <= 'Z' ||
               ch >= '0' && ch <= '9';
               cursor.next()) {
            cb.append(cursor.current());
          }
          country = cb.toString();
        }

        if (language.length() > 0) {
          Locale locale = new Locale(language, country);
          _locales.add(locale);
        }

        for (;
             cursor.current() != CharacterIterator.DONE && cursor.current() != ',';
             cursor.next()) {
        }
        cursor.next();
      }
    }

    if (_locales.size() == 0)
      _locales.add(Locale.getDefault());
  }

  //
  // security
  //

  /**
   * Returns true if the request is secure.
   */
  public boolean isSecure()
  {
    return _conn.isSecure();
  }

  public String runAs(String string)
  {
    if (_requestFacade != null)
      return _requestFacade.runAs(string);
    else
      return null;
  }

  public boolean isUserInRole(String role)
  {
    if (_requestFacade != null)
      return _requestFacade.isUserInRole(role);
    else
      return false;
  }

  public Principal getUserPrincipal()
  {
    if (_requestFacade != null)
      return _requestFacade.getUserPrincipal();
    else
      return null;
  }

  //
  // internal goodies
  //

  /**
   * Returns the date for the current request.
   */
  public final long getStartTime()
  {
    return _startTime;
    /*
    if (_tcpConn != null)
      return _tcpConn.getRequestStartTime();
    else
      return _startTime;
      */
  }

  /**
   * Returns the log buffer.
   */
  public final byte []getLogBuffer()
  {
    return _httpBuffer.getLogBuffer();
  }

  protected Invocation getInvocation(CharSequence host,
                                     byte []uri,
                                     int uriLength)
    throws IOException
  {
    _invocationKey.init(isSecure(),
                        host, getServerPort(),
                        uri, uriLength);

    Invocation invocation = _server.getInvocation(_invocationKey);

    if (invocation != null)
      return invocation.getRequestInvocation(_requestFacade);

    invocation = _server.createInvocation();
    invocation.setSecure(isSecure());

    if (host != null) {
      String hostName = host.toString().toLowerCase();

      invocation.setHost(hostName);
      invocation.setPort(getServerPort());

      // Default host name if the host doesn't have a canonical
      // name
      int p = hostName.lastIndexOf(':');
      int q = hostName.lastIndexOf(']');
      if (p > 0 && q < p)
        invocation.setHostName(hostName.substring(0, p));
      else
        invocation.setHostName(hostName);
    }

    return buildInvocation(invocation, uri, uriLength);
  }

  protected Invocation buildInvocation(Invocation invocation,
                                       byte []uri,
                                       int uriLength)
    throws IOException
  {
    InvocationDecoder decoder = _server.getInvocationDecoder();

    decoder.splitQueryAndUnescape(invocation, uri, uriLength);

    if (_server.isModified()) {
      _server.logModified(log);

      _requestFacade.setInvocation(invocation);
      if (_server instanceof Server)
        invocation.setWebApp(((Server) _server).getDefaultWebApp());

      HttpServletResponse res = _responseFacade;
      res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

      restartServer();

      return null;
    }

    invocation = _server.buildInvocation(_invocationKey.clone(),
                                         invocation);

    return invocation.getRequestInvocation(_requestFacade);
  }

  /**
   * Handles a comet-style resume.
   *
   * @return true if the connection should stay open (keepalive)
   */
  public boolean handleResume()
    throws IOException
  {
    try {
      startInvocation();

      HttpServletRequestImpl request = getRequestFacade();

      /*
      if (! request.isAsyncStarted())
        return false;
        */

      if (request == null)
        return false;

      AsyncContextImpl asyncContext = request.getAsyncContext();

      ServletContext webApp = asyncContext.getDispatchContext();
      String url = asyncContext.getDispatchPath();

      if (url != null) {
        if (webApp == null)
          webApp = getWebApp();

        RequestDispatcherImpl disp
          = (RequestDispatcherImpl) webApp.getRequestDispatcher(url);

        if (disp != null) {
          disp.dispatchResume(getRequestFacade(), getResponseFacade());

          return isSuspend();
        }
      }

      Invocation invocation = getRequestFacade().getInvocation();

      invocation.service(getRequestFacade(), getResponseFacade());
    } catch (ClientDisconnectException e) {
      _responseFacade.killCache();

      throw e;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      if (_responseFacade != null)
        _responseFacade.killCache();
      killKeepalive();

      return false;
    } finally {
      finishInvocation();

      if (! isSuspend()) {
        finishRequest();
      }
   }

    if (log.isLoggable(Level.FINE)) {
      log.fine(dbgId() +
               (isKeepalive() ? "keepalive" : "no-keepalive"));
    }

    return isSuspend();
  }

  WebApp getAsyncDispatchWebApp()
  {
    // XXX:
    throw new UnsupportedOperationException();
  }

  String getAsyncDispatchUrl()
  {
    // XXX:
    throw new UnsupportedOperationException();
  }

  /**
   * Starts duplex mode.
   */
  public SocketLinkDuplexController startDuplex(SocketLinkDuplexListener handler)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  protected void sendRequestError(Throwable e)
    throws IOException
  {
    try {
      getErrorManager().sendServletError(e, _requestFacade, _responseFacade);
    } catch (ClientDisconnectException e1) {
      throw e1;
    } catch (Throwable e1) {
      log.log(Level.FINE, e1.toString(), e1);
    }

    if (_server instanceof Server) {
      WebApp webApp = ((Server) _server).getDefaultWebApp();

      if (webApp != null && getRequestFacade() != null) {
        webApp.accessLog(getRequestFacade(), getResponseFacade());
      }
    }
  }

  /**
   * Returns the default error manager
   */
  protected ErrorPageManager getErrorManager()
  {
    Server server = (Server) _server;

    WebApp webApp = server.getWebApp("error.resin", 80, "/");

    if (webApp != null)
      return webApp.getErrorPageManager();
    else
      return _errorManager;
  }

  /**
   * Returns the depth of the request calls.
   */
  public int getRequestDepth(int depth)
  {
    return depth + 1;
  }

  public int getRequestDepth()
  {
    return 0;
  }

  /**
   * Kills the keepalive.
   */
  public void killKeepalive()
  {
    SocketLink conn = _conn;

    if (conn != null)
      conn.killKeepalive();

    /*
    ConnectionController controller = _conn.getController();
    if (controller != null)
      controller.close();
    */
  }

  /**
   * Returns true if the keepalive is active.
   */
  protected boolean isKeepalive()
  {
    return _conn != null && _conn.isKeepaliveAllocated();
  }

  public boolean isCometActive()
  {
    return _tcpConn != null && _tcpConn.isCometActive();
  }

  public boolean isSuspend()
  {
    // return _tcpConn != null && (_tcpConn.isSuspend() || _tcpConn.isDuplex());
    return _tcpConn != null && (_tcpConn.isCometActive() || _tcpConn.isDuplex());
  }

  public boolean isDuplex()
  {
    return _tcpConn != null && _tcpConn.isDuplex();
  }

  /**
   * Returns true if a keepalive has been allocated for the request.
   *
   * The keepalives are preallocated at the start of the request to keep
   * the connection state machine simple.
   */
  public boolean isKeepaliveAllowed()
  {
    SocketLink conn = _conn;

    if (conn != null)
      return conn.isKeepaliveAllocated();
    else
      return true;
  }

  protected HashMapImpl<String,String[]> getForm()
  {
    _form.clear();

    return _form;
  }

  protected Form getFormParser()
  {
    return _formParser;
  }

  /**
   * Restarts the server.
   */
  protected void restartServer()
  {
    _server.update();
  }

  /**
   * Prepare the Request object for a new request.
   *
   */
  protected void startInvocation()
    throws IOException
  {
    _startTime = Alarm.getCurrentTime();

    _response.startInvocation();
  }

  /**
   * Cleans up at the end of the invocation
   */
  public void finishInvocation()
  {
    // to avoid finish when no request server/05b0
    /*
    if (_startTime < 0)
      return;
      */

    try {
      _response.finishInvocation();
    } catch (IOException e) {
      log.finer(e.toString());
    }
  }

  /**
   * Cleans up at the end of the request
   */
  public void finishRequest()
    throws IOException
  {
    try {
      HttpServletRequestImpl requestFacade = _requestFacade;
      _requestFacade = null;
      _responseFacade = null;

      if (requestFacade != null)
        requestFacade.finishRequest();

      // server/0219, but must be freed for GC
      _response.finishRequest();

      cleanup();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _requestFacade = null;

      _responseFacade = null;

      HttpBufferStore httpBuffer = _httpBuffer;
      _httpBuffer = null;

      if (_tcpConn != null) {
        _tcpConn.finishRequest();
      }

      if (httpBuffer != null)
        HttpBufferStore.free(httpBuffer);
    }
  }

  public void cleanup()
  {
    HttpServletRequestImpl requestFacade = getRequestFacade();

    if (requestFacade != null)
      requestFacade.cleanup();

    if (_form != null)
      _form.clear();
    _cookies.clear();
  }

  /**
   * Called by server shutdown to kill any active threads
   */
  public void shutdown()
  {
  }

  protected String dbgId()
  {
    return "Tcp[" + _conn.getId() + "] ";
  }

  static {
    _headerCodes = new CaseInsensitiveIntMap();

    TOKEN = new boolean[256];
    VALUE = new boolean[256];

    for (int i = 0; i < 256; i++) {
      TOKEN[i] = true;
    }

    for (int i = 0; i < 32; i++) {
      TOKEN[i] = false;
    }

    for (int i = 127; i < 256; i++) {
      TOKEN[i] = false;
    }

    //TOKEN['('] = false;
    //TOKEN[')'] = false;
    //TOKEN['<'] = false;
    //TOKEN['>'] = false;
    //TOKEN['@'] = false;
    TOKEN[','] = false;
    TOKEN[';'] = false;
    //TOKEN[':'] = false;
    TOKEN['\\'] = false;
    TOKEN['"'] = false;
    //TOKEN['/'] = false;
    //TOKEN['['] = false;
    //TOKEN[']'] = false;
    //TOKEN['?'] = false;
    TOKEN['='] = false;
    //TOKEN['{'] = false;
    //TOKEN['}'] = false;
    TOKEN[' '] = false;

    System.arraycopy(TOKEN, 0, VALUE, 0, TOKEN.length);

    VALUE['='] = true;
  }
}
