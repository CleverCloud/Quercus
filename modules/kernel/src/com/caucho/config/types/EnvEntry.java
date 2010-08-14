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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.types;

import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.spi.CreationalContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.DefaultLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.el.Expr;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

/**
 * Configuration for the env-entry pattern.
 */
public class EnvEntry extends ResourceGroupConfig implements Validator {
  private static final L10N L = new L10N(EnvEntry.class);
  private static final Logger log = Logger.getLogger(EnvEntry.class.getName());

  private String _name;
  private Class<?> _type;
  private String _value;
  private Object _objectValue;
  
  public EnvEntry()
  {
  }

  @Override
  public void setId(String id)
  {
  }

  /**
   * Sets the env-entry-name
   */
  public void setEnvEntryName(String name)
  {
    _name = name;
  }

  /**
   * Gets the env-entry-name
   */
  public String getEnvEntryName()
  {
    return _name;
  }

  /**
   * Sets the env-entry-type
   */
  public void setEnvEntryType(Class<?> type)
  {
    _type = type;
  }

  /**
   * Gets the env-entry-type
   */
  public Class<?> getEnvEntryType()
  {
    return _type;
  }

  /**
   * Sets the env-entry-value
   */
  public void setEnvEntryValue(RawString value)
  {
    _value = value.getValue();
  }

  /**
   * Gets the env-entry-value
   */
  public String getEnvEntryValue()
  {
    return _value;
  }

  /**
   * Gets the env-entry-value
   */
  // XXX: ejb/0fd0 vs ejb/0g03
  @PostConstruct
  public void init()
    throws Exception
  {
    if (_name == null)
      throw new ConfigException(L.l("env-entry needs 'env-entry-name' attribute"));
    
    /*
    if (_type == null)
      throw new ConfigException(L.l("env-entry needs 'env-entry-type' attribute"));
      */

    super.init();

    // actually, should register for validation
    /*
    if (_value == null)
      return;
      */
    
    
    if (! isProgram())
      deploy();
  }
  
  /**
   * Configures the bean using the current program.
   * 
   * @param bean the bean to configure
   * @param env the Config environment
   */
  @Override
  public <T> void inject(T bean, CreationalContext<T> env)
  {
    
  }
  
  @Override
  public Object getValue()
  {
    if (getLookupName() != null) {
      try {
        return Jndi.lookup(getLookupName());
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
    
    if (_objectValue == null)
      deploy();
    /*
    if (_objectValue == null)
      throw new NullPointerException(toString());
    */
    return _objectValue;
  }

  @Override
  public void deploy()
  {
    if (_objectValue != null)
      return;
    
    super.deploy();

    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    try {
      if (getJndiClassLoader() != null)
        thread.setContextClassLoader(getJndiClassLoader());
      
      Jndi.bindDeepShort(_name, this);
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      thread.setContextClassLoader(loader);
    }
    
    if (_value == null)
      return;
    
    LinkedHashSet<Type> types = new LinkedHashSet<Type>();
    
    Class<?> type = _type;
    Object value = _value;
    
    if (type == null)
      type = inferTypeFromInjection();
    
    if (type != null)
      types.add(type);
    else
      types.add(value.getClass());
    
    if (getLookupName() != null) {
    }
    else if (type == null) {
    }
    else if (type.equals(String.class)) {
    }
    else if (Boolean.class.equals(type) || boolean.class.equals(type)) {
      if (_value != null)
        value = new Boolean("true".equals(_value.toLowerCase()));
      else
        value = Boolean.FALSE;
      
      types.add(boolean.class);
    }
    else if (Byte.class.equals(type) || byte.class.equals(type)) {
      value = new Byte((byte) Expr.toLong(_value, null));
      
      types.add(byte.class);
    }
    else if (Short.class.equals(type) || short.class.equals(type)) {
      value = new Short((short) Expr.toLong(_value, null));
      
      types.add(short.class);
    }
    else if (Integer.class.equals(type) || int.class.equals(type)) {
      value = new Integer((int) Expr.toLong(_value, null));
      
      types.add(int.class);
    }
    else if (Long.class.equals(type) || long.class.equals(type)) {
      value = new Long(Expr.toLong(_value, null));
      
      types.add(long.class);
    }
    else if (Float.class.equals(type) || float.class.equals(type)) {
      value = new Float((float) Expr.toDouble(_value, null));
      
      types.add(float.class);
    }
    else if (Double.class.equals(type) || double.class.equals(type)) {
      value = new Double(Expr.toDouble(_value, null));
      
      types.add(double.class);
    }
    else if (Character.class.equals(type) || char.class.equals(type)) {
      String v = Expr.toString(_value, null);

      if (v == null || v.length() == 0)
        value = new Character(' ');
      else
        value = new Character(v.charAt(0));
      
      types.add(char.class);
    }
    else if (Enum.class.isAssignableFrom(type)) {
      value = Enum.valueOf((Class) type, _value);
    }
    else if (Class.class.isAssignableFrom(type)) {
      try {
        loader = Thread.currentThread().getContextClassLoader();

        value = Class.forName(_value, false, loader);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
    
    _objectValue = value;
    
    if (value == null)
      return;
    
    InjectManager cdiManager = InjectManager.create();
    BeanBuilder<?> builder = cdiManager.createBeanFactory(value.getClass());
    
    // CDI names can't have '.'
    if (_name.indexOf('.') < 0)
      builder.name(_name);
    
    // server/1516
    builder.qualifier(Names.create(_name));
    builder.qualifier(DefaultLiteral.DEFAULT);
    
    builder.type(types);

    cdiManager.addBean(builder.singleton(value));
  }

  /**
   * Validates the env-entry, i.e. checking that it exists in
   * JNDI.
   */
  @Override
  public void validate()
    throws ConfigException
  {
    Object obj = null;

    try {
      obj = new InitialContext().lookup("java:comp/env/" + _name);
    } catch (NamingException e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (obj == null)
      throw error(L.l("env-entry '{0}' was not configured.  All resources defined by <env-entry> tags must be defined in a configuration file.",
                      _name));
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}

