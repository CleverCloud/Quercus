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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.ConfigException;
import com.caucho.env.service.AbstractResinService;
import com.caucho.env.service.ResinSystem;
import com.caucho.network.listen.SocketLinkListener;
import com.caucho.network.listen.TcpSocketLink;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.vfs.QServerSocket;

public class NetworkListenService
  extends AbstractResinService 
  implements AlarmListener
{
  private static final L10N L = new L10N(NetworkListenService.class);
  private static final Logger log
    = Logger.getLogger(NetworkListenService.class.getName());

  public static final int START_PRIORITY_AT_BEGIN = 1000;
  public static final int START_PRIORITY_AT_END = 100000;
  
  private static final long ALARM_TIMEOUT = 120 * 1000L;
  
  private final CloudServer _cloudServer;
  
  private SocketLinkListener _clusterListener;
  
  private final ArrayList<SocketLinkListener> _listeners
    = new ArrayList<SocketLinkListener>();

  private boolean _isBindPortsAtEnd = true;
  
  private Alarm _alarm;
  
  /**
   * Creates a new servlet server.
   */
  public NetworkListenService(CloudServer cloudServer)
  {
    _cloudServer = cloudServer;
    
    NetworkClusterService clusterService = NetworkClusterService.getCurrent();
    
    if (clusterService != null)
      _clusterListener = clusterService.getClusterListener();
    
    if (_clusterListener != null)
      _listeners.add(_clusterListener);

    NetworkServerConfig config = new NetworkServerConfig(this);
   
    configure(_cloudServer, config);
  }
  
  public static NetworkListenService getCurrent()
  {
    return ResinSystem.getCurrentService(NetworkListenService.class);
  }
  
  /**
   * Returns the cluster listener, if in a clustered environment.
   */
  public SocketLinkListener getClusterListener()
  {
   return _clusterListener;
  }

  public void addListener(SocketLinkListener listener)
  {
    try {
      if (! _listeners.contains(listener))
        _listeners.add(listener);
    
      /*
      if (_lifecycle.isAfterStarting()) {
        // server/1e00
        port.bind();
        port.start();
      }
      */
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * If true, ports are bound at end.
   */
  public void setBindPortsAfterStart(boolean bindAtEnd)
  {
    _isBindPortsAtEnd = bindAtEnd;
  }

  /**
   * If true, ports are bound at end.
   */
  public boolean isBindPortsAfterStart()
  {
    return _isBindPortsAtEnd;
  }

  /**
   * Returns the {@link SocketLinkListener}s for this server.
   */
  public Collection<SocketLinkListener> getListeners()
  {
    return Collections.unmodifiableList(_listeners);
  }

  public void bind(String address, int port, QServerSocket ss)
    throws Exception
  {
    if ("null".equals(address))
      address = null;

    for (int i = 0; i < _listeners.size(); i++) {
      SocketLinkListener serverPort = _listeners.get(i);

      if (port != serverPort.getPort())
        continue;

      if ((address == null) != (serverPort.getAddress() == null))
        continue;
      else if (address == null || address.equals(serverPort.getAddress())) {
        serverPort.bind(ss);

        return;
      }
    }

    throw new IllegalStateException(L.l("No matching port for {0}:{1}",
                                        address, port));
  }

  /**
   * Finds the TcpConnection given the threadId
   */
  public TcpSocketLink findConnectionByThreadId(long threadId)
  {
    for (SocketLinkListener listener : getListeners()) {
      TcpSocketLink conn = listener.findConnectionByThreadId(threadId);

      if (conn != null)
        return conn;
    }

    return null;
  }
  
  //
  // lifecycle
  //

  @Override
  public int getStartPriority()
  {
    if (_isBindPortsAtEnd)
      return START_PRIORITY_AT_END;
    else
      return START_PRIORITY_AT_BEGIN;
  }
  /**
   * Bind the ports.
   */
  @Override
  public void start()
    throws Exception
  {
    boolean isFirst = true;

    for (SocketLinkListener listener : _listeners) {
      if (listener == _clusterListener)
        continue;

      if (isFirst)
        log.info("");

      isFirst = false;

      listener.bind();
      
      listener.start();
    }

    if (! isFirst)
      log.info("");
    
    _alarm = new Alarm(this);
    _alarm.queue(ALARM_TIMEOUT);
  }
  
  private void configure(CloudServer server, Object config)
  {
    ClusterServerProgram program;
    
    program = server.getCluster().getData(ClusterServerProgram.class);

    if (program != null)
      program.getProgram().configure(config);
    
    program = server.getPod().getData(ClusterServerProgram.class);

    if (program != null)
      program.getProgram().configure(config);
    
    program = server.getData(ClusterServerProgram.class);

    if (program != null)
      program.getProgram().configure(config);
  }

  /**
   * Handles the alarm.
   */
  @Override
  public void handleAlarm(Alarm alarm)
  {
    try {
      for (SocketLinkListener listener : _listeners) {
        if (listener.isClosed()) {
          log.severe("Resin restarting due to closed listener: " + listener);
          // destroy();
          //_controller.restart();
        }
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      // destroy();
      //_controller.restart();
    } finally {
      alarm = _alarm;
      
      if (alarm != null)
        alarm.queue(ALARM_TIMEOUT);
    }
  }

  /**
   * Closes the server.
   */
  @Override
  public void stop()
  {
    Alarm alarm = _alarm;
    _alarm = null;

    if (alarm != null)
      alarm.dequeue();

    for (SocketLinkListener listener : _listeners) {
      try {
        if (listener != _clusterListener) {
          listener.close();
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
}
