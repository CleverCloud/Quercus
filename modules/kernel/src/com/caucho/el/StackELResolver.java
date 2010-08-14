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

import javax.el.ELContext;
import javax.el.ELResolver;
import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Stack-based composite resolver,
 */
public class StackELResolver extends ELResolver {
  public final ArrayList<ELResolver> _resolverStack
    = new ArrayList<ELResolver>();
  
  public StackELResolver()
  {
  }
  
  public StackELResolver(ELResolver a, ELResolver b)
  {
    push(b);
    push(a);
  }

  public void push(ELResolver elResolver)
  {
    if (elResolver == null)
      throw new NullPointerException();

    _resolverStack.add(elResolver);
  }

  public ELResolver pop()
  {
    if (_resolverStack.size() > 0)
      return _resolverStack.remove(_resolverStack.size() - 1);
    else
      return null;
  }
  
  @Override
  public Class<?> getCommonPropertyType(ELContext env, Object base)
  {
    for (int i = _resolverStack.size() - 1; i >= 0; i--) {
      ELResolver resolver = _resolverStack.get(i);

      return getCommonPropertyType(env, base);
    }

    return null;
  }

  @Override
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext env,
                                                           Object base)
  {
    ArrayList<FeatureDescriptor> descriptors = null;

    for (int i = _resolverStack.size() - 1; i >= 0; i--) {
      ELResolver resolver = _resolverStack.get(i);

      Iterator<FeatureDescriptor> iter
        = resolver.getFeatureDescriptors(env, base);

      if (iter == null)
        continue;

      if (descriptors == null)
        descriptors = new ArrayList<FeatureDescriptor>();

      while (iter.hasNext()) {
        FeatureDescriptor desc = iter.next();

        descriptors.add(desc);
      }
    }

    if (descriptors != null)
      return descriptors.iterator();
    else
      return null;
  }

  @Override
  public Class<?> getType(ELContext context,
                          Object base,
                          Object property)
  {
    context.setPropertyResolved(false);

    for (int i = _resolverStack.size() - 1; i >= 0; i--) {
      ELResolver resolver = _resolverStack.get(i);

      Class type = resolver.getType(context, base, property);

      if (context.isPropertyResolved())
        return type;
    }

    return null;
  }

  @Override
  public Object getValue(ELContext context,
                         Object base,
                         Object property)
  {
    context.setPropertyResolved(false);

    for (int i = _resolverStack.size() - 1; i >= 0; i--) {
      ELResolver resolver = _resolverStack.get(i);

      Object value = resolver.getValue(context, base, property);

      if (context.isPropertyResolved()) {
        return value;
      }
    }

    return null;
  }

  @Override
  public boolean isReadOnly(ELContext context,
                            Object base,
                            Object property)
  {
    context.setPropertyResolved(false);

    for (int i = _resolverStack.size() - 1; i >= 0; i--) {
      ELResolver resolver = _resolverStack.get(i);

      boolean isReadOnly = resolver.isReadOnly(context, base, property);

      if (context.isPropertyResolved())
        return isReadOnly;
    }

    return false;
  }

  @Override
  public void setValue(ELContext context,
                       Object base,
                       Object property,
                       Object value)
  {
    context.setPropertyResolved(false);

    for (int i = _resolverStack.size() - 1; i >= 0; i--) {
      ELResolver resolver = _resolverStack.get(i);

      resolver.setValue(context, base, property, value);

      if (context.isPropertyResolved()) {
        return;
      }
    }
  }
}
