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

package com.caucho.server.cluster;

import com.caucho.cloud.network.ClusterServer;
import com.caucho.config.ConfigException;
import com.caucho.network.balance.ClientSocketFactory;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ClusterPod is a reliable triplicate for clustered data.
 * 
 * For small clusters, the triad may only have 1 or 2 servers, so
 * server B and server C may return null.
 */
abstract public class ClusterPod
{
  private static final L10N L = new L10N(ClusterPod.class);
  private static final Logger log
    = Logger.getLogger(ClusterPod.class.getName());

  public final static Owner []OWNER_VALUES = Owner.class.getEnumConstants();
  
  private final Cluster _cluster;

  private final int _index;

  private String _id;

  /**
   * Creates a new triad for the cluster.
   * 
   * @param cluster the owning cluster
   * @param index the triad index
   */
  protected ClusterPod(Cluster cluster,
                       int index)
  {
    _cluster = cluster;
    _index = index;

    if (index == 0)
      _id = "main";
    else
      _id = String.valueOf(index);
  }

  /**
   * Returns the triad id.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Returns the triad index.
   */
  public int getIndex()
  {
    return _index;
  }

  /**
   * Returns the triad's cluster
   */
  public Cluster getCluster()
  {
    return _cluster;
  }

  /**
   * Return the servers statically configured in the triad
   */
  abstract public ClusterServer []getServerList();

  /**
   * Return the servers statically configured in the triad
   */
  abstract public ArrayList<ClusterServer> getStaticServerList();
  
  /**
   * Returns the triad's first server
   * 
   * @return the first server.
   */
  abstract public ClusterServer getServerA();
  
  /**
   * Returns the triad's second server
   * 
   * @return the second server.
   */
  abstract public ClusterServer getServerB();
  
  /**
   * Returns the triad's third server
   * 
   * @return the third server.
   */
  abstract public ClusterServer getServerC();

  /**
   * Returns true for any of the triad servers.
   */
  public boolean isTriad(ClusterServer server)
  {
    return (getServerA() == server
            || getServerB() == server
            || getServerC() == server);
  }

  /**
   * Finds the matching server.
   */
  public ClusterServer findServer(int index)
  {
    for (ClusterServer server : getServerList()) {
      if (server != null && server.getIndex() == index)
        return server;
    }

    return null;
  }

  //
  // configuration
  //

  /**
   * Creates a cluster server
   */
  abstract public ClusterServer createServer();

  /**
   * Adds cluster server
   */
  public void addServer(ClusterServer server)
  {
  }

  /**
   * Adds a dynamic server
   */
  public void addDynamicServer(String serverId, String address, int port)
    throws ConfigException
  {
    throw new ConfigException(L.l("addDynamicServer requires Resin Professional"));
  }

  /**
   * Remove a dynamic server
   */
  public void removeDynamicServer(String serverId, String address, int port)
    throws ConfigException
  {
    throw new ConfigException(L.l("removeDynamicServer requires Resin Professional"));
  }

  /**
   * Sets the active dynamic server
   */
  public ClusterServer setActiveDynamicServer(String serverId,
                                              String address,
                                              int port,
                                              int index)
    throws ConfigException
  {
    throw new ConfigException(L.l("setDynamicServer requires Resin Professional"));
  }

  //
  // lifecycle
  //

