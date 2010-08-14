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

import com.caucho.config.ConfigException;
import com.caucho.config.types.InitParam;
import com.caucho.config.types.Period;
import com.caucho.env.meter.ActiveTimeSensor;
import com.caucho.env.meter.MeterService;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.management.server.JdbcDriverMXBean;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.sql.spy.*;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.spi.ManagedConnectionFactory;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import java.io.PrintWriter;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages a pool of database connections.  In addition, DBPool configures
 * the database connection from a configuration file.
 *
 * <p>Like JDBC 2.0 pooling, DBPool returns a wrapped Connection.
 * Applications can use that connection just like an unpooled connection.
 * It is more important than ever to <code>close()</code> the connection,
 * because the close returns the connection to the connection pool.
 *
 * <h4>Example using DataSource JNDI style (recommended)</h4>
 *
 * <pre><code>
 * Context env = (Context) new InitialContext().lookup("java:comp/env");
 * DataSource pool = (DataSource) env.lookup("jdbc/test");
 * Connection conn = pool.getConnection();
 * try {
 *   ... // normal connection stuff
 * } finally {
 *   conn.close();
 * }
 * </code></pre>
 *
 * <h4>Configuration</h4>
 *
 * <pre><code>
 * &lt;database name='jdbc/test'>
 *   &lt;init>
 *     &lt;driver>postgresql.Driver&lt;/driver>
 *     &lt;url>jdbc:postgresql://localhost/test&lt;/url>
 *     &lt;user>ferg&lt;/user>
 *     &lt;password>foobar&lt;/password>
 *   &lt;/init>
 * &lt;/database>
 * </code></pre>
 *
 * <h4>Pool limits and timeouts</h4>
 *
 * The pool will only allow getMaxConnections() connections alive at a time.
 * If <code>getMaxConnection</code> connections are already active,
 * <code>getPooledConnection</code> will block waiting for an available
 * connection.  The wait is timed.  If connection-wait-time passes
 * and there is still no connection, <code>getPooledConnection</code>
 * create a new connection anyway.
 *
 * <p>Connections will only stay in the pool for about 5 seconds.  After
 * that they will be removed and closed.  This reduces the load on the DB
 * and also protects against the database dropping old connections.
 */
public class DBPoolImpl implements AlarmListener, EnvironmentListener {
  protected static final Logger log
    = Logger.getLogger(DBPoolImpl.class.getName());
  private static final L10N L = new L10N(DBPoolImpl.class);

  /**
   * The key used to look into the properties passed to the
   * connect method to find the username.
   */
  public static final String PROPERTY_USER = "user" ;
  /**
   * The key used to look into the properties passed to the
   * connect method to find the password.
   */
  public static final String PROPERTY_PASSWORD = "password" ;

  // How long an unused connection can remain in the pool
  private static final long MAX_IDLE_TIME = 30000;

  private String _name;

  private ArrayList<DriverConfig> _driverList
    = new ArrayList<DriverConfig>();

  private ArrayList<DriverConfig> _backupDriverList
    = new ArrayList<DriverConfig>();

  private ConnectionConfig _connectionConfig
    = new ConnectionConfig();

  // private ManagedFactoryImpl _mcf;
  private ManagedConnectionFactory _mcf;

  private String _user;
  private String _password;

  // total connections allowed in this pool
  private int _maxConnections = 128;
  // time before an idle connection is closed
  private long _maxIdleTime = MAX_IDLE_TIME;
  // max time a connection is allowed to be active (6 hr)
  private long _maxActiveTime = 6L * 3600L * 1000L;
  // max time a connection is allowed in the pool
  private long _maxPoolTime = 24L * 3600L * 1000L;

  // how long to wait for a connection, say 10 minutes
  private long _connectionWaitTime = 600 * 1000;
  private int _connectionWaitCount = (int) (_connectionWaitTime / 1000);

  // connections to create even when the max-connections overflows.
  private int _maxOverflowConnections = 0;

