/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is software; you can redistribute it and/or modify
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

package com.caucho.network.listen;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.shutdown.ShutdownService;
import com.caucho.inject.Module;
import com.caucho.inject.RequestContext;
import com.caucho.loader.Environment;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.TcpConnectionMXBean;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StreamImpl;

/**
 * A protocol-independent TcpConnection.  TcpConnection controls the
 * TCP Socket and provides buffered streams.
 *
 * <p>Each TcpConnection has its own thread.
 */
@Module
public class TcpSocketLink extends AbstractSocketLink
{
  private static final Logger log
    = Logger.getLogger(TcpSocketLink.class.getName());

  private static final ThreadLocal<ProtocolConnection> _currentRequest
    = new ThreadLocal<ProtocolConnection>();

  private static ClassLoader _systemClassLoader;

  private final int _connectionId;  // The connection's id
  private final String _id;
  private final String _name;
  private String _dbgId;

  private final SocketLinkListener _port;
  private final QSocket _socket;
  private final ProtocolConnection _request;
  private final ClassLoader _loader;
  private final byte []_testBuffer = new byte[1];

  private final AcceptTask _acceptTask = new AcceptTask();
  // HTTP keepalive task
  private final KeepaliveRequestTask _keepaliveTask
    = new KeepaliveRequestTask();
  // Comet resume task
  private final ResumeTask _resumeTask = new ResumeTask();
  // duplex (websocket) task
  private DuplexReadTask _duplexReadTask;

  private final Admin _admin = new Admin();

  private SocketLinkState _state = SocketLinkState.INIT;
  private AsyncController _controller;
  
  private boolean _isWakeRequested;
  private boolean _isCompleteRequested;

  private long _idleTimeout;

  private long _connectionStartTime;
  private long _requestStartTime;

  private long _idleStartTime;
  private long _idleExpireTime;

  // statistics state
  private String _displayState;

  private Thread _thread;

  /**
   * Creates a new TcpConnection.
   *
   * @param server The TCP server controlling the connections
   * @param request The protocol Request
   */
  TcpSocketLink(int connId,
                SocketLinkListener port,
                QSocket socket)
  {
    _connectionId = connId;

    _port = port;
    _socket = socket;

    int id = getId();

    _loader = port.getClassLoader();

    Protocol protocol = port.getProtocol();

    _request = protocol.createConnection(this);

    _id = port.getDebugId() + "-" + id;
    _name = _id;
  }

  /**
   * Returns the ServerRequest for the current thread.
   */
  public static ProtocolConnection getCurrentRequest()
  {
    return _currentRequest.get();
  }

  /**
   * For QA only, set the current request.
   */
  public static void qaSetCurrentRequest(ProtocolConnection request)
  {
    _currentRequest.set(request);
  }

  /**
   * Returns the connection id.  Primarily for debugging.
   */
  @Override
  public int getId()
  {
    return _connectionId;
  }

  /**
   * Returns the object name for jmx.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the port which generated the connection.
   */
  public SocketLinkListener getPort()
  {
    return _port;
  }

  /**
   * Returns the request for the connection.
   */
  public final ProtocolConnection getRequest()
  {
    return _request;
  }

  /**
   * Returns the admin
   */
  public TcpConnectionMXBean getAdmin()
  {
    return _admin;
  }

  //
  // timeout properties
  //

  /**
   * Sets the idle time for a keepalive connection.
   */
  public void setIdleTimeout(long idleTimeout)
  {
    _idleTimeout = idleTimeout;
  }

  /**
   * The idle time for a keepalive connection
   */
  public long getIdleTimeout()
  {
    return _idleTimeout;
  }

  //
  // port information
  //
  
  @Override
  public boolean isPortActive()
  {
    return _port.isActive();
  }
  
  //
  // state information
  //

  /**
   * Returns the state.
   */
  @Override
  public SocketLinkState getState()
  {
    return _state;
  }

  public final boolean isIdle()
  {
    return _state.isIdle();
  }

  /**
   * Returns true for active.
   */
  public boolean isActive()
  {
    return _state.isActive();
  }

  /**
   * Returns true for active.
   */
  public boolean isRequestActive()
  {
    return _state.isRequestActive();
  }

