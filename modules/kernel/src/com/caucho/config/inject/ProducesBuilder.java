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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;

import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.program.Arg;
import com.caucho.config.program.BeanArg;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * Builder for produces beans.
 */
@Module
public class ProducesBuilder {
  private static final Logger log = Logger.getLogger(ProducesBuilder.class.getName());
  private static final L10N L = new L10N(ProducesBuilder.class);
  
  private InjectManager _manager;

  public ProducesBuilder(InjectManager manager)
  {
    _manager = manager;
  }

  /**
   * Introspects the methods for any @Produces
   */
  public <X> void introspectProduces(Bean<X> bean, AnnotatedType<X> beanType)
  {
    HashSet<AnnotatedMethod<?>> disposesSet
      = new HashSet<AnnotatedMethod<?>>();
    
    for (AnnotatedMethod<? super X> beanMethod : beanType.getMethods()) {
      if (beanMethod.isAnnotationPresent(Produces.class)) {
        AnnotatedMethod<? super X> disposesMethod 
          = findDisposesMethod(beanType, 
                               beanMethod.getBaseType(),
                               getQualifiers(beanMethod));
        
        addProducesMethod(bean, beanType, beanMethod, disposesMethod);

        if (disposesMethod != null)
          disposesSet.add(disposesMethod);
      }
    }
    
    for (AnnotatedField<? super X> beanField : beanType.getFields()) {
      if (beanField.isAnnotationPresent(Produces.class)) {
        AnnotatedMethod<?> disposesMethod
          = findDisposesMethod(beanType, beanField.getBaseType(),
                               getQualifiers(beanField));
        
        addProduces(bean, beanType, beanField);
        
        if (disposesMethod != null)
          disposesSet.add(disposesMethod);
      }
    }
    
    for (AnnotatedMethod<? super X> beanMethod : beanType.getMethods()) {
      if (isDisposes(beanMethod)
          && ! disposesSet.contains(beanMethod))
        throw new ConfigException(L.l("{0}.{1} is an invalid disposes method because it doesn't match a @Produces method",
                                      beanMethod.getJavaMember().getDeclaringClass().getName(),
                                      beanMethod.getJavaMember().getName()));
    }
  }

  protected <X,T> void addProducesMethod(Bean<X> bean,
                                         AnnotatedType<X> beanType,
                                         AnnotatedMethod<? super X> producesMethod,
                                         AnnotatedMethod<? super X> disposesMethod)
  {
    // ioc/07g2 vs ioc/07d0 
    /*
    // 
    if (producesMethod.getJavaMember().getDeclaringClass() != beanType.getJavaClass()
        && ! beanType.isAnnotationPresent(Specializes.class)) {
      return;
    }
    */
    
    Arg<? super X> []producesArgs = introspectArguments(bean, producesMethod);
    Arg<? super X> []disposesArgs = null;
    
    if (disposesMethod != null)
      disposesArgs = introspectDisposesArgs(disposesMethod,
                                            disposesMethod.getParameters());

    ProducesMethodBean<X,T> producesBean
      = ProducesMethodBean.create(_manager, bean, 
                                  producesMethod, producesArgs,
                                  disposesMethod, disposesArgs);
    
    if (producesBean.isAlternative()
        && ! _manager.isEnabled(producesBean)) {
      return;
    }
        
    

    // bean.init();

    // _manager.addBean(producesBean);
    _manager.addProducesBean(producesBean);
  }

  protected <X> void addProduces(Bean<X> bean,
                                 AnnotatedType<X> beanType,
                                 AnnotatedField<?> beanField)
  {
    Class<?> beanClass = beanType.getJavaClass();
    
    if (beanField.getJavaMember().getDeclaringClass() != beanClass
        && ! beanClass.isAnnotationPresent(Specializes.class))
      return;
    
    AnnotatedMethod<? super X> disposesMethod 
      = findDisposesMethod(beanType, beanField.getBaseType(), 
                           getQualifiers(beanField));
    
    Arg<? super X> []disposesArgs = null;
    
    if (disposesMethod != null)
      disposesArgs = introspectDisposesArgs(disposesMethod, disposesMethod.getParameters());
    
    ProducesFieldBean producesFieldBean
      = ProducesFieldBean.create(_manager, bean, beanField,
                                 disposesMethod, disposesArgs);

    // bean.init();

    _manager.addProducesFieldBean(producesFieldBean);
  }
  
