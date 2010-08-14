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

import com.caucho.util.L10N;

/**
 * Defines a cloud server, a single Resin instance.
 * 
 * Each server has the following:
 * <ul>
 * <li>A unique id across the entire domain.
 * <li>A containing pod (contained within the server).
 * <li>A unique index within the pod. Servers 0-2 are special servers called
 * "triad" servers.
 * <li>An IP address and port.
 * </ul>
 *
 * Servers are organized into pods of up to 64 servers, contained in a cluster.
 * 
 * All the clusters are contained in a domain.
 */
public class CloudServer {
  private static final L10N L = new L10N(CloudServer.class);
  private static final int DECODE[];

  private final String _id;

  private final CloudPod _pod;
  private final int _index;
  
  // unique identifier for the server within the cluster
  private String _uniqueClusterId;
  // unique identifier for the server within all Resin clusters
  private String _uniqueDomainId;
  
  private String _address;
  private int _port;
  private boolean _isSSL;
  
  private final ConcurrentHashMap<Class<?>,Object> _dataMap
    = new ConcurrentHashMap<Class<?>,Object>();

  public CloudServer(String id,
                     CloudPod pod, 
                     int index,
                     String address,
                     int port,
                     boolean isSSL,
                     boolean isStatic)
  {
    _id = id;
    
    _pod = pod;
    _index = index;
    
    if (index < 0 || index >= 64)
      throw new IllegalArgumentException(L.l("'{0}' is an invalid server index because it must be between 0 and 64",
                                            index));
    
    if (! isStatic && index == 0)
      throw new IllegalArgumentException(L.l("The first server must be static."));
    
    if (id == null)
      throw new NullPointerException();
    if (pod == null)
      throw new NullPointerException();
    
    _address = address;
    _port = port;
    _isSSL = isSSL;

    // _clusterPort = new ClusterPort(this);
    // _ports.add(_clusterPort);

    StringBuilder sb = new StringBuilder();

    sb.append(convert(getIndex()));
    sb.append(convert(getPod().getIndex()));
    sb.append(convert(getPod().getIndex() / 64));

    _uniqueClusterId = sb.toString();

    String clusterId = pod.getCluster().getId();
    if (clusterId.equals(""))
      clusterId = "default";

    _uniqueDomainId = _uniqueClusterId + "." + clusterId.replace('.', '_');
  }

  /**
   * Gets the unique server identifier.
   */
  public final String getId()
  {
    return _id;
  }

  public final String getDebugId()
  {
    if ("".equals(_id))
      return "default";
    else
      return _id;
  }

  /**
   * Returns the index within the pod.
   */
  public final int getIndex()
  {
    return _index;
  }

  /**
   * Returns the server's id within the cluster
   */
  public final String getIdWithinCluster()
  {
    return _uniqueClusterId;
  }

  /**
   * Returns the server's id within all Resin clusters
   */
  public final String getIdWithinDomain()
  {
    return _uniqueDomainId;
  }

  /**
   * True if this server is a triad.
   */
  public boolean isTriad()
  {
    return false;
  }

  /**
   * True for statically configured servers.
   */
  public boolean isStatic()
  {
    return true;
  }
  
  //
  // topology attributes
  //

  /**
   * Returns the pod
   */
  public CloudPod getPod()
  {
    return _pod;
  }

  /**
   * Returns the cluster.
   */
  public CloudCluster getCluster()
  {
    return getPod().getCluster();
  }

  /**
   * Returns the system.
   */
  public CloudSystem getSystem()
  {
    return getCluster().getSystem();
  }

  /**
   * Returns the pod owner
   */
  public TriadOwner getTriadOwner()
  {
    return TriadOwner.getOwner(getIndex());
  }
  
  //
  // IP addresses
  //
  
  /**
   * Gets the address
   */
  public final String getAddress()
  {
    return _address;
  }
  
  /**
   * Gets the port
   */
  public final int getPort()
  {
    return _port;
  }
  
  public boolean isSSL()
  {
    return _isSSL;
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
  
  @SuppressWarnings("unchecked")
  public <T> T removeData(Class<T> cl)
  {
    return (T) _dataMap.remove(cl);
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() 
            + "[" + _id + "," + _index
            + "," + _address + ":" + _port
            + "]");
  }

  private static char convert(long code)
  {
    code = code & 0x3f;

    if (code < 26)
      return (char) ('a' + code);
    else if (code < 52)
      return (char) ('A' + code - 26);
    else if (code < 62)
      return (char) ('0' + code - 52);
    else if (code == 62)
      return '_';
    else
      return '-';
  }

  public static int decode(int code)
  {
    return DECODE[code & 0x7f];
  }

  static {
    DECODE = new int[128];
    for (int i = 0; i < 64; i++)
      DECODE[(int) convert(i)] = i;
  }
}
