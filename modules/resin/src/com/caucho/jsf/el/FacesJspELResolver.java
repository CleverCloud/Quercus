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

import com.caucho.el.AbstractVariableResolver;
import com.caucho.jsf.application.*;
import com.caucho.jsf.cfg.*;
import com.caucho.jsp.el.*;
import com.caucho.config.el.*;

import javax.el.*;
import javax.faces.component.*;
import javax.faces.context.*;
import java.beans.FeatureDescriptor;
import java.util.*;

/**
 * Variable resolution for JSF variables
 */
public class FacesJspELResolver extends ELResolver {
  private static final ArrayList<FeatureDescriptor> _implicitFeatureDescriptors
    = new ArrayList<FeatureDescriptor>();
  
  private static HashMap<String,Type> _typeMap
    = new HashMap<String,Type>();

  private ApplicationImpl _app;
  private ELResolver _managedBeanResolver;
  private ELResolver _cdiResolver;
  private ELResolver _resourceBundleResolver;

  public FacesJspELResolver(ApplicationImpl app)
  {
    _app = app;

    FacesContextELResolver facesResolver
      = (FacesContextELResolver) _app.getELResolver();

    _managedBeanResolver = facesResolver.getManagedBeanResolver();
    _cdiResolver = new CandiElResolver();
    _resourceBundleResolver = facesResolver.getResourceBundleResolver();
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext env,
                                        Object base)
  {
    return null;
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

    descriptors.addAll(_implicitFeatureDescriptors);

    addDescriptors(descriptors,
                   _managedBeanResolver.getFeatureDescriptors(env, base));

    addDescriptors(descriptors,
                   _cdiResolver.getFeatureDescriptors(env, base));

    addDescriptors(descriptors,
                   _resourceBundleResolver.getFeatureDescriptors(env, base));

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
    if (base == null && property instanceof String) {
      Type type = _typeMap.get((String) property);

      if (type != null) {
        env.setPropertyResolved(true);

        return null;
      }
    }

    return null;
  }
  
  @Override
  public Object getValue(ELContext env, Object base, Object property)
  {
    env.setPropertyResolved(false);

    if (base == null && property instanceof String) {
      Type type = _typeMap.get((String) property);

      if (type != null) {
        env.setPropertyResolved(true);

        FacesContext facesContext = FacesContext.getCurrentInstance();

        if (facesContext != null) {
          switch (type) {
          case FACES_CONTEXT:
            return facesContext;
          case VIEW:
            return facesContext.getViewRoot();
          }
        }
      }

      Object value = _cdiResolver.getValue(env, base, property);

      if (env.isPropertyResolved())
        return value;

      value = _managedBeanResolver.getValue(env, base, property);

      if (env.isPropertyResolved())
        return value;

      value = _resourceBundleResolver.getValue(env, base, property);

      if (env.isPropertyResolved())
        return value;
    }
    
    return null;
  }
  
  @Override
  public boolean isReadOnly(ELContext env, Object base, Object property)
  {
    env.setPropertyResolved(false);

    if (base == null && property instanceof String) {
      Type type = _typeMap.get((String) property);

      if (type != null) {
        env.setPropertyResolved(true);

        return true;
      }
    }

    return false;
  }

  @Override
  public void setValue(ELContext env,
                       Object base,
                       Object property,
                       Object value)
  {
    if (base == null && property instanceof String) {
      String key = (String) property;
      
      Type type = _typeMap.get(key);

      if (type != null) {
        throw new PropertyNotWritableException(key);
      }
    }
  }

  enum Type {
    FACES_CONTEXT,
    VIEW
  };

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
    _typeMap.put("facesContext", Type.FACES_CONTEXT);
    _typeMap.put("view", Type.VIEW);
    
    addDescriptor("facesContext", FacesContext.class);
    addDescriptor("view", UIViewRoot.class);
  }
}
