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
 * A client-view of a cluster's server.  The load balancer and
 * persistent store will use the ClusterServer to communicate to
 * other servers in the cluster.
 *
 * The JMX name looks like:
 * <pre>
 *   resin:type=ClusterServer,name=web-a
 * </pre>
 */
@Description("Client-view of a cluster's server, i.e. a target server with which this instance can communicate")
public interface ClusterServerMXBean extends ManagedObjectMXBean {
  /**
   * The containing cluster.
   */
  @Description("The configured Cluster which contains the server")
  public ClusterMXBean getCluster();

  /**
   * The cluster index of the server.
   */
  @Description("The configured index of this server in the cluster, used for distributed objects.")
  public int getClusterIndex();

  /**
   * Returns the ip address or host name of the server.
   */
  @Description("The configured IP address or host name of the server")
  public String getAddress();

  /**
   * Returns the resin/admin port number of the server.
   */
  @Description("The configured port number of the target server")
  public int getPort();

  /**
   * Returns true if this is a dynamic server
   */
  @Description("Returns true for a dynamic server")
  public boolean isDynamicServer();

  /**
   * Returns true if this is a triad server
   */
  @Description("Returns true for a triad server")
  public boolean isTriadServer();

  /**
   * Returns true for the server's own ClusterServer
   */
  @Description("Returns true for the server's own ClusterServer")
  public boolean isSelfServer();

  /**
   * The timeout in milliseconds for connecting to the server.
   */
  @Description("The configured timeout for a client connect to the server")
  @Units("milliseconds")
  public long getConnectTimeout();

  /**
   * The minimum number of connections for green load balancing.
   */
  @Description("The minimum connections for green load balancing")
  public int getConnectionMin();

  /**
   * Returns the timeout for assuming a target server remains unavailable once
   * a connection attempt fails. When the timeout period elapses another attempt
   * is made to connect to the target server
   */
  @Description("The configured timeout for assuming a target server remains" +
              " unavailable once a connection attempt fails." +
              " When the timeout period elapses another" +
              " attempt is made to connect to the target server")
  @Units("milliseconds")
  public long getRecoverTime();

  /**
   * Returns the timeout for an idle socket that is connected to the target
   * server. If the socket is not used within the timeout period the idle
   * connection is closed.
   */
  @Description("The configured timeout for an idle socket that is connected" +
               " to the target server. If the socket is not" +
               " used within the timeout period the idle" +
               " connection is closed")
  @Units("milliseconds")
  public long getIdleTime();

  /**
   * Returns the timeout to use for reads when communicating with
   * the target server.
   */
  @Description("The configured timeout for a client read from the server")
  @Units("milliseconds")
  public long getSocketTimeout();

  /**
   * Returns the warmup time in milliseconds.
   */
  @Description("The configured warmup time in milliseconds for ramping up connections to the server")
  @Units("milliseconds")
  public long getWarmupTime();

  /**
   * Returns the load-balancer weight, defaulting to 100.
   *
   */
  @Description("The configured load balance weight.  Weights over 100 will get more traffic and weights less than 100 will get less traffic")
  public int getWeight();

  //
  // State attributes
  //

  /**
   * Returns the lifecycle state.
   */
  @Description("The current lifecycle state of the client")
  public String getState();

  //
  // Statistics attributes
  //

  /**
   * Returns the number of connections actively being used to communicate with
   * the target server.
   */
  @Description("The current number of connections actively being used" +
               " to communicate with the target server")
  public int getConnectionActiveCount();

  /**
   * Returns the number of open but currently unused connections to the
   * target server.
   */
  @Description("The current number of idle connections in the connection pool")
  public int getConnectionIdleCount();

  /**
   * Returns the number of connections that have been made to the target server.
   */
  @Description("The total number of new connections that have been made" +
               " to the target server")
  public long getConnectionNewCountTotal();

  /**
   * Returns the number of connections that have been made to the target server.
   */
  @Description("The total number of keepalive connections that have been made" +
               " to the target server")
  public long getConnectionKeepaliveCountTotal();

  /**
   * Returns the number of connections which could not connect
   * to the target server.
   */
  @Description("The total number of failed connections attempts" +
               " to the target server")
  public long getConnectionFailCountTotal();

  /**
   * Returns the time of the last failure.
   */
  @Description("The current last time a connection attempt failed")
  public Date getLastFailTime();

  /**
   * Returns the number of connections which resulted in a busy
   * response.
   */
  @Description("The total number of busy responses" +
               " from the target server")
  public long getConnectionBusyCountTotal();

  /**
   * Returns the last time of the busy response.
   */
  @Description("The current last time the target server refused a request because it was busy")
  public Date getLastBusyTime();

  /**
   * Returns the server's load average.
   */
  @Description("The load average of the backend server")
  public double getServerCpuLoadAvg();

  /**
   * Returns the server's latency factory
   */
  @Description("The latency factor of the backend server")
  public double getLatencyFactor();

  //
  // operations
  //

  /**
   * Enables connections to the target server.
   */
  @Description("Enables connections to the target server")
  public void start();

  /**
   * Enables connections to the target server.
   */
  @Description("Enable only sticky-session requests to the target server")
  public void enableSessionOnly();

  /**
   * Disables connections to the target server.
   */
  @Description("Disables connections to the target server")
  public void stop();

  /**
   * Remove this server as a dynamic server
   */
  @Description("Remove this server as a dynamic server")
  public void removeDynamicServer();

  /**
   * Returns true if a connection can be made to the target server.
   */
  @Description("Tries to connect to the target server, returning true if successful")
  public boolean ping();
}
