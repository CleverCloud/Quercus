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
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.inject.Module;

/**
 * SingletonBean represents a singleton instance exported as a web beans.
 *
 * <code><pre>
 * @Current Manager manager;
 *
 * manager.addBean(new SingletonBean(myValue));
 * </pre></code>
 */
@Module
public class InjectionBean<T> extends AbstractSingletonBean<T>
  implements Closeable
{
  private InjectionTarget<T> _target;

  InjectionBean(ManagedBeanImpl<T> managedBean,
                Set<Type> types,
                Annotated annotated,
                Set<Annotation> bindings,
                Set<Class<? extends Annotation>> stereotypes,
                Class<? extends Annotation> scopeType,
                String name,
                InjectionTarget<T> target)
  {
    super(managedBean, types, annotated, bindings, stereotypes,
          scopeType, name);

    _target = target;

    if (target instanceof PassivationSetter)
      ((PassivationSetter) target).setPassivationId(getId());
  }

  @Override
  public InjectionTarget<T> getInjectionTarget()
  {
    return _target;
  }

  @Override
  public T create(CreationalContext<T> cxt)
  {
    T value = _target.produce(cxt);
    cxt.push(value);
    _target.inject(value, cxt);
    _target.postConstruct(value);

    return value;
  }
}
