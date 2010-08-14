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
 * @author Emil Ong
 */

package com.caucho.jaxb.accessor;

import javax.xml.bind.JAXBException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class GetterSetterAccessor extends Accessor {
  private final Method _get;
  private final Method _set;
  private final Class _type;
  private final Type _genericType;
  private final Package _package;

  public GetterSetterAccessor(String name, Method get, Method set)
    throws JAXBException
  {
    Class declarer = get.getDeclaringClass();

    _package = declarer.getPackage();
    _get = get;
    _set = set;
    _name = name;
    _type = _get.getReturnType();
    _genericType = _get.getGenericReturnType();

    if ("clazz".equals(_name))
      _name = "class";
  }

  public Object get(Object o)
    throws JAXBException
  {
    try {
      if (_get == null)
        return null;

      return _get.invoke(o);
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  public void set(Object o, Object value)
    throws JAXBException
  {
    try {
      if (_set == null)
        return;

      _set.invoke(o, value);
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  public <A extends Annotation> A getAnnotation(Class<A> c)
  {
    A a = null;

    if (_get != null)
      a = _get.getAnnotation(c);

    if (a == null && _set != null)
      a = _set.getAnnotation(c);

    return a;
  }

  public <A extends Annotation> A getPackageAnnotation(Class<A> c)
  {
    return _package.getAnnotation(c);
  }

  public Package getPackage()
  {
    return _package;
  }

  public Class getType()
  {
    return _get.getReturnType();
  }

  public Type getGenericType()
  {
    return _get.getGenericReturnType();
  }

  public String getName()
  {
    return _name;
  }

  public String toString()
  {
    return "GetterSetterAccessor[" + _get.getDeclaringClass().getName() + "." +
                                     getName() + "]";
  }
}
