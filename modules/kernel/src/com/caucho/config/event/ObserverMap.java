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

package com.caucho.config.event;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.ObserverMethod;

import com.caucho.config.reflect.BaseType;
import com.caucho.inject.Module;

/**
 * Matches bindings
 */
@Module
public class ObserverMap {
  private static final Logger log
    = Logger.getLogger(ObserverMap.class.getName());
  
  private Class<?> _type;

  private ArrayList<ObserverEntry<?>> _observerList
    = new ArrayList<ObserverEntry<?>>();

  public ObserverMap(Class<?> type)
  {
    _type = type;
  }

  public void addObserver(ObserverMethod<?> observer,
                          BaseType type,
                          Annotation []bindings)
  {
    ObserverEntry<?> entry = new ObserverEntry(observer, type, bindings);

    _observerList.add(entry);
  }

  public <T> void resolveObservers(Set<ObserverMethod<? super T>> set,
                                   BaseType eventType,
                                   Annotation []qualifiers)
  {
    for (int i = 0; i < _observerList.size(); i++) {
      ObserverEntry observer = _observerList.get(i);
      
      // ioc/0b5c
      if (observer.isAssignableFrom(eventType, qualifiers)) {
        set.add(observer.getObserver());
      }
    }
  }

  public void resolveEntries(ArrayList<ObserverEntry<?>> list,
                             BaseType eventType,
                             BaseType subType)
  {
    for (int i = 0; i < _observerList.size(); i++) {
      ObserverEntry<?> observer = _observerList.get(i);
      
      BaseType observerType = observer.getType();
      
      if ((observerType.isAssignableFrom(eventType)
           || observerType.isAssignableFrom(subType))
          && ! list.contains(observer)) {
        list.add(observer);
      }
    }
  }

  public void fireEvent(Object event,
                        BaseType eventType,
                        Annotation []qualifiers)
  {
    for (int i = 0; i < _observerList.size(); i++) {
      ObserverEntry observer = _observerList.get(i);

      if (observer.isAssignableFrom(eventType, qualifiers)) {
        if (log.isLoggable(Level.FINEST))
          log.finest(observer.getObserver() + " notify " + event);

        observer.notify(event);
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _type + "]";
  }
}
