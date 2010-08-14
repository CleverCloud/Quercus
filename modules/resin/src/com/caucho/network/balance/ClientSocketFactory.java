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

package com.caucho.network.balance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.meter.ActiveMeter;
import com.caucho.env.meter.ActiveTimeMeter;
import com.caucho.env.meter.CountMeter;
import com.caucho.env.meter.MeterService;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.ReadWritePair;
import com.caucho.vfs.Vfs;

/**
 * A pool of connections to a server.
 */
public class ClientSocketFactory
{
  private static final Logger log
    = Logger.getLogger(ClientSocketFactory.class.getName());
  private static final L10N L = new L10N(ClientSocketFactory.class);

  // number of chunks in the throttling
  private static final int WARMUP_MAX = 16;
  private static final int WARMUP_MIN = -16;
  private static final int []WARMUP_CONNECTION_MAX
    = new int[] { 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 8, 8, 16, 32, 64, 128 };

  private final String _sourceId;
  private final String _targetId;

  private final String _address;
  private final int _port;
  private final boolean _isSecure;

  private String _debugId;
  private String _statCategory;
  private String _statId;

  private Path _tcpPath;

  private int _maxConnections = Integer.MAX_VALUE / 2;

  private long _loadBalanceConnectTimeout = 5000;
  private long _loadBalanceConnectionMin = 0;
  private long _loadBalanceSocketTimeout = 30000;
  private long _loadBalanceIdleTime = 10000;
  private long _loadBalanceRecoverTime = 15000;
  private long _loadBalanceWarmupTime = 60000;
  private int _loadBalanceWeight = 100;

  private ClientSocket []_idle = new ClientSocket[64];
  private volatile int _idleHead;
  private volatile int _idleTail;
  private int _idleSize = 16;

  private int _streamCount;

  private long _warmupChunkTime;

  private long _failChunkTime;

  private volatile State _state = State.NEW;

  // server start/stop sequence for heartbeat/restarts
  private final AtomicInteger _startSequenceId
    = new AtomicInteger();

  // current connection count
  private final AtomicInteger _activeCount = new AtomicInteger();
  private final AtomicInteger _startingCount = new AtomicInteger();

  private final AtomicInteger _loadBalanceAllocateCount = new AtomicInteger();

  // numeric value representing the throttle state
  private volatile int _warmupState;
  private volatile int _currentFailCount;

  // load management data
  private volatile long _lastFailConnectTime;
  private volatile long _dynamicFailRecoverTime = 1000L;

  private volatile long _lastFailTime;
  private volatile long _lastBusyTime;

  private volatile long _failTime;
  private volatile long _firstSuccessTime;
  private volatile long _lastSuccessTime;
  private volatile long _prevSuccessTime;
  private volatile double _latencyFactor;

  // statistics
  private ActiveTimeMeter _requestTimeProbe;
  private ActiveMeter _connProbe;
  private ActiveMeter _idleProbe;
  private CountMeter _connFailProbe;
  private CountMeter _requestFailProbe;
  private CountMeter _requestBusyProbe;

  private volatile long _keepaliveCountTotal;
  private volatile long _connectCountTotal;
  private final AtomicLong _failCountTotal = new AtomicLong();
  private volatile long _busyCountTotal;

  private volatile double _cpuLoadAvg;
  private volatile long _cpuSetTime;
  
  public ClientSocketFactory(String address, int port)
  {
    this(address, port, false);
  }
  
  public ClientSocketFactory(String address, int port, boolean isSecure)
  {
    this("client", address + ":" + port, null, null, address, port, isSecure); 
  }

  public ClientSocketFactory(String sourceId,
                             String targetId,
                             String statCategory,
                             String statId,
                             String address,
                             int port,
                             boolean isSecure)
  {
    _sourceId = sourceId;

    if ("".equals(targetId))
      targetId = "default";

    _targetId = targetId;
    _debugId = _sourceId + "->" + _targetId;
    _address = address;    
    _port = port;
    _isSecure = isSecure;

    _statCategory = statCategory;

    if (statId != null && ! "".equals(statId) && ! statId.startsWith("|"))
      statId = "|" + statId;

    _statId = statId;
  }

  /**
   * Returns the user-readable id of the target server.
   */
  public String getId()
  {
    return _targetId;
  }

