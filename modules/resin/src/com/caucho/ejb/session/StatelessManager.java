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
package com.caucho.ejb.session;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.SessionBeanType;

import com.caucho.config.gen.BeanGenerator;
import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.ejb.SessionPool;
import com.caucho.ejb.cfg.EjbLazyGenerator;
import com.caucho.ejb.gen.StatefulGenerator;
import com.caucho.ejb.gen.StatelessGenerator;
import com.caucho.ejb.inject.StatelessBeanImpl;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.server.AbstractContext;
import com.caucho.util.L10N;

/**
 * Server home container for a stateless session bean
 */
public class StatelessManager<X> extends AbstractSessionManager<X> {
  private static final L10N L = new L10N(StatelessManager.class);

  private static Logger log
    = Logger.getLogger(StatelessManager.class.getName());
  
  private int _sessionIdleMax = 16;
  private int _sessionConcurrentMax = -1;
  private long _sessionConcurrentTimeout = -1;

  /**
   * Creates a new stateless server.
   *
   * @param urlPrefix
   *          the url prefix for any request to the server
   * @param allowJVMCall
   *          allows fast calls to the same JVM (with serialization)
   * @param config
   *          the session configuration from the ejb.xml
   */
  public StatelessManager(EjbManager ejbContainer, 
                          String moduleName,
                          AnnotatedType<X> rawAnnType,
                          AnnotatedType<X> annotatedType,
                          EjbLazyGenerator<X> ejbGenerator)
  {
    super(ejbContainer, moduleName, rawAnnType, annotatedType, ejbGenerator);
    
    introspect();
  }

  @Override
  protected String getType()
  {
    return "stateless:";
  }

  @Override
  protected SessionBeanType getSessionBeanType()
  {
    return SessionBeanType.STATELESS;
  }

  public int getSessionIdleMax()
  {
    return _sessionIdleMax;
  }
  
  public int getSessionConcurrentMax()
  {
    return _sessionConcurrentMax;
  }
  
  public long getSessionConcurrentTimeout()
  {
    return _sessionConcurrentTimeout;
  }
  
  @Override
  protected <T> StatelessContext<X,T> getSessionContext(Class<T> api)
  {
    return (StatelessContext<X,T>) super.getSessionContext(api);
  }

  /**
   * Returns the JNDI proxy object to create instances of the local interface.
   */
  @Override
  public <T> Object getLocalJndiProxy(Class<T> api)
  {
    StatelessContext<X,T> context = getSessionContext(api);

    return new StatelessProviderProxy<X,T>(context.createProxy(null));
  }

  /**
   * Returns the object implementation
   */
  @Override
  public <T> T getLocalProxy(Class<T> api)
  {
    return getSessionContext(api).createProxy(null);
  }

  @Override
  protected <T> Bean<T> createBean(ManagedBeanImpl<X> mBean,
                                   Class<T> api,
                                   Set<Type> apiList,
                                   AnnotatedType<X> extAnnType)
  {
    StatelessContext<X,T> context = getSessionContext(api);

    if (context == null)
      throw new NullPointerException(L.l("'{0}' is an unknown api for {1}",
          api, this));

    StatelessBeanImpl<X,T> statelessBean
      = new StatelessBeanImpl<X,T>(this, mBean, api, apiList, context, extAnnType);

    return statelessBean;
  }

  @Override
  protected Class<?> getContextClass()
  {
    return StatelessContext.class;
  }

  /**
   * Creates the bean generator for the session bean.
   */
  @Override
  protected BeanGenerator<X> createBeanGenerator()
  {
    EjbLazyGenerator<X> lazyGen = getLazyGenerator();
    
    return new StatelessGenerator<X>(getEJBName(), getAnnotatedType(),
                                     lazyGen.getLocalApi(),
                                     lazyGen.getLocalBean(),
                                     lazyGen.getRemoteApi());
  }
  
  /**
   * Called by the StatelessProxy on initialization.
   */
  public <T> StatelessPool<X,T> createStatelessPool(StatelessContext<X,T> context,
                                                    List<Interceptor<?>> interceptorBeans)
  {
    return new StatelessPool<X,T>(this, context, interceptorBeans);
  }

  /**
   * Returns the remote stub for the container
   */
  @Override
  public <T> T getRemoteObject(Class<T> api, String protocol)
  {
    if (api == null)
      return null;

    StatelessContext<X,T> context = getSessionContext(api);

    if (context != null) {
      T result = context.createProxy(null);

      return result;
    } else {
      log.fine(this + " unknown api " + api.getName());
      return null;
    }
  }

  @Override
  public void init() throws Exception
  {
    super.init();
  }

  private void introspect()
  {
    AnnotatedType<?> annType = getAnnotatedType();
    SessionPool sessionPool = annType.getAnnotation(SessionPool.class);

    if (sessionPool != null) {
      if (sessionPool.maxIdle() >= 0)
        _sessionIdleMax = sessionPool.maxIdle();
      
      if (sessionPool.maxConcurrent() >= 0)
        _sessionConcurrentMax = sessionPool.maxConcurrent();
      
      if (sessionPool.maxConcurrentTimeout() >= 0)
        _sessionConcurrentTimeout = sessionPool.maxConcurrentTimeout();
    }
  }

  @Override
  protected <T> StatelessContext<X,T> createSessionContext(Class<T> api)
  {
    return new StatelessContext<X,T>(this, api);
  }

  @Override
  protected void postStart()
  {
  }

  @Override
  public AbstractContext getContext(Object key, boolean forceLoad)
  {
    return getContext();
  }

  // XXX
  public Object[] getInterceptorBindings(List<Interceptor<?>> interceptorBeans,
                                         CreationalContextImpl<?> parentEnv)
  {
    int size = interceptorBeans.size();
    
    if (size == 0)
      return null;
    
    Object []interceptors = new Object[size];
    
    for (int i = 0; i < size; i++) {
      Interceptor<?> bean = interceptorBeans.get(i);
      
      interceptors[i] = getInjectManager().getReference(bean, parentEnv); 
    }
    
    return interceptors;
  }
}
