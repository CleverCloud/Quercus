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

package com.caucho.jca.ra;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.jca.cfg.ConfigPropertyConfig;
import com.caucho.util.L10N;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Configuration for an object with config values.
 */
public class ObjectConfig {
  private static final L10N L = new L10N(ObjectConfig.class);

  private Class<?> _type;
  private HashMap<String,ConfigPropertyConfig> _propertyMap =
    new HashMap<String,ConfigPropertyConfig>();
  
  public ObjectConfig()
  {
  }

  /**
   * Sets the type.
   */
  public void setType(Class<?> type)
    throws ConfigException
  {
    _type = type;

    Config.validate(type, Object.class);
  }

  /**
   * Gets the config property type.
   */
  public Class<?> getType()
  {
    return _type;
  }

  /**
   * Adds a new config property.
   */
  public void addConfigProperty(ConfigPropertyConfig property)
    throws ConfigException
  {
    if (_propertyMap.get(property.getName()) != null)
      throw new ConfigException(L.l("'{0}' is a duplicate property name.  Property names must be declared only once.",
                                    property.getName()));

    _propertyMap.put(property.getName(), property);
  }

  /**
   * Instantiates the object and sets the default properties.
   */
  public Object instantiate()
    throws Exception
  {
    if (_type == null)
      throw new ConfigException(L.l("The type must be set for an object configuration."));
    
    Object object = _type.newInstance();

    Iterator<ConfigPropertyConfig> iter = _propertyMap.values().iterator();
    while (iter.hasNext()) {
      ConfigPropertyConfig prop = iter.next();

      if (prop.getValue() != null) {
        Config.setAttribute(object, prop.getName(), prop.getValue());
      }
    }

    return object;
  }
}