  /**
   * Returns the debug id.
   */
  public String getDebugId()
  {
    return _debugId;
  }

  /**
   * Returns the hostname of the target server.
   */
  public String getAddress()
  {
    return _address;
  }

  /**
   * Gets the port of the target server.
   */
  public int getPort()
  {
    return _port;
  }

  /**
   * The socket timeout when connecting to the target server.
   */
  public long getLoadBalanceConnectTimeout()
  {
    return _loadBalanceConnectTimeout;
  }

  /**
   * The socket timeout when connecting to the target server.
   */
  public void setLoadBalanceConnectTimeout(long timeout)
  {
    _loadBalanceConnectTimeout = timeout;
  }

  /**
   * The minimum connections for green load balancing.
   */
  public long getLoadBalanceConnectionMin()
  {
    return _loadBalanceConnectionMin;
  }

  /**
   * The minimum connections for green load balancing.
   */
  public void setLoadBalanceConnectionMin(int connectionMin)
  {
    _loadBalanceConnectionMin = connectionMin;
  }

  /**
   * The socket timeout when reading from the target server.
   */
  public long getLoadBalanceSocketTimeout()
  {
    return _loadBalanceSocketTimeout;
  }

  /**
   * The socket timeout when reading from the target server.
   */
  public void setLoadBalanceSocketTimeout(long timeout)
  {
    _loadBalanceSocketTimeout = timeout;
  }

  /**
   * How long the connection can be cached in the free pool.
   */
  public long getLoadBalanceIdleTime()
  {
    return _loadBalanceIdleTime;
  }

  /**
   * How long the connection can be cached in the free pool.
   */
  public void setLoadBalanceIdleTime(long timeout)
  {
    _loadBalanceIdleTime = timeout;
  }

  /**
   * Returns how long the connection will be treated as dead.
   */
  public void setLoadBalanceRecoverTime(long timeout)
  {
    _loadBalanceRecoverTime = timeout;
  }

  /**
   * Returns the time in milliseconds for the slow start throttling.
   */
  public void setLoadBalanceWarmupTime(long timeout)
  {
    _loadBalanceWarmupTime = timeout;
  }

  /**
   * The load balance weight.
   */
  public int getLoadBalanceWeight()
  {
    return _loadBalanceWeight;
  }

  /**
   * The load balance weight.
   */
  public void setLoadBalanceWeight(int weight)
  {
    _loadBalanceWeight = weight;
  }

  /**
   * Returns the server start/stop sequence. Each enable/disable
   * increments the sequence, allowing old streams to be purge.
   */
  public int getStartSequenceId()
  {
    return _startSequenceId.get();
  }

  /**
   * Initialize
   */
  public void init()
  {
    _warmupChunkTime = _loadBalanceWarmupTime / WARMUP_MAX;
    if (_warmupChunkTime <= 0)
      _warmupChunkTime = 1;

    _failChunkTime = _loadBalanceRecoverTime / WARMUP_MAX;
    if (_failChunkTime <= 0)
      _failChunkTime = 1;

    String address = getAddress();

    if (address == null)
      address = "localhost";

    HashMap<String,Object> attr = new HashMap<String,Object>();
    attr.put("connect-timeout", _loadBalanceConnectTimeout);
    attr.put("socket-timeout", _loadBalanceSocketTimeout);
    attr.put("no-delay", true);

    if (_isSecure)
      _tcpPath = Vfs.lookup("tcps://" + address + ":" + _port, attr);
    else
      _tcpPath = Vfs.lookup("tcp://" + address + ":" + _port, attr);

    _state = State.STARTING;
  }

  //
  // statistics
  //

  /**
   * Returns the number of active connections.
   */
  public int getActiveCount()
  {
    return _activeCount.get();
  }

  /**
   * Returns the number of idle connections.
   */
  public int getIdleCount()
  {
    return (_idleHead - _idleTail + _idle.length) % _idle.length;
  }

  /**
   * Returns the number of load balance allocations
   */
  public int getLoadBalanceAllocateCount()
  {
    return _loadBalanceAllocateCount.get();
  }

  /**
   * Allocate a connection for load balancing.
   */
  public void allocateLoadBalance()
  {
    _loadBalanceAllocateCount.incrementAndGet();
  }

