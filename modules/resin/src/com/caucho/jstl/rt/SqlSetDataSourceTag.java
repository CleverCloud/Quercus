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
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.tagext.TagSupport;
import javax.sql.DataSource;
import java.util.logging.Logger;

public class SqlSetDataSourceTag extends TagSupport {
  private static final Logger log
    = Logger.getLogger(SqlSetDataSourceTag.class.getName());
  private static final L10N L = new L10N(SqlSetDataSourceTag.class);
  
  private Object _dataSource;
  
  private String _url;
  private String _driver;
  private String _user;
  private String _password;
  
  private String _var;
  private String _scope;

  /**
   * Sets the object for the dataSource.
   */
  public void setDataSource(Object dataSource)
  {
    _dataSource = dataSource;
  }

  /**
   * Sets the URL.
   */
  public void setUrl(String url)
  {
    _url = url;
  }

  /**
   * Sets the driver.
   */
  public void setDriver(String driver)
  {
    _driver = driver;
  }

  /**
   * Sets the user.
   */
  public void setUser(String user)
  {
    _user = user;
  }

  /**
   * Sets the password.
   */
  public void setPassword(String password)
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
    String var = _var;

    if (var == null)
      var = Config.SQL_DATA_SOURCE;

    DataSource dataSource = null;

    if (_dataSource != null)
      dataSource = SqlQueryTag.getDataSource(pageContext, _dataSource);
    else
      dataSource = openDataSource(_driver, _url, _user, _password);

    CoreSetTag.setValue(pageContext, var, _scope, dataSource);
    
    return SKIP_BODY;
  }

  private DataSource openDataSource(String driver, String url,
                                    String user, String password)
    throws JspException
  {
    try {
      return com.caucho.jstl.el.SqlSetDataSourceTag.openDataSource(driver, url, user, password);
    } catch (Exception e) {
      throw new JspException(e);
    }
  }
}
