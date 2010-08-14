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

import java.util.Date;
import java.util.Map;

import com.caucho.jmx.Description;
import com.caucho.jmx.Units;

/**
 * MBean API for the WebApp.
 *
 * <pre>
 * resin:type=WebAppMBean,name=/wiki,Host=foo.com
 * </pre>
 */
@Description("The web-app management interface")
public interface WebAppMXBean extends DeployControllerMXBean {
  //
  // Hierarchy attributes
  //

  /**
   * Returns the owning host
   */
  @Description("The web-app's host")
  public HostMXBean getHost();

  /**
   * Returns the session manager
   */
  @Description("The web-app's session manager")
  public SessionManagerMXBean getSessionManager();

  //
  // Configuration attributes
  //

  /**
   * Returns the application's context path.
   */
  @Description("The configured context path that identifies the web-app in a url")
  public String getContextPath();

  /**
   * Returns the deployed version
   */
  @Description("The deployed version of the web-app")
  public String getVersion();

  /**
   * Returns any manifest entries from the .war file
   */
  @Description("The manifest attributes from the .war file")
  public Map<String,String> getManifestAttributes();

  //
  // Status attributes
  //
  
  /**
   * Returns the number of 500 status requests
   */
  @Description("The total number of 500 status errors")
  public long getStatus500CountTotal();
  
  /**
   * Returns the time of the last 500 status requests
   */
  @Description("The time of the last 500 status error")
  public Date getStatus500LastTime();

  //
  // Statistics attributes
  //
  
  /**
   * Returns the current number of requests being serviced by the web-app.
   */
  @Description("The current number of requests served by the web-app")
  public int getRequestCount();

  /**
   * Returns the total number of requests serviced by the web-app
   * since it started.
   */
  @Description("The total number of requests served by the web-app since starting")
  public long getRequestCountTotal();

  /**
   * Returns the total duration in milliseconds that connections serviced by
   * this web-app have taken.
   */
  @Description("The total real (wall-clock) time in milliseconds taken by requests served by the web-app")
  @Units("milliseconds")
  public long getRequestTimeTotal();

  /**
   * Returns the total number of bytes that requests serviced by
   * this web-app have read.
   */
  @Description("The total number of bytes received in client requests")
  @Units("bytes")
  public long getRequestReadBytesTotal();

  /**
   * Returns the total number of bytes that connections serviced by this
   * web-app have written.
   */
  @Description("The total number of bytes sent to clients")
  @Units("bytes")
  public long getRequestWriteBytesTotal();

  /**
   * Returns the number of connections that have ended with a
   * {@link com.caucho.vfs.ClientDisconnectException} for this web-app in it's lifetime.
   */
  @Description("The total number of times a client has disconnected before a request completed")
  public long getClientDisconnectCountTotal();
}
