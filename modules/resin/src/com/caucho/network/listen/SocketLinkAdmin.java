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


package com.caucho.network.listen;

import com.caucho.inject.Module;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.PortMXBean;
import com.caucho.management.server.TcpConnectionInfo;

@Module
public class SocketLinkAdmin extends AbstractManagedObject
  implements PortMXBean
{
  private SocketLinkListener _port;

  public SocketLinkAdmin(SocketLinkListener port)
  {
    _port = port;
  }

  public String getName()
  {
    String addr = _port.getAddress();

    if (addr == null)
      addr = "INADDR_ANY";
    
    return addr + '-' + _port.getPort();
  }

  public String getProtocolName()
  {
    return _port.getProtocolName();
  }

  public String getAddress()
  {
    return _port.getAddress();
  }

  public int getPort()
  {
    return _port.getPort();
  }

  public boolean isSSL()
  {
    return _port.isSSL();
  }

  //
  // Config
  //

  public int getAcceptThreadMin()
  {
    return _port.getAcceptThreadMin();
  }

  public int getAcceptThreadMax()
  {
    return _port.getAcceptThreadMax();
  }

  public int getAcceptListenBacklog()
  {
    return _port.getAcceptListenBacklog();
  }

  public int getConnectionMax()
  {
    return _port.getConnectionMax();
  }

  public int getKeepaliveMax()
  {
    return _port.getKeepaliveMax();
  }

  public int getKeepaliveSelectMax()
  {
    return _port.getKeepaliveSelectMax();
  }

  public long getKeepaliveConnectionTimeMax()
  {
    return _port.getKeepaliveConnectionTimeMax();
  }

  public long getKeepaliveTimeout()
  {
    return _port.getKeepaliveTimeout();
  }

  public long getSocketTimeout()
  {
    return _port.getSocketTimeout();
  }

  public long getSuspendTimeMax()
  {
    return _port.getSuspendTimeMax();
  }

  public String getState()
  {
    return _port.getLifecycleState().getStateName();
  }

  public int getThreadCount()
  {
    return _port.getThreadCount();
  }

  public int getThreadActiveCount()
  {
    return _port.getActiveThreadCount();
  }

  public int getThreadIdleCount()
  {
    return _port.getIdleThreadCount();
  }

  public int getThreadStartCount()
  {
    return _port.getStartThreadCount();
  }

  public int getKeepaliveCount()
  {
    return _port.getKeepaliveConnectionCount();
  }

  public int getKeepaliveThreadCount()
  {
    return _port.getKeepaliveThreadCount();
  }

  public int getKeepaliveSelectCount()
  {
    return _port.getSelectConnectionCount();
  }

  public int getCometIdleCount()
  {
    return _port.getCometIdleCount();
  }

  public long getRequestCountTotal()
  {
    return _port.getLifetimeRequestCount();
  }

  public long getKeepaliveCountTotal()
  {
    return _port.getLifetimeKeepaliveCount();
  }

  public long getClientDisconnectCountTotal()
  {
    return _port.getLifetimeClientDisconnectCount();
  }

  public long getRequestTimeTotal()
  {
    return _port.getLifetimeRequestTime();
  }

  public long getReadBytesTotal()
  {
    return _port.getLifetimeReadBytes();
  }

  public long getWriteBytesTotal()
  {
    return _port.getLifetimeWriteBytes();
  }

  //
  // Operations
  //
  
  /**
   * Enable the port, letting it listening to new requests.
   */
  public void start()
  {
    _port.enable();
  }
  
  /**
   * Disable the port, stopping it from listening to new requests.
   */
  public void stop()
  {
    _port.disable();
  }

  /**
   * returns information for all the port's connections
   */
  public TcpConnectionInfo []connectionInfo()
  {
    return _port.connectionInfo();
  }

  void register()
  {
    registerSelf();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getObjectName() + "]";
  }
}
