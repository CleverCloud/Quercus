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

package com.caucho.network.listen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.config.program.ContainerProgram;
import com.caucho.env.service.AbstractResinService;
import com.caucho.env.service.ResinSystem;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.L10N;

/**
 * The socket listen service, which accepts sockets and dispatches them to
 * protocols. 
 */
public class ListenService extends AbstractResinService
{
  private static final L10N L = new L10N(ListenService.class);
  private static final Logger log
    = Logger.getLogger(ListenService.class.getName());
  
  public static final int START_PRIORITY_LISTEN = 2000;
  public static final int START_PRIORITY_CLUSTER = 2100;
  
  private final ResinSystem _server;
  
  private final ArrayList<SocketLinkListener> _listeners
    = new ArrayList<SocketLinkListener>();
  
  private final ContainerProgram _listenDefaults
    = new ContainerProgram();

  private boolean _isBindListenersAtEnd = true;
  
  private final Lifecycle _lifecycle = new Lifecycle();
  private AtomicBoolean _isStartedListeners = new AtomicBoolean();
  
  public ListenService(ResinSystem server)
  {
    _server = server;
  }

  /**
   * Creates a listener with the defaults applied.
   * The listener will not be registered until addListener is called.
   */
  public SocketLinkListener createListener()
  {
    SocketLinkListener listener = new SocketLinkListener();
  
    applyListenerDefaults(listener);
    
    return listener;
  }

  /**
   * Registers a listener with the service.
   */
  public void addListener(SocketLinkListener listener)
  {
    try {
      if (_listeners.contains(listener))
        throw new IllegalStateException(L.l("listener '{0}' has already been registered", listener));
      
      _listeners.add(listener);
    
      if (_lifecycle.isAfterStarting()) {
        // server/1e00
        listener.bind();
        listener.start();
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Returns the {@link SocketLinkListener}s for this server.
   */
  public Collection<SocketLinkListener> getListeners()
  {
    return Collections.unmodifiableList(_listeners);
  }

  private void applyListenerDefaults(SocketLinkListener port)
  {
    _listenDefaults.configure(port);
  }
  
  //
  // lifecycle
  //

  @Override
  public int getStartPriority()
  {
    return START_PRIORITY_LISTEN;
  }
  
  @Override
  public void start()
    throws Exception
  {
    bindListeners();
    startListeners();
  }

  @Override
  public void stop()
    throws Exception
  {
    ArrayList<SocketLinkListener> listeners = _listeners;
    for (int i = 0; i < listeners.size(); i++) {
      SocketLinkListener listener = listeners.get(i);

      try {
        listener.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Bind the ports.
   */
  private void bindListeners()
    throws Exception
  {
    if (_isStartedListeners.getAndSet(true))
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(_server.getClassLoader());

      ArrayList<SocketLinkListener> listeners = _listeners;
      boolean isFirst = true;

      for (int i = 0; i < listeners.size(); i++) {
        SocketLinkListener listener = listeners.get(i);
          
        if (listener.isAfterBind())
          continue;
          
        if (isFirst) {
          log.info("");
          isFirst = false;
        }

        listener.bind();
      }

      if (! isFirst)
        log.info("");
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Start the listeners
   */
  private void startListeners()
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_server.getClassLoader());

      ArrayList<SocketLinkListener> listeners = _listeners;
      for (int i = 0; i < listeners.size(); i++) {
        SocketLinkListener listener = listeners.get(i);

        listener.start();
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
}
