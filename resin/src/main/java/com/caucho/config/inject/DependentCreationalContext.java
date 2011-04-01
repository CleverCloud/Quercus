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
 */

package com.caucho.config.inject;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.InjectionPoint;

import com.caucho.inject.Module;

/**
 * Stack of partially constructed beans.
 */
@Module
public class DependentCreationalContext<T> extends CreationalContextImpl<T> {
  private OwnerCreationalContext<?> _owner;
  private InjectionPoint _injectionPoint;
  private DependentCreationalContext<?> _next;
  
  public DependentCreationalContext(Contextual<T> bean,
                                    CreationalContextImpl<?> parent,
                                    InjectionPoint injectionPoint)
  {
    super(bean, parent);
    
    _owner = parent.getOwner();
    _injectionPoint = injectionPoint;
    
    _next = _owner.getNext();
    _owner.setNext(this);
  }

  @Override
  public boolean isTop()
  {
    return false;
  }
  
  @Override
  public OwnerCreationalContext<?> getOwner()
  {
    return _owner;
  }
  
  @Override
  public DependentCreationalContext<?> getNext()
  {
    return _next;
  }
  
  @Override
  public InjectionPoint getInjectionPoint()
  {
    return _injectionPoint;
  }
  
  @Override
  public void setInjectionPoint(InjectionPoint injectionPoint)
  {
    _injectionPoint = injectionPoint;
  }
}
