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

import java.lang.reflect.Method;

import com.caucho.config.ConfigException;
import com.caucho.config.type.ConfigType;
import com.caucho.config.type.TypeFactory;
import com.caucho.xml.QName;

public class CreateAttribute<T> extends Attribute {
  private final Method _create;
  private final Method _setter;
  private Class<T> _type;
  
  private ConfigType<T> _configType;

  public CreateAttribute(Method create, Class<T> type)
  {
    _create = create;
    if (_create != null)
      _create.setAccessible(true);
    _type = type;

    _setter = null;
  }

  public CreateAttribute(Method create, Class<T> type, Method setter)
  {
    _create = create;
    if (_create != null)
      _create.setAccessible(true);
    
    _type = type;

    _setter = setter;
    if (_setter != null)
      _setter.setAccessible(true);
  }
  
  /**
   * Returns the config type of the attribute value.
   */
  public ConfigType<?> getConfigType()
  {
    if (_configType == null)
      _configType = TypeFactory.getType(_type);
    
    return _configType;
  }

  /**
   * True if it allows text.
   */
  @Override
  public boolean isAllowText()
  {
    return false;
  }

  /**
   * True if it allows inline beans
   */
  @Override
  public boolean isAllowInline()
  {
    return _setter != null;
  }

  /**
   * True if it allows inline beans
   */
  @Override
  public boolean isInlineType(ConfigType<?> type)
  {
    // server/0219
    
    if (_setter == null)
      return false;
    else if (type == null)
      return false;
    else
      return _type.isAssignableFrom(type.getType());
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setValue(Object bean, QName name, Object value)
    throws ConfigException
  {
    try {
      if (_setter != null)
        _setter.invoke(bean, value);
    } catch (Exception e) {
      throw ConfigException.create(_setter, e);
    }
  }

  /**
   * Returns true for attributes which create objects.
   */
  public boolean isSetter()
  {
    return _setter != null;
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, QName name)
    throws ConfigException
  {
    try {
      return _create.invoke(parent);
    } catch (Exception e) {
      throw ConfigException.create(_create, e);
    }
  }
  
  
  @Override
  public boolean isAssignableFrom(Attribute attr)
  {
    if (! (attr instanceof CreateAttribute<?>))
      return false;
    
    CreateAttribute<?> createAttr = (CreateAttribute<?>) attr;
    Method create = createAttr._create;

    if (create == null || _create == null)
      return false;
    
    if (! _create.getName().equals(create.getName()))
      return false;
    
    if (! _create.getDeclaringClass().isAssignableFrom(create.getDeclaringClass()))
      return false;
    
    Method setter = createAttr._setter;

    if ((setter == null) != (_setter == null))
      return false;
    
    if (setter == null)
      return true;
    
    if (! _setter.getName().equals(setter.getName()))
      return false;
    
    if (! _setter.getDeclaringClass().isAssignableFrom(setter.getDeclaringClass()))
      return false;
    
    return true;
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
    
    CreateAttribute<?> attr = (CreateAttribute<?>) o;
    
    return (_type.equals(attr._type)
            && _setter == attr._setter
            && _create == attr._create);
  }
  
}
