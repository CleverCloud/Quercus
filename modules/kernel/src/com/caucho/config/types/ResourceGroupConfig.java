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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.spi.CreationalContext;
import javax.naming.NamingException;

import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ResourceInjectionTargetProgram;
import com.caucho.config.program.ResourceProgram;
import com.caucho.naming.Jndi;
import com.caucho.naming.ObjectProxy;
import com.caucho.util.L10N;

/**
 * Configuration for the resource group
 */
abstract public class ResourceGroupConfig extends ConfigProgram
  implements ObjectProxy
{
  private static final Logger log 
    = Logger.getLogger(ResourceGroupConfig.class.getName());
  private static final L10N L = new L10N(ResourceGroupConfig.class);
  
  private String _location = "";

  private String _defaultInjectionClass;

  private ArrayList<InjectionTarget> _injectionTargets
    = new ArrayList<InjectionTarget>();
  
  private String _lookupName;
  
  private boolean _isProgram;
  
  private ClassLoader _jndiClassLoader;

  public ResourceGroupConfig()
  {
  }

  public void setDefaultInjectionClass(String className)
  {
    _defaultInjectionClass = className;
  }

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
  }
  
  /**
   * Adds an injection-target
   */
  public void addInjectionTarget(InjectionTarget injectionTarget)
  {
    _injectionTargets.add(injectionTarget);
  }
  
  /**
   * Sets the lookup-name 
   */
  public void setLookupName(String lookupName)
  {
    _lookupName = lookupName;
  }
  
  public String getLookupName()
  {
    return _lookupName;
  }
  
  public void setProgram(boolean isProgram)
  {
    _isProgram = isProgram;
  }
  
  public boolean isProgram()
  {
    return _isProgram;
  }
  
  public void setJndiClassLoader(ClassLoader loader)
  {
    _jndiClassLoader = loader;
  }
  
  public ClassLoader getJndiClassLoader()
  {
    return _jndiClassLoader;
  }


  /**
   * Registers any injection targets
   */
  @PostConstruct
  public void init()
    throws Exception
  {
    if (! isProgram())
      deploy();
  }
  
  public void deploy()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    InjectManager cdiManager = InjectManager.getCurrent();
    
    for (InjectionTarget target : _injectionTargets) {
      String targetClassName = target.getInjectionTargetClass();
      String targetMethod = target.getInjectionTargetName();
      
      try {
        Class<?> targetClass = Class.forName(targetClassName, false, loader);
        
        ResourceInjectionTargetProgram resourceProgram
            = new ResourceInjectionTargetProgram(this, 
                                                 targetClass,
                                                 targetMethod);
        
        cdiManager.getResourceManager().addResource(resourceProgram);
        
        if (getJndiClassLoader() != null)
          thread.setContextClassLoader(getJndiClassLoader());
        
        String jndiName = "java:comp/env/" + targetClassName + "/" + targetMethod;

        Jndi.bindDeep(jndiName, this);
      } catch (ConfigException e) {
        throw e;
      } catch (Exception e) {
        throw new ConfigException(L.l("'{0}' is an unknown class in {1}",
                                      targetClassName, this),
                                  e);
      } finally {
        thread.setContextClassLoader(loader);
      }
    }
  }
  
  public ConfigProgram getProgram()
  {
    return new ResourceProgram(this);
  }
  
  public ConfigProgram getProgram(Class<?> cl)
  {
    throw new IllegalStateException();
  }
  
  protected Class<?> inferTypeFromInjection()
  {
    for (InjectionTarget target : _injectionTargets) {
      try {
        String className = target.getInjectionTargetClass();
        String name = target.getInjectionTargetName();
        
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        
        Class<?> cl = Class.forName(className, false, loader);
        
        Class<?> type = findProperty(cl, name);
        
        if (type != null)
          return type;
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
    
    return null;
  }
  
  private Class<?> findProperty(Class<?> cl, String name)
  {
    if (cl == null)
      return null;
    
    for (Field field : cl.getDeclaredFields()) {
      if (name.equals(field.getName()))
        return field.getType();
    }
    
    return null;
  }
  
  /**
   * Configures the bean using the current program.
   * 
   * @param bean the bean to configure
   * @param env the Config environment
   */
  public <T> void inject(T bean, CreationalContext<T> env)
  {
  }

  @Override
  public Object createObject(Hashtable<?,?> env)
    throws NamingException
  {
    Object value = getValue();
    
    return value;
  }
  
  public Object getValue()
  {
    return null;
  }
  
  protected ConfigException error(String msg)
  {
    if (_location != null)
      return new LineConfigException(_location + msg);
    else
      return new ConfigException(msg);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _location + "]";
  }
}

