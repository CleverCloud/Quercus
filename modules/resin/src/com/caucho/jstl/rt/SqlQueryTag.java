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

package com.caucho.jstl.rt;

import com.caucho.jstl.ResultImpl;
import com.caucho.util.L10N;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.jstl.sql.Result;
import javax.servlet.jsp.jstl.sql.SQLExecutionTag;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqlQueryTag extends BodyTagSupport implements SQLExecutionTag {
  private static final Logger log
    = Logger.getLogger(SqlQueryTag.class.getName());
  private static final L10N L = new L10N(SqlQueryTag.class);
  
  private String _sql;
  private String _var;
  private String _scope;
  private Object _dataSource;
  
  private int _maxRows = -1;
  private boolean _hasMaxRows;
  
  private int _startRow = -1;

  private ArrayList<Object> _params;

  /**
   * Sets the SQL.
   */
  public void setSql(String sql)
  {
    _sql = sql;
  }

  /**
   * Sets the variable name.
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the scope.
   */
  public void setScope(String scope)
  {
    _scope = scope;
  }

  /**
   * Sets the data source.
   */
  public void setDataSource(Object dataSource)
    throws JspException
  {
    _dataSource = dataSource;

    if (this.pageContext.getAttribute("caucho.jstl.sql.conn") != null)
      throw new JspException(L.l("sql:query cannot set data-source inside sql:transaction"));
  }

  /**
   * Sets the maximum number of rows.
   */
  public void setMaxRows(int maxRows)
    throws JspException
  {
    _maxRows = maxRows;
    _hasMaxRows = true;
  }

  /**
   * Sets the start row.
   */
  public void setStartRow(int startRow)
  {
    _startRow = startRow;
  }

  /**
   * Adds a parameter.
   */
  public void addSQLParameter(Object value)
  {
    if (_params == null)
      _params = new ArrayList<Object>();
    
    _params.add(value);
  }

  public int doEndTag() throws JspException
  {
    Connection conn = null;
    boolean isTransaction = false;
    
    String sql = null;

    try {
      if (_sql != null)
        sql = _sql;
      else
        sql = bodyContent.getString();

      conn = (Connection) pageContext.getAttribute("caucho.jstl.sql.conn");
      if (conn != null)
        isTransaction = true;

      if (! isTransaction) {
        conn = getConnection(pageContext, _dataSource);
      }

      Object value = null;

      ResultSet rs;

      ArrayList params = _params;
      _params = null;
      Statement stmt;

      // jsp/1f07
      int paramCount = countParameters(sql);

      if (params == null && paramCount != 0
          || params != null && paramCount != params.size()) {
        throw new SQLException(L.l("sql:param does not match expected parameters\nin '{0}'",
                                   sql));
      }

      int maxRows = -1;

      if (_hasMaxRows)
        maxRows = _maxRows;
      else {
        Object maxRowsValue
          = Config.find(pageContext, Config.SQL_MAX_ROWS);

        try {
          if (maxRowsValue instanceof Number)
            maxRows = ((Number) maxRowsValue).intValue();
          else if (maxRowsValue != null)
            maxRows = Integer.valueOf(String.valueOf(maxRowsValue));
        } catch (NumberFormatException e) {
          throw new JspException(e.getMessage());
        }
      }
        
      if (maxRows < -1)
        throw new JspException(L.l("sql:query maxRows '{0}' must not be less than -1.",
                                   maxRows));

      if (params == null) {
        stmt = conn.createStatement();
        rs = stmt.executeQuery(sql);
      }
      else {
        PreparedStatement pstmt = conn.prepareStatement(sql);
        stmt = pstmt;

        for (int i = 0; i < params.size(); i++) {
          Object paramValue = params.get(i);

          pstmt.setObject(i + 1, paramValue);
        }

        rs = pstmt.executeQuery();
      }

      int startRow = _startRow;

      while (startRow-- > 0 && rs.next()) {
      }

      Result result;
      result = new ResultImpl(rs, maxRows);

      rs.close();
      stmt.close();

      CoreSetTag.setValue(pageContext, _var, _scope, result);
    } catch (JspException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new JspException(L.l("sql:query '{0}' failed:\n{1}",
                                 sql, e.getMessage()),
                             e);
    } finally {
      // if (! isTransaction && conn != null) {
      if (conn != null) {
        try {
          conn.close();
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }

    return EVAL_PAGE;
  }

  private int countParameters(String sql)
  {
    if (sql == null)
      return 0;
    
    int len = sql.length();
    boolean inQuote = false;
    int count = 0;

    for (int i = 0; i < len; i++) {
      char ch = sql.charAt(i);

      if (ch == '\'') {
        inQuote = ! inQuote;
      }
      else if (ch == '\\') {
        i++;
      }
      else if (ch == '?') {
        count++;
      }
    }

    return count;
  }

  public static DataSource getDataSource(PageContext pageContext,
                                         Object ds)
    throws JspException
  {
    if (ds == null)
      ds = Config.find(pageContext, Config.SQL_DATA_SOURCE);

    if (ds instanceof DataSource)
      return (DataSource) ds;
    else if (! (ds instanceof String))
      throw new JspException(L.l("'{0}' is an invalid DataSource.", ds));

    String key = (String) ds;

    try {
      String jndiName;
      
      if (key.startsWith("java:comp/"))
        jndiName = key;
      else
        jndiName = "java:comp/env/" + key;

      Object value = new InitialContext().lookup(jndiName);

      if (value instanceof DataSource)
        return (DataSource) value;
    } catch (NamingException e) {
    }

    DataSource dataSource = getDataSource(key);

    if (dataSource != null)
      return dataSource;
    
    throw new JspException(L.l("'{0}' is an invalid DataSource.", ds));
  }

  public static Connection getConnection(PageContext pageContext,
                                         Object ds)
    throws JspException
  {
    try {
      if (ds == null)
        ds = Config.find(pageContext, Config.SQL_DATA_SOURCE);

      if (ds instanceof DataSource)
        return ((DataSource) ds).getConnection();
      else if (! (ds instanceof String))
        throw new JspException(L.l("'{0}' is an invalid DataSource.", ds));

      String key = (String) ds;

      try {
        String jndiName;
      
        if (key.startsWith("java:comp/"))
          jndiName = key;
        else
          jndiName = "java:comp/env/" + key;

        Object value = new InitialContext().lookup(jndiName);

        if (value instanceof DataSource)
          return ((DataSource) value).getConnection();
      } catch (NamingException e) {
      }

      return getDriverConnection(key);
    } catch (SQLException e) {
      throw new JspException(L.l("'{0}' is an invalid DataSource.\n{1}",
                                 ds, e.getMessage()),
                             e);
    }
  }

  private static DataSource getDataSource(String key)
  {
    String []split = key.split(",");
    String url = split[0];
    String user = split.length >= 3 ? split[2] : null;
    String password = split.length >= 4 ? split[3] : null;

    try {
      String className = null;

      if (split.length >= 2)
        className = split[1];

      if (className != null) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class cl = Class.forName(className, false, loader);
        Driver driver = (Driver) cl.newInstance();

        Properties info = new Properties();

        if (user != null)
          info.put("user", user);
        if (password != null)
          info.put("password", password);

        return new DriverDataSource(driver, url, info);
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return null;
  }

  private static Connection getDriverConnection(String key)
    throws SQLException
  {
    String []split = key.split(",");
    String url = split[0];
    String user = split.length >= 3 ? split[2] : null;
    String password = split.length >= 4 ? split[3] : null;

    try {
      String className = null;

      if (split.length >= 2)
        className = split[1];

      if (className != null) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class cl = Class.forName(className, false, loader);
        Driver driver = (Driver) cl.newInstance();

        Properties info = new Properties();

        if (user != null)
          info.put("user", user);
        if (password != null)
          info.put("password", password);

        return driver.connect(url, info);
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    if (user != null && password != null)
      return DriverManager.getConnection(url, user, password);
    else
      return DriverManager.getConnection(url);
  }

  static class DriverDataSource implements DataSource {
    private Driver _driver;
    private String _url;
    private Properties _info;

    DriverDataSource(Driver driver, String url, Properties info)
    {
      _driver = driver;
      _url = url;
      _info = info;
    }

    public Connection getConnection()
      throws SQLException
    {
      return _driver.connect(_url, _info);
    }

    public Connection getConnection(String user, String password)
      throws SQLException
    {
      return getConnection();
    }

    public PrintWriter getLogWriter()
    {
      return null;
    }

    public void setLogWriter(PrintWriter out)
    {
    }

    public int getLoginTimeout()
    {
      return 0;
    }

    public void setLoginTimeout(int timeout)
    {
    }

        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
  }
}
