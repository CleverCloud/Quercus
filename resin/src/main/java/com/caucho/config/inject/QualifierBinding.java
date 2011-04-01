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

package com.caucho.config.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.util.Nonbinding;

import com.caucho.config.ConfigException;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * Introspected annotation binding
 */
@Module
public class QualifierBinding {
  private static final L10N L = new L10N(QualifierBinding.class);
  private static final Logger log = Logger.getLogger(QualifierBinding.class.getName());
  private static final Class<?> []NULL_ARG = new Class[0];

  private Annotation _ann;
  private Class<? extends Annotation> _annType;

  private ArrayList<Method> _methodList
    = new ArrayList<Method>();

  public QualifierBinding(Annotation ann)
  {
    _ann = ann;
    _annType = ann.annotationType();

    validateQualifier(_annType, _methodList);
  }
  
  public static void validateQualifier(Class<?> cl,
                                       ArrayList<Method> methodList)
  {
    for (Method method : cl.getMethods()) {
      if (method.getName().equals("annotationType"))
        continue;
      else if (method.isAnnotationPresent(Nonbinding.class))
        continue;
      else if (method.getParameterTypes().length > 0)
        continue;
      else if (Object.class.equals(method.getDeclaringClass()))
        continue;
      else if (Annotation.class.equals(method.getDeclaringClass()))
        continue;
      
      Class<?> type = method.getReturnType();
      
      if (type.isArray())
        throw new ConfigException(L.l("@{0} is an invalid qualifier because its member '{1}' has an array value and is missing @Nonbinding",
                                      cl.getSimpleName(), method.getName()));
      if (Annotation.class.isAssignableFrom(type))
        throw new ConfigException(L.l("@{0} is an invalid qualifier because its member '{1}' has an annotation value and is missing @Nonbinding",
                                      cl.getSimpleName(), method.getName()));
      
      method.setAccessible(true);

      if (methodList != null)
        methodList.add(method);
    }
  }

  public boolean isAny()
  {
    return _annType == Any.class;
  }

  public boolean isMatch(Annotation []annList)
  {
    for (Annotation ann : annList) {
      if (isMatch(ann)) {
        return true;
      }
    }

    return false;
  }

  boolean isMatch(Annotation ann)
  {
    Class<?> annType = ann.annotationType();

    /*
    // ioc/0b05
    if (_annType == Any.class)
      return true;
      */

    if (! _annType.equals(annType)) {
      return false;
    }

    for (int i = 0; i < _methodList.size(); i++) {
      Method method = _methodList.get(i);

      try {
        Object a = method.invoke(_ann);

        Object b;

        if (method.getDeclaringClass().isAssignableFrom(ann.getClass()))
          b = method.invoke(ann);
        else {
          Method bMethod = null;

          try {
            bMethod =
              ann.getClass().getMethod(method.getName(), NULL_ARG);
          } catch (NoSuchMethodException e) {
            log.log(Level.FINEST, e.toString(), e);
          }

          if (bMethod != null) {
            bMethod.setAccessible(true);
            b = bMethod.invoke(ann);
          }
          else
            b = method.getDefaultValue();
        }
        
        if (a == b)
          continue;
        else if (a == null)
          return false;
        else if (! a.equals(b))
          return false;
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);

        return false;
      }
    }

    return true;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _ann + "]";
  }
}
