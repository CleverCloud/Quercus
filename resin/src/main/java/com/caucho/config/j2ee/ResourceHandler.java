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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.Resources;
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
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

/**
 * Handles the @Resource annotation for JavaEE
 */
public class ResourceHandler extends JavaeeInjectionHandler {
  private static final L10N L = new L10N(ResourceHandler.class);
  private static final Logger log 
    = Logger.getLogger(ResourceHandler.class.getName());
  private static final HashMap<Class<?>,Class<?>> _boxingMap
    = new HashMap<Class<?>,Class<?>>();
  
  private static final Method _lookupMethod;
  
  public ResourceHandler(InjectManager manager)
  {
    super(manager);
  }
  
  @Override
  public ConfigProgram introspectField(AnnotatedField<?> field)
  {
    Resource resource = field.getAnnotation(Resource.class);
    
    Field javaField = field.getJavaMember();
    
    String loc = getLocation(javaField);
    
    String jndiName = javaField.getDeclaringClass().getName() + "/" + javaField.getName();
    Class<?> bindType = javaField.getType();
    
    ValueGenerator gen = generateContext(loc, bindType, jndiName, resource);
    
    bindJndi(javaField, gen);
    
    return new FieldGeneratorProgram(field.getJavaMember(), gen);
  }
  
  @Override
  public ConfigProgram introspectMethod(AnnotatedMethod<?> method)
  {
    Resource resource = method.getAnnotation(Resource.class);
    
    Method javaMethod = method.getJavaMember();
    
    String loc = getLocation(method.getJavaMember());
    
    String jndiName = (javaMethod.getDeclaringClass().getName()
                       + "/" + javaMethod.getName());
    
    Class<?> bindType = javaMethod.getParameterTypes()[0];
    
    ValueGenerator gen = generateContext(loc, bindType, jndiName, resource);
    
    bindJndi(javaMethod, gen);
    
    return new MethodGeneratorProgram(method.getJavaMember(), gen);
  }

  @Override
  public ConfigProgram introspectType(AnnotatedType<?> type)
  {
    for (Class<?> parentClass = type.getJavaClass().getSuperclass();
         parentClass != null;
         parentClass = parentClass.getSuperclass()) {
      Resources resources = parentClass.getAnnotation(Resources.class);

      if (resources != null) {
        for (Resource resource : resources.value()) {
          introspectClass(getClass().getName(), resource);
        }
      }

      Resource resource = parentClass.getAnnotation(Resource.class);

      if (resource != null)
        introspectClass(getClass().getName(), resource);
    }
    
    Resources resources = type.getAnnotation(Resources.class);

    if (resources != null) {
      for (Resource resource : resources.value()) {
        introspectClass(getClass().getName(), resource);
      }
    }

    Resource resource = type.getAnnotation(Resource.class);

    if (resource != null)
      introspectClass(getClass().getName(), resource);
    

    return new NullProgram();
  }

  private void introspectClass(String location, Resource resource)
  {
    String name = resource.name();

    Class<?> bindType = resource.type();
    
    if ("".equals(name))
      throw new ConfigException(L.l("{0}: @Resource name() attribute is required for @Resource on a class.",
                                    location));
    
    if (Object.class.equals(bindType))
      throw new ConfigException(L.l("{0}: @Resource beanInterface() attribute is required for @Resource on a class.",
                                    location));
    
    ValueGenerator gen = generateContext(location, bindType, null, resource);

    if (name != null && ! "".equals(name)) {
      bindJndi(name, gen, name);
    }
  }

  private ValueGenerator generateContext(String loc,
                                         Class<?> bindType,
                                         String fullJndiName,
                                         Resource resource)
    throws ConfigException
  {
    String name = resource.name();
    String mappedName = resource.mappedName();
    String lookupName = null;
    
    if (_lookupMethod != null) {
      try {
        lookupName = (String) _lookupMethod.invoke(resource);
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
    
    if (! resource.type().equals(Object.class)
        && ! resource.type().equals(void.class)) {
      bindType = resource.type();
    }

    ValueGenerator gen = null;

    if (lookupName != null && ! "".equals(lookupName))
      gen = new JndiValueGenerator(loc, bindType, lookupName);
    else
      gen = lookupJndi(loc, bindType, name);
    
    if (gen != null) {
      if (fullJndiName != null)
        bindJndi(null, gen, fullJndiName);
    }
    else {
      gen = bindValueGenerator(loc, bindType, name, mappedName);
      
      bindJndi(name, gen, fullJndiName);
    }
    
    return gen;
  }
  
  private ValueGenerator lookupJndi(String loc,
                                    Class<?> bindType,
                                    String lookupName)
  {
    if (lookupName == null || "".equals(lookupName))
      return null;
    
    if (! lookupName.startsWith("java:") && ! lookupName.startsWith("/"))
      lookupName = "java:comp/env/" + lookupName;
    
    Object value = Jndi.lookup(lookupName);

    if (value != null)
      return new JndiValueGenerator(loc, bindType, lookupName);
    
    return null;
  }

  private ValueGenerator bindValueGenerator(String location, 
                                            Class<?> bindType,
                                            String name,
                                            String mappedName)
  {
    Class<?> boxedType = _boxingMap.get(bindType);
  
    if (boxedType != null)
      bindType = boxedType;
  
    /*
    if (! "".equals(pContext.name()))
      jndiName = pContext.name();
     */

    Bean<?> bean;
  
    bean = bind(location, bindType, name);
  
    if (bean == null)
      bean = bind(location, bindType, mappedName);

    if (bean != null) {
      // valid bean
    }
    else if (! "".equals(name)) {
      throw new ConfigException(location + L.l("name='{0}' is an unknown @Resource.",
                                               name));
    }
    else if (! "".equals(mappedName)) {
      throw new ConfigException(location + L.l("mappedName='{0}' is an unknown @Resource.",
                                               mappedName));
    }
    else {
      throw new ConfigException(location + L.l("@Resource cannot find any matching resources with type='{0}' in {1}",
                                               bindType, getManager()));
    }

    // bindJndi(location, jndiName, bean);

    // return new ComponentValueGenerator(location, (AbstractBean) bean);
  
    return new BeanValueGenerator(location, bean);
  }
  
  static {
    _boxingMap.put(boolean.class, Boolean.class);
    _boxingMap.put(char.class, Character.class);
    _boxingMap.put(byte.class, Byte.class);
    _boxingMap.put(short.class, Short.class);
    _boxingMap.put(int.class, Integer.class);
    _boxingMap.put(long.class, Long.class);
    _boxingMap.put(float.class, Float.class);
    _boxingMap.put(double.class, Double.class);
    
    Method lookupMethod = null;
    
    try {
      lookupMethod = Resource.class.getMethod("lookup");
    } catch (Exception e) {
      log.log(Level.ALL, e.toString(), e);
    }
    
    _lookupMethod = lookupMethod;
  }
}
