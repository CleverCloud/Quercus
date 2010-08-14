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

package com.caucho.jstl.el;

import com.caucho.el.Expr;
import com.caucho.util.L10N;

import javax.el.ELContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.tagext.TagSupport;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public class SqlSetDataSourceTag extends TagSupport {
  private static final Logger log
    = Logger.getLogger(SqlSetDataSourceTag.class.getName());
  private static final L10N L = new L10N(SqlSetDataSourceTag.class);
  
  private Expr _dataSource;
  
  private Expr _url;
  private Expr _driver;
  private Expr _user;
  private Expr _password;
  
  private String _var;
  private String _scope;

  /**
   * Sets the JSP-EL expression for the dataSource.
   */
  public void setDataSource(Expr dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Sets the JSP-EL expression for the URL.
   */
  public void setUrl(Expr url)
  {
    _url = url;
  }

  /**
   * Sets the JSP-EL expression for the driver.
   */
  public void setDriver(Expr driver)
  {
    _driver = driver;
  }

  /**
   * Sets the JSP-EL expression for the user.
   */
  public void setUser(Expr user)
  {
    _user = user;
  }

  /**
   * Sets the JSP-EL expression for the password.
   */
  public void setPassword(Expr password)
  {
    _password = password;
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

  public int doStartTag() throws JspException
  {
    try {
      String var = _var;

      if (var == null)
        var = Config.SQL_DATA_SOURCE;

      ELContext env = pageContext.getELContext();

      DataSource dataSource = null;

      if (_dataSource != null) {
        Object ds = _dataSource.evalObject(env);
      
        dataSource = SqlQueryTag.getDataSource(pageContext, ds);
      }
      else {
        dataSource = openDataSource(_driver.evalString(env),
                                    _url.evalString(env),
                                    _user != null ? _user.evalString(env) : null,
                                    _password != null ? _password.evalString(env) : null);
      }

      CoreSetTag.setValue(pageContext, var, _scope, dataSource);
    
      return SKIP_BODY;
    } catch (Exception e) {
      throw new JspException(e);
    }
  }

  public static DataSource openDataSource(String driverClass, String url,
                                          String user, String password)
    throws Exception
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    Class cl = Class.forName(driverClass, false, loader);
    Driver driver = (Driver) cl.newInstance();
    
    return new DataSourceAdapter(driver, url, user, password);
  }

  public static class DataSourceAdapter implements DataSource {
    private Driver _driver;
    private String _url;
    private String _user;
    private String _password;

    public DataSourceAdapter(Driver driver, String url,
                             String user, String password)
    {
      _driver = driver;
      _url = url;
      _user = user;
      _password = password;
    }

    public Connection getConnection(String user, String password)
      throws SQLException
    {
      Properties props = new Properties();
      props.put("user", user);
      props.put("password", user);
      
      return _driver.connect(_url, props);
    }

    public Connection getConnection()
      throws SQLException
    {
      Properties props = new Properties();
      if (_user != null)
        props.put("user", _user);
      if (_password != null)
        props.put("password", _password);
      
      return _driver.connect(_url, props);
    }

    public void setLogWriter(PrintWriter out)
    {
    }

    public PrintWriter getLogWriter()
    {
      return null;
    }

    public void setLoginTimeout(int timeout)
    {
    }

    public int getLoginTimeout()
    {
      return -1;
    }

        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
  }
}
