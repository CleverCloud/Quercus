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
import java.lang.reflect.Modifier;

class NamedPropertyDescriptor extends ESPropertyDescriptor {
  ESMethodDescriptor namedGetter;
  ESMethodDescriptor namedSetter;
  ESMethodDescriptor namedRemover;
  ESMethodDescriptor namedIterator;

  public ESMethodDescriptor getNamedReadMethod() { return namedGetter; }
  public ESMethodDescriptor getNamedWriteMethod() { return namedSetter; }
  public ESMethodDescriptor getNamedRemoveMethod() { return namedRemover; }
  public ESMethodDescriptor getNamedIteratorMethod() { return namedIterator; }

  public boolean isStatic()
  {
    if (namedGetter != null) {
      int modifiers = namedGetter.getMethod().getModifiers();
      return Modifier.isStatic(modifiers);
    }
    else if (namedSetter != null) {
      int modifiers = namedSetter.getMethod().getModifiers();
      return Modifier.isStatic(modifiers);
    } else
      return false;
  }

  public boolean isStaticVirtual()
  {
    if (namedGetter != null) {
      return namedGetter.isStaticVirtual();
    }
    else if (namedSetter != null) {
      return namedSetter.isStaticVirtual();
    } else
      return false;
  }

  public NamedPropertyDescriptor(String propertyName, Class beanClass)
    throws IntrospectionException
  {
    super(propertyName, null, null, null);
  }

  public NamedPropertyDescriptor(String propertyName,
                                 ESMethodDescriptor getter,
                                 ESMethodDescriptor setter,
                                 ESMethodDescriptor namedGetter,
                                 ESMethodDescriptor namedSetter,
                                 ESMethodDescriptor namedRemover,
                                 ESMethodDescriptor namedIterator)
    throws IntrospectionException
  {
    super(propertyName, null, getter, setter);

    this.namedGetter = namedGetter;
    this.namedSetter = namedSetter;
    this.namedRemover = namedRemover;
    this.namedIterator = namedIterator;
  }
}


