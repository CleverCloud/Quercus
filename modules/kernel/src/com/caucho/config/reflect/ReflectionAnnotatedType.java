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
import java.lang.annotation.Inherited;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.NormalScope;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Scope;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.util.L10N;

/**
 * Read-only introspected annotated type.
 */
public class ReflectionAnnotatedType<T>
  extends ReflectionAnnotated
  implements AnnotatedType<T>
{
  private static final L10N L = new L10N(ReflectionAnnotatedType.class);
  
  private static final Logger log
    = Logger.getLogger(ReflectionAnnotatedType.class.getName());

  private Class<T> _javaClass;
  
  private ReflectionAnnotatedType<?> _parentType;

  private Set<AnnotatedConstructor<T>> _constructorSet
    = new LinkedHashSet<AnnotatedConstructor<T>>();

  private Set<AnnotatedField<? super T>> _fieldSet
    = new LinkedHashSet<AnnotatedField<? super T>>();

  private Set<AnnotatedMethod<? super T>> _methodSet
    = new LinkedHashSet<AnnotatedMethod<? super T>>();

  ReflectionAnnotatedType(InjectManager manager, 
                          BaseType type)
  {
    super(type.getRawClass(),
          type.getTypeClosure(manager),
          type.getRawClass().getDeclaredAnnotations());

    _javaClass = (Class<T>) type.getRawClass();
    
    Class<?> parentClass = _javaClass.getSuperclass();
    
    if (parentClass != null && ! parentClass.equals(Object.class))
      _parentType = ReflectionAnnotatedFactory.introspectType(parentClass);
    
    introspect(_javaClass);
  }

  /**
   * Returns the concrete Java class
   */
  @Override
  public Class<T> getJavaClass()
  {
    return _javaClass;
  }

  /**
   * Returns the abstract introspected constructors
   */
  @Override
  public Set<AnnotatedConstructor<T>> getConstructors()
  {
    return _constructorSet;
  }

  /**
   * Returns the abstract introspected methods
   */
  @Override
  public Set<AnnotatedMethod<? super T>> getMethods()
  {
    return _methodSet;
  }

  /**
   * Returns the matching method, creating one if necessary.
   */
  public AnnotatedMethod<? super T> createMethod(Method method)
  {
    for (AnnotatedMethod<? super T> annMethod : _methodSet) {
      if (AnnotatedMethodImpl.isMatch(annMethod.getJavaMember(), method)) {
        return annMethod;
      }
    }

    AnnotatedMethod<T> annMethod = new AnnotatedMethodImpl<T>(this, null, method);

    _methodSet.add(annMethod);

    return annMethod;
  }

  /**
   * Returns the abstract introspected fields
   */
  @Override
  public Set<AnnotatedField<? super T>> getFields()
  {
    return _fieldSet;
  }

  private void introspect(Class<T> cl)
  {
    try {
      introspectInheritedAnnotations(cl);
      
      if (cl.isAnnotationPresent(Specializes.class))
        introspectSpecializesAnnotations(cl);

      introspectFields(cl);

      introspectMethods(cl);

      if (! cl.isInterface()) {
        for (Constructor<?> ctor : cl.getDeclaredConstructors()) {
          _constructorSet.add(new AnnotatedConstructorImpl(this, ctor));
        }

        if (_constructorSet.size() == 0) {
          try {
            Constructor<T> ctor = cl.getConstructor(new Class[0]);
            _constructorSet.add(new AnnotatedConstructorImpl<T>(this, ctor));
          } catch (NoSuchMethodException e) {
            log.log(Level.FINE, e.toString(), e);
          }
        }
      }
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(L.l("{0} introspection failed: {1}", cl.getName(), e), e);
    }
  }

  private void introspectFields(Class<?> cl)
  {
    if (cl == null)
      return;
    
    introspectFields(cl.getSuperclass());

    for (Field field : cl.getDeclaredFields()) {
      try {
        // ioc/0p23
        _fieldSet.add(new AnnotatedFieldImpl<T>(this, field));
      } catch (ConfigException e) {
        throw e;
      } catch (Throwable e) {
        throw ConfigException.create(L.l("{0}: {1}\n", field, e), e);
      }
    }
  }

  private void introspectMethods(Class<?> cl)
  {
    if (cl == null)
      return;
    
    for (Method method : cl.getDeclaredMethods()) {
      if (method.getDeclaringClass().equals(Object.class))
        continue;
      
      // ejb/4018
      // hasBeanAnnotation(method)
      if (hasBeanAnnotation(method)
          || ! Modifier.isPrivate(method.getModifiers())) {
        AnnotatedMethod<?> childMethod = null;
        
        if (! Modifier.isPrivate(method.getModifiers())) {
          childMethod = AnnotatedTypeUtil.findMethod(_methodSet, method);
        }
        
        if (childMethod == null) {
          _methodSet.add(new AnnotatedMethodImpl<T>(this, null, method));
        }
        /*
        else if (! isParent)
          continue;
          */
        else if (childMethod instanceof AnnotatedMethodImpl<?>){
          AnnotatedMethodImpl<?> oldMethodImpl
            = (AnnotatedMethodImpl<?>) childMethod;
          
          // ejb/1290
          for (Annotation ann : method.getAnnotations()) {
            Class<?> annType = ann.annotationType();
            
            if (oldMethodImpl.isAnnotationPresent(ann.annotationType()))
              continue;
            
            if (! annType.isAnnotationPresent(Inherited.class))
              continue;
            
            oldMethodImpl.addAnnotation(ann);
          }
        }
      }
    }
    
    // ejb/4050
    if (cl.isInterface()) {
      for (Class<?> superInterface : cl.getInterfaces()) 
        introspectMethods(superInterface);
    }
    else {
      introspectParentMethods(_parentType);
    }
  }

  private void introspectParentMethods(AnnotatedType<?> parentType)
  {
    if (parentType == null)
      return;
    
    for (AnnotatedMethod<?> annMethod : parentType.getMethods()) {
      Method javaMethod = annMethod.getJavaMember();
      
      if (hasBeanAnnotation(javaMethod)
          || ! Modifier.isPrivate(javaMethod.getModifiers())) {
        AnnotatedMethod<?> childMethod = null;
        
        if (! Modifier.isPrivate(javaMethod.getModifiers()))
          childMethod = AnnotatedTypeUtil.findMethod(_methodSet, javaMethod);
            
        if (childMethod == null) {
          _methodSet.add((AnnotatedMethod<? super T>) annMethod);
        }
        /*
        else if (! isParent)
          continue;
          */
        else if (childMethod instanceof AnnotatedMethodImpl<?>){
          AnnotatedMethodImpl<?> oldMethodImpl
            = (AnnotatedMethodImpl<?>) childMethod;
          
          // ejb/1290
          for (Annotation ann : annMethod.getAnnotations()) {
            Class<?> annType = ann.annotationType();
            
            if (oldMethodImpl.isAnnotationPresent(ann.annotationType()))
              continue;
            
            // ioc/0a02
            if (! annType.isAnnotationPresent(Inherited.class)
                && ! annType.equals(PostConstruct.class)
                && ! annType.equals(PreDestroy.class))
              continue;
            
            oldMethodImpl.addAnnotation(ann);
          }
        }
      }
    }
  }
  
  private void introspectInheritedAnnotations(Class<?> cl)
  {
    introspectInheritedAnnotations(cl, false, false);
  }

  private void introspectInheritedAnnotations(Class<?> cl,
                                              boolean isScope,
                                              boolean isQualifier)
  {
    if (cl == null)
      return;

    for (Annotation ann : cl.getDeclaredAnnotations()) {
      Class<? extends Annotation> annType = ann.annotationType();

      if ((ann.annotationType().isAnnotationPresent(Scope.class)
           || ann.annotationType().isAnnotationPresent(NormalScope.class))) {
        if (isScope)
          continue;
  
        isScope = true;
      }

      if (ann.annotationType().isAnnotationPresent(Qualifier.class)) {
        if (isQualifier)
          continue;
  
        isQualifier = true;
      }
      
      if (cl == _javaClass)
        continue;

      if (! annType.isAnnotationPresent(Inherited.class)) {
        continue;
      }

      if (isAnnotationPresent(annType)) {
        continue;
      }

      /*
      if (ann.annotationType().isAnnotationPresent(DeploymentType.class)
          && hasMetaAnnotation(getAnnotations(), DeploymentType.class)) {
        continue;
      }
      */

      addAnnotation(ann);
    }

    introspectInheritedAnnotations(cl.getSuperclass(), isScope, isQualifier);
  }

  private void introspectSpecializesAnnotations(Class<?> cl)
  {
    Class<?> parentClass = cl.getSuperclass();
    
    if (parentClass == null)
      return;
    
    if (cl.isAnnotationPresent(Named.class))
      throw new ConfigException(L.l("'{0}' is an invalid @Specializes bean because it has a @Named annotation.",
                                    cl.getName()));
    
    boolean isQualifierPresent = false;

    if (isMetaAnnotationPresent(cl.getAnnotations(), Qualifier.class)) {
      // isQualifierPresent = true;
      /*
      throw new ConfigException(L.l("'{0}' is an invalid @Specializes bean because it has a @Qualifier annotation.",
                                    cl.getName()));
                                    */
    }
    
    for (Annotation ann : parentClass.getDeclaredAnnotations()) {
      Class<? extends Annotation> annType = ann.annotationType();

      /*
      if (! isQualifierPresent && annType.isAnnotationPresent(Qualifier.class)) {
        addAnnotation(ann);
      }
      */
      if (annType.isAnnotationPresent(Qualifier.class)) {
        addAnnotation(ann);
      }
      else if (Named.class.equals(annType)) {
        addAnnotation(ann);
      }
    }
  }

  private boolean hasBeanAnnotation(Method method)
  {
    if (hasBeanAnnotation(method.getAnnotations()))
      return true;

    Annotation [][]paramAnn = method.getParameterAnnotations();
    if (paramAnn != null) {
      for (int i = 0; i < paramAnn.length; i++) {
        if (hasBeanAnnotation(paramAnn[i]))
          return true;
      }
    }

    return false;
  }

  private boolean hasBeanAnnotation(Annotation []annotations)
  {
    if (annotations == null)
      return false;

    for (Annotation ann : annotations) {
      if (isBeanAnnotation(ann.annotationType()))
        return true;

      for (Annotation metaAnn : ann.annotationType().getAnnotations()) {
        if (isBeanAnnotation(metaAnn.annotationType()))
          return true;
      }
    }

    return false;
  }

  private boolean isMetaAnnotationPresent(Annotation []annotations,
                                          Class<? extends Annotation> metaAnnType)
  {
    if (annotations == null)
      return false;

    for (Annotation ann : annotations) {
      if (ann.annotationType().isAnnotationPresent(metaAnnType))
        return true;
    }
    
    return false;
  }

  private boolean isBeanAnnotation(Class<?> annType)
  {
    String name = annType.getName();

    return name.startsWith("javax.");
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _javaClass + "]";
  }
}
