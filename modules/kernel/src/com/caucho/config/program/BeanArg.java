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

package com.caucho.config.program;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.InjectionPointImpl;
import com.caucho.config.inject.InjectManager.ReferenceFactory;
import com.caucho.inject.Module;

/**
 * Custom bean configured by namespace
 */
@Module
public class BeanArg<T> extends Arg<T> {
  private InjectManager _beanManager;
  private Type _type;
  private Annotation []_bindings;
  private ReferenceFactory<?> _factory;
  private InjectionPoint _ip;

  public BeanArg(InjectManager injectManager,
                 Type type, 
                 Annotation []bindings,
                 InjectionPoint ip)
  {
    _beanManager = injectManager;
    
    _type = type;
    _bindings = bindings;
    
    _ip = ip;
  }

  @Override
  public void bind()
  {
    if (_factory == null) {
      HashSet<Annotation> qualifiers = new HashSet<Annotation>();
      
      for (Annotation ann : _bindings) {
        qualifiers.add(ann);
      }
      
      _factory = (ReferenceFactory<T>) _beanManager.getReferenceFactory(_type, qualifiers, _ip);
    }
  }

  @Override
  public Object eval(CreationalContext<T> parentEnv)
  {
    if (_factory == null)
      bind();

    return _factory.create(null, (CreationalContextImpl<T>) parentEnv, _ip);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _factory + "]";
  }
}
