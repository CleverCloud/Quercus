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
import com.caucho.config.el.*;

import javax.el.*;
import javax.faces.component.*;
import javax.faces.context.*;

import java.beans.FeatureDescriptor;
import java.util.*;

/**
 * Variable resolution for JSF variables
 */
public class FacesContextELResolver extends CompositeELResolver {
  private static final L10N L = new L10N(FacesContextELResolver.class);

  private static final ArrayList<FeatureDescriptor> _implicitFeatureDescriptors
    = new ArrayList<FeatureDescriptor>();
  
  private ELResolver []_customResolvers;

  /*
  private final ImplicitObjectELResolver _implicitResolver
    = new ImplicitObjectELResolver();
  private final ScopedAttributeELResolver _attrResolver
    = new ScopedAttributeELResolver();
  */
  
  private final MapELResolver _mapResolver = new MapELResolver();
  private final ListELResolver _listResolver = new ListELResolver();
  private final ArrayELResolver _arrayResolver = new ArrayELResolver();
  private final JsfResourceBundleELResolver _jsfBundleResolver;
  private final BeanELResolver _beanResolver = new BeanELResolver();
  private final ResourceBundleELResolver _bundleResolver
    = new ResourceBundleELResolver();  

  private final CandiElResolver _cdiResolver
    = new CandiElResolver();

  private final ManagedBeanELResolver _managedBeanResolver
    = new ManagedBeanELResolver();

  public FacesContextELResolver(ELResolver []customResolvers,
                                JsfResourceBundleELResolver jsfBundleResolver)
  {
    _customResolvers = customResolvers;
    _jsfBundleResolver = jsfBundleResolver;
  }

  public void addManagedBean(String name, ManagedBeanConfig managedBean)
  {
    _managedBeanResolver.addManagedBean(name, managedBean);
  }

  public ManagedBeanELResolver getManagedBeanResolver()
  {
    return _managedBeanResolver;
  }

  public ELResolver getResourceBundleResolver()
  {
    return _jsfBundleResolver;
  }

  public void addELResolver(ELResolver elResolver)
  {
    ELResolver []elResolvers = new ELResolver[_customResolvers.length + 1];

    System.arraycopy(_customResolvers, 0,
                     elResolvers, 0,
                     _customResolvers.length);

    elResolvers[elResolvers.length - 1] = elResolver;

    _customResolvers = elResolvers;
  }

  public ELResolver []getCustomResolvers()
  {
    return _customResolvers;
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext env,
                                        Object base)
  {
    Class common = null;

    if (base == null)
      common = String.class;

    for (int i = 0; i < _customResolvers.length; i++) {
      common = common(common,
                      _customResolvers[i].getCommonPropertyType(env, base));
    }

    common = common(common, _mapResolver.getCommonPropertyType(env, base));
    common = common(common, _listResolver.getCommonPropertyType(env, base));
    common = common(common, _arrayResolver.getCommonPropertyType(env, base));
    common = common(common, _beanResolver.getCommonPropertyType(env, base));
    common = common(common, _jsfBundleResolver.getCommonPropertyType(env, base));
    common = common(common, _bundleResolver.getCommonPropertyType(env, base));

    return common;
  }

  private static Class common(Class a, Class b)
  {
    if (a == null)
      return b;
    else if (b == null)
      return a;
    else if (a.isAssignableFrom(b))
      return a;
    else if (b.isAssignableFrom(a))
      return b;
    else // XXX:
      return Object.class;
  }

