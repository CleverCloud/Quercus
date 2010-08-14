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

import java.util.Date;

/**
 * Management interface for the watchdog thread watching a server.
 *
 * <pre>
 * resin:type=Watchdog,name=a
 * </pre>
 */
@Description("The Watchdog for a Server")
public interface WatchdogMXBean {
  //
  // ID attributes
  //
  
  /**
   * Returns the -server id.
   */
  @Description("The server id used when starting this instance"
               + " of Resin, the value of `--server'")
  public String getId();

  /**
   * Returns the resinHome
   */
  @Description("Returns the resin.home value")
  public String getResinHome();

  /**
   * Returns the resinRoot
   */
  @Description("Returns the resin.root value")
  public String getResinRoot();

  /**
   * Returns the resinConf
   */
  @Description("Returns the resin.conf value")
  public String getResinConf();

  /**
   * Returns the userName
   */
  @Description("Returns the user-name value")
  public String getUserName();

  //
  // state
  //

  /**
   * The current lifecycle state.
   */
  @Description("The current lifecycle state")
  public String getState();

  /**
   * Returns the last start time.
   */
  @Description("The time that the watchdog was started")
  public Date getInitialStartTime();

  /**
   * Returns the last start time.
   */
  @Description("The time that this instance was last started or restarted")
  public Date getStartTime();

  //
  // statistics
  //

  /**
   * Returns the number of times the server has been restarted.
   */
  @Description("The number of times the server has been restarted")
  public int getStartCount();

  //
  // operations
  //

  /**
   * Start the Resin process
   */
  @Description("Starts the Resin process")
  public void start();

  /**
   * Stop the Resin process
   */
  @Description("Stops the Resin process")
  public void stop();

  /**
   * Kills the Resin process
   */
  @Description("Stops the Resin process")
  public void kill();
}
