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

package com.caucho.env.service;

import java.io.IOException;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.InjectManager;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

public class ResinSystem
{
  private static final L10N L = new L10N(ResinSystem.class);
  private static final Logger log
    = Logger.getLogger(ResinSystem.class.getName());

  private static final EnvironmentLocal<ResinSystem> _serverLocal
    = new EnvironmentLocal<ResinSystem>();

  private final String _id;
  private EnvironmentClassLoader _classLoader;
  
  private final ConcurrentHashMap<Class<?>,ResinService> _serviceMap
    = new ConcurrentHashMap<Class<?>,ResinService>();
  
  private final TreeSet<ResinService> _pendingStart
    = new TreeSet<ResinService>(new StartComparator());
  
  private InjectManager _injectManager;

  private Throwable _configException;

  // private long _shutdownWaitMax = 60 * 1000;

  // private ServerAdmin _admin;

  private final Lifecycle _lifecycle;

  // stats

  private long _startTime;
  
  /**
   * Creates a new servlet server.
   */
  public ResinSystem(String id)
  {
    this(id, (ClassLoader) null);
  }
  
  /**
   * Creates a new servlet server.
   */
  public ResinSystem(String id,
                     ClassLoader loader)
  {
    _id = id;

    if (loader instanceof EnvironmentClassLoader)
      _classLoader = (EnvironmentClassLoader) loader;
    else {
      // the environment id must be independent of the server because
      // of cluster cache requirements. 
      _classLoader = EnvironmentClassLoader.create(loader, "system:");
    }

    _serverLocal.set(this, _classLoader);

    _lifecycle = new Lifecycle(log, toString(), Level.FINE);
    
    // lifecycle for the classloader itself
    addService(new ClassLoaderService());
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_classLoader);
      
      Environment.init();
      
      _injectManager = InjectManager.create();
      
      BeanBuilder<ResinSystem> beanFactory
        = _injectManager.createBeanFactory(ResinSystem.class);
      // factory.deployment(Standard.class);
      beanFactory.type(ResinSystem.class);
      _injectManager.addBean(beanFactory.singleton(this));
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Creates a new servlet server.
   */
  public ResinSystem(String id,
                     Path rootDirectory)
    throws IOException
  {
    this(id);
    
    addService(new RootDirectoryService(rootDirectory));
  }

  /**
   * Creates a new servlet server.
   */
  public ResinSystem(String id,
                      Path rootDirectory,
                      Path dataDirectory)
    throws IOException
  {
    this(id);
    
    addService(new RootDirectoryService(rootDirectory, dataDirectory));
  }
 
  /**
   * Returns the current server
   */
  public static ResinSystem getCurrent()
  {
    return _serverLocal.get();
  }
  
  /**
   * Returns the current identified service.
   */
  public static <T extends ResinService> T
  getCurrentService(Class<T> serviceClass)
  {
    ResinSystem manager = getCurrent();
    
    if (manager != null)
      return manager.getService(serviceClass);
    else
      return null;
  }
  
  /**
   * Returns the current system id.
   */
  public static String getCurrentId()
  {
    ResinSystem system = getCurrent();
    
    if (system == null) {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      throw new IllegalStateException(L.l("ResinSystem is not available in this context.\n  {0}",
                                          loader));
    }
    
    return system.getId();
  }
  
  /**
   * Returns the server id
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Returns the classLoader
   */
  public EnvironmentClassLoader getClassLoader()
  {
    return _classLoader;
  }

  /**
   * Returns the configuration exception
   */
  public Throwable getConfigException()
  {
    return _configException;
  }

  /**
   * Returns the configuration instance.
   */
  public void setConfigException(Throwable exn)
  {
    _configException = exn;
  }

  //
  // statistics
  //

  /**
   * Returns the time the server started in ms.
   */
  public long getStartTime()
  {
    return _startTime;
  }

  /**
   * Returns the lifecycle state
   */
  public String getState()
  {
    return _lifecycle.getStateName();
  }

  /**
   * Returns true if the server has been modified and needs restarting.
   */
  public boolean isModified()
  {
    boolean isModified = _classLoader.isModified();

    if (isModified)
      _classLoader.logModified(log);

    return isModified;
  }

  /**
   * Returns true if the server has been modified and needs restarting.
   */
  public boolean isModifiedNow()
  {
    boolean isModified = _classLoader.isModifiedNow();

    if (isModified)
      log.fine("server is modified");

    return isModified;
  }

  /**
   * Returns true if the server is starting or active
   */
  public boolean isAfterStarting()
  {
    return _lifecycle.isAfterStarting();
  }

  /**
   * Returns true if the server is stopped.
   */
  public boolean isStopping()
  {
    return _lifecycle.isStopping();
  }

  /**
   * Returns true if the server is stopped.
   */
  public boolean isStopped()
  {
    return _lifecycle.isStopped();
  }

  /**
   * Returns true if the server is closed.
   */
  public boolean isDestroyed()
  {
    return _lifecycle.isDestroyed();
  }

  /**
   * Returns true if the server is closed.
   */
  public boolean isDestroying()
  {
    return _lifecycle.isDestroying();
  }

  /**
   * Returns true if the server is currently active and accepting requests
   */
  public boolean isActive()
  {
    return _lifecycle.isActive();
  }
  
  //
  // Service operations
  //
  
  /**
   * Adds a new service.
   */
  public void addService(ResinService service)
  {
    addService(service.getClass(), service);
  }
  
