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

package com.caucho.ejb.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.ApplicationException;
import javax.ejb.MessageDriven;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.config.ConfigException;
import com.caucho.config.gen.ApplicationExceptionConfig;
import com.caucho.config.types.FileSetType;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.server.AbstractEjbBeanManager;
import com.caucho.ejb.util.AppExceptionItem;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.jms.JmsMessageListener;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

/**
 * Manages the EJB configuration files.
 */
public class EjbConfig {
  private static final L10N L = new L10N(EjbConfig.class);
  
  protected final EjbManager _ejbContainer;

  private ArrayList<FileSetType> _fileSetList = new ArrayList<FileSetType>();

  private HashMap<String,EjbBean<?>> _cfgBeans
    = new HashMap<String,EjbBean<?>>();
  
  private HashSet<Class<?>> _beanSet = new HashSet<Class<?>>();
  
  private ArrayList<EjbBean<?>> _pendingBeans = new ArrayList<EjbBean<?>>();
  private ArrayList<EjbBean<?>> _deployingBeans = new ArrayList<EjbBean<?>>();

  private ArrayList<EjbBeanConfigProxy> _proxyList
    = new ArrayList<EjbBeanConfigProxy>();

  private ArrayList<FunctionSignature> _functions
    = new ArrayList<FunctionSignature>();

  private HashMap<String, MessageDestination> _messageDestinations;

  private ArrayList<Interceptor> _cfgInterceptors
    = new ArrayList<Interceptor>();

  private ArrayList<InterceptorBinding> _cfgInterceptorBindings
    = new ArrayList<InterceptorBinding>();

  private HashMap<Class<?>,ApplicationExceptionConfig> _appExceptionConfig
    = new HashMap<Class<?>,ApplicationExceptionConfig>(); 

  private ConcurrentHashMap<Class<?>,AppExceptionItem> _appExceptionMap
    = new ConcurrentHashMap<Class<?>,AppExceptionItem>(); 

  public EjbConfig(EjbManager ejbContainer)
  {
    _ejbContainer = ejbContainer;
  }

  /**
   * Adds a path for an EJB config file to the config list.
   */
  public void addEjbPath(Path path)
    throws ConfigException
  {
    throw new UnsupportedOperationException();
  }

  public void addConfigProxy(EjbBeanConfigProxy proxy)
  {
    _proxyList.add(proxy);
    _beanSet.add(proxy.getEjbClass());
  }

  /**
   * Returns the schema name.
   */
  public String getSchema()
  {
    return "com/caucho/ejb/cfg/resin-ejb.rnc";
  }


  /**
   * Returns the EJB manager.
   */
  public EjbManager getEjbContainer()
  {
    return _ejbContainer;
  }

  /**
   * Returns the cfg bean with the given name.
   */
  public EjbBean<?> getBeanConfig(String name)
  {
    assert name != null;

    return _cfgBeans.get(name);
  }

  /**
   * Sets the cfg bean with the given name.
   */
  public void setBeanConfig(String name, EjbBean<?> bean)
  {
    if (name == null || bean == null)
      throw new NullPointerException();

    EjbBean<?> oldBean = _cfgBeans.get(name);

    if (oldBean == bean)
      return;
    else if (oldBean != null) {
      throw new IllegalStateException(L.l("{0}: duplicate bean '{1}' old ejb-class={2} new ejb-class={3}",
                                          this, name,
                                          oldBean, // .getEJBClass().getName()));
                                          bean)); // .getEJBClass().getName()));
    }

    _pendingBeans.add(bean);
    _cfgBeans.put(name, bean);
    
    _beanSet.add(bean.getEJBClass());
  }

  /**
   * Returns the interceptor with the given class name.
   */
  public Interceptor getInterceptor(String className)
  {
    assert className != null;

    for (Interceptor interceptor : _cfgInterceptors) {
      if (interceptor.getInterceptorClass().equals(className))
        return interceptor;
    }

    return null;
  }

  /**
   * Adds an interceptor.
   */
  public void addInterceptor(Interceptor interceptor)
  {
    if (interceptor == null)
      throw new NullPointerException();

    _cfgInterceptors.add(interceptor);
  }

  /**
   * Returns the interceptor bindings for a given ejb name.
   */
  public InterceptorBinding getInterceptorBinding(String ejbName,
                                                  boolean isExcludeDefault)
  {
    assert ejbName != null;

    for (InterceptorBinding binding : _cfgInterceptorBindings) {
      if (binding.getEjbName().equals(ejbName))
        return binding;
    }

    // ejb/0fbe vs ejb/0fbf
    for (InterceptorBinding binding : _cfgInterceptorBindings) {
      if (binding.getEjbName().equals("*")) {
        if (isExcludeDefault)
          continue;

        return binding;
      }
    }

    return null;
  }

