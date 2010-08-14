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
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

class ESPropertyDescriptor extends PropertyDescriptor {
  String name;
  Field field;
  ESMethodDescriptor getter;
  ESMethodDescriptor setter;

  public String getName() { return name; }
  public Field getESField() { return field; }
  public ESMethodDescriptor getESReadMethod() { return getter; }
  public ESMethodDescriptor getESWriteMethod() { return setter; }

  public ESPropertyDescriptor(String propertyName, Class beanClass)
    throws IntrospectionException
  {
    super(propertyName, null, null);
    this.name = propertyName;
  }

  public ESPropertyDescriptor(String propertyName, Field field,
                              ESMethodDescriptor getter,
                              ESMethodDescriptor setter)
    throws IntrospectionException
  {
    super(propertyName, null, null);
    this.name = propertyName;

    this.field = field;
    this.getter = getter;
    this.setter = setter;
  }
}


