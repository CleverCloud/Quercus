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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.decorator.Delegate;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import com.caucho.config.reflect.AnnotatedFieldImpl;
import com.caucho.config.reflect.AnnotatedMethodImpl;
import com.caucho.config.reflect.ReflectionAnnotatedFactory;
import com.caucho.inject.Module;

/**
 */
@Module
public class InjectionPointImplHandle implements Serializable
{
  private final String _beanClass;
  private final Annotation []_beanQualifiers;
  
  private final String _typeClass;
  private final String _memberClass;
  private final String _memberName;
  private final MemberType _memberType;
  
  private HashSet<Annotation> _qualifiers;

  InjectionPointImplHandle(String beanClass,
                           Set<Annotation> beanQualifierSet,
                           Member member,
                           HashSet<Annotation> qualifiers,
                           String typeClass)
  {
    Annotation []beanQualifiers = new Annotation[beanQualifierSet.size()];
    beanQualifierSet.toArray(beanQualifiers);

    _beanClass = beanClass;
    _beanQualifiers = beanQualifiers;
    
    _typeClass = typeClass;
    
    _memberClass = member.getDeclaringClass().getName();
    _memberName = member.getName();
    
    if (member instanceof Field)
      _memberType = MemberType.FIELD;
    else if (member instanceof Method)
      _memberType = MemberType.METHOD;
    else
      _memberType = MemberType.CONSTRUCTOR;
    
    _qualifiers = new HashSet<Annotation>(qualifiers);
  }
  
  private Object readResolve()
  {
    try {
      InjectManager cdiManager = InjectManager.create();
      
      Class<?> beanClass = Class.forName(_beanClass,
                                         false,
                                         cdiManager.getClassLoader());
      
      Class<?> typeClass = Class.forName(_typeClass,
                                         false,
                                         cdiManager.getClassLoader());
      
      Annotation []qualifiers = new Annotation[_qualifiers.size()];
      _qualifiers.toArray(qualifiers);

      Bean<?> bean 
        = cdiManager.resolve(cdiManager.getBeans(beanClass, _beanQualifiers));

      Class<?> memberClass = Class.forName(_memberClass,
                                           false,
                                           cdiManager.getClassLoader());

      AnnotatedType annType
        = ReflectionAnnotatedFactory.introspectType(memberClass);
      
      Member member;
      Annotated annotated;
      
      switch (_memberType) {
      case FIELD:
        member = getField(memberClass, _memberName);
        annotated = new AnnotatedFieldImpl(annType, (Field) member);
        break;
        
      case METHOD:
        member = getMethod(memberClass, _memberName);
        annotated = new AnnotatedMethodImpl((Method) member);
        break;
        
      default:
        member = memberClass.getConstructors()[0];
        annotated = annType;
        break;
      }
      
      return new InjectionPointImpl(cdiManager, bean, annotated, member, 
                                    typeClass);
    } catch (Exception e) {
      throw new RuntimeException(_memberClass + ":" + _memberName + ":" + e, e);
    }
  }
  
  private Field getField(Class<?> cl, String name)
  {
    if (cl == null)
      return null;
    
    for (Field field : cl.getDeclaredFields()) {
      if (field.getName().equals(name))
        return field;
    }
    
    return getField(cl.getSuperclass(), name);
  }
  
  private Method getMethod(Class<?> cl, String name)
  {
    if (cl == null)
      return null;
    
    for (Method method : cl.getDeclaredMethods()) {
      if (method.getName().equals(name))
        return method;
    }
    
    return getMethod(cl.getSuperclass(), name);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _memberName + "]";
  }
  
  enum MemberType {
    FIELD,
    METHOD,
    CONSTRUCTOR;
  }
}
