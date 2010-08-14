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

package com.caucho.ejb.manager;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.jms.ConnectionFactory;

import com.caucho.amber.manager.AmberContainer;
import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.ejb.cfg.EjbConfigManager;
import com.caucho.ejb.cfg.EjbRootConfig;
import com.caucho.ejb.protocol.EjbProtocolManager;
import com.caucho.ejb.server.AbstractEjbBeanManager;
import com.caucho.ejb.util.AppExceptionItem;
import com.caucho.java.WorkDir;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.enhancer.ScanClass;
import com.caucho.loader.enhancer.ScanListener;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;

/**
 * Environment-based container.
 */
public class EjbManager implements ScanListener, EnvironmentListener {
  private static final Logger log = Logger.getLogger(EjbManager.class
      .getName());

  private static final EnvironmentLocal<EjbManager> _localContainer
    = new EnvironmentLocal<EjbManager>();
  
  private static final EnvironmentLocal<Boolean> _localScanAll
    = new EnvironmentLocal<Boolean>();

  private ClassLoader _globalClassLoader;
  private final EnvironmentClassLoader _classLoader;
  private final ClassLoader _tempClassLoader;

  private final EjbManager _parentContainer;

  private final EjbConfigManager _configManager;
  private final EjbProtocolManager _protocolManager;

  private final HashSet<String> _ejbUrls = new HashSet<String>();

  // the exact list of root to scan - used by EJBContainer
  private ArrayList<Path> _scannableRoots = null;

  //
  // configuration
  //

  private boolean _isAutoCompile = true;
  private Path _workDir;

  private ConnectionFactory _jmsConnectionFactory;
  private int _messageConsumerMax = 5;
  
  //
  // active servers
  //

  private final ArrayList<AbstractEjbBeanManager<?>> _serverList 
    = new ArrayList<AbstractEjbBeanManager<?>>();

  private EjbManager(ClassLoader loader)
  {
    _parentContainer = _localContainer.get(loader);

    _classLoader = Environment.getEnvironmentClassLoader(loader);

    _tempClassLoader = _classLoader.getNewTempClassLoader();

    _localContainer.set(this, _classLoader);

    if (_parentContainer != null)
      copyContainerDefaults(_parentContainer);

    // _ejbAdmin = new EJBAdmin(this);

    _protocolManager = new EjbProtocolManager(this);

    _configManager = new EjbConfigManager(this);

    // _workDir = WorkDir.getLocalWorkDir().lookup("ejb");

    _classLoader.addScanListener(this);

    Environment.addEnvironmentListener(this);
    
    EjbModule.create("default");
  }

