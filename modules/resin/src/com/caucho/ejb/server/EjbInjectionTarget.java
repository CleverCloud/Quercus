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

package com.caucho.ejb.server;

import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.ejb.TimedObject;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.DependentCreationalContext;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.InjectionTargetBuilder;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.inject.OwnerCreationalContext;
import com.caucho.config.program.ConfigProgram;
import com.caucho.ejb.cfg.PostConstructConfig;
import com.caucho.ejb.cfg.PreDestroyConfig;
import com.caucho.ejb.timer.EjbTimerService;

/**
 * Creates an configures an ejb instance
 */
public class EjbInjectionTarget<T> {
  private AbstractEjbBeanManager<T> _manager;
  
  private Class<T> _ejbClass;
  private AnnotatedType<T> _annotatedType;
  
  private Bean<T> _bean;
  
  private ClassLoader _envLoader;
  
  private InjectionTarget<T> _injectionTarget;
  private ArrayList<ConfigProgram> _resourceProgram;

  private PreDestroyConfig _preDestroyConfig;
  private PostConstructConfig _postConstructConfig;
  private Method _cauchoPostConstruct;

  private Method _timeoutMethod;
  private TimerService _timerService;
  
  EjbInjectionTarget(AbstractEjbBeanManager<T> manager,
                     AnnotatedType<T> annotatedType)
  {
    _manager = manager;
    _ejbClass = annotatedType.getJavaClass();
    _annotatedType = annotatedType;
    
    try {
      _cauchoPostConstruct = _ejbClass.getDeclaredMethod("__caucho_postConstruct");
      _cauchoPostConstruct.setAccessible(true);
    } catch (NoSuchMethodException e) {
    }
  }
  
  /**
   * Sets the classloader for the EJB's private environment
   * 
   * @param loader the environment classloader
   */
  public void setEnvLoader(ClassLoader envLoader)
  {
    _envLoader = envLoader;
  }
  

  /**
   * Sets the injection target
   */
  public void setInjectionTarget(InjectionTarget<T> injectionTarget)
  {
    _injectionTarget = injectionTarget;

    if (injectionTarget instanceof InjectionTargetBuilder<?>) {
      InjectionTargetBuilder<T> targetImpl
        = (InjectionTargetBuilder<T>) injectionTarget;
      
      targetImpl.setGenerateInterception(false);
    }
  }

  /**
   * Gets the injection target
   */
  public InjectionTarget<T> getInjectionTarget()
  {
    return _injectionTarget;
  }

  public PostConstructConfig getPostConstruct()
  {
    return _postConstructConfig;
  }

  public PreDestroyConfig getPreDestroy()
  {
    return _preDestroyConfig;
  }

  public void setPostConstruct(PostConstructConfig postConstruct)
  {
    _postConstructConfig = postConstruct;
  }

  public void setPreDestroy(PreDestroyConfig preDestroy)
  {
    _preDestroyConfig = preDestroy;
  }
  
  public TimerService getTimerService()
  {
    return _timerService;
  }
  
  public Method getTimeoutMethod()
  {
    return _timeoutMethod;
  }

  public void bindInjection()
  {
    if (_bean != null)
      return;
    
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_manager.getClassLoader());
      
      InjectManager beanManager = InjectManager.create();

      ManagedBeanImpl<T> managedBean
      = beanManager.createManagedBean(_annotatedType);

      _bean = managedBean;
      setInjectionTarget(managedBean.getInjectionTarget());

      _timeoutMethod = getTimeoutMethod(_bean.getBeanClass());

      if (_timeoutMethod != null)
        _timerService = new EjbTimerService(_manager);

      // Injection binding occurs in the start phase

      InjectManager inject = InjectManager.create();

      // server/4751
      if (_injectionTarget == null) {
        _injectionTarget = inject.createInjectionTarget(_ejbClass);
        _injectionTarget.getInjectionPoints();
      }

      // _resourceProgram = _manager.getResourceProgram(_ejbClass);

      if (_timerService != null) {
        BeanBuilder<TimerService> factory = inject.createBeanFactory(TimerService.class);
        inject.addBean(factory.singleton(_timerService));
      }
    } finally {
      thread.setContextClassLoader(loader);
    }
  }

  Bean<?> getBean()
  {
    return _bean;
  }
  
  private Method getTimeoutMethod(Class<?> targetBean)
  {
    if (TimedObject.class.isAssignableFrom(targetBean)) {
      try {
        return targetBean.getMethod("ejbTimeout", Timer.class);
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    for (Method method : targetBean.getMethods()) {
      if (method.getAnnotation(Timeout.class) != null) {
        return method;
      }
    }

    return null;
  }

  public T newInstance()
  {
    return newInstance(null);
  }
  
  public T newInstance(CreationalContextImpl<?> parentEnv)
  {
    if (_bean == null)
      bindInjection();
    
    T instance = CreationalContextImpl.find(parentEnv, _bean);
    
    if (instance != null)
      return instance;
   
    if (parentEnv == null)
      parentEnv = new OwnerCreationalContext<T>(_bean);
    
    // XXX: circular for stateful
    CreationalContextImpl<T> env 
      = new DependentCreationalContext<T>(_bean, parentEnv, null);
    
    // instance = _bean.create(env);
    instance = _injectionTarget.produce(env);
    
    _injectionTarget.inject(instance, env);

    /*
    for (ConfigProgram program : _resourceProgram) {
      program.inject(instance, env);
    }
    */
    
    _injectionTarget.postConstruct(instance);
    
    return instance;
  }
  
  /**
   * Initialize an instance
   */
  public <X> void initInstance(T instance,
                               InjectionTarget<T> target,
                               X proxy,
                               CreationalContextImpl<X> proxyEnv)
  {
    Bean<T> bean = _bean;

    if (proxyEnv != null && bean != null) {
      // server/4762
      // env.put((AbstractBean) bean, proxy);
      proxyEnv.push(proxy);
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    CreationalContextImpl<T> cxt
      = new DependentCreationalContext<T>(bean, proxyEnv, null);

    try {
      thread.setContextClassLoader(_envLoader);

      if (target != null) {
        target.inject(instance, cxt);
      }

      InjectionTarget<T> selfInjectionTarget = getInjectionTarget();
      
      if (selfInjectionTarget != null) {
        if (target != selfInjectionTarget) {
          selfInjectionTarget.inject(instance, cxt);
        }
      }
      
      for (ConfigProgram program : _resourceProgram) {
        program.inject(instance, cxt);
      }

      if (selfInjectionTarget != null) {
        selfInjectionTarget.postConstruct(instance);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    /*
    if (cxt != null && bean != null)
      cxt.remove(bean);
      */
  }
  
  /**
   * Remove an object.
   */
  public void destroyInstance(T instance)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_envLoader);

      if (getInjectionTarget() != null) {
        getInjectionTarget().preDestroy(instance);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _ejbClass + "]";
  }
}
