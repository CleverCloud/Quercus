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

package com.caucho.management.server;

import com.caucho.jmx.Description;
import com.caucho.jmx.Units;

import java.util.Date;
import java.util.Properties;

/**
 * MBean API for a JDBC driver.
 *
 * <pre>
 * resin:type=JdbcDriver,name=jdbc/resin,...
 * </pre>
 */
@Description("A JDBC database driver")
public interface JdbcDriverMXBean extends ManagedObjectMXBean {
  //
  // Configuration
  //
  
  /**
   * Returns the driver class
   */
  @Description("The driver class")
  public String getClassName();
  
  /**
   * Returns the URL
   */
  @Description("The driver URL")
  public String getUrl();
  
  /**
   * Returns the drivers configured properties
   */
  @Description("The driver properties")
  public Properties getProperties();

  //
  // state
  //
  @Description("Returns the current state")
  public String getState();
  
  //
  // Statistics
  //
  
  /**
   * Returns the total number of connections.
   */
  @Description("The total number of connections")
  public long getConnectionCountTotal();
  
  /**
   * Returns the total number of failed connections.
   */
  @Description("The total number of failed connections")
  public long getConnectionFailCountTotal();
  
  /**
   * Returns the last connection fail time.
   */
  @Description("The last connection fail time")
  public Date getLastFailTime();

  //
  // Operations
  //

  /**
   * Enables the driver.
   */
  @Description("enable the driver")
  public boolean start();

  /**
   * Disables the driver.
   */
  @Description("disble the driver")
  public boolean stop();
}
