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

package com.caucho.config.j2ee;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceProperty;

import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.InjectionPointHandler;
import com.caucho.config.program.BeanValueGenerator;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.FieldGeneratorProgram;
import com.caucho.config.program.MethodGeneratorProgram;
import com.caucho.config.program.NullProgram;
import com.caucho.config.program.ValueGenerator;
import com.caucho.util.L10N;

/**
 * Handles the @PersistenceContext annotation for JavaEE
 */
public class PersistenceContextHandler extends JavaeeInjectionHandler {
  private static final L10N L = new L10N(PersistenceContextHandler.class);

  public PersistenceContextHandler(InjectManager manager)
  {
    super(manager);
  }
  
  @Override
  public ConfigProgram introspectType(AnnotatedType<?> type)
  {
    PersistenceContext pContext = type.getAnnotation(PersistenceContext.class);
    
    String location = type.getJavaClass().getName() + ": ";

    String jndiName = null;
    
    if (! "".equals(pContext.name()))
      jndiName = pContext.name();

    Bean<?> bean = bindEntityManager(location, pContext);
    
    BeanValueGenerator gen
      = new BeanValueGenerator(location, bean);
    
    if (jndiName != null)
      bindJndi(jndiName, gen, null);
    
    return new NullProgram();
  }
  
  @Override
  public ConfigProgram introspectField(AnnotatedField<?> field)
  {
    PersistenceContext pContext = field.getAnnotation(PersistenceContext.class);
    
    PersistenceContextType type = pContext.type();
    
    Field javaField = field.getJavaMember();
    String location = getLocation(javaField);
    
    if (! javaField.getType().isAssignableFrom(EntityManager.class)) {
      throw new ConfigException(L.l("{0}: @PersistenceContext field must be assignable from EntityManager.",
                                    getLocation(javaField)));
    }
    
    ValueGenerator gen;
    
    if (PersistenceContextType.EXTENDED.equals(type))
      gen = generateExtendedContext(location, pContext);
    else
      gen = generateTransactionContext(location, pContext);
    
    return new FieldGeneratorProgram(javaField, gen);
  }
  // InjectIntrospector.introspect(_injectProgramList, field);
  
  @Override
  public ConfigProgram introspectMethod(AnnotatedMethod<?> method)
  {
    PersistenceContext pContext = method.getAnnotation(PersistenceContext.class);
    
    Method javaMethod= method.getJavaMember();
    String location = getLocation(javaMethod);
    
    Class<?> param = javaMethod.getParameterTypes()[0];
    
    if (! param.isAssignableFrom(EntityManager.class)) {
      throw new ConfigException(L.l("{0}: @PersistenceContext method must be assignable from EntityManager.",
                                    getLocation(javaMethod)));
    }
    
    BeanValueGenerator gen;
    
    /*
    if (PersistenceContextType.EXTENDED.equals(type))
      return generateExtendedContext(field, pContext);
    else
    */
    
    gen = generateTransactionContext(location, pContext);
    
    return new MethodGeneratorProgram(javaMethod, gen);
  }
  // InjectIntrospector.introspect(_injectProgramList, field);

  private BeanValueGenerator 
  generateTransactionContext(String location,
                             PersistenceContext pContext)
    throws ConfigException
  {
    Bean<?> bean = bindEntityManager(location, pContext);

    BeanValueGenerator gen
      = new BeanValueGenerator(location, bean);
    
    return gen;
  }

  private ValueGenerator generateExtendedContext(String location,
                                                 PersistenceContext pContext)
  {
    PersistenceContextGenerator gen;

    gen = new PersistenceContextGenerator(location, pContext);
    
    return gen;
  }
  
  private Bean<?> bindEntityManager(String location, 
                                    PersistenceContext pContext)
  {
    String name = pContext.name();
    String unitName = pContext.unitName();

    Bean<?> bean;
    
    bean = bind(location, EntityManager.class, unitName);
    
    if (bean == null)
      bean = bind(location, EntityManager.class, name);

    if (bean != null) {
      // valid bean
    }
    else if (! "".equals(unitName)) {
      throw new ConfigException(location + L.l("unitName='{0}' is an unknown @PersistenceContext.",
                                               unitName));
    }
    else if (! "".equals(name)) {
      throw new ConfigException(location + L.l("name='{0}' is an unknown @PersistenceContext.",
                                               name));

    }
    else {
      throw new ConfigException(location + L.l("@PersistenceContext cannot find any persistence contexts.  No JPA persistence-units have been deployed"));
    }
    
    return bean;
  }
}