  @Override
  public boolean isKeepaliveAllocated()
  {
    return _state.isKeepaliveAllocated();
  }

  /**
   * Returns true for closed.
   */
  public boolean isClosed()
  {
    return _state.isClosed();
  }

  public final boolean isDestroyed()
  {
    return _state.isDestroyed();
  }

  @Override
  public boolean isCometActive()
  {
    return _state.isCometActive() && ! _isCompleteRequested;
  }

  @Override
  public boolean isCometSuspend()
  {
    return _state.isCometSuspend();
  }

  @Override
  public boolean isCometComplete()
  {
    return _state.isCometComplete();
  }

  @Override
  public boolean isDuplex()
  {
    return _state.isDuplex();
  }

  public boolean isWakeRequested()
  {
    return _isWakeRequested;
  }

  //
  // port/socket information
  //

  /**
   * Returns the connection's socket
   */
  public QSocket getSocket()
  {
    return _socket;
  }

  /**
   * Returns the local address of the socket.
   */
  @Override
  public InetAddress getLocalAddress()
  {
    return _socket.getLocalAddress();
  }

  /**
   * Returns the local host name.
   */
  @Override
  public String getLocalHost()
  {
    return _socket.getLocalHost();
  }

  /**
   * Returns the socket's local TCP port.
   */
  @Override
  public int getLocalPort()
  {
    return _socket.getLocalPort();
  }

  /**
   * Returns the socket's remote address.
   */
  @Override
  public InetAddress getRemoteAddress()
  {
    return _socket.getRemoteAddress();
  }

  /**
   * Returns the socket's remote host name.
   */
  @Override
  public String getRemoteHost()
  {
    return _socket.getRemoteHost();
  }

  /**
   * Adds from the socket's remote address.
   */
  @Override
  public int getRemoteAddress(byte []buffer, int offset, int length)
  {
    return _socket.getRemoteAddress(buffer, offset, length);
  }

  /**
   * Returns the socket's remote port
   */
  @Override
  public int getRemotePort()
  {
    return _socket.getRemotePort();
  }

  /**
   * Returns true if the connection is secure, i.e. a SSL connection
   */
  @Override
  public boolean isSecure()
  {
    return _socket.isSecure() || _port.isSecure();
  }

  /**
   * Returns the virtual host.
   */
  @Override
  public String getVirtualHost()
  {
    return getPort().getVirtualHost();
  }
  
  //
  // SSL api
  //
  
  /**
   * Returns the cipher suite
   */
  @Override
  public String getCipherSuite()
  {
    return _socket.getCipherSuite();
  }
  
  /***
   * Returns the key size.
   */
  @Override
  public int getKeySize()
  {
    return _socket.getCipherBits();
  }
  
  /**
   * Returns any client certificates.
   * @throws CertificateException 
   */
  @Override
  public X509Certificate []getClientCertificates()
    throws CertificateException
  {
    return _socket.getClientCertificates();
  }
  

  //
  // thread information
  //

  /**
   * Returns the thread id.
   */
  public final long getThreadId()
  {
    Thread thread = _thread;

    if (thread != null)
      return thread.getId();
    else
      return -1;
  }

  //
  // request information
  //

  /**
   * Returns the time the request started
   */
  public final long getRequestStartTime()
  {
    return _requestStartTime;
  }

  /**
   * Returns the idle expire time (keepalive or suspend).
   */
  public long getIdleExpireTime()
  {
    return _idleExpireTime;
  }

  /**
   * Returns the idle start time (keepalive or suspend)
   */
  public long getIdleStartTime()
  {
    return _idleStartTime;
  }

  /**
   * Returns the current keepalive task
   */
  public Runnable getAcceptTask()
  {
    return _acceptTask;
  }

  /**
   * Returns the current keepalive task (request or duplex)
   */
  public Runnable getKeepaliveTask()
  {
    if (_state.isDuplex())
      return _duplexReadTask;
    else
      return _keepaliveTask;
  }

  /**
   * Returns the current write task
   */
  public Runnable getResumeTask()
  {
    return _resumeTask;
  }

  //
  // statistics state
  //

  /**
   * Returns the user statistics state
   */
  public String getDisplayState()
  {
    return _displayState;
  }

