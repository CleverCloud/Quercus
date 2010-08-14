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

package com.caucho.env.jpa;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import com.caucho.amber.manager.AmberPersistenceProvider;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.Names;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.CurrentLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ConfigProgram;
import com.caucho.inject.Module;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentEnhancerListener;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.enhancer.ScanClass;
import com.caucho.loader.enhancer.ScanListener;
import com.caucho.util.CharBuffer;
import com.caucho.util.IoUtil;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

/**
 * Manages the JPA persistence contexts.
 */
@Module
public class PersistenceManager 
  implements ScanListener, EnvironmentEnhancerListener
{
  private static final Logger log
    = Logger.getLogger(PersistenceManager.class.getName());

  private static final EnvironmentLocal<PersistenceManager> _localManager
    = new EnvironmentLocal<PersistenceManager>();

  private EnvironmentClassLoader _classLoader;
  private ClassLoader _tempLoader;
  
  private HashMap<String, PersistenceUnitManager> _persistenceUnitMap
    = new HashMap<String, PersistenceUnitManager>();
  
  private ArrayList<ConfigProgram> _unitDefaultList
    = new ArrayList<ConfigProgram>();

  private HashMap<String, ArrayList<ConfigProgram>> _unitDefaultMap
    = new HashMap<String, ArrayList<ConfigProgram>>();
  
  private ArrayList<Path> _pendingRootList = new ArrayList<Path>();
  
  private HashMap<String, EntityManager> _persistenceContextMap
    = new HashMap<String, EntityManager>();

  private ArrayList<LazyEntityManagerFactory> _pendingFactoryList
    = new ArrayList<LazyEntityManagerFactory>();

  private PersistenceManager(ClassLoader loader)
  {
    _classLoader = Environment.getEnvironmentClassLoader(loader);
    _localManager.set(this, _classLoader);

    _tempLoader = _classLoader.getNewTempClassLoader();

    _classLoader.addScanListener(this);

    Environment.addEnvironmentListener(this, _classLoader);

    try {
      if (_classLoader instanceof DynamicClassLoader)
        ((DynamicClassLoader) _classLoader).make();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the local container.
   */
  public static PersistenceManager create()
  {
    return create(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the local container.
   */
  public static PersistenceManager create(ClassLoader loader)
  {
    synchronized (_localManager) {
      PersistenceManager container = _localManager.getLevel(loader);

      if (container == null) {
        container = new PersistenceManager(loader);

        _localManager.set(container, loader);
      }

      return container;
    }
  }

  /**
   * Returns the local container.
   */
  public static PersistenceManager getCurrent()
  {
    return getCurrent(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the current environment container.
   */
  public static PersistenceManager getCurrent(ClassLoader loader)
  {
    synchronized (_localManager) {
      return _localManager.get(loader);
    }
  }

  /**
   * Returns the environment's class loader
   */
  public EnvironmentClassLoader getClassLoader()
  {
    return _classLoader;
  }

  /**
   * Returns the JClassLoader.
   */
  public ClassLoader getTempClassLoader()
  {
    return _tempLoader;
  }

  /**
   * Adds a persistence-unit default
   */
  public void addPersistenceUnitDefault(ConfigProgram program)
  {
    _unitDefaultList.add(program);
  }

  /**
   * Returns the persistence-unit default list.
   */
  public ArrayList<ConfigProgram> getPersistenceUnitDefaults()
  {
    return _unitDefaultList;
  }

  void addPersistenceUnit(String name,
                          ConfigJpaPersistenceUnit configJpaPersistenceUnit)
  {
    PersistenceUnitManager pUnit = createPersistenceUnit(name);
    
    if (pUnit.getRoot() == null)
      pUnit.setRoot(configJpaPersistenceUnit.getPath());
    
    pUnit.addOverrideProgram(configJpaPersistenceUnit.getProgram());
  }

  /**
   * Adds a persistence-unit default
   */
  public void addPersistenceUnitProxy(String name,
                                      ArrayList<ConfigProgram> program)
  {
    ArrayList<ConfigProgram> oldProgram = _unitDefaultMap.get(name);

    if (oldProgram == null)
      oldProgram = new ArrayList<ConfigProgram>();

    oldProgram.addAll(program);

    _unitDefaultMap.put(name, oldProgram);
  }

  public ArrayList<ConfigProgram> getProxyProgram(String name)
  {
    return _unitDefaultMap.get(name);
  }

  public Class<?> loadTempClass(String name) throws ClassNotFoundException
  {
    return Class.forName(name, false, getTempClassLoader());
  }

  public void init()
  {
  }

  public void start()
  {
    configurePersistenceRoots();
    
    startPersistenceUnits();
  }

  public void configurePersistenceRoots()
  {
    ArrayList<Path> rootList = new ArrayList<Path>();

    synchronized (_pendingRootList) {
      rootList.addAll(_pendingRootList);
      _pendingRootList.clear();
    }

    for (Path root : rootList) {
      parsePersistenceConfig(root);
    }
  }

  /**
   * Adds a persistence root.
   */
  private void parsePersistenceConfig(Path root)
  {
    Path persistenceXml = root.lookup("META-INF/persistence.xml");
    
    if (root.getFullPath().endsWith("WEB-INF/classes/")
        && ! persistenceXml.canRead()) {
      persistenceXml = root.lookup("../persistence.xml");
    }

    if (! persistenceXml.canRead())
      return;

    persistenceXml.setUserPath(persistenceXml.getURL());

    if (log.isLoggable(Level.FINE))
      log.fine(this + " parsing " + persistenceXml.getURL());

    InputStream is = null;

    try {
      is = persistenceXml.openRead();

      ConfigPersistence persistence = new ConfigPersistence(root);

      new Config().configure(persistence, is,
          "com/caucho/amber/cfg/persistence-31.rnc");

      for (ConfigPersistenceUnit unitConfig : persistence.getUnitList()) {
        PersistenceUnitManager pUnit
          = createPersistenceUnit(unitConfig.getName());
        
        if (pUnit.getRoot() == null)
          pUnit.setRoot(unitConfig.getRoot());
        
        if (unitConfig.getVersion() != null)
          pUnit.setVersion(unitConfig.getVersion());
        
        pUnit.setPersistenceXmlProgram(unitConfig.getProgram());
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw LineConfigException.create(e);
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (Exception e) {
      }
    }
  }
  
  private PersistenceUnitManager createPersistenceUnit(String name)
  {
    PersistenceUnitManager unit;
    
    synchronized (_persistenceUnitMap) {
      unit = _persistenceUnitMap.get(name);
      
      if (unit != null)
        return unit;
      
      unit = new PersistenceUnitManager(this, name);
      _persistenceUnitMap.put(name, unit);
    }
    
    registerPersistenceUnit(unit);
      
    return unit;
  }

  /**
   * Adds the URLs for the classpath.
   */
  public void startPersistenceUnits()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      // jpa/1630
      thread.setContextClassLoader(_classLoader);

      ArrayList<PersistenceUnitManager> pUnitList
        = new ArrayList<PersistenceUnitManager>();
      
      synchronized (_persistenceUnitMap) {
        pUnitList.addAll(_persistenceUnitMap.values());
      }
      
      for (PersistenceUnitManager pUnit : pUnitList) {
        pUnit.start();
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private void registerPersistenceUnit(PersistenceUnitManager pUnit)
  {
    try {
      InjectManager beanManager = InjectManager.create(_classLoader);
      
      BeanBuilder<EntityManagerFactory> emfFactory;
      emfFactory = beanManager.createBeanFactory(EntityManagerFactory.class);

      emfFactory.qualifier(CurrentLiteral.CURRENT);
      emfFactory.qualifier(Names.create(pUnit.getName()));
      beanManager.addBean(emfFactory.singleton(pUnit.getEntityManagerFactoryProxy()));

      BeanBuilder<EntityManager> emFactory;
      emFactory = beanManager.createBeanFactory(EntityManager.class);

      emFactory.qualifier(CurrentLiteral.CURRENT);
      emFactory.qualifier(Names.create(pUnit.getName()));
      beanManager.addBean(emFactory.singleton(pUnit.getEntityManagerJtaProxy()));

      /*
      factory = manager.createBeanFactory(EntityManager.class);
      factory.binding(CurrentLiteral.CURRENT);
      factory.binding(Names.create(unitName));
      */

      /*
      PersistenceContextComponent pcComp
        = new PersistenceContextComponent(unitName, persistenceContext);
      */

      // manager.addBean(factory.singleton(persistenceContext));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  void addProviderUnit(ConfigPersistenceUnit unit)
  {
    try {
      Class<?> cl = unit.getProvider();

      if (cl == null)
        cl = getServiceProvider();

      if (cl == null)
        cl = AmberPersistenceProvider.class;

      if (log.isLoggable(Level.CONFIG)) {
        log.config("JPA PersistenceUnit[" + unit.getName() + "] handled by "
            + cl.getName());
      }

      PersistenceProvider provider = (PersistenceProvider) cl.newInstance();

      String unitName = unit.getName();
      Map props = null;

      /*
      synchronized (this) {
        LazyEntityManagerFactory lazyFactory
          = new LazyEntityManagerFactory(unit, provider, props);

        _pendingFactoryList.add(lazyFactory);
      }
      */

      /*
      EntityManagerTransactionProxy persistenceContext
        = new EntityManagerTransactionProxy(this, unitName, props);

      _persistenceContextMap.put(unitName, persistenceContext);

      InjectManager manager = InjectManager.create(_classLoader);
      BeanFactory factory;
      factory = manager.createBeanFactory(EntityManagerFactory.class);
      */
      /*
      EntityManagerFactoryComponent emf
        = new EntityManagerFactoryComponent(manager, this, provider, unit);
      */
/*
      EntityManagerFactoryProxy emf
        = new EntityManagerFactoryProxy(this, unitName);

      factory.binding(CurrentLiteral.CURRENT);
      factory.binding(Names.create(unitName));
      manager.addBean(factory.singleton(emf));

      factory = manager.createBeanFactory(EntityManager.class);
      factory.binding(CurrentLiteral.CURRENT);
      factory.binding(Names.create(unitName));
*/
      /*
      PersistenceContextComponent pcComp
        = new PersistenceContextComponent(unitName, persistenceContext);
      */

//      manager.addBean(factory.singleton(persistenceContext));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  Class<?> getServiceProvider()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try {
      Enumeration<URL> e = loader.getResources("META-INF/services/" + PersistenceProvider.class.getName());
      while (e.hasMoreElements()) {
        URL url  = e.nextElement();

        Class<?> providerClass = loadProvider(url);

        if (providerClass != null
            && ! providerClass.equals(AmberPersistenceProvider.class)) {
          return providerClass;
        }
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return null;
  }

  private Class<?> loadProvider(URL url)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    InputStream is = null;

    try {
      is = url.openStream();

      ReadStream in = Vfs.openRead(is);
      String line;
      while ((line = in.readLine()) != null) {
        line = line.trim();

        if (! "".equals(line) && ! line.startsWith("#")) {
          return Class.forName(line, false, loader);
        }
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      IoUtil.close(is);
    }

    return null;
  }

  //
  // private

 
  //
  // ScanListener
  //

  /**
   * Since JPA enhances, it is priority 0
   */
  @Override
  public int getScanPriority()
  {
    return 0;
  }

  /**
   * Returns true if the root is a valid scannable root.
   */
  @Override
  public boolean isRootScannable(Path root, String packageRoot)
  {
    if (root.lookup("META-INF/persistence.xml").canRead()
        || (root.getFullPath().endsWith("WEB-INF/classes/")
            && root.lookup("../persistence.xml").canRead())) {
      _pendingRootList.add(root);
    }

    return false;
  }

  @Override
  public ScanClass scanClass(Path root, String packageRoot,
                             String className, int modifiers)
  {
    return null;
  }

  @Override
  public boolean isScanMatchAnnotation(CharBuffer annotationName)
  {
    return false;
  }

  /**
   * Callback to note the class matches
   */
  @Override
  public void classMatchEvent(EnvironmentClassLoader loader, 
                              Path root,
                              String className)
  {
  }

  //
  // EnvironmentListener
  //

  /**
   * Handles the environment config phase
   */
  @Override
  public void environmentConfigureEnhancer(EnvironmentClassLoader loader)
  {
    configurePersistenceRoots();
    
    // env/0h31
    startPersistenceUnits();
  }

  /**
   * Handles the environment config phase
   */
  @Override
  public void environmentConfigure(EnvironmentClassLoader loader)
  {
    configurePersistenceRoots();
  }

  /**
   * Handles the environment config phase
   */
  @Override
  public void environmentBind(EnvironmentClassLoader loader)
  {
    configurePersistenceRoots();
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  @Override
  public void environmentStart(EnvironmentClassLoader loader)
  {
    start();
  }

  /**
   * Handles the case where the environment is stopping
   */
  @Override
  public void environmentStop(EnvironmentClassLoader loader)
  {
  }

  /**
   * @param unitName
   * @return
   */
  private EntityManagerFactory getEntityManagerFactory(String unitName)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _classLoader.getId() + "]";
  }

  class LazyEntityManagerFactory {
    private final ConfigPersistenceUnit _unit;
    private final PersistenceProvider _provider;
    private final Map _props;

    LazyEntityManagerFactory(ConfigPersistenceUnit unit,
                             PersistenceProvider provider, Map props)
    {
      _unit = unit;
      _provider = provider;
      _props = props;
    }

    void init()
    {
      /*
      synchronized (ManagerPersistence.this) {
        String unitName = _unit.getName();

        EntityManagerFactory factory = _factoryMap.get(unitName);

        if (factory == null) {
          factory = _provider.createContainerEntityManagerFactory(_unit, _props);

          if (factory == null)
            throw new ConfigException(L.l(
                "'{0}' must return an EntityManagerFactory",
                _provider.getClass().getName()));

          if (log.isLoggable(Level.FINE)) {
            log.fine(L.l("{0} creating persistence unitName={1} created with provider '{2}'",
                         this, unitName, _provider.getClass().getName()));
          }

          _factoryMap.put(unitName, factory);
        }
      }
      */
    }
  }

}
