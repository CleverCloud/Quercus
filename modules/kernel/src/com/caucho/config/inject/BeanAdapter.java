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
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import com.caucho.inject.Module;

/**
 * Adapter from an internal bean to an external exposed bean as used by
 * session beans.
 */
@Module
abstract public class BeanAdapter<X,T> extends AbstractBean<T>
{
  private final Bean<X> _bean;

  public BeanAdapter(InjectManager manager, Bean<X> bean)
  {
    super(manager);

    _bean = bean;
  }

  protected Bean<X> getBean()
  {
    return _bean;
  }

  //
  // from javax.enterprise.inject.InjectionTarget
  //

  @Override
  abstract public T create(CreationalContext<T> env);

  @Override
  abstract public void destroy(T instance, CreationalContext<T> env);

  //
  // metadata for the bean
  //

  @Override
  public Annotated getAnnotated()
  {
    Bean<X> bean = getBean();

    if (bean instanceof AnnotatedBean)
      return ((AnnotatedBean) bean).getAnnotated();
    else
      return null;
  }

  @Override
  public Set<Annotation> getQualifiers()
  {
    return getBean().getQualifiers();
  }

  @Override
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return getBean().getStereotypes();
  }
  
  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return getBean().getInjectionPoints();
  }

  @Override
  public String getName()
  {
    return getBean().getName();
  }

  @Override
  public boolean isAlternative()
  {
    return getBean().isAlternative();
  }

  /**
   * Returns true if the bean can be null
   */
  @Override
  public boolean isNullable()
  {
    return getBean().isNullable();
  }

  /**
   * Returns the bean's scope type.
   */
  @Override
  public Class<? extends Annotation> getScope()
  {
    return getBean().getScope();
  }

  /**
   * Returns the types that the bean exports for bindings.
   */
  @Override
  public Set<Type> getTypes()
  {
    return getBean().getTypes();
  }

  @Override
  public Class<?> getBeanClass()
  {
    return getBean().getBeanClass();
  }
}
