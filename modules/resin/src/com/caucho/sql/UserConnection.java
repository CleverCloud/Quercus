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

import com.caucho.env.meter.ActiveTimeSensor;
import com.caucho.util.L10N;

import java.sql.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapts the JDBC 2.0 connection pooling and XA support for database drivers
 * which don't understand it.
 *
 * <p>Closing the connection will return the real connection to the pool
 * and close any statements.
 */
public class UserConnection implements java.sql.Connection {
  protected static final Logger log
    = Logger.getLogger(UserConnection.class.getName());
  protected static final L10N L = new L10N(UserConnection.class);

  // The parent XAConnectionAdapter
  private ManagedConnectionImpl _mConn;

  // Maximum statements saved for automatic closing

  private Statement _statement;
  // ArrayList of all the statements created by this connection
  private ArrayList<Statement> _statements;

  /**
   * Creates a new PooledConnection.
   *
   * @param pool the pool the connection belongs to.
   * @param conn the underlying connection.
   */
  UserConnection(ManagedConnectionImpl mConn)
  {
    if (mConn == null || mConn.getDriverConnection() == null)
      throw new NullPointerException();

    _mConn = mConn;
  }

  /**
   * Returns the underlying connection.
   */
  public Connection getConnection()
    throws SQLException
  {
    Connection conn = getDriverConnection();

    if (conn instanceof com.caucho.sql.spy.SpyConnection)
      conn = ((com.caucho.sql.spy.SpyConnection)conn).getConnection();

    return conn;
  }

  ActiveTimeSensor getTimeProbe()
  {
    return _mConn.getDBPool().getTimeProbe();
  }

  public Class getDriverClass()
  {
    return getMConn().getDriverClass();
  }
  
  public String getURL()
  {
    return getMConn().getDBPool().getURL();
  }

  /**
   * Associates with a different mConn.
   */
  void associate(ManagedConnectionImpl mConn)
  {
    _mConn = mConn;
  }

  /**
   * JDBC api to create a new statement.  Any SQL exception thrown here
   * will make the connection invalid, i.e. it can't be put back into
   * the pool.
   *
   * @return a new JDBC statement.
   */
  public Statement createStatement()
    throws SQLException
  {
    Statement stmt;

    Connection conn = getDriverConnection();

    try {
      stmt = conn.createStatement();
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }

    addStatement(stmt);

    if (_mConn.isWrapStatements())
      return new UserStatement(this, stmt);
    else
      return stmt;
  }

  /**
   * JDBC api to create a new statement.  Any SQL exception thrown here
   * will make the connection invalid, i.e. it can't be put back into
   * the pool.
   *
   * @return a new JDBC statement.
   */
  public Statement createStatement(int resultSetType, int resultSetConcurrency)
    throws SQLException
  {
    Statement stmt;

    Connection conn = getDriverConnection();

    try {
      stmt = conn.createStatement(resultSetType, resultSetConcurrency);
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }

    addStatement(stmt);

    if (_mConn.isWrapStatements())
      return new UserStatement(this, stmt);
    else
      return stmt;
  }

  /**
   * Creates a statement.
   */
  public Statement createStatement(int resultSetType,
                                   int resultSetConcurrency,
                                   int resultSetHoldability)
    throws SQLException
  {
    Statement stmt;

    Connection conn = getDriverConnection();

    try {
      stmt = conn.createStatement(resultSetType,
                                  resultSetConcurrency,
                                  resultSetHoldability);
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }

    addStatement(stmt);

    if (_mConn.isWrapStatements())
      return new UserStatement(this, stmt);
    else
      return stmt;
  }

  /**
   * Returns a prepared statement with the given sql.
   *
   * @param sql the prepared sql.
   */
  public PreparedStatement prepareStatement(String sql)
    throws SQLException
  {
    PreparedStatement stmt;

    if (getDriverConnection() == null) {
      fatalEvent();
      throw new SQLException(L.l("can't create statement from closed connection."));
    }

    try {
      stmt = getMConn().prepareStatement(this, sql);
    } catch (SQLException e) {
      getMConn().fatalEvent(e);
      throw e;
    }

    addStatement(stmt);

    if (_mConn.isWrapStatements())
      return new UserPreparedStatement(this, stmt);
    else
      return stmt;
  }

  /**
   * Returns a prepared statement with the given sql.
   *
   * @param sql the prepared sql.
   */
  public PreparedStatement prepareStatement(String sql,
                                            int resultSetType,
                                            int resultSetConcurrency)
    throws SQLException
  {
    PreparedStatement stmt;

    if (getDriverConnection() == null) {
      fatalEvent();
      throw new SQLException(L.l("can't create statement from closed connection."));
    }

    try {
      stmt = getDriverConnection().prepareStatement(sql, resultSetType, resultSetConcurrency);
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }

    addStatement(stmt);

    if (_mConn.isWrapStatements())
      return new UserPreparedStatement(this, stmt);
    else
      return stmt;
  }

