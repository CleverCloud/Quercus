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

package com.caucho.config.xml;

import java.lang.reflect.Constructor;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.PassivationCapable;

import com.caucho.config.inject.BeanWrapper;
import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.inject.ScopeAdapterBean;
import com.caucho.config.program.Arg;
import com.caucho.config.program.ConfigProgram;
import com.caucho.inject.Module;

/**
 * Internal implementation for a Bean
 */
@Module
public class XmlBean<X> extends BeanWrapper<X>
  implements InjectionTarget<X>, ScopeAdapterBean<X>, PassivationCapable
{
  private ManagedBeanImpl<X> _bean;
  private XmlInjectionTarget<X> _injectionTarget;

  private ClassLoader _loader = Thread.currentThread().getContextClassLoader();

  public XmlBean(ManagedBeanImpl<X> bean,
                 XmlInjectionTarget<X> injectionTarget)
  {
    super(bean.getBeanManager(), bean);
    
    _bean = bean;
    _injectionTarget = injectionTarget;
  }

  public ManagedBeanImpl<X> getBean()
  {
    return _bean;
  }

  @Override
  public Annotated getAnnotated()
  {
    return _bean.getAnnotated();
  }

  @Override
  public AnnotatedType<X> getAnnotatedType()
  {
    return _bean.getAnnotatedType();
  }

  @Override
  public InjectionTarget<X> getInjectionTarget()
  {
    return this;
  }

  @Override
  public X getScopeAdapter(Bean<?> topBean, CreationalContextImpl<X> context)
  {
    Bean<X> bean = getBean();

    if (bean instanceof ScopeAdapterBean<?>)
      return ((ScopeAdapterBean<X>) bean).getScopeAdapter(topBean, context);
    else
      return null;
  }

  @Override
  public X create(CreationalContext<X> env)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_loader);

      X instance = produce(env);
      env.push(instance);
      
      inject(instance, env);
      postConstruct(instance);

      return instance;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  @Override
  public X produce(CreationalContext<X> context)
  {
    return _injectionTarget.produce(context);
  }

  @Override
  public void inject(X instance, CreationalContext<X> env)
  {
    _injectionTarget.inject(instance, env);
  }

  @Override
  public void postConstruct(X instance)
  {
    _injectionTarget.postConstruct(instance);
  }

  /**
   * Call destroy
   */
  public void preDestroy(X instance)
  {
    _injectionTarget.preDestroy(instance);
  }

  /**
   * Call destroy
   */
  public void dispose(X instance)
  {
    _injectionTarget.dispose(instance);
  }

  /**
   * Returns the injection points.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    return getBean().getInjectionPoints();
  }
}
