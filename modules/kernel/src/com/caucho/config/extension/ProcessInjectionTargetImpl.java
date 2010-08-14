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

package com.caucho.config.extension;

import java.lang.annotation.Annotation;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionTarget;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;

@Module
public class ProcessInjectionTargetImpl<X> implements ProcessInjectionTarget<X>
{
  private InjectManager _cdiManager;
  
  private InjectionTarget<X> _target;
  private AnnotatedType<X> _type;

  ProcessInjectionTargetImpl(InjectManager cdiManager,
                             InjectionTarget<X> target,
                             AnnotatedType<X> type)
  {
    _cdiManager = cdiManager;
    _target = target;
    _type = type;
  }

  @Override
  public AnnotatedType<X> getAnnotatedType()
  {
    return _type;
  }

  @Override
  public InjectionTarget<X> getInjectionTarget()
  {
    return _target;
  }

  @Override
  public void setInjectionTarget(InjectionTarget<X> target)
  {
    _target = target;
  }

  @Override
  public void addDefinitionError(Throwable t)
  {
    _cdiManager.addDefinitionError(t);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _target + "]";
  }
}
