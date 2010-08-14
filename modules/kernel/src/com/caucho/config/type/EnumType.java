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

import java.beans.*;
import java.util.*;

/**
 * Represents an enumeration type
 */
public final class EnumType extends ConfigType
{
  private static final L10N L = new L10N(EnumType.class);

  private Class _type;

  private HashMap<String,Object> _nameMap = new HashMap<String,Object>();
  
  /**
   * The enumeration type
   */
  public EnumType(Class type)
  {
    _type = type;

    for (Object value : type.getEnumConstants()) {
      Enum enumValue = (Enum) value;

      String name = enumValue.name();

      _nameMap.put(name, enumValue);
    }
  }
  
  /**
   * Returns the Java type.
   */
  public Class getType()
  {
    return _type;
  }
  
  /**
   * Converts the string to a value of the type.
   */
  public Object valueOf(String text)
  {
    if (text == null)
      return null;

    Object value = _nameMap.get(text);

    if (value != null)
      return value;

    throw new ConfigException(L.l("{0}: '{1}' is an unknown enumeration value.",
                                  _type.getName(), text));
  }
  
  /**
   * Converts the value to a value of the type.
   */
  public Object valueOf(Object value)
  {
    if (value == null)
      return null;
    else if (value instanceof String)
      return valueOf((String) value);
    else if (_type.isAssignableFrom(value.getClass()))
      return value;
    else
      return valueOf(String.valueOf(value));
  }
}
