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

/**
 * Management interface for the host.
 *
 * <pre>
 * resin:type=Host,name=foo.com
 * </pre>
 */
@Description("")
public interface HostMXBean extends DeployControllerMXBean
{
  /**
   * Returns the host name.
   */
  @Description("The configured canonical host name")
  public String getHostName();

  /**
   * Returns the URL
   */
  @Description("The configured canonical URL")
  public String getURL();

  //
  // Relation attributes
  //
  
  /**
   * Returns an array of the webapps
   */
  @Description("The configured webapps for the virtual host")
  public WebAppMXBean []getWebApps();

  //
  // Configuration
  //

  /**
   * Returns the root directory.
   */
  @Description("The configured root directory for the virtual host")
  public String getRootDirectory();

  /**
   * Returns the primary war directory.
   */
  public String getWarDirectory();

  /**
   * Returns the primary war expand directory.
   */
  public String getWarExpandDirectory();

  //
  // Operations
  //
  
  /**
   * Updates a web-app entry from the deployment directories.
   */
  public void updateWebAppDeploy(String name)
    throws Exception;

  /**
   * Updates an ear entry from the deployment directories.
   */
  public void updateEarDeploy(String name)
    throws Exception;

  /**
   * Expand an ear entry from the deployment directories.
   */
  public void expandEarDeploy(String name);

  /**
   * Start an ear entry from the deployment directories.
   */
  public void startEarDeploy(String name);
}
