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

package com.caucho.boot;

import java.io.*;

import com.caucho.config.ConfigException;

/**
 * Process responsible for watching a backend server.
 */
public interface WatchdogAPI {
  /**
   * Returns the status of the watchdog manager
   * 
   * @param password the watchdog password
   * 
   * @return a user-readable status report
   */
  public String status(String password);
  
  /**
   * Starts the server with the given arguments.  If the
   * start fails, a ConfigException is thrown.
   */
  public void start(String password, String []argv)
    throws ConfigException, IllegalStateException, IOException;

  /**
   * Restarts the server with the given arguments.
   * 
   * @param password watchdog password 
   * @param serverId the server to be restarted
   * @param argv the new arguments for the server
   */
  public void restart(String password, String serverId, String []argv)
    throws ConfigException, IllegalStateException, IOException;

  /**
   * Stops the named server
   * 
   * @param password the watchdog password
   * @param serverId the server to stop
   * @throws com.caucho.config.ConfigException
   */
  public void stop(String password, String serverId)
    throws ConfigException, IllegalStateException, IOException;
  
  /**
   * Kills the named server, terminating the process.
   * 
   * @param password the watchdog password
   * @param serverId the server to kill
   */
  public void kill(String password, String serverId)
    throws ConfigException, IllegalStateException, IOException;
 
  /**
   * Shuts the entire watchdog manager down.
   * 
   * @param password
   * @return true on success
   */
  public boolean shutdown(String password)
    throws IOException;
}
