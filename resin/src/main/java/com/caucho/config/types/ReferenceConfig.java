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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.types;

import com.caucho.config.ConfigException;
import com.caucho.config.program.ContainerProgram;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Configuration for the init-param pattern.
 */
public class ReferenceConfig {
  private static L10N L = new L10N(ReferenceConfig.class);

  private String _name;
  private Class _factory;
  private String _description;

  private ContainerProgram _init;
  private HashMap<String,String> _params;
  
  private ObjectFactory _objectFactory;

  /**
   * Sets the name
   */
  public void setJndiName(String name)
  {
    _name = name;
  }

  /**
   * Gets the name
   */
  public String getJndiName()
  {
    return _name;
  }

  /**
   * Gets the object factory;
   */
  public Class getFactory()
  {
    return _factory;
  }

  /**
   * Sets the object factory;
   */
  public void setFactory(Class factory)
  {
    _factory = factory;
  }

  /**
   * Sets the init program
   */
  public void setInit(ContainerProgram init)
  {
    _init = init;
  }

  /**
   * Gets the init program;
   */
  public ContainerProgram getInit()
  {
    return _init;
  }

  /**
   * Sets an init param.
   */
  public void addInitParam(InitParam initParam)
  {
    if (_params == null)
      _params = new HashMap<String,String>();
    
    _params.putAll(initParam.getParameters());
  }

  /**
   * Initialize the resource.
   */
  @PostConstruct
  public void init()
    throws Exception
  {
    Object obj = null;
    
    if (_factory == null) {
      throw new ConfigException(L.l("<reference> configuration need a <factory>.  The <factory> is the class name of the resource bean."));
    }
    else if (ObjectFactory.class.isAssignableFrom(_factory)) {
      Reference ref;

      if (_init != null) {
        throw new ConfigException(L.l("<init> is not allowed for object factories.  A <resource> with a <factory> must only have <init-param> configuration."));
      }

      String factoryName = _factory.getName();
      ref = new Reference(factoryName, factoryName, null);

      if (_params != null) {
        ArrayList<String> names = new ArrayList<String>(_params.keySet());
        Collections.sort(names);

        for (int i = 0; i < names.size(); i++) {
          String name = names.get(i);
          String value = _params.get(name);

          ref.add(new StringRefAddr(name, value));
        }
      }

      obj = ref;
    }
    else {
      throw new ConfigException(L.l("`{0}' must implement ObjectFactory.  <factory> classes in <resource> must implement ObjectFactory.", _factory.getName()));
    }

    if (_name.startsWith("java:comp"))
      Jndi.bindDeep(_name, obj);
    else
      Jndi.bindDeep("java:comp/env/" + _name, obj);
  }

  protected void configure(Object obj)
    throws Throwable
  {
    if (_init != null)
      _init.init(obj);
  }

  public String toString()
  {
    return "Resource[" + _name + "]";
  }
}

