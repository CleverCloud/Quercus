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
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.decorator.Decorator;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InterceptionType;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;

import com.caucho.config.ConfigException;
import com.caucho.config.reflect.AnnotatedTypeImpl;
import com.caucho.util.L10N;

/**
 * InterceptorBean represents a Java interceptor
 */
public class InterceptorBean<X> extends InterceptorRuntimeBean<X>
{
  private static final L10N L = new L10N(InterceptorBean.class);
  
  private Class<X> _type;

  private ManagedBeanImpl<X> _bean;

  private HashSet<Annotation> _qualifiers
    = new HashSet<Annotation>();

  public InterceptorBean(InjectManager beanManager,
                         Class<X> type)
  {
    super(null, type);
    
    _type = type;
    
    if (Modifier.isAbstract(type.getModifiers()))
      throw new IllegalStateException(type + " is an unexpected abstract type");

    AnnotatedType<X> annType = beanManager.createAnnotatedType(_type);
    AnnotatedTypeImpl<X> enhAnnType = AnnotatedTypeImpl.create(annType);
    
    enhAnnType.addAnnotation(new InterceptorLiteral());
    _bean = beanManager.createManagedBean(enhAnnType);

    init();
  }

  public InterceptorBean(Class<X> type)
  {
    this(InjectManager.create(), type);
  }

  @Override
  public Bean<X> getBean()
  {
    return this; // _bean;
  }
  
  //
  // metadata for the bean
  //

  /**
   * Returns the bean's bindings
   */
  @Override
  public Set<Annotation> getQualifiers()
  {
    return _bean.getQualifiers();
  }

  /**
   * Returns the bean's stereotypes
   */
  @Override
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return _bean.getStereotypes();
  }

  /**
   * Returns the bean's name or null if the bean does not have a
   * primary name.
   */
  public String getName()
  {
    return _bean.getName();
  }

  /**
   * Returns true if the bean can be null
   */
  public boolean isNullable()
  {
    return false;
  }

  /**
   * Returns true if the bean can be null
   */
  public boolean isAlternative()
  {
    return false;
  }

  /**
   * Returns true if the bean is serializable
   */
  public boolean isPassivationCapable()
  {
    return false;
  }

  /**
   * Returns the bean's scope
   */
  public Class<? extends Annotation> getScope()
  {
    return _bean.getScope();
  }

  /**
   * Returns the types that the bean implements
   */
  public Set<Type> getTypes()
  {
    return _bean.getTypes();
  }

  @Override
  public Class<?> getBeanClass()
  {
    return _bean.getBeanClass();
  }

  //
  // lifecycle
  //

  @Override
  public X create(CreationalContext<X> creationalContext)
  {
    return _bean.create(creationalContext);
  }

  /**
   * Destroys a bean instance
   */
  @Override
  public void destroy(X instance, CreationalContext<X> env)
  {
    // ioc/0558
    // _bean.destroy(instance, env);
  }

  //
  // interceptor
  //

  /**
   * Returns the bean's binding types
   */
  public Set<Annotation> getInterceptorBindings()
  {
    return _qualifiers;
  }

  /**
   * Returns the set of injection points, for validation.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _bean.getInjectionPoints();
  }

  //
  // introspection
  //

  public void init()
  {
    // _bean.init();

    introspect();
  }

  protected void introspect()
  {
    introspectQualifiers(_type.getAnnotations());

    // introspectMethods(_type);
    
    if (_type.isAnnotationPresent(Decorator.class))
      throw new ConfigException(L.l("@Interceptor {0} cannot have a @Decorator annotation",
                                    _type.getName()));
  }

  protected void introspectQualifiers(Annotation []annList)
  {
    for (Annotation ann : annList) {
      if (ann.annotationType().isAnnotationPresent(InterceptorBinding.class)) {
        _qualifiers.add(ann);
      }
    }
  }

  @Override
  public boolean equals(Object o)
  {
    if (! (o instanceof InterceptorBean))
      return false;

    InterceptorBean bean = (InterceptorBean) o;

    return _type.equals(bean._type);
  }

  @Override
  public int hashCode()
  {
    return _type.hashCode();
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(_type.getSimpleName());

    sb.append("]");

    return sb.toString();
  }
}
