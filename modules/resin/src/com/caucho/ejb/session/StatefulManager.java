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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.ejb.NoSuchEJBException;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.SessionBeanType;

import com.caucho.config.ConfigException;
import com.caucho.config.gen.BeanGenerator;
import com.caucho.config.gen.CandiEnhancedBean;
import com.caucho.config.gen.CandiUtil;
import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.inject.OwnerCreationalContext;
import com.caucho.ejb.cfg.EjbLazyGenerator;
import com.caucho.ejb.gen.StatefulGenerator;
import com.caucho.ejb.inject.SessionBeanImpl;
import com.caucho.ejb.inject.StatefulBeanImpl;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.server.AbstractContext;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

/**
 * Server container for a session bean.
 */
public class StatefulManager<X> extends AbstractSessionManager<X>
{
  private static final L10N L = new L10N(StatefulManager.class);
  private static final Logger log
    = Logger.getLogger(StatefulManager.class.getName());
  
  // XXX: need real lifecycle
  private LruCache<String,StatefulObject> _remoteSessions;
  
  private Object _decoratorClass;
  private List<Decorator<?>> _decoratorBeans;

  public StatefulManager(EjbManager ejbContainer,
                         String moduleName,
                         AnnotatedType<X> rawAnnType,
                         AnnotatedType<X> annotatedType,
                         EjbLazyGenerator<X> lazyGenerator)
  {
    super(ejbContainer, moduleName, rawAnnType, annotatedType, lazyGenerator);
  }

  @Override
  protected String getType()
  {
    return "stateful:";
  }
  
  @Override
  protected Class<?> getContextClass()
  {
    return StatefulContext.class;
  }

  @Override
  protected SessionBeanType getSessionBeanType()
  {
    return SessionBeanType.STATEFUL;
  }
  
  public void bind()
  {
    super.bind();
    
    Class<?> instanceClass = getProxyImplClass();

    if (instanceClass != null
        && CandiEnhancedBean.class.isAssignableFrom(instanceClass)) {
      try {
        Method method = instanceClass.getMethod("__caucho_decorator_init");

        _decoratorClass = method.invoke(null);
      
        Annotation []qualifiers = new Annotation[getBean().getQualifiers().size()];
        getBean().getQualifiers().toArray(qualifiers);
        
        InjectManager moduleBeanManager = InjectManager.create();

        _decoratorBeans = moduleBeanManager.resolveDecorators(getBean().getTypes(), qualifiers);
      
        method = instanceClass.getMethod("__caucho_init_decorators",
                                         List.class);
        
      
        method.invoke(null, _decoratorBeans);
      } catch (InvocationTargetException e) {
        throw ConfigException.create(e.getCause());
      } catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }
  }
  
  @Override
  public <T> StatefulContext<X,T> getSessionContext(Class<T> api)
  {
    return (StatefulContext<X,T>) super.getSessionContext(api);
  }

  /**
   * Returns the JNDI proxy object to create instances of the
   * local interface.
   */
  @Override
  public <T> Object getLocalJndiProxy(Class<T> api)
  {
    StatefulContext<X,T> context = getSessionContext(api);

    return new StatefulProviderProxy<X,T>(context);
  }

  /**
   * Returns the object implementation
   */
  @Override
  public <T> T getLocalProxy(Class<T> api)
  {
    StatefulContext<X,T> context = getSessionContext(api);

    if (context != null) {
      CreationalContextImpl<T> env = 
        new OwnerCreationalContext<T>(null);

      return context.createProxy(env);
    }
    else
      return null;
  }
  
  public Object getStatefulProxy(String key)
  {
    AnnotatedType<?> annType = getLocalBean();
    
    if (annType != null)
      return getLocalProxy(annType.getJavaClass());
    
    ArrayList<AnnotatedType<? super X>> localApi = getLocalApi();
    
    if (localApi.size() > 0)
      return getLocalProxy(localApi.get(0).getJavaClass());
    
    throw new UnsupportedOperationException(L.l("{0} cannot return proxy for {1} because no @Local interface exists",
                                                this, key));
    
  }
  
