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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jca.ra;

import com.caucho.config.ConfigException;
import com.caucho.loader.*;
import com.caucho.transaction.ConnectionPool;
import com.caucho.transaction.UserTransactionProxy;
import com.caucho.util.L10N;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkManager;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the resource manager.
 */
public class ResourceManagerImpl implements BootstrapContext {
  private static final L10N L = new L10N(ResourceManagerImpl.class);
  private static final Logger log
    = Logger.getLogger(ResourceManagerImpl.class.getName());

  private static EnvironmentLocal<ResourceManagerImpl> _localManager
    = new EnvironmentLocal<ResourceManagerImpl>();

  private final EnvironmentClassLoader _loader;

  private UserTransactionProxy _tm;

  private ArrayList<ResourceAdapter> _resources
    = new ArrayList<ResourceAdapter>();

  private ArrayList<ConnectionPool> _connectionManagers
    = new ArrayList<ConnectionPool>();

  private ArrayList<SoftReference<Timer>> _timers
    = new ArrayList<SoftReference<Timer>>();

  private WorkManagerImpl _workManager;

  private boolean _isInit;
  private boolean _isClosed;

  /**
   * Constructor.
   */
  private ResourceManagerImpl()
  {
    _loader = Environment.getEnvironmentClassLoader();
    
    Environment.addClassLoaderListener(new CloseListener(this));

    _tm = UserTransactionProxy.getInstance();

    if (_tm == null)
      throw new IllegalStateException();
  }

  /**
   * Returns the impl.
   */
  public static ResourceManagerImpl createLocalManager()
  {
    return create();
  }

  /**
   * Returns the impl.
   */
  public static ResourceManagerImpl create()
  {
    ResourceManagerImpl rm;
    
    synchronized (_localManager) {
      rm = _localManager.getLevel();

      if (rm == null) {
        rm = new ResourceManagerImpl();
        _localManager.set(rm);
      }
    }

    return rm;
  }

  /**
   * Returns the impl.
   */
  public static ResourceManagerImpl getLocalManager()
  {
    return _localManager.getLevel();
  }

  /**
   * Adds a resource to the manager.
   */
  public static void addResource(ResourceAdapter resource)
    throws ConfigException
  {
    ResourceManagerImpl rm = createLocalManager();

    rm.addResourceImpl(resource);
  }

  /**
   * Adds a resource to the resource manager.
   */
  private void addResourceImpl(ResourceAdapter resource)
  {
    try {
      resource.start(this);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    _resources.add(resource);
  }

  /**
   * Returns a connection manager
   */
  public ConnectionPool createConnectionPool()
  {
    ConnectionPool cm = new ConnectionPool();

    cm.setTransactionManager(_tm);

    _connectionManagers.add(cm);

    return cm;
  }

  /**
   * Returns a WorkManager instance.
   */
  public WorkManager getWorkManager()
  {
    synchronized (this) {
      if (_workManager == null)
        _workManager = new WorkManagerImpl();
    }

    return _workManager;
  }

  /**
   * Returns an XATerminator.  The XATerminator could be used for
   * transaction completion and crash recovery.
   */
  public XATerminator getXATerminator()
  {
    return null;
  }

  /**
   * Creates a new Timer instance.
   */
  public Timer createTimer() throws javax.resource.spi.UnavailableException 
  {
    TimerImpl timer = new TimerImpl(this);

    synchronized (_timers) {
      SoftReference<Timer> timerRef = new SoftReference<Timer>(timer);
      
      _timers.add(timerRef);
    }
    
    return timer;
  }

  /**
   * Removes a new Timer instance.
   */
  void removeTimer(Timer timer)
  {
    if (_timers == null)
      return;
    
    synchronized (_timers) {
      for (int i = _timers.size(); i >= 0; i--) {
        SoftReference<Timer> timerRef = _timers.get(i);
        Timer oldTimer = timerRef.get();

        if (oldTimer == null)
          _timers.remove(i);
        else if (oldTimer == timer)
          _timers.remove(i);
      }
    }
  }
  
  /**
   * Handles the case where a class loader is activated.
   */
  public void classLoaderInit(DynamicClassLoader loader)
  {
  }
  
  /**
   * Handles the case where a class loader is dropped.
   */
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
    destroy();
  }

  /**
   * Closes the resource manager.
   */
  public void destroy()
  {
    ArrayList<ConnectionPool> connectionManagers;
    ArrayList<ResourceAdapter> resources;
    ArrayList<SoftReference<Timer>> timers;
    
    synchronized (this) {
      if (_isClosed)
        return;
      _isClosed = true;
      
      connectionManagers = _connectionManagers;
      _connectionManagers = null;
      
      resources = _resources;
      _resources = null;
      
      timers = _timers;
      _timers = null;
    }

    // Kill timers first, so they won't try to spawn work tasks
    for (int i = 0; i < timers.size(); i++) {
      SoftReference<Timer> timerRef = timers.get(i);
      Timer timer = timerRef.get();

      try {
        if (timer != null)
          timer.cancel();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    // cancel the work managers
    if (_workManager != null)
      _workManager.destroy();

    for (int i = 0; i < connectionManagers.size(); i++) {
      ConnectionPool connectionManager = connectionManagers.get(i);

      try {
        connectionManager.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    // finally, close the resources
    for (int i = 0; i < resources.size(); i++) {
      ResourceAdapter resource = resources.get(i);

      try {
        resource.stop();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  public class TimerImpl extends Timer {
    private ResourceManagerImpl _rm;

    TimerImpl(ResourceManagerImpl rm)
    {
      super(true);

      _rm = rm;
    }
    
    public void cancel()
    {
      _rm.removeTimer(this);

      super.cancel();
    }
  }

  public String toString()
  {
    if (_loader != null)
      return getClass().getSimpleName() + "[" + _loader.getId() + "]";
    else
      return getClass().getSimpleName() + "[]";
  }
}
