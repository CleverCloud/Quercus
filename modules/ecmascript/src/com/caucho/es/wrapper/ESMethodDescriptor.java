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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.es.wrapper;

import java.beans.IntrospectionException;
import java.beans.MethodDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Describes a method from a JavaScript perspective.
 */
public class ESMethodDescriptor extends MethodDescriptor {
  String name;
  boolean overwrite;
  boolean staticVirtual;
  Class declaringClass;
  String classJVMName;
  Class []parameterTypes;

  /**
   * Create a new method descriptor.
   *
   * @param method the underlying java method.
   * @param overwrite true if this method should overwrite the standard one.
   * @param staticVirtual true if this is a "static-virtual" method.
   */
  public ESMethodDescriptor(Method method, boolean overwrite,
                            boolean staticVirtual)
    throws IntrospectionException
  {
    super(method);

    this.name = method.getName();
    if (this.name == null)
      throw new RuntimeException();
    this.overwrite = overwrite;
    this.staticVirtual = staticVirtual;
  }

  public ESMethodDescriptor(ESMethodDescriptor md)
    throws IntrospectionException
  {
    super(md.getMethod());

    this.name = md.getName();
    this.overwrite = md.overwrite;
    this.staticVirtual = md.staticVirtual;
    this.declaringClass = md.declaringClass;
    this.parameterTypes = md.parameterTypes;
    this.classJVMName = md.classJVMName;
  }

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  /**
   * True if this overwrites the standard method.
   */
  boolean isOverwrite()
  {
    return overwrite;
  }

  public boolean isStaticVirtual()
  {
    return staticVirtual;
  }

  /**
   * True if this is a static method.
   */
  public boolean isStatic()
  {
    return ! staticVirtual && Modifier.isStatic(getMethod().getModifiers());
  }

  /**
   * True if this method should replace md.
   */
  public boolean overwrites(ESMethodDescriptor md)
  {
    if (! isStatic() && md.isStatic())
      return true;
    else if (isOverwrite() && ! md.isOverwrite())
      return true;
    else
      return false;
  }

  /**
   * Returns the method's parameter types.
   */
  public Class []getParameterTypes()
  {
    if (parameterTypes != null)
      return parameterTypes;

    if (! isStaticVirtual())
      parameterTypes = getMethod().getParameterTypes();
    else {
      Class []realParam = getMethod().getParameterTypes();

      parameterTypes = new Class[realParam.length - 1];
      
      for (int i = 0; i < parameterTypes.length; i++)
        parameterTypes[i] = realParam[i + 1];
    }

    return parameterTypes;
  }
  
  /**
   * Returns the declaring class for the method.
   */
  public Class getDeclaringClass()
  {
    if (declaringClass == null) {
      if (staticVirtual)
        declaringClass = getMethod().getParameterTypes()[0];
      else
        declaringClass = getMethod().getDeclaringClass();
    }

    return declaringClass;
  }

  /**
   * Returns the return type of the method.
   */
  public Class getReturnType()
  {
    return getMethod().getReturnType();
  }

  /**
   * Returns the class name for the method, i.e. the class we should
   * cast to get the right method.
   */
  public String getMethodClassName()
  {
    return getMethod().getDeclaringClass().getName();
  }

  String getObjectClassName()
  {
    return getDeclaringClass().getName();
  }

  String getClassJVMName()
  {
    if (classJVMName == null)
      classJVMName = getDeclaringClass().getName().replace('.', '/');

    return classJVMName;
  }

  public String toString()
  {
    Class []param = getParameterTypes();

    if (param == null || param.length == 0)
      return ("[ESMethodDescriptor " + getDeclaringClass().getName() +
              "." + getName() + "]");
    else
      return ("[ESMethodDescriptor " + getDeclaringClass().getName() +
              "." + getName() + " " + param[0] + "]");
  }
}


