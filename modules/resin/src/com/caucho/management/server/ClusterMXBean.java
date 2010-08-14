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

import javax.management.ObjectName;

/**
 * A Cluster is a collection of cluster members,
 *
 * Each instance of Resin has 0 or 1 active srun ports and accept
 * inbound connections, available here with {@link #getPortObjectName}.
 *
 * Every instance of Resin can use
 * {@link com.caucho.server.cluster.ClusterClient}s to establish
 * outbound connections to other members of the cluster, available
 * here with {@link #getServers()}.
 *
 * A typical ObjectName for a ClusterMBean is
 * <pre>
 * resin:type=Cluster,name=app-tier
 * </pre>
 */
public interface ClusterMXBean extends ManagedObjectMXBean {
  //
  // Hierarchy attributes
  //

  /**
   * Returns the owning ResinMXBean
   */
  public ResinMXBean getResin();
  
  /**
   * Returns a list of {@link ObjectName}s for the
   * {@link com.caucho.server.cluster.ClusterClient}s that
   * are used to create outbound connections to communicate with
   * members of the cluster.
   */
  @Description("The ClusterServers that are used to create" +
               " outbound connections to communicate with" +
               " members of the cluster")
  public ClusterServerMXBean []getServers();

  /**
   * Returns a list of the ObjectNames for the virtual hosts.
   */
  @Description("Hosts are containers that are uniquely identified"
               + " by the hostname used in making an HTTP request")
  public HostMXBean []getHosts();

  /**
   * Returns the persistent-store ObjectName.
   */
  @Description("The PersistentStore saves persistent and distributed session" +
               " information")
  public PersistentStoreMXBean getPersistentStore();

  /**
   * Returns true if this cluster supports adding dynamic servers.
   */
  @Description("Determines if the cluster supports dynamic servers")
  public boolean isDynamicServerEnable();

  //
  // operations
  //

  /**
   * Adds a new dynamic server to the cluster
   */
  @Description("Adds a dynamic server")
  public void addDynamicServer(String id, String address, int port);

}
