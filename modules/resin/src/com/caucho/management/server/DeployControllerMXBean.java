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

/**
 * Management interface for the deploy controller.
 */
public interface DeployControllerMXBean extends ManagedObjectMXBean
{
  //
  // Configuration
  //
  
  /**
   * Returns the root directory.
   */
  @Description("The configured filesystem directory for the web-app")
  public String getRootDirectory();
  
  /**
   * Returns the startup mode, one of "default", "automatic", "lazy", or "manual".
   */
  @Description("The configured startup-mode, one of `default', `automatic', `lazy', or `manual'")
  public String getStartupMode();

  /**
   * Returns the redeploy mode, one of "default", "automatic", "lazy", or "manual".
   */
  @Description("The configured redeploy-mode, one of `default', `automatic', `lazy', or `manual'")
  public String getRedeployMode();

  /**
   * Returns the interval between redploy checks.
   */
  @Description("The configured millisecond interval between checks for the need to redeploy")
  @Units("milliseconds")
  public long getRedeployCheckInterval();

  //
  // Lifecycle
  //

  /**
   * Returns the controller's state.
   */
  @Description("The current lifecycle state")
  public String getState();

  /**
   * Returns any error message
   */
  @Description("Any startup error message")
  public String getErrorMessage();

  /**
   * Returns the time the controller was last started.
   */
  @Description("The current time of the last start")
  public Date getStartTime();

  //
  // Operations
  //

  /**
   * Starts the instance.
   */
  @Description("Starts instance of a controller")
  public void start()
    throws Exception;

  /**
   * Stops the instance.
   */
  @Description("Stops instance of a controller")
  public void stop()
    throws Exception;

  /**
   * Restarts the instance.
   */
  @Description("Restarts instance of a controller (Stop then Start)")
  public void restart()
    throws Exception;

  /**
   * Restarts the instance if any changes are detected.
   */
  @Description("Restarts instance if changes are detected")
  public void update()
    throws Exception;

  @Description("Destroys instance")
  public boolean destroy()
    throws Exception;
}
