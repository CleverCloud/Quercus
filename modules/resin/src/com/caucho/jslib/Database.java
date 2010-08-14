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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jslib;

import com.caucho.util.Exit;
import com.caucho.util.ExitListener;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Encapsulates a database connection for JavaScript.
 *
 * <code><pre>
 * var conn = new Database("jdbc/test");
 * var rs = conn.query("select NAME from HOUSES");
 *
 * while (rs.next())
 *   out.writeln(rs.get(1) + "&lt;br/>");
 *
 * conn.close();
 * </pre></code>
 */
public class Database {
  private DataSource dataSource;
  private Connection conn;
  private Statement stmt;
  private ResultSet rs;

  /**
   * Creates a new database looking up the DataSource in JNDI.
   *
   * @param name jndi name to the data source
   */
  public Database(String name)
    throws Exception
  {
    InitialContext cxt = null;

    try {
      cxt = new InitialContext();
    } catch (Exception e) {
    }
    
    try {
      if (cxt != null)
        dataSource = (DataSource) cxt.lookup(name);
    } catch (Exception e) {
    }

    try {
      if (dataSource == null && cxt != null)
        dataSource = (DataSource) cxt.lookup("java:comp/env/" + name);
    } catch (Exception e) {
    }

    try {
      if (dataSource != null && cxt != null)
        dataSource = (DataSource) cxt.lookup("java:comp/env/jdbc/" + name);
    } catch (Exception e) {
    }

    if (dataSource == null)
      throw new SQLException("no data source: " + name);

    // Automatically reclaim the database when the script ends
    Exit.addExit(exitHandler, this);
  }

  /**
   * Execute the sql as a query.
   */
  public ResultSet query(String sql)
    throws SQLException
  {
    if (rs != null) {
      ResultSet rs = this.rs;
      this.rs = null;
      rs.close();
    }
    
    Statement stmt = getStatement();

    rs = stmt.executeQuery(sql);

    return rs;
  }

  /**
   * Execute the sql as a query.
   */
  public int update(String sql)
    throws SQLException
  {
    if (rs != null) {
      ResultSet rs = this.rs;
      this.rs = null;
      rs.close();
    }

    Connection conn = dataSource.getConnection();

    try {
      Statement stmt = conn.createStatement();
    
      int count = stmt.executeUpdate(sql);

      stmt.close();

      return count;
    } finally {
      conn.close();
    }
  }

  /**
   * Returns the JDBC DataSource.  Applications that
   * need direct access to the data source.
   */
  public DataSource getDataSource()
    throws SQLException
  {
    return dataSource;
  }

  /**
   * Returns the JDBC Connection for the database.  Applications that need
   * direct access to the Connection can use this.
   */
  public Connection getConnection()
    throws SQLException
  {
    if (conn == null) {
      conn = dataSource.getConnection();
    }

    return conn;
  }

  /**
   * Commits the current connection.
   */
  public void commit()
    throws SQLException
  {
    if (this.conn != null) {
      Connection conn = this.conn;
      this.conn = null;
      this.stmt = null;
      this.rs = null;
      conn.close();
    }
  }

  /**
   * Returns the JDBC Statement for the database.  Applications that
   * need direct access to the Java Statement can use this.
   */
  public Statement getStatement()
    throws SQLException
  {
    if (stmt == null) {
      Connection conn = getConnection();
    
      stmt = conn.createStatement();
    }

    return stmt;
  }

  /**
   * Returns the JDBC Statement for the database.  Applications that
   * need direct access to the Java Statement can use this.
   */
  public PreparedStatement prepare(String sql)
    throws SQLException
  {
    Connection conn = getConnection();
    
    return conn.prepareStatement(sql);
  }

  /**
   * Close the connection.  Automatically closes the ResultSet, Statement
   * and Connection.
   */
  public void close()
    throws SQLException
  {
    try {
      if (rs != null) {
        ResultSet rs = this.rs;
        this.rs = null;
        rs.close();
      }
      
      if (stmt != null) {
        Statement stmt = this.stmt;
        this.stmt = null;
        stmt.close();
      }
    } finally {
      this.stmt = null;
      this.rs = null;
      if (conn != null) {
        Connection conn = this.conn;
        this.conn = null;
        conn.close();
      }
    }
  }

  /**
   * Listens for the exit event.  When Database is used in JavaScript,
   * the database will be automatically closed on exit.
   */
  private static ExitListener exitHandler = new ExitListener() {
    public void handleExit(Object o)
    {
      Database db = (Database) o;

      try {
        db.close();
      } catch (SQLException e) {
      }
    }
  };
}
