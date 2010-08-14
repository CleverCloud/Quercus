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
 * Manages aspect factories for a bean.
 */
@Module
public interface AspectBeanFactory<X> {
  /**
   * Returns the bean type.
   */
  public AnnotatedType<X> getBeanType();
  
  /**
   * Returns the head aspect factory
   */
  public AspectFactory<X> getHeadAspectFactory();

  /**
   * Returns the generated bean name.
   */
  public String getGeneratedClassName();

  /**
   * Returns the instance class name.
   */
  public String getInstanceClassName();
  
  /**
   * Returns true for a proxied instance
   */
  public boolean isProxy();
  
  /**
   * Returns the bean instance Java reference.
   */
  public String getBeanInstance();
  
  /**
   * Returns the bean instance call.
   */
  public String getBeanSuper();
  
  /**
   * Returns the proxy Java reference.
   */
  public String getBeanProxy();
  
  /**
   * Returns the beanInfo variable for shared bean instance information
   */
  public String getBeanInfo();
  
  /**
   * Creates an aspect generator.
   */
  public AspectGenerator<X> create(AnnotatedMethod<? super X> method);

  /**
   * Generates final closing information.
   */
  public void generateInject(JavaWriter out, 
                             HashMap<String, Object> hashMap)
    throws IOException;

  /**
   * Generates final initialization information.
   */
  public void generatePostConstruct(JavaWriter out, 
                                    HashMap<String, Object> hashMap)
    throws IOException;

  /**
   * Generates final closing information.
   */
  public void generatePreDestroy(JavaWriter out, 
                                 HashMap<String, Object> hashMap)
    throws IOException;

  /**
   * Generates final closing information.
   */
  public void generateEpilogue(JavaWriter out, 
                               HashMap<String, Object> hashMap)
    throws IOException;

  /**
   * Returns true if the factory forces enhancement.
   */
  public boolean isEnhanced();
}