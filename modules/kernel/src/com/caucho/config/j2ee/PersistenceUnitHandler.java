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
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceProperty;
import javax.persistence.PersistenceUnit;

import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.InjectionPointHandler;
import com.caucho.config.program.BeanValueGenerator;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.FieldGeneratorProgram;
import com.caucho.config.program.MethodGeneratorProgram;
import com.caucho.config.program.ValueGenerator;
import com.caucho.util.L10N;

/**
 * Handles the @PersistenceUnit annotation for JavaEE
 */
public class PersistenceUnitHandler extends JavaeeInjectionHandler {
  private static final L10N L = new L10N(PersistenceUnitHandler.class);
  
  public PersistenceUnitHandler(InjectManager manager)
  {
    super(manager);
  }
  
  @Override
  public ConfigProgram introspectField(AnnotatedField<?> field)
  {
    PersistenceUnit pUnit = field.getAnnotation(PersistenceUnit.class);
    
    Field javaField = field.getJavaMember();
    
    if (! javaField.getType().isAssignableFrom(EntityManagerFactory.class)) {
      throw new ConfigException(L.l("{0}: @PersistenceUnit field must be assignable from EntityManagerFactory.",
                                    getLocation(javaField)));
    }
    
    return generateContext(field, pUnit);
  }
  
  @Override
  public ConfigProgram introspectMethod(AnnotatedMethod<?> method)
  {
    PersistenceUnit pUnit = method.getAnnotation(PersistenceUnit.class);
    
    Method javaMethod = method.getJavaMember();
    
    Class<?> param = null;
    
    if (javaMethod.getParameterTypes().length == 1)
      param = javaMethod.getParameterTypes()[0];
    
    if (param == null || ! param.isAssignableFrom(EntityManagerFactory.class)) {
      throw new ConfigException(L.l("{0}: @PersistenceUnit method must be assignable from EntityManagerFactory.",
                                    getLocation(javaMethod)));
    }
    
    return generateContext(method, pUnit);
  }

  private ConfigProgram generateContext(AnnotatedField<?> field,
                                        PersistenceUnit pUnit)
    throws ConfigException
  {
    Field javaField = field.getJavaMember();
    
    String location = getLocation(javaField);

    /*
    if (! "".equals(pContext.name()))
      jndiName = pContext.name();
      */
    BeanValueGenerator gen = bind(location, pUnit);
    
    return new FieldGeneratorProgram(javaField, gen);
  }

  private ConfigProgram generateContext(AnnotatedMethod<?> method,
                                        PersistenceUnit pUnit)
    throws ConfigException
  {
    Method javaMethod = method.getJavaMember();
    
    String location = getLocation(javaMethod);

    /*
    if (! "".equals(pContext.name()))
      jndiName = pContext.name();
      */
    BeanValueGenerator gen = bind(location, pUnit);
    
    return new MethodGeneratorProgram(javaMethod, gen);
  }
  
  private BeanValueGenerator bind(String location, PersistenceUnit pUnit)
  {
    String name = pUnit.name();
    String unitName = pUnit.unitName();

    /*
    if (! "".equals(pContext.name()))
      jndiName = pContext.name();
      */

    Bean<?> bean = null;
    
    if (! "".equals(unitName)) {
      bean = bind(location, EntityManagerFactory.class, unitName);
      
      if (bean == null) {
        Set<Bean<?>> beans = getManager().getBeans(EntityManagerFactory.class,
                                                   new AnnotationLiteral<Any>() {});
        
        throw new ConfigException(location + L.l("unitName='{0}' is an unknown @PersistenceUnit.\n  {1}",
                                                 unitName,
                                                 beans));
      }
    }
    
    if (bean == null)
      bean = bind(location, EntityManagerFactory.class, name);

    if (bean != null) {
      // valid bean
    }
    else if (! "".equals(unitName)) {
    }
    else if (! "".equals(name)) {
      Set<Bean<?>> beans = getManager().getBeans(EntityManagerFactory.class,
                                                 new AnnotationLiteral<Any>() {});
      
      throw new ConfigException(location + L.l("name='{0}' is an unknown @PersistenceUnit.\n  {1}",
                                               name, beans));

    }
    else {
      throw new ConfigException(location + L.l("@PersistenceUnit cannot find any persistence contexts.  No JPA persistence-units have been deployed"));
    }

    // bindJndi(location, jndiName, bean);

    // return new ComponentValueGenerator(location, (AbstractBean) bean);
    
    BeanValueGenerator gen
      = new BeanValueGenerator(location, bean);

    return gen;
  }
}