  /**
   * Returns a prepared statement with the given sql.
   *
   * @param sql the prepared sql.
   */
  public PreparedStatement prepareStatement(String sql,
                                            int resultSetType,
                                            int resultSetConcurrency,
                                            int resultSetHoldability)
    throws SQLException
  {
    PreparedStatement stmt;

    if (getDriverConnection() == null) {
      fatalEvent();
      throw new SQLException(L.l("can't create statement from closed connection."));
    }

    try {
      stmt = getDriverConnection().prepareStatement(sql,
                                              resultSetType,
                                              resultSetConcurrency,
                                              resultSetHoldability);
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }

    addStatement(stmt);

    if (_mConn.isWrapStatements())
      return new UserPreparedStatement(this, stmt);
    else
      return stmt;
  }

  /**
   * Returns a prepared statement with the given sql.
   *
   * @param sql the prepared sql.
   */
  public PreparedStatement prepareStatement(String sql,
                                            int resultSetType)
    throws SQLException
  {
    PreparedStatement stmt;

    if (getDriverConnection() == null) {
      fatalEvent();
      throw new SQLException(L.l("can't create statement from closed connection."));
    }

    try {
      stmt = getMConn().prepareStatement(this, sql, resultSetType);
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }

    if (stmt == null)
      throw new IllegalStateException(L.l("prepareStatement returned empty SQL\n{0}",
                                          sql));

    addStatement(stmt);

    if (_mConn.isWrapStatements())
      return new UserPreparedStatement(this, stmt);
    else
      return stmt;
  }

  /**
   * Returns a prepared statement with the given sql.
   *
   * @param sql the prepared sql.
   */
  public PreparedStatement prepareStatement(String sql,
                                            int []columnIndexes)
    throws SQLException
  {
    PreparedStatement stmt;

    if (getDriverConnection() == null) {
      fatalEvent();
      throw new SQLException(L.l("can't create statement from closed connection."));
    }

    try {
      stmt = getDriverConnection().prepareStatement(sql, columnIndexes);
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }

    addStatement(stmt);

    if (_mConn.isWrapStatements())
      return new UserPreparedStatement(this, stmt);
    else
      return stmt;
  }

  /**
   * Returns a prepared statement with the given sql.
   *
   * @param sql the prepared sql.
   */
  public PreparedStatement prepareStatement(String sql,
                                            String []columnNames)
    throws SQLException
  {
    PreparedStatement stmt;

    if (getDriverConnection() == null) {
      fatalEvent();
      throw new SQLException(L.l("can't create statement from closed connection."));
    }

    try {
      stmt = getDriverConnection().prepareStatement(sql, columnNames);
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }

    addStatement(stmt);

    if (_mConn.isWrapStatements())
      return new UserPreparedStatement(this, stmt);
    else
      return stmt;
  }

  public CallableStatement prepareCall(String sql, int resultSetType,
               int resultSetConcurrency)
    throws SQLException
  {
    CallableStatement stmt;

    if (getDriverConnection() == null) {
      fatalEvent();
      throw new SQLException(L.l("can't create statement from closed connection."));
    }

    try {
      stmt = getDriverConnection().prepareCall(sql, resultSetType, resultSetConcurrency);
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }

    addStatement(stmt);

    if (_mConn.isWrapStatements())
      return new UserCallableStatement(this, stmt);
    else
      return stmt;
  }

  public CallableStatement prepareCall(String sql)
    throws SQLException
  {
    CallableStatement stmt;

    if (getDriverConnection() == null) {
      fatalEvent();
      throw new SQLException(L.l("can't create statement from closed connection."));
    }

    try {
      stmt = getDriverConnection().prepareCall(sql);
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }

    addStatement(stmt);

    if (_mConn.isWrapStatements())
      return new UserCallableStatement(this, stmt);
    else
      return stmt;
  }

  public CallableStatement prepareCall(String sql,
                                       int resultSetType,
                                       int resultSetConcurrency,
                                       int resultSetHoldability)
    throws SQLException
  {
    CallableStatement stmt;

    if (getDriverConnection() == null) {
      fatalEvent();
      throw new SQLException(L.l("can't create statement from closed connection."));
    }

    try {
      stmt = getDriverConnection().prepareCall(sql,
                                         resultSetType,
                                         resultSetConcurrency,
                                         resultSetHoldability);
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }

    addStatement(stmt);

    if (_mConn.isWrapStatements())
      return new UserCallableStatement(this, stmt);
    else
      return stmt;
  }

