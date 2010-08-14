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

package com.caucho.sql;

import com.caucho.config.program.ContainerProgram;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.*;
import com.caucho.loader.*;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

/**
 * The configuration database-default.
 */
public class DatabaseConfig {
  private final static Logger log
    = Logger.getLogger(DatabaseConfig.class.getName());

  private final static
    EnvironmentLocal<ArrayList<DatabaseConfig>> _databaseDefault
    = new EnvironmentLocal<ArrayList<DatabaseConfig>>();

  // The configuration program
  private ContainerProgram _program = new ContainerProgram();

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

  @PostConstruct
  public void init()
  {
    ArrayList<DatabaseConfig> defaultList = _databaseDefault.getLevel();

    if (defaultList == null) {
      defaultList = new ArrayList<DatabaseConfig>();
      _databaseDefault.set(defaultList);
    }

    defaultList.add(this);
  }

  static void configDefault(DBPool pool)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    configDefault(pool, loader);
  }

  static void configDefault(DBPool pool, ClassLoader loader)
  {
    if (loader != null)
      configDefault(pool, loader.getParent());
    
    ArrayList<DatabaseConfig> defaultList = _databaseDefault.getLevel(loader);

    if (defaultList != null) {
      for (int i = 0; i < defaultList.size(); i++) {
        DatabaseConfig config = defaultList.get(i);

        config.getBuilderProgram().configure(pool);
      }
    }
  }
}