  /**
   * Free a connection for load balancing.
   */
  public void freeLoadBalance()
  {
    _loadBalanceAllocateCount.decrementAndGet();
  }

  /**
   * Returns the total number of successful socket connections
   */
  public long getConnectCountTotal()
  {
    return _connectCountTotal;
  }

  /**
   * Returns the number of times a keepalive connection has been used.
   */
  public long getKeepaliveCountTotal()
  {
    return _keepaliveCountTotal;
  }

  /**
   * Returns the total number of failed connect attempts.
   */
  public long getFailCountTotal()
  {
    return _failCountTotal.get();
  }

  /**
   * Returns the time of the last failure.
   */
  public Date getLastFailTime()
  {
    return new Date(_lastFailTime);
  }

  /**
   * Returns the time of the last failure.
   */
  public Date getLastFailConnectTime()
  {
    return new Date(_lastFailConnectTime);
  }

  /**
   * Returns the time of the last failure.
   */
  public long getLastSuccessTime()
  {
    return _lastSuccessTime;
  }

  /**
   * Returns the latency factory
   */
  public double getLatencyFactor()
  {
    long now = Alarm.getCurrentTime();

    long decayPeriod = 60000;

    long delta = decayPeriod - (now - _lastSuccessTime);

    // decay the latency factor over 60s
    
    if (delta <= 0)
      return 0;
    else
      return (_latencyFactor * delta) / decayPeriod;
  }

  /**
   * Returns the count of busy connections.
   */
  public long getBusyCountTotal()
  {
    return _busyCountTotal;
  }

  /**
   * Returns the time of the last busy.
   */
  public Date getLastBusyTime()
  {
    return new Date(_lastBusyTime);
  }

  /**
   * Sets the CPU load avg (from backend).
   */
  public void setCpuLoadAvg(double load)
  {
    _cpuSetTime = Alarm.getCurrentTime();
    _cpuLoadAvg = load;
  }

  /**
   * Gets the CPU load avg
   */
  public double getCpuLoadAvg()
  {
    double avg = _cpuLoadAvg;
    long time = _cpuSetTime;

    long now = Alarm.getCurrentTime();

    if (now - time < 10000L)
      return avg;
    else
      return avg * 10000L / (now - time);
  }

  /**
   * Returns true if the server is active.
   */
  public final boolean isActive()
  {
    return _state.isLive();
  }

  /**
   * Returns true if the server is dead.
   */
  public boolean isDead()
  {
    return ! isActive();
  }

  /**
   * Enable the client
   */
  public void enable()
  {
    start();
  }

  /**
   * Disable the client
   */
  public void disable()
  {
    stop();
  }

  /**
   * Returns the lifecycle state.
   */
  public String getState()
  {
    updateWarmup();

    return String.valueOf(_state);
  }

  /**
   * Returns true if the server can open a connection.
   */
  public boolean canOpen()
  {
    if (getIdleCount() > 0)
      return true;
    State state = _state;

    if (state == State.ACTIVE)
      return true;
    else if (! state.isEnabled())
      return false;
    else {
      long now = Alarm.getCurrentTime();

      if (now < _lastFailConnectTime + _dynamicFailRecoverTime) {
        return false;
      }

      return true;
    }
  }

  /**
   * Returns true if the server can open a connection.
   */
  public boolean canOpenWarmOrRecycle()
  {
    return getIdleCount() > 0 || canOpenWarm();
  }

  /**
   * Returns true if the server can open a connection.
   */
  public boolean canOpenWarm()
  {
    State state = _state;

    if (state == State.ACTIVE)
      return true;
    else if (state.isEnabled()) {
      long now = Alarm.getCurrentTime();

      if (now < _lastFailConnectTime + _dynamicFailRecoverTime) {
        return false;
      }

      long firstSuccessTime = _firstSuccessTime;
      int warmupState = 0;

      if (firstSuccessTime > 0)
        warmupState = (int) ((now - firstSuccessTime) / _warmupChunkTime);

      warmupState -= _currentFailCount;

      if (warmupState < 0) {
        return (_failTime - warmupState * _failChunkTime < now);
      }
      else if (WARMUP_MAX <= warmupState)
        return true;

      int connectionMax = WARMUP_CONNECTION_MAX[warmupState];

      int idleCount = getIdleCount();
      int activeCount = _activeCount.get() + _startingCount.get();
      int totalCount = activeCount + idleCount;
      
      return totalCount < connectionMax;
    }
    else {
      return false;
    }
  }

