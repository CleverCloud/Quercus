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

package javax.el;

import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Resolves properties based on resourceBundles.
 */
public class ResourceBundleELResolver extends ELResolver {
  private final static Logger log
    = Logger.getLogger(ResourceBundleELResolver.class.getName());
  
  public ResourceBundleELResolver()
  {
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext context, Object base)
  {
    if (base instanceof ResourceBundle)
      return String.class;
    else
      return null;
  }

  @Override
  public Iterator getFeatureDescriptors(ELContext context, Object base)
  {
    if (! (base instanceof ResourceBundle))
      return null;

    ResourceBundle bundle = (ResourceBundle) base;

    ArrayList<FeatureDescriptor> list = new ArrayList<FeatureDescriptor>();

    Enumeration<String> e = bundle.getKeys();
    while (e.hasMoreElements()) {
      String key = e.nextElement();

      FeatureDescriptor desc = new FeatureDescriptor();
      desc.setName(key);
      desc.setDisplayName(key);
      desc.setShortDescription("");
      desc.setExpert(false);
      desc.setHidden(false);
      desc.setPreferred(true);

      desc.setValue(ELResolver.TYPE, String.class);
      desc.setValue(ELResolver.RESOLVABLE_AT_DESIGN_TIME, Boolean.TRUE);

      list.add(desc);
    }

    return list.iterator();
  }

  @Override
  public Class<?> getType(ELContext context,
                          Object base,
                          Object property)
  {
    if (base instanceof ResourceBundle) {
      context.setPropertyResolved(true);

      return null;
    }
    else
      return null;
  }

  @Override
  public Object getValue(ELContext context,
                         Object base,
                         Object property)
  {
    if (base instanceof ResourceBundle) {
      context.setPropertyResolved(true);
      
      ResourceBundle bundle = (ResourceBundle) base;
      
      String key = String.valueOf(property);

      String value = bundle.getString(key);

      if (value != null)
        return value;
      else
        return "???" + key + "???";
    }
    
    return null;
  }

  @Override
  public boolean isReadOnly(ELContext context,
                            Object base,
                            Object property)
  {
    if (base instanceof ResourceBundle) {
      context.setPropertyResolved(true);

      return true;
    }
    
    return true;
  }

  @Override
  public void setValue(ELContext context,
                       Object base,
                       Object property,
                       Object value)
  {
    if (base instanceof ResourceBundle) {
      context.setPropertyResolved(true);
      
      throw new PropertyNotWritableException(String.valueOf(base));
    }
  }
}
