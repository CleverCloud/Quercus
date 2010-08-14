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

package com.caucho.jstl.el;

import com.caucho.el.Expr;
import com.caucho.jstl.ResultImpl;
import com.caucho.util.L10N;

import javax.el.ELContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.jstl.sql.Result;
import javax.servlet.jsp.jstl.sql.SQLExecutionTag;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqlQueryTag extends BodyTagSupport implements SQLExecutionTag {
  private static final Logger log
    = Logger.getLogger(SqlQueryTag.class.getName());
  private static final L10N L = new L10N(SqlQueryTag.class);
  
  private Expr _sql;
  private String _var;
  private String _scope;
  private Expr _dataSource;
  private Expr _maxRows;
  private Expr _startRow;

  private ArrayList<Object> _params;

  /**
   * Sets the JSP-EL expression for the SQL.
   */
  public void setSql(Expr sql)
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
  public void setDataSource(Expr dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Sets the maximum number of rows.
   */
  public void setMaxRows(Expr maxRows)
  {
    _maxRows = maxRows;
  }

  /**
   * Sets the start row.
   */
  public void setStartRow(Expr startRow)
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
    ELContext env = pageContext.getELContext();
    
    try {
      String sql;

      if (_sql != null)
        sql = _sql.evalString(env);
      else
        sql = bodyContent.getString();

      conn = (Connection) pageContext.getAttribute("caucho.jstl.sql.conn");
      if (conn != null)
        isTransaction = true;

      if (! isTransaction) {
        DataSource ds;

        if (_dataSource != null)
          ds = getDataSource(pageContext, _dataSource.evalObject(env));
        else
          ds = getDataSource(pageContext, null);

        conn = ds.getConnection();
      }

      Object value = null;

      ResultSet rs;

      ArrayList params = _params;
      _params = null;
      Statement stmt;

      if (params == null) {
        stmt = conn.createStatement();
        rs = stmt.executeQuery(sql);
      }
      else {
        PreparedStatement pstmt = conn.prepareStatement(sql);
        stmt = pstmt;

        for (int i = 0; i < params.size(); i++) {
          Object paramValue = params.get(i);

          if (paramValue == null)
            pstmt.setNull(i + 1, Types.VARCHAR);
          else
            pstmt.setObject(i + 1, paramValue);
        }

        rs = pstmt.executeQuery();
      }

      if (_startRow != null) {
        int startRow = (int) _startRow.evalLong(env);

        while (startRow-- > 0 && rs.next()) {
        }
      }

      Result result;
      if (_maxRows != null)
        result = new ResultImpl(rs, (int) _maxRows.evalLong(env));
      else
        result = new ResultImpl(rs, -1);

      rs.close();
      stmt.close();

      CoreSetTag.setValue(pageContext, _var, _scope, result);
    } catch (Exception e) {
      throw new JspException(e);
    } finally {
      if (! isTransaction && conn != null) {
        try {
          conn.close();
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }

    return EVAL_PAGE;
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
      throw new JspException(L.l("`{0}' is an invalid DataSource.", ds));

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
    
    throw new JspException(L.l("`{0}' is an invalid DataSource.", ds));
  }
}
