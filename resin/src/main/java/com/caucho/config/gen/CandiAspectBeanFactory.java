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

import java.io.IOException;
import java.util.HashMap;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Manages aspect factories for a bean.
 */
@Module
public class CandiAspectBeanFactory<X> implements AspectBeanFactory<X> {
  private InjectManager _manager;
  
  private AnnotatedType<X> _beanType;
  private AspectFactory<X> _factory;
  
  public CandiAspectBeanFactory(InjectManager manager,
                                AnnotatedType<X> beanType)
  {
    _manager = manager;
    
    _beanType = beanType;
    _factory = createAspectFactory();
  }
  
  /**
   * Returns the bean type.
   */
  @Override
  public AnnotatedType<X> getBeanType()
  {
    return _beanType;
  }
  
  /**
   * Returns the generated bean name
   */
  @Override
  public String getGeneratedClassName()
  {
    return getBeanType().getJavaClass().getSimpleName() + "__ResinWebBean";
  }

  @Override
  public String getInstanceClassName()
  {
    return getGeneratedClassName();
  }
  /**
   * Returns the head aspect factory
   */
  @Override
  public AspectFactory<X> getHeadAspectFactory()
  {
    return _factory;
  }
  
  /**
   * Returns true for a proxied instance
   */
  @Override
  public boolean isProxy()
  {
    return false;
  }
  
  /**
   * Returns the bean instance Java reference.
   */
  @Override
  public String getBeanInstance()
  {
    return "this";
  }
  
  /**
   * Returns the bean super reference.
   */
  @Override
  public String getBeanSuper()
  {
    return "super";
  }
  
  /**
   * Returns the proxy Java reference.
   */
  @Override
  public String getBeanProxy()
  {
    return "this";
  }
  
  /**
   * Returns the beanInfo variable for shared bean instance information
   */
  @Override
  public String getBeanInfo()
  {
    return "this";
  }
  
  /**
   * Creates a new aspect for a method.
   */
  @Override
  public AspectGenerator<X> create(AnnotatedMethod<? super X> method)
  {
    return _factory.create(method, false); 
  }
  
  protected AspectFactory<X> createAspectFactory()
  {
    InjectManager manager = _manager;
    
    AspectFactory<X> next = new MethodTailFactory<X>(this);
    
    next = new InterceptorFactory<X>(this, next, manager);
    next = new XaFactory<X>(this, next);
    next = new LockFactory<X>(this, next);
    next = new AsynchronousFactory<X>(this, next);
    next = new SecurityFactory<X>(this, next);
    
    return new MethodHeadFactory<X>(this, next);
  }

  @Override
  public boolean isEnhanced()
  {
    if (_factory != null)
      return _factory.isEnhanced();
    else
      return false;
  }

  @Override
  public void generateInject(JavaWriter out, HashMap<String, Object> map) 
    throws IOException
  {
    if (_factory != null)
      _factory.generateInject(out, map);
  }

  @Override
  public void generatePostConstruct(JavaWriter out, HashMap<String, Object> map) 
    throws IOException
  {
    if (_factory != null)
      _factory.generatePostConstruct(out, map);
  }

  @Override
  public void generatePreDestroy(JavaWriter out, HashMap<String, Object> map) 
    throws IOException
  {
    if (_factory != null)
      _factory.generatePreDestroy(out, map);
  }

  @Override
  public void generateEpilogue(JavaWriter out, HashMap<String, Object> map) 
    throws IOException
  {
    if (_factory != null)
      _factory.generateEpilogue(out, map);
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _beanType + "]"; 
  }
}