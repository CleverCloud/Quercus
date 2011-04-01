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
import javax.enterprise.inject.spi.ProcessBean;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;

@Module
public class BeforeBeanDiscoveryImpl implements BeforeBeanDiscovery
{
  private InjectManager _cdiManager;
  
  BeforeBeanDiscoveryImpl(InjectManager cdiManager)
  {
    _cdiManager = cdiManager;
  }
  
  @Override
  public void addAnnotatedType(AnnotatedType<?> annType)
  {
    // _cdiManager.addAnnotatedType(annType);
  }
  
  @Override
  public void addQualifier(Class<? extends Annotation> qualifier)
  {
    _cdiManager.addQualifier(qualifier);
  }

  @Override
  public void addScope(Class<? extends Annotation> scopeType,
                       boolean isNormal,
                       boolean isPassivating)
  {
    _cdiManager.addScope(scopeType, isNormal, isPassivating);
  }

  @Override
  public void addStereotype(Class<? extends Annotation> stereotype,
                            Annotation... stereotypeDef)
  {
    _cdiManager.addStereotype(stereotype, stereotypeDef);
  }

  @Override
  public void addInterceptorBinding(Class<? extends Annotation> bindingType,
                                    Annotation... bindings)
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cdiManager + "]";
  }
}
