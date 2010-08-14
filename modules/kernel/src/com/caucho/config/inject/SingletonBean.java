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
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * SingletonBean represents a singleton instance exported as a web beans.
 *
 * <code><pre>
 * @Current Manager manager;
 *
 * manager.addBean(new SingletonBean(myValue));
 * </pre></code>
 */
public class SingletonBean<T> extends AbstractSingletonBean<T>
  implements Closeable
{
  private static final Set<InjectionPoint> EMPTY_INJECTION_POINTS
    = new HashSet<InjectionPoint>();
  
  private T _value;

  SingletonBean(ManagedBeanImpl<T> managedBean,
                Set<Type> types,
                Annotated annotated,
                Set<Annotation> bindings,
                Set<Class<? extends Annotation>> stereotypes,
                Class<? extends Annotation> scopeType,
                String name,
                T value)
  {
    super(managedBean, types, annotated, bindings,
          stereotypes, scopeType, name);

    _value = value;

    if (value instanceof HandleAware) {
      HandleAware handleAware = (HandleAware) value;
      String id = getId();

      if (id != null)
        handleAware.setSerializationHandle(new SingletonHandle(id));
    }
  }

  @Override
  public T create(CreationalContext<T> env)
  {
    return _value;
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return EMPTY_INJECTION_POINTS;
  }
  /**
   * Frees the singleton on environment shutdown
   */
  @Override
  public void close()
  {
    if (_value != null) {
      CreationalContext<T> env = null;

      destroy(_value, env);
    }
  }
}
