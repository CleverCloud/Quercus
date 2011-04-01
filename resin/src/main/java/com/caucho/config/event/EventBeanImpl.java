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

package com.caucho.config.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;

/**
 * Internal implementation for a Bean
 */
@Module
public class EventBeanImpl<T> implements Bean<T>
{
  private InjectManager _beanManager;
  private Type _type;
  private HashSet<Annotation> _qualifiers;

  private EventImpl<T> _event;

  public EventBeanImpl(InjectManager beanManager,
                Type type,
                HashSet<Annotation> qualifierSet)
  {
    _beanManager = beanManager;
    _type = type;
    _qualifiers = qualifierSet;
    
    Annotation []qualifiers = new Annotation[qualifierSet.size()];
    
    int i = 0;
    for (Annotation ann : qualifierSet)
      qualifiers[i++] = ann;

    _event = new EventImpl(_beanManager, _type, qualifiers);
  }

  @Override
  public Class<?> getBeanClass()
  {
    return Event.class;
  }

  @Override
  public T create(CreationalContext<T> env)
  {
    return (T) _event;
  }

  @Override
  public void destroy(T instance, CreationalContext<T> env)
  {
  }

  //
  // metadata for the bean
  //

  /**
   * Returns the bean's binding annotations.
   */
  @Override
  public Set<Annotation> getQualifiers()
  {
    LinkedHashSet<Annotation> qualifiers = new LinkedHashSet<Annotation>();
    
    for (Annotation ann : _qualifiers)
      qualifiers.add(ann);
    
    return qualifiers;
  }

  /**
   * Returns the bean's stereotype annotations.
   */
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return null;
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
  public Class<? extends Annotation> getScope()
  {
    return Dependent.class;
  }

  /**
   * Returns the types that the bean exports for bindings.
   */
  public Set<Type> getTypes()
  {
    LinkedHashSet<Type> typeSet = new LinkedHashSet<Type>();
    
    typeSet.add(_type);
    typeSet.add(Object.class);
    
    return typeSet;
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _type + "]"; 
  }
}