  private <X> AnnotatedMethod<? super X>
  findDisposesMethod(AnnotatedType<X> beanType,
                     Type producesBaseType,
                     Annotation []qualifiers)
  {
    for (AnnotatedMethod<? super X> beanMethod : beanType.getMethods()) {
      List<AnnotatedParameter<?>> params = (List) beanMethod.getParameters();
      
      if (params.size() == 0)
        continue;
      
      AnnotatedParameter<?> param = params.get(0);
      
      if (! param.isAnnotationPresent(Disposes.class))
        continue;
      
      if (! producesBaseType.equals(param.getBaseType()))
        continue;
      
      Annotation []testQualifiers = getQualifiers(param);

      if (! isQualifierMatch(qualifiers, testQualifiers))
        continue;
      
      // XXX: check @Qualifiers
      
      Method javaMethod = beanMethod.getJavaMember();
      
      if (beanMethod.isAnnotationPresent(Inject.class))
        throw new ConfigException(L.l("{0}.{1} is an invalid @Disposes method because it has an @Inject annotation",
                                      javaMethod.getDeclaringClass().getName(),
                                      javaMethod.getName()));
      
      return beanMethod;
    }
    
    return null;
  }

  protected <X,T> Arg<T> []introspectArguments(Bean<X> bean,
                                               AnnotatedMethod<T> method)
  {
    List<AnnotatedParameter<T>> params = method.getParameters();
    Method javaMethod = method.getJavaMember();
    
    Arg<T> []args = new Arg[params.size()];

    for (int i = 0; i < args.length; i++) {
      AnnotatedParameter<?> param = params.get(i);
      
      if (param.isAnnotationPresent(Disposes.class))
        throw new ConfigException(L.l("'{0}.{1}' is an invalid producer method because a parameter is annotated with @Disposes",
                                      javaMethod.getDeclaringClass().getName(), 
                                      javaMethod.getName()));
      
      InjectionPoint ip = new InjectionPointImpl(_manager,
                                                 bean,
                                                 param);

      if (InjectionPoint.class.equals(param.getBaseType()))
        args[i] = new InjectionPointArg();
      else
        args[i] = new BeanArg(_manager,
                              param.getBaseType(), 
                              getQualifiers(param),
                              ip);
    }

    return args;
  }

  protected <X> Arg<X> []introspectDisposesArgs(AnnotatedMethod<?> method,
                                                List<AnnotatedParameter<X>> params)
  {
    Arg<X> []args = new Arg[params.size()];
    
    boolean hasDisposes = false;

    for (int i = 0; i < args.length; i++) {
      AnnotatedParameter<X> param = params.get(i);
      
      InjectionPoint ip = null;

      if (param.isAnnotationPresent(Disposes.class)) {
        if (hasDisposes)
          throw new ConfigException(L.l("{0}.{1} is an invalid @Disposes method because two parameters are marked @Disposes",
                                        method.getJavaMember().getDeclaringClass().getName(),
                                        method.getJavaMember().getName()));
        hasDisposes = true;

        args[i] = null;
      }
      else
        args[i] = new BeanArg(_manager,
                              param.getBaseType(), 
                              getQualifiers(param),
                              ip);
    }

    return args;
  }

  protected boolean isDisposes(AnnotatedMethod<?> method)
  {
    List<AnnotatedParameter<?>> params = (List) method.getParameters();

    for (int i = 0; i < params.size(); i++) {
      AnnotatedParameter<?> param = params.get(i);
      
      if (param.isAnnotationPresent(Disposes.class))
        return true;
    }
    
    return false;
  }
  
  private boolean isQualifierMatch(Annotation []aList, Annotation []bList)
  {
    for (Annotation a : aList) {
      if (! isQualifierPresent(a, bList))
        return false;
    }
    
    return true;
  }
  
  private boolean isQualifierPresent(Annotation a, Annotation []list)
  {
    for (Annotation ann : list) {
      if (! ann.annotationType().equals(a.annotationType()))
        continue;
      
      return true;
    }
    
    return false;
  }
  
  private Annotation []getQualifiers(Annotated annotated)
  {
    ArrayList<Annotation> qualifierList = new ArrayList<Annotation>();

    for (Annotation ann : annotated.getAnnotations()) {
      if (ann.annotationType().equals(Named.class)) {
        Named named = (Named) ann;
        
        String namedValue = named.value();

        if ("".equals(namedValue)) {
          String name = ((Class) annotated.getBaseType()).getSimpleName();

          ann = Names.create(name);
        }

        qualifierList.add(ann);

      }
      else if (ann.annotationType().isAnnotationPresent(Qualifier.class)) {
        qualifierList.add(ann);
      }
    }

    if (qualifierList.size() == 0)
      qualifierList.add(CurrentLiteral.CURRENT);

    Annotation []qualifiers = new Annotation[qualifierList.size()];
    qualifierList.toArray(qualifiers);

    return qualifiers;
  }
}
