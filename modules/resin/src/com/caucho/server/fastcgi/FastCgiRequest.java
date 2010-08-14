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

package com.caucho.server.fastcgi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.server.cluster.Server;
import com.caucho.server.dispatch.BadRequestException;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.http.AbstractHttpRequest;
import com.caucho.server.http.HttpBufferStore;
import com.caucho.server.http.InvocationKey;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.CharBuffer;
import com.caucho.util.CharSegment;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StreamImpl;
import com.caucho.vfs.WriteStream;

/**
 * Handles a new request from a FastCGI connection.
 */
public class FastCgiRequest extends AbstractHttpRequest
  implements ProtocolConnection
{
  private static final Logger log
    = Logger.getLogger(FastCgiRequest.class.getName());

  private static final int FCGI_HEADER_LEN = 8;
  private static final int FCGI_VERSION_1 = 1;

  private static final int FCGI_BEGIN_REQUEST = 1;
  private static final int FCGI_ABORT_REQUEST = 2;
  private static final int FCGI_END_REQUEST = 3;
  private static final int FCGI_PARAMS = 4;
  private static final int FCGI_STDIN = 5;
  private static final int FCGI_STDOUT = 6;
  private static final int FCGI_STDERR = 7;
  private static final int FCGI_DATA = 8;
  private static final int FCGI_GET_VALUES = 9;
  private static final int FCGI_GET_VALUES_RESULT = 10;
  private static final int FCGI_UNKNOWN_TYPE = 11;
  private static final int FCGI_MAXTYPE = FCGI_UNKNOWN_TYPE;

  private static final int FCGI_NULL_REQUEST_ID = 0;

  private static final int FCGI_KEEP_CONN = 1;

  private static final int FCGI_RESPONDER = 1;
  private static final int FCGI_AUTHORIZER = 2;
  private static final int FCGI_FILTER = 3;

  // protocolStatus for FCGI_EndRequestBody
  private static final int FCGI_REQUEST_COMPLETE = 0;
  private static final int FCGI_CANT_MPX_CONN = 1;
  private static final int FCGI_OVERLOADED = 2;
  private static final int FCGI_UNKNOWN_ROLE = 3;

  private static final int HTTP_1_1 = 0x11;

  private static final int LEN_CONTENT_LENGTH = 14;

  private static final int HU_CONTENT_LENGTH = (14 << 8) | 'C';
  private static final int HL_CONTENT_LENGTH = (14 << 8) | 'c';

  private static final int LEN_CONTENT_TYPE = 12;

  private static final byte []REQUEST_METHOD = "REQUEST_METHOD".getBytes();
  private static final int HU_REQUEST_METHOD = (14 << 8) | 'R';
  private static final int HL_REQUEST_METHOD = (14 << 8) | 'r';

  private static final byte []REQUEST_URI = "REQUEST_URI".getBytes();
  private static final int HU_REQUEST_URI = (11 << 8) | 'R';
  private static final int HL_REQUEST_URI = (11 << 8) | 'r';

  private static final byte []SERVER_PROTOCOL = "SERVER_PROTOCOL".getBytes();
  private static final int HU_SERVER_PROTOCOL = (15 << 8) | 'S';
  private static final int HL_SERVER_PROTOCOL = (15 << 8) | 's';

  private static final int LEN_SCRIPT_NAME = 11;

  private CharBuffer _method;       // "GET"
  private String _methodString;

  private CharBuffer _uriHost;      // www.caucho.com:8080
  private CharSequence _host;
  private CharBuffer _hostBuffer = new CharBuffer();
  private CharBuffer _queryString = new CharBuffer();

  private byte []_uri;              // "/path/test.jsp/Junk?query=7"
  private int _uriLength;

  private int _urlLengthMax = 8192;

  private byte []_keyBuffer = new byte[256];

  private CharBuffer _protocol;     // "HTTP/1.0"
  private int _version;

  private final InvocationKey _invocationKey = new InvocationKey();

  private char []_headerBuffer;
  private int _headerOffset;
  private boolean _isSecure;

  private CharSegment []_headerKeys;
  private CharSegment []_headerValues;
  private int _headerSize;

  private boolean _hasRequest;

  /*
  private ChunkedInputStream _chunkedInputStream = new ChunkedInputStream();
  private ContentLengthStream _contentLengthStream = new ContentLengthStream();
  */

  // write stream from the connection
  private WriteStream _rawWrite;
  // servlet write stream
  private WriteStream _writeStream;

  private ServletFilter _filter = new ServletFilter(this);

  private boolean _initAttributes;
  private byte []_buffer = new byte[16];

  /**
   * Creates a new HttpRequest.  New connections reuse the request.
   *
   * @param server the owning server.
   */
  public FastCgiRequest(Server server, SocketLink conn)
  {
    super(server, conn);

    _method = new CharBuffer();
    _uriHost = new CharBuffer();
    _protocol = new CharBuffer();

    _rawWrite = conn.getWriteStream();
    getWriteStream();
  }

  @Override
  public FastCgiResponse createResponse()
  {
    return new FastCgiResponse(this, getWriteStream());
  }

  WriteStream getWriteStream()
  {
    if (_writeStream == null) {
      _writeStream = new WriteStream();
      _writeStream.setReuseBuffer(true);
    }

    return _writeStream;
  }

  /**
   * Return true if the request waits for a read before beginning.
   */
  public final boolean isWaitForRead()
  {
    return true;
  }

  /**
   * Returns true if the request exists
   */
  @Override
  public boolean hasRequest()
  {
    return _hasRequest;
  }

  /**
   * Handles a new HTTP request.
   *
   * <p>Note: ClientDisconnectException must be rethrown to
   * the caller.
   *
   * @return true if the connection should stay open (keepalive)
   */
  public boolean handleRequest()
    throws IOException
  {
    _hasRequest = false;

    SocketLink conn = getConnection();
    Server server = getServer();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(server.getClassLoader());

      HttpBufferStore httpBuffer = HttpBufferStore.allocate((Server) server);

      startRequest(httpBuffer);
      startInvocation();

      ReadStream is = getRawRead();
      WriteStream os = getRawWrite();

      _filter.init(is, os);
      _writeStream.init(_filter);
      // _writeStream.setWritePrefix(3);

      try {
        _hasRequest = false;

        while (readPacket(is)) {
        }

        if (! _hasRequest) {
          if (log.isLoggable(Level.FINE))
            log.fine(dbgId() + "read timeout");

          return false;
        }

        startInvocation();

        _isSecure = conn.isSecure() || conn.getLocalPort() == 443;

        /*
        if (_protocol.length() == 0)
          _protocol.append("HTTP/0.9");

        if (log.isLoggable(Level.FINE)) {
          log.fine(dbgId() + _method + " " +
                   new String(_uri, 0, _uriLength) + " " + _protocol);
          log.fine(dbgId() + "Remote-IP: " + _conn.getRemoteHost() + ":" + _conn.getRemotePort());
        }

        parseHeaders(_rawRead);

        if (getVersion() >= HTTP_1_1 && isForce10()) {
          _protocol.clear();
          _protocol.append("HTTP/1.0");
          _version = HTTP_1_0;
        }
        */
      } catch (ClientDisconnectException e) {
        throw e;
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);

        throw new BadRequestException(String.valueOf(e), e);
      }

      CharSequence host = getHost();

      String ipHost = conn.getVirtualHost();
      if (ipHost != null)
        host = ipHost;

      _invocationKey.init(_isSecure,
                          host, conn.getLocalPort(),
                          _uri, _uriLength);

      Invocation invocation = getInvocation(host);

      if (invocation == null)
        return false;

      getRequestFacade().setInvocation(invocation);

      startInvocation();

      invocation.service(getRequestFacade(), getResponseFacade());
    } catch (ClientDisconnectException e) {
      // XXX: _response.killCache();

      throw e;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      // XXX: _response.killCache();
      killKeepalive();

      /*
      try {
        getErrorManager().sendServletError(e, this, _response);
      } catch (ClientDisconnectException e1) {
        throw e1;
      } catch (Throwable e1) {
        log.log(Level.FINE, e1.toString(), e1);
      }
      */

      WebApp webApp = server.getDefaultWebApp();
      if (webApp != null)
        webApp.accessLog(getRequestFacade(), getResponseFacade());

      return false;
    } finally {
      finishInvocation();

      if (! isSuspend()) {
        finishRequest();
      }

      thread.setContextClassLoader(oldLoader);
    }

    if (log.isLoggable(Level.FINE)) {
      log.fine(dbgId() +
               (isKeepalive() ? "keepalive" : "no-keepalive"));
    }

    return isKeepalive();
  }

  private Invocation getInvocation(CharSequence host)
    throws Throwable
  {
    Server server = getServer();
    Invocation invocation = server.getInvocation(_invocationKey);

    if (invocation == null) {
      invocation = server.createInvocation();
      invocation.setSecure(_isSecure);

      if (host != null) {
        String hostName = host.toString().toLowerCase();

        invocation.setHost(hostName);
        invocation.setPort(getConnection().getLocalPort());

        // Default host name if the host doesn't have a canonical
        // name
        int p = hostName.indexOf(':');
        if (p > 0)
          invocation.setHostName(hostName.substring(0, p));
        else
          invocation.setHostName(hostName);
      }

      InvocationDecoder decoder = server.getInvocationDecoder();

      decoder.splitQueryAndUnescape(invocation, _uri, _uriLength);

      /* XXX: common to AbstractHttpRequest
      if (_server.isModified()) {
        _server.logModified(log);

        _invocation = invocation;
        if (_server instanceof Server)
          _invocation.setWebApp(((Server) _server).getDefaultWebApp());

        restartServer();

        return null;
      }
      */

      invocation = server.buildInvocation(_invocationKey.clone(),
                                           invocation);
    }

    invocation = invocation.getRequestInvocation(getRequestFacade());

    return invocation;
  }

  public int getVersion()
  {
    return HTTP_1_1;
  }

  /**
   * Returns the byte buffer containing the request URI
   */
  public byte []getUriBuffer()
  {
    return _uri;
  }

  /**
   * Returns the length of the request URI
   */
  public int getUriLength()
  {
    return _uriLength;
  }

  /**
   * Returns the header.
   */
  public String getMethod()
  {
    if (_methodString == null) {
      CharSegment cb = getMethodBuffer();
      if (cb.length() == 0) {
        _methodString = "GET";
        return _methodString;
      }

      /*
      switch (cb.charAt(0)) {
      case 'G':
        _methodString = cb.equals(_getCb) ? "GET" : cb.toString();
        break;

      case 'H':
        _methodString = cb.equals(_headCb) ? "HEAD" : cb.toString();
        break;

      case 'P':
        _methodString = cb.equals(_postCb) ? "POST" : cb.toString();
        break;

      default:
        _methodString = cb.toString();
      }
      */

      _methodString = cb.toString();
    }

    return _methodString;

  }

  /**
   * Returns the protocol.
   */
  public String getProtocol()
  {
    if (_protocol.getLength() > 0)
      return _protocol.toString();
    else
      return "HTTP/1.1";
  }

  /**
   * Returns a buffer containing the request method.
   */
  public CharSegment getMethodBuffer()
  {
    return _method;
  }

  /**
   * Adds a new header.  Used only by the caching to simulate
   * If-Modified-Since.
   *
   * @param key the key of the new header
   * @param value the value for the new header
   */
  public void setHeader(String key, String value)
  {
    int tail;

    if (_headerSize > 0) {
      tail = (_headerValues[_headerSize - 1].getOffset() +
              _headerValues[_headerSize - 1].getLength());
    }
    else
      tail = 0;

    char []headerBuffer = _headerBuffer;
    for (int i = key.length() - 1; i >= 0; i--)
      headerBuffer[tail + i] = key.charAt(i);

    _headerKeys[_headerSize].init(headerBuffer, tail, key.length());

    tail += key.length();

    for (int i = value.length() - 1; i >= 0; i--)
      headerBuffer[tail + i] = value.charAt(i);

    _headerValues[_headerSize].init(headerBuffer, tail, value.length());
    _headerSize++;
  }

  /**
   * Returns the number of headers.
   */
  @Override
  public int getHeaderSize()
  {
    return _headerSize;
  }

  /**
   * Returns the header key
   */
  @Override
  public CharSegment getHeaderKey(int index)
  {
    return _headerKeys[index];
  }

  /**
   * Returns the header value
   */
  @Override
  public CharSegment getHeaderValue(int index)
  {
    return _headerValues[index];
  }

  /**
   * Returns the header.
   */
  public String getHeader(String key)
  {
    CharSegment buf = getHeaderBuffer(key);
    if (buf != null)
      return buf.toString();
    else
      return null;
  }

  /**
   * Returns the matching header.
   *
   * @param testBuf header key
   * @param length length of the key.
   */
  public CharSegment getHeaderBuffer(char []testBuf, int length)
  {
    char []keyBuf = _headerBuffer;
    CharSegment []headerKeys = _headerKeys;

    for (int i = _headerSize - 1; i >= 0; i--) {
      CharSegment key = headerKeys[i];

      if (key.length() != length)
        continue;

      int offset = key.getOffset();
      int j;
      for (j = length - 1; j >= 0; j--) {
        char a = testBuf[j];
        char b = keyBuf[offset + j];
        if (a == b)
          continue;

        if (a >= 'A' && a <= 'Z')
          a += 'a' - 'A';
        if (b >= 'A' && b <= 'Z')
          b += 'a' - 'A';
        if (a != b)
          break;
      }

      if (j < 0)
        return _headerValues[i];
    }

    return null;
  }

  /**
   * Returns the header value for the key, returned as a CharSegment.
   */
  public CharSegment getHeaderBuffer(String key)
  {
    int i = matchNextHeader(0, key);

    if (i >= 0)
      return _headerValues[i];
    else
      return null;
  }

  /**
   * Fills an ArrayList with the header values matching the key.
   *
   * @param values ArrayList which will contain the maching values.
   * @param key the header key to select.
   */
  public void getHeaderBuffers(String key, ArrayList<CharSegment> values)
  {
    int i = -1;
    while ((i = matchNextHeader(i + 1, key)) >= 0)
      values.add(_headerValues[i]);
  }

  /**
   * Return an enumeration of headers matching a key.
   *
   * @param key the header key to match.
   * @return the enumeration of the headers.
   */
  public Enumeration getHeaders(String key)
  {
    ArrayList<String> values = new ArrayList<String>();
    int i = -1;
    while ((i = matchNextHeader(i + 1, key)) >= 0)
      values.add(_headerValues[i].toString());

    return Collections.enumeration(values);
  }

  /**
   * Returns the index of the next header matching the key.
   *
   * @param i header index to start search
   * @param key header key to match
   *
   * @return the index of the next header matching, or -1.
   */
  private int matchNextHeader(int i, String key)
  {
    int size = _headerSize;
    int length = key.length();

    char []keyBuf = _headerBuffer;

    for (; i < size; i++) {
      CharSegment header = _headerKeys[i];

      if (header.length() != length)
        continue;

      int offset = header.getOffset();

      int j;
      for (j = 0; j < length; j++) {
        char a = key.charAt(j);
        char b = keyBuf[offset + j];
        if (a == b)
          continue;

        if (a >= 'A' && a <= 'Z')
          a += 'a' - 'A';
        if (b >= 'A' && b <= 'Z')
          b += 'a' - 'A';
        if (a != b)
          break;
      }

      if (j == length)
        return i;
    }

    return -1;
  }

  /**
   * Returns an enumeration of all the header keys.
   */
  public Enumeration getHeaderNames()
  {
    ArrayList<String> names = new ArrayList<String>();

    for (int i = 0; i < _headerSize; i++) {
      CharSegment name = _headerKeys[i];

      int j;
      for (j = 0; j < names.size(); j++) {
        String oldName = names.get(j);
        if (name.matches(oldName))
          break;
      }
      if (j == names.size())
        names.add(j, name.toString());
    }

    return Collections.enumeration(names);
  }

  /**
   * Returns a stream for reading POST data.
   */
  public boolean initStream(ReadStream readStream, ReadStream rawRead)
    throws IOException
  {
    readStream.init(_filter, null);

    return true;
  }

  /**
   * Handles a comet-style resume.
   *
   * @return true if the connection should stay open (keepalive)
   */
  /*
  @Override
  public boolean handleResume()
    throws IOException
  {
    try {
      startInvocation();

      if (! isComet())
        return false;

      String url = _tcpConn.getCometPath();

      // servlet 3.0 spec defaults to suspend
      _tcpConn.suspend();

      if (url != null) {
        WebApp webApp = getWebApp();

        RequestDispatcherImpl disp
          = (RequestDispatcherImpl) webApp.getRequestDispatcher(url);

        if (disp != null) {
          disp.forwardResume(_requestFacade, _responseFacade);

          return isSuspend();
        }
      }

      _invocation.doResume(_requestFacade, _responseFacade);
    } catch (ClientDisconnectException e) {
      _response.killCache();

      throw e;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      // isResume = false;
      _response.killCache();
      killKeepalive();

      return false;
    } finally {
      finishInvocation();

      if (! isSuspend())
        finishRequest();
    }

    if (log.isLoggable(Level.FINE)) {
      log.fine(dbgId() +
               (isKeepalive() ? "keepalive" : "no-keepalive"));
    }

    return isSuspend();
  }
  */

  /**
   * Returns true for the top-level request, but false for any include()
   * or forward()
   */
  public boolean isTop()
  {
    return true;
  }

  protected boolean checkLogin()
  {
    return true;
  }

  /**
   * Clear the request variables in preparation for a new request.
   *
   * @param s the read stream for the request
   */
  @Override
  protected void startRequest(HttpBufferStore httpBuffer)
    throws IOException
  {
    super.startRequest(httpBuffer);

    _method.clear();
    _methodString = null;
    _protocol.clear();

    _uriLength = 0;
    _uri = httpBuffer.getUriBuffer();

    _uriHost.clear();
    _host = null;

    _headerSize = 0;
    _headerOffset = 0;
    _headerBuffer = httpBuffer.getHeaderBuffer();
    _headerKeys = httpBuffer.getHeaderKeys();
    _headerValues = httpBuffer.getHeaderValues();
    _initAttributes = false;
  }

  /**
   * Returns true for a secure connection.
   */
  public boolean isSecure()
  {
    return _isSecure;
  }

  /**
   * Read the first line of a request:
   *
   * GET [http://www.caucho.com[:80]]/path [HTTP/1.x]
   *
   * @return true if the request is valid
   */
  private boolean readPacket(ReadStream is)
    throws IOException
  {
    int version = is.read();
    int code = is.read();
    int id = (is.read() << 8) + is.read();
    int len = (is.read() << 8) + is.read();
    int pad = is.read();
    int reserved = is.read();

    if (reserved < 0) {
      // end of file
      return false;
    }

    if (version != FCGI_VERSION_1) {
      log.warning(this + " unexpected fastcgi version '" + version + "'");
      return false;
    }

    switch (code) {
    case FCGI_BEGIN_REQUEST:
      return readBeginRequest(is, id);

    case FCGI_PARAMS:
      return readParams(is, id, len, pad);

    case FCGI_STDIN:
      return readStdin(is, id, len, pad);

    default:
      log.warning(this + " unexpected fastcgi code '" + code + "'");
      return false;
    }

    /*
    int i = 0;


    byte []readBuffer = s.getBuffer();
    int readOffset = s.getOffset();
    int readLength = s.getLength();
    int ch;

    if (readOffset >= readLength) {
      try {
        if ((readLength = s.fillBuffer()) < 0)
          return false;
      } catch (InterruptedIOException e) {
        log.fine(dbgId() + "keepalive timeout");
        return false;
      }
      readOffset = 0;
    }
    ch = readBuffer[readOffset++];

    // conn.setAccessTime(getDate());

    // skip leading whitespace
    while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
      if (readOffset >= readLength) {
        if ((readLength = s.fillBuffer()) < 0)
          return false;

        readOffset = 0;
      }
      ch = readBuffer[readOffset++];
    }

    char []buffer = _method.getBuffer();
    int length = buffer.length;
    int offset = 0;

    // scan method
    while (true) {
      if (length <= offset) {
      }
      else if (ch >= 'a' && ch <= 'z')
        buffer[offset++] = ((char) (ch + 'A' - 'a'));
      else if (ch > ' ')
        buffer[offset++] = (char) ch;
      else
        break;

      if (readLength <= readOffset) {
        if ((readLength = s.fillBuffer()) < 0)
          return false;

        readOffset = 0;
      }
      ch = readBuffer[readOffset++];
    }

    _method.setLength(offset);

    // skip whitespace
    while (ch == ' ' || ch == '\t') {
      if (readOffset >= readLength) {
        if ((readLength = s.fillBuffer()) < 0)
          return false;

        readOffset = 0;
      }

      ch = readBuffer[readOffset++];
    }

    byte []uriBuffer = _uri;
    int uriLength = 0;

    // skip 'http:'
    if (ch != '/') {
      while (ch > ' ' && ch != '/') {
        if (readOffset >= readLength) {
          if ((readLength = s.fillBuffer()) < 0)
            return false;
          readOffset = 0;
        }
        ch = readBuffer[readOffset++];
      }

      if (readOffset >= readLength) {
        if ((readLength = s.fillBuffer()) < 0) {
          if (ch == '/') {
            uriBuffer[uriLength++] = (byte) ch;
            _uriLength = uriLength;
          }

          return true;
        }
        readOffset = 0;
      }

      int ch1 = readBuffer[readOffset++];

      if (ch1 != '/') {
        uriBuffer[uriLength++] = (byte) ch;
        ch = ch1;
      }
      else {
        // read host
        host:
        while (true) {
          if (readOffset >= readLength) {
            if ((readLength = s.fillBuffer()) < 0) {
              return true;
            }
            readOffset = 0;
          }
          ch = readBuffer[readOffset++];

          switch (ch) {
          case ' ': case '\t': case '\n': case '\r':
            break host;

          case '?':
            break host;

          case '/':
            break host;

          default:
            _uriHost.append((char) ch);
            break;
          }
        }
      }
    }

    // read URI
    uri:
    while (true) {
      switch (ch) {
      case ' ': case '\t': case '\n': case '\r':
        break uri;

      default:
        // There's no check for overrunning the length because
        // allowing resizing would allow a DOS memory attack and
        // also lets us save a bit of efficiency.
        uriBuffer[uriLength++] = (byte) ch;
        break;
      }

      if (readOffset >= readLength) {
        readOffset = 0;
        if ((readLength = s.fillBuffer()) < 0) {
          _uriLength = uriLength;
          return true;
        }
      }
      ch = readBuffer[readOffset++];
    }

    _uriLength = uriLength;

    // skip whitespace
    while (ch == ' ' || ch == '\t') {
      if (readOffset >= readLength) {
        readOffset = 0;
        if ((readLength = s.fillBuffer()) < 0)
          return true;
      }
      ch = readBuffer[readOffset++];
    }

    buffer = _protocol.getBuffer();
    length = buffer.length;
    offset = 0;
    // scan protocol
    while (ch != ' ' && ch != '\t' && ch != '\r' && ch != '\n') {
      if (offset >= length) {
      }
      else if (ch >= 'a' && ch <= 'z')
        buffer[offset++] = ((char) (ch + 'A' - 'a'));
      else
        buffer[offset++] = (char) ch;

      if (readOffset >= readLength) {
        readOffset = 0;
        if ((readLength = s.fillBuffer()) < 0) {
          _protocol.setLength(offset);
          return true;
        }
      }
      ch = readBuffer[readOffset++];
    }
    _protocol.setLength(offset);

    if (offset != 8) {
      _protocol.append("HTTP/0.9");
      _version = HTTP_0_9;
    }
    else if (buffer[7] == '1') // && _protocol.equals(_http11Cb))
      _version = HTTP_1_1;
    else if (buffer[7] == '0') // && _protocol.equals(_http10Cb))
      _version = HTTP_1_0;
    else
      _version = HTTP_0_9;

    // skip to end of line
    while (ch != '\n') {
      if (readOffset >= readLength) {
        if ((readLength = s.fillBuffer()) < 0)
          return true;
        readOffset = 0;
      }
      ch = readBuffer[readOffset++];
    }

    s.setOffset(readOffset);

    return true;
    */
  }

  /**
   * Read the first line of a request:
   *
   * GET [http://www.caucho.com[:80]]/path [HTTP/1.x]
   *
   * @return true if the request is valid
   */
  private boolean readBeginRequest(ReadStream is, int id)
    throws IOException
  {
    int role = (is.read() << 8) + is.read();
    int flags = is.read();
    is.skip(5);

    if (role != FCGI_RESPONDER) {
      log.warning(this + " does not support role=" + role);
      return false;
    }

    boolean isKeepConn = (flags & FCGI_KEEP_CONN) != 0;

    if (! isKeepConn) {
      killKeepalive();
    }

    return true;
  }

  /**
   * Read the parameters from a request.
   *
   * @return true if the request is valid
   */
  private boolean readParams(ReadStream is, int id, int len, int pad)
    throws IOException
  {
    if (len == 0) {
      // end of params
      return true;
    }

    long pos = is.getPosition();
    long end = pos + len;

    while (is.getPosition() < end) {
      int keyLen = is.read();
      int valueLen = is.read();

      is.readAll(_keyBuffer, 0, keyLen);

      readParam(is, _keyBuffer, keyLen, valueLen);
    }

    is.skip(pad);

    return true;
  }

  /**
   * Read the stdin
   *
   * @return true if the request is valid
   */
  private boolean readStdin(ReadStream is, int id, int len, int pad)
    throws IOException
  {
    if (len == 0)
      return false;

    _filter.setPending(len, pad);

    return false;
  }

  private void readParam(ReadStream is,
                         byte []key, int keyLength,
                         int valueLength)
    throws IOException
  {
    int ch = key[0];

    switch ((keyLength << 8) | ch) {
    case HU_REQUEST_URI:
    case HL_REQUEST_URI:
      if (isMatch(key, REQUEST_URI, keyLength)) {
        is.readAll(_uri, 0, valueLength);
        _uriLength = valueLength;
        _hasRequest = true;
        return;
      }
      break;

    case HU_REQUEST_METHOD:
    case HL_REQUEST_METHOD:
      if (isMatch(key, REQUEST_METHOD, keyLength)) {
        _method.setLength(valueLength);
        is.readAll(_method.getBuffer(), 0, valueLength);
        return;
      }
      break;

    case HU_SERVER_PROTOCOL:
    case HL_SERVER_PROTOCOL:
      if (isMatch(key, SERVER_PROTOCOL, keyLength)) {
        _protocol.setLength(valueLength);
        is.readAll(_protocol.getBuffer(), 0, valueLength);
        return;
      }
      break;
    }

    CharSegment headerKey = _headerKeys[_headerSize];
    CharSegment headerValue = _headerValues[_headerSize];
    char []headerBuffer = _headerBuffer;

    if (keyLength > 5
        && ch == 'H'
        && key[1] == 'T'
        && key[2] == 'T'
        && key[3] == 'P'
        && key[4] == '_') {

      int headerOffset = _headerOffset;

      for (int i = 5; i < keyLength; i++) {
        ch = (char) (key[i] & 0xff);

        if (ch == '_')
          ch = '-';

        headerBuffer[headerOffset++] = (char) ch;
      }

      headerKey.init(headerBuffer, _headerOffset, keyLength - 5);

      is.readAll(headerBuffer, headerOffset, valueLength);

      headerValue.init(headerBuffer, headerOffset, valueLength);

      _headerOffset = headerOffset + valueLength;

      _headerSize++;

      return;
    }

    is.skip(valueLength);

    if (log.isLoggable(Level.FINE))
      log.fine(this + " skipping " + new String(key, 0, keyLength));
  }

  private boolean isMatch(byte []bufferA, byte []bufferB, int length)
  {
    for (int i = length - 1; i >= 0; i--) {
      if (bufferA[i] != bufferB[i])
        return false;
    }

    return true;
  }

  /**
   * Returns the raw input stream.
   */
  public ReadStream getRawInput()
  {
    return getRawRead();
  }

  public final void onCloseConnection()
  {
  }

  /**
   * Cleans up at the end of the invocation
   */
  /*
  @Override
  public void finishRequest()
    throws IOException
  {
    super.finishRequest();

    skip();

    finishResponse();
  }
  */

  void writeTail()
    throws IOException
  {
    _writeStream.flushBuffer();

    int id = 1;

    byte []tempBuf = _buffer;

    tempBuf[0] = FCGI_VERSION_1;
    tempBuf[1] = FCGI_STDOUT;
    tempBuf[2] = (byte) (id >> 8);
    tempBuf[3] = (byte) (id);
    tempBuf[4] = (byte) 0;
    tempBuf[5] = (byte) 0;
    tempBuf[6] = 0;
    tempBuf[7] = 0;

    _rawWrite.write(tempBuf, 0, 8);

    tempBuf[0] = FCGI_VERSION_1;
    tempBuf[1] = FCGI_END_REQUEST;
    tempBuf[2] = (byte) (id >> 8);
    tempBuf[3] = (byte) (id);
    tempBuf[4] = (byte) 0;
    tempBuf[5] = (byte) 8;
    tempBuf[6] = 0;
    tempBuf[7] = 0;

    _rawWrite.write(tempBuf, 0, 8);

    int status = 0;
    tempBuf[0] = (byte) (status >> 24);
    tempBuf[1] = (byte) (status >> 16);
    tempBuf[2] = (byte) (status >> 8);
    tempBuf[3] = (byte) (status);
    tempBuf[4] = (byte) FCGI_REQUEST_COMPLETE;
    tempBuf[5] = 0;
    tempBuf[6] = 0;
    tempBuf[7] = 0;

    _rawWrite.write(tempBuf, 0, 8);
    _rawWrite.flush();
  }

  protected String dbgId()
  {
    String serverId = getServer().getServerId();
    int connId = getConnectionId();

    if ("".equals(serverId))
      return "FastCgi[" + connId + "] ";
    else
      return "FastCgi[" + serverId + ", " + connId + "] ";
  }

  public String toString()
  {
    String serverId = getServer().getServerId();
    int connId = getConnectionId();

    if ("".equals(serverId))
      return getClass().getSimpleName() + "[" + connId + "]";
    else {
      return (getClass().getSimpleName() + "[" + serverId
              + ", " + connId + "]");
    }
  }


  /**
   * Implements the protocol for data reads and writes.  Data from the
   * web server to the JVM must be acked, except for the first data.
   * Data back to the web server needs no ack.
   */
  static class ServletFilter extends StreamImpl {
    private FastCgiRequest _request;
    private ReadStream _is;
    private WriteStream _os;
    private byte []_buffer = new byte[16];
    private int _pendingData;
    private int _pad;
    private boolean _isClosed;
    private boolean _isClientClosed;

    ServletFilter(FastCgiRequest request)
    {
      _request = request;
    }

    void init(ReadStream nextRead, WriteStream nextWrite)
    {
      _is = nextRead;
      _os = nextWrite;
      _pendingData = 0;
      _isClosed = false;
      _isClientClosed = false;
    }

    void setPending(int pendingData, int pad)
    {
      _pendingData = pendingData;
      _pad = pad;
    }

    void setClientClosed(boolean isClientClosed)
    {
      _isClientClosed = isClientClosed;
    }

    @Override
    public boolean canRead()
    {
      return true;
    }

    @Override
    public int getAvailable()
    {
      return _pendingData;
    }

    /**
     * Reads available data.  If the data needs an ack, then do so.
     */
    @Override
    public int read(byte []buf, int offset, int length)
      throws IOException
    {
      if (_pendingData <= 0)
        return -1;

      int sublen = _pendingData;
      if (length < sublen)
        sublen = length;

      ReadStream is = _request.getRawRead();

      is.readAll(buf, offset, sublen);

      _pendingData -= sublen;

      if (_pendingData == 0) {
        if (_pad > 0)
          is.skip(_pad);

        _pad = 0;

        int version = is.read();
        int code = is.read();
        int id = (is.read() << 8) + is.read();
        _pendingData = (is.read() << 8) + is.read();
        _pad = is.read();
        int reserved = is.read();

        if (reserved < 0 || code != FCGI_STDIN)
          _pendingData = 0;
      }

      return sublen;
    }

    @Override
    public boolean canWrite()
    {
      return true;
    }

    /**
     * Send data back to the web server
     */
    @Override
    public void write(byte []buf, int offset, int length, boolean isEnd)
      throws IOException
    {
      if (log.isLoggable(Level.FINE)) {
        log.fine(_request.dbgId() + ":data " + length);

        if (log.isLoggable(Level.FINEST))
          log.finest(_request.dbgId() + "data <" + new String(buf, offset, length) + ">");
      }

      byte []tempBuf = _buffer;

      while (length > 0) {
        int sublen = length;

        if (32 * 1024 < sublen)
          sublen = 32 * 1024;

        int id = 1;

        tempBuf[0] = FCGI_VERSION_1;
        tempBuf[1] = FCGI_STDOUT;
        tempBuf[2] = (byte) (id >> 8);
        tempBuf[3] = (byte) (id);
        tempBuf[4] = (byte) (sublen >> 8);
        tempBuf[5] = (byte) sublen;
        tempBuf[6] = 0;
        tempBuf[7] = 0;

        _os.write(tempBuf, 0, 8);
        _os.write(buf, offset, sublen);

        length -= sublen;
        offset += sublen;
      }

    }

    @Override
    public void flush()
      throws IOException
    {
      if (! _request._hasRequest)
        return;

      if (log.isLoggable(Level.FINE))
        log.fine(_request.dbgId() + ":flush");

      _os.flush();
    }

    @Override
    public void close()
      throws IOException
    {
      if (_isClosed)
        return;

      _isClosed = true;

      if (_pendingData > 0) {
        _is.skip(_pendingData);
        _pendingData = 0;
      }

      boolean keepalive = _request.isKeepaliveAllowed();

      if (! _isClientClosed) {
        if (log.isLoggable(Level.FINE)) {
          if (keepalive)
            log.fine(_request.dbgId() + " quit channel");
          else
            log.fine(_request.dbgId() + " exit socket");
        }
      }

      if (keepalive)
        _os.flush();
      else
        _os.close();
      //nextRead.close();
    }
  }
}
