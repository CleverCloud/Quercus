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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.cfg;

import com.caucho.amber.type.AbstractEnhancedType;
import com.caucho.bytecode.JClass;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.lang.annotation.Annotation;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Configuration for a mapped superclass type
 */
public class MappedSuperIntrospector extends BaseConfigIntrospector {
  private static final L10N L = new L10N(MappedSuperIntrospector.class);
  private static final Logger log
    = Logger.getLogger(MappedSuperIntrospector.class.getName());

  // HashMap<String, MappedSuperType> _mappedSuperMap
  // = new HashMap<String, MappedSuperType>();

  /**
   * Creates the introspector.
   */
  public MappedSuperIntrospector(AmberConfigManager manager)
  {
    super(manager);
  }

  /**
   * Returns true for mapped superclass type.
   */
  public boolean isMappedSuper(Class type)
  {
    getInternalMappedSuperclassConfig(type, _annotationCfg);
    Annotation mappedSuperAnn = _annotationCfg.getAnnotation();
    MappedSuperclassConfig mappedSuperConfig
      = _annotationCfg.getMappedSuperclassConfig();

    return (! _annotationCfg.isNull());
  }

  /**
   * Introspects.
   */
  //XXX: public MappedSuperType introspect(JClass type)
  public AbstractEnhancedType introspect(JClass type)
    throws ConfigException, SQLException
  {
    return null;

    /* XXX:
    String typeName = type.getName();

    MappedSuperType mappedSuperType = _mappedSuperMap.get(typeName);

    if (mappedSuperType != null)
      return mappedSuperType;

    try {
      mappedSuperType = _persistenceUnit.createMappedSuper(typeName, type);
      _mappedSuperMap.put(typeName, mappedSuperType);

      boolean isField = isField(type, mappedSuperConfig);

      if (isField)
        mappedSuperType.setFieldAccess(true);

      mappedSuperType.setInstanceClassName(type.getName() +
                                           "__ResinExt");
      mappedSuperType.setEnhanced(true);

      if (isField)
        introspectFields(_persistenceUnit, mappedSuperType, null,
                         type, entityConfig, false);
      else
        introspectMethods(_persistenceUnit, mappedSuperType, null,
                          type, entityConfig);

    } catch (ConfigException e) {
      mappedSuperType.setConfigException(e);

      throw e;
    } catch (SQLException e) {
      mappedSuperType.setConfigException(e);

      throw e;
    } catch (RuntimeException e) {
      mappedSuperType.setConfigException(e);

      throw e;
    }

    return mappedSuperType;
    */
  }
}
