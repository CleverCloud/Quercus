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

import javax.ejb.Remove;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.inject.Module;

/**
 * Aspect factory for generating @Asynchronous aspects.
 */
@Module
public class XaFactory<X>
  extends AbstractAspectFactory<X>
{
  private TransactionAttributeType _classXa;
  private TransactionManagementType _classXaManagement;
  
  public XaFactory(AspectBeanFactory<X> beanFactory,
                   AspectFactory<X> next)
  {
    super(beanFactory, next);
    
    AnnotatedType<X> beanType = beanFactory.getBeanType();
    TransactionAttribute xa = beanType.getAnnotation(TransactionAttribute.class);
    
    TransactionManagement xaManagement
      = beanType.getAnnotation(TransactionManagement.class);
    
    if (xa != null)
      _classXa = xa.value();
    
    if (xaManagement != null)
      _classXaManagement = xaManagement.value();
  }
  
  /**
   * Creates an aspect for interception if the method should be intercepted.
   */
  @Override
  public AspectGenerator<X> create(AnnotatedMethod<? super X> method,
                                   boolean isEnhanced)
  {
    // ejb/2101 - XXX: should be part of EjbXaFactory?
    if (method.isAnnotationPresent(Remove.class))
      return super.create(method, isEnhanced);
    
    AnnotatedType<?> declType = method.getDeclaringType();
    
    TransactionManagement xaManagement
      = method.getAnnotation(TransactionManagement.class);
    
    TransactionManagementType xaManagementType = _classXaManagement;
    
    if (xaManagement != null)
      xaManagementType = xaManagement.value();

    TransactionAttribute xa = method.getAnnotation(TransactionAttribute.class);
    TransactionAttributeType xaType = _classXa;
    
    if (xa != null) {
      xaType = xa.value();
    }
    else if (declType != null) {
      xa = declType.getAnnotation(TransactionAttribute.class);
      
      if (xa != null)
        xaType = xa.value();
    }
    
    boolean isBeanManaged = xaManagementType == TransactionManagementType.BEAN;

    if (isBeanManaged)
      xaType = TransactionAttributeType.NOT_SUPPORTED;
      
    
    if (xaType != null) {
      isEnhanced = true;
      
      AspectGenerator<X> next = super.create(method, isEnhanced);
      
      return new XaGenerator<X>(this, method, next, xaType, isBeanManaged);
    }
    
    return super.create(method, isEnhanced);
  }
}
