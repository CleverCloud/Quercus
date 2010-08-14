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
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.cloud.bam.BamService;
import com.caucho.cloud.hmtp.HmtpRequest;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.server.cluster.Server;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.http.AbstractHttpRequest;
import com.caucho.server.http.AbstractResponseStream;
import com.caucho.server.http.HttpBufferStore;
import com.caucho.server.http.HttpServletRequestImpl;
import com.caucho.server.http.HttpServletResponseImpl;
import com.caucho.server.webapp.ErrorPageManager;
import com.caucho.util.ByteBuffer;
import com.caucho.util.CharBuffer;
import com.caucho.util.CharSegment;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StreamImpl;
import com.caucho.vfs.WriteStream;

/**
 * Handles requests from a remote dispatcher.  For example, mod_caucho
 * and the IIS plugin use this protocol to talk to the backend.
 *
 * <p>Packets are straightforward:
 * <pre>code l2 l1 l0 contents</pre>
 * Where code is the code of the packet and l2-0 give 12 bits of length.
 *
 * <p>The protocol is designed to allow pipelining and buffering whenever
 * possible.  So most commands are not acked.  Data from the frontend (POST)
 * does need acks to prevent deadlocks at either end while waiting
 * for new data.
 *
 * <p>The overriding protocol is controlled by requests from the
 * frontend server.
 *
 * <p>A ping request:
 * <pre>
 * Frontend       Backend
 * CSE_PING
 * CSE_END
 *                CSE_END
 * <pre>
 *
 * <p>A GET request:
 * <pre>
 * Frontend       Backend
 * CSE_METHOD
 * ...
 * CSE_HEADER/CSE_VALUE
 * CSE_END
 *                CSE_DATA
 *                CSE_DATA
 *                CSE_END
 * <pre>
 *
 * <p>Short POST:
 * <pre>
 * Frontend       Backend
 * CSE_METHOD
 * ...
 * CSE_HEADER/CSE_VALUE
 * CSE_DATA
 * CSE_END
 *                CSE_DATA
 *                CSE_DATA
 *                CSE_END
 * <pre>
 *
 * <p>Long POST:
 * <pre>
 * Frontend       Backend
 * CSE_METHOD
 * ...
 * CSE_HEADER/CSE_VALUE
 * CSE_DATA
 *                CSE_DATA (optional)
 * CSE_DATA
 *                CSE_ACK
 *                CSE_DATA (optional)
 * CSE_DATA
 *                CSE_ACK
 * CSE_END
 *                CSE_DATA
 *                CSE_END
 * <pre>
 *
 */