  /**
   * Sets the user statistics state
   */
  public void setStatState(String state)
  {
    _displayState = state;
  }

  //
  // async/comet predicates
  //

  public AsyncController getAsyncController()
  {
    return _controller;
  }

  /**
   * Poll the socket to test for an end-of-file for a comet socket.
   */
  public boolean isReadEof()
  {
    QSocket socket = _socket;

    if (socket == null)
      return true;

    try {
      StreamImpl s = socket.getStream();

      int len = s.read(_testBuffer, 0, 0);

      return len < 0;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return true;
    }
  }

  //
  // request processing
  //

  /**
   * Handles a new connection/socket from the client.
   */
  RequestState handleRequests(boolean isKeepalive)
    throws IOException
  {
    Thread thread = Thread.currentThread();

    RequestState result = null;

    try {
      // clear the interrupted flag
      Thread.interrupted();

      result = handleRequestsImpl(isKeepalive);
    } catch (ClientDisconnectException e) {
      _port.addLifetimeClientDisconnectCount();

      if (log.isLoggable(Level.FINER)) {
        log.finer(dbgId() + e);
      }
    } catch (InterruptedIOException e) {
      if (log.isLoggable(Level.FINEST)) {
        log.log(Level.FINEST, dbgId() + e, e);
      }
    } catch (IOException e) {
      if (log.isLoggable(Level.FINE)) {
        log.log(Level.FINE, dbgId() + e, e);
      }
    } finally {
      thread.setContextClassLoader(_loader);

      if (result == null) {
        destroy();
        return RequestState.EXIT;
      }
    }

    if (result == RequestState.DUPLEX)
      return _duplexReadTask.doTask();
    else
      return result;
  }

  /**
   * Handles a new connection/socket from the client.
   */
  private RequestState handleRequestsImpl(boolean isKeepalive)
    throws IOException
  {
    RequestState result = null;

    do {
      result = RequestState.REQUEST;
      
      if (_port.isClosed()) {
        return RequestState.EXIT;
      }

      if (isKeepalive && (result = processKeepalive()) != RequestState.REQUEST) {
        return result;
      }

      dispatchRequest();

      if (_state == SocketLinkState.DUPLEX) {
        // duplex (xmpp/hmtp) handling
        return RequestState.DUPLEX;
      }

      getWriteStream().flush();

      if (_state.isCometActive() && toSuspend()) {
        return RequestState.THREAD_DETACHED;
      }
      
      isKeepalive = true;
    } while (_state.isKeepaliveAllocated());

    return result;
  }

  private void dispatchRequest()
    throws IOException
  {
    Thread thread = Thread.currentThread();

    try {
      thread.setContextClassLoader(_loader);

      _currentRequest.set(_request);
      RequestContext.begin();

      _isWakeRequested = false;
      
      if (_port.isKeepaliveAllowed(_connectionStartTime))
        _state = _state.toActiveWithKeepalive(this);
      else
        _state = _state.toActiveNoKeepalive(this);

      if (! getRequest().handleRequest()) {
        _state = _state.toKillKeepalive(this);
      }
    }
    finally {
      thread.setContextClassLoader(_loader);

      _currentRequest.set(null);
      RequestContext.end();
    }
  }

  /**
   * Starts a keepalive, either returning available data or
   * returning false to close the loop
   *
   * If keepaliveRead() returns true, data is available.
   * If it returns false, either the connection is closed,
   * or the connection has been registered with the select.
   */
  private RequestState processKeepalive()
    throws IOException
  {
    SocketLinkListener port = _port;

    // quick timed read to see if data is already available
    int available = port.keepaliveThreadRead(getReadStream());
    
    if (available > 0) {
      return RequestState.REQUEST;
    }
    else if (available < 0) {
      setStatState(null);
      close();
      
      return RequestState.EXIT;
    }

    _idleStartTime = Alarm.getCurrentTime();
    _idleExpireTime = _idleStartTime + _idleTimeout;

    _state = _state.toKeepalive(this);

    // use select manager if available
    if (_port.getSelectManager() != null) {
      // keepalive to select manager succeeds
      if (_port.getSelectManager().keepalive(this)) {
        if (log.isLoggable(Level.FINE))
          log.fine(dbgId() + " keepalive (select)");

        return RequestState.THREAD_DETACHED;
      }
      else {
        log.warning(dbgId() + " failed keepalive (select)");
      }
    }

    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + " keepalive (thread)");

