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

package com.caucho.config.types;

import com.caucho.config.type.*;
import java.util.*;
import java.lang.reflect.*;
import java.lang.annotation.*;

/**
 * Configuration for the xml web bean component.
 */
public class AnnotationConfig implements InvocationHandler {
  private static final Method EQUALS;
  private static final Method HASH_CODE;
  private static final Method TO_STRING;
  private static final Method ANNOTATION_TYPE;

  private AnnotationInterfaceType _configType;
  private Class<?> _annotationType;

  private HashMap<String,Object> _valueMap = new HashMap<String,Object>(8);

  public AnnotationConfig(Class<?> annotationType)
  {
    this((AnnotationInterfaceType) TypeFactory.getType(annotationType),
         annotationType);
  }

  public AnnotationConfig(AnnotationInterfaceType configType,
                          Class<?> annotationType)
  {
    _configType = configType;
    _annotationType = annotationType;
  }

  public ConfigType<?> getConfigType()
  {
    return _configType;
  }

  public void setAttribute(String name, Object value)
  {
    if ("#text".equals(name))
      name = "value";

    Object oldValue = _valueMap.get(name);

    if (oldValue != null
        && oldValue.getClass().isArray()
        && value != null
        && oldValue.getClass() == value.getClass()) {
      Object []oldArray = (Object []) oldValue;
      Object []valueArray = (Object []) value;
      Class<?> componentType = oldValue.getClass().getComponentType();
      value = Array.newInstance(componentType,
                                oldArray.length + valueArray.length);

      System.arraycopy(oldArray, 0, value, 0, oldArray.length);
      System.arraycopy(valueArray, 0,
                       value, oldArray.length, valueArray.length);
    }

    _valueMap.put(name, value);
  }

  public Object invoke(Object proxy, Method method, Object []args)
  {
    if (ANNOTATION_TYPE.equals(method)) {
      return _annotationType;
    }
    else if (TO_STRING.equals(method)) {
      return _configType.toString(_valueMap);
    }
    else if (EQUALS.equals(method)) {
      return equalsImpl(args[0]);
    }
    else if (HASH_CODE.equals(method)) {
      // XXX:
      return _annotationType.hashCode();
    }

    if (args == null || args.length == 0) {
      Object value = _valueMap.get(method.getName());

      if (value != null)
        return value;

      if (Annotation.class.isAssignableFrom(method.getDeclaringClass()))
        return method.getDefaultValue();
    }

    throw new IllegalArgumentException("AnnotationProxy: " + method.toString());
  }

  private boolean equalsImpl(Object o)
  {
      if (o == null)
        return false;
      else if (! _annotationType.equals(o.getClass())) {
        return false;
      }

      // XXX:

      return true;
  }

  public Annotation replace()
  {
    // ioc/0h20
    ClassLoader loader = _annotationType.getClassLoader();
    // Thread.currentThread().getContextClassLoader();

    return (Annotation) Proxy.newProxyInstance(loader,
                                               new Class[] { _annotationType },
                                               this);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _annotationType + "]";
  }

  static {
    Method equals = null;
    Method hashCode = null;
    Method toString = null;
    Method annotationType = null;

    try {
      equals = Object.class.getMethod("equals", new Class[] { Object.class });
      hashCode = Object.class.getMethod("hashCode", new Class[] {});
      toString = Object.class.getMethod("toString", new Class[] {});
      annotationType
        = Annotation.class.getMethod("annotationType", new Class[] {});
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }

    EQUALS = equals;
    HASH_CODE = hashCode;
    TO_STRING = toString;
    ANNOTATION_TYPE = annotationType;
  }
}
