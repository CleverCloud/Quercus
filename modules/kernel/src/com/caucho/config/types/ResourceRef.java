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

package com.caucho.config.types;

import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.DefaultLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.Names;
import com.caucho.el.Expr;
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.naming.Jndi;
import com.caucho.naming.ObjectProxy;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.spi.Bean;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for the init-param pattern.
 */
public class ResourceRef extends ResourceGroupConfig
  implements Validator, ObjectProxy {
  private static Logger log = Logger.getLogger(ResourceRef.class.getName());
  private static L10N L = new L10N(ResourceRef.class);

  private String _location = "";

  private String _name;
  private Class<?> _type;
  private String _description;
  private boolean _sharing;

  private ConfigProgram _init;
  private HashMap<String,String> _params = new HashMap<String,String>();
  
  private Bean<?> _bean;
  private Object _value;

  private InjectionTarget _injectionTarget;

  /**
   * Sets the id
   */
  public void setId(String id)
  {
  }

  /**
   * Sets the configuration location.
   */
  public void setConfigLocation(String filename, int line)
  {
    _location = filename + ":" + line + " ";
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Sets the injection-target
   */
  public void setInjectionTarget(InjectionTarget injectionTarget)
  {
    _injectionTarget = injectionTarget;
  }

  /**
   * Sets the name
   */
  public void setResRefName(String name)
  {
    _name = name;
  }

  /**
   * Gets the name
   */
  public String getResRefName()
  {
    return _name;
  }

  /**
   * Sets the type
   */
  public void setResType(Class<?> type)
  {
    _type = type;
  }

  /**
   * Sets the auth
   */
  public void setResAuth(String auth)
  {
  }

  /**
   * Sets the sharing scope
   */
  public void setResSharingScope(String share)
  {
  }

  /**
   * Sets the type
   */
  public void setClassName(Class<?> type)
  {
    _type = type;
  }

  /**
   * Gets the injection-target
   */
  public InjectionTarget getInjectionTarget()
  {
    return _injectionTarget;
  }

  /**
   * Gets the type;
   */
  public Class<?> getResType()
  {
    return _type;
  }

  /**
   * Sets the init program
   */
  public void setInit(ConfigProgram init)
  {
    _init = init;
  }

  /**
   * Gets the init program;
   */
  public ConfigProgram getInit()
  {
    return _init;
  }

  /**
   * Sets an init-parameter
   */
  public void setInitParam(InitParam initParam)
  {
    _params.putAll(initParam.getParameters());
  }

  /**
   * Initialize the resource.
   */
  @PostConstruct
  public void init()
    throws Exception
  {
    super.init();
    
    if (_init == null && _params.size() == 0) {
      return;
    }

    try {
      Class<?> cl = _type;

      if (javax.sql.DataSource.class.equals(_type))
        cl = Class.forName("com.caucho.sql.DBPool");

      Object obj = cl.newInstance();

      if (_init != null)
        _init.configure(obj);

      Iterator iter = _params.keySet().iterator();
      while (iter.hasNext()) {
        String key = (String) iter.next();
        String value = (String) _params.get(key);

        Config.setAttribute(obj, key, value);
      }

      if (obj instanceof ClassLoaderListener) {
        ClassLoaderListener listener = (ClassLoaderListener) obj;

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (; loader != null; loader = loader.getParent()) {
          if (loader instanceof EnvironmentClassLoader) {
            ((DynamicClassLoader) loader).addListener(listener);
            break;
          }
        }
      }
      
      _value = obj;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  @Override
  public void deploy()
  {
    super.deploy();
    
    try {
      Object value = Jndi.lookup(_name);
      
      if (value != null)
        return;
    } catch (Exception e) {
      log.log(Level.ALL, e.toString(), e);
    }
    
    if (_value == null && getLookupName() == null) {
      InjectManager cdiManager = InjectManager.getCurrent();
      
      Set<Bean<?>> beans = cdiManager.getBeans(_type);
      
      _bean = cdiManager.resolve(beans);
    }

    
    try {
      Jndi.bindDeepShort(_name, this);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
 
  @Override
  public Object getValue()
  {
    Object value;
   
    if (_value != null)
      value = _value;
    else if (getLookupName() != null)
      return Jndi.lookup(getLookupName());
    else {
      InjectManager cdiManager = InjectManager.getCurrent();
      
      value = cdiManager.getReference(_bean);
    }
    
    return value;
  }

  /**
   * Validates the resource-ref, i.e. checking that it exists in
   * JNDI.
   */
  @Override
  public void validate()
    throws ConfigException
  {
    Object obj = getValue();

    try {
      obj = new InitialContext().lookup("java:comp/env/" + _name);
    } catch (NamingException e) {
      log.log(Level.FINEST, e.toString(), e);
    }

    if (obj == null)
      throw error(L.l("resource-ref '{0}' was not configured.  All resources defined by <resource-ref> tags must be defined in a configuration file.",
                      _name));
  }

  public ConfigException error(String msg)
  {
    if (_location != null)
      return new LineConfigException(_location + msg);
    else
      return new ConfigException(msg);
  }

  public String toString()
  {
    return "ResourceRef[" + _name + "]";
  }
}
