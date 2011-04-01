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

import com.caucho.inject.Module;

/**
 * Stack of partially constructed beans.
 */
@Module
public class OwnerCreationalContext<T> extends CreationalContextImpl<T> {
  private DependentCreationalContext<?> _next;
  
  public OwnerCreationalContext(Contextual<T> bean)
  {
    super(bean, null);
  }
  
  public OwnerCreationalContext(Contextual<T> bean, 
                                CreationalContextImpl<?> parent)
  {
    super(bean, parent);
  }

  @Override
  protected boolean isTop()
  {
    return true;
  }
  
  @Override
  protected OwnerCreationalContext<T> getOwner()
  {
    return this;
  }
  
  @Override
  protected DependentCreationalContext<?> getNext()
  {
    return _next;
  }
  
  protected void setNext(DependentCreationalContext<?> dep)
  {
    _next = dep;
  }
}
