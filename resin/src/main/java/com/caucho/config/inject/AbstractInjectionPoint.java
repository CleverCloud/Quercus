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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.*;

import com.caucho.config.inject.InjectManager.ReferenceFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.Member;
import java.util.Set;

/**
 */
public class AbstractInjectionPoint implements InjectionPoint
{
  private Type _type;
  private Member _member;
  private Bean _bean;
  private Set<Annotation> _bindings;
  private Annotation []_annotations;

  public AbstractInjectionPoint(InjectManager inject,
                                Bean bean,
                                Member member,
                                Type type,
                                Set<Annotation> bindings,
                                Annotation []annotations)
  {
    _bean = bean;
    _member = member;
    _type = type;
    _bindings = bindings;
    _annotations = annotations;
  }

  public Annotated getAnnotated()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Set<Annotation> getQualifiers()
  {
    return _bindings;
  }

  public Type getType()
  {
    return _type;
  }

  public Bean<?> getBean()
  {
    return _bean;
  }

  public Member getMember()
  {
    return _member;
  }

  public Annotation []getAnnotations()
  {
    return _annotations;
  }

  public <T extends Annotation> T getAnnotation(Class<T> annotationType)
  {
    for (Annotation ann : getAnnotations()) {
      if (ann.annotationType().equals(annotationType))
        return (T) ann;
    }

    return null;
  }

  public boolean isAnnotationPresent(Class<? extends Annotation> annType)
  {
    for (Annotation ann : getAnnotations()) {
      if (ann.annotationType().equals(annType))
        return true;
    }

    return false;
  }

  public boolean isDelegate()
  {
    return false;
  }

  public boolean isTransient()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _type + "," + _bindings + "]";
  }
}
