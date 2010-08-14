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


package com.caucho.server.cluster;

import java.util.ArrayList;

import com.caucho.cloud.network.ClusterServer;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.ClusterMXBean;
import com.caucho.management.server.ClusterServerMXBean;
import com.caucho.management.server.HostMXBean;
import com.caucho.management.server.PersistentStoreMXBean;
import com.caucho.management.server.PortMXBean;
import com.caucho.management.server.ResinMXBean;

public class ClusterAdmin extends AbstractManagedObject
  implements ClusterMXBean
{
  private final Cluster _cluster;

  public ClusterAdmin(Cluster cluster)
  {
    _cluster = cluster;
  }

  public String getName()
  {
    return _cluster.getId();
  }
  
  public HostMXBean []getHosts()
  {
    return new HostMXBean[0];
  }
  
  public PortMXBean getPort()
  {
    /*
    ClusterServer clusterServer = _cluster.getSelfServer();

    if (clusterServer == null)
      return null;

     return clusterServer.getClusterPort().getAdmin();
    */
    return null;
  }

  public ResinMXBean getResin()
  {
    return _cluster.getResin().getAdmin();
  }

  public PersistentStoreMXBean getPersistentStore()
  {
    return null;
  }

  public ClusterServerMXBean []getServers()
  {
    ArrayList<ClusterServerMXBean> serverMBeansList
      = new ArrayList<ClusterServerMXBean>();

    for (ClusterPod pod : _cluster.getPodList()) {
      for (ClusterServer server : pod.getServerList()) {
        if (server != null)
          serverMBeansList.add(server.getAdmin());
      }
    }

    ClusterServerMXBean []serverMBeans
      = new ClusterServerMXBean[serverMBeansList.size()];
    serverMBeansList.toArray(serverMBeans);

    return serverMBeans;
  }

  /**
   * Adds a new dynamic server
   */
  public void addDynamicServer(String id, String address, int port)
  {
    _cluster.addDynamicServer(id, address, port);
    /*
    Server server = _cluster.getResin().getServer();
    
    server.addDynamicServer(_cluster.getId(), id, address, port);
    */
  }

  public boolean isDynamicServerEnable()
  {
    return _cluster.isDynamicServerEnable();
  }


  //
  // lifecycle
  //

  void register()
  {
    registerSelf();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getObjectName() + "]";
  }
}
