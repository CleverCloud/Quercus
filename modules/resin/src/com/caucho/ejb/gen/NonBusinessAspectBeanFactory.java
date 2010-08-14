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
 * @author Emil Ong
 */

package com.caucho.ejb.gen;

import java.lang.reflect.Method;
import java.util.HashMap;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.gen.AspectBeanFactory;
import com.caucho.config.gen.AspectFactory;
import com.caucho.config.gen.AspectGenerator;
import com.caucho.config.gen.SecurityFactory;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Factory for creating non-business methods that are package accessible.
 */
@Module
public class NonBusinessAspectBeanFactory<X> implements AspectBeanFactory<X>
{
  private final AnnotatedType<X> _beanType;

  public NonBusinessAspectBeanFactory(AnnotatedType<X> beanType)
  {
    _beanType = beanType;
  }
  
  @Override
  public AnnotatedType<X> getBeanType()
  {
    return _beanType;
  }
  
  /**
   * Returns the head aspect factory
   */
  @Override
  public AspectFactory<X> getHeadAspectFactory()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the generated bean name
   */
  @Override
  public String getGeneratedClassName()
  {
    return getBeanType().getJavaClass().getName();
  }

  /**
   * Returns the generated bean name
   */
  @Override
  public String getInstanceClassName()
  {
    return getGeneratedClassName();
  }
  
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
    return "bean";
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
  public AspectGenerator<X> create(AnnotatedMethod<? super X> method)
  {
    return new NonBusinessMethodGenerator<X>(method);
  }

  @Override
  public void generateInject(JavaWriter out, HashMap<String, Object> hashMap)
  {
  }

  @Override
  public void generatePostConstruct(JavaWriter out, HashMap<String, Object> hashMap)
  {
  }

  @Override
  public void generatePreDestroy(JavaWriter out, HashMap<String, Object> hashMap)
  {
  }
  
  @Override
  public void generateEpilogue(JavaWriter out, HashMap<String, Object> hashMap)
  {
  }

  @Override
  public boolean isEnhanced()
  {
    return false;
  }
}