  /**
   * Returns the local container.
   */
  public static EjbManager create()
  {
    return create(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the local container.
   */
  public static EjbManager create(ClassLoader loader)
  {
    synchronized (_localContainer) {
      EjbManager container = _localContainer.getLevel(loader);

      if (container == null) {
        Boolean ejbManager = null;
        
        ejbManager = (Boolean) Environment.getAttribute("ejb.manager", loader);
        
        if (ejbManager == null || Boolean.TRUE.equals(ejbManager))
          container = new EjbManager(loader);
        else
          container = _localContainer.get(loader);

        _localContainer.set(container, loader);
      }

      return container;
    }
  }

  /**
   * Returns the local container.
   */
  public static EjbManager getCurrent()
  {
    return getCurrent(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the current environment container.
   */
  public static EjbManager getCurrent(ClassLoader loader)
  {
    synchronized (_localContainer) {
      return _localContainer.get(loader);
    }
  }
  
  public static void setScanAll()
  {
    _localScanAll.set(true);
  }
  
  public void setGlobalClassLoader(ClassLoader globalClassLoader)
  {
    _globalClassLoader = globalClassLoader;
  }

  /**
   * Returns the parent loader
   */
  public EnvironmentClassLoader getClassLoader()
  {
    return _classLoader;
  }

  public ClassLoader getGlobalClassLoader()
  {
    if (_globalClassLoader != null)
      return _globalClassLoader;
    else
      return _classLoader;
  }
  
  /**
   * Returns the introspection class loader
   */
  public ClassLoader getIntrospectionClassLoader()
  {
    return _tempClassLoader;
  }

  /**
   * Returns the configuration manager.
   */
  public EjbConfigManager getConfigManager()
  {
    return _configManager;
  }

  public EjbManager getParent()
  {
    return _parentContainer;
  }

  /**
   * Returns the protocol manager.
   */
  public EjbProtocolManager getProtocolManager()
  {
    return _protocolManager;
  }

  /**
   * true if beans should be auto-compiled
   */
  public void setAutoCompile(boolean isAutoCompile)
  {
    _isAutoCompile = isAutoCompile;
  }

  /**
   * true if beans should be auto-compiled
   */
  public boolean isAutoCompile()
  {
    return _isAutoCompile;
  }

  /**
   * The work directory for EJB-generated files
   */
  public void setWorkDir(Path workDir)
  {
    _workDir = workDir;
  }

  /**
   * The work directory for EJB-generated files
   */
  public Path getWorkDir()
  {
    if (_workDir != null)
      return _workDir;
    else
      return WorkDir.getLocalWorkDir().lookup("ejb");
  }

  /**
   * The JMS connection factory for the container.
   */
  public void setJmsConnectionFactory(ConnectionFactory factory)
  {
    _jmsConnectionFactory = factory;
  }

  /**
   * Sets the consumer maximum for the container.
   */
  public void setMessageConsumerMax(int consumerMax)
  {
    _messageConsumerMax = consumerMax;
  }

  /**
   * The consumer maximum for the container.
   */
  public int getMessageConsumerMax()
  {
    return _messageConsumerMax;
  }

  /**
   * Copy defaults from the parent container when first created.
   */
  private void copyContainerDefaults(EjbManager parent)
  {
    _isAutoCompile = parent._isAutoCompile;
    _jmsConnectionFactory = parent._jmsConnectionFactory;
    _messageConsumerMax = parent._messageConsumerMax;
  }

  //
  // Bean configuration and management
  //
  
  public boolean isConfiguredBean(Class<?> beanType)
  {
    return _configManager.isConfiguredBean(beanType);
  }

  public <T> void createBean(AnnotatedType<T> type, 
                             InjectionTarget<T> injectionTarget)
  {
    // XXX moduleName
    _configManager.addAnnotatedType(type, type, injectionTarget, "");
  }

  //
  // AbstractServer management
  //

  /**
   * Adds a server.
   */
  public void addServer(AbstractEjbBeanManager<?> server)
  {
    _serverList.add(server);

    getProtocolManager().addServer(server);
  }
  
  public AbstractEjbBeanManager<?> getServerByEjbName(String name)
  {
    for (AbstractEjbBeanManager<?> server : _serverList) {
      if (server.getEJBName().equals(name))
        return server;
    }
    
    if (_parentContainer != null)
      return _parentContainer.getServerByEjbName(name);
    else
      return null;
  }

  /**
   * Since EJB doesn't bytecode enhance, it's priority 1
   */
  @Override
  public int getScanPriority()
  {
    return 1;
  }

  /**
   * Adds a root URL
   */
  public void configureRootPath(Path root)
  {
    if (root.getURL().endsWith(".jar"))
      root = JarPath.create(root);

    // XXX: ejb/0fbn
    Path ejbJar = root.lookup("META-INF/ejb-jar.xml");
    if (ejbJar.canRead()) {
      getConfigManager().configureRootPath(root);
    }
    else if (root.getURL().endsWith("WEB-INF/classes")) {
      ejbJar = root.lookup("../ejb-jar.xml");
    
      if (ejbJar.canRead()) {
        getConfigManager().configureRootPath(root);
      }
    }

    _ejbUrls.add(root.getURL());
  }

  public void setScannableRoots(ArrayList<Path> roots)
  {
    _scannableRoots = roots;
  }

  /**
   * Returns true if the root is a valid scannable root.
   */
  @Override
  public boolean isRootScannable(Path root, String packageRoot)
  {
    Path scanRoot = root;
    
    if (packageRoot != null)
      scanRoot = scanRoot.lookup(packageRoot.replace('.', '/'));
      
    if (_scannableRoots == null) {
      if (! Boolean.TRUE.equals(_localScanAll.get())) {
        if (! scanRoot.lookup("META-INF/ejb-jar.xml").canRead()) {
          return false;
        }
      }     
    
      if (_ejbUrls.contains(root.getURL())) {
        return false;
      }
    }
    else {
      Path path = scanRoot;

      if (root instanceof JarPath)
        path = ((JarPath) root).getContainer();

      if (! _scannableRoots.contains(path))
        return false;
    }
    
    if (log.isLoggable(Level.FINE))
        log.fine("EJB scanning '" + root + "'");

    EjbRootConfig context = _configManager.createRootConfig(scanRoot);

    if (context.isScanComplete())
      return false;
    else {
      context.setScanComplete(true);
      return true;
    }
  }

  @Override
  public ScanClass scanClass(Path root, String packageRoot,
                             String className, int modifiers)
  {
    if (Modifier.isInterface(modifiers))
      return null;
    else if (Modifier.isAbstract(modifiers))
      return null;
    else
      return new EjbScanClass(root, className, this);
  }

  @Override
  public boolean isScanMatchAnnotation(CharBuffer annotationName)
  {
    if (annotationName.matches("javax.ejb.Stateless")) {
      return true;
    }
    if (annotationName.matches("javax.ejb.Singleton")) {
      return true;
    }
    else if (annotationName.matches("javax.ejb.Stateful")) {
      return true;
    }
    else if (annotationName.matches("javax.ejb.MessageDriven")) {
      return true;
    }
    else if (annotationName.matches("javax.annotation.ManagedBean")) {
      return true;
    }
    else
      return false;
  }

  /**
   * Callback to note the class matches
   */
  public void classMatchEvent(EnvironmentClassLoader loader,
                              Path root,
                              String className)
  {
    EjbRootConfig config = _configManager.createRootConfig(root);
    config.addClassName(className);
  }
  
  void addScanClass(Path root,
                    String className)
  {
    EjbRootConfig config = _configManager.createRootConfig(root);
    config.addClassName(className);
  }
  
  //
  // app exception

  /**
   * Returns the configuration for a system exception.
   */
  public AppExceptionItem
  getSystemException(Class<?> exceptionClass)
  {
    return _configManager.getApplicationException(exceptionClass, true);
  }

  /**
   * Returns the configuration for an application exception.
   */
  public AppExceptionItem
  getApplicationException(Class<?> exceptionClass)
  {
    return _configManager.getApplicationException(exceptionClass, false);
  }

  //
  // lifecycle methods
  //

  public void init()
  {
  }

  private void config()
  {
    _configManager.start();
  }

  private void bind()
  {
    config();
    
    InjectManager.create().bind();
    
    for (AbstractEjbBeanManager<?> server : _serverList) {
      server.bind();
    }
  }

  public void start() throws ConfigException
  {
    try {
      // AmberContainer.create().start();

      bind();  // ejb/4200

      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      for (AbstractEjbBeanManager<?> server : _serverList) {
        try {
          thread.setContextClassLoader(server.getClassLoader());

          server.start();
        } finally {
          thread.setContextClassLoader(oldLoader);
        }
      }

      AmberContainer.create().start();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Closes the container.
   */
  public void destroy()
  {
    /*
     * if (! _lifecycle.toDestroy()) return;
     */

    try {
      ArrayList<AbstractEjbBeanManager<?>> servers;
      servers = new ArrayList<AbstractEjbBeanManager<?>>(_serverList);

      _serverList.clear();

      // only purpose of the sort is to make the qa order consistent
      Collections.sort(servers, new ServerCmp());

      for (AbstractEjbBeanManager<?> server : servers) {
        try {
          getProtocolManager().removeServer(server);
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      for (AbstractEjbBeanManager<?> server : servers) {
        try {
          server.destroy();
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Handles the case where the environment is configuring
   */
  public void environmentConfigure(EnvironmentClassLoader loader)
  {
    config();
  }

  /**
   * Handles the case where the environment is configuring
   */
  @Override
  public void environmentBind(EnvironmentClassLoader loader)
  {
    bind();
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    start();
  }

  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    destroy();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _classLoader + "]";
  }

  /**
   * Sorts the servers so they can be destroyed in a consistent order. (To make
   * QA sane.)
   */
  static class ServerCmp implements Comparator<AbstractEjbBeanManager<?>> {
    public int compare(AbstractEjbBeanManager<?> a, AbstractEjbBeanManager<?> b)
    {
      return a.getEJBName().compareTo(b.getEJBName());
    }
  }
}
