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

package com.caucho.sql.spy;

import com.caucho.util.L10N;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Spying on a driver.
 */
public class SpyDriver implements java.sql.Driver {
  protected final static Logger log
    = Logger.getLogger(SpyDriver.class.getName());
  protected final static L10N L = new L10N(SpyDriver.class);

  private static int _staticId;

  private SpyDataSource _spyDataSource;
  
  private int _id;
  private int _connCount;

  // The underlying driver
  private Driver _driver;
  
  /**
   * Creates a new SpyDriver.
   */
  public SpyDriver(Driver driver)
  {
    _spyDataSource = new SpyDataSource();
    _driver = driver;
    _id = _staticId++;
  }
  
  public boolean acceptsURL(String url)
    throws SQLException
  {
    try {
      boolean result = _driver.acceptsURL(url);

      log.fine(_id + ":acceptsURL(" + url + ") -> " + result);
      
      return result;
    } catch (SQLException e) {
      log.fine(_id + ":exn-acceptURL(" + e + ")");
      
      throw e;
    }
  }
  
  public Connection connect(String url, Properties fine)
    throws SQLException
  {
    try {
      Connection conn = _driver.connect(url, fine);

      int connId = _connCount++;

      log.fine(_id + ":connect(" + url + ",fine=" + fine + ") -> " + connId + ":" + conn);

      return new SpyConnection(conn, _spyDataSource);
    } catch (SQLException e) {
      log.fine(_id + ":exn-connect(" + e + ")");
      
      throw e;
    }
  }
  
  public int getMajorVersion()
  {
      int result = _driver.getMajorVersion();

      log.fine(_id + ":getMajorVersion() -> " + result);

      return result;
  }
  
  public int getMinorVersion()
  {
      int result = _driver.getMinorVersion();

      log.fine(_id + ":getMinorVersion() -> " + result);

      return result;
  }
  
  public DriverPropertyInfo []getPropertyInfo(String url, Properties fine)
    throws SQLException
  {
    try {
      DriverPropertyInfo []result = _driver.getPropertyInfo(url, fine);

      Hashtable<String,String> cleanFine 
        = new Hashtable<String,String>();
      
      if (fine != null) {
        for (Map.Entry<Object,Object> entry : fine.entrySet()) {
          cleanFine.put((String) entry.getKey(), (String) entry.getValue());
        }
      }
        
      if (cleanFine.get("password") != null)
        cleanFine.put("password", "****");
      
      log.fine(_id + ":getPropertyInfo(" + url + ") -> " + result);

      return result;
    } catch (SQLException e) {
      log.fine(_id + ":exn-getPropertyInfo(" + e + ")");
      
      throw e;
    }
  }
  
  public boolean jdbcCompliant()
  {
    boolean result = _driver.jdbcCompliant();

    log.fine(_id + ":jdbcCompliant() -> " + result);

    return result;
  }

  public String toString()
  {
    return "SpyDriver[id=" + _id + ",driver=" + _driver + "]";
  }
}
