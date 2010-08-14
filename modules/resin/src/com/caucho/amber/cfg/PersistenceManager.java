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

package com.caucho.amber.cfg;

import com.caucho.amber.manager.*;
import com.caucho.config.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.env.jpa.ListenerPersistenceEnvironment;
import com.caucho.loader.*;
import com.caucho.util.*;

import java.util.logging.*;
import javax.sql.*;
import javax.annotation.*;

/**
 * Configures the persistence for a level.
 */
public class PersistenceManager
{
  private static final L10N L = new L10N(PersistenceManager.class);
  protected static final Logger log
    = Logger.getLogger(PersistenceManager.class.getName());

  private AmberContainer _amberManager;

  /**
   * Create a persistence manager
   */
  public PersistenceManager()
    throws ConfigException
  {
    _amberManager = AmberContainer.create();
  }

  public void setDataSource(DataSource dataSource)
  {
    _amberManager.setDataSource(dataSource);
  }

  public void addPersistenceUnitDefault(ContainerProgram program)
  {
    _amberManager.addPersistenceUnitDefault(program);
  }

  public void addPersistenceUnit(PersistenceUnitProxy proxy)
  {
    _amberManager.addPersistenceUnitProxy(proxy.getName(),
                                          proxy.getProgramList());
  }

  @PostConstruct
  public void init()
  {
    Environment.addChildLoaderListener(new ListenerPersistenceEnvironment());
  }
}

