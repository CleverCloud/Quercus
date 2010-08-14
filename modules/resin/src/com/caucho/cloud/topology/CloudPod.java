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

import com.caucho.util.L10N;

/**
 * The CloudPod controls up to 64 CloudServers.
 * 
 * The first three servers are the triad servers.
 */
public class CloudPod
{
  private static final L10N L = new L10N(CloudPod.class);
  private final String _id;
  
  private final CloudCluster _cluster;

  private final int _index;
  
  private final CloudServer []_servers = new CloudServer[64];
  
  private final CopyOnWriteArrayList<CloudServerListener> _listeners
    = new CopyOnWriteArrayList<CloudServerListener>();
  
  private final ConcurrentHashMap<Class<?>,Object> _dataMap
    = new ConcurrentHashMap<Class<?>,Object>();
  
  private TriadDispatcher<CloudServer> _serverDispatcher
    = new TriadDispatcher<CloudServer>();
  
  private int _maxIndex = -1;

  /**
   * Creates a new triad for the cluster.
   * 
   * @param cluster the owning cluster
   * @param index the triad index
   */
  public CloudPod(CloudCluster cluster,
                  String id,
                  int index)
  {
    _cluster = cluster;
    _index = index;
    
    if (index < 0 || index >= 64 * 64)
      throw new IllegalArgumentException(L.l("'{0}' is an invalid pod index because it's not between 0 and 64.",
                                             64 * 64));
    
    if (cluster == null)
      throw new NullPointerException();

    if (id != null)
      _id = id;
    else if (index == 0)
      _id = "main";
    else
      _id = String.valueOf(index);
  }

  /**
   * Returns the pod id.
   */
  public final String getId()
  {
    return _id;
  }

  /**
   * Returns the pod index.
   */
  public final int getIndex()
  {
    return _index;
  }

  /**
   * Returns the pod's cluster
   */
  public final CloudCluster getCluster()
  {
    return _cluster;
  }

  /**
   * Returns the pod's system
   */
  public final CloudSystem getSystem()
  {
    return getCluster().getSystem();
  }

  public CloudServer []getServerList()
  {
    return _servers;
  }
  
  public int getServerLength()
  {
    return _maxIndex + 1;
  }
  
  /**
   * Finds the first server with the given server-id.
   */
  public CloudServer findServer(String id)
  {
    for (int i = 0; i <= _maxIndex; i++) {
      CloudServer server = _servers[i];
      
      if (server != null && server.getId().equals(id))
        return server;
    }

    return null;
  }
  
  /**
   * Finds the first server with the given cluster id, the
   * three-digit base-64 identifier.
   */
  public CloudServer findServerByUniqueClusterId(String id)
  {
    for (int i = 0; i <= _maxIndex; i++) {
      CloudServer server = _servers[i];
      
      if (server != null && server.getIdWithinCluster().equals(id))
        return server;
    }

    return null;
  }

  /**
   * Returns the pod with the given index.
   */
  public CloudServer findServer(int index)
  {
    return _servers[index];
  }

  /**
   * Finds the first server with the given address and port.
   */
  public CloudServer findServer(String address, int port)
  {
    for (int i = 0; i <= _maxIndex; i++) {
      CloudServer server = _servers[i];
      
      if (server != null 
          && server.getAddress().equals(address)
          && server.getPort() > 0
          && server.getPort() == port) {
        return server;
      }
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
   * Adds a listener to detect server add and removed.
   */
  public void addServerListener(CloudServerListener listener)
  {
    if (listener == null)
      throw new NullPointerException();
    
    if (! _listeners.contains(listener))
      _listeners.add(listener);
    
    for (CloudServer server : getServerList()) {
      if (server != null)
        listener.onServerAdd(server);
    }
  }
  
  /**
   * Removes a listener to detect server add and removed.
   */
  public void removeServerListener(CloudServerListener listener)
  {
    _listeners.remove(listener);
  }
  
  //
  // Add/remove servers
  //
  
  /**
   * Creates a new static server
   */
  public CloudServer createStaticServer(String id,
                                        String address,
                                        int port,
                                        boolean isSecure)
  {
    return createServer(id, address, port, isSecure, true);
  }
  
  /**
   * Creates a new dynamic server
   */
  public CloudServer createDynamicServer(String id,
                                         String address,
                                         int port,
                                         boolean isSecure)
  {
    return createServer(id, address, port, isSecure, false);
  }
  
  /**
   * Creates a new server
   */
  private CloudServer createServer(String id,
                                   String address,
                                   int port,
                                   boolean isSecure,
                                   boolean isStatic)
  {
    int index;
    CloudServer server;
    boolean isSSL = false;
    
    synchronized (this) {
      if (findServer(id) != null)
        throw new IllegalArgumentException(L.l("'{0}' is an invalid server name because that name already exists as\n  {1}.",
                                               id, 
                                               findServer(id)));
      
      if (findServer(address, port) != null)
        throw new IllegalArgumentException(L.l("'{0}:{1}' is an invalid server address because that name already exists as\n  {2}.",
                                               address, port,
                                               findServer(address, port)));
      
      index = findFirstFreeIndex();
      
      if (index <= 2)
        server = new TriadServer(id, this, index, address, port, isSSL, isStatic);
      else
        server = new CloudServer(id, this, index, address, port, isSSL, isStatic);
      
      _servers[index] = server;
      
      if (_maxIndex < index)
        _maxIndex = index;
    }

    updateDispatcher();
    
    for (CloudServerListener listener : _listeners) {
      listener.onServerAdd(server);
      
      if (server instanceof TriadServer)
        listener.onTriadAdd((TriadServer) server);
      
    }
    
    return server;
  }
  
  //
  // dispatcher
  //
  
  /**
   * Returns the triad server dispatcher
   */
  public TriadDispatcher<CloudServer> getTriadServerDispatcher()
  {
    return _serverDispatcher;
  }
  
  /**
   * Updates the triad server dispatcher.
   */
  private void updateDispatcher()
  {
    
    switch (_maxIndex) {
    case 0:
      _serverDispatcher
        = new TriadDispatcherSingle<CloudServer>(_servers[0]);
      break;

    case 1:
      _serverDispatcher 
        = new TriadDispatcherDouble<CloudServer>(_servers[0],
                                                 _servers[1]);
      break;

    default:
      _serverDispatcher 
        = new TriadDispatcherTriple<CloudServer>(_servers[0],
                                                 _servers[1],
                                                 _servers[2]);
      break;
    }
  }
  
  private int findFirstFreeIndex()
  {
    for (int i = 0; i <= _maxIndex; i++) {
      if (_servers[i] == null)
        return i;
    }
    
    return _maxIndex + 1;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + getIndex()
            + "," + getId()
            + ",cluster=" + _cluster.getId() + "]");
  }
}
