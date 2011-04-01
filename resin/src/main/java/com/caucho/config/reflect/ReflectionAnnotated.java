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
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;

/**
 * Annotated object based only on reflection.
 */
public class ReflectionAnnotated implements Annotated
{
  private static final LinkedHashSet<Annotation> _emptyAnnSet
    = new  LinkedHashSet<Annotation>();

  private static final Annotation []_emptyAnnArray = new Annotation[0];

  private Type _type;

  private Set<Type> _typeSet;

  private LinkedHashSet<Annotation> _annSet;

  private Annotation []_annArray;

  protected ReflectionAnnotated(Type type,
                                Set<Type> typeClosure,
                                Annotation []annList)
  {
    _type = type;
    _typeSet = typeClosure;

    if (annList != null && annList.length > 0) {
      _annSet = new LinkedHashSet<Annotation>();

      for (Annotation ann : annList) {
        _annSet.add(ann);
      }

      _annArray = new Annotation[_annSet.size()];
      _annSet.toArray(_annArray);
    }
    else {
      _annSet = _emptyAnnSet;
      _annArray = _emptyAnnArray;
    }
  }

  /**
   * Returns the base type of the annotated member.
   */
  public Type getBaseType()
  {
    return _type;
  }

  /**
   * Returns all the types implemented by the member.
   */
  public Set<Type> getTypeClosure()
  {
    return _typeSet;
  }

  /**
   * Returns the introspected annotations
   */
  public Set<Annotation> getAnnotations()
  {
    return _annSet;
  }

  /**
   * Returns the matching annotation
   */
  public <T extends Annotation> T getAnnotation(Class<T> annType)
  {
    for (Annotation ann : _annArray) {
      if (annType.equals(ann.annotationType()))
        return (T) ann;
    }

    return null;
  }

  protected void addAnnotation(Annotation ann)
  {
    if (_annSet == _emptyAnnSet)
      _annSet = new LinkedHashSet<Annotation>();

    _annSet.add(ann);
    _annArray = new Annotation[_annSet.size()];
    _annSet.toArray(_annArray);
  }

  /**
   * Returns true if the annotation is present)
   */
  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annType)
  {
    for (Annotation ann : _annArray) {
      if (annType.equals(ann.annotationType()))
        return true;
    }

    return false;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _type + "]";
  }
}
