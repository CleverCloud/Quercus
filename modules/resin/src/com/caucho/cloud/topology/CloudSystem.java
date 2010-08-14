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

package com.caucho.cloud.topology;

import java.util.concurrent.CopyOnWriteArrayList;

import com.caucho.util.L10N;

/**
 * CloudDomain represents all the clusters in a Resin configuration.
 */
public class CloudSystem
{
  private static final L10N L = new L10N(CloudSystem.class);
  
  private CloudCluster []_clusterArray = new CloudCluster[0];

  private final String _id;
  
  private final CopyOnWriteArrayList<CloudClusterListener> _listeners
    = new CopyOnWriteArrayList<CloudClusterListener>();

  /**
   * Creates a CloudDomain with an identifying id.
   */
  public CloudSystem(String id)
  {
    _id = id;
  }

  /**
   * Returns the domain id.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Return the clusters in the cloud.
   */
  public CloudCluster []getClusterList()
  {
    return _clusterArray;
  }

  /**
   * Finds the server with the unique id.
   */
  public CloudCluster findCluster(String id)
  {
    for (CloudCluster cluster : getClusterList()) {
      if (id.equals(cluster.getId()))
        return cluster;
    }

    return null;
  }

  /**
   * Finds the server with the unique id.
   */
  public CloudServer findServer(String id)
  {
    for (CloudCluster cluster : getClusterList()) {
      CloudServer server = cluster.findServer(id);
      
      if (server != null)
        return server;
    }

    return null;
  }

  /**
   * Finds the first server with the given address and port
   */
  public CloudServer findServer(String address, int port)
  {
    for (CloudCluster cluster : getClusterList()) {
      CloudServer server = cluster.findServer(address, port);
      
      if (server != null)
        return server;
    }

    return null;
  }
  
  /**
   * Creates a new cluster.
   */
  public CloudCluster createCluster(String id)
  {
    CloudCluster cluster;
    
    synchronized (this) {
      cluster = findCluster(id);
      
      if (cluster != null) {
        throw new IllegalStateException(L.l("'{0}' is a duplicate cluster id",
                                            id));
      }
      
      
      cluster = new CloudCluster(this, id);
      
      CloudCluster []clusterArray = new CloudCluster[_clusterArray.length + 1];
      System.arraycopy(_clusterArray, 0, clusterArray, 0, _clusterArray.length);
      
      clusterArray[_clusterArray.length] = cluster;
      _clusterArray = clusterArray;
    }
    
    for (CloudClusterListener listener : _listeners) {
      listener.onClusterAdd(cluster);
    }
    
    return cluster;
  }
  
  /**
   * Creates a new cluster.
   */
  public boolean removeCluster(String id)
  {
    CloudCluster cluster = null;
    
    synchronized (this) {
      int index;
      for (index = _clusterArray.length - 1; index >= 0; index--) {
        if (_clusterArray[index].getId().equals(id)) {
          cluster = _clusterArray[index];
          break;
        }
      }
      
      if (cluster == null)
        return false;
      
      if (cluster.getPodList().length != 0) {
        throw new IllegalStateException(L.l("{0} may not be removed because it has a non-empty pod list",
                                            cluster));
        
      }
      
      CloudCluster []clusterArray = new CloudCluster[_clusterArray.length - 1];
      System.arraycopy(_clusterArray, 0, clusterArray, 0, index);
      
      if (index < clusterArray.length) {
        System.arraycopy(_clusterArray, index + 1, clusterArray, index, 
                         _clusterArray.length - index - 1);
      }
      
      _clusterArray = clusterArray;
    }
    
    for (CloudClusterListener listener : _listeners) {
      listener.onClusterRemove(cluster);
    }
    
    return true;
  }

  //
  // listeners
  //
  
  /**
   * Adds a listener to detect server add and removed.
   */
  public void addClusterListener(CloudClusterListener listener)
  {
    if (listener == null)
      throw new NullPointerException();
    
    if (! _listeners.contains(listener))
      _listeners.add(listener);
    
    for (CloudCluster cluster : getClusterList()) {
      listener.onClusterAdd(cluster);
    }
  }
  
  /**
   * Removes a listener to detect server add and removed.
   */
  public void removeClusterListener(CloudClusterListener listener)
  {
    _listeners.remove(listener);
  }

  /**
   * Adds a listener
   */
  

  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[" + getId() + "]");
  }
}
