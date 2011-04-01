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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.reflect.AnnotatedElementImpl;

/**
 * SingletonBean represents a singleton instance exported as a web beans.
 *
 * <code><pre>
 * @Current Manager manager;
 *
 * manager.addBean(new SingletonBean(myValue));
 * </pre></code>
 */
public class BeanBuilder<T>
{
  private ManagedBeanImpl<T> _managedBean;

  private Set<Type> _types;
  private AnnotatedElementImpl _annotated;
  private Set<Annotation> _bindings;
  private Set<Class<? extends Annotation>> _stereotypes;
  private String _name;
  private Class<? extends Annotation> _scopeType;

  private InjectionTarget<T> _injectionTarget;
  private ContainerProgram _init;

  public BeanBuilder(ManagedBeanImpl<T> managedBean)
  {
    _managedBean = managedBean;
    _injectionTarget = managedBean.getInjectionTarget();
  }

  public AnnotatedType<T> getAnnotatedType()
  {
    return _managedBean.getAnnotatedType();
  }

  public Annotated getExtendedAnnotated()
  {
    return _annotated;
  }

  public BeanBuilder<T> name(String name)
  {
    _name = name;

    return this;
  }

  public BeanBuilder<T> qualifier(Annotation ann)
  {
    if (_bindings == null)
      _bindings = new LinkedHashSet<Annotation>();

    _bindings.add(ann);

    return this;
  }

  public BeanBuilder<T> binding(Collection<Annotation> list)
  {
    if (_bindings == null)
      _bindings = new LinkedHashSet<Annotation>();

    _bindings.addAll(list);

    return this;
  }

  public BeanBuilder<T> stereotype(Class<? extends Annotation> annType)
  {
    if (_stereotypes == null)
      _stereotypes = new LinkedHashSet<Class<? extends Annotation>>();

    _stereotypes.add(annType);

    return this;
  }

  public BeanBuilder<T> stereotype(Collection<Class<? extends Annotation>> list)
  {
    if (_stereotypes == null)
      _stereotypes = new LinkedHashSet<Class<? extends Annotation>>();

    _stereotypes.addAll(list);

    return this;
  }

  public BeanBuilder<T> annotation(Annotation ann)
  {
    if (_annotated == null)
      _annotated = new AnnotatedElementImpl(_managedBean.getAnnotated());

    _annotated.addAnnotation(ann);

    return this;
  }

  public BeanBuilder<T> annotation(Collection<Annotation> list)
  {
    if (_annotated == null)
      _annotated = new AnnotatedElementImpl(_managedBean.getAnnotated());

    for (Annotation ann : list) {
      _annotated.addAnnotation(ann);
    }

    return this;
  }

  public BeanBuilder<T> scope(Class<? extends Annotation> scopeType)
  {
    _scopeType = scopeType;

    return this;
  }

  public BeanBuilder<T> type(Type ...types)
  {
    if (_types == null)
      _types = new LinkedHashSet<Type>();

    if (types != null) {
      for (Type type : types) {
        _types.add(type);
      }
    }

    return this;
  }

  public BeanBuilder<T> type(Set<Type> types)
  {
    if (_types == null)
      _types = new LinkedHashSet<Type>();

    if (types != null) {
      _types.addAll(types);
    }

    return this;
  }

  public BeanBuilder<T> init(ConfigProgram init)
  {
    if (init != null) {
      if (_init == null) {
        _init = new ContainerProgram();
        _injectionTarget = new InjectionTargetFilter<T>(_injectionTarget, _init);
      }

      _init.addProgram(init);
    }

    return this;
  }

  @SuppressWarnings("unchecked")
  public Bean<T> singleton(Object value)
  {
    return new SingletonBean<T>(_managedBean,
                               _types,
                               _annotated,
                               _bindings,
                               _stereotypes,
                               _scopeType,
                               _name,
                               (T) value);
  }

  public Bean<T> injection(InjectionTarget<T> injection)
  {
    return new InjectionBean<T>(_managedBean,
                               _types,
                               _annotated,
                               _bindings,
                               _stereotypes,
                               _scopeType,
                               _name,
                               injection);
  }

  public Bean<T> bean()
  {
    return new InjectionBean<T>(_managedBean,
                               _types,
                               _annotated,
                               _bindings,
                               _stereotypes,
                               _scopeType,
                               _name,
                               _injectionTarget);
  }
  
  public String toString()
  {
    return (getClass().getSimpleName() + "[" + _managedBean.getBeanClass()
            + _bindings + ", " + _name + "]");
  }
}
