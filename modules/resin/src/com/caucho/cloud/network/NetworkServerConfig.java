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

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.network.listen.AbstractProtocol;
import com.caucho.network.listen.SocketLinkListener;
import com.caucho.server.cluster.ProtocolPort;
import com.caucho.server.cluster.ProtocolPortConfig;
import com.caucho.server.http.HttpProtocol;

public class NetworkServerConfig {
  private NetworkListenService _listenService;
  
  private ContainerProgram _listenerDefaults = new ContainerProgram();

  /**
   * Creates a new servlet server.
   */
  NetworkServerConfig(NetworkListenService listenService)
  {
    _listenService = listenService;
  }
  
  private NetworkListenService getListenService()
  {
    return _listenService;
  }

  @Configurable
  public SocketLinkListener createClusterPort()
  {
   return getListenService().getClusterListener();
  }
  
  @Configurable
  public SocketLinkListener createHttp()
    throws ConfigException
  {
    SocketLinkListener listener = new SocketLinkListener();
    
    applyPortDefaults(listener);

    HttpProtocol protocol = new HttpProtocol();
    listener.setProtocol(protocol);

    getListenService().addListener(listener);

    return listener;
  }

  @Configurable
  public SocketLinkListener createProtocol()
  {
    ProtocolPortConfig port = new ProtocolPortConfig();

    getListenService().addListener(port);

    return port;
  }

  @Configurable
  public SocketLinkListener createListen()
  {
    ProtocolPortConfig listener= new ProtocolPortConfig();

    getListenService().addListener(listener);

    return listener;
  }

  @Configurable
  public void add(ProtocolPort protocolPort)
  {
    SocketLinkListener listener = new SocketLinkListener();

    AbstractProtocol protocol = protocolPort.getProtocol();
    listener.setProtocol(protocol);

    applyPortDefaults(listener);

    protocolPort.getConfigProgram().configure(listener);

    getListenService().addListener(listener);
  }

  /**
   * Adds a port-default
   */
  @Configurable
  public void addPortDefault(ConfigProgram program)
  {
    addListenDefault(program);
  }

  /**
   * Adds a listen-default
   */
  @Configurable
  public void addListenDefault(ConfigProgram program)
  {
    _listenerDefaults.addProgram(program);
  }

  /**
   * If true, ports are bound at end.
   */
  @Configurable
  public void setBindPortsAfterStart(boolean bindAtEnd)
  {
    getListenService().setBindPortsAfterStart(bindAtEnd);
  }
  
  @Configurable
  public void addContentProgram(ConfigProgram builder)
  {
    _listenerDefaults.addProgram(builder);
  }

  private void applyPortDefaults(SocketLinkListener port)
  {
    _listenerDefaults.configure(port);
  }
}
