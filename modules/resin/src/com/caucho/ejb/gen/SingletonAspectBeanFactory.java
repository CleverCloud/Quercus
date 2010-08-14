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

package com.caucho.ejb.gen;

import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.gen.AspectFactory;
import com.caucho.config.gen.AsynchronousFactory;
import com.caucho.config.gen.CandiAspectBeanFactory;
import com.caucho.config.gen.InterceptorFactory;
import com.caucho.config.gen.LockFactory;
import com.caucho.config.gen.MethodTailFactory;
import com.caucho.config.gen.SecurityFactory;
import com.caucho.config.gen.XaFactory;
import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;

/**
 * Factory for creating stateful business methods.
 */
@Module
public class SingletonAspectBeanFactory<X> extends CandiAspectBeanFactory<X>
{
  public SingletonAspectBeanFactory(InjectManager manager,
                                    AnnotatedType<X> beanType)
  {
    super(manager, beanType);
  }
  
  /**
   * Returns the generated bean name
   */
  @Override
  public String getGeneratedClassName()
  {
    return getBeanType().getJavaClass().getName() + "__SingletonProxy";
  }
  
  /**
   * Returns the generated bean name
   */
  @Override
  public String getInstanceClassName()
  {
    return getBeanType().getJavaClass().getName();
  }
  
  /**
   * Returns true for proxy-style generation
   */
  @Override
  public boolean isProxy()
  {
    return true;
  }
  
  /**
   * Generates the underlying bean object
   */
  @Override
  public String getBeanInstance()
  {
    return "_bean";
  }
  
  @Override
  public String getBeanSuper()
  {
    return "_bean";
  }
  
  /**
   * Generates the proxy object.
   */
  @Override
  public String getBeanProxy()
  {
    return "this";
  }
  
  /**
   * Generates data associated with the bean
   */
  @Override
  public String getBeanInfo()
  {
    return "this";
  }
  
  @Override
  protected AspectFactory<X> createAspectFactory()
  {
    InjectManager manager = InjectManager.getCurrent();
    
    AspectFactory<X> next = new MethodTailFactory<X>(this);
    
    next = new InterceptorFactory<X>(this, next, manager);
    next = new XaFactory<X>(this, next);
    next = new LockFactory<X>(this, next);
    next = new AsynchronousFactory<X>(this, next);
    next = new SecurityFactory<X>(this, next);
    
    return new SingletonMethodHeadFactory<X>(this, next);
  }
}
