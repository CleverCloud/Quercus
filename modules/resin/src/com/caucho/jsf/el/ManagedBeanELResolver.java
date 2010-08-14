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

package com.caucho.jsf.el;

import com.caucho.jsf.cfg.*;
import com.caucho.util.*;

import javax.el.*;
import javax.faces.context.*;
import javax.faces.FacesException;

import java.beans.FeatureDescriptor;
import java.util.*;

/**
 * Variable resolution for JSF managed beans
 */
public class ManagedBeanELResolver extends ELResolver {
  private static final L10N L = new L10N(ManagedBeanELResolver.class);

  private final HashMap<String,ManagedBeanConfig> _managedBeanMap
    = new  HashMap<String,ManagedBeanConfig>();

  private HashSet<String> _managedBeanInitSet;

  public ManagedBeanELResolver()
  {
  }

  public void addManagedBean(String name, ManagedBeanConfig managedBean)
  {
    _managedBeanMap.put(name, managedBean);
  }

  @Override
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext env,
                                                           Object base)
  {
    ArrayList<FeatureDescriptor> descriptors
      = new ArrayList<FeatureDescriptor>();
    
    return descriptors.iterator();
  }
  
  @Override
  public Class getType(ELContext env, Object base, Object property)
  {
    if (base == null && property instanceof String) {
      String key = property.toString();
      
      if (_managedBeanMap.get(key) != null) {
        env.setPropertyResolved(true);

        return Object.class;
      }
    }

    return null;
  }
  
  @Override
  public Object getValue(ELContext env, Object base, Object property)
  {
    if (base == null && property instanceof String) {
      String key = property.toString();
      
      ManagedBeanConfig managedBean = _managedBeanMap.get(key);

      if (managedBean != null) {
        env.setPropertyResolved(true);

        FacesContext facesContext = FacesContext.getCurrentInstance();

        ExternalContext extContext = facesContext.getExternalContext();

        Map requestMap = extContext.getRequestMap();
        Object value = requestMap.get(key);

        if (value != null)
          return value;

        value = extContext.getSessionMap().get(key);

        if (value != null)
          return value;

        value = extContext.getApplicationMap().get(key);

        if (value != null)
          return value;

        Scope oldScope = (Scope) env.getContext(Scope.class);
        Scope scope = oldScope;

        if (scope == null) {
          scope = new Scope();
          env.putContext(Scope.class, scope);
        }

        int oldScopeValue = scope.getScope();

        try {
          if (scope.containsBean(key))
            throw new FacesException(L.l("'{0}' is a circular managed bean reference.",
                                      key));

          scope.addBean(key);

          return managedBean.create(facesContext, scope);
        } finally {
          scope.removeBean(key);
          scope.setScope(oldScopeValue);
          env.putContext(Scope.class, oldScope);
        }
      }
    }

    return null;
  }
  
  @Override
  public boolean isReadOnly(ELContext env,
                            Object base,
                            Object property)
  {
    if (base == null && property instanceof String) {
      String key = property.toString();
      
      ManagedBeanConfig managedBean = _managedBeanMap.get(key);

      if (managedBean != null) {
        env.setPropertyResolved(true);

        return true;
      }
    }

    return false;
  }
  
  @Override
  public Class getCommonPropertyType(ELContext env,
                                     Object base)
  {
    if (base == null)
      return Object.class;
    else
      return null;
  }
  
  @Override
  public void setValue(ELContext env,
                       Object base,
                       Object property,
                       Object value)
  {
    if (base == null && property instanceof String) {
      String key = property.toString();
      
      ManagedBeanConfig managedBean = _managedBeanMap.get(key);

      if (managedBean != null) {
        throw new PropertyNotWritableException(L.l("Managed bean ${{0}} is not writable",
                                      key));
      }
    }
  }

  public static class Scope {
    private int _scope = Integer.MAX_VALUE;

    private HashSet<String> _initSet = new HashSet<String>();

    public int getScope()
    {
      return _scope;
    }

    public int setScope(int scope)
    {
      int oldScope = _scope;

      _scope = scope;

      return oldScope;
    }

    public boolean containsBean(String name)
    {
      return _initSet.contains(name);
    }

    public void addBean(String name)
    {
      _initSet.add(name);
    }

    public void removeBean(String name)
    {
      _initSet.remove(name);
    }
  }
}
