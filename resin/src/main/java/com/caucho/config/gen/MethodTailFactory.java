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

import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Represents a filter for invoking a method
 */
@Module
public class MethodTailFactory<X> implements AspectFactory<X> {
  private AspectBeanFactory<X> _beanFactory;
  
  public MethodTailFactory(AspectBeanFactory<X> beanFactory)
  {
    _beanFactory = beanFactory;
  }
  
  /**
   * Returns the bean factory
   */
  @Override
  public AspectBeanFactory<X> getAspectBeanFactory()
  {
    return _beanFactory;
  }
  
  /**
   * Returns the owning bean type.
   */
  @Override
  public AnnotatedType<X> getBeanType()
  {
    return getAspectBeanFactory().getBeanType();
  }
  
  @Override
  public boolean isEnhanced()
  {
    return false;
  }
  
  /**
   * Returns an aspect for the method if one exists.
   */
  @Override
  public AspectGenerator<X> create(AnnotatedMethod<? super X> method,
                                   boolean isEnhanced)
  {
    if (isEnhanced)
      return new MethodTailGenerator<X>(this, method);
    else
      return null;
  }
  
  @Override
  public void generateInject(JavaWriter out, HashMap<String, Object> map)
  {
  }
  
  @Override
  public void generatePostConstruct(JavaWriter out, HashMap<String, Object> map)
    throws IOException
  {
    out.println("try {");
    out.println("  __caucho_postConstructImpl();");
    out.println("} catch (Exception e) {");
    out.println("  com.caucho.config.ConfigException.create(e);");
    out.println("}");
  }
  
  @Override
  public void generatePreDestroy(JavaWriter out, HashMap<String, Object> map)
  {
  }
  
  @Override
  public void generateEpilogue(JavaWriter out, HashMap<String, Object> map)
  {
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
