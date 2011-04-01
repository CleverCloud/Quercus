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
 * @author Scott Ferguson;
 */

package com.caucho.config.event;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Qualifier;

import com.caucho.inject.Module;
import com.caucho.util.L10N;

@Module
@SuppressWarnings("serial")
public class EventImpl<T> implements Event<T>, Serializable
{
  private static final L10N L = new L10N(EventImpl.class);
  
  private final BeanManager _manager;
  private final Type _type;
  private final Annotation []_bindings;

  public EventImpl(BeanManager manager,
                   Type type,
                   Annotation []bindings)
  {
    _manager = manager;
    _type = type;
    _bindings = bindings;
  }

  @Override
  public void fire(T event)
  {
    _manager.fireEvent(event, _bindings);
  }

  @Override
  public Event<T> select(Annotation... bindings)
  {
    if (bindings == null)
      return this;
    
    validateBindings(bindings);

    // ioc/0b54 - union would cause problems with @Current
    return new EventImpl<T>(_manager, _type, bindings);
  }

  @Override
  public <U extends T> Event<U> select(Class<U> subtype,
                                       Annotation... bindings)
  {
    validateType(subtype);
    validateBindings(bindings);

    return new EventImpl<U>(_manager, subtype, bindings);
  }

  @Override
  public <U extends T> Event<U> select(TypeLiteral<U> subtype,
                                       Annotation... bindings)
  {
    validateType(subtype.getType());
    validateBindings(bindings);

    return new EventImpl<U>(_manager, subtype.getType(), bindings);
  }
  
  private void validateType(Type type)
  {
    if (type instanceof Class<?>) {
      Class<?> cl = (Class<?>) type; 

      if (cl.getTypeParameters().length > 0)
        throw new IllegalArgumentException(L.l("Generic class '{0}' is not allowed in select.",
                                               type));
    }
    else if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;
      
      for (Type param : pType.getActualTypeArguments()) {
        if (param instanceof TypeVariable<?>) {
          throw new IllegalArgumentException(L.l("Generic class '{0}' is not allowed in select.",
                                                 type));
          
        }
      }
    }
  }
  
  private void validateBindings(Annotation ...bindings)
  {
    if (bindings == null)
      return;
    
    for (int i = 0; i < bindings.length; i++) {
      Class<? extends Annotation> annType = bindings[i].annotationType();
      
      if (! annType.isAnnotationPresent(Qualifier.class))
        throw new IllegalArgumentException(L.l("select with non-@Qualifier annotation '{0}' is not allowed.",
                                               bindings[i]));
      
      for (int j = i + 1; j < bindings.length; j++) {
        if (annType.equals(bindings[j].annotationType())) {
          throw new IllegalArgumentException(L.l("select with duplicate Qualifier '{0}' is not allowed.",
                                                 bindings[i]));
        }
      }
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _type + "]";
  }
}
