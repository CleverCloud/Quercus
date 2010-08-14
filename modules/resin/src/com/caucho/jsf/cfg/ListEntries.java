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

public class ListEntries extends AbstractValueConfig
  implements AbstractValue
{
  private ArrayList<AbstractValue> _list = new ArrayList<AbstractValue>();

  private Class _valueClass = String.class;
  
  public void setId(String id)
  {
  }

  public void setValueClass(Class valueClass)
  {
    _valueClass = valueClass;
  }

  public void addValue(String value)
  {
    _list.add(PropertyValue.create(value, _valueClass));
  }

  public void setNullValue(String value)
  {
    _list.add(NullPropertyValue.NULL);
  }

  public void setMapEntries(MappedEntries value)
  {
    _list.add(value);
  }

  public void setListEntries(ListEntries value)
  {
    _list.add(value);
  }

  public ArrayList<AbstractValue> getListValues()
  {
    return _list;
  }
  
  AbstractValue getValue(Class type)
  {
    return this;
  }
  
  public Object getValue(FacesContext context)
  {
    ArrayList list = new ArrayList();

    int size = _list.size();
    for (int i = 0; i < size; i++)
      list.add(_list.get(i).getValue(context));
    
    return list;
  }

  public Object getValue()
  {
    return _list;
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

    if (getter != null && getter.getReturnType().isArray())
      program.add(new ArrayPropertyBeanProgram(getter, setter, _list, name));
    else
      program.add(new ListPropertyBeanProgram(getter, setter, _list, name));
  }
}
