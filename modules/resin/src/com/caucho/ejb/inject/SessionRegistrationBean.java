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

package com.caucho.ejb.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import com.caucho.config.inject.BeanAdapter;
import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.InjectEnvironmentBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.j2ee.BeanName;
import com.caucho.ejb.session.AbstractSessionContext;
import com.caucho.inject.Module;

/**
 * Internal registration for the EJB.
 */
@Module
public class SessionRegistrationBean<X,T> extends BeanAdapter<X,T>
  implements InjectEnvironmentBean
{
  private AbstractSessionContext<X,T> _context;
  private Set<Annotation> _qualifierSet;
  private Set<Type> _types;
  
  public SessionRegistrationBean(InjectManager beanManager,
                                 AbstractSessionContext<X,T> context,
                                 Bean<X> bean,
                                 BeanName beanName)
  {
    super(beanManager, bean);
    
    _context = context;
    
    _qualifierSet = new HashSet<Annotation>();
    _qualifierSet.add(beanName);
  }
 
  public InjectManager getCdiManager()
  {
    return _context.getInjectManager();
  }
  
  /**
   * The registration bean is not registered by name
   */
  @Override
  public String getName()
  {
    return null;
  }
  
  @Override
  public Set<Annotation> getQualifiers()
  {
    return _qualifierSet;
  }

  @Override
  public T create(CreationalContext<T> env)
  {
    if (env instanceof CreationalContextImpl<?>)
      return _context.createProxy((CreationalContextImpl<T>) env);
    else
      return _context.createProxy(null);
  }

  @Override
  public void destroy(T instance, CreationalContext<T> cxt)
  {
    CreationalContextImpl<T> env;
    
    if (cxt instanceof CreationalContextImpl<?>)
      env = (CreationalContextImpl<T>) cxt;
    else
      env = null;
    
   _context.destroyProxy(instance, env);
  }

  @Override
  public Set<Type> getTypes()
  {
    if (_types == null) {
      _types = new HashSet<Type>();
      
      // ejb/2018
      _types.add(_context.getApi());
    }
    
    return _types;
  }
}
