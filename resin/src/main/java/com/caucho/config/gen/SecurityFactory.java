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

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.inject.Module;

/**
 * Aspect factory for generating @Asynchronous aspects.
 */
@Module
public class SecurityFactory<X>
  extends AbstractAspectFactory<X>
{
  private final RunAs _classRunAs;
  private final RolesAllowed _classRolesAllowed;
  private final PermitAll _classPermitAll;
  private final DenyAll _classDenyAll;
  
  public SecurityFactory(AspectBeanFactory<X> beanFactory,
                         AspectFactory<X> next)
  {
    super(beanFactory, next);
    
    _classRunAs = beanFactory.getBeanType().getAnnotation(RunAs.class);
    _classRolesAllowed = beanFactory.getBeanType().getAnnotation(RolesAllowed.class);
    _classPermitAll = beanFactory.getBeanType().getAnnotation(PermitAll.class);
    _classDenyAll = beanFactory.getBeanType().getAnnotation(DenyAll.class);
  }
  
  /**
   * Creates an aspect for interception if the method should be intercepted.
   */
  @Override
  public AspectGenerator<X> create(AnnotatedMethod<? super X> method,
                                   boolean isEnhanced)
  {
    RunAs runAs = method.getAnnotation(RunAs.class);
      
    if (runAs == null)
      runAs = _classRunAs;
    
    String runAsName = null;
      
    if (runAs != null)
      runAsName = runAs.value();
      
    RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
    
    if (rolesAllowed == null)
      rolesAllowed = _classRolesAllowed;

    String []roleNames = null;

    if (rolesAllowed != null)
      roleNames = rolesAllowed.value();

    PermitAll permitAll = method.getAnnotation(PermitAll.class);

    if (permitAll != null || _classPermitAll != null)
      roleNames = null;
      
    DenyAll denyAll = method.getAnnotation(DenyAll.class);

    if (denyAll != null || _classDenyAll != null)
      roleNames = new String[0];
    
    if (roleNames != null || runAs != null) {
      AspectGenerator<X> next = super.create(method, true);

      return new SecurityGenerator<X>(this, method, next, roleNames, runAsName);
    }
    else
      return super.create(method, isEnhanced);
  }
}
