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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import com.caucho.inject.Module;

/**
 * Marker bean for @Delegate injection
 */
@Module
public class DelegateProxyBean implements Bean<Object>
{
  public static final DelegateProxyBean BEAN = new DelegateProxyBean();

  @Override
  public Class<?> getBeanClass()
  {
    return null;
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return null;
  }

  @Override
  public String getName()
  {
    return null;
  }

  @Override
  public Set<Annotation> getQualifiers()
  {
    return null;
  }

  @Override
  public Class<? extends Annotation> getScope()
  {
    return null;
  }

  @Override
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return null;
  }

  @Override
  public Set<Type> getTypes()
  {
    return null;
  }

  @Override
  public boolean isAlternative()
  {
    return false;
  }

  @Override
  public boolean isNullable()
  {
    return false;
  }

  @Override
  public Object create(CreationalContext<Object> creationalContext)
  {
    return null;
  }

  @Override
  public void destroy(Object instance,
                      CreationalContext<Object> creationalContext)
  {
  }
}
