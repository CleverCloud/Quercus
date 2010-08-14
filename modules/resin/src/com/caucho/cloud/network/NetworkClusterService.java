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

package com.caucho.cloud.network;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.cloud.topology.AbstractCloudClusterListener;
import com.caucho.cloud.topology.AbstractCloudPodListener;
import com.caucho.cloud.topology.AbstractCloudServerListener;
import com.caucho.cloud.topology.CloudCluster;
import com.caucho.cloud.topology.CloudPod;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.cloud.topology.CloudSystem;
import com.caucho.cloud.topology.TopologyService;
import com.caucho.env.service.AbstractResinService;
import com.caucho.env.service.ResinSystem;
import com.caucho.network.balance.ClientSocketFactory;
import com.caucho.network.listen.SocketLinkListener;
import com.caucho.server.hmux.HmuxProtocol;
import com.caucho.util.L10N;

/**
 * NetworkClusterService manages the cluster network code, the communication
 * between Resin servers in a cluster. 
 */
public class NetworkClusterService extends AbstractResinService
{
  private static final L10N L = new L10N(NetworkClusterService.class);
  private static final Logger log
    = Logger.getLogger(NetworkClusterService.class.getName());
  
  public static final int START_PRIORITY = 2000;
  public static final int START_PRIORITY_CLUSTER_SERVICE = 2100;
  
  private final CloudServer _selfServer;
  
  private SocketLinkListener _clusterListener;
  
  private CopyOnWriteArrayList<ClusterServerListener> _serverListeners
  = new CopyOnWriteArrayList<ClusterServerListener>();

  private CopyOnWriteArrayList<ClusterLinkListener> _linkListeners
  = new CopyOnWriteArrayList<ClusterLinkListener>();

  /**
   * Creates a new network cluster service.
   */
  public NetworkClusterService(CloudServer selfServer)
  {
    _selfServer = selfServer;
    
    _selfServer.getSystem().addClusterListener(new NetworkClusterListener());
    
    if (_selfServer.getPort() >= 0) {
      _clusterListener = new ClusterListener(_selfServer.getAddress(), 
                                             _selfServer.getPort());
    }
  }
  
  /**
   * Returns the current network service.
   */
  public static NetworkClusterService getCurrent()
  {
    return ResinSystem.getCurrentService(NetworkClusterService.class);
  }
  
  /**
   * Returns the self server for the network.
   */
  public CloudServer getSelfServer()
  {
    return _selfServer;
  }
  
  /**
   * Returns the active server id.
   */
  public String getServerId()
  {
    return getSelfServer().getId();
  }
  
  /**
   * Returns the cluster port.
   */
  public SocketLinkListener getClusterListener()
  {
    return _clusterListener;
  }

  //
  // listeners
  //

  public void addServerListener(ClusterServerListener listener)
  {
    _serverListeners.add(listener);

    CloudServer []serverList = _selfServer.getPod().getServerList();
    int serverLength = _selfServer.getPod().getServerLength();
    
    for (int i = 0; i < serverLength; i++) {
      CloudServer cloudServer = serverList[i];

      if (cloudServer != null) {
        ClusterServer server = cloudServer.getData(ClusterServer.class);
      
        if (server.isActive())
          listener.serverStart(server);
      }
    }
  }

  public void removeServerListener(ServerListener listener)
  {
    _serverListeners.remove(listener);
  }

  protected void notifyServerStart(ClusterServer server)
  {
    for (ClusterServerListener listener : _serverListeners) {
      listener.serverStart(server);
    }
  }

  protected void notifyServerStop(ClusterServer server)
  {
    for (ClusterServerListener listener : _serverListeners) {
      listener.serverStop(server);
    }
  }

  public void addLinkListener(ClusterLinkListener listener)
  {
    _linkListeners.add(listener);
  }
  
  public void notifyLinkClose(Object payload)
  {
    for (ClusterLinkListener listener : _linkListeners) {
      listener.onLinkClose(payload);
    }
  }
  
  //
  // lifecycle
  //

  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }

  @Override
  public void start()
    throws Exception
  {
    super.start();
    
    startClusterListener();
    
    ClusterServer selfServer = _selfServer.getData(ClusterServer.class);
    
    selfServer.notifyStart();

    CloudSystem cloudSystem = TopologyService.getCurrent().getSystem();
    
    for (CloudCluster cluster : cloudSystem.getClusterList()) {
      for (CloudPod pod : cluster.getPodList()) {
        int serverLength = pod.getServerLength();

        for (int i = 0; i < serverLength; i++) {
          CloudServer cloudServer = pod.getServerList()[i];

          if (cloudServer == null)
            continue;

          ClusterServer server = cloudServer.getData(ClusterServer.class);

          if (server != null) {
            ClientSocketFactory pool = server.getServerPool();

            if (pool != null)
              pool.start();
          }
        }
      }
    }
  }

  /**
   * Closes the server.
   */
  @Override
  public void stop()
    throws Exception
  {
    super.stop();
    
    try {
      if (_clusterListener != null)
        _clusterListener.close();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Configures the server.
   */
  private void configureServer(CloudServer cloudServer)
  {
    CloudCluster cluster = cloudServer.getCluster();
    CloudPod pod = cloudServer.getPod();
    
    ClusterServerProgram clusterServerProgram
      = cluster.getData(ClusterServerProgram.class);
    
    ClusterServerProgram podServerProgram
      = pod.getData(ClusterServerProgram.class);
    
    ClusterServerProgram serverProgram
      = cloudServer.getData(ClusterServerProgram.class);
    
    ClusterServer server = new ClusterServer(this, cloudServer);
    
    if (clusterServerProgram != null)
      clusterServerProgram.getProgram().configure(server);
    
    if (podServerProgram != null)
      podServerProgram.getProgram().configure(server);
    
    if (serverProgram != null)
      serverProgram.getProgram().configure(server);
    
    if (cloudServer.putDataIfAbsent(server) != null) {
      throw new IllegalStateException(L.l("{0} cannot be configured twice.",
                                          server));
    }
    
    server.init();
  }
 
  /**
   * Start the cluster port
   */
  private void startClusterListener()
    throws Exception
  {
    SocketLinkListener listener = _clusterListener;
    
    if (listener != null) {
      listener.setProtocol(new HmuxProtocol());
      listener.init();
      
      log.info("");
      listener.bind();
      listener.start();
      log.info("");
    }
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _clusterListener + "]");
  }
  
  private class NetworkClusterListener extends AbstractCloudClusterListener {
    @Override
    public void onClusterAdd(CloudCluster cluster)
    {
      cluster.addPodListener(new PodListener());
    }
    
  }
  
  private class PodListener extends AbstractCloudPodListener {
    @Override
    public void onPodAdd(CloudPod pod)
    {
      pod.addServerListener(new ServerListener());
    }
    
  }
  
  private class ServerListener extends AbstractCloudServerListener {
    @Override
    public void onServerAdd(CloudServer server)
    {
      configureServer(server);
    }
  }
}
