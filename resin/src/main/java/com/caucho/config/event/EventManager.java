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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.inject.Inject;
import javax.inject.Qualifier;

import com.caucho.config.inject.InjectManager;
import com.caucho.config.reflect.BaseType;
import com.caucho.config.reflect.ParamType;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * Internal implementation for a Bean
 */
@Module
public class EventManager
{
  private static final L10N L = new L10N(EventManager.class);
  private static final Logger log = Logger.getLogger(EventManager.class.getName());
  
  private InjectManager _cdiManager;

  private ConcurrentHashMap<Class<?>,ObserverMap> _extObserverMap
    = new ConcurrentHashMap<Class<?>,ObserverMap>();
  
  private ConcurrentHashMap<Class<?>,ObserverMap> _observerMap
    = new ConcurrentHashMap<Class<?>,ObserverMap>();

  private ConcurrentHashMap<EventKey,Set<ObserverMethod<?>>> _observerMethodCache
    = new ConcurrentHashMap<EventKey,Set<ObserverMethod<?>>>();
  
  public EventManager(InjectManager cdiManager)
  {
    _cdiManager = cdiManager;
  }

  public <X,Z> void addObserver(Bean<X> bean, AnnotatedMethod<Z> beanMethod)
  {
    int param = findObserverAnnotation(beanMethod);

    if (param < 0)
      return;

    Method method = beanMethod.getJavaMember();
    Type eventType = method.getGenericParameterTypes()[param];
    
    // ioc/0b22 vs ioc/0b0h
    /*
    if (! method.getDeclaringClass().equals(bean.getBeanClass())
        && ! bean.getBeanClass().isAnnotationPresent(Specializes.class)) {
      return;
    }
    */
    
    HashSet<Annotation> bindingSet = new HashSet<Annotation>();

    List<AnnotatedParameter<Z>> paramList = beanMethod.getParameters();
    for (Annotation ann : paramList.get(param).getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class))
        bindingSet.add(ann);
    }
    
    Observes observes = paramList.get(param).getAnnotation(Observes.class);

    if (method.isAnnotationPresent(Inject.class)) {
      throw InjectManager.error(method, L.l("A method may not have both an @Observer and an @Inject annotation."));
    }

    if (method.isAnnotationPresent(Produces.class)) {
      throw InjectManager.error(method, L.l("A method may not have both an @Observer and a @Produces annotation."));
    }

    if (method.isAnnotationPresent(Disposes.class)) {
      throw InjectManager.error(method, L.l("A method may not have both an @Observer and a @Disposes annotation."));
    }

    ObserverMethodImpl<X,Z> observerMethod;
    switch(observes.during()) {
    case BEFORE_COMPLETION:
      observerMethod 
        = new ObserverMethodBeforeCompletionImpl(_cdiManager, 
                                                 bean, 
                                                 beanMethod,
                                                 eventType, 
                                                 bindingSet);
      break;
      
    case AFTER_COMPLETION:
      observerMethod 
        = new ObserverMethodAfterCompletionImpl(_cdiManager, 
                                                bean, 
                                                beanMethod,
                                                eventType, 
                                                bindingSet);
      break;
      
    case AFTER_SUCCESS:
      observerMethod 
        = new ObserverMethodAfterSuccessImpl(_cdiManager, 
                                             bean, 
                                             beanMethod,
                                             eventType, 
                                             bindingSet);
      break;
      
    case AFTER_FAILURE:
      observerMethod 
        = new ObserverMethodAfterFailureImpl(_cdiManager, 
                                             bean, 
                                             beanMethod,
                                             eventType, 
                                             bindingSet);
      break;
      
    default:
      observerMethod = new ObserverMethodImpl(_cdiManager, bean, beanMethod,
                                              eventType, bindingSet);
    }

    _cdiManager.addObserver(observerMethod, beanMethod);
  }

  public static <Z> int findObserverAnnotation(AnnotatedMethod<Z> method)
  {
    List<AnnotatedParameter<Z>> params = method.getParameters();
    int size = params.size();
    int observer = -1;

    for (int i = 0; i < size; i++) {
      AnnotatedParameter<?> param = params.get(i);

      if (param.isAnnotationPresent(Observes.class)) {
        if (observer >= 0 && observer != i)
          throw InjectManager.error(method.getJavaMember(), L.l("Only one param may have an @Observer"));

        observer = i;
      }
    }

    return observer;
  }
  
  public void fireEvent(Object event, Annotation... qualifiers)
  {
    for (ObserverMethod<?>  method : resolveObserverMethods(event, qualifiers)) {
      ((ObserverMethod) method).notify(event);
    }
  }
  
  public <T> Set<ObserverMethod<? super T>>
  resolveObserverMethods(T event, Annotation... qualifiers)
  {
    Class<?> eventClass = event.getClass();
    
    EventKey key = new EventKey(eventClass, qualifiers);
    
    Set<ObserverMethod<?>> observerList = _observerMethodCache.get(key);
    
    if (observerList == null) {
      BaseType eventType = _cdiManager.createSourceBaseType(event.getClass());

      // ioc/0b71
      if (eventType.isGeneric() || eventType instanceof ParamType)
        throw new IllegalArgumentException(L.l("'{0}' is an invalid event type because it's generic.",
                                               eventType));
      
      validateEventQualifiers(qualifiers);
            
      observerList = new LinkedHashSet<ObserverMethod<?>>();
    
      fillObserverMethodList(observerList, eventType, qualifiers);
    
      _observerMethodCache.put(key, observerList);
    }
    
    return (Set) observerList;
  }

  private void validateEventQualifiers(Annotation []qualifiers)
  {
    int length = qualifiers.length;
    for (int i = 0; i < length; i++) {
      Annotation qualifierA = qualifiers[i];

      Class<? extends Annotation> annType = qualifierA.annotationType();
    
      if (! _cdiManager.isQualifier(annType))
        throw new IllegalArgumentException(L.l("'{0}' is an invalid event annotation because it's not a @Qualifier.",
                                               qualifierA));
    
      Retention retention = annType.getAnnotation(Retention.class);
    
      if (retention == null || retention.value() != RetentionPolicy.RUNTIME) {
        throw new IllegalArgumentException(L.l("'{0}' is an invalid event qualifier because it doesn't have RUNTIME retention.",
                                               qualifierA));        
      }
    
      for (int j = i + 1; j < length; j++) {
        if (qualifierA.annotationType() == qualifiers[j].annotationType()) {
          throw new IllegalArgumentException(L.l("fireEvent is invalid because the bindings are duplicate types: {0} and {1}",
                                                 qualifiers[i], qualifiers[j]));
        
        }
      }
    }
  }

  public void
  fillObserverMethodList(Set<ObserverMethod<?>> list,
                         BaseType type, Annotation []qualifiers)
  {
    if (_cdiManager.getParent() != null) {
      EventManager parentManager = _cdiManager.getParent().getEventManager();
      
      parentManager.fillObserverMethodList(list, type, qualifiers);
    }
    
    fillLocalObserverList(_observerMap, list, type, qualifiers);
  }

  public void fireExtensionEvent(Object event, 
                                 Annotation... qualifiers)
  {
    fireLocalEvent(_extObserverMap, event, qualifiers);
  }

  @Module
  public void fireExtensionEvent(Object event, BaseType eventType, 
                                 Annotation... qualifiers)
  {
    fireLocalEvent(_extObserverMap, event, eventType, qualifiers);
  }

  private void fireLocalEvent(ConcurrentHashMap<Class<?>,ObserverMap> localMap,
                              Object event, Annotation... bindings)
  {
    // ioc/0062 - class with type-param handled specially
    BaseType eventType = _cdiManager.createTargetBaseType(event.getClass());

    fireLocalEvent(localMap, event, eventType, bindings);
  }
  
  private void fireLocalEvent(ConcurrentHashMap<Class<?>,ObserverMap> localMap,
                              Object event, BaseType eventType,
                              Annotation... qualifiers)
  {
    Set<ObserverMethod<?>> observerList = new LinkedHashSet<ObserverMethod<?>>();

    fillLocalObserverList(localMap, observerList, eventType, qualifiers);

    for (ObserverMethod<?> method : observerList) {
      ((ObserverMethod) method).notify(event);
    }
  }

  private void fillLocalObserverList(ConcurrentHashMap<Class<?>,ObserverMap> localMap,
                                     Set<ObserverMethod<?>> list,
                                     BaseType eventType,
                                     Annotation []qualifiers)
  {
    for (BaseType type : eventType.getBaseTypeClosure(_cdiManager)) {
      Class<?> rawClass = type.getRawClass();

      ObserverMap map = localMap.get(rawClass);
      
      if (map != null) {
        // ioc/0b5c, ioc/0b82
        map.resolveObservers((Set) list, eventType, qualifiers);
        map.resolveObservers((Set) list, type, qualifiers);
      }
    }
  }

  //
  // events
  //

  //
  // event management
  //

  /**
   * Registers an event observer
   *
   * @param observer the observer object
   * @param bindings the binding set for the event
   */
  public void addObserver(ObserverMethod<?> observer)
  {
    BaseType observedType = _cdiManager.createTargetBaseType(observer.getObservedType());
    Set<Annotation> qualifierSet = observer.getObservedQualifiers();

    Annotation[] qualifiers = new Annotation[qualifierSet.size()];
    int i = 0;
    for (Annotation qualifier : qualifierSet) {
      qualifiers[i++] = qualifier;
    }

    addObserver(observer, observedType, qualifiers);
  }

  /**
   * Registers an event observer
   *
   * @param observer the observer object
   * @param bindings the binding set for the event
   */
  public void addObserver(ObserverMethod<?> observer,
                          Type type,
                          Annotation... bindings)
  {
    BaseType eventType = _cdiManager.createTargetBaseType(type);

    addObserver(observer, eventType, bindings);
  }

  /**
   * Registers an event observer
   *
   * @param observer the observer object
   * @param bindings the binding set for the event
   */
  public void addObserver(ObserverMethod<?> observer,
                          BaseType eventBaseType,
                          Annotation... bindings)
  {
    Class<?> eventType = eventBaseType.getRawClass();

    _cdiManager.checkActive();
    
    /*
    if (eventType.getTypeParameters() != null
        && eventType.getTypeParameters().length > 0) {
      throw new IllegalArgumentException(L.l("'{0}' is an invalid event type because it's a parameterized type.",
                                             eventType));
    }
    */

    ObserverMap map = _observerMap.get(eventType);

    if (map == null) {
      map = new ObserverMap(eventType);
      ObserverMap oldMap = _observerMap.putIfAbsent(eventType, map);
        
      if (oldMap != null)
        map = oldMap;
    }

    map.addObserver(observer, eventBaseType, bindings);

    _observerMethodCache.clear();
  }

  /**
   * Registers an event observer
   *
   * @param observer the observer object
   * @param bindings the binding set for the event
   */
  public void addExtensionObserver(ObserverMethod<?> observer,
                            BaseType eventBaseType,
                            Annotation... bindings)
  {
    addObserver(_extObserverMap, observer, eventBaseType, bindings);
  }

  /**
   * Registers an event observer
   *
   * @param observer the observer object
   * @param bindings the binding set for the event
   */
  private void addObserver(ConcurrentHashMap<Class<?>,ObserverMap> observerMap,
                           ObserverMethod<?> observer,
                           BaseType eventBaseType,
                           Annotation... bindings)
  {
    Class<?> eventType = eventBaseType.getRawClass();

    _cdiManager.checkActive();

    /*
    if (eventType.getTypeParameters() != null
        && eventType.getTypeParameters().length > 0) {
      throw new IllegalArgumentException(L.l("'{0}' is an invalid event type because it's a parameterized type.",
                                             eventType));
    }
    */

    ObserverMap map = observerMap.get(eventType);

    if (map == null) {
      map = new ObserverMap(eventType);
      
      ObserverMap oldMap;
      
      oldMap = observerMap.putIfAbsent(eventType, map);
      
      if (oldMap != null)
        map = oldMap;
    }

    map.addObserver(observer, eventBaseType, bindings);

    _observerMethodCache.clear();
  }

  /**
   * Removes an event observer
   *
   * @param observer the observer object
   * @param eventType the type of event to listen for
   * @param bindings the binding set for the event
   */
  public void removeObserver(ObserverMethod<?> observer)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Registers an event observer
   *
   * @param observerMethod the observer method
   */
  /*
  public void addObserver(ObserverMethod<?,?> observerMethod)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cdiManager + "]"; 
  }
  
  static class EventKey {
    private Class<?> _type;
    private Annotation []_qualifiers;
    
    EventKey(Class<?> type, Annotation []qualifiers)
    {
      _type = type;
      _qualifiers = qualifiers;
    }

    @Override
    public int hashCode()
    {
      int hash = _type.hashCode();
      
      if (_qualifiers == null)
        return hash;
      
      for (Annotation ann : _qualifiers) {
        hash += 65521 * ann.hashCode(); 
      }
      
      return hash;
    }

    @Override
    public boolean equals(Object o)
    {
      if (! (o instanceof EventKey))
        return false;
      
      EventKey key = (EventKey) o;
      
      if (! _type.equals(key._type))
        return false;
      
      if (_qualifiers.length != key._qualifiers.length)
        return false;
      
      for (int i = 0; i < _qualifiers.length; i++) {
        if (_qualifiers[i] != key._qualifiers[i])
          return false;
      }
      
      return true;
    }
  }
}