  @Override
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext env,
                                                           Object base)
  {
    ArrayList<FeatureDescriptor> descriptors
      = new ArrayList<FeatureDescriptor>();

    if (base == null)
      descriptors.addAll(_implicitFeatureDescriptors);

    for (int i = 0; i < _customResolvers.length; i++) {
      addDescriptors(descriptors,
                     _customResolvers[i].getFeatureDescriptors(env, base));
    }

    if (base == null) {
      addDescriptors(descriptors,
                     _cdiResolver.getFeatureDescriptors(env, base));
      
      addDescriptors(descriptors,
                     _managedBeanResolver.getFeatureDescriptors(env, base));

      addDescriptors(descriptors,
                     _jsfBundleResolver.getFeatureDescriptors(env, base));
    }
    
    addDescriptors(descriptors, _mapResolver.getFeatureDescriptors(env, base));
    addDescriptors(descriptors,
                   _beanResolver.getFeatureDescriptors(env, base));
    addDescriptors(descriptors,
                   _jsfBundleResolver.getFeatureDescriptors(env, base));
    /*
    addDescriptors(descriptors,
                   _implicitResolver.getFeatureDescriptors(env, base));
    addDescriptors(descriptors,
                   _attrResolver.getFeatureDescriptors(env, base));
    */

    return descriptors.iterator();
  }

  private void addDescriptors(ArrayList<FeatureDescriptor> descriptors,
                              Iterator<FeatureDescriptor> iter)
  {
    if (iter == null)
      return;

    while (iter.hasNext()) {
      FeatureDescriptor desc = iter.next();

      descriptors.add(desc);
    }
  }
  
  @Override
  public Class getType(ELContext env, Object base, Object property)
  {
    env.setPropertyResolved(false);
    
    for (int i = 0; i < _customResolvers.length; i++) {
      Class type = _customResolvers[i].getType(env, base, property);

      if (env.isPropertyResolved())
        return type;
    }
    
    if (base != null) {
      if (base instanceof Map)
        return _mapResolver.getType(env, base, property);
      else if (base instanceof List)
        return _listResolver.getType(env, base, property);
      else if (base.getClass().isArray())
        return _arrayResolver.getType(env, base, property);
      else if (base instanceof ResourceBundle)
        return _bundleResolver.getType(env, base, property);
      else
        return _beanResolver.getType(env, base, property);
    }
    else if (base == null && property instanceof String) {
      ImplicitObjectExpr expr = ImplicitObjectExpr.create((String) property);

      if (expr != null) {
        env.setPropertyResolved(true);

        return Object.class;
      }

      Class type = _jsfBundleResolver.getType(env, base, property);

      if (env.isPropertyResolved())
        return type;
    }

    Object value = getValue(env, base, property);

    if (value != null)
      return value.getClass();
    else
      return null;
  }
  
  @Override
  public Object getValue(ELContext env, Object base, Object property)
  {
    env.setPropertyResolved(false);

    if (base == null
        && property instanceof String) {
      ImplicitObjectExpr expr = ImplicitObjectExpr.create((String) property);

      if (expr != null) {
        env.setPropertyResolved(true);

        return expr.getValue(env);
      }

      Object value = _jsfBundleResolver.getValue(env, base, property);
      if (env.isPropertyResolved())
        return value;
    }

    for (int i = 0; i < _customResolvers.length; i++) {
      Object value = _customResolvers[i].getValue(env, base, property);

      if (env.isPropertyResolved())
        return value;
    }
    
    if (base != null) {
      if (base instanceof Map)
        return _mapResolver.getValue(env, base, property);
      else if (base instanceof List)
        return _listResolver.getValue(env, base, property);
      else if (base.getClass().isArray())
        return _arrayResolver.getValue(env, base, property);
      else if (base instanceof ResourceBundle)
        return _bundleResolver.getValue(env, base, property);
      else
        return _beanResolver.getValue(env, base, property);
    }
    else if (property instanceof String) {
      
      FacesContext facesContext
        = (FacesContext) env.getContext(FacesContext.class);
      ExternalContext ec = facesContext.getExternalContext();
      
      Object value = ec.getRequestMap().get(property);

      if (value != null) {
        env.setPropertyResolved(true);
        return value;
      }
      
      value = ec.getSessionMap().get(property);

      if (value != null) {
        env.setPropertyResolved(true);
        return value;
      }
      
      value = ec.getApplicationMap().get(property);

      if (value != null) {
        env.setPropertyResolved(true);
        return value;
      }

      value = _cdiResolver.getValue(env, base, property);

      if (env.isPropertyResolved())
        return value;

      value = _managedBeanResolver.getValue(env, base, property);

      if (env.isPropertyResolved())
        return value;

      return null;
    }
    else
      return null;
  }

  @Override
  public boolean isReadOnly(ELContext env, Object base, Object property)
  {
    env.setPropertyResolved(false);

    for (int i = 0; i < _customResolvers.length; i++) {
      boolean readOnly = _customResolvers[i].isReadOnly(env, base, property);

      if (env.isPropertyResolved())
        return readOnly;
    }

    if (base != null) {
      if (base instanceof Map) {
        env.setPropertyResolved(true);

        return _mapResolver.isReadOnly(env, base, property);
      }
      else if (base instanceof List) {
        env.setPropertyResolved(true);

        return _listResolver.isReadOnly(env, base, property);
      }
      else if (base.getClass().isArray()) {
        env.setPropertyResolved(true);

        return _arrayResolver.isReadOnly(env, base, property);
      }
      else if (base instanceof ResourceBundle) {
        env.setPropertyResolved(true);

        return _jsfBundleResolver.isReadOnly(env, base, property);
      }
      else {
        env.setPropertyResolved(true);

        return _beanResolver.isReadOnly(env, base, property);
      }
    }
    else if (base == null && property instanceof String) {
      ImplicitObjectExpr expr = ImplicitObjectExpr.create((String) property);

      if (expr != null) {
        env.setPropertyResolved(true);

        return true;
      }

      boolean value = _jsfBundleResolver.isReadOnly(env, base, property);
      if (env.isPropertyResolved())
        return value;
    }

    for (int i = 0; i < _customResolvers.length; i++) {
      boolean value = _customResolvers[i].isReadOnly(env, base, property);

      if (env.isPropertyResolved())
        return value;
    }

    env.setPropertyResolved(true);

    return false;
  }
    
  public void setValue(ELContext env,
                       Object base,
                       Object property,
                       Object value)
  {
    env.setPropertyResolved(false);

    for (int i = 0; i < _customResolvers.length; i++) {
      _customResolvers[i].setValue(env, base, property, value);

      if (env.isPropertyResolved())
        return;
    }

    if (base != null) {
      if (base instanceof Map)
        _mapResolver.setValue(env, base, property, value);
      else if (base instanceof List)
        _listResolver.setValue(env, base, property, value);
      else if (base.getClass().isArray())
        _arrayResolver.setValue(env, base, property, value);
      else if (base instanceof ResourceBundle)
        _jsfBundleResolver.setValue(env, base, property, value);
      else
        _beanResolver.setValue(env, base, property, value);
    }
    else if (property instanceof String) {
      String key = (String) property;
      ImplicitObjectExpr expr = ImplicitObjectExpr.create(key);

      if (expr != null)
        throw new PropertyNotWritableException(key);
      
      _jsfBundleResolver.setValue(env, base, property, value);
      if (env.isPropertyResolved())
        return;
      
      FacesContext facesContext
        = (FacesContext) env.getContext(FacesContext.class);
      ExternalContext ec = facesContext.getExternalContext();

      
      Object oldValue = ec.getRequestMap().get(key);

      if (oldValue != null) {
        ec.getRequestMap().put(key, value);
        env.setPropertyResolved(true);
        return;
      }
      
      oldValue = ec.getSessionMap().get(key);

      if (oldValue != null) {
        ec.getSessionMap().put(key, value);
        env.setPropertyResolved(true);
        return;
      }
      
      oldValue = ec.getApplicationMap().get(key);

      if (oldValue != null) {
        ec.getApplicationMap().put(key, value);
        env.setPropertyResolved(true);
        return;
      }

      ec.getRequestMap().put(key, value);
      env.setPropertyResolved(true);
      return;
    }
  }

  private static void addDescriptor(String name, Class type)
  {
    FeatureDescriptor desc = new FeatureDescriptor();
    desc.setName(name);
    desc.setDisplayName(name);
    desc.setExpert(false);
    desc.setHidden(false);
    desc.setPreferred(true);
    desc.setValue(ELResolver.RESOLVABLE_AT_DESIGN_TIME, Boolean.TRUE);
    desc.setValue(ELResolver.TYPE, type);
    
    _implicitFeatureDescriptors.add(desc);
  }

  static {
    addDescriptor("application", Object.class);
    addDescriptor("applicationScope", Map.class);
    addDescriptor("cookie", Map.class);
    addDescriptor("facesContext", FacesContext.class);
    addDescriptor("header", Map.class);
    addDescriptor("headerValues", Map.class);
    addDescriptor("initParam", Map.class);
    addDescriptor("param", Map.class);
    addDescriptor("paramValues", Map.class);
    addDescriptor("request", Object.class);
    addDescriptor("requestScope", Map.class);
    addDescriptor("resource", Object.class);
    addDescriptor("session", Object.class);
    addDescriptor("sessionScope", Map.class);
    addDescriptor("view", UIViewRoot.class);
  }
}
