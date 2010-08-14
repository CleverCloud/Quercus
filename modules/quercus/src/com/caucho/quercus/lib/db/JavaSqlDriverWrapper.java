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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.db;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/*
 * javax.sql.DataSource adapter for java.sql.Driver
 */
public class JavaSqlDriverWrapper implements javax.sql.DataSource
{
  private Driver _driver;
  private String _url;
  
  public JavaSqlDriverWrapper(Driver driver, String url)
  {
    _driver = driver;
    _url = url;
  }
  
  public Connection getConnection()
    throws SQLException
  {
    Properties props = new Properties();
    props.put("user", "");
    props.put("password", "");
    
    return _driver.connect(_url, props);
  }
  
  public Connection getConnection(String user, String password)
    throws SQLException
  {
    Properties props = new Properties();
    
    if (user != null)
      props.put("user", user);
    else
      props.put("user", "");
    
    if (password != null)
      props.put("password", password);
    else
      props.put("password", "");
    
    return _driver.connect(_url, props);
  }
  
  public int getLoginTimeout()
  {
    throw new UnsupportedOperationException();
  }
  
  public PrintWriter getLogWriter()
  {
    throw new UnsupportedOperationException();
  }
  
  public void setLoginTimeout(int seconds)
  {
    throw new UnsupportedOperationException();
  }
  
  public void setLogWriter(PrintWriter out)
  {
    throw new UnsupportedOperationException();
  }
  
  public <T> T unwrap(Class<T> iface)
    throws SQLException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}