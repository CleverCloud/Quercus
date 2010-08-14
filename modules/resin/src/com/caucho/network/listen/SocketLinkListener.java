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

package com.caucho.network.listen;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.Period;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.thread.TaskWorker;
import com.caucho.env.thread.ThreadPool;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.management.server.PortMXBean;
import com.caucho.management.server.TcpConnectionInfo;
import com.caucho.server.cluster.Server;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;
import com.caucho.vfs.JsseSSLFactory;
import com.caucho.vfs.QJniServerSocket;
import com.caucho.vfs.QServerSocket;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.SSLFactory;

/**
 * Represents a protocol connection.
 */
@Configurable
public class SocketLinkListener extends TaskWorker
  implements Runnable
{
  private static final L10N L = new L10N(SocketLinkListener.class);

  private static final Logger log
    = Logger.getLogger(SocketLinkListener.class.getName());

  private final AtomicInteger _connectionCount = new AtomicInteger();

  // started at 128, but that seems wasteful since the active threads
  // themselves are buffering the free connections
  private FreeList<TcpSocketLink> _idleConn
    = new FreeList<TcpSocketLink>(32);

  // The owning server
  // private ProtocolDispatchServer _server;

  private ThreadPool _threadPool = ThreadPool.getThreadPool();

  private ClassLoader _classLoader
    = Thread.currentThread().getContextClassLoader();

  // The id
  private String _serverId = "";

  // The address
  private String _address;
  // The port
  private int _port = -1;

  // URL for debugging
  private String _url;

  // The protocol
  private Protocol _protocol;

  // The SSL factory, if any
  private SSLFactory _sslFactory;

  // Secure override for load-balancers/proxies
  private boolean _isSecure;

  private InetAddress _socketAddress;

  private int _idleThreadMin = 2;
  private int _idleThreadMax = 8;

  private int _acceptListenBacklog = 100;

  private int _connectionMax = 1024 * 1024;

  private int _keepaliveMax = -1;

  private long _keepaliveTimeMax = 10 * 60 * 1000L;
  private long _keepaliveTimeout = 120 * 1000L;
  
  private boolean _isKeepaliveSelectEnable = true;
  private long _keepaliveSelectThreadTimeout = 1000;
  
  // default timeout
  private long _socketTimeout = 120 * 1000L;

  private long _suspendReaperTimeout = 60000L;
  private long _suspendTimeMax = 600 * 1000L;
  // after for 120s start checking for EOF on comet requests
  private long _suspendCloseTimeMax = 120 * 1000L;

  private boolean _tcpNoDelay = true;
  
  private boolean _isEnableJni = true;

  // The virtual host name
  private String _virtualHost;

  private final SocketLinkAdmin _admin = new SocketLinkAdmin(this);

  // the server socket
  private QServerSocket _serverSocket;

  // the throttle
  private Throttle _throttle;

  // the selection manager
  private AbstractSelectManager _selectManager;

  // active set of all connections
  private Set<TcpSocketLink> _activeConnectionSet
    = Collections.synchronizedSet(new HashSet<TcpSocketLink>());

  private final AtomicInteger _activeConnectionCount = new AtomicInteger();

  // server push (comet) suspend set
  private Set<TcpSocketLink> _suspendConnectionSet
    = Collections.synchronizedSet(new HashSet<TcpSocketLink>());

  private final AtomicInteger _idleThreadCount = new AtomicInteger();
  private final AtomicInteger _startThreadCount = new AtomicInteger();

  // reaper alarm for timed out comet requests
  private Alarm _suspendAlarm;

  // statistics

  private final AtomicInteger _threadCount = new AtomicInteger();

  private volatile long _lifetimeRequestCount;
  private volatile long _lifetimeKeepaliveCount;
  private volatile long _lifetimeClientDisconnectCount;
  private volatile long _lifetimeRequestTime;
  private volatile long _lifetimeReadBytes;
  private volatile long _lifetimeWriteBytes;

  // total keepalive
  private AtomicInteger _keepaliveAllocateCount = new AtomicInteger();
  // thread-based
  private AtomicInteger _keepaliveThreadCount = new AtomicInteger();
  // True if the port has been bound
  private final AtomicBoolean _isBind = new AtomicBoolean();
  private final AtomicBoolean _isPostBind = new AtomicBoolean();

  // The port lifecycle
  private final Lifecycle _lifecycle = new Lifecycle();

  public SocketLinkListener()
  {
    if ("64".equals(System.getProperty("sun.arch.data.model"))) {
      // on 64-bit machines we can use more threads before parking in nio
      _keepaliveSelectThreadTimeout = 60000;
    }
  }

  /**
   * Sets the id.
   */
  // exists only for QA regressions
  @Deprecated
  public void setId(String id)
  {
  }

  public String getDebugId()
  {
    return getUrl();
  }

  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  public PortMXBean getAdmin()
  {
    return _admin;
  }

  /**
   * Set protocol.
   */
  public void setProtocol(Protocol protocol)
    throws ConfigException
  {
    /* server/0170
    if (_server == null)
      throw new IllegalStateException(L.l("Server is not set."));
    */

    _protocol = protocol;

    // protocol.setPort(this);
    // _protocol.setServer(_server);
  }

  /**
   * Returns the protocol handler responsible for generating protocol-specific
   * ProtocolConnections.
   */
  public Protocol getProtocol()
  {
    return _protocol;
  }

  /**
   * Gets the protocol name.
   */
  public String getProtocolName()
  {
    if (_protocol != null)
      return _protocol.getProtocolName();
    else
      return null;
  }

  /**
   * Sets the address
   */
  @Configurable
  public void setAddress(String address)
    throws UnknownHostException
  {
    if ("*".equals(address))
      address = null;

    _address = address;

    if (address != null)
      _socketAddress = InetAddress.getByName(address);
  }

  /**
   * Gets the IP address
   */
  public String getAddress()
  {
    return _address;
  }

  /**
   * @deprecated
   */
  public void setHost(String address)
    throws UnknownHostException
  {
    setAddress(address);
  }

  /**
   * Sets the port.
   */
  @Configurable
  public void setPort(int port)
  {
    _port = port;
  }

  /**
   * Gets the port.
   */
  public int getPort()
  {
    return _port;
  }

  /**
   * Gets the local port (for ephemeral ports)
   */
  public int getLocalPort()
  {
    if (_serverSocket != null)
      return _serverSocket.getLocalPort();
    else
      return _port;
  }

  /**
   * Sets the virtual host for IP-based virtual host.
   */
  @Configurable
  public void setVirtualHost(String host)
  {
    _virtualHost = host;
  }

  /**
   * Gets the virtual host for IP-based virtual host.
   */
  public String getVirtualHost()
  {
    return _virtualHost;
  }

  /**
   * Sets the SSL factory
   */
  public void setSSL(SSLFactory factory)
  {
    _sslFactory = factory;
  }

  /**
   * Sets the SSL factory
   */
  @Configurable
  public SSLFactory createOpenssl()
    throws ConfigException
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class<?> cl = Class.forName("com.caucho.vfs.OpenSSLFactory", false, loader);

      _sslFactory = (SSLFactory) cl.newInstance();

      return _sslFactory;
    } catch (Throwable e) {
      e.printStackTrace();

      log.log(Level.FINER, e.toString(), e);

      throw new ConfigException(L.l("<openssl> requires Resin Professional.  See http://www.caucho.com for more information."),
                                e);
    }
  }

  /**
   * Sets the SSL factory
   */
  public JsseSSLFactory createJsse()
  {
    // should probably check that openssl exists
    return new JsseSSLFactory();
  }

  /**
   * Sets the SSL factory
   */
  public void setJsseSsl(JsseSSLFactory factory)
  {
    _sslFactory = factory;
  }

  /**
   * Gets the SSL factory.
   */
  public SSLFactory getSSL()
  {
    return _sslFactory;
  }

  /**
   * Returns true for ssl.
   */
  public boolean isSSL()
  {
    return _sslFactory != null;
  }

  /**
   * Sets true for secure
   */
  @Configurable
  public void setSecure(boolean isSecure)
  {
    _isSecure = isSecure;
  }

  /**
   * Return true for secure
   */
  public boolean isSecure()
  {
    return _isSecure || _sslFactory != null;
  }

  /**
   * Sets the server socket.
   */
  public void setServerSocket(QServerSocket socket)
  {
    _serverSocket = socket;
  }

  //
  // Configuration/Tuning
  //

  /**
   * Sets the minimum spare listen.
   */
  @Configurable
  public void setAcceptThreadMin(int minSpare)
    throws ConfigException
  {
    if (minSpare < 1)
      throw new ConfigException(L.l("accept-thread-min must be at least 1."));

    _idleThreadMin = minSpare;
  }

  /**
   * The minimum spare threads.
   */
  public int getAcceptThreadMin()
  {
    return _idleThreadMin;
  }

  /**
   * Sets the minimum spare listen.
   */
  @Configurable
  public void setAcceptThreadMax(int maxSpare)
    throws ConfigException
  {
    if (maxSpare < 1)
      throw new ConfigException(L.l("accept-thread-max must be at least 1."));

    _idleThreadMax = maxSpare;
  }

  /**
   * The maximum spare threads.
   */
  public int getAcceptThreadMax()
  {
    return _idleThreadMax;
  }

  /**
   * Sets the operating system listen backlog
   */
  @Configurable
  public void setAcceptListenBacklog(int listen)
    throws ConfigException
  {
    if (listen < 1)
      throw new ConfigException(L.l("accept-listen-backlog must be at least 1."));

    _acceptListenBacklog = listen;
  }

  /**
   * The operating system listen backlog
   */
  public int getAcceptListenBacklog()
  {
    return _acceptListenBacklog;
  }

  /**
   * Sets the connection max.
   */
  @Configurable
  public void setConnectionMax(int max)
  {
    _connectionMax = max;
  }

  /**
   * Gets the connection max.
   */
  public int getConnectionMax()
  {
    return _connectionMax;
  }

  /**
   * Sets the read/write timeout for the accepted sockets.
   */
  @Configurable
  public void setSocketTimeout(Period period)
  {
    _socketTimeout = period.getPeriod();
  }

  /**
   * Sets the read/write timeout for the accepted sockets.
   */
  public void setSocketTimeoutMillis(long timeout)
  {
    _socketTimeout = timeout;
  }

  /**
   * Gets the read timeout for the accepted sockets.
   */
  public long getSocketTimeout()
  {
    return _socketTimeout;
  }

  /**
   * Gets the tcp-no-delay property
   */
  public boolean getTcpNoDelay()
  {
    return _tcpNoDelay;
  }

  /**
   * Sets the tcp-no-delay property
   */
  @Configurable
  public void setTcpNoDelay(boolean tcpNoDelay)
  {
    _tcpNoDelay = tcpNoDelay;
  }

  /**
   * Configures the throttle.
   */
  @Configurable
  public void setThrottleConcurrentMax(int max)
  {
    Throttle throttle = createThrottle();

    if (throttle != null)
      throttle.setMaxConcurrentRequests(max);
  }

  /**
   * Configures the throttle.
   */
  public long getThrottleConcurrentMax()
  {
    if (_throttle != null)
      return _throttle.getMaxConcurrentRequests();
    else
      return -1;
  }
  
  public void setEnableJni(boolean isEnableJni)
  {
    _isEnableJni = isEnableJni;
  }

  private Throttle createThrottle()
  {
    if (_throttle == null) {
      _throttle = Throttle.createPro();

      if (_throttle == null
          && Server.getCurrent() != null
          && ! Server.getCurrent().isWatchdog())
        throw new ConfigException(L.l("throttle configuration requires Resin Professional"));
    }

    return _throttle;
  }

  //
  // compat config
  //

  /**
   * Sets the keepalive max.
   */
  public void setKeepaliveMax(int max)
  {
    _keepaliveMax = max;
  }

  /**
   * Gets the keepalive max.
   */
  public int getKeepaliveMax()
  {
    return _keepaliveMax;
  }

  /**
   * Sets the keepalive max.
   */
  public void setKeepaliveConnectionTimeMax(Period period)
  {
    _keepaliveTimeMax = period.getPeriod();
  }

  /**
   * Gets the keepalive max.
   */
  public long getKeepaliveConnectionTimeMax()
  {
    return _keepaliveTimeMax;
  }

  /**
   * Gets the suspend max.
   */
  public long getSuspendTimeMax()
  {
    return _suspendTimeMax;
  }

  public void setSuspendTimeMax(Period period)
  {
    _suspendTimeMax = period.getPeriod();
  }

  public void setKeepaliveTimeout(Period period)
  {
    _keepaliveTimeout = period.getPeriod();
  }

  public long getKeepaliveTimeout()
  {
    return _keepaliveTimeout;
  }

  public boolean isKeepaliveSelectEnabled()
  {
    return _isKeepaliveSelectEnable;
  }

  public void setKeepaliveSelectEnabled(boolean isKeepaliveSelect)
  {
    _isKeepaliveSelectEnable = isKeepaliveSelect;
  }

  public void setKeepaliveSelectEnable(boolean isKeepaliveSelect)
  {
    setKeepaliveSelectEnabled(isKeepaliveSelect);
  }
  
  public void setKeepaliveSelectMax(int max)
  {
    
  }

  public long getKeepaliveSelectThreadTimeout()
  {
    return _keepaliveSelectThreadTimeout;
  }

  public void setKeepaliveSelectThreadTimeout(Period period)
  {
    setKeepaliveSelectThreadTimeoutMillis(period.getPeriod());
  }

  public void setKeepaliveSelectThreadTimeoutMillis(long timeout)
  {
    _keepaliveSelectThreadTimeout = timeout;
  }

  public long getBlockingTimeoutForSelect()
  {
    long timeout = _keepaliveSelectThreadTimeout;

    if (timeout <= 10)
      return timeout;
    else if (_threadPool.getFreeThreadCount() < 64)
      return 10;
    else
      return timeout;
  }

  public int getKeepaliveSelectMax()
  {
    if (getSelectManager() != null)
      return getSelectManager().getSelectMax();
    else
      return -1;
  }
  
  /**
   * Ignore unknown tags.
   * 
   * server/0940
   */
  @Configurable
  public void addContentProgram(ConfigProgram program)
  {
    
  }

  //
  // statistics
  //

  /**
   * Returns the thread count.
   */
  public int getThreadCount()
  {
    return _threadCount.get();
  }

  /**
   * Returns the active thread count.
   */
  public int getActiveThreadCount()
  {
    return _threadCount.get() - _idleThreadCount.get();
  }

  /**
   * Returns the count of idle threads.
   */
  public int getIdleThreadCount()
  {
    return _idleThreadCount.get();
  }

  /**
   * Returns the count of start threads.
   */
  public int getStartThreadCount()
  {
    return _startThreadCount.get();
  }

  /**
   * Returns the number of keepalive connections
   */
  public int getKeepaliveCount()
  {
    return _keepaliveAllocateCount.get();
  }

  public Lifecycle getLifecycleState()
  {
    return _lifecycle;
  }

  public boolean isAfterBind()
  {
    return _isBind.get();
  }
  /**
   * Returns true if the port is active.
   */
  public boolean isActive()
  {
    return _lifecycle.isActive();
  }

  /**
   * Returns the active connections.
   */
  public int getActiveConnectionCount()
  {
    return _threadCount.get() - _idleThreadCount.get();
  }

  /**
   * Returns the keepalive connections.
   */
  public int getKeepaliveConnectionCount()
  {
    return getKeepaliveCount();
  }

  /**
   * Returns the number of keepalive connections
   */
  public int getKeepaliveThreadCount()
  {
    return _keepaliveThreadCount.get();
  }

  /**
   * Returns the number of connections in the select.
   */
  public int getSelectConnectionCount()
  {
    if (_selectManager != null)
      return _selectManager.getSelectCount();
    else
      return -1;
  }

  /**
   * Returns true if the port should start a new thread because there are
   * less than _idleThreadMin accepting threads.
   */
  private boolean isStartThreadRequired()
  {
    return (_startThreadCount.get() + _idleThreadCount.get() < _idleThreadMin);
  }
  
  /**
   * Returns the server socket class name for debugging.
   */
  public String getServerSocketClassName()
  {
    QServerSocket ss = _serverSocket;
    
    if (ss != null)
      return ss.getClass().getName();
    else
      return null;
  }

  /**
   * Initializes the port.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (! _lifecycle.toInit())
      return;
  }
  
  public String getUrl()
  {
    if (_url == null) {
      StringBuilder url = new StringBuilder();

      if (_protocol != null)
        url.append(_protocol.getProtocolName());
      else
        url.append("unknown");
      url.append("://");

      if (getAddress() != null)
        url.append(getAddress());
      else
        url.append("*");
      url.append(":");
      url.append(getPort());

      if (_serverId != null && ! "".equals(_serverId)) {
        url.append("(");
        url.append(_serverId);
        url.append(")");
      }

      _url = url.toString();
    }
    
    return _url;
  }

  /**
   * Starts the port listening.
   */
  public void bind()
    throws Exception
  {
    if (_isBind.getAndSet(true))
      return;

    if (_protocol == null)
      throw new IllegalStateException(L.l("'{0}' must have a configured protocol before starting.", this));

    // server 1e07
    if (_port < 0)
      return;

    if (_throttle == null)
      _throttle = new Throttle();

    if (_serverSocket != null) {
      if (_address != null)
        log.info("listening to " + _address + ":" + _serverSocket.getLocalPort());
      else
        log.info("listening to " + _serverSocket.getLocalPort());
    }
    else if (_sslFactory != null && _socketAddress != null) {
      _serverSocket = _sslFactory.create(_socketAddress, _port);

      log.info(_protocol.getProtocolName() + "s listening to " + _socketAddress.getHostName() + ":" + _port);
    }
    else if (_sslFactory != null) {
      if (_address == null) {
        _serverSocket = _sslFactory.create(null, _port);
        log.info(_protocol.getProtocolName() + "s listening to *:" + _port);
      }
      else {
        InetAddress addr = InetAddress.getByName(_address);

        _serverSocket = _sslFactory.create(addr, _port);

        log.info(_protocol.getProtocolName() + "s listening to " + _address + ":" + _port);
      }
    }
    else if (_socketAddress != null) {
      _serverSocket = QJniServerSocket.create(_socketAddress, _port,
                                              _acceptListenBacklog,
                                              _isEnableJni);

      log.info(_protocol.getProtocolName() + " listening to " + _socketAddress.getHostName() + ":" + _serverSocket.getLocalPort());
    }
    else {
      _serverSocket = QJniServerSocket.create(null, _port, _acceptListenBacklog,
                                              _isEnableJni);

      log.info(_protocol.getProtocolName() + " listening to *:"
               + _serverSocket.getLocalPort());
    }

    assert(_serverSocket != null);

    postBind();
  }

  /**
   * Starts the port listening.
   */
  public void bind(QServerSocket ss)
    throws Exception
  {
    if (ss == null)
      throw new NullPointerException();

    _isBind.set(true);

    if (_protocol == null)
      throw new IllegalStateException(L.l("'{0}' must have a configured protocol before starting.", this));

    if (_throttle == null)
      _throttle = new Throttle();

    _serverSocket = ss;

    String scheme = _protocol.getProtocolName();

    if (_address != null)
      log.info(scheme + " listening to " + _address + ":" + _port);
    else
      log.info(scheme + " listening to *:" + _port);

    if (_sslFactory != null)
      _serverSocket = _sslFactory.bind(_serverSocket);
  }

  public void postBind()
  {
    if (_isPostBind.getAndSet(true))
      return;

    if (_serverSocket == null)
      return;

    if (_tcpNoDelay)
      _serverSocket.setTcpNoDelay(_tcpNoDelay);

    _serverSocket.setConnectionSocketTimeout((int) getSocketTimeout());

    if (_serverSocket.isJni()) {
      ResinSystem server = ResinSystem.getCurrent();

      if (server != null) {
        SocketPollService pollService 
          = server.getService(SocketPollService.class);
        
        if (pollService != null) {
          _selectManager = pollService.getSelectManager();
          
        }
      }

      /*
      if (_selectManager == null) {
        throw new IllegalStateException(L.l("Cannot load select manager"));
      }
      */
    }

    /*
    if (_keepaliveMax < 0)
      _keepaliveMax = _server.getKeepaliveMax();
    */

    if (_keepaliveMax < 0 && _selectManager != null)
      _keepaliveMax = _selectManager.getSelectMax();

    if (_keepaliveMax < 0)
      _keepaliveMax = 256;

    _admin.register();
  }

  /**
   * binds for the watchdog.
   */
  public QServerSocket bindForWatchdog()
    throws java.io.IOException
  {
    QServerSocket ss;

    // use same method for ports for testability reasons
    /*
    if (_port >= 1024)
      return null;
    else
    */
    
    if (_sslFactory instanceof JsseSSLFactory) {
      if (_port < 1024) {
        log.warning(this + " cannot bind jsse in watchdog");
      }
      
      return null;
    }

    if (_socketAddress != null) {
      ss = QJniServerSocket.createJNI(_socketAddress, _port);

      if (ss == null)
        return null;

      log.fine(this + " watchdog binding to " + _socketAddress.getHostName() + ":" + _port);
    }
    else {
      ss = QJniServerSocket.createJNI(null, _port);

      if (ss == null)
        return null;

      log.fine(this + " watchdog binding to *:" + _port);
    }

    if (! ss.isJni()) {
      ss.close();

      return ss;
    }

    if (_tcpNoDelay)
      ss.setTcpNoDelay(_tcpNoDelay);

    ss.setConnectionSocketTimeout((int) getSocketTimeout());

    return ss;
  }

  /**
   * Starts the port listening.
   */
  public void start()
    throws Exception
  {
    if (_port < 0)
      return;

    if (! _lifecycle.toStarting())
      return;

    boolean isValid = false;
    try {
      bind();
      postBind();

      enable();

      wake();

      _suspendAlarm = new Alarm(new SuspendReaper());
      _suspendAlarm.queue(_suspendReaperTimeout);

      isValid = true;
    } finally {
      if (! isValid)
        close();
    }
  }

  /**
   * Starts the port listening for new connections.
   */
  void enable()
  {
    if (_lifecycle.toActive()) {
      if (_serverSocket != null)
        _serverSocket.listen(_acceptListenBacklog);
    }
  }

  /**
   * Stops the port from listening for new connections.
   */
  void disable()
  {
    if (_lifecycle.toStop()) {
      if (_serverSocket != null)
        _serverSocket.listen(0);

      if (_port < 0) {
      }
      else if (_address != null)
        log.info(_protocol.getProtocolName() + " disabled "
                 + _address + ":" + getLocalPort());
      else
        log.info(_protocol.getProtocolName() + " disabled *:" + getLocalPort());
    }
  }

  /**
   * returns the connection info for jmx
   */
  TcpConnectionInfo []connectionInfo()
  {
    TcpSocketLink []connections;

    connections = new TcpSocketLink[_activeConnectionSet.size()];
    _activeConnectionSet.toArray(connections);

    long now = Alarm.getExactTime();
    TcpConnectionInfo []infoList = new TcpConnectionInfo[connections.length];

    for (int i = 0 ; i < connections.length; i++) {
      TcpSocketLink conn = connections[i];

      long requestTime = -1;
      long startTime = conn.getRequestStartTime();

      if (conn.isRequestActive() && startTime > 0)
        requestTime = now - startTime;

      TcpConnectionInfo info
        = new TcpConnectionInfo(conn.getId(),
                                conn.getThreadId(),
                                getAddress() + ":" + getPort(),
                                conn.getState().toString(),
                                requestTime);

      infoList[i] = info;
    }

    return infoList;
  }

  /**
   * returns the select manager.
   */
  public AbstractSelectManager getSelectManager()
  {
    return _selectManager;
  }

  /**
   * Accepts a new connection.
   *
   * @param isStart boolean to mark the first request on the thread for
   *   bookkeeping.
   */
  public boolean accept(QSocket socket)
  {
    boolean isDecrementIdle = true;

    try {
      int idleThreadCount = _idleThreadCount.incrementAndGet();

      while (_lifecycle.isActive()) {
        idleThreadCount = _idleThreadCount.get();

        if (_idleThreadMax < idleThreadCount
            && _idleThreadCount.compareAndSet(idleThreadCount,
                                              idleThreadCount - 1)) {
          isDecrementIdle = false;
          return false;
        }

        Thread.interrupted();
        if (_serverSocket.accept(socket)) {
          if (_throttle.accept(socket))
            return true;
          else
            socket.close();
        }
      }
    } catch (Throwable e) {
      if (_lifecycle.isActive() && log.isLoggable(Level.FINER))
        log.log(Level.FINER, e.toString(), e);
    } finally {
      if (isDecrementIdle)
        _idleThreadCount.decrementAndGet();

      if (isStartThreadRequired()) {
        // if there are not enough idle threads, wake the manager to
        // create a new one
        wake();
      }
    }

    return false;
  }

  /**
   * Notification when a socket closes.
   */
  void closeSocket(QSocket socket)
  {
    if (_throttle != null)
      _throttle.close(socket);
  }

  /**
   * Registers the new connection as started
   */
  void startConnection(TcpSocketLink conn)
  {
    _startThreadCount.decrementAndGet();

    wake();
  }

  /**
   * Marks a new thread as running.
   */
  void threadBegin(TcpSocketLink conn)
  {
    _threadCount.incrementAndGet();
  }

  /**
   * Marks a new thread as stopped.
   */
  void threadEnd(TcpSocketLink conn)
  {
    _threadCount.decrementAndGet();

    wake();
  }

  /**
   * Allocates a keepalive for the connection.
   *
   * @param connectionStartTime - when the connection's accept occurred.
   */
  boolean isKeepaliveAllowed(long connectionStartTime)
  {
    if (! _lifecycle.isActive())
      return false;
    else if (connectionStartTime + _keepaliveTimeMax < Alarm.getCurrentTime())
      return false;
    else if (_keepaliveMax <= _keepaliveAllocateCount.get())
      return false;
    else
      return true;
  }

  /**
   * Marks the keepalive allocation as starting.
   * Only called from ConnectionState.
   */
  void keepaliveAllocate()
  {
    _keepaliveAllocateCount.incrementAndGet();
  }

  /**
   * Marks the keepalive allocation as ending.
   * Only called from ConnectionState.
   */
  void keepaliveFree()
  {
    _keepaliveAllocateCount.decrementAndGet();
  }

  /**
   * Reads data from a keepalive connection
   */
  int keepaliveThreadRead(ReadStream is)
    throws IOException
  {
    if (isClosed())
      return -1;

    int available = is.getBufferAvailable();
    
    if (available > 0) {
      return available;
    }

    long timeout = getKeepaliveTimeout();

    // boolean isSelectManager = getServer().isSelectManagerEnabled();

    if (_isKeepaliveSelectEnable) {
      timeout = getBlockingTimeoutForSelect();
    }

    if (getSocketTimeout() < timeout)
      timeout = getSocketTimeout();

    if (timeout < 0)
      timeout = 0;
    
    // server/2l02

    _keepaliveThreadCount.incrementAndGet();

    try {
      int result = is.fillWithTimeout(timeout);

      return result;
    } catch (IOException e) {
      if (isClosed()) {
        log.log(Level.FINEST, e.toString(), e);

        return -1;
      }

      throw e;
    } finally {
      _keepaliveThreadCount.decrementAndGet();
    }
  }

  /**
   * Suspends the controller (for comet-style ajax)
   *
   * @return true if the connection was added to the suspend list
   */
  void cometSuspend(TcpSocketLink conn)
  {
    if (conn.isWakeRequested()) {
      conn.toCometResume();
      
      _threadPool.schedule(conn.getResumeTask());
    }
    else {
      _suspendConnectionSet.add(conn);

      // XXX: wake
    }
  }

  /**
   * Remove from suspend list.
   */
  boolean cometDetach(TcpSocketLink conn)
  {
    return _suspendConnectionSet.remove(conn);
  }

  /**
   * Resumes the controller (for comet-style ajax)
   */
  boolean cometResume(TcpSocketLink conn)
  {
    if (_suspendConnectionSet.remove(conn)) {
      conn.toCometResume();
      
      _threadPool.schedule(conn.getResumeTask());

      return true;
    }
    else
      return false;
  }

  void duplexKeepaliveBegin()
  {
  }

  void duplexKeepaliveEnd()
  {
  }

  /**
   * Returns true if the port is closed.
   */
  public boolean isClosed()
  {
    return _lifecycle.isAfterActive();
  }

  //
  // statistics
  //

  /**
   * Returns the number of connections
   */
  public int getConnectionCount()
  {
    return _activeConnectionCount.get();
  }

  /**
   * Returns the number of comet connections.
   */
  public int getCometIdleCount()
  {
    return _suspendConnectionSet.size();
  }

  /**
   * Returns the number of duplex connections.
   */
  public int getDuplexCount()
  {
    return 0;
  }

  void addLifetimeRequestCount()
  {
    _lifetimeRequestCount++;
  }

  public long getLifetimeRequestCount()
  {
    return _lifetimeRequestCount;
  }

  void addLifetimeKeepaliveCount()
  {
    _lifetimeKeepaliveCount++;
  }

  public long getLifetimeKeepaliveCount()
  {
    return _lifetimeKeepaliveCount;
  }

  void addLifetimeClientDisconnectCount()
  {
    _lifetimeClientDisconnectCount++;
  }

  public long getLifetimeClientDisconnectCount()
  {
    return _lifetimeClientDisconnectCount;
  }

  void addLifetimeRequestTime(long time)
  {
    _lifetimeRequestTime += time;
  }

  public long getLifetimeRequestTime()
  {
    return _lifetimeRequestTime;
  }

  void addLifetimeReadBytes(long time)
  {
    _lifetimeReadBytes += time;
  }

  public long getLifetimeReadBytes()
  {
    return _lifetimeReadBytes;
  }

  void addLifetimeWriteBytes(long time)
  {
    _lifetimeWriteBytes += time;
  }

  public long getLifetimeWriteBytes()
  {
    return _lifetimeWriteBytes;
  }

  /**
   * Find the TcpConnection based on the thread id (for admin)
   */
  public TcpSocketLink findConnectionByThreadId(long threadId)
  {
    ArrayList<TcpSocketLink> connList
      = new ArrayList<TcpSocketLink>(_activeConnectionSet);

    for (TcpSocketLink conn : connList) {
      if (conn.getThreadId() == threadId)
        return conn;
    }

    return null;
  }

  /**
   * The port thread is responsible for creating new connections.
   */
  public long runTask()
  {
    if (_lifecycle.isDestroyed())
      return -1;

    try {
      TcpSocketLink startConn = null;

      if (isStartThreadRequired()
          && _lifecycle.isActive()
          && _activeConnectionCount.get() < _connectionMax) {
        startConn = _idleConn.allocate();

        if (startConn != null) {
          startConn.toInit(); // change to the init/ready state
        }
        else {
          int connId = _connectionCount.incrementAndGet();
          QSocket socket = _serverSocket.createSocket();
          startConn = new TcpSocketLink(connId, this, socket);
        }

        _startThreadCount.incrementAndGet();
        _activeConnectionCount.incrementAndGet();
        _activeConnectionSet.add(startConn);

        if (! _threadPool.schedule(startConn.getAcceptTask())) {
          log.severe(L.l("Schedule failed for {0}", startConn));
        }
      }
    } catch (Throwable e) {
      log.log(Level.SEVERE, e.toString(), e);
    }
    
    return -1;
  }

  /**
   * Frees the connection to the idle pool.
   *
   * only called from ConnectionState
   */
  void free(TcpSocketLink conn)
  {
    closeConnection(conn);

    _idleConn.free(conn);
  }

  /**
   * Destroys the connection.
   *
   * only called from ConnectionState
   */
  void destroy(TcpSocketLink conn)
  {
    closeConnection(conn);
  }

  /**
   * Closes the stats for the connection.
   */
  private void closeConnection(TcpSocketLink conn)
  {
    _activeConnectionSet.remove(conn);
    _activeConnectionCount.decrementAndGet();

    // wake the start thread
    wake();
  }

  /**
   * Shuts the Port down.  The server gives connections 30
   * seconds to complete.
   */
  public void close()
  {
    if (! _lifecycle.toDestroy())
      return;

    if (log.isLoggable(Level.FINE))
      log.fine(this + " closing");

    super.destroy();

    Alarm suspendAlarm = _suspendAlarm;
    _suspendAlarm = null;

    if (suspendAlarm != null)
      suspendAlarm.dequeue();

    QServerSocket serverSocket = _serverSocket;
    _serverSocket = null;

    _selectManager = null;
    AbstractSelectManager selectManager = null;

    /*
    if (_server != null) {
      selectManager = _server.getSelectManager();
      _server.initSelectManager(null);
    }
    */

    InetAddress localAddress = null;
    int localPort = 0;
    if (serverSocket != null) {
      localAddress = serverSocket.getLocalAddress();
      localPort = serverSocket.getLocalPort();
    }

    // close the server socket
    if (serverSocket != null) {
      try {
        serverSocket.close();
      } catch (Throwable e) {
      }

      try {
        synchronized (serverSocket) {
          serverSocket.notifyAll();
        }
      } catch (Throwable e) {
      }
    }

    if (selectManager != null) {
      try {
        selectManager.close();
      } catch (Throwable e) {
      }
    }

    Set<TcpSocketLink> activeSet;

    synchronized (_activeConnectionSet) {
      activeSet = new HashSet<TcpSocketLink>(_activeConnectionSet);
    }

    for (TcpSocketLink conn : activeSet) {
      try {
        conn.destroy();
      }
      catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }

    // wake the start thread
    wake();

    // Close the socket server socket and send some request to make
    // sure the Port accept thread is woken and dies.
    // The ping is before the server socket closes to avoid
    // confusing the threads

    // ping the accept port to wake the listening threads
    if (localPort > 0) {
      int idleCount = _idleThreadCount.get() + _startThreadCount.get();

      for (int i = 0; i < idleCount + 10; i++) {
        InetSocketAddress addr;

        if (localAddress == null ||
            localAddress.getHostAddress().startsWith("0.")) {
          addr = new InetSocketAddress("127.0.0.1", localPort);
          connectAndClose(addr);
          
          addr = new InetSocketAddress("[::1]", localPort);
          connectAndClose(addr);
        }
        else {
          addr = new InetSocketAddress(localAddress, localPort);
          connectAndClose(addr);
        }
      }
    }

    TcpSocketLink conn;
    while ((conn = _idleConn.allocate()) != null) {
      conn.destroy();
    }

    log.finest(this + " closed");
  }
  
  private void connectAndClose(InetSocketAddress addr)
  {
    try {
      Socket socket = new Socket();

      socket.connect(addr, 100);

      socket.close();
    } catch (ConnectException e) {
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);
    }

  }

  public String toURL()
  {
    return getUrl();
  }

  @Override
  protected String getThreadName()
  {
    return "resin-port-" + getAddress() + ":" + getPort();
  }

  @Override
  public String toString()
  {
    if (_url != null)
      return getClass().getSimpleName() + "[" + _url + "]";
    else
      return getClass().getSimpleName() + "[" + getAddress() + ":" + getPort() + "]";
  }

  public class SuspendReaper implements AlarmListener {
    private ArrayList<TcpSocketLink> _suspendSet
      = new ArrayList<TcpSocketLink>();

    private ArrayList<TcpSocketLink> _timeoutSet
      = new ArrayList<TcpSocketLink>();

    private ArrayList<TcpSocketLink> _completeSet
      = new ArrayList<TcpSocketLink>();

    public void handleAlarm(Alarm alarm)
    {
      try {
        _suspendSet.clear();
        _timeoutSet.clear();
        _completeSet.clear();

        long now = Alarm.getCurrentTime();

        synchronized (_suspendConnectionSet) {
          _suspendSet.addAll(_suspendConnectionSet);
        }

        for (int i = _suspendSet.size() - 1; i >= 0; i--) {
          TcpSocketLink conn = _suspendSet.get(i);
          
          if (conn.getIdleExpireTime() < now) {
            _timeoutSet.add(conn);
            continue;
          }

          long idleStartTime = conn.getIdleStartTime();

          // check periodically for end of file
          if (idleStartTime + _suspendCloseTimeMax < now
              && conn.isReadEof()) {
            _completeSet.add(conn);
          }
        }

        for (int i = _timeoutSet.size() - 1; i >= 0; i--) {
          TcpSocketLink conn = _timeoutSet.get(i);

          if (log.isLoggable(Level.FINE))
            log.fine(this + " suspend idle timeout " + conn);

          conn.toCometTimeout();
        }

        for (int i = _completeSet.size() - 1; i >= 0; i--) {
          TcpSocketLink conn = _completeSet.get(i);

          if (log.isLoggable(Level.FINE))
            log.fine(this + " async end-of-file " + conn);

          AsyncController async = conn.getAsyncController();

          if (async != null)
            async.complete();

          // server/1lc2
          // conn.wake();
          // conn.destroy();
        }
      } catch (Throwable e) {
        e.printStackTrace();
      } finally {
        if (! isClosed())
          alarm.queue(_suspendReaperTimeout);
      }
    }
  }

}
