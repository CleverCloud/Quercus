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
package com.caucho.db.jdbc;

import com.caucho.db.Database;
import com.caucho.db.sql.Query;
import com.caucho.db.xa.Transaction;
import com.caucho.util.L10N;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The JDBC connection implementation.
 */
public class ConnectionImpl implements java.sql.Connection {
  private static final L10N L = new L10N(ConnectionImpl.class);
  private static final Logger log
    = Logger.getLogger(ConnectionImpl.class.getName());

  private Database _db;
  private PooledConnectionImpl _pooledConnection;
  
  private boolean _isClosed;
  private boolean _isAutoCommit = true;

  private Transaction _xa;

  private StatementImpl _statement;
  private ArrayList<StatementImpl> _statements;
  
  public ConnectionImpl(PooledConnectionImpl pooledConnection)
  {
    _pooledConnection = pooledConnection;
    _db = pooledConnection.getDatabase();

    if (_db == null)
      throw new NullPointerException();
  }
  
  public ConnectionImpl(Database db)
  {
    _db = db;

    if (_db == null)
      throw new NullPointerException();
  }

  Database getDatabase()
  {
    return _db;
  }

  public void clearWarnings()
  {
  }

  public void setTransaction(Transaction xa)
  {
    _xa = xa;
  }

  public Transaction getTransaction()
  {
    if (_isAutoCommit) {
      Transaction xa = Transaction.create(this);
      // XXX: value?
      // xa.setTransactionTimeout(15000);
      xa.setAutoCommit(true);
      return xa;
    }
    else if (_xa == null) {
      _xa = Transaction.create(this);
      
      if (log.isLoggable(Level.FINER))
        log.finer("start transaction " + this + " " + _xa);
    }

    _xa.setAutoCommit(false);

    return _xa;
  }
  
  public void commit()
    throws SQLException
  {
    if (log.isLoggable(Level.FINER))
      log.finer("commit " + this + " " + _xa);
    
    Transaction xa = _xa;
    _xa = null;
    
    if (xa != null)
      xa.commit();
  }

  public void rollback()
    throws SQLException
  {
    Transaction xa = _xa;
    _xa = null;

    if (xa != null) {
      if (log.isLoggable(Level.FINER))
        log.finer("rollback " + this + " " + _xa);
    
      xa.rollback();
    }
  }

  public java.sql.Statement createStatement()
    throws SQLException
  {
    if (_db == null)
      throw new SQLException(L.l("Connection is already closed"));
    
    StatementImpl stmt = new StatementImpl(this);

    if (_statement == null)
      _statement = stmt;
    else {
      if (_statements == null)
        _statements = new ArrayList<StatementImpl>();
      _statements.add(stmt);
    }
    
    return stmt;
  }

  public java.sql.Statement createStatement(int resultSetType,
                                            int resultSetConcurrency)
    throws SQLException
  {
    return createStatement();
  }

  public boolean getAutoCommit()
  {
    return _isAutoCommit;
  }

  public void setAutoCommit(boolean autoCommit)
    throws SQLException
  {
    if (! _isAutoCommit && autoCommit) {
      Transaction xa = _xa;
      _xa = null;
    
      if (xa != null)
        xa.commit();
    }
    
    _isAutoCommit = autoCommit;
  }

  public String getCatalog()
  {
    return null;
  }

  public void setCatalog(String catalog)
    throws SQLException
  {
  }

  public java.sql.DatabaseMetaData getMetaData()
    throws SQLException
  {
    return new DatabaseMetaDataImpl(this);
  }

  public int getTransactionIsolation()
  {
    return TRANSACTION_NONE;
  }

  public void setTransactionIsolation(int level)
  {
  }

  public Map getTypeMap()
  {
    return null;
  }

  public void setTypeMap(Map<String,Class<?>> map)
  {
  }

  public SQLWarning getWarnings()
  {
    return null;
  }

  public boolean isClosed()
  {
    return _isClosed;
  }

  public boolean isReadOnly()
  {
    return false;
  }

