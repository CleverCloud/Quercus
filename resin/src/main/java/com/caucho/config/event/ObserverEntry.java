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
 */

package com.caucho.config.event;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.inject.spi.ObserverMethod;

import com.caucho.config.inject.QualifierBinding;
import com.caucho.config.reflect.BaseType;

public class ObserverEntry<T> {
  private ObserverMethod<T> _observer;
  private BaseType _type;
  private QualifierBinding []_qualifiers;
  
  ObserverEntry(ObserverMethod<T> observer,
                BaseType type,
                Annotation []qualifiers)
  {
    _observer = observer;
    _type = type;
    
    _qualifiers = new QualifierBinding[qualifiers.length];
    for (int i = 0; i < qualifiers.length; i++) {
      _qualifiers[i] = new QualifierBinding(qualifiers[i]);
    }
  }

  ObserverMethod<T> getObserver()
  {
    return _observer;
  }
  
  BaseType getType()
  {
    return _type;
  }
  
  void notify(T event)
  {
    _observer.notify(event);
  }
  
  void fireEvent(T event, Annotation []qualifiers)
  {
    if (isMatch(qualifiers))
      _observer.notify(event);
  }

  void resolveObservers(Set<ObserverMethod<?>> set, Annotation []qualifiers)
  {
    if (isMatch(qualifiers))
      set.add(_observer);
  }

  boolean isAssignableFrom(BaseType type, Annotation []qualifiers)
  {
    if (! _type.isAssignableFrom(type)) {
      return false;
    }

    /*
    if (qualifiers.length < _qualifiers.length)
      return false;
      */

    return isMatch(qualifiers);
  }
  
  private boolean isMatch(Annotation []qualifiers)
  {
    for (QualifierBinding qualifier : _qualifiers) {
      if (qualifier.isAny()) {
      }
      else if (! qualifier.isMatch(qualifiers)) {
        return false;
      }
    }
    
    return true;
  }


  public String toString()
  {
    return getClass().getSimpleName() + "[" + _observer + "," + _type + "]";
  }
}
