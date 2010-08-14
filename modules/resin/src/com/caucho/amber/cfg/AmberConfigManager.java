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

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.type.*;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Configuration for an entity bean
 */
public class AmberConfigManager {
  private static final L10N L = new L10N(AmberConfigManager.class);
  private static final Logger log
    = Logger.getLogger(AmberConfigManager.class.getName());

  private final AmberPersistenceUnit _persistenceUnit;
  
  // EntityType or MappedSuperclassType.
  HashMap<String, TypeConfig> _typeMap = new HashMap<String, TypeConfig>();

  private ArrayList<BaseConfigIntrospector> _pendingIntrospectorList
    = new ArrayList<BaseConfigIntrospector>();

  /**
   * Creates the introspector.
   */
  public AmberConfigManager(AmberPersistenceUnit persistenceUnit)
  {
    _persistenceUnit = persistenceUnit;
  }

  AmberPersistenceUnit getPersistenceUnit()
  {
    return _persistenceUnit;
  }

  /**
   * Introspects the type
   */
  public BeanType introspect(Class type)
  {
    TypeConfig typeConfig = _typeMap.get(type.getName());

    if (typeConfig != null)
      return typeConfig.getType();

    try {
      EntityIntrospector introspector
        = new EntityIntrospector(this);

      return introspector.introspect(type);
    } catch (SQLException e) {
      throw ConfigException.create(e);
    }
  }

  public void configure()
  {
    ArrayList<BaseConfigIntrospector> introspectorList
      = new ArrayList<BaseConfigIntrospector>(_pendingIntrospectorList);

    _pendingIntrospectorList.clear();

    for (BaseConfigIntrospector introspector : introspectorList) {
      introspector.configureLinks();
    }

    for (BaseConfigIntrospector introspector : introspectorList) {
      introspector.configureDependencies();
    }
  }

  void addType(Class type, TypeConfig typeConfig)
  {
    _typeMap.put(type.getName(), typeConfig);

    if (typeConfig.getIntrospector() != null)
      _pendingIntrospectorList.add(typeConfig.getIntrospector());
  }
}
