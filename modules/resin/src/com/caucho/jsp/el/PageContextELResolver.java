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

package com.caucho.jsp.el;

import com.caucho.el.AbstractVariableResolver;
import com.caucho.jsp.PageContextImpl;

import javax.el.*;
import javax.servlet.jsp.el.ImplicitObjectELResolver;
import javax.servlet.jsp.el.ScopedAttributeELResolver;
import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Variable resolution for JSP variables
 */
public class PageContextELResolver extends AbstractVariableResolver {
  private final static Logger log
    = Logger.getLogger(PageContextELResolver.class.getName());
  
  private final PageContextImpl _pageContext;

  private final ELResolver []_customResolvers;
  
  private final ImplicitObjectELResolver _implicitResolver
    = new ImplicitObjectELResolver();
  private final ScopedAttributeELResolver _attrResolver
    = new ScopedAttributeELResolver();
  
  private final MapELResolver _mapResolver = new MapELResolver();
  private final ListELResolver _listResolver = new ListELResolver();
  private final ArrayELResolver _arrayResolver = new ArrayELResolver();
  private final ResourceBundleELResolver _bundleResolver
    = new ResourceBundleELResolver();
  private final BeanELResolver _beanResolver = new BeanELResolver();

  public PageContextELResolver(PageContextImpl pageContext,
                               ELResolver []customResolvers)
  {
    _pageContext = pageContext;
    _customResolvers = customResolvers;
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

    for (int i = 0; i < _customResolvers.length; i++) {
      addDescriptors(descriptors,
                     _customResolvers[i].getFeatureDescriptors(env, base));
    }

    addDescriptors(descriptors, _mapResolver.getFeatureDescriptors(env, base));
    addDescriptors(descriptors,
                   _beanResolver.getFeatureDescriptors(env, base));
    addDescriptors(descriptors,
                   _bundleResolver.getFeatureDescriptors(env, base));
    addDescriptors(descriptors,
                   _implicitResolver.getFeatureDescriptors(env, base));
    addDescriptors(descriptors,
                   _attrResolver.getFeatureDescriptors(env, base));

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
  public Object getValue(ELContext env, Object base, Object property)
  {
    try {
      env.setPropertyResolved(false);

      for (int i = 0; i < _customResolvers.length; i++) {
        Object value = _customResolvers[i].getValue(env, base, property);

        if (env.isPropertyResolved()) {
          return value;
        }
      }
    
      if (base != null) {
        if (base instanceof Map)
          return _mapResolver.getValue(env, base, property);
        else if (base instanceof List)
          return _listResolver.getValue(env, base, property);
        else if (base.getClass().isArray())
          return _arrayResolver.getValue(env, base, property);
        else if (base instanceof PropertyResourceBundle)
          return _bundleResolver.getValue(env, base, property);
        else
          return _beanResolver.getValue(env, base, property);
      }
      else if (property instanceof String) {
        env.setPropertyResolved(true);

        return _pageContext.findAttribute(property.toString());
      }
      else
        return null;
    }
    catch (PropertyNotFoundException e) {
      // jsp/3253
      throw e;
    }
    catch (ELException e) {
      // jsp/3094 vs jsp/30cc

      if (e.getCause() != null)
        throw e;
      
      log.log(Level.FINER, e.toString(), e);

      return null;
    }
  }
  
  @Override
  public Class getType(ELContext env, Object base, Object property)
  {
    env.setPropertyResolved(false);

    for (int i = 0; i < _customResolvers.length; i++) {
      Class value = _customResolvers[i].getType(env, base, property);

      if (env.isPropertyResolved())
        return value;
    }
    
    if (base != null) {
      if (base instanceof Map)
        return _mapResolver.getType(env, base, property);
      else if (base instanceof List)
        return _listResolver.getType(env, base, property);
      else if (base.getClass().isArray())
        return _arrayResolver.getType(env, base, property);
      else if (base instanceof PropertyResourceBundle)
              return _bundleResolver.getType(env, base, property);
      else
        return _beanResolver.getType(env, base, property);
    }
    else if (base == null && property instanceof String) {
      env.setPropertyResolved(true);

      Object value = _pageContext.findAttribute(property.toString());

      if (value != null)
        return value.getClass();
      else
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
    
    if (base != null) {
      if (base instanceof Map)
        _mapResolver.setValue(env, base, property, value);
      else if (base instanceof List)
        _listResolver.setValue(env, base, property, value);
      else if (base.getClass().isArray())
        _arrayResolver.setValue(env, base, property, value);
      else
        _beanResolver.setValue(env, base, property, value);
    }
    else if (property instanceof String) {
      for (int i = 0; i < _customResolvers.length; i++) {
        _customResolvers[i].setValue(env, base, property, value);

        if (env.isPropertyResolved())
          return;
      }
      
      env.setPropertyResolved(true);

      _pageContext.setAttribute(property.toString(), value);
    }
  }
}
