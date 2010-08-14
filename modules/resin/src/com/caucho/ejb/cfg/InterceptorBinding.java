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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Rodrigo Westrupp
 */

package com.caucho.ejb.cfg;

import java.util.ArrayList;

/**
 * Configuration for interceptor-binding.
 */
public class InterceptorBinding {
  private String _ejbName;

  private boolean _isExcludeDefaultInterceptors;
  private boolean _isExcludeClassInterceptors;

  private InterceptorOrder _interceptorOrder;

  private ArrayList<Class<?>> _interceptors = new ArrayList<Class<?>>();

  public InterceptorBinding()
  {
  }

  public String getEjbName()
  {
    return _ejbName;
  }

  public InterceptorOrder getInterceptorOrder()
  {
    return _interceptorOrder;
  }

  public ArrayList<Class<?>> getInterceptors()
  {
    return _interceptors;
  }

  public boolean isExcludeDefaultInterceptors()
  {
    return _isExcludeDefaultInterceptors;
  }

  public void setEjbName(String ejbName)
  {
    _ejbName = ejbName;
  }

  public void setExcludeDefaultInterceptors(boolean b)
  {
    _isExcludeDefaultInterceptors = b;
  }
  
  public void setExcludeClassInterceptors(boolean value)
  {
    _isExcludeClassInterceptors = value;
  }

  public void setInterceptorOrder(InterceptorOrder interceptorOrder)
  {
    _interceptorOrder = interceptorOrder;
  }

  public void addInterceptorClass(Class<?> interceptorClass)
  {
    _interceptors.add(interceptorClass);
  }
  
  public void addMethod(EjbMethod method)
  {
    
  }
}
