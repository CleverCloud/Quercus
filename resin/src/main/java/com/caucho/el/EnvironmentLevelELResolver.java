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

import com.caucho.config.inject.InjectManager;
import com.caucho.loader.*;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.enterprise.inject.spi.BeanManager;
import java.beans.FeatureDescriptor;
import java.util.Iterator;

/**
 * Creates a variable resolver based on the classloader.
 */
public class EnvironmentLevelELResolver extends ELResolver {
  private static final EnvironmentLocal<EnvironmentLevelELResolver> _local
    = new EnvironmentLocal<EnvironmentLevelELResolver>();
  
  private final ClassLoader _loader;
  private final ELResolver _beanResolver;

  private EnvironmentLevelELResolver(ClassLoader loader)
  {
    _loader = loader;

    if (Environment.getEnvironmentClassLoader(loader) != null) {
      BeanManager beanManager = InjectManager.create(loader);
      _beanResolver = beanManager.getELResolver();
    }
    else
      _beanResolver = null;
  }
  
  /**
   * Creates the resolver
   */
  public static EnvironmentLevelELResolver create()
  {
    return create(Thread.currentThread().getContextClassLoader());
  }
  
  /**
   * Creates the resolver
   */
  public static EnvironmentLevelELResolver create(ClassLoader loader)
  {
    EnvironmentLevelELResolver elResolver = _local.getLevel(loader);

    if (elResolver == null) {
      for (; loader != null; loader = loader.getParent()) {
        if (loader instanceof EnvironmentClassLoader) {
          elResolver = new EnvironmentLevelELResolver(loader);
          _local.set(elResolver, loader);

          return elResolver;
        }
      }

      loader = ClassLoader.getSystemClassLoader();
      elResolver = new EnvironmentLevelELResolver(loader);
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
  public Object getValue(ELContext env,
                         Object base,
                         Object property)
  {
    if (base != null)
      return null;
    else if (! (property instanceof String))
      return null;

    String var = (String) property;

    if (_beanResolver != null) {
      Object value = _beanResolver.getValue(env, base, property);

      if (value != null) {
        env.setPropertyResolved(true);

        return value;
      }
    }

    Object value = EL.getLevelVar(var, _loader);

    if (value == null)
      return null;

    env.setPropertyResolved(true);

    if (value == EL.NULL)
      return null;
    else
      return value;
  }
  
  /**
   * Sets the value for the named variable.
   */
  @Override
  public void setValue(ELContext env,
                       Object base,
                       Object property,
                       Object value)
  {
    if (base != null || ! (property instanceof String))
      return;

    env.setPropertyResolved(true);

    String name = (String) property;

    EL.putVar(name, value, _loader);
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof EnvironmentLevelELResolver))
      return false;

    EnvironmentLevelELResolver resolver = (EnvironmentLevelELResolver) o;

    return _loader == resolver._loader;
  }

  public String toString()
  {
    return "EnvironmentLevelELResolver[" + _loader + "]";
  }
}
