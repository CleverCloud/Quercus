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

package com.caucho.ejb.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

import com.caucho.config.event.EventManager;
import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.InjectEnvironmentBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.inject.ScopeAdapterBean;
import com.caucho.config.reflect.AnnotatedMethodImpl;
import com.caucho.config.reflect.AnnotatedParameterImpl;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.ejb.session.AbstractSessionContext;
import com.caucho.inject.Module;

/**
 * Internal implementation for a Bean
 */
@Module
public class SessionBeanImpl<X,T>
  implements ScopeAdapterBean<T>, Bean<T>, PassivationCapable, EjbGeneratedBean,
             InjectEnvironmentBean
{
  private AbstractSessionContext<X,T> _context;
  private ManagedBeanImpl<X> _bean;
  private LinkedHashSet<Type> _types = new LinkedHashSet<Type>();
  
  public SessionBeanImpl(AbstractSessionContext<X,T> context,
                         ManagedBeanImpl<X> bean,
                         Set<Type> apiList,
                         AnnotatedType<X> extAnnType)
  {
    _context = context;
    _bean = bean;
    
    _types.addAll(apiList);
    
    introspectObservers(bean.getAnnotatedType(), extAnnType);
  }
  
  public InjectManager getCdiManager()
  {
    return _context.getInjectManager();
  }
  
  protected ManagedBeanImpl<X> getBean()
  {
    return _bean;
  }
  
  @Override
  public Set<Type> getTypes()
  {
    return _types;
  }

  @Override
  public T getScopeAdapter(Bean<?> topBean, CreationalContextImpl<T> context)
  {
    return null;
  }

  @Override
  public T create(CreationalContext<T> env)
  {
    T value;
    
    if (env instanceof CreationalContextImpl<?>)
      value = _context.createProxy((CreationalContextImpl<T>) env);
    else
      value = _context.createProxy(null);
    
    if (env != null)
      env.push(value);
    
    return value;
  }
  
  @Override
  public void destroy(T instance, CreationalContext<T> cxt)
  {
    CreationalContextImpl<T> env;
    
    if (cxt instanceof CreationalContextImpl<?>)
      env = (CreationalContextImpl<T>) cxt;
    else
      env = null;
    
    _context.destroyProxy(instance, env);
    
    if (env != null)
      env.release();
  }

  /**
   * Returns the injection points.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    // ejb/1210, ioc/05al
    return getBean().getInjectionPoints();
    
    /*
    HashSet<InjectionPoint> injectionPoints = new HashSet<InjectionPoint>();
    
    return injectionPoints;
    */
  }

  @Override
  public Class<?> getBeanClass()
  {
    return getBean().getBeanClass();
  }

  @Override
  public String getName()
  {
    return getBean().getName();
  }

  @Override
  public Set<Annotation> getQualifiers()
  {
    return getBean().getQualifiers();
  }

  @Override
  public Class<? extends Annotation> getScope()
  {
    return getBean().getScope();
  }

  @Override
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return getBean().getStereotypes();
  }

  @Override
  public boolean isAlternative()
  {
    return getBean().isAlternative();
  }

  @Override
  public boolean isNullable()
  {
    return false;
  }

  @Override
  public String getId()
  {
    return getBean().getId();
  }
  
  /**
   * Introspects the methods for any @Produces
   */
  private void introspectObservers(AnnotatedType<X> beanType,
                                   AnnotatedType<X> extAnnType)
  {
    EventManager eventManager = _context.getModuleInjectManager().getEventManager();

    for (AnnotatedMethod<? super X> beanMethod : beanType.getMethods()) {
      if (! beanMethod.getJavaMember().getDeclaringClass().equals(beanType.getJavaClass())
          && ! beanType.isAnnotationPresent(Specializes.class)) {
        continue;
      }
      
      AnnotatedMethod<? super X> apiMethod
        = AnnotatedTypeUtil.findMethod(extAnnType, beanMethod);
      
      if (apiMethod == null)
        apiMethod = beanMethod;
      else if (apiMethod instanceof AnnotatedMethodImpl<?>) {
        // ioc/0b0h
        AnnotatedMethodImpl<? super X> apiMethodImpl
          = (AnnotatedMethodImpl<? super X>) apiMethod;
        
        apiMethodImpl.addAnnotations(beanMethod.getAnnotations());
        
        for (int i = 0; i < apiMethod.getParameters().size(); i++) {
          AnnotatedParameterImpl<?> paramImpl
            = (AnnotatedParameterImpl<?>) apiMethod.getParameters().get(i);
          AnnotatedParameter<?> beanParam = beanMethod.getParameters().get(i);
          
          paramImpl.addAnnotations(beanParam.getAnnotations());
        }
      }
        
      
      int param = EventManager.findObserverAnnotation(apiMethod);
      
      if (param >= 0)
        eventManager.addObserver(this, apiMethod);
    }
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + getBeanClass().getSimpleName()
            + ", " + getQualifiers() + "]");
  }
}
