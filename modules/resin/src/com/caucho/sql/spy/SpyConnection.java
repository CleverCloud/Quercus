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

package com.caucho.sql.spy;

import com.caucho.util.L10N;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.logging.*;

/**
 * Spying on a connection.
 */
public class SpyConnection implements java.sql.Connection {
  protected final static Logger log
    = Logger.getLogger(SpyConnection.class.getName());
  protected final static Logger logXA =
    Logger.getLogger(SpyConnection.class.getName() + ".XA");
  protected final static L10N L = new L10N(SpyConnection.class);

  private SpyDataSource _spyDataSource;
  
  private String _id;
  private int _stmtIdCount;

  // The underlying connection
  private Connection _conn;

  /**
   * Creates a new SpyConnection.
   */
  public SpyConnection(Connection conn, SpyDataSource spyDataSource)
  {
    _spyDataSource = spyDataSource;
    _conn = conn;
  }

  /**
   * Creates a new SpyConnection.
   */
  public SpyConnection(Connection conn,
                       SpyDataSource spyDataSource,
                       String id)
  {
    _spyDataSource = spyDataSource;
    _conn = conn;
    _id = id;
  }

  /**
   * Returns the underlying connection.
   */
  public Connection getConnection()
  {
    return _conn;
  }

  /**
   * Returns the id.
   */
  public String getId()
  {
    if (_id == null)
      _id = _spyDataSource.createConnectionId();

    return _id;
  }

