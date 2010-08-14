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
import java.util.Iterator;

/**
 * Resolves properties based on arrays.
 */
public class CompositeELResolver extends ELResolver {
  private final ArrayList<ELResolver> _resolvers
    = new ArrayList<ELResolver>();
  
  public CompositeELResolver()
  {
  }

  public void add(ELResolver elResolver)
  {
    if (elResolver == null)
      throw new NullPointerException();

    _resolvers.add(elResolver);
  }
  
  @Override
  public Class<?> getCommonPropertyType(ELContext env, Object base)
  {
    Class commonClass = null;
    
    int size = _resolvers.size();
    for (int i = 0; i < size; i++) {
      ELResolver resolver = _resolvers.get(i);

      Class cl = resolver.getCommonPropertyType(env, base);

      if (cl == null)
        continue;

      commonClass = cl;
    }

    return commonClass;
  }

  @Override
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext env,
                                                           Object base)
  {
    ArrayList<FeatureDescriptor> descriptors
      = new ArrayList<FeatureDescriptor>();

    int size = _resolvers.size();
    for (int i = 0; i < size; i++) {
      ELResolver resolver = _resolvers.get(i);

      Iterator<FeatureDescriptor> iter
        = resolver.getFeatureDescriptors(env, base);
      
      if (iter == null)
        continue;

      while (iter.hasNext()) {
        FeatureDescriptor desc = iter.next();

        descriptors.add(desc);
      }
    }

    return descriptors.iterator();
  }

  @Override
  public Class<?> getType(ELContext env,
                          Object base,
                          Object property)
  {
    env.setPropertyResolved(false);

    int size = _resolvers.size();
    for (int i = 0; i < size; i++) {
      ELResolver resolver = _resolvers.get(i);

      Class type = resolver.getType(env, base, property);

      if (env.isPropertyResolved())
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

    int size = _resolvers.size();
    for (int i = 0; i < size; i++) {
      ELResolver resolver = _resolvers.get(i);

      Object value = resolver.getValue(context, base, property);

      if (context.isPropertyResolved())
        return value;
    }

    return null;
  }

  @Override
  public boolean isReadOnly(ELContext context,
                            Object base,
                            Object property)
  {
    context.setPropertyResolved(false);

    int size = _resolvers.size();
    for (int i = 0; i < size; i++) {
      ELResolver resolver = _resolvers.get(i);

      boolean value = resolver.isReadOnly(context, base, property);

      if (context.isPropertyResolved())
        return value;
    }
    
    return true;
  }

  @Override
  public void setValue(ELContext context,
                       Object base,
                       Object property,
                       Object value)
  {
    context.setPropertyResolved(false);

    int size = _resolvers.size();
    for (int i = 0; i < size; i++) {
      ELResolver resolver = _resolvers.get(i);

      resolver.setValue(context, base, property, value);

      if (context.isPropertyResolved())
        return;
    }
  }
}
