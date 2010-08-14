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

import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ValueGenerator;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.naming.*;
import com.caucho.util.L10N;

import javax.naming.*;
import javax.persistence.*;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;
import javax.rmi.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generator for the JavaEE JNDI Resources
 */
public class JavaeeResourceGenerator extends ValueGenerator {
  private static final Logger log
    = Logger.getLogger(JavaeeResourceGenerator.class.getName());
  private static final L10N L = new L10N(JavaeeResourceGenerator.class);
  
  private static HashMap<Class<?>,Class<?>> _primitiveTypeMap
    = new HashMap<Class<?>,Class<?>>();

  private final String _location;
  private final Class<?> _fieldType;
  private final Class<?> _type;
  private final String _jndiName;
  private final String _mappedName;
  private final String _beanName;

  private InjectManager _beanManager;
  private Bean<?> _bean;

  JavaeeResourceGenerator(String location,
                          Class<?> fieldType,
                          Class<?> type,
                          String jndiName,
                          String mappedName,
                          String beanName)
  {
    _beanManager = InjectManager.create();
    
    if (! fieldType.isAssignableFrom(type))
      type = fieldType;

    if (type.isPrimitive())
      type = _primitiveTypeMap.get(type);

    _location = location;
    _fieldType = fieldType;
    _type = type;
    _jndiName = jndiName;
    _mappedName = mappedName;
    _beanName = beanName;
  }

  /**
   * Returns the expected type
   */
  @Override
  public Class getType()
  {
    return _type;
  }

  /**
   * Creates the value.
   */
  public Object create()
  {
    Object value = Jndi.lookup(_jndiName);

    // XXX: can use lookup-link and store the proxy

    if (value != null)
      return value;

    Bean bean = _bean;

    if (_bean == null)
      bean = bind();

    CreationalContext cxt = _beanManager.createCreationalContext(bean);
    value = _beanManager.getReference(bean, bean.getBeanClass(), cxt);

    return value;
  }

  synchronized private Bean bind()
  {
    Bean bean = null;

    // ejb/0f92
    /*
    if (mappedName == null || "".equals(mappedName))
      mappedName = jndiName;
    */

    _bean = bind(_location, _type, _mappedName);

    if (_bean != null) {
      bindJndi(_location, _jndiName, _bean);

      return _bean;
    }

    if (_bean == null && _beanName != null && ! "".equals(_beanName)) {
      _bean = bind(_location, _type, _beanName);
      
      if (_bean != null) {
        bindJndi(_location, _jndiName, _bean);

        return _bean;
      }
    }

    if (_bean == null && _jndiName != null && ! "".equals(_jndiName)) {
      _bean = bind(_location, _type, _jndiName);

      if (_bean != null) {
        bindJndi(_location, _jndiName, _bean);

        return _bean;
      }
    }

    if (_bean == null)
      _bean = bind(_location, _type);

    if (_bean != null) {
      bindJndi(_location, _jndiName, _bean);

      return _bean;
    }

    else
      throw new ConfigException(_location + L.l("{0} with mappedName={1}, beanName={2}, and jndiName={3} does not match anything",
                                             _type.getName(),
                                             _mappedName,
                                             _beanName,
                                             _jndiName));
  }

  private Object getJndiValue(Class type)
  {
    if (_jndiName == null || "".equals(_jndiName))
      return null;

    try {
      Object value = Jndi.lookup(_jndiName);

      if (value != null)
        return PortableRemoteObject.narrow(value, type);
      else
        return null;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  public Bean bind(String location, Class type)
  {
    return bind(location, type, null);
  }

  public Bean bind(String location, Class type, String name)
  {
    InjectManager webBeans = _beanManager;

    Set<Bean<?>> beans = null;

    if (name != null)
      beans = webBeans.getBeans(type, Names.create(name));

    if (beans != null && beans.size() != 0)
      return webBeans.resolve(beans);

    beans = webBeans.getBeans(type, new AnnotationLiteral<Any>() {});

    if (beans == null || beans.size() == 0)
      return null;

    for (Bean bean : beans) {
      // XXX: dup

      if (name == null || name.equals(bean.getName()))
        return bean;
    }

    return null;
  }

  private static void bindJndi(String location, String name, Object value)
  {
    try {
      if (! "".equals(name))
        Jndi.bindDeepShort(name, value);
    } catch (NamingException e) {
      throw new ConfigException(location + e.getMessage(), e);
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(_type.getName());

    if (_mappedName != null) {
      sb.append(", mappedName=");
      sb.append(_mappedName);
    }

    if (_jndiName != null) {
      sb.append(", jndiName=");
      sb.append(_jndiName);
    }

    sb.append("]");

    return sb.toString();
  }

  static {
    _primitiveTypeMap.put(boolean.class, Boolean.class);
    _primitiveTypeMap.put(byte.class, Byte.class);
    _primitiveTypeMap.put(char.class, Character.class);
    _primitiveTypeMap.put(short.class, Short.class);
    _primitiveTypeMap.put(int.class, Integer.class);
    _primitiveTypeMap.put(long.class, Long.class);
    _primitiveTypeMap.put(float.class, Float.class);
    _primitiveTypeMap.put(double.class, Double.class);
  }
}