  /**
   * Returns a new statement id.
   */
  public String createStatementId()
  {
    return getId() + "." + _stmtIdCount++;
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
      String catalog = _conn.getCatalog();

      log.fine(getId() + ":getCatalog() -> " + catalog);
      
      return catalog;
    } catch (SQLException e) {
      log.fine(getId() + ":exn-getCatalog(" + e + ")");
      
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
      log.fine(getId() + ":setCatalog(" + catalog + ")");
    
      _conn.setCatalog(catalog);
    } catch (SQLException e) {
      log.fine(getId() + ":exn-setCatalog(" + e + ")");
      throw e;
    }
  }

  /**
   * Gets the connection's metadata.
   */
  public DatabaseMetaData getMetaData()
    throws SQLException
  {
    try {
      DatabaseMetaData metaData = _conn.getMetaData();

      // Avoid logging result of metaData.toString() here
      // because it may not be overloaded. The default
      // toString() result contains a hex representation
      // of the memory location that can't be matched in a
      // regression test.

      String ident = metaData.getClass().getName();

      log.fine(getId() + ":getMetaData() -> " + ident);

      return metaData;
    } catch (SQLException e) {
      log.fine(getId() + ":exn-getMetaData(" + e + ")");
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
      Map map = _conn.getTypeMap();

      log.fine(getId() + ":getTypeMap() -> " + map);
      
      return map;
    } catch (SQLException e) {
      log.fine(getId() + ":exn-getTypeMap(" + e + ")");
      throw e;
    }
  }

  /**
   * Sets the connection's type map.
   */
  public void setTypeMap(Map<String,Class<?>> map)
    throws SQLException
  {
    try {
      log.fine(getId() + ":setTypeMap(" + map + ")");
      
      _conn.setTypeMap(map);
    } catch (SQLException e) {
      log.fine(getId() + ":exn-setTypeMap(" + e + ")");

      throw e;
    }
  }

  /**
   * Calls the nativeSQL method for the connection.
   */
  public String nativeSQL(String sql)
    throws SQLException
  {
    try {
      String nativeSQL = _conn.nativeSQL(sql);

      log.fine(getId() + ":nativeSQL() -> " + nativeSQL);
      
      return nativeSQL;
    } catch (SQLException e) {
      log.fine(getId() + ":exn-nativeSQL(" + e + ")");

      throw e;
    }
  }

  public int getTransactionIsolation()
    throws SQLException
  {
    try {
      int isolation = _conn.getTransactionIsolation();

      log.fine(getId() + ":getTransactionIsolation() -> " + isolation);
      
      return isolation;
    } catch (SQLException e) {
      log.fine(getId() + ":exn-getTransactionIsolation(" + e + ")");

      throw e;
    }
  }

  public void setTransactionIsolation(int isolation)
    throws SQLException
  {
    try {
      log.fine(getId() + ":setTransactionIsolation(" + isolation + ")");
      
      _conn.setTransactionIsolation(isolation);
    } catch (SQLException e) {
      log.fine(getId() + ":exn-setTransactionIsolation(" + e + ")");

      throw e;
    }
  }

  public SQLWarning getWarnings()
    throws SQLException
  {
    try {
      SQLWarning warning = _conn.getWarnings();
      
      log.fine(getId() + ":getWarnings() -> " + warning);
      
      return warning;
    } catch (SQLException e) {
      log.fine(getId() + ":exn-getWarnings(" + e + ")");

      throw e;
    }
  }

  public void clearWarnings()
    throws SQLException
  {
    try {
      log.fine(getId() + ":clearWarnings()");

      _conn.clearWarnings();
    } catch (SQLException e) {
      log.fine(getId() + ":exn-clearWarnings(" + e + ")");

      throw e;
    }
  }

  public void setReadOnly(boolean readOnly)
    throws SQLException
  {
    try {
      log.fine(getId() + ":setReadOnly(" + readOnly + ")");
      
      _conn.setReadOnly(readOnly);
    } catch (SQLException e) {
      log.fine(getId() + ":exn-setReadOnly(" + e + ")");

      throw e;
    }
  }

  public boolean isReadOnly()
    throws SQLException
  {
    try {
      boolean isReadOnly = _conn.isReadOnly();

      log.fine(getId() + "isReadOnly() -> " + isReadOnly);

      return isReadOnly;

    } catch (SQLException e) {
      log.fine(getId() + ":exn-isReadOnly(" + e + ")");

      throw e;
    }
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
    try {
      String stmtId = null;

      if (log.isLoggable(Level.FINE)) {
        stmtId = createStatementId();
        log.fine(stmtId + ":createStatement()");
      }
      
      Statement stmt;
      
      stmt = _conn.createStatement();
      
      return new SpyStatement(stmtId, this, stmt);
    } catch (SQLException e) {
      log.fine(getId() + ":exn-createStatement(" + e + ")");
      throw e;
    }
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
    try {
      String stmtId = null;

      if (log.isLoggable(Level.FINE)) {
        stmtId = createStatementId();
      
        log.fine(stmtId + ":createStatement(type=" + resultSetType +
                 ",concurrency=" + resultSetConcurrency + ")");
      }
      
      Statement stmt;
      
      stmt = _conn.createStatement(resultSetType, resultSetConcurrency);
      
      return new SpyStatement(stmtId, this, stmt);
    } catch (SQLException e) {
      log.fine(getId() + ":exn-createStatement(" + e + ")");
      throw e;
    }
  }
  
  public Statement createStatement(int resultSetType,
                                   int resultSetConcurrency,
                                   int resultSetHoldability)
    throws SQLException
  {
    try {
      String stmtId = null;

      if (log.isLoggable(Level.FINE)) {
        stmtId = createStatementId();
      
        log.fine(stmtId + ":createStatement(type=" + resultSetType +
                 ",concurrency=" + resultSetConcurrency +
                 ",holdability=" + resultSetHoldability + ")");
      }
      
      Statement stmt;
      
      stmt = _conn.createStatement(resultSetType,
                                   resultSetConcurrency,
                                   resultSetHoldability);
      
      return new SpyStatement(stmtId, this, stmt);
    } catch (SQLException e) {
      log.fine(getId() + ":exn-createStatement(" + e + ")");
      throw e;
    }
  }


  public PreparedStatement prepareStatement(String sql)
    throws SQLException
  {
    try {
      String stmtId = null;

      if (log.isLoggable(Level.FINE)) {
        stmtId = createStatementId();
        log.fine(stmtId + ":prepareStatement(" + sql + ")");
      }
      
      PreparedStatement stmt;
      
      stmt = _conn.prepareStatement(sql);
      
      return new SpyPreparedStatement(stmtId, this, stmt, sql);
    } catch (SQLException e) {
      log.fine(getId() + ":exn-prepareStatement(" + e + ")");

      throw e;
    }
  }
  
  public PreparedStatement prepareStatement(String sql,
                                            int resultSetType)
    throws SQLException
  {
    try {
      String stmtId = null;

      if (log.isLoggable(Level.FINE)) {
        stmtId = createStatementId();
        log.fine(stmtId + ":prepareStatement(" + sql + ",type=" + resultSetType + ")");
      }
      
      PreparedStatement stmt;
      
      stmt = _conn.prepareStatement(sql, resultSetType);
      
      return new SpyPreparedStatement(stmtId, this, stmt, sql);
    } catch (SQLException e) {
      log.fine(getId() + ":exn-prepareStatement(" + e + ")");

      throw e;
    }
  }

  public PreparedStatement prepareStatement(String sql, int resultSetType,
                                            int resultSetConcurrency)
    throws SQLException
  {
    try {
      String stmtId = null;

      if (log.isLoggable(Level.FINE)) {
        stmtId = createStatementId();

        log.fine(stmtId + ":prepareStatement(" + sql + ",type=" + resultSetType +
                 ",concurrency=" + resultSetConcurrency + ")");
      }
      
      PreparedStatement stmt;
      
      stmt = _conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
      
      return new SpyPreparedStatement(stmtId, this, stmt, sql);
    } catch (SQLException e) {
      log.fine(getId() + ":exn-prepareStatement(" + e + ")");

      throw e;
    }
  }

  public PreparedStatement prepareStatement(String sql,
                                            int resultSetType,
                                            int resultSetConcurrency,
                                            int resultSetHoldability)
    throws SQLException
  {
    return null;
  }
  
  public PreparedStatement prepareStatement(String sql,
                                            int []columnIndexes)
    throws SQLException
  {
    return null;
  }
  
  public PreparedStatement prepareStatement(String sql,
                                            String []columnNames)
    throws SQLException
  {
    return null;
  }

  public CallableStatement prepareCall(String sql)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":prepareCall(" + sql + ")");
      
      CallableStatement stmt;
      
      stmt = _conn.prepareCall(sql);
      
      return stmt;
    } catch (SQLException e) {
      log.fine(getId() + ":exn-prepareCall(" + e + ")");

      throw e;
    }
  }

  public CallableStatement prepareCall(String sql, int resultSetType,
                                       int resultSetConcurrency)
    throws SQLException
  {
    try {
      if (log.isLoggable(Level.FINE))
        log.fine(getId() + ":prepareCall(" + sql + ",type=" + resultSetType +
              ",concurrency=" + resultSetConcurrency + ")");
      
      CallableStatement stmt;
      
      stmt = _conn.prepareCall(sql);
      
      return stmt;
    } catch (SQLException e) {
      log.fine(getId() + ":exn-prepareCall(" + e + ")");

      throw e;
    }
  }

  public CallableStatement prepareCall(String sql,
                                       int resultSetType,
                                       int resultSetConcurrency,
                                       int resultSetHoldability)
    throws SQLException
  {
    return null;
  }

  public boolean getAutoCommit()
    throws SQLException
  {
    try {
      boolean autoCommit = _conn.getAutoCommit();
      
      log.fine(getId() + ":getAutoCommit() -> " + autoCommit);
      
      return autoCommit;
    } catch (SQLException e) {
      log.fine(getId() + ":exn-getAutoCommit(" + e + ")");

      throw e;
    }
  }

  public void setAutoCommit(boolean autoCommit)
    throws SQLException
  {
    try {
      logXA.fine(getId() + ":setAutoCommit(" + autoCommit + ")");
      
      _conn.setAutoCommit(autoCommit);
    } catch (SQLException e) {
      logXA.fine(getId() + ":exn-setAutoCommit(" + e + ")");

      throw e;
    }
  }

  public void commit()
    throws SQLException
  {
    try {
      logXA.fine(getId() + ":commit()");

      _conn.commit();
    } catch (SQLException e) {
      logXA.fine(getId() + ":exn-commit(" + e + ")");

      throw e;
    }
  }

  public void rollback()
    throws SQLException
  {
    try {
      logXA.fine(getId() + ":rollback()");
      
      _conn.rollback();
    } catch (SQLException e) {
      logXA.fine(getId() + ":exn-rollback(" + e + ")");

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
      boolean isClosed = _conn.isClosed();

      log.fine(getId() + ":isClosed() -> " + isClosed);
      
      return isClosed;
    } catch (SQLException e) {
      log.fine(getId() + ":exn-isClosed(" + e + ")");

      throw e;
    }
  }

  /**
   * Reset the connection and return the underlying JDBC connection to
   * the pool.
   */
  public void close() throws SQLException
  {
    log.fine(getId() + ":close()");

    try {
      _conn.close();
    } catch (SQLException e) {
      log.fine(getId() + ":exn-close(" + e + ")");

      throw e;
    }
  }

  public void setHoldability(int hold)
    throws SQLException
  {
    _conn.setHoldability(hold);
  }

  public int getHoldability()
    throws SQLException
  {
    return _conn.getHoldability();
  }

  public Savepoint setSavepoint()
    throws SQLException
  {
    return _conn.setSavepoint();
  }

  public Savepoint setSavepoint(String name)
    throws SQLException
  {
    return _conn.setSavepoint(name);
  }

  public void releaseSavepoint(Savepoint savepoint)
    throws SQLException
  {
    _conn.releaseSavepoint(savepoint);
  }

  public void rollback(Savepoint savepoint)
    throws SQLException
  {
    _conn.rollback(savepoint);
  }

  public String toString()
  {
    return "SpyConnection[id=" + getId() + ",conn=" + _conn + "]";
  }

    public Clob createClob() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Blob createBlob() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NClob createNClob() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SQLXML createSQLXML() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isValid(int timeout) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getClientInfo(String name) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Properties getClientInfo() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
