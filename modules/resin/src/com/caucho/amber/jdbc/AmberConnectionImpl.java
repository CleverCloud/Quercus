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

package com.caucho.amber.jdbc;

import java.sql.*;
import java.util.Map;
import java.util.Properties;

/**
 * Wrapper of the JDBC Connection.
 */
public class AmberConnectionImpl implements Connection {
  private Connection _conn;

  /**
   * Returns the underlying connection.
   */
  public Connection getConnection()
  {
    return _conn;
  }

  /**
   * JDBC api to return the connection's catalog.
   *
   * @return the JDBC catalog.
   */
  public String getCatalog()
    throws SQLException
  {
    return getConnection().getCatalog();
  }

  /**
   * Sets the JDBC catalog.
   */
  public void setCatalog(String catalog)
    throws SQLException
  {
    getConnection().setCatalog(catalog);
  }

  /**
   * Gets the connection's metadata.
   */
  public DatabaseMetaData getMetaData()
    throws SQLException
  {
    return getConnection().getMetaData();
  }

  /**
   * Returns the connection's type map.
   */
  public Map getTypeMap()
    throws SQLException
  {
    return getConnection().getTypeMap();
  }

  /**
   * Sets the connection's type map.
   */
  public void setTypeMap(Map<String,Class<?>> map)
    throws SQLException
  {
    getConnection().setTypeMap(map);
  }

  /**
   * Calls the nativeSQL method for the connection.
   */
  public String nativeSQL(String sql)
    throws SQLException
  {
    return getConnection().nativeSQL(sql);
  }

  public int getTransactionIsolation()
    throws SQLException
  {
    return getConnection().getTransactionIsolation();
  }

  public void setTransactionIsolation(int isolation)
    throws SQLException
  {
    getConnection().setTransactionIsolation(isolation);
  }

  public SQLWarning getWarnings()
    throws SQLException
  {
    return getConnection().getWarnings();
  }

  public void clearWarnings()
    throws SQLException
  {
    getConnection().clearWarnings();
  }

  public void setReadOnly(boolean readOnly)
    throws SQLException
  {
    getConnection().setReadOnly(readOnly);
  }

  public boolean isReadOnly()
    throws SQLException
  {
    return getConnection().isReadOnly();
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
    return getConnection().createStatement();
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
    return getConnection().createStatement(resultSetType, resultSetConcurrency);
  }
  
  public Statement createStatement(int resultSetType,
                                   int resultSetConcurrency,
                                   int resultSetHoldability)
    throws SQLException
  {
    return getConnection().createStatement(resultSetType,
                                           resultSetConcurrency,
                                           resultSetHoldability);
  }


  public PreparedStatement prepareStatement(String sql)
    throws SQLException
  {
    return getConnection().prepareStatement(sql);
  }
  
  public PreparedStatement prepareStatement(String sql,
                                            int resultSetType)
    throws SQLException
  {
    return getConnection().prepareStatement(sql, resultSetType);
  }

  public PreparedStatement prepareStatement(String sql, int resultSetType,
                                            int resultSetConcurrency)
    throws SQLException
  {
    return getConnection().prepareStatement(sql, resultSetType,
                                            resultSetConcurrency);
  }

  public PreparedStatement prepareStatement(String sql,
                                            int resultSetType,
                                            int resultSetConcurrency,
                                            int resultSetHoldability)
    throws SQLException
  {
    return getConnection().prepareStatement(sql, resultSetType,
                                            resultSetConcurrency,
                                            resultSetHoldability);
  }
  
  public PreparedStatement prepareStatement(String sql,
                                            int []columnIndexes)
    throws SQLException
  {
    return getConnection().prepareStatement(sql, columnIndexes);
  }
  
  public PreparedStatement prepareStatement(String sql,
                                            String []columnNames)
    throws SQLException
  {
    return getConnection().prepareStatement(sql, columnNames);
  }

  public CallableStatement prepareCall(String sql)
    throws SQLException
  {
    return getConnection().prepareCall(sql);
  }

  public CallableStatement prepareCall(String sql, int resultSetType,
                                       int resultSetConcurrency)
    throws SQLException
  {
    return getConnection().prepareCall(sql, resultSetType,
                                       resultSetConcurrency);
  }

  public CallableStatement prepareCall(String sql,
                                       int resultSetType,
                                       int resultSetConcurrency,
                                       int resultSetHoldability)
    throws SQLException
  {
    return getConnection().prepareCall(sql, resultSetType,
                                       resultSetConcurrency,
                                       resultSetHoldability);
  }

  public boolean getAutoCommit()
    throws SQLException
  {
    return getConnection().getAutoCommit();
  }

  public void setAutoCommit(boolean autoCommit)
    throws SQLException
  {
    getConnection().setAutoCommit(autoCommit);
  }

  public void commit()
    throws SQLException
  {
    getConnection().commit();
  }

  public void rollback()
    throws SQLException
  {
    getConnection().rollback();
  }

  /**
   * Returns true if the connection is closed.
   */
  public boolean isClosed()
    throws SQLException
  {
    return getConnection().isClosed();
  }

  /**
   * Reset the connection and return the underlying JDBC connection to
   * the pool.
   */
  public void close() throws SQLException
  {
    getConnection().close();
  }

  public void setHoldability(int hold)
    throws SQLException
  {
    getConnection().setHoldability(hold);
  }

  public int getHoldability()
    throws SQLException
  {
    return getConnection().getHoldability();
  }

  public Savepoint setSavepoint()
    throws SQLException
  {
    return getConnection().setSavepoint();
  }

  public Savepoint setSavepoint(String name)
    throws SQLException
  {
    return getConnection().setSavepoint(name);
  }

  public void releaseSavepoint(Savepoint savepoint)
    throws SQLException
  {
    getConnection().releaseSavepoint(savepoint);
  }

  public void rollback(Savepoint savepoint)
    throws SQLException
  {
    getConnection().rollback(savepoint);
  }

  public String toString()
  {
    return "AmberConnection[" + _conn + "]";
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