  /**
   * Return true if active.
   */
  public boolean isEnabled()
  {
    return _state.isEnabled();
  }

  public void toBusy()
  {
    _lastBusyTime = Alarm.getCurrentTime();
    _firstSuccessTime = 0;

    _requestBusyProbe.start();

    synchronized (this) {
      _busyCountTotal++;

      _state = _state.toBusy();
    }
  }

  public void toFail()
  {
    _failTime = Alarm.getCurrentTime();
    _lastFailTime = _failTime;
    _firstSuccessTime = 0;

    getRequestFailProbe().start();

    _failCountTotal.incrementAndGet();

    _state = _state.toFail();

    clearRecycle();
  }

  /**
   * Called when the socket read/write fails.
   */
  public void failSocket()
  {
    getRequestFailProbe().start();

    _failCountTotal.incrementAndGet();
    
    synchronized (this) {
      long now = Alarm.getCurrentTime();
      _firstSuccessTime = 0;

      // only degrade one per 100ms
      if (now - _failTime >= 100) {
        _currentFailCount++;
        _warmupState--;
        _failTime = now;
        _lastFailTime = _failTime;
      }

      if (_warmupState < WARMUP_MIN)
        _warmupState = WARMUP_MIN;

      _state = _state.toFail();
    }
  }

  /**
   * Called when the socket read/write fails.
   */
  public void failConnect()
  {
    getConnectionFailProbe().start();

    _failCountTotal.incrementAndGet();

    synchronized (this) {
      _firstSuccessTime = 0;

      // only degrade one per 100ms
      _currentFailCount++;
      _warmupState--;
      long now = Alarm.getCurrentTime();
      _failTime = now;
      _lastFailTime = now;
      _lastFailConnectTime = now;
      _dynamicFailRecoverTime *= 2;
      if (_loadBalanceRecoverTime < _dynamicFailRecoverTime)
        _dynamicFailRecoverTime = _loadBalanceRecoverTime;

      if (_warmupState < WARMUP_MIN)
        _warmupState = WARMUP_MIN;

      _state = _state.toFail();
    }
  }

  /**
   * Called when the server responds with "busy", e.g. HTTP 503
   */
  public void busy()
  {
    getRequestBusyProbe().start();

    synchronized (this) {
      _lastBusyTime = Alarm.getCurrentTime();
      _firstSuccessTime = 0;

      _currentFailCount++;
      _warmupState--;
      if (_warmupState < 0)
        _warmupState = 0;

      _busyCountTotal++;

      _state = _state.toBusy();
    }
  }

  /**
   * Called when the server has a successful response
   */
  public void success()
  {
    _currentFailCount = 0;

    if (_firstSuccessTime <= 0) {
      _firstSuccessTime = Alarm.getCurrentTime();
    }

    // reset the connection fail recover time
    _dynamicFailRecoverTime = 1000L;
  }

  /**
   * Enable the client.
   */
  public void start()
  {
    // State state = _state;
    
    _state = _state.toStart();

    /*
    if (state != State.ACTIVE)
      _startSequenceId.incrementAndGet();
      */
  }

  /**
   * Disable the client.
   */
  public void stop()
  {
    _state = _state.toStandby();
    _firstSuccessTime = 0;

    _startSequenceId.incrementAndGet();
    clearRecycle();
  }

  /**
   * Session only
   */
  public void enableSessionOnly()
  {
    _state = _state.toSessionOnly();
  }

  /**
   * Open a stream to the target server, restricted by warmup.
   *
   * @return the socket's read/write pair.
   */
  public ClientSocket openWarm()
  {
    State state = _state;

    if (! state.isEnabled()) {
      return null;
    }

    ClientSocket stream = openRecycle();

    if (stream != null)
      return stream;

    if (canOpenWarm()) {
      return connect();
    }
    else {
      return null;
    }
  }

