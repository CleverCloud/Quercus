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

import com.caucho.util.L10N;

import javax.xml.bind.JAXBException;
import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

public class ArrayComponentAccessor extends Accessor {
  public static final L10N L = new L10N(ArrayComponentAccessor.class);

  private Accessor _accessor;
  private Class _type;
  private Type _genericType;

  public ArrayComponentAccessor(Accessor a) 
  {
    _accessor = a;

    _type = _accessor.getType().getComponentType();

    if (a.getGenericType() instanceof GenericArrayType) {
      GenericArrayType arrayType = (GenericArrayType) a.getGenericType();
      _genericType = arrayType.getGenericComponentType();
    }
    else {
      _genericType = _type;
    }
  }

  public Object get(Object o) throws JAXBException
  {
    throw new JAXBException(L.l("cannot invoke ArrayComponentAccessor.get()"));
  }

  public void set(Object o, Object value) throws JAXBException
  {
    throw new JAXBException(L.l("cannot invoke ArrayComponentAccessor.set()"));
  }

  public String getName()
  {
    return _accessor.getName();
  }

  public Class getType()
  {
    return _type;
  }

  public Type getGenericType()
  {
    return _genericType;
  }

  public <A extends Annotation> A getAnnotation(Class<A> c)
  {
    return null;
  }

  public <A extends Annotation> A getPackageAnnotation(Class<A> c)
  {
    return null;
  }

  public Package getPackage()
  {
    return null;
  }
}