  public void setReadOnly(boolean readOnly)
  {
  }

  public String nativeSQL(String sql)
  {
    return null;
  }

  public CallableStatement prepareCall(String sql)
    throws SQLException
  {
    return null;
  }

  public CallableStatement prepareCall(String sql, int resultSetType,
                                       int resultSetConcurrency)
    throws SQLException
  {
    return null;
  }

  public java.sql.PreparedStatement prepareStatement(String sql)
    throws SQLException
  {
    return prepareStatementImpl(sql);
  }
  
  public java.sql.PreparedStatement prepareStatement(String sql,
                                                     int autoGeneratedKeys)
    throws SQLException
  {
    PreparedStatementImpl pstmt = prepareStatementImpl(sql);

    if (autoGeneratedKeys == Statement.RETURN_GENERATED_KEYS)
      pstmt.setReturnGeneratedKeys(true);
    
    return pstmt;
  }

  public java.sql.PreparedStatement prepareStatement(String sql,
                                                     int []columnIndices)
    throws SQLException
  {
    PreparedStatementImpl pstmt = prepareStatementImpl(sql);

    pstmt.setReturnGeneratedKeys(true);
    
    return pstmt;
  }

  public java.sql.PreparedStatement prepareStatement(String sql,
                                                     String []columnNames)
    throws SQLException
  {
    PreparedStatementImpl pstmt = prepareStatementImpl(sql);

    pstmt.setReturnGeneratedKeys(true);
    
    return pstmt;
  }

  public java.sql.PreparedStatement prepareStatement(String sql,
                                                     int resultSetType,
                                                     int resultSetConcurrency)
    throws SQLException
  {
    return prepareStatement(sql);
  }

  public java.sql.PreparedStatement prepareStatement(String sql,
                                                     int resultSetType,
                                                     int resultSetConcurrency,
                                                     int resultSetHoldability)
    throws SQLException
  {
    return prepareStatement(sql);
  }
  
  /**
   * Prepares the statement implementation.
   */
  private PreparedStatementImpl prepareStatementImpl(String sql)
    throws SQLException
  {
    Query query = _db.parseQuery(sql);
    
    PreparedStatementImpl stmt = new PreparedStatementImpl(this, query);

    if (_statement == null)
      _statement = stmt;
    else {
      if (_statements == null)
        _statements = new ArrayList<StatementImpl>();
      _statements.add(stmt);
    }
    
    return stmt;
  }

  public void rollback(Savepoint savepoint)
    throws SQLException
  {
  }

  public void releaseSavepoint(Savepoint savepoint)
    throws SQLException
  {
  }

  public Savepoint setSavepoint(String savepoint)
    throws SQLException
  {
    return null;
  }

  public Savepoint setSavepoint()
    throws SQLException
  {
    return null;
  }

  public int getHoldability()
    throws SQLException
  {
    return 0;
  }

  public void setHoldability(int hold)
    throws SQLException
  {
  }

  public java.sql.Statement createStatement(int resultSetType,
                                            int resultSetConcurrency,
                                            int resultSetHoldability)
    throws SQLException
  {
    return createStatement();
  }

  public CallableStatement prepareCall(String sql, int resultSetType,
                                       int resultSetConcurrency,
                                       int holdability)
    throws SQLException
  {
    return null;
  }

  void closeStatement(StatementImpl stmt)
  {
    if (_statement == stmt)
      _statement = null;

    if (_statements != null)
      _statements.remove(stmt);
  }

  public void close()
    throws SQLException
  {
    synchronized (this) {
      if (_isClosed)
        return;
      
      _isClosed = true;
      _db = null;
    }

    StatementImpl stmt = _statement;
    _statement = null;
    
    if (stmt != null)
      _statement = null;

    if (_statements != null) {
      for (int i = 0; i < _statements.size(); i++) {
        stmt = _statements.get(i);

        stmt.close();
      }
    }

    if (_pooledConnection != null)
      _pooledConnection.closeEvent(this);
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
