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

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.config.program.ConfigProgram;

/**
 * Adds behavior for an injection target.
 */
public class InjectionTargetFilter<T> implements InjectionTarget<T>,
                                                 PassivationSetter
{
  private InjectionTarget<T> _next;
  private ConfigProgram _init;

  public InjectionTargetFilter(InjectionTarget<T> next, ConfigProgram init)
  {
    _next = next;
    _init = init;
  }

  public T produce(CreationalContext<T> ctx)
  {
    return _next.produce(ctx);
  }

  public Set<InjectionPoint> getInjectionPoints()
  {
    return _next.getInjectionPoints();
  }

  public void setPassivationId(String id)
  {
    if (_next instanceof PassivationSetter)
      ((PassivationSetter) _next).setPassivationId(id);
  }

  public void inject(T instance, CreationalContext<T> ctx)
  {
    _next.inject(instance, ctx);

    if (_init != null)
      _init.inject(instance, ctx);
  }

  public void postConstruct(T instance)
  {
    _next.postConstruct(instance);
  }

  public void preDestroy(T instance)
  {
    _next.preDestroy(instance);
  }

  public void dispose(T instance)
  {
    _next.dispose(instance);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _next + "," + _init + "]";
  }
}