  /**
   * Open a stream to the target server object persistence.
   *
   * @return the socket's read/write pair.
   */
  public ClientSocket openIfLive()
  {
    if (_state == State.CLOSED) {
      return null;
    }

    ClientSocket stream = openRecycle();

    if (stream != null)
      return stream;

    long now = Alarm.getCurrentTime();

    if (now <= _failTime + _loadBalanceRecoverTime)
      return null;
    else if (_state == State.FAIL && _startingCount.get() > 0) {
      // if in fail state, only one thread should try to connect
      return null;
    }

    return connect();
  }

  /**
   * Open a stream to the target server for a session.
   *
   * @return the socket's read/write pair.
   */
  public ClientSocket openSticky()
  {
    State state = _state;
    if (! state.isSessionEnabled()) {
      return null;
    }

    ClientSocket stream = openRecycle();

    if (stream != null)
      return stream;

    long now = Alarm.getCurrentTime();

    if (now < _failTime + _loadBalanceRecoverTime) {
      return null;
    }

    if (now < _lastBusyTime + _loadBalanceRecoverTime) {
      return null;
    }

    return connect();
  }

  /**
   * Open a stream to the target server for the load balancer.
   *
   * @return the socket's read/write pair.
   */
  public ClientSocket open()
  {
    State state = _state;
    if (! state.isInit())
      return null;

    ClientSocket stream = openRecycle();

    if (stream != null)
      return stream;

    return connect();
  }

  /**
   * Returns a valid recycled stream from the idle pool to the backend.
   *
   * If the stream has been in the pool for too long (> live_time),
   * close it instead.
   *
   * @return the socket's read/write pair.
   */
  private ClientSocket openRecycle()
  {
    long now = Alarm.getCurrentTime();
    ClientSocket stream = null;

    synchronized (this) {
      if (_idleHead != _idleTail) {
        stream = _idle[_idleHead];
        long freeTime = stream.getIdleStartTime();

        _idle[_idleHead] = null;
        _idleHead = (_idleHead + _idle.length - 1) % _idle.length;

        if (now < freeTime + _loadBalanceIdleTime) {
          _activeCount.incrementAndGet();
          _keepaliveCountTotal++;

          stream.clearIdleStartTime();
          stream.toActive();

          return stream;
        }
      }
    }

    if (stream != null) {
      if (log.isLoggable(Level.FINER))
        log.finer(this + " close idle " + stream
                  + " expire=" + QDate.formatISO8601(stream.getIdleStartTime() + _loadBalanceIdleTime));

      stream.closeImpl();
    }

    return null;
  }

