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

import com.caucho.config.*;
import com.caucho.util.*;

import java.util.*;
import java.lang.reflect.Array;

/**
 * Represents a class type.
 */
public final class ClassType extends ConfigType
{
  private static final L10N L = new L10N(ClassType.class);
  
  public static final ClassType TYPE = new ClassType();

  private static final HashMap<String,Class> _primitiveTypes
    = new HashMap<String,Class>();
  
  /**
   * The ClassType is a singleton
   */
  private ClassType()
  {
  }
  
  /**
   * Returns the Java type.
   */
  public Class getType()
  {
    return Class.class;
  }
  
  /**
   * Converts the string to a value of the type.
   */
  public Object valueOf(String text)
  {
    if (text == null || text.length() == 0)
      return null;
    else {
      Class type = _primitiveTypes.get(text);

      if (type != null)
        return type;
      
      try {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        int bracketIdx = text.indexOf('[');

        if (bracketIdx > 0) {
          String componentTypeName = text.substring(0, bracketIdx);

          Class componentClass = _primitiveTypes.get(componentTypeName);

          if (componentClass == null)
            componentClass = Class.forName(componentTypeName, false, loader);

          while (bracketIdx > 0) {
            componentClass = Array.newInstance(componentClass, 0).getClass();
            bracketIdx = text.indexOf('[', (bracketIdx + 1));
          }

          return componentClass;
        }
        else {
          return Class.forName(text, false, loader);
        }
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
  }
  
  /**
   * Converts the value to a value of the type.
   */
  public Object valueOf(Object value)
  {
    if (value instanceof Class)
      return value;
    else if (value == null)
      return null;
    else if (value instanceof String)
      return valueOf((String) value);
    else
      throw new ConfigException(L.l("'{0}' cannot be converted to an Class",
                                    value));
  }

  static {
    _primitiveTypes.put("void", void.class);
    _primitiveTypes.put("boolean", boolean.class);
    _primitiveTypes.put("char", char.class);
    _primitiveTypes.put("byte", byte.class);
    _primitiveTypes.put("short", short.class);
    _primitiveTypes.put("int", int.class);
    _primitiveTypes.put("long", long.class);
    _primitiveTypes.put("float", float.class);
    _primitiveTypes.put("double", double.class);
    
    _primitiveTypes.put(Boolean.class.getName(), Boolean.class);
    _primitiveTypes.put(Character.class.getName(), Character.class);
    _primitiveTypes.put(Byte.class.getName(), Byte.class);
    _primitiveTypes.put(Short.class.getName(), Short.class);
    _primitiveTypes.put(Integer.class.getName(), Integer.class);
    _primitiveTypes.put(Long.class.getName(), Long.class);
    _primitiveTypes.put(Float.class.getName(), Float.class);
    _primitiveTypes.put(Double.class.getName(), Double.class);
    
    _primitiveTypes.put(String.class.getName(), String.class);
    _primitiveTypes.put(Class.class.getName(), Class.class);
    
    _primitiveTypes.put("Boolean", Boolean.class);
    _primitiveTypes.put("Character", Character.class);
    _primitiveTypes.put("Byte", Byte.class);
    _primitiveTypes.put("Short", Short.class);
    _primitiveTypes.put("Integer", Integer.class);
    _primitiveTypes.put("Long", Long.class);
    _primitiveTypes.put("Float", Float.class);
    _primitiveTypes.put("Double", Double.class);
    
    _primitiveTypes.put("String", String.class);
    _primitiveTypes.put("Date", Date.class);
    _primitiveTypes.put("Class", Class.class);
  }
}
