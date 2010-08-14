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

package com.caucho.jca.cfg;

import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Configuration for a connector configuration property.
 */
public class ConfigPropertyConfig {
  private static final L10N L = new L10N(ConfigPropertyConfig.class);
 
  private String _name;
  private Class _type;
  private Object _value;
  
  public ConfigPropertyConfig()
  {
  }

  /**
   * Sets the property description.
   */
  public void setDescription(String description)
  {
  }

  /**
   * Sets the config property name.
   */
  public void setConfigPropertyName(String name)
  {
    _name = name;
  }

  /**
   * Gets the config property name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the config property type.
   */
  public void setConfigPropertyType(Class type)
  {
    _type = type;
  }

  /**
   * Gets the config property type.
   */
  public Class getType()
  {
    return _type;
  }

  /**
   * Sets the config property default value.
   */
  public void setConfigPropertyValue(Object value)
  {
    _value = value;
  }

  /**
   * Gets the config property default value.
   */
  public Object getValue()
  {
    return _value;
  }
}