  /**
   * JDBC api to return the connection's catalog.
   *
   * @return the JDBC catalog.
   */
  public String getCatalog()
    throws SQLException
  {
    try {
      return getDriverConnection().getCatalog();
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
    getMConn().setCatalog(catalog);
  }

  /**
   * Gets the connection's metadata.
   */
  public DatabaseMetaData getMetaData()
    throws SQLException
  {
    try {
      return getDriverConnection().getMetaData();
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }
  }

  /**
   * Returns the connection's type map.
   */
  public Map getTypeMap()
    throws SQLException
  {
    try {
      return getDriverConnection().getTypeMap();
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }
  }

  /**
   * Sets the connection's type map.
   */
  public void setTypeMap(Map<String,Class<?>> map)
    throws SQLException
  {
    getMConn().setTypeMap(map);
  }

  /**
   * Calls the nativeSQL method for the connection.
   */
  public String nativeSQL(String sql)
    throws SQLException
  {
    try {
      return getDriverConnection().nativeSQL(sql);
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }
  }

  public int getTransactionIsolation()
    throws SQLException
  {
    try {
      return getDriverConnection().getTransactionIsolation();
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }
  }

  public void setTransactionIsolation(int isolation)
    throws SQLException
  {
    getMConn().setTransactionIsolation(isolation);
  }

  public SQLWarning getWarnings()
    throws SQLException
  {
    try {
      Connection conn = getDriverConnection();

      if (conn != null)
        return conn.getWarnings();
      else
        return null;
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }
  }

  public void clearWarnings()
    throws SQLException
  {
    try {
      Connection conn = getDriverConnection();

      if (conn != null)
        conn.clearWarnings();
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }
  }

  public void setReadOnly(boolean readOnly)
    throws SQLException
  {
    getMConn().setReadOnly(readOnly);
  }

  public boolean isReadOnly()
    throws SQLException
  {
    try {
      return getDriverConnection().isReadOnly();
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }
  }

  public boolean getAutoCommit()
    throws SQLException
  {
    try {
      return getDriverConnection().getAutoCommit();
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }
  }

  public void setAutoCommit(boolean autoCommit)
    throws SQLException
  {
    getMConn().setAutoCommit(autoCommit);
  }

  public void commit()
    throws SQLException
  {
    try {
      Connection conn = getDriverConnection();

      if (conn != null)
        conn.commit();
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }
  }

  public void rollback()
    throws SQLException
  {
    try {
      Connection conn = getDriverConnection();

      if (conn != null)
        conn.rollback();
    } catch (SQLException e) {
      fatalEvent();
      throw e;
    }
  }

  /**
   * Returns true if the connection is closed.
   */
  public boolean isClosed()
    throws SQLException
  {
    try {
      return _mConn == null || getDriverConnection() == null || getDriverConnection().isClosed();
    } catch (SQLException e) {
      log.log(Level.FINER, e.toString(), e);

      return true;
    }
  }

  /**
   * Reset the connection and return the underlying JDBC connection to
   * the pool.
   */
  public void close() throws SQLException
  {
    ManagedConnectionImpl mConn;

    synchronized (this) {
      mConn = _mConn;
      _mConn = null;

      if (mConn == null)
        return;
    }

    try {
      // Clean up the connection and put it back in the pool
      resetConnection(mConn);
    } catch (Throwable e) {
      mConn.fatalEvent();

      log.log(Level.WARNING, e.toString(), e);
      /*
      if (e instanceof SQLException)
        throw (SQLException) e;
      else if (e instanceof RuntimeException)
        throw (RuntimeException) e;
      else
        throw new SQLExceptionWrapper(e);
      */
    } finally {
      mConn.closeEvent(this);
    }
  }

  /**
   * Adds the statement to a list, so they can be automatically closed
   * when the connection returns to the pool.
   */
  private void addStatement(Statement stmt)
  {
    if (_statement == null)
      _statement = stmt;
    else if (_statements != null)
      _statements.add(stmt);
    else {
      _statements = new ArrayList<Statement>();
      _statements.add(stmt);
    }
  }

  public void setHoldability(int hold)
    throws SQLException
  {
    getDriverConnection().setHoldability(hold);
  }

  public int getHoldability()
    throws SQLException
  {
    return getDriverConnection().getHoldability();
  }

  public Savepoint setSavepoint()
    throws SQLException
  {
    return getDriverConnection().setSavepoint();
  }

  public Savepoint setSavepoint(String name)
    throws SQLException
  {
    return getDriverConnection().setSavepoint(name);
  }

  public void releaseSavepoint(Savepoint savepoint)
    throws SQLException
  {
    getDriverConnection().releaseSavepoint(savepoint);
  }

  public void rollback(Savepoint savepoint)
    throws SQLException
  {
    getDriverConnection().rollback(savepoint);
  }

  @Override
  public Clob createClob()
    throws SQLException
  {
    return getDriverConnection().createClob();
  }

  @Override
  public Blob createBlob()
    throws SQLException
  {
    return getDriverConnection().createBlob();
  }

  @Override
  public NClob createNClob()
    throws SQLException
  {
    return getDriverConnection().createNClob();
  }

  @Override
  public SQLXML createSQLXML()
    throws SQLException
  {
    return getDriverConnection().createSQLXML();
  }

  @Override
  public boolean isValid(int timeout)
    throws SQLException
  {
    return getDriverConnection().isValid(timeout);
  }

  @Override
  public void setClientInfo(String name, String value)
    throws SQLClientInfoException
  {
      try {
        getDriverConnection().setClientInfo(name, value);
      } catch (SQLClientInfoException e) {
        throw e;
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
  }

  @Override
  public void setClientInfo(Properties properties)
    throws SQLClientInfoException
  {
      try {
        getDriverConnection().setClientInfo(properties);
      } catch (SQLClientInfoException e) {
        throw e;
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
  }

  @Override
  public String getClientInfo(String name)
    throws SQLException
  {
    try {
      return getDriverConnection().getClientInfo(name);
    } catch (SQLClientInfoException e) {
      throw e;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Properties getClientInfo()
    throws SQLException
  {
      return getDriverConnection().getClientInfo();
  }

  @Override
  public Array createArrayOf(String typeName, Object[] elements)
    throws SQLException
  {
      return getDriverConnection().createArrayOf(typeName, elements);
  }

  @Override
  public Struct createStruct(String typeName, Object[] attributes)
    throws SQLException
  {
    return getDriverConnection().createStruct(typeName, attributes);
  }

  @Override
  public <T> T unwrap(Class<T> iface)
    throws SQLException
  {
    return (T) getConnection();
  }

  @Override
  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    return iface.isAssignableFrom(getConnection().getClass());
  }

  /**
   * Resets the underlying connection to its initial state and closes
   * any open statements.
   */
  private void resetConnection(ManagedConnectionImpl mConn)
  {
    closeStatements(mConn);
  }

  /**
   * Closes a single statement.
   */
  void closeStatement(Statement stmt)
  {
    if (_statement == stmt)
      _statement = null;
    else if (_statements != null)
      _statements.remove(stmt);
  }

  /**
   * Closes the connection's statements.
   */
  private void closeStatements(ManagedConnectionImpl mConn)
  {
    ArrayList<Statement> statements = _statements;
    _statements = null;

    Statement stmt = _statement;
    _statement = null;

    try {
      if (stmt != null)
        stmt.close();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      // Can't set fatalEvent because Sybase throws an exception
      // if statements are closed twice
      // fatalEvent();
    }

    for (int i = 0; statements != null && i < statements.size(); i++) {
      try {
        stmt = statements.get(i);

        if (stmt != null)
          stmt.close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);

        // Can't set fatalEvent because Sybase throws an exception
        // if statements are closed twice
        // fatalEvent();
      }
    }
  }

  /**
   * Returns the underlying connection.
   */
  public Connection getDriverConnection()
    throws SQLException
  {
    ManagedConnectionImpl mConn = getMConn();

    if (mConn == null)
      throw new IllegalStateException(L.l("Cannot use closed connection.  Check max-active-time and review application code. "));

    Connection conn = mConn.getDriverConnection();

    return conn;
  }

  /**
   * Returns the underlying connection.
   */
  public ManagedConnectionImpl getMConn()
  {
    ManagedConnectionImpl mConn = _mConn;

    if (mConn == null)
      throw new IllegalStateException("connection is closed");

    return mConn;
  }

  /**
   * Marks the connection as error.
   */
  public void discardConnection()
  {
    fatalEvent();
  }

  /**
   * Returns the underlying connection.
   */
  private void fatalEvent()
  {
    ManagedConnectionImpl mConn = _mConn;

    if (mConn != null)
      mConn.fatalEvent();
  }

  /**
   * Returns the underlying connection.
   */
  private void fatalEvent(SQLException exn)
  {
    ManagedConnectionImpl mConn = _mConn;

    if (mConn != null)
      mConn.fatalEvent(exn);
  }

  /**
   * Returns the underlying connection.
   */
  void killPool()
  {
    ManagedConnectionImpl mConn = _mConn;

    if (mConn != null)
      mConn.killPool();
  }

  protected void finalize()
    throws Exception
  {
    close();
  }

  public String toString()
  {
    return "UserConnection[" + _mConn + "]";
  }
}
