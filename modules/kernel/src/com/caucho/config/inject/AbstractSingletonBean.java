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

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.PassivationCapable;

/**
 * SingletonBean represents a singleton instance exported as a web beans.
 *
 * <code><pre>
 * @Current Manager manager;
 *
 * manager.addBean(new SingletonBean(myValue));
 * </pre></code>
 */
abstract public class AbstractSingletonBean<T> extends BeanWrapper<T>
  implements Closeable, AnnotatedBean, PassivationCapable
{
  private ManagedBeanImpl<T> _managedBean;

  private Set<Type> _types;
  private Annotated _annotated;
  private Set<Annotation> _bindings;
  private Set<Class<? extends Annotation>> _stereotypes;
  private Class<? extends Annotation> _scopeType;
  private String _name;

  private String _passivationId;

  AbstractSingletonBean(ManagedBeanImpl<T> managedBean,
                        Set<Type> types,
                        Annotated annotated,
                        Set<Annotation> bindings,
                        Set<Class<? extends Annotation>> stereotypes,
                        Class<? extends Annotation> scopeType,
                        String name)
  {
    super(managedBean.getBeanManager(), managedBean);

    _managedBean = managedBean;

    _types = types;
    _annotated = annotated;
    _bindings = bindings;
    _stereotypes = stereotypes;
    _scopeType = scopeType;
    _name = name;

    // ioc/0e13
    _managedBean.setPassivationId(getId());
  }

  //
  // metadata for the bean
  //

  @Override
  public Annotated getAnnotated()
  {
    if (_annotated != null)
      return _annotated;
    else
      return _managedBean.getAnnotated();
  }

  @Override
  public AnnotatedType<T> getAnnotatedType()
  {
    if (_annotated instanceof AnnotatedType<?>)
      return (AnnotatedType<T>) _annotated;
    else
      return _managedBean.getAnnotatedType();
  }

  @Override
  public Set<Annotation> getQualifiers()
  {
    if (_bindings != null)
      return _bindings;
    else
      return super.getQualifiers();
  }
  
  @Override
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    if (_stereotypes != null)
      return _stereotypes;
    else
      return getBean().getStereotypes();
  }

  @Override
  public String getName()
  {
    if (_name != null)
      return _name;
    else
      return getBean().getName();
  }

  /**
   * Return passivation id
   */
  @Override
  public String getId()
  {
    if (_passivationId == null)
      _passivationId = calculatePassivationId();

    return _passivationId;
  }

  /**
   * Returns the bean's scope type.
   */
  @Override
  public Class<? extends Annotation> getScope()
  {
    if (_scopeType != null)
      return _scopeType;
    else
      return getBean().getScope();
  }

  /**
   * Returns the types that the bean exports for bindings.
   */
  @Override
  public Set<Type> getTypes()
  {
    if (_types != null)
      return _types;
    else
      return getBean().getTypes();
  }

  @Override
  abstract public T create(CreationalContext<T> env);


  /**
   * Frees the singleton on environment shutdown
   */
  @Override
  public void close()
  {
  }
}
