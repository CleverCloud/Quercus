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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.L10N;

/**
 * Defines a set of clustered servers.
 */
public class CloudCluster
{
  private static final L10N L = new L10N(CloudCluster.class);
  private static final Logger log = Logger.getLogger(CloudCluster.class.getName());
  
  private static final int POD_INDEX_MAX = 64 * 64;

  private final CloudSystem _domain;
  private final String _id;

  private CloudPod []_podList = new CloudPod[0];
  
  private final CopyOnWriteArrayList<CloudPodListener> _listeners
    = new CopyOnWriteArrayList<CloudPodListener>();
  
  private final ConcurrentHashMap<Class<?>,Object> _dataMap
    = new ConcurrentHashMap<Class<?>,Object>();

  CloudCluster(CloudSystem domain, String id)
  {
    _domain = domain;
    _id = id;
  }

  /**
   * Gets the cluster id.
   */
  public String getId()
  {
    return _id;
  }
  
  /**
   * Returns the owning domain.
   */
  public CloudSystem getSystem()
  {
    return _domain;
  }

  /**
   * Returns the list of pods for the cluster
   */
  public CloudPod []getPodList()
  {
    return _podList;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public CloudServer findServer(String id)
  {
    for (CloudPod pod : _podList) {
      CloudServer server = pod.findServer(id);

      if (server != null)
        return server;
    }

    return null;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public CloudServer findServerByUniqueClusterId(String id)
  {
    for (CloudPod pod : _podList) {
      CloudServer server = pod.findServerByUniqueClusterId(id);

      if (server != null)
        return server;
    }

    return null;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public CloudServer findServer(int podIndex,
                                int index)
  {
    CloudPod pod = findPod(podIndex);
      
    if (pod != null)
      return pod.findServer(index);
    else
      return null;
  }

  /**
   * Returns the pod with the given index.
   */
  public CloudPod findPod(int podIndex)
  {
    CloudPod []podList = _podList;
    
    if (podIndex < podList.length)
      return podList[podIndex];
    else
      return null;
  }
  
  /**
   * Creates a new pod
   */
  public CloudPod createPod()
  {
    return createPod(-1);
  }
  
  /**
   * Creates a new pod
   */
  public CloudPod createPod(int index)
  {
    CloudPod pod = null;
    
    synchronized (this) {
      if (POD_INDEX_MAX <= index)
        throw new IllegalArgumentException(L.l("'{0}' is an invalid pod index because it's greater than the max value.",
                                               index));

      if (index > 0 && index < _podList.length && _podList[index] != null) {
        throw new IllegalStateException(L.l("'{0}' is an invalid pod index because that pod already exists {1}",
                                            index, _podList[index]));
      }
      
      for (int i = 0; i < _podList.length; i++) {
        CloudPod oldPod = _podList[i];
        
        if (oldPod == null && index < 0) {
          index = i;
        }
      }
      
      if (index < 0)
        index = _podList.length;
      
      pod = new CloudPod(this, null, index);
      
      if (_podList.length <= index) {
        CloudPod []podList = new CloudPod[index + 1];
        System.arraycopy(_podList, 0, podList, 0, _podList.length);
        _podList = podList;
      }
      
      _podList[index] = pod;
    }
    
    for (CloudPodListener listener : _listeners) {
      listener.onPodAdd(pod);
    }
    
    if (log.isLoggable(Level.FINEST))
      log.finest(this + " added " + pod);
    
    return pod;
  }
  
  /**
   * Creates a new cluster.
   */
  public boolean removePod(int index)
  {
    if (index < 0 || _podList.length <= index)
      return false;
    
    CloudPod pod = null;
    
    synchronized (this) {
      pod = _podList[index];
      
      if (pod == null)
        return false;
      
      for (CloudServer server : pod.getServerList()) {
        if (server != null) {
          throw new IllegalStateException(L.l("{0} may not be removed because it has a non-empty server list",
                                              pod));
        }
      }
      
      // XXX: possibly always make a copy
      _podList[index] = null;
      
      int tail = _podList.length - 1;
      for (; tail >= 0 && _podList[tail] == null; tail--) {
      }

      if (tail + 1 < _podList.length) {
        CloudPod []podArray = new CloudPod[tail + 1];
        
        System.arraycopy(_podList, 0, podArray, 0, tail + 1);
      
        _podList = podArray;
      }
    }
    
    for (CloudPodListener listener : _listeners) {
      listener.onPodRemove(pod);
    }
    
    return true;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public CloudServer findServer(String address, int port)
  {
    for (CloudPod pod : _podList) {
      CloudServer server = pod.findServer(address, port);

      if (server != null)
        return server;
    }

    return null;
  }
  
  //
  // data
  //
  
  public void putData(Object value)
  {
    _dataMap.put(value.getClass(), value);
  }
  
  @SuppressWarnings("unchecked")
  public <T> T putDataIfAbsent(T value)
  {
    return (T) _dataMap.putIfAbsent(value.getClass(), value);
  }
  
  @SuppressWarnings("unchecked")
  public <T> T getData(Class<T> cl)
  {
    return (T) _dataMap.get(cl);
  }

  //
  // listeners
  //
  
  /**
   * Adds a listener to detect pod add and remove.
   */
  public void addPodListener(CloudPodListener listener)
  {
    if (! _listeners.contains(listener))
      _listeners.add(listener);
    
    for (CloudPod pod : getPodList()) {
      listener.onPodAdd(pod);
    }
  }
  
  /**
   * Removes a listener to detect pod add and remove.
   */
  public void removePodListener(CloudPodListener listener)
  {
    _listeners.remove(listener);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}
