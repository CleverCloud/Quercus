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
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

import com.caucho.config.reflect.AnnotatedElementImpl;
import com.caucho.inject.Module;

/**
 * Bean object for the general InjectionPoint. 
 */
@Module
public class InjectionPointStandardBean 
  implements Bean<InjectionPoint>, AnnotatedBean
{
  private Annotated _annotated;
  
  public InjectionPointStandardBean()
  {
    _annotated = new AnnotatedElementImpl(getClass(), null, 
                                          getClass().getAnnotations());
  }
  
  //
  // metadata for the bean
  //

  @Override
  public Class<InjectionPoint> getBeanClass()
  {
    return InjectionPoint.class;
  }

  /**
   * Returns the bean's binding annotations.
   */
  public Set<Annotation> getQualifiers()
  {
    HashSet<Annotation> set = new HashSet<Annotation>();
    
    set.add(DefaultLiteral.DEFAULT);
    set.add(AnyLiteral.ANY);
    
    return set;
  }

  /**
   * Returns the bean's stereotype annotations.
   */
  @Override
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return new HashSet<Class<? extends Annotation>>();
  }

  /**
   * Returns the set of injection points, for validation.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    return new HashSet<InjectionPoint>();
  }

  /**
   * Returns the bean's name or null if the bean does not have a
   * primary name.
   */
  public String getName()
  {
    return null;
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
    return true;
  }

  /**
   * Returns the bean's scope type.
   */
  @Override
  public Class<? extends Annotation> getScope()
  {
    return Dependent.class;
  }

  /**
   * Returns the types that the bean exports for bindings.
   */
  @Override
  public Set<Type> getTypes()
  {
    HashSet<Type> types = new HashSet<Type>();
    
    types.add(InjectionPoint.class);
    
    return types;
  }

  @Override
  public InjectionPoint create(CreationalContext<InjectionPoint> env)
  {
    // ioc/0i3o
    if (env instanceof CreationalContextImpl<?>)
      return ((CreationalContextImpl<InjectionPoint>) env).findInjectionPoint();
    else
      throw new IllegalStateException();
  }

  @Override
  public void destroy(InjectionPoint instance, 
                      CreationalContext<InjectionPoint> env)
  {
  }
  
  @Override
  public Annotated getAnnotated()
  {
    return _annotated;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
