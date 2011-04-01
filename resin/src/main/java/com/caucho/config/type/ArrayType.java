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

package com.caucho.config.type;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.config.*;
import com.caucho.config.attribute.*;
import com.caucho.util.*;
import com.caucho.xml.QName;

/**
 * Represents an array of values for configuration.
 */
public class ArrayType<T,X> extends ConfigType<T>
{
  private static final L10N L = new L10N(ArrayType.class);
  private static final Logger log
    = Logger.getLogger(ListType.class.getName());

  private final ConfigType<X> _componentType;
  private final Class<X> _componentClass;
  private final Class<T> _type;

  public ArrayType(ConfigType<X> componentType, Class<X> componentClass)
  {
    _componentType = componentType;
    _componentClass = componentClass;

    Class<?> type = null;
    try {
      type = Array.newInstance(componentClass, 0).getClass();
    } catch (Exception e) {
    }

    _type = (Class<T>) type;
  }

  @Override
  public boolean isArray()
  {
    return true;
  }

  @Override
  public ConfigType<X> getComponentType()
  {
    return _componentType;
  }

  /**
   * Returns the given type.
   */
  @Override
  public Class<T> getType()
  {
    return _type;
  }

  /**
   * Creates a new instance
   */
  @Override
  public Object create(Object parent, QName name)
  {
    return new ArrayList<X>();
  }

  /**
   * Returns the attribute based on the given name.
   */
  @Override
  public Attribute getAttribute(QName name)
  {
    // XXX: type
    
    return TypeFactory.getFactory().getListAttribute(name);
  }

  /**
   * Replaces the object.
   */
  @Override
  public Object replaceObject(Object value)
  {
    if (value == null)
      return null;

    if (value instanceof List<?>) {
      List<X> list = (List<X>) value;

      Object []array
        = (Object []) Array.newInstance(_componentClass, list.size());

      list.toArray(array);

      return array;
    }
    else {
      // ioc/2184
      value = _componentType.replaceObject(value);

      Object []array
        = (Object []) Array.newInstance(_componentClass, 1);

      Array.set(array, 0, value);
      
      return array;
    }
  }
  
  /**
   * Converts the string to the given value.
   */
  public Object valueOf(String text)
  {
    Object value = _componentType.valueOf(text);
    
    Object []valueArray
      = (Object []) Array.newInstance(_componentClass, 1);

    Array.set(valueArray, 0, value);

    return valueArray;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _componentClass.getName() + "]";
  }
}