  /**
   * Connect to the backend server.
   *
   * @return the socket's read/write pair.
   */
  private ClientSocket connect()
  {
    if (_maxConnections <= _activeCount.get() + _startingCount.get())
      return null;

    _startingCount.incrementAndGet();

    State state = _state;
    if (! state.isInit()) {
      _startingCount.decrementAndGet();
      
      IllegalStateException e = new IllegalStateException(L.l("'{0}' connection cannot be opened because the server pool has not been started.", this));

      log.log(Level.WARNING, e.toString(), e);

      throw e;
    }

    try {
      ReadWritePair pair = openTCPPair();
      ReadStream rs = pair.getReadStream();
      rs.setEnableReadTime(true);
      
      rs.setAttribute("timeout", new Integer((int) _loadBalanceSocketTimeout));

      _activeCount.incrementAndGet();
      
      synchronized (this) {
        _connectCountTotal++;
      }

      ClientSocket stream = new ClientSocket(this, _streamCount++,
                                               rs, pair.getWriteStream());

      if (log.isLoggable(Level.FINER))
        log.finer("connect " + stream);

      if (_firstSuccessTime <= 0) {
        if (_state.isStarting()) {
          if (_loadBalanceWarmupTime > 0)
            _state = State.WARMUP;
          else
            _state = State.ACTIVE;

          _firstSuccessTime = Alarm.getCurrentTime();
        }

        if (_warmupState < 0)
          _warmupState = 0;
      }

      return stream;
    } catch (IOException e) {
      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, this + " " + e.toString(), e);
      else
        log.finer(this + " " + e.toString());

      failConnect();

      return null;
    } finally {
      _startingCount.decrementAndGet();
    }
  }

  /**
   * We now know that the server is live, e.g. if a sibling has
   * contacted us.
   */
  public void wake()
  {
    synchronized (this) {
      if (_state == State.FAIL) {
        _state = State.STARTING;
      }

      _failTime = 0;
    }
  }

  /**
   * Free the read/write pair for reuse.  Called only from
   * ClusterStream.free().
   *
   * @param stream the stream to free
   */
  void free(ClientSocket stream)
  {
    success();

    _activeCount.decrementAndGet();

    synchronized (this) {
      int size = (_idleHead - _idleTail + _idle.length) % _idle.length;

      if (_state != State.CLOSED && size < _idleSize) {
        _idleHead = (_idleHead + 1) % _idle.length;
        _idle[_idleHead] = stream;

        stream = null;
      }

      long now = Alarm.getCurrentTime();

      long prevSuccessTime = _prevSuccessTime;

      if (prevSuccessTime > 0) {
        _latencyFactor = (0.95 * _latencyFactor
                          + 0.05 * (now - prevSuccessTime));
      }

      if (_activeCount.get() > 0)
        _prevSuccessTime = now;
      else
        _prevSuccessTime = 0;

      _lastSuccessTime = now;
    }

    updateWarmup();

    long now = Alarm.getCurrentTime();
    long maxIdleTime = _loadBalanceIdleTime;
    ClientSocket oldStream = null;

    do {
      oldStream = null;

      synchronized (this) {
        if (_idleHead != _idleTail) {
          int nextTail = (_idleTail + 1) % _idle.length;

          oldStream = _idle[nextTail];

          if (oldStream != null
              && oldStream.getIdleStartTime() + maxIdleTime < now) {
            _idle[nextTail] = null;
            _idleTail = nextTail;
          }
          else
            oldStream = null;
        }
      }

      if (oldStream != null)
        oldStream.closeImpl();
    } while (oldStream != null);

    if (stream != null) {
      stream.closeImpl();
    }
  }

  private void updateWarmup()
  {
    synchronized (this) {
      if (! isEnabled())
        return;

      long now = Alarm.getCurrentTime();
      int warmupState = _warmupState;

      if (warmupState >= 0 && _firstSuccessTime > 0) {
        warmupState = (int) ((now - _firstSuccessTime) / _warmupChunkTime);

        // reset the connection fail recover time
        _dynamicFailRecoverTime = 1000L;

        if (WARMUP_MAX <= warmupState) {
          warmupState = WARMUP_MAX;
          _state = _state.toActive();
        }
      }

      _warmupState = warmupState;
    }
  }

  /**
   * Closes the read/write pair for reuse.  Called only
   * from ClusterStream.close().
   */
  void close(ClientSocket stream)
  {
    if (log.isLoggable(Level.FINER))
      log.finer("close " + stream);

    _activeCount.decrementAndGet();
  }

  /**
   * Notify that a start has occurred.
   */
  public void notifyStart()
  {
    // _startSequenceId.incrementAndGet();

    clearRecycle();
    wake();
  }

  /**
   * Notify that a stop has occurred.
   */
  public void notifyStop()
  {
    _startSequenceId.incrementAndGet();

    clearRecycle();
    toFail();
  }

  /**
   * Clears the recycled connections, e.g. on detection of backend
   * server going down.
   */
  public void clearRecycle()
  {
    ArrayList<ClientSocket> recycleList = null;

    synchronized (this) {
      _idleHead = _idleTail = 0;

      for (int i = 0; i < _idle.length; i++) {
        ClientSocket stream;

        stream = _idle[i];
        _idle[i] = null;

        if (stream != null) {
          if (recycleList == null)
            recycleList = new ArrayList<ClientSocket>();

          recycleList.add(stream);
        }
      }
    }

    if (recycleList != null) {
      for (ClientSocket stream : recycleList) {
        stream.closeImpl();
      }
    }
  }

  /**
   * Close the client
   */
  public void close()
  {
    synchronized (this) {
      if (_state == State.CLOSED)
        return;

      _state = State.CLOSED;
    }

    synchronized (this) {
      _idleHead = _idleTail = 0;
    }

    for (int i = 0; i < _idle.length; i++) {
      ClientSocket stream;

      synchronized (this) {
        stream = _idle[i];
        _idle[i] = null;
      }

      if (stream != null)
        stream.closeImpl();
    }
  }

  /**
   * Open a read/write pair to the target srun connection.
   *
   * @return the socket's read/write pair.
   */
  ReadWritePair openTCPPair()
    throws IOException
  {
    return _tcpPath.openReadWrite();
  }

  /**
   * Returns true if can connect to the client.
   */
  public boolean canConnect()
  {
    try {
      wake();

      ClientSocket stream = open();

      if (stream != null) {
        stream.free(stream.getIdleStartTime());

        return true;
      }

      return false;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);

      return false;
    }
  }

  //
  // BAM API
  //

  //
  // statistics
  //

  public ActiveMeter getConnectionProbe()
  {
    if (_connProbe == null) {
      _connProbe
        = MeterService.createActiveMeter(_statCategory + "|Connection",
                                         _statId);
    }

    return _connProbe;
  }

  public CountMeter getConnectionFailProbe()
  {
    if (_connFailProbe == null) {
      String name = _statCategory + "|Connection Fail|" + _statId;
      _connFailProbe = MeterService.createCountMeter(name);
    }

    return _connFailProbe;
  }

  public ActiveTimeMeter getRequestTimeProbe()
  {
    if (_requestTimeProbe == null) {
      _requestTimeProbe
        = MeterService.createActiveTimeMeter(_statCategory + "|Request",
                                             "Time", _statId);
    }

    return _requestTimeProbe;
  }

  public CountMeter getRequestFailProbe()
  {
    if (_requestFailProbe == null) {
      String name = _statCategory + "|Request Fail" + _statId;
      _requestFailProbe = MeterService.createCountMeter(name);
    }

    return _requestFailProbe;
  }

  public CountMeter getRequestBusyProbe()
  {
    if (_requestBusyProbe == null) {
      String name = _statCategory + "|Request Busy" + _statId;
      _requestBusyProbe = MeterService.createCountMeter(name);
    }

    return _requestBusyProbe;
  }

  public ActiveMeter getIdleProbe()
  {
    if (_idleProbe == null) {
      _idleProbe
        = MeterService.createActiveMeter(_statCategory + "|Idle",
                                         _statId);
    }

    return _idleProbe;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + getDebugId()
            + "," + _address + ":" + _port + "]");
  }

  enum State {
    NEW {
      boolean isInit() { return false; }
      boolean isEnabled() { return false; }
      boolean isSessionEnabled() { return false; }
    },
    STANDBY {
      boolean isEnabled() { return false; }
      boolean isSessionEnabled() { return false; }

      State toFail() { return this; }
      State toBusy() { return this; }
      State toSessionOnly() { return this; }
    },
    SESSION_ONLY {
      boolean isEnabled() { return false; }

      // XXX: should change to standby?
      State toFail() { return this; }
      State toBusy() { return this; }
    },
    // the following 5 are the active states
    STARTING {
      boolean isStarting() { return true; }
      boolean isLive() { return true; }
    },
    WARMUP {
      boolean isStarting() { return true; }
      boolean isLive() { return true; }

      State toStart() { return this; }
    },
    BUSY {
      boolean isStarting() { return true; }
    },
    FAIL {
      boolean isStarting() { return true; }
    },
    ACTIVE {
      boolean isLive() { return true; }

      State toStart() { return this; }
    },
    CLOSED {
      boolean isInit() { return false; }
      boolean isClosed() { return true; }
      boolean isSessionEnabled() { return false; }
      boolean isEnabled() { return false; }

      State toStart() { return this; }
      State toActive() { return this; }
      State toBusy() { return this; }
      State toFail() { return this; }
      State toStandby() { return this; }
      State toSessionOnly() { return this; }
    };

    boolean isInit() { return true; }
    boolean isClosed() { return false; }
    boolean isStarting() { return false; }
    boolean isLive() { return true; }
    boolean isSessionEnabled() { return true; }
    boolean isEnabled() { return true; }

    State toStart() { return STARTING; }
    State toActive() { return ACTIVE; }
    State toFail() { return FAIL; }
    State toBusy() { return BUSY; }
    State toStandby() { return STANDBY; }
    State toSessionOnly() { return SESSION_ONLY; }
  }
}