  // true if the pool has started
  private boolean _isStarted;
  // true if the pool has closed
  private boolean _isClosed;
  // if true, the pool can't be closed.
  private boolean _forbidClose;

  // The JDBC table to be used to ping for connection liveness.
  private String _pingTable;
  // The Query used for connection liveness.
  private String _pingQuery;
  // Ping when the connection is reused.
  private Boolean _isPing;
  // How long between pings
  private long _pingInterval = 1000;

  // True if the pool is transactional
  private boolean _isTransactional = true;
  // True if the pool should never allow isSameRM
    private boolean _isXAForbidSameRM = true;
  // The transaction manager if the pool participates in transactions.
  private TransactionManager _tm;
  // how long before the transaction times out
  private long _transactionTimeout = 0;

  private boolean _isSpy;
  private SpyDataSource _spyDataSource;

  private int _maxCloseStatements = 256;
  // The prepared statement cache size.
  private int _preparedStatementCacheSize = 0;

  private boolean _isWrapStatements = true;
  
  // The connections currently in the pool.
  // transient ArrayList<PoolItem> _connections = new ArrayList<PoolItem>();

  // Count for debugging ids.
  private int _idCount;

  private ActiveTimeSensor _timeProbe;

  /**
   * Null constructor for the Driver interface; called by the JNDI
   * configuration.  Applications should not call this directly.
   */
  public DBPoolImpl()
  {
  }

  /**
   * Returns the Pool's name
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the Pool's name.  Also puts the pool in the classloader's
   * list of pools.
   */
  public void setName(String name)
  {
    _name = name;
  }

  public String getURL()
  {
    return _driverList.get(0).getURL();
  }

  /**
   * Returns the driver config.
   */
  public DriverConfig createDriver()
  {
    DriverConfig driver = new DriverConfig(this);

    _driverList.add(driver);

    return driver;
  }

  /**
   * Returns the driver config.
   */
  public DriverConfig createBackupDriver()
  {
    DriverConfig driver = new DriverConfig(this);

    _backupDriverList.add(driver);

    return driver;
  }

  /**
   * Sets a driver parameter.
   */
  public void setInitParam(InitParam init)
  {
    DriverConfig driver = _driverList.get(0);

    HashMap<String,String> params = init.getParameters();

    Iterator<String> iter = params.keySet().iterator();
    while (iter.hasNext()) {
      String key = iter.next();
      driver.setInitParam(key, params.get(key));
    }
  }

  /**
   * Sets the jdbc-driver config.
   */
  public void setJDBCDriver(Driver jdbcDriver)
    throws SQLException
  {
    DriverConfig driver;

    if (_driverList.size() > 0)
      driver = _driverList.get(0);
    else
      driver = createDriver();

    driver.setDriver(jdbcDriver);
  }

  /**
   * Returns the driver admin
   */
  public JdbcDriverMXBean []getDriverAdmin()
  {
    JdbcDriverMXBean []drivers = new JdbcDriverMXBean[_driverList.size()];

    for (int i = 0; i < _driverList.size(); i++) {
      DriverConfig driver = _driverList.get(i);

      drivers[i] = driver.getAdmin();
    }

    return drivers;
  }

  /**
   * Creates the connection config.
   */
  public ConnectionConfig createConnection()
  {
    return _connectionConfig;
  }

  /**
   * Returns the connection config.
   */
  public ConnectionConfig getConnectionConfig()
  {
    return _connectionConfig;
  }

  ActiveTimeSensor getTimeProbe()
  {
    if (_timeProbe == null) {
      _timeProbe = MeterService.createActiveTimeMeter("Resin|Database|Query");
    }

    return _timeProbe;
  }

  /**
   * Sets the jdbc-driver config.
   */
  public void setPoolDataSource(ConnectionPoolDataSource poolDataSource)
    throws SQLException
  {
    DriverConfig driver;

    if (_driverList.size() > 0)
      driver = _driverList.get(0);
    else
      driver = createDriver();

    driver.setPoolDataSource(poolDataSource);
  }

