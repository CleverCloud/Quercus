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

import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.inject.InjectManager;


/**
 * Represents the reflected type where only the top-level annotations are
 * introspected, i.e. the class itself, not the fields or methods.
 */
public class ReflectionSimpleAnnotatedType<X>
  extends ReflectionAnnotated
  implements AnnotatedType<X>
{
  private Class<X> _javaClass;

  private static Set _emptyConstructorSet
    = new LinkedHashSet<AnnotatedConstructor<?>>();

  private static Set _emptyFieldSet
    = new LinkedHashSet<AnnotatedField<?>>();

  private static Set _emptyMethodSet
    = new LinkedHashSet<AnnotatedMethod<?>>();
  
  public ReflectionSimpleAnnotatedType(InjectManager manager, BaseType type)
  {
    super(type.toType(),
          type.getTypeClosure(manager),
          type.getRawClass().getAnnotations());
    
    _javaClass = (Class<X>) type.getRawClass();
  }
  
  public Class<X> getJavaClass()
  {
    return _javaClass;
  }

  /**
   * Returns the abstract introspected constructors
   */
  public Set<AnnotatedConstructor<X>> getConstructors()
  {
    return _emptyConstructorSet;
  }

  /**
   * Returns the abstract introspected methods
   */
  public Set<AnnotatedMethod<? super X>> getMethods()
  {
    return _emptyMethodSet;
  }

  /**
   * Returns the abstract introspected fields
   */
  public Set<AnnotatedField<? super X>> getFields()
  {
    return _emptyFieldSet;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _javaClass + "]";
  }
}
