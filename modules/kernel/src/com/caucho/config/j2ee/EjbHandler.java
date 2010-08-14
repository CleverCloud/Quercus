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

import javax.ejb.EJB;
import javax.ejb.EJBs;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.BeanValueGenerator;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.FieldGeneratorProgram;
import com.caucho.config.program.MethodGeneratorProgram;
import com.caucho.config.program.NullProgram;
import com.caucho.config.program.ValueGenerator;
import com.caucho.util.L10N;

/**
 * Handles the @EJB annotation for JavaEE
 */
public class EjbHandler extends JavaeeInjectionHandler {
  private static final L10N L = new L10N(EjbHandler.class);
  
  public EjbHandler(InjectManager manager)
  {
    super(manager);
  }
  
  @Override
  public ConfigProgram introspectField(AnnotatedField<?> field)
  {
    EJB ejb = field.getAnnotation(EJB.class);
    
    return generateContext(field, ejb);
  }
  
  @Override
  public ConfigProgram introspectMethod(AnnotatedMethod<?> method)
  {
    EJB ejb = method.getAnnotation(EJB.class);
    
    return generateContext(method, ejb);
  }

  @Override
  public ConfigProgram introspectType(AnnotatedType<?> type)
  {
    // ejb/123j
    for (Class<?> parentClass = type.getJavaClass().getSuperclass();
         parentClass != null;
         parentClass = parentClass.getSuperclass()) {
      EJBs ejbs = parentClass.getAnnotation(EJBs.class);

      if (ejbs != null) {
        for (EJB ejb : ejbs.value()) {
          introspectClass(getClass().getName(), ejb);
        }
      }

      EJB ejb = parentClass.getAnnotation(EJB.class);

      if (ejb != null)
        introspectClass(getClass().getName(), ejb);
    }
    
    EJBs ejbs = type.getAnnotation(EJBs.class);

    if (ejbs != null) {
      for (EJB ejb : ejbs.value()) {
        introspectClass(getClass().getName(), ejb);
      }
    }

    EJB ejb = type.getAnnotation(EJB.class);

    if (ejb != null)
      introspectClass(getClass().getName(), ejb);
    

    return new NullProgram();
  }

  private void introspectClass(String location, EJB ejb)
  {
    String name = ejb.name();

    Class<?> bindType = ejb.beanInterface();
    
    if ("".equals(name))
      throw new ConfigException(L.l("{0}: @EJB name() attribute is required for @EJB on a class.",
                                    location));
    
    if (Object.class.equals(bindType))
      throw new ConfigException(L.l("{0}: @EJB beanInterface() attribute is required for @EJB on a class.",
                                    location));
    
    ValueGenerator gen = bindGenerator(location, ejb, bindType);

    if (name != null && ! "".equals(name)) {
      bindJndi(name, gen, name);
    }
    
  }

  private ConfigProgram generateContext(AnnotatedField<?> field,
                                        EJB ejb)
    throws ConfigException
  {
    String name = ejb.name();

    Field javaField = field.getJavaMember();
    
    String location = getLocation(javaField);

    Class<?> bindType = javaField.getType();
    
    ValueGenerator gen = bindGenerator(location, ejb, bindType);

    if (name != null && ! "".equals(name))
      bindJndi(name, gen, name);
    
    bindJndi(javaField, gen);
    
    return new FieldGeneratorProgram(javaField, gen);
  }
  

  private ConfigProgram generateContext(AnnotatedMethod<?> method,
                                        EJB ejb)
    throws ConfigException
  {
    String name = ejb.name();

    Method javaMethod = method.getJavaMember();
    
    String location = getLocation(javaMethod);

    Class<?> bindType = javaMethod.getParameterTypes()[0];
    
    // ejb/2005
    if (! Object.class.equals(ejb.beanInterface())) {
      bindType = ejb.beanInterface();
    }
    
    ValueGenerator gen = bindGenerator(location, ejb, bindType);

    if (name != null && ! "".equals(name))
      bindJndi(name, gen, name);
    
    return new MethodGeneratorProgram(javaMethod, gen);
  }
  
  protected ValueGenerator bindGenerator(String location,
                                         EJB ejb,
                                         Class<?> bindType)
  {
    String name = ejb.name();
    String mappedName = ejb.mappedName();
    String beanName = ejb.beanName();
    String lookupName = ejb.lookup();

    // ejb/2005
    if (! Object.class.equals(ejb.beanInterface())) {
      bindType = ejb.beanInterface();
    }
    
    if (lookupName != null && ! "".equals(lookupName)) {
      return new JndiValueGenerator(location, bindType, lookupName);
    }
    
    /*
    if (! "".equals(pContext.name()))
      jndiName = pContext.name();
      */

    Bean<?> bean = null;

    if (! "".equals(beanName))
      bean = bind(location, bindType, new BeanNameLiteral(beanName));
    
    if (bean == null)
      bean = bind(location, bindType, name);
    
    if (bean == null)
      bean = bind(location, bindType, mappedName);

    if (bean != null) {
      // valid bean
    }
    else if (! "".equals(name)) {
      throw new ConfigException(location + L.l("name='{0}' is an unknown @EJB.",
                                               name));
    }
    else if (! "".equals(mappedName)) {
      throw new ConfigException(location + L.l("mappedName='{0}' is an unknown @EJB.",
                                               mappedName));

    }
    else {
      throw new ConfigException(location + L.l("@EJB cannot find any defined EJBs.  No @EJB with type='{0}'",
                                               bindType));
    }

    return new BeanValueGenerator(location, bean);
  }
}
