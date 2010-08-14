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

package com.caucho.jstl.rt;

import com.caucho.jsp.PageContextImpl;
import com.caucho.util.L10N;

import javax.servlet.jsp.JspException;
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

public class SqlUpdateTag extends BodyTagSupport implements SQLExecutionTag {
  private static final Logger log
    = Logger.getLogger(SqlUpdateTag.class.getName());
  private static final L10N L = new L10N(SqlUpdateTag.class);
  
  private String _sql;
  private String _var;
  private String _scope;
  private Object _dataSource;

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
    PageContextImpl pageContext = (PageContextImpl) this.pageContext;
    
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
        conn = SqlQueryTag.getConnection(pageContext, _dataSource);
      }

      Object value = null;

      ResultSet rs;

      ArrayList params = _params;
      _params = null;
      
      int paramCount = countParameters(sql);

      if (params == null && paramCount != 0
          || params != null && paramCount != params.size()) {
        throw new JspException(L.l("sql:param does not match expected parameters\nin '{0}'",
                                   sql));
      }
      
      Statement stmt;
      int rows = 0;

      if (params == null) {
        stmt = conn.createStatement();
        rows = stmt.executeUpdate(sql);
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

        rows = pstmt.executeUpdate();
      }

      stmt.close();

      CoreSetTag.setValue(pageContext, _var, _scope, new Integer(rows));
    } catch (RuntimeException e) {
      throw e;
    } catch (JspException e) {
      throw e;
    } catch (Exception e) {
      throw new JspException(L.l("sql:update '{0}' failed:\n{1}",
                                 sql, e.getMessage()),
                             e);
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
}
