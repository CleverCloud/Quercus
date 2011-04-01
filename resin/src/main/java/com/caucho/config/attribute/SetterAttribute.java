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

package com.caucho.config.attribute;

import java.lang.reflect.*;

import com.caucho.config.*;
import com.caucho.config.type.*;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

public class SetterAttribute<T> extends Attribute {
  private static final L10N L = new L10N(SetterAttribute.class);
  
  private final Method _setter;
  private final Class<T> _type;
  private ConfigType<T> _configType;

  public SetterAttribute(Method setter, Class<T> type)
  {
    _setter = setter;
    _setter.setAccessible(true);
    _type = type;
  }
  
  /**
   * Returns the config type of the attribute value.
   */
  @Override
  public ConfigType<T> getConfigType()
  {
    if (_configType == null)
      _configType = TypeFactory.getType(_type);
    
    return _configType;
  }

  @Override
  public boolean isAllowText()
  {
    return getConfigType().isConstructableFromString();
  }

  /**
   * True if it allows inline beans
   */
  @Override
  public boolean isAllowInline()
  {
    return true;
  }
  

  /**
   * True if it allows inline beans
   */
  @Override
  public boolean isInlineType(ConfigType<?> type)
  {
    if (type == null)
      return false;
    else if (type.isReplace())
      return true;
    else
      return _type.isAssignableFrom(type.getType());
  }
  
  /**
   * Returns true if the setter is marked with @Configurable
   */
  @Override
  public boolean isConfigurable()
  {
    return _setter.isAnnotationPresent(Configurable.class);
  }
  
  @Override
  public boolean isAssignableFrom(Attribute attr)
  {
    if (! (attr instanceof SetterAttribute<?>))
      return false;
    
    SetterAttribute<?> setterAttr = (SetterAttribute<?>) attr;
    Method setter = setterAttr._setter;
    
    if (! _setter.getName().equals(setter.getName()))
      return false;
    
    return _setter.getDeclaringClass().isAssignableFrom(setter.getDeclaringClass());
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setText(Object bean, QName name, String value)
    throws ConfigException
  {
    try {
      ConfigType<?> configType = getConfigType();

      _setter.invoke(bean, configType.valueOf(value));
    } catch (Exception e) {
      throw ConfigException.create(_setter, e);
    }
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setValue(Object bean, QName name, Object value)
    throws ConfigException
  {
    try {
      _setter.invoke(bean, value);
    } catch (IllegalArgumentException e) {
      throw ConfigException.create(_setter,
                                   L.l("'{0}' is an illegal value.",
                                       value),
                                       e);
    } catch (Exception e) {
      throw ConfigException.create(_setter, e);
    }
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, QName name, ConfigType<?> configType)
    throws ConfigException
  {
    try {
      if (configType != null && _type.isAssignableFrom(configType.getType())) {
        // ioc/2172
        return configType.create(parent, name);
      }
      else {
        return getConfigType().create(parent, name);
      }
    } catch (Exception e) {
      throw ConfigException.create(_setter, e);
    }
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, QName name)
    throws ConfigException
  {
    try {
      return getConfigType().create(parent, name);
    } catch (Exception e) {
      throw ConfigException.create(_setter, e);
    }
  }
  
  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (o == null)
      return false;
    else if (getClass() != o.getClass())
      return false;
    
    SetterAttribute<?> attr = (SetterAttribute<?>) o;
    
    return _type.equals(attr._type) && _setter.equals(attr._setter);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _setter + "]";
  }
}
