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

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.validator.*;

import com.caucho.config.*;
import com.caucho.util.*;

public class MappedEntries extends AbstractValueConfig
  implements AbstractValue
{
  private ArrayList<AbstractValue> _keyList = new ArrayList<AbstractValue>();
  private ArrayList<AbstractValue> _valueList = new ArrayList<AbstractValue>();

  private Class _keyClass = String.class;
  private Class _valueClass = String.class;

  public void setId(String id)
  {
  }

  public void setKeyClass(Class keyClass)
  {
    _keyClass = keyClass;
  }

  public void setValueClass(Class valueClass)
  {
    _valueClass = valueClass;
  }

  public void addMapEntry(MapEntry entry)
  {
    _keyList.add(entry.getKey(_keyClass));
    _valueList.add(entry.getValue(_valueClass));
  }

  public ArrayList<AbstractValue> getKeyList()
  {
    return _keyList;
  }

  public ArrayList<AbstractValue> getValueList()
  {
    return _valueList;
  }

  public Object getValue()
  {
    return null;
  }
  
  AbstractValue getValue(Class type)
  {
    return this;
  }
  
  public Object getValue(FacesContext context)
  {
    TreeMap map = new TreeMap();

    int size = _keyList.size();
    
    for (int i = 0; i < size; i++) {
      map.put(_keyList.get(i).getValue(context),
              _valueList.get(i).getValue(context));
    }
    
    return map;
  }

  public void addProgram(ArrayList<BeanProgram> program,
                         String name,
                         Class type)
  {
    String getterName = ("get"
                         + Character.toUpperCase(name.charAt(0))
                         + name.substring(1));
    String setterName = ("set"
                         + Character.toUpperCase(name.charAt(0))
                         + name.substring(1));
    
    Method getter = findGetter(type, getterName);
    Method setter = findSetter(type, setterName);

    program.add(new MapPropertyBeanProgram(getter, setter,
                                           _keyList, _valueList,
                                           name));
  }

  public static class MapEntry {
    private String _key;
    
    private AbstractValueConfig _value = NullValue.NULL;

    public void setKey(String key)
    {
      _key = key;
    }

    public AbstractValue getKey(Class type)
    {
      return PropertyValue.create(_key, type);
    }

    public void setValue(String value)
    {
      _value = new ValueConfig(value);
    }

    public void setNullValue(String value)
    {
      _value = NullValue.NULL;
    }

    public void setMapEntries(MappedEntries entries)
    {
      _value = entries;
    }

    public void setListEntries(ListEntries entries)
    {
      _value = entries;
    }

    public Object getValue()
    {
      return _value;
    }

    public AbstractValue getValue(Class type)
    {
      return _value.getValue(type);
    }
  }
}
