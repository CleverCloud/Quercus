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

package com.caucho.ejb.cfg;

import java.util.ArrayList;

import javax.annotation.PostConstruct;

import com.caucho.config.DependencyBean;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.vfs.PersistentDependency;

/**
 * Proxy for an ejb bean configuration.  This proxy is needed to handle
 * the merging of ejb definitions.
 */
public class EjbBeanConfigProxy implements DependencyBean {
  private final EjbConfig _config;
  private final String _ejbModuleName;

  private String _ejbName;
  private Class<?> _ejbClass;

  private String _filename = "";
  private String _location = "";
  
  // The configuration program
  private ContainerProgram _program = new ContainerProgram();
  
  ArrayList<PersistentDependency> _dependList =
    new ArrayList<PersistentDependency>();
  
  /**
   * Creates a new entity bean configuration.
   */
  public EjbBeanConfigProxy(EjbConfig config, String ejbModuleName)
  {
    _config = config;
    _ejbModuleName = ejbModuleName;
  }

  /**
   * Returns the configuration.
   */
  public EjbConfig getConfig()
  {
    return _config;
  }

   public String getEJBModuleName()
  {
    return _ejbModuleName;
  }

  /**
   * Sets the location
   */
  public void setConfigLocation(String filename, int line)
  {
    _filename = filename;
    _location = filename + ":" + line + ": ";
  }

  /**
   * Gets the location
   */
  public String getLocation()
  {
    return _location;
  }

  /**
   * Gets the filename
   */
  public String getFilename()
  {
    return _filename;
  }
    
  /**
   * Sets the ejbName
   */
  public void setEjbName(String ejbName)
  {
    _ejbName = ejbName;
  }

  /**
   * Gets the ejbName
   */
  public String getEjbName()
  {
    return _ejbName;
  }
  
  /**
   * Sets the ejb-class
   */
  public void setEjbClass(Class<?> ejbClass)
  {
    _ejbClass = ejbClass;
  }
  
  /**
   * Gets the ejb-class
   */
  public Class<?> getEjbClass()
  {
    return _ejbClass;
  }

  /**
   * Add a dependency.
   */
  @Override
  public void addDependency(PersistentDependency depend)
  {
    if (! _dependList.contains(depend))
      _dependList.add(depend);
  }

  /**
   * Gets the depend list.
   */
  public ArrayList<PersistentDependency> getDependencyList()
  {
    return _dependList;
  }

  /**
   * Adds to the builder program.
   */
  public void addBuilderProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }

  /**
   * Returns the program.
   */
  public ConfigProgram getBuilderProgram()
  {
    return _program;
  }

  /**
   * Initializes and configures the entity bean.
   */
  @PostConstruct
  public void init()
  {
    getConfig().addConfigProxy(this);
  }
  
  public void configure()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
