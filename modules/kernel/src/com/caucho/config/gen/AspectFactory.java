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
 * Creates an aspect generator for a method.
 */
@Module
public interface AspectFactory<X> {
  /**
   * Returns the owning AspectBeanFactory
   */
  public AspectBeanFactory<X> getAspectBeanFactory();
  
  /**
   * Returns the bean type.
   */
  public AnnotatedType<X> getBeanType();

  public boolean isEnhanced();
  
  /**
   * Returns an aspect for the method if one exists.
   */
  public AspectGenerator<X> create(AnnotatedMethod<? super X> method,
                                   boolean isEnhanced);

  public void generateInject(JavaWriter out, HashMap<String, Object> map) 
    throws IOException;

  public void generatePostConstruct(JavaWriter out, HashMap<String, Object> map) 
    throws IOException;

  public void generatePreDestroy(JavaWriter out, HashMap<String, Object> map) 
    throws IOException;

  public void generateEpilogue(JavaWriter out, HashMap<String, Object> map) 
    throws IOException;
}