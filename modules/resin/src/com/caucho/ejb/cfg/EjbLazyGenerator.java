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

package com.caucho.ejb.cfg;

import java.util.ArrayList;

import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.java.gen.JavaClassGenerator;


/**
 * Configuration for an ejb entity bean.
 */
public class EjbLazyGenerator<X> {
  private AnnotatedType<X> _beanType;
  private JavaClassGenerator _javaGen;
  private ArrayList<AnnotatedType<? super X>> _localApi;
  private AnnotatedType<X> _localBean;
  private ArrayList<AnnotatedType<? super X>> _remoteApi;
  
  EjbLazyGenerator(AnnotatedType<X> beanType,
                   JavaClassGenerator javaGen,
                   ArrayList<AnnotatedType<? super X>> localApi,
                   AnnotatedType<X> localBean,
                   ArrayList<AnnotatedType<? super X>> remoteApi)
  {
    _beanType = beanType;
    _javaGen = javaGen;
    
    _localApi = localApi;
    _localBean = localBean;
    
    _remoteApi = remoteApi;
  }
  
  public AnnotatedType<X> getBeanType()
  {
    return _beanType;
  }
  
  public JavaClassGenerator getJavaClassGenerator()
  {
    return _javaGen;
  }
  
  public ArrayList<AnnotatedType<? super X>> getLocalApi()
  {
    return _localApi;
  }
  
  public AnnotatedType<X> getLocalBean()
  {
    return _localBean;
  }
  
  public ArrayList<AnnotatedType<? super X>> getRemoteApi()
  {
    return _remoteApi;
  }
}
