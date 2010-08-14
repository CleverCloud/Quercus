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

import com.caucho.util.L10N;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqlTransactionTag extends TagSupport implements TryCatchFinally  {
  private static final Logger log
    = Logger.getLogger(SqlTransactionTag.class.getName());
  private static final L10N L = new L10N(SqlTransactionTag.class);
  
  private Object _dataSource;
  private String _isolation;

  private Connection _conn;
  private int _oldIsolation;

  /**
   * Sets the data source
   */
  public void setDataSource(Object dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Sets the JSP-EL expression for the isolation.
   */
  public void setIsolation(String isolation)
  {
    _isolation = isolation;
  }

  public int doStartTag() throws JspException
  {
    if (pageContext.getAttribute("caucho.jstl.sql.conn") != null)
      throw new JspTagException(L.l("nested sql:transaction are forbidden"));

    try {
      DataSource ds;

      ds = SqlQueryTag.getDataSource(pageContext, _dataSource);

      int isolationCode = -1;
      if (_isolation == null) {
      }
      else if (_isolation.equals("read_committed"))
        isolationCode = Connection.TRANSACTION_READ_COMMITTED;
      else if (_isolation.equals("read_uncommitted"))
        isolationCode = Connection.TRANSACTION_READ_UNCOMMITTED;
      else if (_isolation.equals("repeatable_read"))
        isolationCode = Connection.TRANSACTION_REPEATABLE_READ;
      else if (_isolation.equals("serializable"))
        isolationCode = Connection.TRANSACTION_SERIALIZABLE;
      else
        throw new JspTagException(L.l("unknown sql:transaction isolation ~{0}'", _isolation));

      _conn = ds.getConnection();

      _oldIsolation = _conn.getTransactionIsolation();

      _conn.setAutoCommit(false);
      
      if (isolationCode < 0 || isolationCode == _oldIsolation) {
        _oldIsolation = -1;
      }
      else if (isolationCode == Connection.TRANSACTION_READ_COMMITTED) {
        _oldIsolation = -1;
      }
      else {
        _conn.setTransactionIsolation(isolationCode);
      }

      pageContext.setAttribute("caucho.jstl.sql.conn", _conn);
    } catch (JspException e) {
      throw e;
    } catch (Exception e) {
      throw new JspException(e);
    }

    return EVAL_PAGE;
  }

  public void doCatch(Throwable t) throws Throwable
  {
    Connection conn = _conn;
    _conn = null;
    
    if (conn != null) {
      try {
        conn.rollback();
      } finally {
        close(conn);
      }
    }

    throw t;
  }

  public void doFinally()
  {
    try {
      pageContext.removeAttribute("caucho.jstl.sql.conn");

      Connection conn = _conn;
      _conn = null;
      
      if (conn != null) {
        try {
          conn.commit();
        } finally {
          close(conn);
        }
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  private void close(Connection conn)
  {
    try {
      if (_oldIsolation >= 0)
        conn.setTransactionIsolation(_oldIsolation);
    } catch (SQLException e) {
    }

    try {
      conn.setAutoCommit(true);
    } catch (SQLException e) {
    }
          
    try {
      conn.close();
    } catch (SQLException e) {
    }
  }
}