  /**
   * Initializes the servers in the triad.
   */
  public void init()
  {
    for (ClusterServer server : getServerList()) {
      try {
        server.init();
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
  }

  public void start()
  {
  }

  /**
   * Update for dynamic server
   */
  public void update()
  {
  }
  
  /**
   * Closes the servers in the triad.
   */
  public void close()
  {
    for (ClusterServer server : getServerList()) {
      try {
        server.close();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  //
  // utilities
  //

  /**
   * Finds the first server with the given id
   */
  public ClusterServer findServer(String id)
  {
    for (ClusterServer server : getServerList()) {
      if (server == null)
        continue;

      if (id.equals(server.getId()))
        return server;
    }

    return null;
  }

  /**
   * Finds the first server with the given id
   */
  public ClusterServer findServerByPrefix(String id)
  {
    for (ClusterServer server : getServerList()) {
      if (server == null)
        continue;

      if (id.equals(server.getServerClusterId()))
        return server;
    }

    return null;
  }

  /**
   * Finds the first server with the given address and port
   */
  public ClusterServer findServer(String address, int port)
  {
    for (ClusterServer server : getServerList()) {
      if (server == null)
        continue;

      if (address.equals(server.getAddress())
          && port == server.getPort()) {
        return server;
      }
    }

    return null;
  }

  //
  // triad ownership
  //

  /**
   * Returns the OwnerServerTriad for the given owner.
   */
  abstract public OwnerServerTriad getOwnerServerTriad(Owner owner);
  
  /**
   * Returns the primary server given an ownership tag.
   */
  public ClusterServer getPrimary(Owner owner)
  {
    switch (owner) {
    case A_B:
    case A_C:
      return getServerA();
      
    case B_C:
    case B_A:
      if (getServerB() != null)
        return getServerB();
      else
        return getServerA();
      
    case C_A:
      if (getServerC() != null)
        return getServerC();
      else
        return getServerB();
      
    case C_B:
      if (getServerC() != null)
        return getServerC();
      else
        return getServerA();
      
    default:
        throw new IllegalStateException(L.l("'{0}' is an unknown owner", owner));
    }
  }
  
  /**
   * Returns the secondary server given an ownership tag.  If the
   * triad has only one server, return null.
   */
  public ClusterServer getSecondary(Owner owner)
  {
    if (getServerB() == null)
      return null;
     
    switch (owner) {
    case B_A:
    case C_A:
      return getServerA();
      
    case A_B:
      if (getServerB() != null)
        return getServerB();
      else
        return getServerC();
      
    case A_C:
      if (getServerC() != null)
        return getServerC();
      else
        return getServerB();
      
    case B_C:
      if (getServerC() != null)
        return getServerC();
      else
        return getServerA();
      
    case C_B:
      if (getServerB() != null)
        return getServerB();
      else
        return getServerA();
      
    default:
        throw new IllegalStateException(L.l("'{0}' is an unknown owner", owner));
     }
  }
  
  /**
   * Returns the tertiary server given an ownership tag.  If the server has
   * only 2 servers, return null.
   */
  public ClusterServer getTertiary(Owner owner)
  {
    if (getServerC() == null)
      return null;
    
    switch (owner) {
    case B_C:
    case C_B:
      return getServerA();
      
    case A_C:
    case C_A:
      return getServerB();
      
    case A_B:
    case B_A:
      return getServerC();
      
    default:
        throw new IllegalStateException(L.l("'{0}' is an unknown owner", owner));
    }
  }

  /**
   * Returns the best primary or secondary triad server.
   */
  public ClusterServer getActiveServer(Owner owner,
                                       ClusterServer oldServer)
  {
    ClusterServer server;
    ClientSocketFactory pool;

    server = getPrimary(owner);

    if (server != null && server.isActive()) {
      pool = server.getServerPool();
    
      if (pool != null && pool.isActive() && server != oldServer)
        return server;
    }

    server = getSecondary(owner);

    if (server != null && server.isActive()) {
      pool = server.getServerPool();
    
      if (pool != null && pool.isActive() && server != oldServer)
        return server;
    }

    server = getTertiary(owner);

    if (server != null && server.isActive()) {
      pool = server.getServerPool();
    
      if (pool != null && pool.isActive() && server != oldServer)
        return server;
    }

    // force the send

    server = getPrimary(owner);
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && server != oldServer)
      return server;

    server = getSecondary(owner);
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && server != oldServer)
      return server;

    server = getTertiary(owner);
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && server != oldServer)
      return server;

    return null;
  }

  /**
   * Returns the best primary or secondary triad server.
   */
  public ClusterServer getActiveOrSelfServer(Owner owner,
                                             ClusterServer oldServer)
  {
    ClusterServer server;
    ClientSocketFactory pool;

    server = getPrimary(owner);

    if (server != null && server.isActive()) {
      pool = server.getServerPool();

      if (pool == null)
        return server;
    
      if (pool.isActive() && server != oldServer)
        return server;
    }

    server = getSecondary(owner);

    if (server != null && server.isActive()) {
      pool = server.getServerPool();

      if (pool == null)
        return server;
    
      if (pool.isActive() && server != oldServer)
        return server;
    }

    server = getTertiary(owner);

    if (server != null && server.isActive()) {
      pool = server.getServerPool();

      if (pool == null)
        return server;
    
      if (pool.isActive() && server != oldServer)
        return server;
    }

    // force the send

    server = getPrimary(owner);
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && server != oldServer)
      return server;

    server = getSecondary(owner);
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && server != oldServer)
      return server;

    server = getTertiary(owner);
    pool = server != null ? server.getServerPool() : null;
    
    if (pool != null && server != oldServer)
      return server;

    return null;
  }

  /**
   * Returns the owner for an index
   */
  public static Owner getOwner(long index)
  {
    return OWNER_VALUES[(int) ((index & Long.MAX_VALUE) % OWNER_VALUES.length)];
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + getIndex()
            + "," + getId()
            + ",cluster=" + _cluster.getId() + "]");
  }
  
  /**
   * For any object, assigns an owner and backup in the triad. 
   */
  public enum Owner {
    A_B,
    B_C,
    C_A,
    
    A_C,
    B_A,
    C_B
  };
}
