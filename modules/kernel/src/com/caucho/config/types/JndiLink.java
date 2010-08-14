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
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentListener;
import com.caucho.naming.Jndi;
import com.caucho.naming.LinkProxy;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.naming.spi.InitialContextFactory;
import java.util.Hashtable;

/**
 * Configuration for the init-param pattern.
 */
public class JndiLink {
  private static L10N L = new L10N(JndiLink.class);

  private String _name;
  private Class<?> _factoryClass;
  private String _description;

  private Hashtable _properties = new Hashtable();

  private ContainerProgram _init;

  /**
   * Sets the name
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the name
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the factory
   */
  public void setFactory(Class factory)
  {
    _factoryClass = factory;
  }

  /**
   * Gets the type;
   */
  public Class getFactory()
  {
    return _factoryClass;
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
   * Initialize the resource.
   */
  @PostConstruct
  public void init()
    throws Exception
  {
    if (_name == null)
      throw new ConfigException(L.l("<jndi-link> configuration needs a <name>.  The <name> is the JNDI name where the context will be linked."));
    
    Class factory = _factoryClass;

    if (factory == null)
      throw new ConfigException(L.l("<jndi-link> configuration need a <factory>.  The <factory> is the class name of the InitialContextFactory bean."));

    Object obj = factory.newInstance();

    /*
    configure(obj);

    TypeBuilderFactory.init(obj);
    */

    if (obj instanceof ClassLoaderListener) {
      ClassLoaderListener listener = (ClassLoaderListener) obj;

      Environment.addClassLoaderListener(listener);
    }

    if (obj instanceof EnvironmentListener) {
      EnvironmentListener listener = (EnvironmentListener) obj;

      Environment.addEnvironmentListener(listener);
    }

    Object proxy = new LinkProxy((InitialContextFactory) obj,
                                 _properties,
                                 null);

    if (_name.startsWith("java:comp"))
      Jndi.bindDeep(_name, proxy);
    else {
      Jndi.bindDeep("java:comp/env/" + _name, proxy);
    }
  }

  protected void configure(Object obj)
    throws Throwable
  {
    if (_init != null)
      _init.init(obj);
  }

  public String toString()
  {
    return "JndiLink[" + _name + "]";
  }
}