  /**
   * Adds an application exception.
   */
  public void addApplicationException(ApplicationExceptionConfig applicationException)
  {
    Class<?> appExnClass = applicationException.getExceptionClass();

    _appExceptionConfig.put(appExnClass, applicationException);
  }

  /**
   * Returns the application exceptions.
   */
  public AppExceptionItem getApplicationException(Class<?> exn,
                                                  boolean isSystem)
  {
    AppExceptionItem appExn = _appExceptionMap.get(exn);
    
    if (appExn == null) {
      appExn = createApplicationException(exn, isSystem);
      
      _appExceptionMap.put(exn, appExn);
    }
    
    return appExn;
  }
  
  private AppExceptionItem createApplicationException(Class<?> exn, 
                                                      boolean isSystem)
  {
    if (exn == Error.class || exn == RuntimeException.class)
      return new AppExceptionItem(true, true, true);
    else if (exn == Exception.class)
      return new AppExceptionItem(false, false, true);
    
    ApplicationExceptionConfig cfg = _appExceptionConfig.get(exn);
    
    if (cfg != null) {
      return new AppExceptionItem(true, cfg.isRollback(), cfg.isInherited());
    }
    
    ApplicationException appExn = exn.getAnnotation(ApplicationException.class);
    
    if (appExn != null) {
      return new AppExceptionItem(true, appExn.rollback(), true);
    }
    
    AppExceptionItem parentItem = getApplicationException(exn.getSuperclass(),
                                                          isSystem);
    
    if (parentItem.isInherited())
      return parentItem;
    else if (isSystem)
      return new AppExceptionItem(true, true, true);
    else
      return new AppExceptionItem(false, false, true);
    
  }
  
  /**
   * Binds an interceptor to an ejb.
   */
  public void addInterceptorBinding(InterceptorBinding interceptorBinding)
  {
    _cfgInterceptorBindings.add(interceptorBinding);
  }

  /**
   * Adds the message destination mapping
   */
  public void addMessageDestination(MessageDestination messageDestination)
  {
    if (_messageDestinations == null)
      _messageDestinations = new HashMap<String, MessageDestination>();

    String name = messageDestination.getMessageDestinationName();

    _messageDestinations.put(name, messageDestination);
  }

  public MessageDestination getMessageDestination(String name)
  {
    if (_messageDestinations == null)
      return null;

    return _messageDestinations.get(name);
  }
  
  public boolean isConfiguredBean(Class<?> beanType)
  {
    return _beanSet.contains(beanType);
  }