  /**
   * Sets the jdbc-driver config.
   */
  public void setXADataSource(XADataSource xaDataSource)
    throws SQLException
  {
    DriverConfig driver;

    if (_driverList.size() > 0)
      driver = _driverList.get(0);
    else
      driver = createDriver();

    driver.setXADataSource(xaDataSource);
  }

  /**
   * Sets the jdbc-driver config.
   */
  public void setURL(String url)
    throws ConfigException
  {
    DriverConfig driver;

    if (_driverList.size() > 0)
      driver = _driverList.get(0);
    else
      throw new ConfigException(L.l("The driver must be assigned before the URL."));

    driver.setURL(url);
  }

  /**
   * Returns the connection's user.
   */
  public String getUser()
  {
    return _user;
  }

  /**
   * Sets the connection's user.
   */
  public void setUser(String user)
  {
    _user = user;
  }

  /**
   * Returns the connection's password
   */
  public String getPassword()
  {
    return _password;
  }

  /**
   * Sets the connection's password
   */
  public void setPassword(String password)
  {
    _password = password;
  }

  /**
   * Get the maximum number of pooled connections.
   */
  public int getMaxConnections()
  {
    return _maxConnections;
  }

  /**
   * Sets the maximum number of pooled connections.
   */
  public void setMaxConnections(int maxConnections)
  {
    _maxConnections = maxConnections;
  }

  /**
   * Get the total number of connections
   */
  public int getTotalConnections()
  {
    // return _connections.size();
    return 0;
  }

  /**
   * Sets the time to wait for a connection when all are used.
   */
  public void setConnectionWaitTime(Period waitTime)
  {
    long period = waitTime.getPeriod();

    _connectionWaitTime = period;

    if (period < 0)
      _connectionWaitCount = 3600; // wait for an hour == infinity
    else {
      _connectionWaitCount = (int) ((period + 999) / 1000);

      if (_connectionWaitCount <= 0)
        _connectionWaitCount = 1;
    }
  }

  /**
   * Gets the time to wait for a connection when all are used.
   */
  public long getConnectionWaitTime()
  {
    return _connectionWaitTime;
  }

  /**
   * The number of connections to overflow if the connection pool fills
   * and there's a timeout.
   */
  public void setMaxOverflowConnections(int maxOverflowConnections)
  {
    _maxOverflowConnections = maxOverflowConnections;
  }

  /**
   * The number of connections to overflow if the connection pool fills
   * and there's a timeout.
   */
  public int getMaxOverflowConnections()
  {
    return _maxOverflowConnections;
  }

  /**
   * Sets the transaction timeout.
   */
  public void setTransactionTimeout(Period period)
  {
    _transactionTimeout = period.getPeriod();
  }

  /**
   * Gets the transaction timeout.
   */
  public long getTransactionTimeout()
  {
    return _transactionTimeout;
  }

  /**
   * Sets the max statement.
   */
  public void setMaxCloseStatements(int max)
  {
    _maxCloseStatements = max;
  }

  /**
   * Gets the max statement.
   */
  public int getMaxCloseStatements()
  {
    return _maxCloseStatements;
  }

  /**
   * Sets true if statements should be wrapped.
   */
  public void setWrapStatements(boolean isWrap)
  {
    _isWrapStatements = isWrap;
  }

  /**
   * Sets true if statements should be wrapped.
   */
  public boolean isWrapStatements()
  {
    return _isWrapStatements;
  }

  /**
   * Returns the prepared statement cache size.
   */
  public int getPreparedStatementCacheSize()
  {
    return _preparedStatementCacheSize;
  }

  /**
   * Sets the prepared statement cache size.
   */
  public void setPreparedStatementCacheSize(int size)
  {
    _preparedStatementCacheSize = size;
  }

