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

package com.caucho.sql;

import com.caucho.sql.spy.SpyConnection;
import com.caucho.sql.spy.SpyXAResource;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a single pooled connection.  For the most part, it just
 * passes the requests to the underlying JDBC connection.
 *
 * <p>Closing the connection will return the real connection to the pool
 * and close any statements.
 */
public class ManagedConnectionImpl
  implements ManagedConnection, javax.sql.ConnectionEventListener {
  protected static final Logger log
    = Logger.getLogger(ManagedConnectionImpl.class.getName());
  protected static L10N L = new L10N(ManagedConnectionImpl.class);

  // Identifier for spy, etc.
  private final String _id;

  private final ManagedFactoryImpl _factory;
  private final DBPoolImpl _dbPool;
  
  private final DriverConfig _driver;
  private final ConnectionConfig _connConfig;

  private final Credential _credentials;

  private PooledConnection _pooledConnection;
  private Connection _driverConnection;

  private XAResource _xaResource;
  private LocalTransaction _localTransaction;

  private ConnectionEventListener _listener;
  private ConnectionEvent _connClosedEvent;

  private ResourceException _connException;

  private long _lastEventTime;

  // Cached prepared statements.
  private LruCache<PreparedStatementKey,PreparedStatementCacheItem>
    _preparedStatementCache;

  // Static prepared statement key.
  private PreparedStatementKey _key;

  // Current value for isolation
  private int _isolation = -1;

  // Value for getAutoCommit
  private boolean _autoCommit = true;
  // old value for getReadOnly
  private boolean _readOnly = false;

  private boolean _hasCatalog;
  // old value for getCatalog
  private String _catalogOrig = null;
  // current value for getCatalog
  private String _catalog = null;
  
  // old value for isolation
  private int _oldIsolation = -1;
  // old value for getTypeMap
  private Map _typeMap;

  ManagedConnectionImpl(ManagedFactoryImpl factory,
                        DriverConfig driver,
                        ConnectionConfig connConfig,
                        Credential credentials)
    throws SQLException
  {
    _factory = factory;
    _dbPool = factory.getDBPool();
    _id = _dbPool.newSpyId();
      
    _driver = driver;
    _connConfig = connConfig;
    _credentials = credentials;

    _connClosedEvent = new ConnectionEvent(this,
                                           ConnectionEvent.CONNECTION_CLOSED);

    initDriverConnection();

    _lastEventTime = Alarm.getCurrentTime();

    int preparedStatementCacheSize = _dbPool.getPreparedStatementCacheSize();

    if (preparedStatementCacheSize > 0) {
      _preparedStatementCache = new LruCache<PreparedStatementKey,PreparedStatementCacheItem>(preparedStatementCacheSize);
      _key = new PreparedStatementKey();
    }
  }

  /**
   * Returns the db pool.
   */
  DBPoolImpl getDBPool()
  {
    return _dbPool;
  }

  /**
   * Returns the credentials.
   */
  Credential getCredentials()
  {
    return _credentials;
  }

  /**
   * Returns true if statements should be wrapped.
   */
  boolean isWrapStatements()
  {
    return _dbPool.isWrapStatements();
  }

  /**
   * Returns the underlying connection.
   */
  public Object getConnection(Subject subject,
                              ConnectionRequestInfo info)
    throws ResourceException
  {
    if (_connException != null)
      throw _connException;

    if (! ping())
      return null;
    
    _lastEventTime = Alarm.getCurrentTime();

    return new UserConnection(this);
  }

  /**
   * Associates the associate connection.
   */
  public void associateConnection(Object connection)
    throws ResourceException
  {
    _lastEventTime = Alarm.getCurrentTime();

    UserConnection uConn = (UserConnection) connection;

    uConn.associate(this);
  }

  /**
   * Adds a connection event listener.
   */
  public void addConnectionEventListener(ConnectionEventListener listener)
  {
    if (_listener != null && _listener != listener)
      throw new IllegalStateException();

    _listener = listener;
  }

  /**
   * Removes a connection event listener.
   */
  public void removeConnectionEventListener(ConnectionEventListener listener)
  {
    if (_listener == listener)
      _listener = null;
  }

  /**
   * Returns the driver connection.
   */
  private void initDriverConnection()
    throws SQLException
  {
    if (_driverConnection != null)
      throw new IllegalStateException();

    String user = _driver.getUser();
    String password = _driver.getPassword();

    if (_credentials != null) {
      user = _credentials.getUserName();
      password = _credentials.getPassword();
    }

    _pooledConnection = _driver.createPooledConnection(user, password);

    if (_pooledConnection != null) {
      _pooledConnection.addConnectionEventListener(this);

      _driverConnection = _pooledConnection.getConnection();
    }

    if (_driverConnection == null)
      _driverConnection = _driver.createDriverConnection(user, password);

    if (_driverConnection == null)
      throw new SQLException(L.l("Failed to create driver connection."));

    DBPoolImpl dbPool = getDBPool();

    long transactionTimeout = dbPool.getTransactionTimeout();
    if (dbPool.isXA() && ! _connConfig.isReadOnly()) {
      if (_pooledConnection instanceof XAConnection) {
        try {
          _xaResource = ((XAConnection) _pooledConnection).getXAResource();
        } catch (SQLException e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }

      if (_xaResource != null && dbPool.isXAForbidSameRM())
        _xaResource = new DisjointXAResource(_xaResource);
      System.out.println("XAXX: " + _xaResource);
      
      
      if (transactionTimeout > 0 && _xaResource != null) {
        try {
          _xaResource.setTransactionTimeout((int) (transactionTimeout / 1000));
        } catch (Throwable e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }

      boolean allowLocalTransaction = true;
      String className = "";

      if (_pooledConnection != null)
        className = _pooledConnection.getClass().getName();
      
      if (! (_pooledConnection instanceof XAConnection)) {
      }
      else if (className.startsWith("oracle")) {
        // Oracle does not allow local transactions
        allowLocalTransaction = false;
      }
      else if (className.equals("com.mysql.jdbc.jdbc2.optional.MysqlXAConnection")) {
        allowLocalTransaction = false;
      }

      if (allowLocalTransaction)
        _localTransaction = new LocalTransactionImpl();
    }

    if (dbPool.isSpy()) {
      _driverConnection = new SpyConnection(_driverConnection,
                                            _dbPool.getSpyDataSource(),
                                            _id);

      if (_xaResource != null)
        _xaResource = new SpyXAResource(_id, _xaResource);
    }

    int isolation = _connConfig.getTransactionIsolation();
    if (isolation >= 0)
      _driverConnection.setTransactionIsolation(isolation);

    if (_connConfig.isReadOnly())
      _driverConnection.setReadOnly(true);

    String configCatalog = _connConfig.getCatalog();
    if (configCatalog != null) {
      _hasCatalog = true;
      _catalogOrig = configCatalog;
      _driverConnection.setCatalog(_catalogOrig);
    }
  }

  /**
   * Returns the driver connection.
   */
  Connection getDriverConnection()
  {
    return _driverConnection;
  }
  
  /*
   * Returns the driver.
   */
  public Class getDriverClass()
  {
    return _driver.getDriverClass();
  }
  
  /*
   * Returns the driver URL.
   */
  public String getURL()
  {
    return getDBPool().getURL();
  }

  /**
   * Returns the XA resource for the connection.
   */
  public XAResource getXAResource()
    throws ResourceException
  {
    if (_xaResource != null)
      return _xaResource;

    throw new NotSupportedException();
  }

  /**
   * Returns the XA resource for the connection.
   */
  public LocalTransaction getLocalTransaction()
    throws ResourceException
  {
    return _localTransaction;
  }

  /**
   * Returns the meta data.
   */
  public ManagedConnectionMetaData getMetaData()
    throws ResourceException
  {
    throw new NotSupportedException();
  }

  /**
   * Sets the log writer.
   */
  public void setLogWriter(PrintWriter out)
    throws ResourceException
  {
  }

  /**
   * Gets the log writer.
   */
  public PrintWriter getLogWriter()
    throws ResourceException
  {
    return null;
  }

  /**
   * Sets the isolation.
   */
  public void setIsolation(int isolation)
    throws SQLException
  {
  }

  /**
   * Returns a new or cached prepared statement.
   */
  PreparedStatement prepareStatement(UserConnection uConn,
                                     String sql,
                                     int resultType)
    throws SQLException
  {
    Connection conn = getDriverConnection();

    if (conn == null)
      throw new IllegalStateException(L.l("can't prepare statement from closed connection"));

    if (resultType > 0)
      return conn.prepareStatement(sql, resultType);
    else
      return conn.prepareStatement(sql);
  }

  /**
   * Returns a new or cached prepared statement.
   */
  PreparedStatement prepareStatement(UserConnection uConn, String sql)
    throws SQLException
  {
    PreparedStatementKey key = _key;

    Connection conn = getDriverConnection();

    if (conn == null)
      throw new IllegalStateException(L.l("can't prepare statement from closed connection"));

    if (key == null) {
      return conn.prepareStatement(sql);
    }

    boolean hasItem = false;

    synchronized (key) {
      key.init(sql);

      PreparedStatementCacheItem item = _preparedStatementCache.get(key);

      if (item != null) {
        UserPreparedStatement upStmt = item.toActive(uConn);

        if (upStmt != null)
          return upStmt;

        hasItem = ! item.isRemoved();
      }
    }

    PreparedStatement pStmt;
    pStmt = conn.prepareStatement(sql);

    if (hasItem)
      return pStmt;

    key = new PreparedStatementKey(sql);

    PreparedStatementCacheItem item;
    item = new PreparedStatementCacheItem(key, pStmt, this);

    UserPreparedStatement upStmt = item.toActive(uConn);

    if (upStmt == null)
      throw new IllegalStateException("preparedStatement can't activate");

    _preparedStatementCache.put(key, item);

    return upStmt;
  }

  /**
   * Removes a cached item.
   */
  void remove(PreparedStatementKey key)
  {
    _preparedStatementCache.remove(key);
  }

  public void connectionClosed(javax.sql.ConnectionEvent event)
  {
    sendFatalEvent(new SQLException(L.l("unexpected close event from pool")));
    closeEvent(null);
  }

  public void connectionErrorOccurred(javax.sql.ConnectionEvent event)
  {
    sendFatalEvent(event.getSQLException());
  }

  /**
   * Sends the close event.
   */
  public void closeEvent(UserConnection userConn)
  {
    if (_listener != null) {
      if (_connException != null) {
        sendFatalEvent(_connException);
      }

      ConnectionEvent evt;
      synchronized (this) {
        evt = _connClosedEvent;
        _connClosedEvent = null;
      }

      if (evt == null)
        evt = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);

      evt.setConnectionHandle(userConn);

      _listener.connectionClosed(evt);

      evt.setConnectionHandle(null);

      _connClosedEvent = evt;

      _lastEventTime = Alarm.getCurrentTime();
    }
  }

  /**
   * Sends the fatal event.
   */
  public void fatalEvent()
  {
    fatalEvent(new ResourceException("fatal event"));
  }

  /**
   * Sends the fatal event.
   */
  public void fatalEvent(Exception e)
  {
    if (_pooledConnection != null) {
    }
    else if (e instanceof ResourceException)
      _connException = (ResourceException) e;
    else
      _connException = new ResourceException(e);
  }

  /**
   * Sends the fatal event.
   */
  public void sendFatalEvent(Exception e)
  {
    if (_listener != null) {
      ConnectionEvent event;

      event = new ConnectionEvent(this,
                                  ConnectionEvent.CONNECTION_ERROR_OCCURRED,
                                  e);

      _listener.connectionErrorOccurred(event);
    }
  }

  /**
   * When closed, the item is not put into the idle pool.
   */
  void killPool()
  {
    if (_listener != null) {
      ConnectionEvent event;

      event = new ConnectionEvent(this,
                                  ConnectionEvent.CONNECTION_ERROR_OCCURRED);

      _listener.connectionErrorOccurred(event);
    }
  }

  /**
   * Sets the auto-commit.
   */
  public void setAutoCommit(boolean autoCommit)
    throws SQLException
  {
    try {
      _autoCommit = autoCommit;
      _driverConnection.setAutoCommit(autoCommit);
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }
  }

  /**
   * Sets the read only attribute.
   */
  public void setReadOnly(boolean readOnly)
    throws SQLException
  {
    try {
      _readOnly = readOnly;
      _driverConnection.setReadOnly(readOnly);
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }
  }

  /**
   * Sets the JDBC catalog.
   */
  public void setCatalog(String catalog)
    throws SQLException
  {
    try {
      if (! _hasCatalog) {
        _hasCatalog = true;
        _catalogOrig = _driverConnection.getCatalog();
        _catalog = _catalogOrig;
      }

      if (catalog == null || catalog.length() == 0) {
        // Clear the current catalog name but don't invoke setCatalog()
        // on the driver.

        _catalog = null;
      } else if (_catalog != null && _catalog.equals(catalog)) {
        // No-op when setting to the currently selected catalog name
      } else {
        _driverConnection.setCatalog(catalog);
        _catalog = catalog;
      }
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }
  }

  /**
   * Sets the connection's type map.
   */
  public void setTypeMap(Map map)
    throws SQLException
  {
    try {
      if (_typeMap == null)
        _typeMap = _driverConnection.getTypeMap();

      _driverConnection.setTypeMap(map);
    } catch (SQLException e) {
      throw e;
    }
  }


  /**
   * Sets the connection's isolation.
   */
  public void setTransactionIsolation(int isolation)
    throws SQLException
  {
    try {
      _oldIsolation = _driverConnection.getTransactionIsolation();
      _isolation = isolation;

      _driverConnection.setTransactionIsolation(isolation);
    } catch (SQLException e) {
      throw e;
    }
  }

  /**
   * Cleans up the instance.
   */
  public void cleanup()
    throws ResourceException
  {
    Connection conn = _driverConnection;

    if (conn == null)
      return;

    try {
      /*
      // If there's a pooled connection, it can cleanup itself
      if (_pooledConnection != null) {
        _autoCommit = true;
        _driverConnection = _pooledConnection.getConnection();
        _isolation = _oldIsolation = -1;
        return;
      }
      */

      if (_readOnly)
        conn.setReadOnly(false);
      _readOnly = false;

      if (_catalog != null
          && ! _catalog.equals(_catalogOrig)
          && _catalogOrig != null
          && ! "".equals(_catalogOrig)) {
        conn.setCatalog(_catalogOrig);
      }
      _catalog = null;

      if (_typeMap != null)
        conn.setTypeMap(_typeMap);
      _typeMap = null;

      // Oracle requires a rollback after a reset of
      // the transaction isolation, since setting the isolation
      // starts a new transaction
      boolean needsRollback = ! _autoCommit;
      if (_isolation != _oldIsolation) {
        needsRollback = true;
        conn.setTransactionIsolation(_oldIsolation);
      }
      _isolation = _oldIsolation;

      if (needsRollback)
        conn.rollback();
      
      if (! _autoCommit) {
        conn.setAutoCommit(true);
      }
      _autoCommit = true;

      conn.clearWarnings();
    } catch (SQLException e) {
      throw new ResourceException(e);
    }
  }

  /**
   * Returns true if the connection is valid.
   */
  boolean isValid()
  {
    try {
      return ping();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Checks the validity with ping.
   */
  private boolean ping()
    throws ResourceException
  {
    DBPoolImpl dbPool = _factory.getDBPool();

    long now = Alarm.getCurrentTime();

    if (now < _lastEventTime + 1000)
      return true;

    if (! dbPool.isPing())
      return true;

    long pingInterval = dbPool.getPingInterval();
    if (pingInterval > 0 && now < _lastEventTime + pingInterval)
      return true;

    Connection conn = _driverConnection;

    try {
      if (conn == null || conn.isClosed()) {
        return false;
      }

      String pingQuery = dbPool.getPingQuery();

      if (pingQuery == null)
        return true;
      
      Statement stmt = conn.createStatement();

      try {
        ResultSet rs = stmt.executeQuery(pingQuery);
        rs.close();

        return true;
      } finally {
        stmt.close();
      }
    } catch (SQLException e) {
      throw new ResourceException(e);
    }
  }

  /**
   * Destroys the physical connection.
   */
  public void destroy()
    throws ResourceException
  { 
    log.finer("destroy " + this);

    PooledConnection poolConn = _pooledConnection;
    _pooledConnection = null;

    Connection driverConn = _driverConnection;
    _driverConnection = null;

    if (_preparedStatementCache != null) {
      Iterator<PreparedStatementCacheItem> iter;

      iter = _preparedStatementCache.values();

      while (iter.hasNext()) {
        PreparedStatementCacheItem item = iter.next();

        item.destroy();
      }
    }

    try {
      if (poolConn != null) {
        poolConn.close();
        driverConn = null;
      }
    } catch (SQLException e) {
      throw new ResourceException(e);
    }

    try {
      if (driverConn != null)
        driverConn.close();
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public String toString()
  {
    return "ManagedConnectionImpl[" + _id + "]";
  }

  class LocalTransactionImpl implements LocalTransaction {
    private boolean _oldAutoCommit;

    public void begin()
      throws ResourceException
    {
      try {
        _oldAutoCommit = _autoCommit;

        setAutoCommit(false);
      } catch (SQLException e) {
        throw new ResourceException(e) ;
      }
    }

    public void commit()
      throws ResourceException
    {
      Connection conn = _driverConnection;

      if (conn == null)
        throw new ResourceException(L.l("connection is closed"));

      try {
        conn.commit();
      } catch (SQLException e) {
        throw new ResourceException(e) ;
      }

      try {
        setAutoCommit(_oldAutoCommit);
      } catch (SQLException e) {
        throw new ResourceException(e) ;
      }
    }

    public void rollback()
      throws ResourceException
    {
      Connection conn = _driverConnection;

      if (conn == null)
        throw new ResourceException(L.l("connection is closed"));

      try {
        conn.rollback();
      } catch (SQLException e) {
        throw new ResourceException(e) ;
      }

      try {
        setAutoCommit(_oldAutoCommit);
      } catch (SQLException e) {
        throw new ResourceException(e) ;
      }
    }
  }
}