  public <T> T initProxy(T instance, CreationalContextImpl<T> env)
  {
    if (instance instanceof CandiEnhancedBean) {
      CandiEnhancedBean bean = (CandiEnhancedBean) instance;
      
      Object []delegates = createDelegates((CreationalContextImpl) env);
      
      bean.__caucho_inject(delegates, env);
    }
    
    return instance;
  }
  
  private Object []createDelegates(CreationalContextImpl<?> env)
  {
    if (_decoratorBeans != null) {
      // if (env != null)
      //   env.setInjectionPoint(oldPoint);
      
      return CandiUtil.generateProxyDelegate(getInjectManager(),
                                             _decoratorBeans,
                                             _decoratorClass,
                                             env);
    }
    else
      return null;
  }

  @Override
  protected <T> StatefulContext<X,T> createSessionContext(Class<T> api)
  {
    return new StatefulContext<X,T>(this, api);
  }

  @Override
  protected <T> Bean<T> createBean(ManagedBeanImpl<X> mBean, 
                                   Class<T> api,
                                   Set<Type> apiList,
                                   AnnotatedType<X> extAnnType)
  {
    StatefulContext<X,T> context = getSessionContext(api);

    if (context == null)
      throw new NullPointerException(L.l("'{0}' is an unknown api for {1}",
                                         api, getContext()));
    
    StatefulBeanImpl<X,T> statefulBean
      = new StatefulBeanImpl<X,T>(context, mBean, apiList, extAnnType);

    return statefulBean;
  }

  public void addSession(StatefulObject remoteObject)
  {
    createSessionKey(remoteObject);
  }

  /**
   * Creates the bean generator for the session bean.
   */
  @Override
  protected BeanGenerator<X> createBeanGenerator()
  {
    EjbLazyGenerator<X> lazyGen = getLazyGenerator();
    
    return new StatefulGenerator<X>(getEJBName(), getAnnotatedType(),
                                    lazyGen.getLocalApi(),
                                    lazyGen.getLocalBean(),
                                    lazyGen.getRemoteApi());
  }

  /**
   * Finds the remote bean by its key.
   *
   * @param key the remote key
   *
   * @return the remote interface of the entity.
   */
  @Override
  public AbstractContext getContext(Object key, boolean forceLoad)
    throws FinderException
  {
    throw new NoSuchEJBException("no matching object:" + key);
    /*
    if (key == null)
      return null;

    StatefulContext cxt = _sessions.get(key);

    // ejb/0fe4
    if (cxt == null)
      throw new NoSuchEJBException("no matching object:" + key);
    // XXX ejb/0fe-: needs refactoring of 2.1/3.0 interfaces.
    // throw new FinderException("no matching object:" + key);

    return cxt;
    */
  }

  /**
   * Creates a handle for a new session.
   */
  public String createSessionKey(StatefulObject remote)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the remote stub for the container
   */
  @Override
  public <T> T getRemoteObject(Class<T> api, String protocol)
  {
    StatefulContext<X,T> context = getSessionContext(api);

    if (context != null) {
      // XXX: bean?
      // T value = context.__caucho_createNew(null, null);
      
      // return value;
      
      throw new UnsupportedOperationException(getClass().getName());
    }
    else
      return null;
  }

  /**
   * Remove an object.
   */
  public void remove(String key)
  {
    if (_remoteSessions != null) {
      _remoteSessions.remove(key);

      /*
      // ejb/0fe2
      if (cxt == null)
        throw new NoSuchEJBException("no matching object:" + key);
      */
    }
  }
  
  /**
   * Cleans up the entity server nicely.
   */
  @Override
  public void destroy()
  {
    super.destroy();

    ArrayList<StatefulObject> values = new ArrayList<StatefulObject>();
    
    if (_remoteSessions != null) {
      Iterator<StatefulObject> iter = _remoteSessions.values();
      while (iter.hasNext()) {
        values.add(iter.next());
      }
    }

    _remoteSessions = null;

    /* XXX: may need to restore this
    for (StatefulObject obj : values) {
      try {
        obj.remove();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
    */
    
    log.fine(this + " closed");
  }
}
