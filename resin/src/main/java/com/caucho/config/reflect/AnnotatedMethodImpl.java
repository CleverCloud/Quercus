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

package com.caucho.config.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.inject.Module;


/**
 * Abstract introspected view of a Bean
 */
@Module
public class AnnotatedMethodImpl<T>
  extends AnnotatedElementImpl implements AnnotatedMethod<T>
{
  private AnnotatedType<T> _declaringType;
  
  private Method _method;

  private List<AnnotatedParameter<T>> _parameterList;
  
  public AnnotatedMethodImpl(Method method)
  {
    this(null, null, method);
  }
  
  public AnnotatedMethodImpl(AnnotatedType<T> declaringType,
                             Annotated annotated,
                             Method method)
  {
    super(method.getGenericReturnType(), annotated, method.getAnnotations());

    _declaringType = declaringType;
    _method = method;
  }

  @Override
  public AnnotatedType<T> getDeclaringType()
  {
    return _declaringType;
  }
  
  /**
   * Returns the reflected Method
   */
  @Override
  public Method getJavaMember()
  {
    return _method;
  }

  /**
   * Returns the constructor parameters
   */
  @Override
  public List<AnnotatedParameter<T>> getParameters()
  {
    if (_parameterList == null)
      _parameterList = introspectParameters(_method);
    
    return _parameterList;
  }

  @Override
  public boolean isStatic()
  {
    return Modifier.isStatic(_method.getModifiers());
  }

  private List<AnnotatedParameter<T>> introspectParameters(Method method)
  {
    ArrayList<AnnotatedParameter<T>> parameterList
      = new ArrayList<AnnotatedParameter<T>>();
    
    Type []paramTypes = method.getGenericParameterTypes();
    Annotation [][]annTypes = method.getParameterAnnotations();
    
    for (int i = 0; i < paramTypes.length; i++) {
      AnnotatedParameterImpl<T> param
        = new AnnotatedParameterImpl<T>(this, paramTypes[i], annTypes[i], i);
    
      parameterList.add(param);
    }
    
    return parameterList;
  }

  public static boolean isMatch(Method methodA, Method methodB)
  {
    if (! methodA.getName().equals(methodB.getName())) {
      return false;
    }
    
    // ejb/4018 
    if (! methodA.getDeclaringClass().equals(methodB.getDeclaringClass())) {
      return false;
    }

    Class<?> []paramA = methodA.getParameterTypes();
    Class<?> []paramB = methodB.getParameterTypes();

    if (paramA.length != paramB.length)
      return false;

    for (int i = 0; i < paramA.length; i++) {
      if (! paramA[i].equals(paramB[i]))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode()
  {
    return _method.hashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    else if (! (obj instanceof AnnotatedMethodImpl<?>))
      return false;

    AnnotatedMethodImpl<?> method = (AnnotatedMethodImpl<?>) obj;

    return isMatch(_method, method._method);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _method + "]";
  }
}