  /**
   * Get the time in milliseconds a connection will remain in the pool before
   * being closed.
   */
  public long getMaxIdleTime()
  {
    if (_maxIdleTime > Long.MAX_VALUE / 2)
      return -1;
    else
      return _maxIdleTime;
  }

  /**
   * Set the time in milliseconds a connection will remain in the pool before
   * being closed.
   */
  public void setMaxIdleTime(Period idleTime)
  {
    long period = idleTime.getPeriod();

    if (period < 0)
      _maxIdleTime = Long.MAX_VALUE / 2;
    else if (period < 1000L)
      _maxIdleTime = 1000L;
    else
      _maxIdleTime = period;
  }

  /**
   * Get the time in milliseconds a connection will remain in the pool before
   * being closed.
   */
  public long getMaxPoolTime()
  {
    if (_maxPoolTime > Long.MAX_VALUE / 2)
      return -1;
    else
      return _maxPoolTime;
  }

  /**
   * Set the time in milliseconds a connection will remain in the pool before
   * being closed.
   */
  public void setMaxPoolTime(Period maxPoolTime)
  {
    long period = maxPoolTime.getPeriod();

    if (period < 0)
      _maxPoolTime = Long.MAX_VALUE / 2;
    else if (period == 0)
      _maxPoolTime = 1000L;
    else
      _maxPoolTime = period;
  }

  /**
   * Get the time in milliseconds a connection can remain active.
   */
  public long getMaxActiveTime()
  {
    if (_maxActiveTime > Long.MAX_VALUE / 2)
      return -1;
    else
      return _maxActiveTime;
  }

  /**
   * Set the time in milliseconds a connection can remain active.
   */
  public void setMaxActiveTime(Period maxActiveTime)
  {
    long period = maxActiveTime.getPeriod();

    if (period < 0)
      _maxActiveTime = Long.MAX_VALUE / 2;
    else if (period == 0)
      _maxActiveTime = 1000L;
    else
      _maxActiveTime = period;
  }

  /**
   * Get the table to 'ping' to see if the connection is still live.
   */
  public String getPingTable()
  {
    return _pingTable;
  }

  /**
   * Set the table to 'ping' to see if the connection is still live.
   *
   * @param pingTable name of the SQL table to ping.
   */
  public void setPingTable(String pingTable)
  {
    _pingTable = pingTable;

    if (pingTable != null)
      _pingQuery = "select 1 from " + pingTable + " where 1=0";
    else
      _pingQuery = null;

    if (_isPing == null)
      _isPing = true;
  }

  /**
   * Returns the ping query.
   */
  public String getPingQuery()
  {
    return _pingQuery;
  }

  /**
   * Sets the ping query.
   */
  public void setPingQuery(String pingQuery)
  {
    _pingQuery = pingQuery;

    if (_isPing == null)
      _isPing = true;
  }

  /**
   * If true, the pool will ping when attempting to reuse a connection.
   */
  public boolean getPingOnReuse()
  {
    return isPing();
  }

  /**
   * Set the table to 'ping' to see if the connection is still live.
   */
  public void setPingOnReuse(boolean pingOnReuse)
  {
    _isPing = pingOnReuse;
  }

  /**
   * If true, the pool will ping in the idle pool.
   */
  public boolean getPingOnIdle()
  {
    return _isPing;
  }

  /**
   * Set the table to 'ping' to see if the connection is still live.
   */
  public void setPingOnIdle(boolean pingOnIdle)
  {
    _isPing = pingOnIdle;
  }

  /**
   * Set true if pinging is enabled.
   */
  public void setPing(boolean ping)
  {
    _isPing = ping;
  }

  /**
   * Returns true if pinging is enabled.
   */
  public boolean isPing()
  {
    if (_isPing != null)
      return _isPing;
    else
      return false;
  }

  /**
   * Sets the time to ping for ping-on-idle
   */
  public void setPingInterval(Period interval)
  {
    _pingInterval = interval.getPeriod();

    if (_pingInterval < 0)
      _pingInterval = Long.MAX_VALUE / 2;
    else if (_pingInterval < 1000)
      _pingInterval = 1000;

    if (_isPing == null)
      _isPing = true;
  }

