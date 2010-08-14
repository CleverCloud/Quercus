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
 * @author Sam
 */


package com.caucho.tools.profiler;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;

public class DriverWrapper
  implements Driver
{
  private final ProfilerPoint _profilerPoint;
  private final Driver _driver;

  public DriverWrapper(ProfilerPoint profilerPoint, Driver driver)
  {
    _profilerPoint = profilerPoint;
    _driver = driver;
  }

  private Connection wrap(Connection connection)
  {
    return new ConnectionWrapper(_profilerPoint, connection);
  }

  public Connection connect(String url, Properties info)
    throws SQLException
  {
    return wrap(_driver.connect(url, info));
  }

  public boolean acceptsURL(String url)
    throws SQLException
  {
    return _driver.acceptsURL(url);
  }

  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
    throws SQLException
  {
    return _driver.getPropertyInfo(url, info);
  }

  public int getMajorVersion()
  {
    return _driver.getMajorVersion();
  }

  public int getMinorVersion()
  {
    return _driver.getMinorVersion();
  }

  public boolean jdbcCompliant()
  {
    return _driver.jdbcCompliant();
  }

  public String toString()
  {
    return "DriverWrapper[" + _profilerPoint.getName() + "]";
  }
}
