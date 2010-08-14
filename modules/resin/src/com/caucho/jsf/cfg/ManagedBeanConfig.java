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
import com.caucho.jsf.el.*;
import com.caucho.util.*;

public class ManagedBeanConfig
{
  private static final L10N L = new L10N(ManagedBeanConfig.class);

  private String _configLocation;
  
  private String _id;

  private String _name;

  private String _typeName;
  private Class _type;

  private ArrayList<BeanProgram> _program
    = new ArrayList<BeanProgram>();

  private Scope _scope = Scope.REQUEST;

  public void setId(String id)
  {
  }

  public void setDescription(String description)
  {
  }

  public void setManagedBeanName(String name)
  {
    _name = name;
  }
  
  public String getName()
  {
    return _name;
  }

  public void setConfigLocation(String location)
  {
    _configLocation = location;
  }
  
  public void setManagedBeanClass(String cl)
  {
    _typeName = cl;
  }

  public String getManagedBeanClass()
  {
    return _typeName;
  }

  public Class getType()
  {
    if (_type == null) {
      try {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        _type = Class.forName(_typeName, false, loader);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
    
    return _type;
  }
  
  public void setManagedBeanScope(String scope)
  {
    if ("request".equals(scope))
      _scope = Scope.REQUEST;
    else if ("session".equals(scope))
      _scope = Scope.SESSION;
    else if ("application".equals(scope))
      _scope = Scope.APPLICATION;
    else if ("none".equals(scope))
      _scope = Scope.NONE;
    else
      throw new ConfigException(L.l("'{0}' is an unknown managed-bean-scope.  Expected values are request, session, application, or none.",
                                    scope));
  }

  public String getManagedBeanScope()
  {
    return _scope.toString();
  }
  
  public void setManagedProperty(ManagedProperty property)
  {
    property.addProgram(_program, getType());
  }

  public ManagedProperty getManagedProperty()
  {
    throw new UnsupportedOperationException();
  }

  public void setMapEntries(MappedEntries map)
  {
    ArrayList<AbstractValue> keyList = map.getKeyList();
    ArrayList<AbstractValue> valueList = map.getValueList();

    for (int i = 0; i < keyList.size(); i++) {
      _program.add(new MapBeanProgram(keyList.get(i), valueList.get(i)));
    }
  }

  public void setListEntries(ListEntries list)
  {
    for (AbstractValue value : list.getListValues()) {
      _program.add(new ListBeanProgram(value));
    }
  }
  
  public Object create(FacesContext context,
                       ManagedBeanELResolver.Scope createScope)
    throws FacesException
  {
    try {
      ELContext elContext = context.getELContext();

      boolean isPropertyResolved = elContext.isPropertyResolved();

      Object value = getType().newInstance();

      if (createScope.getScope() < _scope.ordinal())
        throw new FacesException(L.l("Scope '{0}' is long for enclosing bean.",
                                     _scope));
      else if (_scope.ordinal() < createScope.getScope())
        createScope.setScope(_scope.ordinal());

      for (int i = 0; i < _program.size(); i++) {
        _program.get(i).configure(context, value);
      }

      ExternalContext extContext = context.getExternalContext();

      switch (_scope) {
      case APPLICATION:
        extContext.getApplicationMap().put(_name, value);
        break;

      case SESSION:
        extContext.getSessionMap().put(_name, value);
        break;

      case REQUEST:
        extContext.getRequestMap().put(_name, value);
        break;
      }

      elContext.setPropertyResolved(isPropertyResolved);
      
      return value;
    } catch (FacesException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new FacesException(e);
    }
  }
  
  enum Scope {
    NONE,
    APPLICATION,
    SESSION,
    REQUEST,
  };
}
