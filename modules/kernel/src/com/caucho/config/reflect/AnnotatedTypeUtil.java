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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.inject.Module;


/**
 * Abstract introspected view of a Bean
 */
@Module
public class AnnotatedTypeUtil {
  /**
   * Finds any method matching the method name and parameter types.
   */
  public static <X> AnnotatedMethod<? super X>
  findMethod(AnnotatedType<X> type, AnnotatedMethod<?> method)
  {
    Method javaMethod = method.getJavaMember();
    
    return findMethod(type, 
                      javaMethod.getName(),
                      javaMethod.getParameterTypes());
  }

  /**
   * Finds any method matching the method name and parameter types.
   */
  public static <X> AnnotatedMethod<? super X>
  findMethod(AnnotatedType<X> type, String methodName, Class<?> []param)
  {
    for (AnnotatedMethod<? super X> annMethod : type.getMethods()) {
      Method method = annMethod.getJavaMember();
      
      if (! method.getName().equals(methodName))
        continue;
      
      if (isMatch(param, method.getParameterTypes()))
        return annMethod;
    }
    
    return null;
  }

  /**
   * Finds any method matching the method name and parameter types.
   */
  public static <X> AnnotatedMethod<? super X>
  findMethod(Set<AnnotatedMethod<? super X>> methods, Method method)
  {
    return findMethod(methods, method.getName(), method.getParameterTypes());
  }
  
  /**
   * Finds any method matching the method name and parameter types.
   */
  public static <X> AnnotatedMethod<? super X>
  findMethod(Set<AnnotatedMethod<? super X>> methods, String methodName, Class<?> []param)
  {
    for (AnnotatedMethod<? super X> annMethod : methods) {
      Method method = annMethod.getJavaMember();
      
      if (! method.getName().equals(methodName))
        continue;
      
      if (isMatch(param, method.getParameterTypes()))
        return annMethod;
    }
    
    return null;
  }
  
  /**
   * Finds any method matching the method name and parameter types.
   */
  public static Method findDeclaredMethod(Class<?> cl, Method testMethod)
  {
    if (cl == null)
      return null;
    
    for (Method method : cl.getDeclaredMethods()) {
      if (isMatch(method, testMethod))
        return method;
    }
    
    return null;
  }
  
  
  /**
   * Finds any method matching the method name and parameter types.
   */
  public static Method findMethod(Method []methods, Method testMethod)
  {
    for (Method method : methods) {
      if (isMatch(method, testMethod))
        return method;
    }
    
    return null;
  }
  
  /**
   * Finds any method matching the method name and parameter types.
   */
  public static Method findMethod(Collection<Method> methods, Method testMethod)
  {
    for (Method method : methods) {
      if (isMatch(method, testMethod))
        return method;
    }
    
    return null;
  }

  /**
   * Tests if two annotated methods are equivalent.
   */
  public static boolean isMatch(AnnotatedMethod<?> methodA,
                                AnnotatedMethod<?> methodB)
  {
    if (methodA == methodB)
      return true;
    else if (methodA == null || methodB == null)
      return false;
    else {
      return isMatch(methodA.getJavaMember(), methodB.getJavaMember());
    }
  }

  /**
   * Tests if an annotated method matches a name and parameter types.
   */
  public static boolean isMatch(AnnotatedMethod<?> method, 
                                String name, Class<?> []param)
  {
    Method javaMethod = method.getJavaMember();
    
    if (! javaMethod.getName().equals(name))
      return false;

    Class<?> []mparam = javaMethod.getParameterTypes();

    return isMatch(mparam, param);
  }

  /**
   * Tests if an annotated method matches a name and parameter types.
   */
  public static boolean isMatch(Method javaMethod,
                                String name, Class<?> []param)
  {
    if (! javaMethod.getName().equals(name))
      return false;

    Class<?> []mparam = javaMethod.getParameterTypes();

    return isMatch(mparam, param);
  }

  /**
   * Tests if an annotated method matches a name and parameter types.
   */
  public static boolean isMatch(Method methodA, Method methodB)
  {
    if (! methodA.getName().equals(methodB.getName()))
      return false;

    return isMatch(methodA.getParameterTypes(),
                   methodB.getParameterTypes());
  }
  
  /**
   * Tests if parameters match a method's parameter types.
   */
  public static boolean isMatch(Class<?> []paramA, Class<?> []paramB)
  {
    if (paramA.length != paramB.length)
      return false;
    
    for (int i = paramA.length - 1; i >= 0; i--) {
      if (! paramA[i].equals(paramB[i]))
        return false;
    }
    
    return true;
  }

  /**
   * Tests if a method throws a checked exception.
   */
  public static boolean hasException(AnnotatedMethod<?> method, Class<?> exn)
  {
    Class<?> []methodExceptions = method.getJavaMember().getExceptionTypes();

    for (int j = 0; j < methodExceptions.length; j++) {
      if (methodExceptions[j].isAssignableFrom(exn))
        return true;
    }

    return false;
  }

}