public class HmuxRequest extends AbstractHttpRequest
  implements ProtocolConnection
{
  private static final Logger log
    = Logger.getLogger(HmuxRequest.class.getName());

  // HMUX channel control codes
  public static final int HMUX_CHANNEL =        'C';
  public static final int HMUX_ACK =            'A';
  public static final int HMUX_ERROR =          'E';
  public static final int HMUX_YIELD =          'Y';
  public static final int HMUX_QUIT =           'Q';
  public static final int HMUX_EXIT =           'X';

  public static final int HMUX_DATA =           'D';
  public static final int HMUX_URI =            'U';
  public static final int HMUX_STRING =         'S';
  public static final int HMUX_HEADER =         'H';
  public static final int HMUX_BINARY =         'B';
  public static final int HMUX_PROTOCOL =       'P';
  public static final int HMUX_META_HEADER =    'M';

  // The following are HTTP codes
  public static final int CSE_NULL =            '?';
  public static final int CSE_PATH_INFO =       'b';
  public static final int CSE_PROTOCOL =        'c';
  public static final int CSE_REMOTE_USER =     'd';
  public static final int CSE_QUERY_STRING =    'e';
  public static final int HMUX_FLUSH  =         'f';
  public static final int CSE_SERVER_PORT =     'g';
  public static final int CSE_REMOTE_HOST =     'h';
  public static final int CSE_REMOTE_ADDR =     'i';
  public static final int CSE_REMOTE_PORT =     'j';
  public static final int CSE_REAL_PATH =       'k';
  public static final int CSE_SCRIPT_FILENAME = 'l';
  public static final int HMUX_METHOD =         'm';
  public static final int CSE_AUTH_TYPE =       'n';
  public static final int CSE_URI =             'o';
  public static final int CSE_CONTENT_LENGTH =  'p';
  public static final int CSE_CONTENT_TYPE =    'q';
  public static final int CSE_IS_SECURE =       'r';
  public static final int HMUX_STATUS =         's';
  public static final int CSE_CLIENT_CERT =     't';
  public static final int CSE_SERVER_TYPE =     'u';
  public static final int HMUX_SERVER_NAME =    'v';

  public static final int CSE_SEND_HEADER =     'G';

  public static final int CSE_FLUSH =           'F';
  public static final int CSE_KEEPALIVE =       'K';
  public static final int CSE_END =             'Z';

  // other, specialized protocols
  public static final int CSE_QUERY =           'Q';
  public static final int CSE_PING =            'P';

  public static final int HMUX_TO_UNIDIR_HMTP = '7';
  public static final int HMUX_SWITCH_TO_HMTP = '8';
  public static final int HMUX_HMTP_OK        = '9';

  public static final int HMUX_CLUSTER_PROTOCOL = 0x101;
  public static final int HMUX_DISPATCH_PROTOCOL = 0x102;
  public static final int HMUX_JMS_PROTOCOL = 0x103;

  public enum ProtocolResult {
    QUIT,
    EXIT,
    YIELD
  };

  static final int HTTP_0_9 = 0x0009;
  static final int HTTP_1_0 = 0x0100;
  static final int HTTP_1_1 = 0x0101;

  private static final int HEADER_CAPACITY = 256;

  static final CharBuffer _getCb = new CharBuffer("GET");
  static final CharBuffer _headCb = new CharBuffer("HEAD");
  static final CharBuffer _postCb = new CharBuffer("POST");

  private final CharBuffer _method;   // "GET"
  private String _methodString;       // "GET"
  // private CharBuffer scheme;       // "http:"
  private CharBuffer _host;            // www.caucho.com
  private ByteBuffer _uri;             // "/path/test.jsp/Junk"
  private CharBuffer _protocol;        // "HTTP/1.0"
  private int _version;

  private CharBuffer _remoteAddr;
  private CharBuffer _remoteHost;
  private CharBuffer _serverName;
  private CharBuffer _serverPort;
  private CharBuffer _remotePort;

  private boolean _isSecure;
  private ByteBuffer _clientCert;

  private CharBuffer []_headerKeys;
  private CharBuffer []_headerValues;
  private int _headerSize;

  private int _serverType;

  // write stream from the connection
  private WriteStream _rawWrite;
  private int _bufferStartOffset;

  // StreamImpl to break reads and writes to the underlying protocol
  private ServletFilter _filter;
  private int _pendingData;

  private CharBuffer _cb1;
  private CharBuffer _cb2;
  private boolean _hasRequest;

  private Server _server;
  private AbstractClusterRequest _clusterRequest;
  private HmuxDispatchRequest _dispatchRequest;

  private HmtpRequest _hmtpRequest;
  private boolean _isHmtpRequest;

  private HmuxProtocol _hmuxProtocol;
  private ErrorPageManager _errorManager = new ErrorPageManager(null);

  public HmuxRequest(Server server,
                     SocketLink conn,
                     HmuxProtocol protocol)
  {
    super(server, conn);

    _server = server;

    if (server == null)
      throw new NullPointerException();

    _hmuxProtocol = protocol;

    _rawWrite = conn.getWriteStream();

    // XXX: response.setIgnoreClientDisconnect(server.getIgnoreClientDisconnect());

    // _server = Server.getCurrent();

    _dispatchRequest = new HmuxDispatchRequest(this);

    _uri = new ByteBuffer();

    _method = new CharBuffer();
    _host = new CharBuffer();
    _protocol = new CharBuffer();

    _headerKeys = new CharBuffer[HEADER_CAPACITY];
    _headerValues = new CharBuffer[_headerKeys.length];
    for (int i = 0; i < _headerKeys.length; i++) {
      _headerKeys[i] = new CharBuffer();
      _headerValues[i] = new CharBuffer();
    }

    _remoteHost = new CharBuffer();
    _remoteAddr = new CharBuffer();
    _serverName = new CharBuffer();
    _serverPort = new CharBuffer();
    _remotePort = new CharBuffer();

    _clientCert = new ByteBuffer();

    _cb1 = new CharBuffer();
    _cb2 = new CharBuffer();

    _filter = new ServletFilter();
    
    BamService bamService = BamService.getCurrent();
    
    _hmtpRequest = new HmtpRequest(conn, bamService);
  }

  @Override
  public HmuxResponse createResponse()
  {
    return new HmuxResponse(this, getConnection().getWriteStream());
  }

  @Override
  public boolean isWaitForRead()
  {
    return true;
  }

  /**
   * Returns true if a valid HTTP request has started.
   */
  @Override
  public boolean hasRequest()
  {
    return _hasRequest;
  }

  @Override
  public boolean isSuspend()
  {
    return super.isSuspend() || _isHmtpRequest;
  }


  public boolean handleRequest()
    throws IOException
  {
    try {
      if (_isHmtpRequest) {
        return _hmtpRequest.handleRequest();
      }
      else {
        return handleRequestImpl();
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    }
  }

  /**
   * Handles a new request.  Initializes the protocol handler and
   * the request streams.
   *
   * <p>Note: ClientDisconnectException must be rethrown to
   * the caller.
   */
  private boolean handleRequestImpl()
    throws IOException
  {
    // XXX: should be moved to TcpConnection
    Thread thread = Thread.currentThread();
    thread.setContextClassLoader(_server.getClassLoader());

    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + "start request");

    _filter.init(this, getRawRead(), getRawWrite());

    try {
      HttpBufferStore httpBuffer = HttpBufferStore.allocate((Server) _server);
      startRequest(httpBuffer);

      startInvocation();

      return handleInvocation();
    } finally {
      if (! _hasRequest)
        getResponse().setHeaderWritten(true);

      if (! _isHmtpRequest) {
        finishInvocation();
      }

      try {
        // server/0190
        finishRequest();
      } catch (ClientDisconnectException e) {
        throw e;
      } catch (Exception e) {
        killKeepalive();
        log.log(Level.FINE, dbgId() + e, e);
      }

      // cluster/6610 - hmtp mode doesn't close the stream.
      if (! _isHmtpRequest) {
        try {
          ReadStream is = getReadStream();

          is.setDisableClose(false);
          is.close();
        } catch (Exception e) {
          killKeepalive();
          log.log(Level.FINE, dbgId() + e, e);
        }
      }
    }
  }

  private boolean handleInvocation()
    throws IOException
  {
    try {
      _hasRequest = false;

      if (! scanHeaders() || ! getConnection().isPortActive()) {
        _hasRequest = false;
        killKeepalive();
        return false;
      }
      else if (! _hasRequest) {
        return true;
      }
    } catch (InterruptedIOException e) {
      killKeepalive();
      log.fine(dbgId() + "interrupted keepalive");
      return false;
    }

    if (_isSecure)
      getClientCertificate();

    _hasRequest = true;
    // setStartDate();

    Server server = getServer();

    if (server == null || server.isDestroyed()) {
      log.fine(dbgId() + "server is closed");

      ReadStream is = getRawRead();

      try {
        is.setDisableClose(false);
        is.close();
      } catch (Exception e) {
      }

      return false;
    }

    _filter.setPending(_pendingData);

    try {
      if (_method.getLength() == 0)
        throw new RuntimeException("HTTP protocol exception, expected method");

      Invocation invocation;

      invocation = getInvocation(getHost(),
                                 _uri.getBuffer(), _uri.getLength());

      getRequestFacade().setInvocation(invocation);

      startInvocation();

      if (_server.isPreview() && ! "resin.admin".equals(getHost())) {
        return sendBusyResponse();
      }

      invocation.service(getRequestFacade(), getResponseFacade());
    } catch (ClientDisconnectException e) {
      throw e;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);

      try {
        _errorManager.sendServletError(e, getRequestFacade(),
                                       getResponseFacade());
      } catch (ClientDisconnectException e1) {
        throw e1;
      } catch (Exception e1) {
        log.log(Level.FINE, e1.toString(), e1);
      }
    }

    return true;
  }

  /**
   * Initialize the read stream from the raw stream.
   */
  @Override
  public boolean initStream(ReadStream readStream,
                            ReadStream rawStream)
    throws IOException
  {
    readStream.init(_filter, null);

    return true;
  }

  private void getClientCertificate()
  {
    HttpServletRequestImpl request = getRequestFacade();

    String cipher = getHeader("SSL_CIPHER");
    if (cipher == null)
      cipher = getHeader("HTTPS_CIPHER");
    if (cipher != null)
      request.setAttribute("javax.servlet.request.cipher_suite", cipher);

    String keySize = getHeader("SSL_CIPHER_USEKEYSIZE");
    if (keySize == null)
      keySize = getHeader("SSL_SECRETKEYSIZE");
    if (keySize != null)
      request.setAttribute("javax.servlet.request.key_size", keySize);

    if (_clientCert.size() == 0)
      return;

    try {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      InputStream is = _clientCert.createInputStream();
      X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
      is.close();

      request.setAttribute("javax.servlet.request.X509Certificate",
                           new X509Certificate[]{cert});
      request.setAttribute(com.caucho.security.AbstractLogin.LOGIN_NAME,
                           ((X509Certificate) cert).getSubjectDN());
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  protected boolean checkLogin()
  {
    return true;
  }

  @Override
  public void onStartConnection()
  {
    super.onStartConnection();
    
    _hmtpRequest.onStartConnection();
    _isHmtpRequest = false;
  }
  
  /**
   * Clears variables at the start of a new request.
   */
  @Override
  protected void startRequest(HttpBufferStore httpBuffer)
    throws IOException
  {
    super.startRequest(httpBuffer);

    _method.clear();
    _methodString = null;
    _protocol.clear();
    _version = 0;
    _uri.clear();
    _host.clear();
    _headerSize = 0;

    _remoteHost.clear();
    _remoteAddr.clear();
    _serverName.clear();
    _serverPort.clear();
    _remotePort.clear();

    _clientCert.clear();

    _pendingData = 0;
    _bufferStartOffset = 0;

    _isSecure = getConnection().isSecure();
  }

  /**
   * Sends busy response for preview mode.
   */
  private boolean sendBusyResponse()
    throws IOException
  {
    HttpServletResponseImpl response = getResponseFacade();

    response.sendError(503);

    return true;
  }

  /**
   * Fills request parameters from the stream.
   */
  private boolean scanHeaders()
    throws IOException
  {
    boolean isLoggable = log.isLoggable(Level.FINE);
    ReadStream is = getRawRead();
    WriteStream os = getRawWrite();
    CharBuffer cb = getCharBuffer();
    int code;
    int len;

    while (true) {
      _rawWrite.flush();

      code = is.read();

      Server server = getServer();
      if (server == null || server.isDestroyed()) {
        log.fine(dbgId() + " request after server close");
        killKeepalive();
        return false;
      }

      switch (code) {
      case -1:
        if (isLoggable)
          log.fine(dbgId() + "r: end of file");
        _filter.setClientClosed(true);
        killKeepalive();
        return false;

      case HMUX_CHANNEL:
        int channel = (is.read() << 8) + is.read();

        if (isLoggable)
          log.fine(dbgId() + "channel-r " + channel);
        break;

      case HMUX_YIELD:
        if (log.isLoggable(Level.FINER))
          log.finer(dbgId() + (char) code + "-r: yield");

        os.write(HMUX_ACK);
        os.write(0);
        os.write(0);
        os.flush();
        break;

      case HMUX_QUIT:
        if (isLoggable)
          log.fine(dbgId() + (char) code + "-r: end of request");

        return true;

      case HMUX_EXIT:
        if (isLoggable)
          log.fine(dbgId() + (char) code + "-r: end of socket");

        killKeepalive();

        return true;

      case HMUX_PROTOCOL:
        len = (is.read() << 8) + is.read();

        if (len != 4) {
          log.fine(dbgId() + (char) code + "-r: protocol length (" + len + ") must be 4.");
          killKeepalive();
          return false;
        }

        int value = ((is.read() << 24)
                     + (is.read() << 16)
                     + (is.read() << 8)
                     + (is.read()));

        dispatchProtocol(is, code, value);
        return true;

      case HMUX_URI:
        len = (is.read() << 8) + is.read();
        _uri.setLength(len);
        is.readAll(_uri.getBuffer(), 0, len);
        if (isLoggable)
          log.fine(dbgId() + (char) code + ":uri " + _uri);
        _hasRequest = true;
        break;

      case HMUX_METHOD:
        len = (is.read() << 8) + is.read();
        is.readAll(_method, len);
        if (isLoggable)
          log.fine(dbgId() +
                   (char) code + ":method " + _method);
        break;

      case CSE_REAL_PATH:
        len = (is.read() << 8) + is.read();
        _cb1.clear();
        is.readAll(_cb1, len);
        code = is.read();
        if (code != HMUX_STRING)
          throw new IOException("protocol expected HMUX_STRING");
        _cb2.clear();
        is.readAll(_cb2, readLength(is));

        //http.setRealPath(cb1.toString(), cb2.toString());
        if (isLoggable)
          log.fine(dbgId() + (char) code + " " +
                   _cb1.toString() + "->" + _cb2.toString());
        //throw new RuntimeException();
        break;

      case CSE_REMOTE_HOST:
        len = (is.read() << 8) + is.read();
        is.readAll(_remoteHost, len);
        if (isLoggable)
          log.fine(dbgId() + (char) code + " " + _remoteHost);
        break;

      case CSE_REMOTE_ADDR:
        len = (is.read() << 8) + is.read();
        is.readAll(_remoteAddr, len);
        if (isLoggable)
          log.fine(dbgId() + (char) code + " " + _remoteAddr);
        break;

      case HMUX_SERVER_NAME:
        len = (is.read() << 8) + is.read();
        is.readAll(_serverName, len);
        if (isLoggable)
          log.fine(dbgId() + (char) code + " server-host: " + _serverName);
        break;

      case CSE_REMOTE_PORT:
        len = (is.read() << 8) + is.read();
        is.readAll(_remotePort, len);
        if (isLoggable)
          log.fine(dbgId() + (char) code +
                  " remote-port: " + _remotePort);
        break;

      case CSE_SERVER_PORT:
        len = (is.read() << 8) + is.read();
        is.readAll(_serverPort, len);
        if (isLoggable)
          log.fine(dbgId() + (char) code +
                  " server-port: " + _serverPort);
        break;

      case CSE_QUERY_STRING:
        len = (is.read() << 8) + is.read();
        if (len > 0) {
          _uri.add('?');
          _uri.ensureCapacity(_uri.getLength() + len);
          is.readAll(_uri.getBuffer(), _uri.getLength(), len);
          _uri.setLength(_uri.getLength() + len);
        }
        break;

      case CSE_PROTOCOL:
        len = (is.read() << 8) + is.read();
        is.readAll(_protocol, len);
        if (isLoggable)
          log.fine(dbgId() + (char) code + " protocol: " + _protocol);
        for (int i = 0; i < len; i++) {
          char ch = _protocol.charAt(i);

          if ('0' <= ch && ch <= '9')
            _version = 16 * _version + ch - '0';
          else if (ch == '.')
            _version = 16 * _version;
        }
        break;

      case HMUX_HEADER:
        len = (is.read() << 8) + is.read();

        int headerSize = _headerSize;

        CharBuffer key = _headerKeys[headerSize];
        key.clear();

        CharBuffer valueCb = _headerValues[headerSize];
        valueCb.clear();

        is.readAll(key, len);
        code = is.read();
        if (code != HMUX_STRING)
          throw new IOException("protocol expected HMUX_STRING at " + (char) code);
        is.readAll(valueCb, readLength(is));

        if (isLoggable)
          log.fine(dbgId() + "H " + key + "=" + valueCb);

        if (addHeaderInt(key.getBuffer(), 0, key.length(), valueCb)) {
          _headerSize++;
        }
        break;

      case CSE_CONTENT_LENGTH:
        len = (is.read() << 8) + is.read();
        if (_headerKeys.length <= _headerSize)
          resizeHeaders();
        _headerKeys[_headerSize].clear();
        _headerKeys[_headerSize].append("Content-Length");
        _headerValues[_headerSize].clear();
        is.readAll(_headerValues[_headerSize], len);

        setContentLength(_headerValues[_headerSize]);

        if (isLoggable)
          log.fine(dbgId() + (char) code + " content-length=" +
                   _headerValues[_headerSize]);
        _headerSize++;
        break;

      case CSE_CONTENT_TYPE:
        len = (is.read() << 8) + is.read();
        if (_headerKeys.length <= _headerSize)
          resizeHeaders();
        _headerKeys[_headerSize].clear();
        _headerKeys[_headerSize].append("Content-Type");
        _headerValues[_headerSize].clear();
        is.readAll(_headerValues[_headerSize], len);
        if (isLoggable)
          log.fine(dbgId() + (char) code
                   + " content-type=" + _headerValues[_headerSize]);
        _headerSize++;
        break;

      case CSE_IS_SECURE:
        len = (is.read() << 8) + is.read();
        _isSecure = true;
        if (isLoggable)
          log.fine(dbgId() + "secure");
        is.skip(len);
        break;

      case CSE_CLIENT_CERT:
        len = (is.read() << 8) + is.read();
        _clientCert.clear();
        _clientCert.setLength(len);
        is.readAll(_clientCert.getBuffer(), 0, len);
        if (isLoggable)
          log.fine(dbgId() + (char) code + " cert=" + _clientCert
                   + " len:" + len);
        break;

      case CSE_SERVER_TYPE:
        len = (is.read() << 8) + is.read();
        _cb1.clear();
        is.readAll(_cb1, len);
        if (isLoggable)
          log.fine(dbgId() + (char) code + " server=" + _cb1);
        if (_cb1.length() > 0)
          _serverType = _cb1.charAt(0);
        break;

      case CSE_REMOTE_USER:
        len = (is.read() << 8) + is.read();
        cb.clear();
        is.readAll(cb, len);
        if (isLoggable)
          log.fine(dbgId() + (char) code + " " + cb);
        getRequestFacade().setAttribute(com.caucho.security.AbstractLogin.LOGIN_NAME,
                                        new com.caucho.security.BasicPrincipal(cb.toString()));
        break;

      case HMUX_DATA:
        len = (is.read() << 8) + is.read();
        _pendingData = len;
        if (isLoggable)
          log.fine(dbgId() + (char) code + " post-data: " + len);

        if (len > 0)
          return true;
        break;

      case HMUX_TO_UNIDIR_HMTP:
      case HMUX_SWITCH_TO_HMTP: {
        if (_hasRequest)
          throw new IllegalStateException();

        is.unread();

        if (isLoggable)
          log.fine(dbgId() + (char) code + "-r switch-to-hmtp");
        
        _isHmtpRequest = true;
        
        return _hmtpRequest.handleRequest();
      }

      case ' ': case '\n':
        // skip for debugging
        break;

      default:
        {
          int d1 = is.read();
          int d2 = is.read();

          if (d2 < 0) {
            if (isLoggable)
              log.fine(dbgId() + "r: unexpected end of file");

            killKeepalive();
            return false;
          }

          len = (d1 << 8) + d2;

          if (isLoggable)
            log.fine(dbgId() + (char) code + " " + len);

          is.skip(len);
          break;
        }
      }
    }
  }

  private void dispatchProtocol(ReadStream is, int code, int value)
    throws IOException
  {
    int result = HMUX_EXIT;
    boolean isKeepalive = false;
    if (value == HMUX_CLUSTER_PROTOCOL) {
      if (log.isLoggable(Level.FINE))
        log.fine(dbgId() + (char) code + "-r: cluster protocol");
      _filter.setClientClosed(true);

      result = _clusterRequest.handleRequest(is, _rawWrite);
    }
    else if (value == HMUX_DISPATCH_PROTOCOL) {
      if (log.isLoggable(Level.FINE))
        log.fine(dbgId() + (char) code + "-r: dispatch protocol");

      _filter.setClientClosed(true);

      isKeepalive = _dispatchRequest.handleRequest(is, _rawWrite);

      if (isKeepalive)
        result = HMUX_QUIT;
      else
        result = HMUX_EXIT;
    }
    else {
      HmuxExtension ext = _hmuxProtocol.getExtension(value);

      if (ext != null) {
        if (log.isLoggable(Level.FINE))
          log.fine(dbgId() + (char) code + "-r: extension " + ext);
        _filter.setClientClosed(true);

        result = ext.handleRequest(this, is, _rawWrite);
      }
      else {
        log.fine(dbgId() + (char) code + "-r: unknown protocol (" + value + ")");
        result = HMUX_EXIT;
      }
    }

    if (result == HMUX_YIELD) {
      _rawWrite.write(HMUX_ACK);
      _rawWrite.write(0);
      _rawWrite.write(0);
      _rawWrite.flush();
      return; // XXX:
    }
    else {
      if (result == HMUX_QUIT && ! isKeepaliveAllowed())
        result = HMUX_EXIT;

      if (result == HMUX_QUIT) {
        _rawWrite.write(HMUX_QUIT);
        _rawWrite.flush();
      }
      else {
        killKeepalive();
        _rawWrite.write(HMUX_EXIT);
        _rawWrite.close();
      }
    }
  }

  private void resizeHeaders()
  {
    CharBuffer []newKeys = new CharBuffer[_headerSize * 2];
    CharBuffer []newValues = new CharBuffer[_headerSize * 2];

    for (int i = 0; i < _headerSize; i++) {
      newKeys[i] = _headerKeys[i];
      newValues[i] = _headerValues[i];
    }

    for (int i = _headerSize; i < newKeys.length; i++) {
      newKeys[i] = new CharBuffer();
      newValues[i] = new CharBuffer();
    }

    _headerKeys = newKeys;
    _headerValues = newValues;
  }

  private int readLength(ReadStream is)
    throws IOException
  {
    return ((is.read() << 8) + is.read());
  }

  void writeFlush()
    throws IOException
  {
    WriteStream out = _rawWrite;

    synchronized (out) {
      out.flush();
    }
  }

  /**
   * Returns the header.
   */
  @Override
  public String getMethod()
  {
    if (_methodString == null) {
      CharSegment cb = getMethodBuffer();
      if (cb.length() == 0) {
        _methodString = "GET";
        return _methodString;
      }

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
    }

    return _methodString;

  }

  public CharSegment getMethodBuffer()
  {
    return _method;
  }

  /**
   * Returns a char buffer containing the host.
   */
  @Override
  protected CharBuffer getHost()
  {
    if (_host.length() > 0)
      return _host;

    _host.append(_serverName);
    _host.toLowerCase();

    return _host;
  }

  public final byte []getUriBuffer()
  {
    return _uri.getBuffer();
  }

  public final int getUriLength()
  {
    return _uri.getLength();
  }

  /**
   * Returns the protocol.
   */
  public String getProtocol()
  {
    return _protocol.toString();
  }

  public CharSegment getProtocolBuffer()
  {
    return _protocol;
  }

  final int getVersion()
  {
    return _version;
  }

  /**
   * Returns true if the request is secure.
   */
  @Override
  public boolean isSecure()
  {
    return super.isSecure() || _isSecure;
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

  @Override
  public CharSegment getHeaderBuffer(String key)
  {
    for (int i = 0; i < _headerSize; i++) {
      CharBuffer test = _headerKeys[i];

      if (test.equalsIgnoreCase(key))
        return _headerValues[i];
    }

    return null;
  }

  public CharSegment getHeaderBuffer(char []buf, int length)
  {
    for (int i = 0; i < _headerSize; i++) {
      CharBuffer test = _headerKeys[i];

      if (test.length() != length)
        continue;

      char []keyBuf = test.getBuffer();
      int j;
      for (j = 0; j < length; j++) {
        char a = buf[j];
        char b = keyBuf[j];
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
        return _headerValues[i];
    }

    return null;
  }

  @Override
  public void setHeader(String key, String value)
  {
    if (_headerKeys.length <= _headerSize)
      resizeHeaders();

    _headerKeys[_headerSize].clear();
    _headerKeys[_headerSize].append(key);
    _headerValues[_headerSize].clear();
    _headerValues[_headerSize].append(value);
    _headerSize++;
  }

  @Override
  public void getHeaderBuffers(String key, ArrayList<CharSegment> values)
  {
    CharBuffer cb = getCharBuffer();

    cb.clear();
    cb.append(key);

    int size = _headerSize;
    for (int i = 0; i < size; i++) {
      CharBuffer test = _headerKeys[i];
      if (test.equalsIgnoreCase(cb))
        values.add(_headerValues[i]);
    }
  }

  public Enumeration<String> getHeaderNames()
  {
    HashSet<String> names = new HashSet<String>();
    for (int i = 0; i < _headerSize; i++)
      names.add(_headerKeys[i].toString());

    return Collections.enumeration(names);
  }

  /**
   * Returns the URI for the request, special casing the IIS issues.
   * Because IIS already escapes the URI before sending it, the URI
   * needs to be re-escaped.
   */
  @Override
  public String getRequestURI()
  {
    if (_serverType == 'R')
      return super.getRequestURI();

    String _rawURI = super.getRequestURI();

    if (_rawURI == null)
      return null;

    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < _rawURI.length(); i++) {
      char ch = _rawURI.charAt(i);

      if (ch <= ' ' || ch >= 0x80 || ch == '%') {
        addHex(cb, ch);
      }
      else
        cb.append(ch);
    }

    return cb.close();
  }

  /**
   * Adds a hex escape.
   *
   * @param cb the char buffer containing the escape.
   * @param ch the character to be escaped.
   */
  private void addHex(CharBuffer cb, int ch)
  {
    cb.append('%');

    int d = (ch >> 4) & 0xf;
    if (d < 10)
      cb.append((char) ('0' + d));
    else
      cb.append((char) ('a' + d - 10));

    d = ch & 0xf;
    if (d < 10)
      cb.append((char) ('0' + d));
    else
      cb.append((char) ('a' + d - 10));
  }

  /**
   * Returns the server name.
   */
  @Override
  public String getServerName()
  {
    CharBuffer host = getHost();
    if (host == null) {
      InetAddress addr = getConnection().getRemoteAddress();
      return addr.getHostName();
    }

    int p = host.indexOf(':');
    if (p >= 0)
      return host.substring(0, p);
    else
      return host.toString();
  }

  @Override
  public int getServerPort()
  {
    int len = _serverPort.length();
    int port = 0;
    for (int i = 0; i < len; i++) {
      char ch = _serverPort.charAt(i);
      port = 10 * port + ch - '0';
    }

    return port;
  }

  @Override
  public String getRemoteAddr()
  {
    return _remoteAddr.toString();
  }

  public void getRemoteAddr(CharBuffer cb)
  {
    cb.append(_remoteAddr);
  }

  @Override
  public int printRemoteAddr(byte []buffer, int offset)
    throws IOException
  {
    char []buf = _remoteAddr.getBuffer();
    int len = _remoteAddr.getLength();

    for (int i = 0; i < len; i++)
      buffer[offset + i] = (byte) buf[i];

    return offset + len;
  }

  @Override
  public String getRemoteHost()
  {
    return _remoteHost.toString();
  }

  /**
   * Called for a connection: close
   */
  @Override
  protected void handleConnectionClose()
  {
    // ignore for hmux
  }

  // Response data
  void writeStatus(CharBuffer message)
    throws IOException
  {
    writeString(HMUX_STATUS, message);
  }

  /**
   * Complete sending of all headers.
   */
  void sendHeader()
    throws IOException
  {
    writeString(CSE_SEND_HEADER, "");
  }

  /**
   * Writes a header to the plugin.
   *
   * @param key the header's key
   * @param value the header's value
   */
  void writeHeader(String key, String value)
    throws IOException
  {
    writeString(HMUX_HEADER, key);
    writeString(HMUX_STRING, value);
  }

  /**
   * Writes a header to the plugin.
   *
   * @param key the header's key
   * @param value the header's value
   */
  void writeHeader(String key, CharBuffer value)
    throws IOException
  {
    writeString(HMUX_HEADER, key);
    writeString(HMUX_STRING, value);
  }

  void flushResponseBuffer()
    throws IOException
  {
    HttpServletRequestImpl request = getRequestFacade();

    if (request != null) {
      AbstractResponseStream stream = request.getResponse().getResponseStream();

      stream.flushNext();
    }
  }

  //
  // HmuxResponseStream methods
  //

  protected byte []getNextBuffer()
  {
    return _rawWrite.getBuffer();
  }

  protected int getNextStartOffset()
  {
    if (_bufferStartOffset == 0) {
      int bufferLength = _rawWrite.getBuffer().length;
      int startOffset = _rawWrite.getBufferOffset() + 3;
      if (bufferLength <= startOffset) {
        try {
          _rawWrite.flush();
        } catch (IOException e) {
          log.log(Level.FINE, e.toString(), e);
        }
        startOffset = _rawWrite.getBufferOffset() + 3;
      }

      _rawWrite.setBufferOffset(startOffset);
      _bufferStartOffset = startOffset;
    }

    return _bufferStartOffset;
  }

  protected int getNextBufferOffset()
    throws IOException
  {
    if (_bufferStartOffset == 0) {
      int bufferLength = _rawWrite.getBuffer().length;
      int startOffset = _rawWrite.getBufferOffset() + 3;
      if (bufferLength <= startOffset) {
        _rawWrite.flush();
        startOffset = _rawWrite.getBufferOffset() + 3;
      }

      _rawWrite.setBufferOffset(startOffset);
      _bufferStartOffset = startOffset;
    }

    return _rawWrite.getBufferOffset();
  }

  protected void setNextBufferOffset(int offset)
    throws IOException
  {
    offset = fillDataBuffer(offset);
    _rawWrite.setBufferOffset(offset);
  }

  protected byte []writeNextBuffer(int offset)
    throws IOException
  {
    offset = fillDataBuffer(offset);

    return _rawWrite.nextBuffer(offset);
  }

  protected void flushNext()
    throws IOException
  {
    flushNextBuffer();

    _rawWrite.flush();
  }

  protected void flushNextBuffer()
    throws IOException
  {
    WriteStream next = _rawWrite;

    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + "flush()");

    int startOffset = _bufferStartOffset;

    if (startOffset > 0) {
      _bufferStartOffset = 0;
      if (startOffset == next.getBufferOffset()) {
        next.setBufferOffset(startOffset - 3);
      }
      else {
        // illegal state because the data isn't filled
        throw new IllegalStateException();
      }
    }
  }

  protected void writeTail()
    throws IOException
  {
    WriteStream next = _rawWrite;

    int offset = next.getBufferOffset();

    offset = fillDataBuffer(offset);

    // server/26a6
    // Use setBufferOffset because nextBuffer would
    // force an early flush
    next.setBufferOffset(offset);
  }

  private int fillDataBuffer(int offset)
    throws IOException
  {
    // server/2642
    int bufferStart = _bufferStartOffset;

    if (bufferStart == 0)
      return offset;

    byte []buffer = _rawWrite.getBuffer();

    // if (bufferStart > 0 && offset == buffer.length) {
    int length = offset - bufferStart;

    _bufferStartOffset = 0;
    // server/26a0
    if (length == 0) {
      offset = bufferStart - 3;
    }
    else {
      buffer[bufferStart - 3] = (byte) HmuxRequest.HMUX_DATA;
      buffer[bufferStart - 2] = (byte) (length >> 8);
      buffer[bufferStart - 1] = (byte) (length);

      if (log.isLoggable(Level.FINE))
        log.fine(dbgId() + (char) HmuxRequest.HMUX_DATA + "-w(" + length + ")");
    }

    _bufferStartOffset = 0;

    return offset;
  }


  void writeString(int code, String value)
    throws IOException
  {
    int len = value.length();

    WriteStream os = _rawWrite;

    os.write(code);
    os.write(len >> 8);
    os.write(len);
    os.print(value);

    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + (char)code + "-w " + value);
  }

  void writeString(int code, CharBuffer cb)
    throws IOException
  {
    int len = cb.length();

    WriteStream os = _rawWrite;

    os.write(code);
    os.write(len >> 8);
    os.write(len);
    os.print(cb.getBuffer(), 0, len);

    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + (char)code + "-w: " + cb);
  }

  /**
   * Close when the socket closes.
   */
  @Override
  public void onCloseConnection()
  {
    _isHmtpRequest = false;
    _hmtpRequest.onCloseConnection();
  }

  protected String getRequestId()
  {
    String id = _server.getServerId();

    if (id.equals(""))
      return "server-" + getConnection().getId();
    else
      return "server-" + id + ":" + getConnection().getId();
  }

  @Override
  public final String dbgId()
  {
    String id = _server.getServerId();

    if (id.equals(""))
      return "Hmux[" + getConnection().getId() + "] ";
    else
      return "Hmux[" + id + ":" + getConnection().getId() + "] ";
  }

  @Override
  public String toString()
  {
    return "HmuxRequest" + dbgId();
  }

  /**
   * Implements the protocol for data reads and writes.  Data from the
   * web server to the JVM must be acked, except for the first data.
   * Data back to the web server needs no ack.
   */
  static class ServletFilter extends StreamImpl {
    HmuxRequest _request;
    ReadStream _is;
    WriteStream _os;
    byte []_buffer = new byte[16];
    int _pendingData;
    boolean _needsAck;
    boolean _isClosed;
    boolean _isClientClosed;

    ServletFilter()
    {
    }

    void init(HmuxRequest request,
              ReadStream nextRead, WriteStream nextWrite)
    {
      _request = request;
      _is = nextRead;
      _os = nextWrite;
      _pendingData = 0;
      _isClosed = false;
      _isClientClosed = false;
      _needsAck = false;
    }

    void setPending(int pendingData)
    {
      _pendingData = pendingData;
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
      int sublen = _pendingData;
      ReadStream is = _is;

      if (sublen <= 0)
        return -1;

      if (length < sublen)
        sublen = length;

      _os.flush();
      int readLen = is.read(buf, offset, sublen);
      _pendingData -= readLen;

      if (log.isLoggable(Level.FINEST))
        log.finest(new String(buf, offset, readLen));

      while (_pendingData == 0) {
        _os.flush();

        int code = is.read();

        switch (code) {
        case HMUX_DATA: {
          int len = (is.read() << 8) + is.read();

          if (log.isLoggable(Level.FINE))
            log.fine(_request.dbgId() + "D-r:post-data " + len);

          _pendingData = len;

          if (len > 0)
            return readLen;

          break;
        }

        case HMUX_QUIT: {
          if (log.isLoggable(Level.FINE))
            log.fine(_request.dbgId() + "Q-r:quit");

          return readLen;
        }

        case HMUX_EXIT: {
          if (log.isLoggable(Level.FINE))
            log.fine(_request.dbgId() + "X-r:exit");

          _request.killKeepalive();
          return readLen;
        }

        case HMUX_YIELD: {
          _request.flushResponseBuffer();

          _os.write(HMUX_ACK);
          _os.write(0);
          _os.write(0);
          _os.flush();

          if (log.isLoggable(Level.FINE)) {
            log.fine(_request.dbgId() + "Y-r:yield");
            log.fine(_request.dbgId() + "A-w:ack");
          }
          break;
        }

        case HMUX_CHANNEL: {
          int channel = (is.read() << 8) + is.read();

          if (log.isLoggable(Level.FINE))
            log.fine(_request.dbgId() + "channel-r " + channel);
          break;
        }

        case ' ': case '\n':
          break;

        default:
          if (code < 0) {
            _request.killKeepalive();

            return readLen;
          }
          else {
            _request.killKeepalive();

            int len = (is.read() << 8) + is.read();

            if (log.isLoggable(Level.FINE))
              log.fine(_request.dbgId() + "unknown-r '" + (char) code + "' " + len);

            is.skip(len);
          }
        }
      }

      return readLen;
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
      _request.flushResponseBuffer();

      if (log.isLoggable(Level.FINE)) {
        log.fine(_request.dbgId() + (char) HMUX_DATA + ":data " + length);

        if (log.isLoggable(Level.FINEST))
          log.finest(_request.dbgId() + "data <" + new String(buf, offset, length) + ">");
      }

      byte []tempBuf = _buffer;

      while (length > 0) {
        int sublen = length;

        if (32 * 1024 < sublen)
          sublen = 32 * 1024;

        // The 3 bytes are already allocated by setPrefixWrite
        tempBuf[0] = HMUX_DATA;
        tempBuf[1] = (byte) (sublen >> 8);
        tempBuf[2] = (byte) sublen;

        _os.write(tempBuf, 0, 3);
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
        log.fine(_request.dbgId() + (char) HMUX_FLUSH + "-w:flush");

      _os.write(HMUX_FLUSH);
      _os.write(0);
      _os.write(0);
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
            log.fine(_request.dbgId() + (char) HMUX_QUIT + "-w: quit channel");
          else
            log.fine(_request.dbgId() + (char) HMUX_EXIT + "-w: exit socket");
        }

        if (keepalive)
          _os.write(HMUX_QUIT);
        else
          _os.write(HMUX_EXIT);
      }

      if (keepalive)
        _os.flush();
      else
        _os.close();
      //nextRead.close();
    }
  }
}
