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

package com.caucho.config.gen;

import javax.ejb.AccessTimeout;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.inject.Module;

/**
 * Aspect factory for generating @Asynchronous aspects.
 */
@Module
public class LockFactory<X>
  extends AbstractAspectFactory<X>
{
  private LockType _classLockType;
  private AccessTimeout _classAccessTimeout; 
  
  public LockFactory(AspectBeanFactory<X> beanFactory,
                     AspectFactory<X> next)
  {
    super(beanFactory, next);
    
    Lock lock = beanFactory.getBeanType().getAnnotation(Lock.class);
    
    if (lock != null)
      _classLockType = lock.value();
    
    _classAccessTimeout = beanFactory.getBeanType().getAnnotation(
                    AccessTimeout.class);
  }
  
  /**
   * Creates an aspect for interception if the method should be intercepted.
   */
  @SuppressWarnings("unchecked")
@Override
  public AspectGenerator<X> create(AnnotatedMethod<? super X> method,
                                   boolean isEnhanced)
  {
    Lock lock = method.getAnnotation(Lock.class);
    
    LockType lockType = _classLockType;
    
    if (lock != null)
      lockType = lock.value();
    
    AccessTimeout accessTimeout = method.getAnnotation(AccessTimeout.class);
    
    if (accessTimeout == null) {
            accessTimeout = _classAccessTimeout;
    }
    
    if (lockType == null)
      return super.create(method, isEnhanced);
    else {
      AspectGenerator<X> next = super.create(method, true);
    
      if (accessTimeout != null) {
        return new LockGenerator(this, method, next, lockType, accessTimeout.value(), accessTimeout.unit());
      } else {
              return new LockGenerator(this, method, next, lockType, -1, null);
      }
    }
  }
}
