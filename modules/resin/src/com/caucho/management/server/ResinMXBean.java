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
 * @author Sam
 */

package com.caucho.management.server;

import com.caucho.jmx.Description;

/**
 * Management interface for the server.
 * There is one ResinServer global for the entire JVM.
 *
 * <pre>
 * resin:type=Resin
 * </pre>
 */
@Description("A single Resin for each JVM provides a global environment for Resin")
public interface ResinMXBean extends ManagedObjectMXBean {
  //
  // Hierarchy Attributes
  //

  /**
   * Returns the Cluster mbean-names for all clusters managed by Resin.
   */
  @Description("The ClusterMBean names managed by Resin")
  public ClusterMXBean []getClusters();

  /**
   * Returns the server MBean's ObjectName for this instance.
   */
  @Description("The current Server instance")
  public ServerMXBean getServer();

  //
  // Configuration Attributes
  //

  /**
   * The Resin home directory used when starting this instance of Resin.
   * This is the location of the Resin program files.
   */
  @Description("The Resin home directory used when starting"
               + " this instance of Resin. This is the location"
               + " of the Resin program files")
  public String getResinHome();
  
  /**
   * The root directory used when starting this instance of Resin.
   * This is the root directory of the web server files.
   */
  @Description("The root directory used when starting"
               + " this instance of Resin. This is the root"
               + " directory of the web server files")
  public String getRootDirectory();

  /**
   * Returns the config file, the value of "-conf foo.conf"
   */
  @Description("The configuration file used when starting this"
               + " instance of Resin, the value of `-conf'")
  public String getConfigFile();
  
  /**
   * Returns the version.
   */
  @Description("The Resin Version")
  public String getVersion();

  /**
   * Returns true for the professional version.
   */
  @Description("True for Resin Professional")
  public boolean isProfessional();

  /**
   * Returns the ip address or host name  of the machine that is running this ResinServer.
   */
  @Description("The ip address or host name of the machine that is running"
               + " this instance of Resin")
  public String getLocalHost();

  /**
   * Returns the user name of the process that is running this ResinServer.
   */
  @Description("The user name of the process that is running"
               + " this instance of Resin")
  public String getUserName();
}
