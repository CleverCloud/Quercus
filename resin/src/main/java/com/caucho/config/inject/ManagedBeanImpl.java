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

package com.caucho.config.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Timer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;
import javax.inject.Qualifier;

import com.caucho.config.ConfigException;
import com.caucho.config.bytecode.ScopeAdapter;
import com.caucho.config.event.EventManager;
import com.caucho.config.event.ObserverMethodImpl;
import com.caucho.config.timer.ScheduleIntrospector;
import com.caucho.config.timer.TimeoutCaller;
import com.caucho.config.timer.TimerTask;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * SimpleBean represents a POJO Java bean registered as a WebBean.
 */
@Module
public class ManagedBeanImpl<X> extends AbstractIntrospectedBean<X>
  implements ScopeAdapterBean<X>, ScheduleBean
{
  private static final L10N L = new L10N(ManagedBeanImpl.class);
  
  private AnnotatedType<X> _annotatedType;

  private InjectionTarget<X> _injectionTarget;

  private HashSet<ObserverMethodImpl<X,?>> _observerMethods
    = new LinkedHashSet<ObserverMethodImpl<X,?>>();

  private boolean _isNormalScope;
  private Object _scopeAdapter;

  public ManagedBeanImpl(InjectManager injectManager,
                         AnnotatedType<X> beanType,
                         boolean isSessionBean)
  {
    super(injectManager, beanType.getBaseType(), beanType);

    _annotatedType = beanType;

    /*
    if (beanType.getType() instanceof Class)
      validateType((Class) beanType.getType());
    */
    
    InjectionTargetBuilder<X> target;
    target = new InjectionTargetBuilder<X>(injectManager, beanType, this);
    
    if (isSessionBean)
      target.setGenerateInterception(false);
    
    _injectionTarget = target; 
  }

  public ManagedBeanImpl(InjectManager webBeans,
                         AnnotatedType<X> beanType,
                         InjectionTarget<X> injectionTarget)
  {
    this(webBeans, beanType, false);

    _injectionTarget = injectionTarget;
  }

  @Override
  public AnnotatedType<X> getAnnotatedType()
  {
    return _annotatedType;
  }

  @Override
  public InjectionTarget<X> getInjectionTarget()
  {
    return _injectionTarget;
  }

  public void setInjectionTarget(InjectionTarget<X> target)
  {
    _injectionTarget = target;
  }
  
  private boolean isNormalScope()
  {
    return _isNormalScope;
  }

  /**
   * Creates a new instance of the component.
   */
  @Override
  public X create(CreationalContext<X> context)
  {
    X instance = _injectionTarget.produce(context);

    if (context != null)
      context.push(instance);

    _injectionTarget.inject(instance, context);
    _injectionTarget.postConstruct(instance);
    
    /*
    if (context instanceof CreationalContextImpl<?>) {
      CreationalContextImpl<X> env = (CreationalContextImpl<X>) context;

      // ioc/0555
      if (env.isTop()) {
        env.postConstruct();
        _injectionTarget.postConstruct(instance);
      }
    }
    else
      _injectionTarget.postConstruct(instance);
      */

    return instance;
  }

  /**
   * Creates a new instance of the component.
   */
  public X createDependent(CreationalContext<X> env)
  {
    X instance = _injectionTarget.produce(env);

    if (env != null) {
      env.push(instance);
      // env.setInjectionTarget(_injectionTarget);
    }

    _injectionTarget.inject(instance, env);
    _injectionTarget.postConstruct(instance);

    // ioc/0555
    /*
    if (env == null)
      _injectionTarget.postConstruct(instance);
      */
    
    return instance;
  }

  @Override
  public X getScopeAdapter(Bean<?> topBean, CreationalContextImpl<X> cxt)
  {
    // ioc/0520
    if (isNormalScope()) {
      Object value = _scopeAdapter;

      if (value == null) {
        ScopeAdapter scopeAdapter = ScopeAdapter.create(getJavaClass());
        _scopeAdapter = scopeAdapter.wrap(getBeanManager().createNormalInstanceFactory(topBean));
        value = _scopeAdapter;
      }

      return (X) value;
    }

    return null;
  }
  
  protected boolean isProxiedScope()
  {
    NormalScope scopeType = getScope().getAnnotation(NormalScope.class);
    
    if (scopeType != null
        && ! getScope().equals(ApplicationScoped.class)) {
      return true;
    }
    else
      return false;
  }

  public Object getScopeAdapter()
  {
    Object value = _scopeAdapter;

    if (value == null) {
      ScopeAdapter scopeAdapter = ScopeAdapter.create(getTargetClass());
      _scopeAdapter = scopeAdapter.wrap(getBeanManager().createNormalInstanceFactory(this));
      value = _scopeAdapter;
    }

    return value;
  }

  /**
   * Returns the set of injection points, for validation.
   */
  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _injectionTarget.getInjectionPoints();
  }

  /*
  public Set<Bean<?>> getProducerBeans()
  {
    return _producerBeans;
  }
  */

  /**
   * Returns the observer methods
   */
  public Set<ObserverMethodImpl<X,?>> getObserverMethods()
  {
    return _observerMethods;
  }

  /**
   * Call post-construct
   */
  public void dispose(X instance)
  {

  }

  /**
   * Call pre-destroy
   */
  @Override
  public void destroy(X instance, CreationalContext<X> cxt)
  {
    _injectionTarget.preDestroy(instance);

    if (cxt!= null) {
      if (cxt instanceof CreationalContextImpl<?>) {
        CreationalContextImpl<?> env = (CreationalContextImpl<?>) cxt;
        env.clearTarget();
      }
      
      cxt.release();
    }
  }

  //
  // introspection
  //

  @Override
  public void introspect()
  {
    super.introspect();
    
    introspect(_annotatedType);

    // ioc/0e13
    if (_injectionTarget instanceof PassivationSetter)
      ((PassivationSetter) _injectionTarget).setPassivationId(getId());
    
    validateBean();
    validatePassivation();
    
    _isNormalScope = getScope().isAnnotationPresent(NormalScope.class);
  }
  
  private void validateBean()
  {
    Class<X> javaClass = _annotatedType.getJavaClass();
    Class<? extends Annotation> scopeType = getScope();
    
    if (javaClass.getTypeParameters().length != 0) {
      if (! Dependent.class.equals(scopeType)) {
        throw new ConfigException(L.l("'{0}' is an invalid bean because it has a generic type and a non-dependent scope.",
                                      javaClass.getName()));
      }
    }
  }
  
  private void validatePassivation()
  {
    Class<? extends Annotation> scopeType = getScope();
    
    if (scopeType == null)
      return;
    
    if (! getBeanManager().isNormalScope(scopeType))
      return;
    
    boolean isPassivation = getBeanManager().isPassivatingScope(scopeType);
    
    Class<?> cl = _annotatedType.getJavaClass();
    
    if (Modifier.isFinal(cl.getModifiers()))
      throw new ConfigException(L.l("'{0}' is an invalid @{1} bean because it is final.",
                                    cl.getName(), scopeType.getName()));
        
  }

  /**
   * Called for implicit introspection.
   */
  protected void introspect(AnnotatedType<X> beanType)
  {
    // super.introspect(beanType);

    // introspectProduces(beanType);

    // introspectObservers(beanType);
  }

  public void introspectProduces()
  {
    ProducesBuilder builder = new ManagedProducesBuilder(getBeanManager());
    
    builder.introspectProduces(this, getAnnotatedType());
  }
  
  /**
   * Introspects the methods for any @Observes
   */
  void introspectObservers()
  {
    EventManager eventManager = getBeanManager().getEventManager();
    
    AnnotatedType<X> annType = getAnnotatedType();
    
    if (! getBeanManager().isIntrospectObservers(annType))
      return;
    
    for (AnnotatedMethod<? super X> beanMethod : getAnnotatedType().getMethods()) {
      int param = EventManager.findObserverAnnotation(beanMethod);
      
      if (param < 0)
        continue;
      
      Class<?> declClass = beanMethod.getJavaMember().getDeclaringClass();
      if (declClass != annType.getJavaClass() 
          && declClass.isAssignableFrom(annType.getJavaClass())
          && ! annType.isAnnotationPresent(Specializes.class)) {
        continue;
      }
      
      eventManager.addObserver(this, beanMethod);
    }
  }

  /**
   * 
   */
  @Override
  public void scheduleTimers(Object value)
  {
    ScheduleIntrospector introspector = new ScheduleIntrospector();
    
    TimeoutCaller timeoutCaller = new BeanTimeoutCaller(value);
    
    ArrayList<TimerTask> taskList
      = introspector.introspect(timeoutCaller, _annotatedType);
    
    if (taskList != null) {
      for (TimerTask task : taskList) {
        task.start();
      }
    }
  }
  
  static class BeanTimeoutCaller implements TimeoutCaller 
  {
    private Object _bean;
    
    BeanTimeoutCaller(Object bean)
    {
      _bean = bean;
    }

    @Override
    public void timeout(Method method)
    throws InvocationTargetException,
           IllegalAccessException
    {
            method.invoke(_bean);
    }

    @Override
    public void timeout(Method method, Timer timer)
        throws InvocationTargetException, IllegalAccessException
    {
      
    }
    
  }
}