  /**
   * Gets how often the ping for ping-on-idle
   */
  public long getPingInterval()
 {
    return _pingInterval;
  }

  /**
   * Set the transaction manager for this pool.
   */
  public void setTransactionManager(TransactionManager tm)
  {
    _tm = tm;
  }

  /**
   * Returns the transaction manager.
   */
  /*
  public TransactionManager getTransactionManager()
  {
    return _tm;
  }
  */

  /**
   * Returns true if this is transactional.
   */
  public boolean isXA()
  {
    return _isTransactional;
  }

  /**
   * Returns true if this is transactional.
   */
  public void setXA(boolean isTransactional)
  {
    _isTransactional = isTransactional;
  }

  /**
   * Returns true if transactions should force isSameRM to be false.
   */
  public boolean isXAForbidSameRM()
  {
    return _isXAForbidSameRM;
  }

  /**
   * Returns true if transactions should force isSameRM to be false.
   */
  public void setXAForbidSameRM(boolean isXAForbidSameRM)
  {
    _isXAForbidSameRM = isXAForbidSameRM;
  }

  /**
   * Set the output for spying.
   */
  public void setSpy(boolean isSpy)
  {
    _isSpy = isSpy;
  }

  /**
   * Return true for a spy.
   */
  public boolean isSpy()
  {
    return _isSpy;
  }

  /**
   * Returns the next spy id.
   */
  public SpyDataSource getSpyDataSource()
  {
    return _spyDataSource;
  }

  /**
   * Returns the next spy id.
   */
  public String newSpyId()
  {
    return _spyDataSource.createConnectionId();
  }

  /**
   * Returns true if the pool supports transactions.
   */
  public boolean isTransactional()
  {
    return _isTransactional;
  }

  /**
   * Returns true if there is a valid XAResource associated
   * with the database.
   */
  public boolean isXATransaction()
  {
    if (_connectionConfig.isReadOnly())
      return false;
    else if (_driverList.size() > 0) {
      DriverConfig driver = _driverList.get(0);

      return driver.isXATransaction();
    }
    else
      return false;
  }

  /**
   * Returns true if there is a valid local transactino associated
   * with the database.
   */
  public boolean isLocalTransaction()
  {
    if (_connectionConfig.isReadOnly())
      return false;
    else if (_driverList.size() > 0) {
      DriverConfig driver = _driverList.get(0);

      return driver.isLocalTransaction();
    }
    else
      return false;
  }

  int createPoolId()
  {
    return _idCount++;
  }

  /**
   * Sets the timeout for a database login.
   */
  public void setLoginTimeout(int seconds) throws SQLException
  {
  }

  /**
   * Gets the timeout for a database login.
   */
  public int getLoginTimeout() throws SQLException
  {
    return 0;
  }
  /**
   * Sets the debugging log for the connection.
   */
  public void setLogWriter(PrintWriter out) throws SQLException
  {
  }

  /**
   * Sets the debugging log for the connection.
   */
  public PrintWriter getLogWriter() throws SQLException
  {
    return null;
  }

