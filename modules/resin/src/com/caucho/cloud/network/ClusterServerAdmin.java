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

import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.ClusterMXBean;
import com.caucho.management.server.ClusterServerMXBean;
import com.caucho.network.balance.ClientSocketFactory;

import java.util.Date;

/**
 * Implementation of the ClusterServer's administration mbean.
 */
public class ClusterServerAdmin extends AbstractManagedObject
  implements ClusterServerMXBean
{
  private final ClusterServer _server;

  public ClusterServerAdmin(ClusterServer server)
  {
    _server = server;
  }

  /**
   * Returns the -server id.
   */
  public String getName()
  {
    return _server.getId();
  }

  @Override
  public String getType()
  {
    return "ClusterServer";
  }

  /**
   * Returns the owning cluster's object name.
   */
  @Override
  public ClusterMXBean getCluster()
  {
    return _server.getCluster().getData(ClusterMXBean.class);
  }

  /**
   * Returns the cluster index.
   */
  @Override
  public int getClusterIndex()
  {
    return _server.getIndex();
  }

  /**
   * Returns the server's IP address.
   */
  @Override
  public String getAddress()
  {
    return _server.getAddress();
  }

  /**
   * Returns the server's port.
   */
  @Override
  public int getPort()
  {
    return _server.getPort();
  }

  /**
   * Returns true for a dynamic server
   */
  @Override
  public boolean isDynamicServer()
  {
    return _server.isDynamic();
  }

  /**
   * Returns true for a triad server
   */
  @Override
  public boolean isTriadServer()
  {
    return _server.isTriad();
  }

  /**
   * Returns true for the self server
   */
  @Override
  public boolean isSelfServer()
  {
    return _server.isSelf();
  }

  //
  // Client connection/load-balancing parameters
  //

  /**
   * Returns the time the client will consider the connection dead
   * before retrying.
   */
  @Override
  public long getRecoverTime()
  {
    return _server.getLoadBalanceRecoverTime();
  }

  /**
   * Returns the maximum time a socket can remain idle in the pool.
   */
  @Override
  public long getIdleTime()
  {
    return _server.getLoadBalanceIdleTime();
  }

  /**
   * Returns the green load-balancing connection minimum
   */
  @Override
  public int getConnectionMin()
  {
    return _server.getLoadBalanceConnectionMin();
  }

  /**
   * Returns the connect timeout for a client.
   */
  @Override
  public long getConnectTimeout()
  {
    return _server.getLoadBalanceConnectTimeout();
  }

  /**
   * Returns the socket timeout for a client.
   */
  @Override
  public long getSocketTimeout()
  {
    return _server.getLoadBalanceSocketTimeout();
  }

  /**
   * Returns the warmup time in milliseconds.
   */
  @Override
  public long getWarmupTime()
  {
    return _server.getLoadBalanceWarmupTime();
  }

  /**
   * Returns the load-balance weight.
   */
  @Override
  public int getWeight()
  {
    return _server.getLoadBalanceWeight();
  }

  //
  // State
  //

  @Override
  public String getState()
  {
    ClientSocketFactory pool = _server.getServerPool();
    
    if (pool != null)
      return pool.getState();
    else
      return "self";
  }

  @Override
  public int getConnectionActiveCount()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      return pool.getActiveCount();
    else
      return 0;
  }

  @Override
  public int getConnectionIdleCount()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      return pool.getIdleCount();
    else
      return 0;
  }

  @Override
  public long getConnectionNewCountTotal()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      return pool.getConnectCountTotal();
    else
      return 0;
  }

  @Override
  public long getConnectionFailCountTotal()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      return pool.getFailCountTotal();
    else
      return 0;
  }

  @Override
  public Date getLastFailTime()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      return pool.getLastFailTime();
    else
      return null;
  }

  public Date getLastSuccessTime()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      return new Date(pool.getLastSuccessTime());
    else
      return null;
  }

  @Override
  public double getLatencyFactor()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      return pool.getLatencyFactor();
    else
      return 0;
  }

  @Override
  public long getConnectionBusyCountTotal()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      return pool.getBusyCountTotal();
    else
      return 0;
  }

  @Override
  public Date getLastBusyTime()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      return pool.getLastBusyTime();
    else
      return null;
  }

  @Override
  public long getConnectionKeepaliveCountTotal()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      return pool.getKeepaliveCountTotal();
    else
      return 0;
  }

  @Override
  public double getServerCpuLoadAvg()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      return pool.getCpuLoadAvg();
    else
      return 0;
  }

  @Override
  public void start()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      pool.start();
  }

  @Override
  public void stop()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      pool.stop();
  }

  @Override
  public void enableSessionOnly()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      pool.enableSessionOnly();
  }

  @Override
  public boolean ping()
  {
    ClientSocketFactory pool = _server.getServerPool();

    if (pool != null)
      return pool.canConnect();
    else
      return true;
  }

  /**
   * Remove the server as a dynamic server
   */
  @Override
  public void removeDynamicServer()
  {
    /*
    ClusterServer clusterServer = _server;

    clusterServer.getClusterPod().removeDynamicServer(clusterServer.getId(),
                                                      clusterServer.getAddress(),
                                                      clusterServer.getPort());
                                                      */
  }

  protected void register()
  {
    registerSelf();
  }

  public String toString()
  {
    return getClass().getSimpleName() +  "[" + getObjectName() + "]";
  }
}
