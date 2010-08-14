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
import com.caucho.jsp.el.*;
import com.caucho.jsp.BundleManager;
import com.caucho.jsf.cfg.*;

import javax.el.*;
import javax.faces.context.*;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import java.beans.FeatureDescriptor;
import java.util.*;

/**
 * Variable resolution for JSF variables
 */
public class JsfResourceBundleELResolver extends ELResolver {
  private BundleManager _bundleManager = BundleManager.create();

  private HashMap<String,ResourceBundleConfig> _bundleMap
    = new HashMap<String,ResourceBundleConfig>();

  public JsfResourceBundleELResolver()
  {
  }

  public void addBundle(String var, ResourceBundleConfig bundle)
  {
    _bundleMap.put(var, bundle);
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext env,
                                        Object base)
  {
    if (base != null)
      return null;
    else
      return ResourceBundle.class;
  }

  @Override
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext env,
                                                           Object base)
  {
    if (base != null)
      return null;
    
    ArrayList<FeatureDescriptor> descriptors
      = new ArrayList<FeatureDescriptor>();

    for (ResourceBundleConfig bundle : _bundleMap.values()) {
      FeatureDescriptor desc = new FeatureDescriptor();
      desc.setName(bundle.getVar());
      
      if (bundle.getDisplayName() != null)
        desc.setDisplayName(bundle.getDisplayName());
      else
        desc.setDisplayName(bundle.getVar());
      
      desc.setExpert(false);
      desc.setHidden(false);
      desc.setPreferred(true);
      desc.setValue(ELResolver.RESOLVABLE_AT_DESIGN_TIME, Boolean.TRUE);
      desc.setValue(ELResolver.TYPE, ResourceBundle.class);

      descriptors.add(desc);
    }

    return descriptors.iterator();
  }
  
  @Override
  public Class getType(ELContext env, Object base, Object property)
  {
    if (base == null && property instanceof String) {
      String var = (String) property;

      ResourceBundleConfig bundle = _bundleMap.get(var);

      if (bundle != null) {
        env.setPropertyResolved(true);

        return ResourceBundle.class;
      }
    }

    return null;
  }
  
  @Override
  public Object getValue(ELContext env, Object base, Object property)
  {
    if (base == null && property instanceof String) {
      String var = (String) property;

      ResourceBundleConfig bundle = _bundleMap.get(var);

      if (bundle != null) {
        env.setPropertyResolved(true);

        FacesContext facesContext = FacesContext.getCurrentInstance();
        Locale locale = null;

        if (facesContext != null)
          locale = facesContext.getViewRoot().getLocale();

        LocalizationContext lc;

        lc = _bundleManager.getBundle(bundle.getBaseName(), locale);

        if (lc == null)
          lc = _bundleManager.getBundle(bundle.getBaseName());

        if (lc != null)
          return lc.getResourceBundle();
        else
          return null;
      }
    }

    return null;
  }
  
  @Override
  public boolean isReadOnly(ELContext env, Object base, Object property)
  {
    if (base == null && property instanceof String) {
      String var = (String) property;

      ResourceBundleConfig bundle = _bundleMap.get(var);

      if (bundle != null) {
        env.setPropertyResolved(true);

        return true;
      }
    }

    return false;
  }
    
  public void setValue(ELContext env,
                       Object base,
                       Object property,
                       Object value)
  {
    if (base == null && property instanceof String) {
      String var = (String) property;

      ResourceBundleConfig bundle = _bundleMap.get(var);

      if (bundle != null) {
        env.setPropertyResolved(true);

        throw new PropertyNotWritableException();
      }
    }
  }
}