  /**
   * Adds a new service.
   */
  public void addService(Class<?> serviceApi, ResinService service)
  {
    ResinService oldService
      = _serviceMap.putIfAbsent(serviceApi, service);
    
    if (oldService != null) {
      throw new IllegalStateException(L.l("duplicate service '{0}' is not allowed because another service with that class is already registered '{1}'",
                                          service, oldService));
    }
    
    _pendingStart.add(service);

    if (_lifecycle.isActive()) {
      startServices();
    }
  }
  
  /**
   * Adds a new service.
   */
  public <T extends ResinService> T addServiceIfAbsent(T service)
  {
    return addServiceIfAbsent(service.getClass(), service);
  }
  
  /**
   * Adds a new service.
   */
  @SuppressWarnings("unchecked")
  public <T extends ResinService> T 
  addServiceIfAbsent(Class<?> serviceApi, T service)
  {
    ResinService oldService
      = _serviceMap.putIfAbsent(serviceApi, service);
    
    if (oldService != null) {
      return (T) oldService;
    }
    
    _pendingStart.add(service);

    if (_lifecycle.isActive()) {
      startServices();
    }
    
    return null;
  }

  /**
   * Returns the service for the given class.
   */
  @SuppressWarnings("unchecked")
  public <T extends ResinService> T getService(Class<T> cl)
  {
    return (T) _serviceMap.get(cl);
  }
  
  //
  // lifecycle operations
  //

  /**
   * Start the server.
   */
  public void start()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_classLoader);

      if (! _lifecycle.toStarting())
        return;

      _startTime = Alarm.getCurrentTime();

      if (! Alarm.isTest()) {
        log.info("");

        log.info(System.getProperty("os.name")
                 + " " + System.getProperty("os.version")
                 + " " + System.getProperty("os.arch"));

        log.info(System.getProperty("java.runtime.name")
                 + " " + System.getProperty("java.runtime.version")
                 + ", " + System.getProperty("file.encoding")
                 + ", " + System.getProperty("user.language"));

        log.info(System.getProperty("java.vm.name")
                 + " " + System.getProperty("java.vm.version")
                 + ", " + System.getProperty("sun.arch.data.model")
                 + ", " + System.getProperty("java.vm.info")
                 + ", " + System.getProperty("java.vm.vendor"));

        log.info("");

        log.info("user.name  = " + System.getProperty("user.name"));
      }

      startServices();

      _lifecycle.toActive();
    } finally {
      if (! _lifecycle.isActive())
        _lifecycle.toError();
      
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  private void startServices()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_classLoader);
      
      while (_pendingStart.size() > 0) {
        ResinService service = _pendingStart.first();
        _pendingStart.remove(service);
        
        thread.setContextClassLoader(_classLoader);
        
        if (log.isLoggable(Level.FINE))
          log.fine(service + " starting");

        service.start();
        
        if (log.isLoggable(Level.FINER))
          log.finer(service + " active");
      }
    } catch (RuntimeException e) {
      log.log(Level.WARNING, e.toString(), e);

      _lifecycle.toError();

      throw e;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      _lifecycle.toError();

      // if the server can't start, it needs to completely fail, especially
      // for the watchdog
      throw new RuntimeException(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Closes the server.
   */
  public void stop()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      if (! _lifecycle.toStopping())
        return;

      TreeSet<ResinService> services
        = new TreeSet<ResinService>(new StopComparator());
      
      services.addAll(_serviceMap.values());
      
      // sort
      
      for (ResinService service : services) {
        try {
          thread.setContextClassLoader(_classLoader);

          if (log.isLoggable(Level.FINE))
            log.fine(service + " stopping");
          
          service.stop();
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    } finally {
      _lifecycle.toStop();
      
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Closes the server.
   */
  public void destroy()
  {
    stop();

    if (! _lifecycle.toDestroy())
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);

      TreeSet<ResinService> services
        = new TreeSet<ResinService>(new StopComparator());
      
      services.addAll(_serviceMap.values());

      _serviceMap.clear();
      
      for (ResinService service : services) {
        try {
          service.destroy();
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      /*
       * destroy
       */

      log.fine(this + " destroyed");

      _classLoader.destroy();
    } finally {
      DynamicClassLoader.setOldLoader(thread, oldLoader);

      _classLoader = null;
      // resin.startShutdown(L.l("Resin shutdown from Server.destroy()"));
    }
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[id=" + getId() + "]");
  }
  
  class ClassLoaderService extends AbstractResinService {
    @Override
    public int getStartPriority()
    {
      return START_PRIORITY_CLASSLOADER;
    }
    
    @Override
    public void start()
    {
      _classLoader.start();
    }
    
    @Override
    public void stop()
    {
      _classLoader.stop();
    }
  
    @Override
    public void destroy()
    {
      _classLoader.destroy();
    }
  }
  
  static class StartComparator implements Comparator<ResinService> {
    @Override
    public int compare(ResinService a, ResinService b)
    {
      int cmp = a.getStartPriority() - b.getStartPriority();
      
      if (cmp != 0)
        return cmp;
      else
        return a.getClass().getName().compareTo(b.getClass().getName());
    }
  }
  
  static class StopComparator implements Comparator<ResinService> {
    @Override
    public int compare(ResinService a, ResinService b)
    {
      int cmp = b.getStopPriority() - a.getStopPriority();
      
      if (cmp != 0)
        return cmp;
      else
        return b.getClass().getName().compareTo(a.getClass().getName());
    }
  }
}