  /**
   * Initialize the pool.
   */
  public void init()
    throws Exception
  {
    Environment.addEnvironmentListener(this);

    try {
      if (_tm == null) {
        Object obj = new InitialContext().lookup("java:comp/TransactionManager");

        if (obj instanceof TransactionManager)
          _tm = (TransactionManager) obj;
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    for (int i = 0; i < _driverList.size(); i++) {
      DriverConfig driver = _driverList.get(i);

      if (driver.getUser() == null)
        driver.setUser(_user);
      if (driver.getPassword() == null)
        driver.setPassword(_password);

      driver.initDriver();
      driver.initDataSource(_isTransactional, _isSpy);

      if (_mcf == null)
        _mcf = driver.getManagedConnectionFactory();

      /*
      if (driver.getXADataSource() == null)
        _isTransactional = false;
      */
    }

    DriverConfig []drivers = new DriverConfig[_driverList.size()];
    _driverList.toArray(drivers);

    for (int i = 0; i < _backupDriverList.size(); i++) {
      DriverConfig driver = _backupDriverList.get(i);

      if (driver.getUser() == null)
        driver.setUser(_user);
      if (driver.getPassword() == null)
        driver.setPassword(_password);

      driver.initDriver();
      driver.initDataSource(_isTransactional, _isSpy);
      /*
      if (driver.getXADataSource() == null)
        _isTransactional = false;
      */
    }

    DriverConfig []backupDrivers = new DriverConfig[_backupDriverList.size()];
    _backupDriverList.toArray(backupDrivers);

    if (_driverList.size() == 0 && _backupDriverList.size() == 0) {
      throw new ConfigException(L.l("<database> configuration needs at least one <driver>, because it needs to know the database to connect."));
    }

    if (_mcf == null)
      _mcf = new ManagedFactoryImpl(this, drivers, backupDrivers);

    if (_name != null) {
      String name = _name;
      if (! name.startsWith("java:"))
        name = "java:comp/env/" + name;

      if (drivers.length == 0) {
        log.config("database " + name + " starting");
      }
      else if (drivers[0].getURL() != null)
        log.config("database " + name + " starting (URL:" + drivers[0].getURL() + ")");
      else
        log.config("database " + name + " starting");

      // XXX: actually should be proxy
      // Jndi.bindDeep(name, this);
    }

    _spyDataSource = new SpyDataSource(_name);
  }

  /**
   * Returns the managed connection factory.
   */
  ManagedConnectionFactory getManagedConnectionFactory()
  {
    return _mcf;
  }

  /**
   * Initialize the pool's data source
   *
   * <ul>
   * <li>If data-source is set, look it up in JNDI.
   * <li>Else if the driver is a pooled or xa data source, use it.
   * <li>Else create wrappers.
   * </ul>
   */
  synchronized void initDataSource()
    throws SQLException
  {
    if (_isStarted)
      return;

    _isStarted = true;

    for (int i = 0; i < _driverList.size(); i++) {
      DriverConfig driver = _driverList.get(i);

      driver.initDataSource(_isTransactional, _isSpy);
    }

    try {
      if (_isTransactional && _tm == null) {
        Object obj = new InitialContext().lookup("java:comp/TransactionManager");

        if (obj instanceof TransactionManager)
          _tm = (TransactionManager) obj;
      }
    } catch (NamingException e) {
      throw new SQLExceptionWrapper(e);
    }
  }

  /**
   * Closes the idle connections in the pool.
   */
  public void closeIdleConnections()
  {
    
  }

  /**
   * At the alarm, close all connections which have been sitting in
   * the pool for too long.
   *
   * @param alarm the alarm event.
   */
  public void handleAlarm(Alarm alarm)
  {
    if (_isClosed)
      return;
  }

  /**
   * Callback when the environment configures.
   */
  public void environmentConfigure(EnvironmentClassLoader loader)
  {
  }

  /**
   * Callback when the environment binds.
   */
  public void environmentBind(EnvironmentClassLoader loader)
  {
  }
  
  /**
   * Callback when the environment starts.
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
  }

  /**
   * Callback when the class loader dies.
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    forceClose();
  }

  /**
   * Returns true if the pool is closed.
   */
  public boolean isClosed()
  {
    return _isClosed;
  }

  /**
   * Close the pool, closing the connections.
   */
  public void close()
  {
    if (_forbidClose)
      throw new IllegalStateException("illegal to call close() for this DBPool");
    forceClose();
  }

  /**
   * Close all the connections in the pool.
   */
  public void forceClose()
  {
    if (_isClosed)
      return;

    _isClosed = true;

    if (log.isLoggable(Level.FINE))
      log.fine("closing pool " + getName());
  }

  /**
   * Returns a string description of the pool.
   */
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}