    // if blocking read has available data
    if (getReadStream().waitForRead()) {
      return RequestState.REQUEST;
    }
    // blocking read timed out or closed
    else {
      close();

      return RequestState.EXIT;
    }
  }

  //
  // state transitions
  //

  /**
   * Sets the keepalive state.  Called only by the SelectManager and Port.
   */
  public void clearKeepalive()
  {
    // to init?

    /*
    if (! _isKeepalive)
      log.warning(this + " illegal state: clearing keepalive with inactive keepalive");

    _state = _state.isKeepalive = false;
    */
  }

  /**
   * Finish a request.
   */
  public void finishRequest()
  {
    AsyncController controller = _controller;
    _controller = null;

    if (controller != null)
      controller.closeImpl();

    /*
    // XXX: to finishRequest
    _state = _state.toCometComplete();
    */
  }

  @Override
  public void killKeepalive()
  {
    _state = _state.toKillKeepalive(this);
  }

  /**
   * Closes on shutdown.
   */
  public void closeOnShutdown()
  {
    QSocket socket = _socket;

    if (socket != null) {
      try {
        socket.close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }

      // Thread.yield();
    }
  }

  //
  // async/comet state transitions
  
  @Override
  public AsyncController toComet(CometHandler cometHandler)
  {
    // XXX: TCK
    if (_isCompleteRequested)
       throw new IllegalStateException("Comet cannot be requested after complete().");
    /*
    if (_isCompleteRequested)
      return null;
    */
    _state = _state.toComet();
    
    _controller = new TcpCometController(this, cometHandler);
    
    if (log.isLoggable(Level.FINER))
      log.finer(this + " starting comet");
    
    return _controller;
  }

  private boolean toSuspend()
  {
    _idleStartTime = Alarm.getCurrentTime();
    _idleExpireTime = _idleStartTime + _idleTimeout;

    _state = _state.toCometSuspend();

    if (log.isLoggable(Level.FINER))
      log.finer(this + " suspending comet");
    
    _port.cometSuspend(this);
    
    if (_isCompleteRequested)
      wake();
    
    return true;
  }
  
  void toCometResume()
  {
    _state = _state.toCometResume();
  }

  /**
   * Wakes the connection (comet-style).
   */
  @Override
  public boolean wake()
  {
    if (! _state.isComet())
      return false;

    _isWakeRequested = true;

    // comet
    if (_state.isCometSuspend() && getPort().cometResume(this)) {
      log.fine(dbgId() + " wake");
      return true;
    }
    else {
      return false;
    }
  }

  void toCometTimeout()
  {
    _isCompleteRequested = true;

    AsyncController async = getAsyncController();

    if (async != null)
      async.timeout();

    wake();
  }

  public void toCometComplete()
  {
    _isCompleteRequested = true;
    
    SocketLinkState state = _state;

    if (state.isCometSuspend()) {
      // XXX: timing issues, need to have isComplete flag
      wake();
    }
    
    // _state = _state.toCometComplete();
  }

  //
  // duplex/websocket
  //

  /**
   * Starts a full duplex (tcp style) request for hmtp/xmpp
   */
  @Override
  public SocketLinkDuplexController startDuplex(SocketLinkDuplexListener handler)
  {
    _state = _state.toDuplex(this);

    SocketLinkDuplexController duplex = new SocketLinkDuplexController(this, handler);

    _controller = duplex;

    _duplexReadTask = new DuplexReadTask(duplex);
    
    if (log.isLoggable(Level.FINER))
      log.finer(this + " starting duplex " + handler);

    try {
      handler.onStart(duplex);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return duplex;
  }

  public void closeController(TcpCometController controller)
  {
    if (controller == _controller) {
      _controller = null;

      closeControllerImpl();
    }
  }

  /**
   * Closes the controller.
   */
  protected void closeControllerImpl()
  {
    _isCompleteRequested = true;

    getPort().cometResume(this);
  }

  public void close()
  {
    closeImpl();
  }

  /**
   * Closes the connection.
   */
  private void closeImpl()
  {
    SocketLinkState state = _state;
    _state = _state.toClosed(this);

    if (state.isClosed() || state.isIdle()) {
      return;
    }

    QSocket socket = _socket;

    // detach any comet
    if (state.isComet() || state.isCometSuspend())
      getPort().cometDetach(this);

    getRequest().onCloseConnection();

    AsyncController controller = _controller;
    _controller = null;

    if (controller != null)
      controller.closeImpl();

    _duplexReadTask = null;

    SocketLinkListener port = getPort();

    if (log.isLoggable(Level.FINER)) {
      if (port != null)
        log.finer(dbgId() + "closing connection " + this + ", total=" + port.getConnectionCount());
      else
        log.finer(dbgId() + "closing connection " + this);
    }

    try {
      getWriteStream().close();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    try {
      getReadStream().close();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (socket != null) {
      getPort().closeSocket(socket);

      try {
        socket.close();
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  public final void toInit()
  {
    _state = _state.toInit();
  }

  /**
   * Destroys the connection()
   */
  public final void destroy()
  {
    _socket.forceShutdown();

    closeImpl();

    _state = _state.toDestroy(this);
  }

  /**
   * Completion processing at the end of the thread
   */
  void finishThread()
  {
    closeImpl();

    SocketLinkState state = _state;
    _state = state.toIdle();
    _isCompleteRequested = false;

    if (state.isAllowIdle()) {
      _port.free(this);
    }
  }

  protected String dbgId()
  {
    if (_dbgId == null) {
      Object serverId = Environment.getAttribute("caucho.server-id");

      if (serverId != null)
        _dbgId = (getClass().getSimpleName() + "[id=" + getId()
                  + "," + serverId + "] ");
      else
        _dbgId = (getClass().getSimpleName() + "[id=" + getId() + "] ");
    }

    return _dbgId;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[id=" + _id + "," + _port.toURL() + "," + _state + "]";
  }

  enum RequestState {
    REQUEST,
    THREAD_DETACHED,
    DUPLEX,
    EXIT
  };

  abstract class ConnectionReadTask implements Runnable {
    /**
     * Handles the read request for the connection
     */
    abstract RequestState doTask()
      throws IOException;

    public void run()
    {
      runThread();
    }

    public void runThread()
    {
      Thread thread = Thread.currentThread();
      String oldThreadName = thread.getName();

      thread.setName(_id);
      _thread = thread;

      ClassLoader systemLoader = _systemClassLoader;
      thread.setContextClassLoader(systemLoader);

      RequestState result = null;

      _port.threadBegin(TcpSocketLink.this);

      try {
        result = doTask();
      } catch (OutOfMemoryError e) {
        String msg = "TcpSocketLink OutOfMemory";
        
        ShutdownService.shutdownActive(ExitCode.MEMORY, msg); 
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        thread.setName(oldThreadName);
        _port.threadEnd(TcpSocketLink.this);

        if (result == null)
          destroy();

        if (result != RequestState.THREAD_DETACHED)
          finishThread();
      }
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + TcpSocketLink.this + "]";
    }
  }

  class AcceptTask extends ConnectionReadTask {
    @Override
    public void run()
    {
      SocketLinkListener port = _port;

      port.startConnection(TcpSocketLink.this);

      runThread();
    }

    /**
     * Loop to accept new connections.
     */
    @Override
    RequestState doTask()
      throws IOException
    {
      RequestState result = RequestState.EXIT;

      // _state = _state.toAccept();

      while (! _port.isClosed() && ! _state.isDestroyed()) {
        _state = _state.toAccept();
        setStatState("accept");

        if (! _port.accept(_socket)) {
          setStatState(null);
          close();

          return RequestState.EXIT;
        }

        _connectionStartTime = Alarm.getCurrentTime();

        setStatState("read");
        initSocket();

        _request.onStartConnection();

        boolean isKeepalive = false;
        result = handleRequests(isKeepalive);

        if (result == RequestState.THREAD_DETACHED) {
          return result;
        }
        else if (result == RequestState.DUPLEX) {
          setStatState("duplex");

          return _duplexReadTask.doTask();
        }

        setStatState(null);

        close();
      }

      return RequestState.EXIT;
    }

    /**
     * Initialize the socket for a new connection
     */
    private void initSocket()
      throws IOException
    {
      _idleTimeout = _port.getKeepaliveTimeout();

      getWriteStream().init(_socket.getStream());

      // ReadStream cannot use getWriteStream or auto-flush
      // because of duplex mode
      getReadStream().init(_socket.getStream(), null);

      if (log.isLoggable(Level.FINE)) {
        log.fine(dbgId() + "starting connection " + TcpSocketLink.this + ", total=" + _port.getConnectionCount());
      }
    }
  }

  class KeepaliveRequestTask extends ConnectionReadTask {
    public void run()
    {
      runThread();
    }

    @Override
    public RequestState doTask()
      throws IOException
    {
      boolean isKeepalive = true;
      RequestState result = handleRequests(isKeepalive);

      if (result == RequestState.THREAD_DETACHED) {
        return result;
      }
      else if (result == RequestState.DUPLEX) {
        return _duplexReadTask.doTask();
      }

      close();

      // acceptTask significantly faster than finishing
      return _acceptTask.doTask();
    }
  }

  class DuplexReadTask extends ConnectionReadTask {
    private final SocketLinkDuplexController _duplex;

    DuplexReadTask(SocketLinkDuplexController duplex)
    {
      _duplex = duplex;
    }

    @Override
    public void run()
    {
      runThread();
    }

    @Override
    public RequestState doTask()
      throws IOException
    {
      _state = _state.toDuplexActive(TcpSocketLink.this);

      RequestState result;

      ReadStream readStream = getReadStream();

      while ((result = processKeepalive()) == RequestState.REQUEST) {
        long position = readStream.getPosition();

        _duplex.serviceRead();

        if (position == readStream.getPosition()) {
          log.warning(_duplex + " was not processing any data. Shutting down.");
          setStatState(null);
          close();
          
          return RequestState.EXIT;
        }
      }

      return result;
    }
  }

  class ResumeTask implements Runnable {
    public void run()
    {
      boolean isValid = false;

      try {
        // _state = _state.toCometResume();

        _isWakeRequested = false;
        
        _state = _state.toCometDispatch();
        
        AsyncController controller = _controller;
        _controller = null;

        if (controller != null)
          controller.close();

        getRequest().handleResume();
        
        if (_state.isCometActive() && toSuspend()) {
          isValid = true;
        } else if (_state.isKeepaliveAllocated()) {
          isValid = true;
          _isCompleteRequested = false;
          _keepaliveTask.run();
        }
     } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      } catch (OutOfMemoryError e) {
        String msg = "TcpSocketLink OutOfMemory";
        
        ShutdownService.shutdownActive(ExitCode.MEMORY, msg);
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        if (! isValid) {
          finishThread();
        }
      }
    }
  }

  class Admin extends AbstractManagedObject implements TcpConnectionMXBean {
    Admin()
    {
      super(_systemClassLoader);
    }

    public String getName()
    {
      return _name;
    }

    public long getThreadId()
    {
      return TcpSocketLink.this.getThreadId();
    }

    public long getRequestActiveTime()
    {
      if (_requestStartTime > 0)
        return Alarm.getCurrentTime() - _requestStartTime;
      else
        return -1;
    }

    public String getUrl()
    {
      ProtocolConnection request = TcpSocketLink.this.getRequest();

      String url = request.getProtocolRequestURL();
      
      if (url != null && ! "".equals(url))
        return url;
      
      SocketLinkListener port = TcpSocketLink.this.getPort();

      if (port.getAddress() == null)
        return "request://*:" + port.getPort();
      else
        return "request://" + port.getAddress() + ":" + port.getPort();
    }

    public String getState()
    {
      return TcpSocketLink.this.getState().toString();
    }

    public String getDisplayState()
    {
      return TcpSocketLink.this.getDisplayState();
    }

    public String getRemoteAddress()
    {
      return TcpSocketLink.this.getRemoteHost();
    }

    void register()
    {
      registerSelf();
    }

    void unregister()
    {
      unregisterSelf();
    }
  }

  static {
    _systemClassLoader = ClassLoader.getSystemClassLoader();
  }
}
