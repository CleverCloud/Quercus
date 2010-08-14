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

package com.caucho.env.jpa;

import java.net.URL;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.util.L10N;

/**
 * <jpa-persistence-unit> tag in the resin-web.xml
 */
public class ConfigJpaPersistenceUnit {
  private static final L10N L = new L10N(ConfigJpaPersistenceUnit.class);
  
  private String _name;
  private URL _root;
  
  private ContainerProgram _program = new ContainerProgram();
  
  /**
   * Returns the unit name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the unit name.
   */
  public void setName(String name)
  {
    _name = name;
  }
  
  /**
   * Sets the path to the unit
   */
  public void setPath(URL root)
  {
    _root = root;
  }
  
  /**
   * Returns the path to the unit
   */
  public URL getPath()
  {
    return _root;
  }
  
  public void addBuilderProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }
  
  public ContainerProgram getProgram()
  {
    return _program;
  }
  
  @PostConstruct
  public void init()
  {
    if (_name == null)
      throw new ConfigException(L.l("jpa-persistence-unit requires a 'name' attribute"));
    
    PersistenceManager manager = PersistenceManager.create();

    manager.addPersistenceUnit(_name, this);
  }
}