  public <X> void addAnnotatedType(AnnotatedType<X> rawAnnType,
                                   AnnotatedType<X> annType,
                                   InjectionTarget<X> injectTarget, 
                                   String moduleName)
  {
    try {
      Class<?> type = annType.getJavaClass();

      if (findBeanByType(type) != null)
        return;

      if (annType.isAnnotationPresent(Stateless.class)) {
        EjbStatelessBean<X> bean
          = new EjbStatelessBean<X>(this, rawAnnType, annType, moduleName);
        bean.setInjectionTarget(injectTarget);

        Stateless stateless = annType.getAnnotation(Stateless.class);

        if (! "".equals(stateless.name()) && stateless.name() != null)
          bean.setEJBName(stateless.name());

        setBeanConfig(bean.getEJBName(), bean);
      }
      else if (annType.isAnnotationPresent(Stateful.class)) {
        EjbStatefulBean<X> bean
          = new EjbStatefulBean<X>(this, rawAnnType, annType, moduleName);
        bean.setInjectionTarget(injectTarget);
        
        Stateful stateful = annType.getAnnotation(Stateful.class);

        if (! "".equals(stateful.name()) && stateful.name() != null)
          bean.setEJBName(stateful.name());

        setBeanConfig(bean.getEJBName(), bean);
      }
      else if (annType.isAnnotationPresent(Singleton.class)) {
        EjbSingletonBean<X> bean
          = new EjbSingletonBean<X>(this, rawAnnType, annType, moduleName);
        bean.setInjectionTarget(injectTarget);

        Singleton singleton = annType.getAnnotation(Singleton.class);

        if (! "".equals(singleton.name()) && singleton.name() != null)
          bean.setEJBName(singleton.name());

        setBeanConfig(bean.getEJBName(), bean);
      }      
      else if (annType.isAnnotationPresent(MessageDriven.class)) {
        EjbMessageBean<X> bean
          = new EjbMessageBean<X>(this, rawAnnType, annType, moduleName);
        bean.setInjectionTarget(injectTarget);

        setBeanConfig(bean.getEJBName(), bean);
      }
      else if (annType.isAnnotationPresent(JmsMessageListener.class)) {
        JmsMessageListener listener
        = annType.getAnnotation(JmsMessageListener.class);

        EjbMessageBean<X> bean
        = new EjbMessageBean<X>(this, rawAnnType, annType, listener.destination());

        bean.setInjectionTarget(injectTarget);

        setBeanConfig(bean.getEJBName(), bean);
      }
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Finds an entity bean by its abstract schema.
   */
  @SuppressWarnings("unchecked")
  public <X> EjbBean<X> findBeanByType(Class<X> type)
  {
    for (EjbBean<?> bean : _cfgBeans.values()) {
      Class<?> cl = bean.getEJBClass();
      
      // ejb/0j03
      if (cl != null && cl.getName().equals(type.getName()))
        return (EjbBean<X>) bean;
    }

    return null;
  }

  /**
   * Adds a function.
   */
  public void addFunction(FunctionSignature sig, String sql)
  {
    _functions.add(sig);
  }

  /**
   * Gets the function list.
   */
  public ArrayList<FunctionSignature> getFunctions()
  {
    return _functions;
  }

  /**
   * Configures the pending beans.
   */
  public void configure()
    throws ConfigException
  {
    findConfigurationFiles();

    try {
      for (EjbBeanConfigProxy configProxy : _proxyList) {
        configProxy.configure();
      }
      
      _proxyList.clear();
      
      ArrayList<EjbBean<?>> beanConfig = new ArrayList<EjbBean<?>>(_pendingBeans);
      _pendingBeans.clear();

      _deployingBeans.addAll(beanConfig);

      /*
      for (EjbBean<?> bean : beanConfig) {
        bean.init();
      }
      */
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }


  /**
   * Configures the pending beans.
   */
  private void findConfigurationFiles()
    throws ConfigException
  {
    for (FileSetType fileSet : _fileSetList) {
      for (Path path : fileSet.getPaths()) {
        addEjbPath(path);
      }
    }
  }

  /**
   * Configures the pending beans.
   */
  public void deploy()
    throws ConfigException
  {
    try {
      ClassLoader parentLoader = _ejbContainer.getClassLoader();

      Path workDir = _ejbContainer.getWorkDir();

      JavaClassGenerator javaGen = new JavaClassGenerator();
      javaGen.setWorkDir(workDir);
      javaGen.setParentLoader(parentLoader);

      ArrayList<EjbBean<?>> deployingBeans
        = new ArrayList<EjbBean<?>>(_deployingBeans);
      _deployingBeans.clear();

      deployBeans(deployingBeans, javaGen);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Configures the pending beans.
   */
  public void deployBeans(ArrayList<EjbBean<?>> beanConfig,
                          JavaClassGenerator javaGen)
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_ejbContainer.getClassLoader());

      // ejb/0g1c, ejb/0f68, ejb/0f69
      ArrayList<EjbBean<?>> beanList = new ArrayList<EjbBean<?>>();

      for (EjbBean<?> bean : beanConfig) {
        if (beanList.contains(bean))
          continue;
        
        bean.init();
        
        deployBean(beanConfig, javaGen, beanList, bean);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  private <X> void deployBean(ArrayList<EjbBean<?>> beanConfig,
                              JavaClassGenerator javaGen,
                              ArrayList<EjbBean<?>> beanList,
                              EjbBean<X> bean)
    throws Exception
  {
    Thread thread = Thread.currentThread();
    
    EjbLazyGenerator<X> lazyGenerator
      = new EjbLazyGenerator<X>(bean.getAnnotatedType(), javaGen,
                                bean.getLocalList(), bean.getLocalBean(), 
                                bean.getRemoteList());

    AbstractEjbBeanManager<X> server = initBean(bean, lazyGenerator);
    
    _ejbContainer.addServer(server);
    
    ArrayList<String> dependList = bean.getBeanDependList();

    for (String depend : dependList) {
      for (EjbBean<?> b : beanConfig) {
        if (bean == b)
          continue;

        // XXX: what test case is this for?
        if (depend.equals(b.getEJBName())) {
          beanList.add(b);

          /*
          AbstractEjbBeanManager<?> dependServer = initBean(b, lazyGenerator);

          initResources(b, dependServer);
          */

          thread.setContextClassLoader(server.getClassLoader());
        }
      }
    }

    // XXX: 4.0.8 timing issues
    // initResources(bean, server);
  }

  private <X> AbstractEjbBeanManager<X>
  initBean(EjbBean<X> bean,
           EjbLazyGenerator<X> lazyGenerator)
    throws Exception
  {
    AbstractEjbBeanManager<X> server = bean.deployServer(_ejbContainer, lazyGenerator);

    server.init();

    return server;
  }

  public String toString()
  {
    String id = _ejbContainer.getClassLoader().getId();
    
    return getClass().getSimpleName() + "[" + id + "]";
  }
}
