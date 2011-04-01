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

package com.caucho.el;

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;

import javax.el.ELContext;
import javax.el.ELResolver;
import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Creates a variable resolver based on the classloader.
 */
public class EnvironmentELResolver extends ELResolver {
  private static final EnvironmentLocal<EnvironmentELResolver> _local
    = new EnvironmentLocal<EnvironmentELResolver>();
  
  private ArrayList<ELResolver> _resolvers
    = new ArrayList<ELResolver>();

  private EnvironmentELResolver(ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentLevelELResolver resolver
          = EnvironmentLevelELResolver.create(loader);

        if (! _resolvers.contains(resolver))
          _resolvers.add(resolver);
      }
    }
    
    EnvironmentLevelELResolver resolver
      = EnvironmentLevelELResolver.create(ClassLoader.getSystemClassLoader());

    if (! _resolvers.contains(resolver))
      _resolvers.add(resolver);
  }
  
  /**
   * Creates the resolver
   */
  public static EnvironmentELResolver create()
  {
    return create(Thread.currentThread().getContextClassLoader());
  }
  
  /**
   * Creates the resolver
   */
  public static EnvironmentELResolver create(ClassLoader loader)
  {
    EnvironmentELResolver elResolver = _local.getLevel(loader);

    if (elResolver == null) {
      elResolver = new EnvironmentELResolver(loader);
      
      _local.set(elResolver, loader);
    }

    return elResolver;
  }

  /**
   * Returns true for read-only.
   */
  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property)
  {
    if (property != null || ! (base instanceof String))
      return true;

    context.setPropertyResolved(true);

    return false;
  }
  
  /**
   * Returns the named variable value.
   */
  @Override
  public Class<?> getType(ELContext context,
                        Object base,
                        Object property)
  {
    Object value = getValue(context, base, property);

    if (value != null)
      return value.getClass();
    else
      return null;
  }

  public Class<?> getCommonPropertyType(ELContext context,
                                        Object base)
  {
    return null;
  }

  public Iterator<FeatureDescriptor>
    getFeatureDescriptors(ELContext context, Object base)
  {
    return null;
  }
  
  /**
   * Returns the named variable value.
   */
  @Override
  public Object getValue(ELContext context,
                         Object base,
                         Object property)
  {
    if (base != null)
      return null;
    else if (! (property instanceof String))
      return null;

    context.setPropertyResolved(false);
    for (int i = 0; i < _resolvers.size(); i++) {
      Object value = _resolvers.get(i).getValue(context, base, property);

      if (context.isPropertyResolved())
        return value;
    }

    return null;
  }
  
  /**
   * Sets the value for the named variable.
   */
  @Override
  public void setValue(ELContext context,
                       Object base,
                       Object property,
                       Object value)
  {
    if (base != null || ! (property instanceof String))
      return;

    context.setPropertyResolved(false);
    for (int i = 0; i < _resolvers.size(); i++) {
      _resolvers.get(i).setValue(context, base, property, value);
      
      if (context.isPropertyResolved()) {
        return;
      }
    }
  }

  public String toString()
  {
    return "EnvironmentELResolver[]";
  }
}
