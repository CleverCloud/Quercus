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

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

import com.caucho.inject.Module;

/**
 * Configuration for the xml web bean component.
 */
@Module
public class InjectionPointBean implements Bean<InjectionPoint>
{
  private InjectionPoint _ij;
  
  public InjectionPointBean(BeanManager manager, InjectionPoint ij)
  {
    _ij = ij;
  }

  //
  // metadata for the bean
  //

  @Override
  public Class<InjectionPoint> getBeanClass()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the bean's binding annotations.
   */
  public Set<Annotation> getQualifiers()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the bean's stereotype annotations.
   */
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the bean's deployment type
   */
  public Class<? extends Annotation> getDeploymentType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the set of injection points, for validation.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the bean's name or null if the bean does not have a
   * primary name.
   */
  public String getName()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if the bean can be null
   */
  public boolean isNullable()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if the bean can be null
   */
  public boolean isAlternative()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if the bean is serializable
   */
  public boolean isPassivationCapable()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the bean's scope type.
   */
  public Class<? extends Annotation> getScope()
  {
    return Dependent.class;
  }

  /**
   * Returns the types that the bean exports for bindings.
   */
  public Set<Type> getTypes()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public InjectionPoint create(CreationalContext<InjectionPoint> env)
  {
    // ioc/0i3o
    if (env instanceof CreationalContextImpl<?>)
      return ((CreationalContextImpl<InjectionPoint>) env).findInjectionPoint();
    else
      return _ij;
  }
  /**
   * Instantiate the bean.
   */
  /*
  public T instantiate()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  /**
   * Inject the bean.
   */
  public void inject(InjectionPoint instance)
  {
  }

  /**
   * Call post-construct
   */
  public void postConstruct(InjectionPoint instance)
  {
  }

  /**
   * Call pre-destroy
   */
  public void preDestroy(InjectionPoint instance)
  {
  }

  @Override
  public void destroy(InjectionPoint instance, 
                      CreationalContext<InjectionPoint> env)
  {
  }
}
