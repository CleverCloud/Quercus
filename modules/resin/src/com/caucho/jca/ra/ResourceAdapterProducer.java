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

package com.caucho.jca.ra;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.inject.Module;

/**
 * Controller for a resource-adapter
 */
@Module
public class ResourceAdapterProducer<X> implements InjectionTarget<X>
{
  private ResourceAdapterController _controller;

  public ResourceAdapterProducer(ResourceAdapterController controller)
  {
    _controller = controller;
  }

  @SuppressWarnings("unchecked")
  @Override
  public X produce(CreationalContext<X> ctx)
  {
    return (X) _controller.getResourceAdapter();
  }

  @Override
  public void inject(X instance, CreationalContext<X> ctx)
  {
  }

  @Override
  public void postConstruct(X instance)
  {
  }

  @Override
  public void preDestroy(X instance)
  {
  }

  @Override
  public void dispose(X instance)
  {
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return new HashSet<InjectionPoint>();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _controller + "]";
  }
}
