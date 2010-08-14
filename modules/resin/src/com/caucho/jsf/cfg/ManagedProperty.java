/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.cfg;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.*;

import com.caucho.config.types.DescriptionGroupConfig;

public class ManagedProperty extends DescriptionGroupConfig
{
  private static final L10N L = new L10N(ManagedProperty.class);
  static final protected Logger log
    = Logger.getLogger(ManagedProperty.class.getName());

  
  private String _id;

  private String _name;

  private AbstractValueConfig _value = NullValue.NULL;

  public String getName()
  {
    return _name;
  }

  public void setId(String id)
  {
    _id = id;
  }

  public void setPropertyName(String name)
  {
    _name = name;
  }

  public void setNullValue(NullValue value)
  {
    _value = NullValue.NULL;
  }

  public void setValue(String value)
  {
    _value = new ValueConfig(value);
  }

  public void setMapEntries(MappedEntries entries)
  {
    _value = entries;
  }

  public void setListEntries(ListEntries entries)
  {
    _value = entries;
  }

  public void setPropertyClass(Class type)
  {
  }

  public Class getPropertyClass()
  {
    return null;
  }

  public void addProgram(ArrayList<BeanProgram> program, Class type)
  {
    if (_value instanceof MappedEntries) {
      ((MappedEntries) _value).addProgram(program, _name, type);
      return;
    }
    else if (_value instanceof ListEntries) {
      ((ListEntries) _value).addProgram(program, _name, type);
      return;
    }
    
    String name = ("set"
                   + Character.toUpperCase(_name.charAt(0))
                   + _name.substring(1));
    
    Method setter = findSetter(type, name);

    if (setter == null) {
      if (log.isLoggable(Level.FINER))
        log.finer(L.l("'{0}' is unknown property of '{1}'", _name, type.getName()));

      program.add(new PropertyBeanProgram(_name, false));
    } else {
      Class propType = setter.getParameterTypes()[0];

      program.add(new PropertyBeanProgram(setter, _value.getValue(propType)));
    }
  }

  private static Method findSetter(Class type, String name)
  {
    if (type == null)
      return null;

    while (! Object.class.equals(type)) {
      for (Method method : type.getDeclaredMethods()) {
        if (! method.getName().equals(name))
          continue;
        else if (method.getParameterTypes().length != 1)
          continue;
        else if (Modifier.isStatic(method.getModifiers()))
          continue;

        return method;
      }

      type = type.getSuperclass();
    }

    return null;
  }
}
