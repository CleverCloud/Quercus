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

package com.caucho.transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ValidatingManagedConnectionFactory;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.env.meter.ActiveTimeMeter;
import com.caucho.env.meter.MeterService;
import com.caucho.inject.Module;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.ConnectionPoolMXBean;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.util.WeakAlarm;

/**
 * Implementation of the connection manager.
 */
@Module
public class ConnectionPool extends AbstractManagedObject
  implements ConnectionManager, AlarmListener, ConnectionPoolMXBean
{
  private static final L10N L = new L10N(ConnectionPool.class);
  private static final Logger log
    = Logger.getLogger(ConnectionPool.class.getName());

  private final AtomicInteger _idGen
    = new AtomicInteger();

  private String _name;

  private UserTransactionProxy _tm;

  // the maximum number of connections
  private int _maxConnections = 1024;

  // the maximum number of overflow connections
  private int _maxOverflowConnections = 1024;

  // the maximum number of connections in the process of creation
  private int _maxCreateConnections = 5;

  // max idle size
  private int _maxIdleCount = 1024;

  // time before an idle connection is closed (30s default)
  private long _idleTimeout = 30000L;

  // time before an active connection is closed (6h default)
  private long _activeTimeout = 6L * 3600L * 1000L;

  // time a connection is allowed to be used (24h default)
  private long _poolTimeout = 24L * 3600L * 1000L;

  // the time to wait for a connection (30s)
  private long _connectionWaitTimeout = 30 * 1000L;

  // True if the connector supports local transactions.
  private boolean _isEnableLocalTransaction = true;

  // True if the connector supports XA transactions.
  private boolean _isEnableXA = true;

  // True if the local transaction optimization is allowed.
  private boolean _isLocalTransactionOptimization = true;

    // server/3087
  private boolean _isShareable = true;

  // If true, the save a stack trace when the collection is allocated
  private boolean _isSaveAllocationStackTrace = false;

  // If true, close dangling connections
  private boolean _isCloseDanglingConnections = true;

  private final ArrayList<ManagedPoolItem> _connectionPool
    = new ArrayList<ManagedPoolItem>();

  private IdlePoolSet _idlePool;

  // temporary connection list for the alarm callback
  private final ArrayList<ManagedPoolItem> _alarmConnections
    = new ArrayList<ManagedPoolItem>();

  private Alarm _alarm;

  // time of the last validation check
  private long _lastValidCheckTime;
  // time the idle set was last empty
  private long _idlePoolExpire;

  private final AtomicInteger _idCount = new AtomicInteger();

  // connections available for reuse or creation, i.e. the idle count
  // plus the available createCount
  private final Object _availableLock = new Object();
  private final AtomicInteger _availableWaitCount = new AtomicInteger();
  
  private final AtomicInteger _createCount = new AtomicInteger();

  //
  // statistics
  //

  private ActiveTimeMeter _connectionTime;
  private ActiveTimeMeter _idleTime;
  private ActiveTimeMeter _activeTime;

  private final AtomicLong _connectionCountTotal = new AtomicLong();
  private final AtomicLong _connectionCreateCountTotal = new AtomicLong();
  private final AtomicLong _connectionFailCountTotal = new AtomicLong();
  private long _lastFailTime;

  private final Lifecycle _lifecycle = new Lifecycle();

  public ConnectionPool()
  {
  }

  /**
   * Sets the connection pool name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the connection pool name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the transaction manager.
   */
  public void setTransactionManager(UserTransactionProxy tm)
  {
    _tm = tm;
  }

  /**
   * Returns the transaction manager.
   */
  public UserTransactionProxy getTransactionManager()
  {
    return _tm;
  }

  /**
   * Returns true if shared connections are allowed.
   */
  public boolean isShareable()
  {
    return _isShareable;
  }

  /**
   * Returns true if shared connections are allowed.
   */
  public void setShareable(boolean isShareable)
  {
    _isShareable = isShareable;
  }

  /**
   * Returns true if the local transaction optimization is enabled
   */
  public boolean isLocalTransactionOptimization()
  {
    return _isLocalTransactionOptimization;
  }

  /**
   * Returns true if the local transaction optimization is enabled
   */
  public void setLocalTransactionOptimization(boolean enable)
  {
    _isLocalTransactionOptimization = enable;
  }

  /**
   * Returns true if the local transaction optimization is enabled
   */
  public boolean allowLocalTransactionOptimization()
  {
    return _isLocalTransactionOptimization && _isShareable;
  }

  /**
   * Returns true if a stack trace should be shared on allocation
   */
  public boolean getSaveAllocationStackTrace()
  {
    return _isSaveAllocationStackTrace;
  }

  /**
   * Returns true if a stack trace should be shared on allocation
   */
  public void setSaveAllocationStackTrace(boolean save)
  {
    _isSaveAllocationStackTrace = save;
  }

  /**
   * Returns true if dangling connections should be closed
   */
  public boolean isCloseDanglingConnections()
  {
    return _isCloseDanglingConnections;
  }

  /**
   * True if dangling connections should be closed.
   */
  public void setCloseDanglingConnections(boolean isClose)
  {
    _isCloseDanglingConnections = isClose;
  }

  /**
   * Set true for local transaction support.
   */
  public void setLocalTransaction(boolean localTransaction)
  {
    _isEnableLocalTransaction = localTransaction;
  }

  /**
   * Set true for local transaction support.
   */
  public boolean isLocalTransaction()
  {
    return _isEnableLocalTransaction;
  }

  /**
   * Set true for XA transaction support.
   */
  public void setXATransaction(boolean enable)
  {
    _isEnableXA = enable;
  }

  /**
   * Set true for XA transaction support.
   */
  public boolean isXATransaction()
  {
    return _isEnableXA;
  }

  /**
   * Returns the max idle time.
   */
  public long getMaxIdleTime()
  {
    if (Long.MAX_VALUE / 2 <= _idleTimeout)
      return -1;
    else
      return _idleTimeout;
  }

  /**
   * Sets the max idle time.
   */
  public void setMaxIdleTime(long maxIdleTime)
  {
    if (maxIdleTime < 0)
      _idleTimeout = Long.MAX_VALUE / 2;
    else
      _idleTimeout = maxIdleTime;
  }

  /**
   * Returns the max idle count.
   */
  public int getMaxIdleCount()
  {
    return _maxIdleCount;
  }

  /**
   * Sets the max idle count.
   */
  public void setMaxIdleCount(int maxIdleCount)
  {
    if (maxIdleCount < 0)
      _maxIdleCount = 0;
    else
      _maxIdleCount = maxIdleCount;
  }

  /**
   * Returns the max active time.
   */
  public long getMaxActiveTime()
  {
    if (Long.MAX_VALUE / 2 <= _activeTimeout)
      return -1;
    else
      return _activeTimeout;
  }

  /**
   * Sets the max active time.
   */
  public void setMaxActiveTime(long maxActiveTime)
  {
    if (maxActiveTime < 0)
      _activeTimeout = Long.MAX_VALUE / 2;
    else
      _activeTimeout = maxActiveTime;
  }

  /**
   * Returns the max pool time.
   */
  public long getMaxPoolTime()
  {
    if (Long.MAX_VALUE / 2 <= _poolTimeout)
      return -1;
    else
      return _poolTimeout;
  }

  /**
   * Sets the max pool time.
   */
  public void setMaxPoolTime(long maxPoolTime)
  {
    if (maxPoolTime < 0)
      _poolTimeout = Long.MAX_VALUE / 2;
    else
      _poolTimeout = maxPoolTime;
  }

  /**
   * Sets the max number of connections
   */
  public void setMaxConnections(int maxConnections)
    throws ConfigException
  {
    if (maxConnections == 0)
      throw new ConfigException(L.l("max-connections '0' must be at least 1."));

    _maxConnections = maxConnections;

    if (maxConnections < 0)
      _maxConnections = Integer.MAX_VALUE / 2;
  }

  /**
   * Gets the maximum number of connections
   */
  public int getMaxConnections()
  {
    if (_maxConnections < Integer.MAX_VALUE / 2)
      return _maxConnections;
    else
      return -1;
  }

  /**
   * Sets the time to wait for connections
   */
  public void setConnectionWaitTime(Period waitTime)
  {
    _connectionWaitTimeout = waitTime.getPeriod();

    if (_connectionWaitTimeout < 0)
      _connectionWaitTimeout = Long.MAX_VALUE / 2;
  }

  /**
   * Sets the time to wait for connections
   */
  public long getConnectionWaitTime()
  {
    if (_connectionWaitTimeout < Long.MAX_VALUE / 2)
      return _connectionWaitTimeout;
    else
      return -1;
  }

  /**
   * Sets the max number of overflow connections
   */
  public void setMaxOverflowConnections(int maxOverflowConnections)
  {
    _maxOverflowConnections = maxOverflowConnections;
  }

  /**
   * Gets the max number of overflow connections
   */
  public int getMaxOverflowConnections()
  {
    return _maxOverflowConnections;
  }

  /**
   * Sets the max number of connections simultaneously creating
   */
  public void setMaxCreateConnections(int maxConnections)
    throws ConfigException
  {
    if (maxConnections == 0)
      throw new ConfigException(L.l("max-create-connections '0' must be at least 1."));

    _maxCreateConnections = maxConnections;

    if (maxConnections < 0)
      _maxCreateConnections = Integer.MAX_VALUE / 2;

  }

  /**
   * Gets the maximum number of connections simultaneously creating
   */
  public int getMaxCreateConnections()
  {
    if (_maxCreateConnections < Integer.MAX_VALUE / 2)
      return _maxCreateConnections;
    else
      return -1;
  }

  //
  // statistics
  //

  /**
   * Returns the connection time probe
   */
  public ActiveTimeMeter getConnectionTimeProbe()
  {
    return _connectionTime;
  }

  /**
   * Returns the idle time probe
   */
  public ActiveTimeMeter getIdleTimeProbe()
  {
    return _idleTime;
  }

  /**
   * Returns the active time probe
   */
  public ActiveTimeMeter getActiveTimeProbe()
  {
    return _activeTime;
  }

  /**
   * Returns the total connections.
   */
  public int getConnectionCount()
  {
    return _connectionPool.size();
  }

  /**
   * Returns the idle connections.
   */
  public int getConnectionIdleCount()
  {
    return _idlePool.size();
  }

  /**
   * Returns the active connections.
   */
  public int getConnectionActiveCount()
  {
    return _connectionPool.size() - _idlePool.size();
  }

  /**
   * Returns the total connections.
   */
  public long getConnectionCountTotal()
  {
    return _connectionCountTotal.get();
  }

  /**
   * Returns the total connections.
   */
  public long getConnectionCreateCountTotal()
  {
    return _connectionCreateCountTotal.get();
  }

  /**
   * Returns the total failed connections.
   */
  public long getConnectionFailCountTotal()
  {
    return _connectionFailCountTotal.get();
  }

  /**
   * Returns the last fail time
   */
  public Date getLastFailTime()
  {
    return new Date(_lastFailTime);
  }

  /**
   * Initialize the connection manager.
   */
  public Object init(ManagedConnectionFactory mcf)
    throws ConfigException, ResourceException
  {
    if (! _lifecycle.toInit())
      return null;

    if (_name == null) {
      int v = _idGen.incrementAndGet();

      _name = mcf.getClass().getSimpleName() + "-" + v;
    }

    if (_tm == null)
      throw new ConfigException(L.l("the connection manager needs a transaction manager."));

    _idlePool = new IdlePoolSet(_maxIdleCount);

    _connectionTime = MeterService.createActiveTimeMeter("Resin|Database|Connection");
    _idleTime = MeterService.createActiveTimeMeter("Resin|Database|Idle");
    _activeTime = MeterService.createActiveTimeMeter("Resin|Database|Active");

    registerSelf();

    _alarm = new WeakAlarm(this);

    if (! (mcf instanceof ValidatingManagedConnectionFactory)) {
      // never check
      _lastValidCheckTime = Long.MAX_VALUE / 2;
    }

    // recover any resources on startup
    if (_isEnableXA) {
      Subject subject = null;
      ManagedConnection mConn = mcf.createManagedConnection(subject, null);

      try {
        XAResource xa = mConn.getXAResource();

        _tm.recover(xa);
      } catch (NotSupportedException e) {
        _isEnableXA = false;
        log.finer(e.toString());
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
      } finally {
        mConn.destroy();
      }
    }

    return mcf.createConnectionFactory(this);
  }

  /**
   * start the connection manager.
   */
  public void start()
  {
    if (! _lifecycle.toActive())
      return;

    if (0 < _idleTimeout && _idleTimeout < 1000)
      _alarm.queue(1000);
    else if (1000 < _idleTimeout && _idleTimeout < 60000)
      _alarm.queue(_idleTimeout);
    else
      _alarm.queue(60000);
  }

  /**
   * Generates a connection id.
   */
  String generateId()
  {
    return String.valueOf(_idCount.getAndIncrement());
  }

  /**
   * Returns the transaction.
   */
  UserTransactionImpl getTransaction()
  {
    return _tm.getUserTransaction();
  }

  /**
   * Allocates the connection.
   *
   * @return connection handle for EIS specific connection.
   */
  public Object allocateConnection(ManagedConnectionFactory mcf,
                                   ConnectionRequestInfo info)
    throws ResourceException
  {
    Subject subject = null;

    Object conn = allocateConnection(mcf, subject, info);

    _connectionCountTotal.incrementAndGet();

    return conn;
  }

  /**
   * Allocates a connection.
   */
  private Object allocateConnection(ManagedConnectionFactory mcf,
                                    Subject subject,
                                    ConnectionRequestInfo info)
    throws ResourceException
  {
    UserPoolItem userPoolItem = null;

    try {
      UserTransactionImpl transaction = _tm.getUserTransaction();

      if (transaction != null)
        userPoolItem = transaction.allocate(mcf, subject, info);

      if (userPoolItem == null)
        userPoolItem = allocatePoolConnection(mcf, subject, info, null);

      Object dataSource = userPoolItem.allocateUserConnection();

      userPoolItem = null;

      return dataSource;
    } finally {
      if (userPoolItem != null)
        userPoolItem.close();
    }
  }

  /**
   * Allocates the pool item for a connection, creating one if necessary.
   * 
   * @param mcf the driver's ManagedConnectionFactory for creating pooled
   *   connections
   * @param subject the user's authentication credentials
   * @param info the user's extra connection information
   */
  UserPoolItem allocatePoolConnection(ManagedConnectionFactory mcf,
                                      Subject subject,
                                      ConnectionRequestInfo info,
                                      UserPoolItem oldPoolItem)
    throws ResourceException
  {
    long expireTime = Alarm.getCurrentTimeActual() + _connectionWaitTimeout;

    if (! _lifecycle.isActive())
      throw new IllegalStateException(L.l("Can't allocate connection because the connection pool is closed."));

    do {
      UserPoolItem userPoolItem
        = allocateIdleConnection(mcf, subject, info, oldPoolItem);

      if (userPoolItem != null)
        return userPoolItem;

      // if no item in pool, try to create one
      if (startCreateConnection()) {
        try {
          return createConnection(mcf, subject, info, oldPoolItem);
        } finally {
          finishCreateConnection();
        }
      }
    } while (_lifecycle.isActive()
             && waitForAvailableConnection(expireTime));
    
    if (! _lifecycle.isActive())
      throw new IllegalStateException(L.l("Can't allocate connection because the connection pool is closed."));

    log.warning(this + " pool overflow");

    // XXX: must restore
    /*
    Resin resin = Resin.getCurrent();

    if (resin != null) {
      resin.dumpThreads();
    }
    */

    if (startCreateOverflow()) {
      try {
        return createConnection(mcf, subject, info, oldPoolItem);
      } finally {
        finishCreateConnection();
      }
    }
    
    throw new ResourceException(L.l("Can't create overflow connection connection-max={0}",
                                    _maxConnections));
  }

  /**
   * Allocates a connection from the idle pool.
   */
  private UserPoolItem allocateIdleConnection(ManagedConnectionFactory mcf,
                                              Subject subject,
                                              ConnectionRequestInfo info,
                                              UserPoolItem oldPoolItem)
    throws ResourceException
  {
    while (_lifecycle.isActive()) {
      ManagedConnection mConn;

      long now = Alarm.getCurrentTime();

      if (_lastValidCheckTime + 1000L < now) {
        _lastValidCheckTime = now;

        if (mcf instanceof ValidatingManagedConnectionFactory) {
          ValidatingManagedConnectionFactory vmcf;
          vmcf = (ValidatingManagedConnectionFactory) mcf;

          validate(vmcf);
        }
      }

      ManagedPoolItem poolItem = null;

      while (true) {
        // asks the Driver's ManagedConnectionFactory to match an
        // idle connection
        synchronized (_connectionPool) {
          mConn = mcf.matchManagedConnections(_idlePool, subject, info);

          // If there are no more idle connections, return null
          if (mConn == null)
            return null;

          // remove can fail for threading reasons, so only succeed if it works.
          if (_idlePool.remove(mConn)) {
            poolItem = findPoolItem(mConn);
            
            if (poolItem == null)
              throw new IllegalStateException(L.l("Unexpected non-matching PoolItem found for {0}",
                                                  mConn));

            break;
          }
        }
      }

      try {
        // Ensure the connection is still valid
        UserPoolItem userPoolItem;
        userPoolItem = poolItem.toActive(subject, info, oldPoolItem);

        if (userPoolItem != null) {
          poolItem = null;
          return userPoolItem;
        }
      } finally {
        if (poolItem != null)
          poolItem.destroy();
      }
    }

    return null;
  }

  private ManagedPoolItem findPoolItem(ManagedConnection mConn)
  {
    synchronized (_connectionPool) {
      for (int i = _connectionPool.size() - 1; i >= 0; i--) {
        ManagedPoolItem testPoolItem = _connectionPool.get(i);

        if (testPoolItem.getManagedConnection() == mConn) {
          return testPoolItem;
        }
      }

      return null;
    }
  }

  /**
   * Validates the pool.
   */
  private void validate(ValidatingManagedConnectionFactory mcf)
  {
    Set invalid = null;
    /*
    synchronized (_idlePool) {
    } */
  }

  /**
   * Creates a new connection.
   */
  private UserPoolItem createConnection(ManagedConnectionFactory mcf,
                                        Subject subject,
                                        ConnectionRequestInfo info,
                                        UserPoolItem oldPoolItem)
    throws ResourceException
  {
    boolean isValid = false;
    ManagedPoolItem poolItem = null;

    try {
      ManagedConnection mConn = mcf.createManagedConnection(subject, info);

      if (mConn == null)
        throw new ResourceException(L.l("'{0}' did not return a connection from createManagedConnection",
                                        mcf));

      poolItem = new ManagedPoolItem(this, mcf, mConn);

      UserPoolItem userPoolItem;

      // Ensure the connection is still valid
      userPoolItem = poolItem.toActive(subject, info, oldPoolItem);
      
      if (userPoolItem == null) {
        throw new IllegalStateException(L.l("Connection '{0}' was not valid on creation",
                                            poolItem));
      }
        
      _connectionCreateCountTotal.incrementAndGet();

      synchronized (_connectionPool) {
        _connectionPool.add(poolItem);
      }

      poolItem = null;
      isValid = true;

      return userPoolItem;
    } finally {
      if (! isValid) {
        _connectionFailCountTotal.incrementAndGet();
        _lastFailTime = Alarm.getCurrentTime();
      }

      // server/308b - connection removed on rollback-only, when it's
      // theoretically possible to reuse it
      if (poolItem != null)
        poolItem.destroy();
   }
  }

  /**
   * Starts creation, pausing for full queue.
   */
  private boolean startCreateConnection()
    throws ResourceException
  {
    if (isCreateAvailable()) {
      _createCount.incrementAndGet();
      
      return true;
    }
    else {
      return false;
    }
  }
  
  private void finishCreateConnection()
  {
    _createCount.decrementAndGet();
    
    notifyConnectionAvailable();
  }

  /**
   * Starts creation of an overflow connection.
   */
  private boolean startCreateOverflow()
    throws ResourceException
  {
    int size = _connectionPool.size();
    int createCount = _createCount.incrementAndGet();

    if (createCount + size <= _maxConnections + _maxOverflowConnections)
      return true;
      
    _createCount.decrementAndGet();
      
    throw new ResourceException(L.l("Can't allocate overflow connection after {0}ms timeout because pool is full."
                                    + "\n  max-connections={1}, max-overflow-connections={2}, create-count={3}, pool-size={4}.",
                                    _connectionWaitTimeout,
                                    _maxConnections,
                                    _maxOverflowConnections,
                                    _createCount,
                                    size));
 }
  
  private boolean waitForAvailableConnection(long expireTime)
  {
    _availableWaitCount.incrementAndGet();
    
    try {
      synchronized (_availableLock) {
        while (! isIdleAvailable() && ! isCreateAvailable()) {
          try {
            long now = Alarm.getCurrentTimeActual();
            
            long delta = now - expireTime;
        
            if (delta <= 0)
              return false;
        
            Thread.interrupted();
            _availableLock.wait(delta);
          } catch (InterruptedException e) {
            log.log(Level.FINER, e.toString(), e);
          }
        }
        
        return true;
      }
    } finally {
      _availableWaitCount.decrementAndGet();
    }
  }
  
  /**
   * Notify that an idle or create connection is available.
   */
  private void notifyConnectionAvailable()
  {
    if (_availableWaitCount.get() > 0) {
      synchronized (_availableLock) {
        _availableLock.notifyAll();
      }
    }
  }
  
  /**
   * True if idlePool has an available connection.
   */
  private boolean isIdleAvailable()
  {
    return _idlePool.size() > 0;
  }

  /**
   * True if a connection can be created, i.e. below max-connections
   * and max-create-connections.
   */
  private boolean isCreateAvailable()
  {
    return (_connectionPool.size() < _maxConnections
            && _createCount.get() < _maxCreateConnections);
  }

  /*
   * Removes a connection from the pool.
   */
  public void markForPoolRemoval(ManagedConnection mConn)
  {
    synchronized (_connectionPool) {
      for (int i = _connectionPool.size() - 1; i >= 0; i--) {
        ManagedPoolItem poolItem = _connectionPool.get(i);

        if (poolItem.getManagedConnection() == mConn) {
          poolItem.setConnectionError();
          return;
        }
      }
    }
  }

  /**
   * Adds a connection to the idle pool.
   */
  void toIdle(ManagedPoolItem item)
  {
    try {
      if (_maxConnections < _connectionPool.size()
          || item.isConnectionError()) {
        return;
      }

      ManagedConnection mConn = item.getManagedConnection();

      if (mConn == null) {
        return;
      }

      mConn.cleanup();

      long now = Alarm.getCurrentTime();

      if (_idlePool.size() == 0)
        _idlePoolExpire = now + _idleTimeout;

      if (_idlePoolExpire < now) {
        // shrink the idle pool when non-empty for idleTimeout
        _idlePoolExpire = now + _idleTimeout;
      }
      else if (_idlePool.add(mConn)) {
        item = null;
        return;
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      notifyConnectionAvailable();

      if (item != null)
        item.destroy();
    }
  }

  /**
   * Removes a connection
   */
  void removeItem(ManagedPoolItem item, ManagedConnection mConn)
  {
    synchronized (_connectionPool) {
      _idlePool.remove(mConn);

      _connectionPool.remove(item);
      _connectionPool.notifyAll();
    }

    try {
      item.destroy();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Clears the idle connections in the pool.
   */
  public void clear()
  {
    ArrayList<ManagedPoolItem> pool = _connectionPool;

    if (pool == null)
      return;

    ArrayList<ManagedPoolItem> clearItems = new ArrayList<ManagedPoolItem>();

    synchronized (_connectionPool) {
      _idlePool.clear();

      clearItems.addAll(pool);

      pool.clear();
    }

    for (int i = 0; i < clearItems.size(); i++) {
      ManagedPoolItem poolItem = clearItems.get(i);

      try {
        poolItem.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Alarm listener.
   */
  public void handleAlarm(Alarm alarm)
  {
    if (! _lifecycle.isActive())
      return;

    try {
     _alarmConnections.clear();

      synchronized (_connectionPool) {
        _alarmConnections.addAll(_connectionPool);
      }

      for (int i = _alarmConnections.size() - 1; i >= 0; i--) {
        ManagedPoolItem item = _alarmConnections.get(i);

        if (! item.isValid())
          item.destroy();
      }

      _alarmConnections.clear();
    } finally {
      if (! _lifecycle.isActive()) {
      }
      else if (0 < _idleTimeout && _idleTimeout < 1000)
        _alarm.queue(1000);
      else if (1000 < _idleTimeout && _idleTimeout < 60000)
        _alarm.queue(_idleTimeout);
      else
        _alarm.queue(60000);
    }
  }

  /**
   * Stops the manager.
   */
  public void stop()
  {
    if (! _lifecycle.toStop())
      return;

    if (_alarm != null)
      _alarm.dequeue();
  }

  /**
   * Destroys the manager.
   */
  public void destroy()
  {
    stop();

    if (! _lifecycle.toDestroy())
      return;

    ArrayList<ManagedPoolItem> pool;

    synchronized (_connectionPool) {
      pool = new ArrayList<ManagedPoolItem>(_connectionPool);
      _connectionPool.clear();

      if (_idlePool != null)
        _idlePool.clear();
    }

    for (int i = 0; i < pool.size(); i++) {
      ManagedPoolItem poolItem = pool.get(i);

      try {
        poolItem.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  public String toString()
  {
    return "ConnectionPool[" + getName() + "]";
  }
}
