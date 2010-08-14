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

package com.caucho.config.j2ee;

import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ValueGenerator;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.persistence.*;
import javax.enterprise.util.AnnotationLiteral;

import java.util.*;
import java.util.logging.Logger;

public class PersistenceContextGenerator extends WebBeanGenerator
{
  private static final Logger log
    = Logger.getLogger(PersistenceContextGenerator.class.getName());
  private static final L10N L = new L10N(PersistenceContextGenerator.class);

  private String _location;

  private String _jndiName;
  private String _unitName;
  private PersistenceContextType _type;
  private Map _properties;
  private EntityManager _manager;

  PersistenceContextGenerator(String location,
                              String jndiName,
                              String unitName,
                              PersistenceContextType type,
                              Map properties)
  {
    _location = location;

    _jndiName = jndiName;
    _unitName = unitName;
    _type = type;
    _properties = properties;
  }

  PersistenceContextGenerator(String location, PersistenceContext pc)
  {
    _location = location;

    _jndiName = pc.name();
    _unitName = pc.unitName();
    _type = pc.type();

    _properties = new HashMap();

    PersistenceProperty[] propertyList = pc.properties();
    for (int i = 0; propertyList != null && i < propertyList.length; i++) {
      PersistenceProperty prop = propertyList[i];

      _properties.put(prop.name(), prop.value());
    }
  }

  /**
   * Returns the expected type
   */
  @Override
  public Class getType()
  {
    return EntityManager.class;
  }

  /**
   * Creates the value.
   */
  public Object create()
  {
    if (_manager != null)
      return _manager;

    try {
      if (PersistenceContextType.EXTENDED.equals(_type)) {
        _manager = create(EntityManager.class,
                      new AnnotationLiteral<JpaPersistenceContext>() {
                        public String value() { return _unitName; }
                        public boolean extended() { return true; }
                      });
      }
      else {
        _manager = create(EntityManager.class,
                      new AnnotationLiteral<JpaPersistenceContext>() {
                        public String value() { return _unitName; }
                      });
      }
    } catch (Exception e) {
      throw ConfigException.create(_location, e);
    }

    return _manager;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _jndiName + "," + _unitName + "]";
  }
}
